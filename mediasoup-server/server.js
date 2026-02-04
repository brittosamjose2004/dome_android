const express = require('express');
const http = require('http');
const socketIO = require('socket.io');
const mediasoup = require('mediasoup');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const io = socketIO(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

const PORT = process.env.PORT || 3002;

// mediasoup workers and routers
let worker;
let router;
const transports = new Map();
const producers = new Map();
const consumers = new Map();
const streams = new Map();

// Initialize mediasoup
async function createWorker() {
  worker = await mediasoup.createWorker({
    logLevel: 'warn',
    rtcMinPort: 10000,
    rtcMaxPort: 10100,
  });

  console.log(`mediasoup worker created [pid:${worker.pid}]`);

  worker.on('died', () => {
    console.error('mediasoup worker died, exiting...');
    process.exit(1);
  });

  return worker;
}

async function createRouter() {
  const mediaCodecs = [
    {
      kind: 'audio',
      mimeType: 'audio/opus',
      clockRate: 48000,
      channels: 2
    },
    {
      kind: 'video',
      mimeType: 'video/VP8',
      clockRate: 90000,
      parameters: {
        'x-google-start-bitrate': 1000
      }
    },
    {
      kind: 'video',
      mimeType: 'video/H264',
      clockRate: 90000,
      parameters: {
        'packetization-mode': 1,
        'profile-level-id': '42e01f',
        'level-asymmetry-allowed': 1
      }
    }
  ];

  router = await worker.createRouter({ mediaCodecs });
  console.log('mediasoup router created');
  return router;
}

// Root endpoint
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Mediasoup SFU Server</title>
      <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 50px auto; padding: 20px; }
        h1 { color: #2563eb; }
        .status { background: #f0fdf4; padding: 15px; border-radius: 8px; margin: 20px 0; }
        .endpoint { background: #f8fafc; padding: 10px; margin: 10px 0; border-left: 4px solid #3b82f6; }
        code { background: #e2e8f0; padding: 2px 6px; border-radius: 3px; }
      </style>
    </head>
    <body>
      <h1>🎥 Mediasoup SFU Server</h1>
      <div class="status">
        <h2>✅ Server is running</h2>
        <p><strong>Active Streams:</strong> ${streams.size}</p>
        <p><strong>Active Producers:</strong> ${producers.size}</p>
        <p><strong>Active Consumers:</strong> ${consumers.size}</p>
      </div>
      
      <h2>📡 API Endpoints</h2>
      <div class="endpoint">
        <strong>GET /api/streams</strong> - List all active streams
      </div>
      <div class="endpoint">
        <strong>GET /api/health</strong> - Server health check
      </div>
      
      <h2>🔌 WebSocket</h2>
      <p>Socket.IO endpoint available at <code>/socket.io/</code></p>
      
      <h2>📱 Usage</h2>
      <ol>
        <li>Start Android app with this server URL</li>
        <li>Open web viewer at your web client URL</li>
        <li>Stream will appear in viewer automatically</li>
      </ol>
      
      <p style="margin-top: 40px; color: #64748b; text-align: center;">
        Dome Android WebRTC Streaming Platform
      </p>
    </body>
    </html>
  `);
});

// REST API for streams list
app.get('/api/streams', (req, res) => {
  const streamList = Array.from(streams.values()).map(stream => ({
    id: stream.id,
    producerId: stream.producerId,
    createdAt: stream.createdAt,
    viewerCount: stream.consumers ? stream.consumers.size : 0
  }));
  res.json({ streams: streamList });
});

app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok',
    activeStreams: streams.size,
    activeProducers: producers.size,
    activeConsumers: consumers.size
  });
});

// Socket.IO handling
io.on('connection', (socket) => {
  console.log(`Client connected: ${socket.id}`);

  // Get router RTP capabilities
  socket.on('getRouterRtpCapabilities', (callback) => {
    callback({ data: router.rtpCapabilities });
  });

  // Create producer transport (for Android streamer)
  socket.on('createProducerTransport', async (callback) => {
    try {
      const transport = await router.createWebRtcTransport({
        listenIps: [
          {
            ip: '0.0.0.0',
            announcedIp: process.env.ANNOUNCED_IP || '127.0.0.1'
          }
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true
      });

      transports.set(`producer_${socket.id}`, transport);

      callback({
        data: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters
        }
      });
    } catch (error) {
      console.error('Error creating producer transport:', error);
      callback({ error: error.message });
    }
  });

  // Create consumer transport (for web viewers)
  socket.on('createConsumerTransport', async (callback) => {
    try {
      const transport = await router.createWebRtcTransport({
        listenIps: [
          {
            ip: '0.0.0.0',
            announcedIp: process.env.ANNOUNCED_IP || '127.0.0.1'
          }
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true
      });

      transports.set(`consumer_${socket.id}`, transport);

      callback({
        data: {
          id: transport.id,
          iceParameters: transport.iceParameters,
          iceCandidates: transport.iceCandidates,
          dtlsParameters: transport.dtlsParameters
        }
      });
    } catch (error) {
      console.error('Error creating consumer transport:', error);
      callback({ error: error.message });
    }
  });

  // Connect producer transport
  socket.on('connectProducerTransport', async ({ transportId, dtlsParameters }, callback) => {
    try {
      const transport = transports.get(`producer_${socket.id}`);
      await transport.connect({ dtlsParameters });
      callback({ success: true });
    } catch (error) {
      console.error('Error connecting producer transport:', error);
      callback({ error: error.message });
    }
  });

  // Connect consumer transport
  socket.on('connectConsumerTransport', async ({ transportId, dtlsParameters }, callback) => {
    try {
      const transport = transports.get(`consumer_${socket.id}`);
      await transport.connect({ dtlsParameters });
      callback({ success: true });
    } catch (error) {
      console.error('Error connecting consumer transport:', error);
      callback({ error: error.message });
    }
  });

  // Produce (stream from Android)
  socket.on('produce', async ({ transportId, kind, rtpParameters }, callback) => {
    try {
      const transport = transports.get(`producer_${socket.id}`);
      if (!transport) {
        return callback({ error: 'Transport not found' });
      }

      const producer = await transport.produce({ kind, rtpParameters });
      const streamId = `stream_${Date.now()}`;

      producers.set(producer.id, { producer, socketId: socket.id });

      // Register stream
      if (!streams.has(streamId)) {
        streams.set(streamId, {
          id: streamId,
          producerId: producer.id,
          producers: new Map(),
          consumers: new Set(),
          createdAt: new Date().toISOString()
        });
      }
      
      streams.get(streamId).producers.set(kind, producer);

      console.log(`Producer created: ${producer.id} for stream: ${streamId}`);
      
      // Notify all clients about new stream
      io.emit('newStream', { streamId, producerId: producer.id });

      callback({ data: { id: producer.id, streamId } });
    } catch (error) {
      console.error('Error producing:', error);
      callback({ error: error.message });
    }
  });

  // Consume (view on web)
  socket.on('consume', async ({ transportId, producerId, rtpCapabilities }, callback) => {
    try {
      const transport = transports.get(`consumer_${socket.id}`);
      if (!transport) {
        return callback({ error: 'Transport not found' });
      }

      // Find the producer
      const producerData = producers.get(producerId);
      if (!producerData) {
        return callback({ error: 'Producer not found' });
      }

      if (!router.canConsume({ producerId, rtpCapabilities })) {
        return callback({ error: 'Cannot consume' });
      }

      const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused: false
      });

      consumers.set(consumer.id, { consumer, socketId: socket.id });

      console.log(`Consumer created: ${consumer.id} for producer: ${producerId}`);

      callback({
        data: {
          id: consumer.id,
          producerId: producerId,
          kind: consumer.kind,
          rtpParameters: consumer.rtpParameters
        }
      });
    } catch (error) {
      console.error('Error consuming:', error);
      callback({ error: error.message });
    }
  });

  // Resume consumer
  socket.on('resumeConsumer', async ({ consumerId }, callback) => {
    try {
      const consumerData = consumers.get(consumerId);
      if (consumerData) {
        await consumerData.consumer.resume();
      }
      if (callback) callback({ success: true });
    } catch (error) {
      if (callback) callback({ error: error.message });
    }
  });

  // Handle disconnect
  socket.on('disconnect', () => {
    console.log(`Client disconnected: ${socket.id}`);
    
    // Clean up transports
    const producerTransport = transports.get(`producer_${socket.id}`);
    if (producerTransport) {
      producerTransport.close();
      transports.delete(`producer_${socket.id}`);
    }

    const consumerTransport = transports.get(`consumer_${socket.id}`);
    if (consumerTransport) {
      consumerTransport.close();
      transports.delete(`consumer_${socket.id}`);
    }

    // Clean up producers
    for (const [producerId, producerData] of producers.entries()) {
      if (producerData.socketId === socket.id) {
        producerData.producer.close();
        producers.delete(producerId);
        
        // Remove associated streams
        for (const [streamId, stream] of streams.entries()) {
          if (stream.producerId === producerId) {
            streams.delete(streamId);
            io.emit('streamEnded', { streamId });
          }
        }
      }
    }

    // Clean up consumers
    for (const [consumerId, consumerData] of consumers.entries()) {
      if (consumerData.socketId === socket.id) {
        consumerData.consumer.close();
        consumers.delete(consumerId);
      }
    }
  });
});

// Initialize and start server
async function startServer() {
  try {
    await createWorker();
    await createRouter();
    
    server.listen(PORT, () => {
      console.log(`mediasoup SFU server running on port ${PORT}`);
      console.log(`REST API: http://localhost:${PORT}/api/streams`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

startServer();
