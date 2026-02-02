import React, { useEffect, useState } from 'react';
import './StreamList.css';

const API_URL = 'https://zany-adventure-pj7j4xpp49653r6x7-3000.app.github.dev';

const StreamList = ({ onSelectStream }) => {
  const [streams, setStreams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchStreams();
    const interval = setInterval(fetchStreams, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const fetchStreams = async () => {
    try {
      const response = await fetch(`${API_URL}/api/streams`);
      const data = await response.json();
      setStreams(data.streams);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching streams:', err);
      setError('Failed to load streams');
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="stream-list">
        <div className="loading">
          <div className="spinner"></div>
          <p>Loading streams...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="stream-list">
        <div className="error">
          <p>⚠️ {error}</p>
          <button onClick={fetchStreams} className="retry-button">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="stream-list">
      <div className="list-header">
        <h2>Available Streams</h2>
        <button onClick={fetchStreams} className="refresh-button">
          🔄 Refresh
        </button>
      </div>

      {streams.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">📹</div>
          <h3>No Active Streams</h3>
          <p>Start streaming from your Android app to see it here!</p>
        </div>
      ) : (
        <div className="streams-grid">
          {streams.map((stream) => (
            <div key={stream.id} className="stream-card">
              <div className="stream-header">
                <div className="live-badge">🔴 LIVE</div>
                <div className="viewer-count">
                  👥 {stream.viewerCount} {stream.viewerCount === 1 ? 'viewer' : 'viewers'}
                </div>
              </div>
              <div className="stream-body">
                <div className="stream-id">
                  <span className="label">Stream ID:</span>
                  <span className="value">{stream.id}</span>
                </div>
                <div className="stream-time">
                  Started: {new Date(stream.createdAt).toLocaleTimeString()}
                </div>
              </div>
              <div className="stream-footer">
                <button
                  onClick={() => onSelectStream(stream.id)}
                  className="watch-button"
                >
                  ▶️ Watch Stream
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default StreamList;
