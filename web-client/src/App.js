import React, { useState, useEffect } from 'react';
import StreamViewer from './components/StreamViewer';
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
        <h1>🎥 WebRTC Stream Viewer</h1>
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
          <StreamViewer streamId={streamId} />
        )}
      </main>
      
      <footer className="App-footer">
        <p>WebRTC Streaming with GStreamer Android</p>
      </footer>
    </div>
  );
}

export default App;
