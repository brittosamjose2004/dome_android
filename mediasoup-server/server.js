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
    callback(router.rtpCapabilities);
  });

  // Create WebRTC transport
  socket.on('createTransport', async ({ producing }, callback) => {
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

      transports.set(socket.id, transport);

      callback({
        id: transport.id,
        iceParameters: transport.iceParameters,
        iceCandidates: transport.iceCandidates,
        dtlsParameters: transport.dtlsParameters
      });
    } catch (error) {
      console.error('Error creating transport:', error);
      callback({ error: error.message });
    }
  });

  // Connect transport
  socket.on('connectTransport', async ({ dtlsParameters }, callback) => {
    try {
      const transport = transports.get(socket.id);
      await transport.connect({ dtlsParameters });
      callback();
    } catch (error) {
      console.error('Error connecting transport:', error);
      callback({ error: error.message });
    }
  });

  // Produce (stream from Android)
  socket.on('produce', async ({ kind, rtpParameters, streamId }, callback) => {
    try {
      const transport = transports.get(socket.id);
      const producer = await transport.produce({ kind, rtpParameters });

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
      io.emit('newStream', { streamId });

      callback({ id: producer.id });
    } catch (error) {
      console.error('Error producing:', error);
      callback({ error: error.message });
    }
  });

  // Consume (view on web)
  socket.on('consume', async ({ streamId, rtpCapabilities }, callback) => {
    try {
      const stream = streams.get(streamId);
      if (!stream) {
        return callback({ error: 'Stream not found' });
      }

      const transport = transports.get(socket.id);
      const consumers = [];

      // Create consumer for each producer in the stream
      for (const [kind, producer] of stream.producers.entries()) {
        if (!router.canConsume({ producerId: producer.id, rtpCapabilities })) {
          continue;
        }

        const consumer = await transport.consume({
          producerId: producer.id,
          rtpCapabilities,
          paused: false
        });

        consumers.push({
          id: consumer.id,
          kind: consumer.kind,
          rtpParameters: consumer.rtpParameters,
          producerId: producer.id
        });

        stream.consumers.add(socket.id);
      }

      callback({ consumers });
    } catch (error) {
      console.error('Error consuming:', error);
      callback({ error: error.message });
    }
  });

  // Resume consumer
  socket.on('resumeConsumer', async ({ consumerId }, callback) => {
    try {
      // Implementation for resume if needed
      callback();
    } catch (error) {
      callback({ error: error.message });
    }
  });

  // Handle disconnect
  socket.on('disconnect', () => {
    console.log(`Client disconnected: ${socket.id}`);
    
    // Clean up transports
    const transport = transports.get(socket.id);
    if (transport) {
      transport.close();
      transports.delete(socket.id);
    }

    // Clean up streams if this was a producer
    for (const [streamId, stream] of streams.entries()) {
      stream.consumers.delete(socket.id);
      
      // Remove stream if no more producers
      if (stream.consumers.size === 0) {
        for (const [kind, producer] of stream.producers.entries()) {
          if (producers.has(producer.id)) {
            producers.delete(producer.id);
          }
        }
        streams.delete(streamId);
        io.emit('streamEnded', { streamId });
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
