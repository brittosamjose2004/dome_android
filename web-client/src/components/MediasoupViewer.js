import React, { useEffect, useRef, useState } from 'react';
import * as mediasoupClient from 'mediasoup-client';
import io from 'socket.io-client';

const MEDIASOUP_SERVER = 'https://zany-adventure-pj7j4xpp49653r6x7-3002.app.github.dev';

function MediasoupViewer({ streamId }) {
  const videoRef = useRef(null);
  const [status, setStatus] = useState('Connecting...');
  const [socket, setSocket] = useState(null);
  const [device, setDevice] = useState(null);
  const [consumer, setConsumer] = useState(null);

  useEffect(() => {
    let mounted = true;
    let recvTransport = null;

    const connectToStream = async () => {
      try {
        // Connect to mediasoup server
        const socketConnection = io(MEDIASOUP_SERVER, {
          transports: ['websocket'],
        });

        socketConnection.on('connect', async () => {
          console.log('Connected to mediasoup server');
          setStatus('Connected');

          try {
            // Create mediasoup device
            const newDevice = new mediasoupClient.Device();

            // Get router RTP capabilities
            socketConnection.emit('getRouterRtpCapabilities', async (response) => {
              if (response.error) {
                setStatus('Error: ' + response.error);
                return;
              }

              await newDevice.load({ routerRtpCapabilities: response.data });
              setDevice(newDevice);

              // Create receive transport
              socketConnection.emit('createConsumerTransport', async (transportResponse) => {
                if (transportResponse.error) {
                  setStatus('Error: ' + transportResponse.error);
                  return;
                }

                const { id, iceParameters, iceCandidates, dtlsParameters } = transportResponse.data;

                recvTransport = newDevice.createRecvTransport({
                  id,
                  iceParameters,
                  iceCandidates,
                  dtlsParameters,
                });

                recvTransport.on('connect', ({ dtlsParameters }, callback, errback) => {
                  socketConnection.emit('connectConsumerTransport', {
                    transportId: recvTransport.id,
                    dtlsParameters,
                  }, (connectResponse) => {
                    if (connectResponse.error) {
                      errback(new Error(connectResponse.error));
                    } else {
                      callback();
                    }
                  });
                });

                recvTransport.on('connectionstatechange', (state) => {
                  console.log('Transport connection state:', state);
                  setStatus('Transport: ' + state);

                  if (state === 'failed' || state === 'closed') {
                    setStatus('Connection failed');
                  }
                });

                // Consume stream
                socketConnection.emit('consume', {
                  transportId: recvTransport.id,
                  producerId: streamId,
                  rtpCapabilities: newDevice.rtpCapabilities,
                }, async (consumeResponse) => {
                  if (consumeResponse.error) {
                    setStatus('Error: ' + consumeResponse.error);
                    return;
                  }

                  const { id, producerId, kind, rtpParameters } = consumeResponse.data;

                  const newConsumer = await recvTransport.consume({
                    id,
                    producerId,
                    kind,
                    rtpParameters,
                  });

                  const { track } = newConsumer;

                  // Set video element
                  if (videoRef.current && track.kind === 'video') {
                    const stream = new MediaStream([track]);
                    videoRef.current.srcObject = stream;
                    await videoRef.current.play();
                    setStatus('Playing');
                  }

                  setConsumer(newConsumer);

                  // Resume consumer
                  socketConnection.emit('resumeConsumer', { consumerId: id });
                });
              });
            });
          } catch (error) {
            console.error('Device load error:', error);
            setStatus('Error: ' + error.message);
          }
        });

        socketConnection.on('disconnect', () => {
          setStatus('Disconnected');
        });

        socketConnection.on('error', (error) => {
          setStatus('Error: ' + error);
        });

        setSocket(socketConnection);
      } catch (error) {
        console.error('Connection error:', error);
        setStatus('Error: ' + error.message);
      }
    };

    if (mounted && streamId) {
      connectToStream();
    }

    return () => {
      mounted = false;
      if (consumer) {
        consumer.close();
      }
      if (recvTransport) {
        recvTransport.close();
      }
      if (socket) {
        socket.disconnect();
      }
    };
  }, [streamId]);

  return (
    <div className="mediasoup-viewer">
      <div className="status">
        <span className="status-label">Status:</span>
        <span className={`status-value ${status.toLowerCase()}`}>{status}</span>
      </div>
      <div className="video-container">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          controls
          muted
          className="stream-video"
        />
      </div>
      <style jsx>{`
        .mediasoup-viewer {
          display: flex;
          flex-direction: column;
          gap: 1rem;
        }
        .status {
          padding: 0.75rem;
          background: #f5f5f5;
          border-radius: 4px;
          display: flex;
          gap: 0.5rem;
        }
        .status-label {
          font-weight: 600;
        }
        .status-value {
          color: #666;
        }
        .status-value.playing {
          color: #22c55e;
        }
        .status-value.connected {
          color: #3b82f6;
        }
        .status-value.error,
        .status-value.failed {
          color: #ef4444;
        }
        .video-container {
          position: relative;
          width: 100%;
          background: #000;
          border-radius: 8px;
          overflow: hidden;
        }
        .stream-video {
          width: 100%;
          height: auto;
          display: block;
        }
      `}</style>
    </div>
  );
}

export default MediasoupViewer;
