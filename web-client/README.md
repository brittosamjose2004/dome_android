# WebRTC Viewer Client

React-based web client for viewing WebRTC streams from Android devices.

## Setup

1. Install dependencies:
```bash
npm install
```

2. Configure environment (optional):
Create a `.env` file:
```
REACT_APP_SIGNALING_SERVER=ws://localhost:3000
REACT_APP_API_URL=http://localhost:3000
```

3. Start development server:
```bash
npm start
```

The app will open at http://localhost:3001

## Features

- View live streams from Android devices
- List all active streams
- Embed streams in other websites
- Real-time connection status
- Responsive design

## Building for Production

```bash
npm run build
```

The build folder will contain the production-ready files.
