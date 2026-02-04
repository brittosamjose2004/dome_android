import React, { useState, useEffect } from 'react';
import StreamViewer from './components/StreamViewer';
import MediasoupViewer from './components/MediasoupViewer';
import StreamList from './components/StreamList';
import './App.css';

function App() {
  const [view, setView] = useState('list'); // 'list' or 'viewer'
  const [streamId, setStreamId] = useState(null);

  useEffect(() => {
    // Check URL parameters for direct stream access
    const params = new URLSearchParams(window.location.search);
    const urlStreamId = params.get('streamId');
    
    if (urlStreamId) {
      setStreamId(urlStreamId);
      setView('viewer');
    }
  }, []);

  const handleSelectStream = (id) => {
    setStreamId(id);
    setView('viewer');
  };

  const handleBackToList = () => {
    setView('list');
    setStreamId(null);
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>🎥 WebRTC Stream Viewer (Mediasoup SFU)</h1>
        {view === 'viewer' && (
          <button onClick={handleBackToList} className="back-button">
            ← Back to Streams
          </button>
        )}
      </header>
      
      <main className="App-main">
        {view === 'list' ? (
          <StreamList onSelectStream={handleSelectStream} />
        ) : (
          <>
            <div className="viewer-info">
              <p>Using mediasoup SFU for reliable streaming</p>
              <p>Stream ID: <code>{streamId}</code></p>
            </div>
            <MediasoupViewer streamId={streamId} />
            <details style={{marginTop: '2rem'}}>
              <summary>Legacy WebRTC Viewer (P2P)</summary>
              <StreamViewer streamId={streamId} />
            </details>
          </>
        )}
      </main>
      
      <footer className="App-footer">
        <p>WebRTC Streaming with GStreamer Android</p>
      </footer>
    </div>
  );
}

export default App;
