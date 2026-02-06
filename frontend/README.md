# FinBuddy Frontend

This is the frontend application for FinBuddy, a financial portfolio management system.

## Project Structure

```
frontend/
├── index.html              # Main entry point
├── api-tester.html        # API testing interface
├── components/            # Reusable HTML components
├── css/                   # Stylesheets
├── js/                    # JavaScript files
│   ├── components/       # Component scripts
│   ├── pages/           # Page-specific scripts
│   └── utils/           # Utility functions
└── pages/               # Application pages
```

## Running the Frontend

### Option 1: Using VS Code Live Server

1. Install the "Live Server" extension in VS Code
2. Right-click on `index.html`
3. Select "Open with Live Server"
4. The app will open at `http://localhost:5500`

### Option 2: Using Python HTTP Server

```bash
# Navigate to frontend directory
cd frontend

# Python 3
python -m http.server 5500

# Python 2
python -m SimpleHTTPServer 5500
```

### Option 3: Using Node.js http-server

```bash
# Install http-server globally
npm install -g http-server

# Navigate to frontend directory
cd frontend

# Start server
http-server -p 5500
```

## Backend API

The frontend connects to the backend API running on `http://localhost:8081`

Make sure the backend is running before using the frontend.

## Configuration

The API base URL is configured in `js/utils/api.js`:

```javascript
const API_BASE_URL = "http://localhost:8081/api";
```

Update this URL if your backend runs on a different port or domain.

## Features

- **Dashboard**: Overview of portfolio performance
- **Portfolios**: Manage investment portfolios
- **Assets**: Track individual assets
- **Market Data**: Real-time market information
- **Wishlist**: Track assets you're interested in
- **Analytics**: Performance analysis and charts
- **Reports**: Generate financial reports
- **AI Chat**: AI-powered financial assistant
