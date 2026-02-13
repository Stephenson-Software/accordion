# accord-prototype

**Accord Chat MVP** - A self-hosted real-time chat application with Spring Boot WebSocket backend and LibGDX frontend.

## Overview

Accord is a Discord-like self-hosted chat application designed for simplicity and extensibility. This MVP demonstrates core chat functionality with a single chat room, username-based login, and real-time messaging.

**Technology Stack:**
- **Backend**: Spring Boot 3.x, WebSocket (STOMP), H2 Database, Spring Data JPA
- **Web Frontend**: Spring Boot, Thymeleaf, SockJS, STOMP.js (browser-based)
- **Desktop Frontend**: LibGDX, Java WebSocket client, Scene2D UI

## Features

- ✅ Single chat room (global)
- ✅ Username-based login (no password required for MVP)
- ✅ Real-time message broadcasting via WebSocket
- ✅ Message persistence in H2 database
- ✅ Message history on login
- ✅ Timestamp for each message
- ✅ User join/leave notifications
- ✅ **Browser-based web interface** (accessible from any device)
- ✅ Cross-platform desktop support (LibGDX)
- ✅ Docker containerization for easy deployment

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (for backend and webapp)
- Gradle 7+ (included via wrapper for LibGDX desktop frontend)
- Docker & Docker Compose (for containerized deployment)

### Running the Application

**Option 1: Using Docker Compose (Recommended)**

```bash
# Copy sample environment file and customize if needed
cp sample.env .env
# Edit .env to configure ports and other settings

# Start all services (backend + web app)
docker compose up -d

# Access the web application
open http://localhost:3000

# For network access from other devices:
# 1. Set DOCKER_HOST_IP to your machine's IP in .env
# 2. Restart: docker compose down && docker compose up -d
# 3. Access from other devices: http://YOUR_IP:3000
```

**Option 2: Run services manually**

```bash
# Terminal 1 - Start backend
cd backend
mvn spring-boot:run

# Terminal 2 - Start web application
cd webapp
mvn spring-boot:run

# Access at http://localhost:3000

# Optional: Terminal 3 - Start LibGDX desktop client
cd frontend
./gradlew desktop:run
```

**Option 3: Run backend + multiple LibGDX clients**

```bash
# Terminal 1 - Start backend
cd backend && mvn spring-boot:run

# Terminal 2+ - Start as many LibGDX clients as you want
cd frontend && ./gradlew desktop:run
```

### Using the Chat Application

**Web Application (Browser):**
1. Navigate to `http://localhost:3000`
2. Enter your username (minimum 3 characters, alphanumeric + underscore)
3. Click "Join Chat"
4. Type your message and click "Send" or press Enter
5. Open in multiple browser tabs to test real-time messaging

**LibGDX Desktop Client:**
1. **Frontend window will open** (800x600)
2. **Enter your username** (minimum 3 characters)
3. **Click "Login"** to enter the chat room
4. **Type your message** and click "Send" or press Enter
5. **Open multiple clients** to test real-time messaging

**Backend API:**
- Backend runs on `http://localhost:8080`
- WebSocket endpoint: `ws://localhost:8080/ws`
- REST API: `http://localhost:8080/api/users/login`, `http://localhost:8080/api/messages`

## Docker Deployment

### Prerequisites for Docker

- Docker 20.10+ 
- Docker Compose 2.0+

### Configuration

The application uses environment variables for configuration. A `sample.env` file is provided with all available options:

```bash
# Copy and customize the environment file
cp sample.env .env

# Edit .env to configure:
# - Port numbers (BACKEND_PORT, WEBAPP_PORT)
# - CORS settings (APP_CORS_ALLOWED_ORIGINS)
# - Database configuration
# - Validation rules (message length, username constraints)
# - And more...
```

**Key configuration options:**

- `BACKEND_PORT`: Backend service port (default: 8080)
- `WEBAPP_PORT`: Web application port (default: 3000)
- `APP_CORS_ALLOWED_ORIGINS`: CORS allowed origins (default: `*` for development)
- `APP_MESSAGE_MAX_LENGTH`: Maximum message length (default: 1000)
- `APP_USERNAME_MIN_LENGTH`: Minimum username length (default: 3)
- `APP_USERNAME_MAX_LENGTH`: Maximum username length (default: 50)

See `sample.env` for the complete list of configurable options.

### Running with Docker Compose

The easiest way to run the backend in a container:

```bash
# Build and start the backend
docker compose up -d

# View logs
docker compose logs -f backend

# Stop the service
docker compose down
```

The services will be available at:
- **Web Application**: `http://localhost:3000` (browser-based UI)
- **Backend API**: `http://localhost:8080` (REST & WebSocket)

**Note:** If you customized ports in `.env`, use those port numbers instead.

### Building Docker Images Manually

**Backend:**
```bash
# Build the backend image
docker build -t accord-backend:latest -f Dockerfile .

# Run the container
docker run -d \
  -p 8080:8080 \
  --name accord-backend \
  accord-backend:latest
```

**Web Application:**
```bash
# Build the webapp image
docker build -t accord-webapp:latest -f Dockerfile.webapp .

# Run the container (requires backend to be running)
docker run -d \
  -p 3000:3000 \
  -e ACCORD_BACKEND_URL=http://backend:8080 \
  -e ACCORD_BACKEND_WS_URL=ws://backend:8080/ws \
  --link accord-backend:backend \
  --name accord-webapp \
  accord-webapp:latest
```

**View logs:**
```bash
docker logs -f accord-backend
docker logs -f accord-webapp
```

**Stop and remove:**
```bash
docker stop accord-backend accord-webapp
docker rm accord-backend
```

### Environment Variables

Configure the application using environment variables:

```bash
docker run -d \
  -p 8080:8080 \
  -e APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com" \
  -e APP_USERNAME_MIN_LENGTH=5 \
  -e APP_MESSAGE_MAX_LENGTH=500 \
  --name accord-backend \
  accord-backend:latest
```

See `compose.yml` for all available environment variables.

### Accessing H2 Console (Optional)

View the database contents:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:chatdb`
- Username: `sa`
- Password: (leave empty)

## Project Structure

```
accord-prototype/
├── MVP.md                          # Detailed MVP documentation
├── README.md                       # This file
├── Dockerfile                      # Docker image for backend
├── Dockerfile.webapp               # Docker image for web application
├── compose.yml                     # Docker Compose configuration
├── .dockerignore                   # Docker build exclusions
├── backend/                        # Spring Boot backend
│   ├── pom.xml                    # Maven configuration
│   └── src/main/java/com/accord/
│       ├── AccordApplication.java
│       ├── config/                # WebSocket configuration
│       ├── controller/            # REST and WebSocket controllers
│       ├── model/                 # JPA entities
│       ├── repository/            # Data repositories
│       ├── service/               # Business logic
│       └── util/                  # Validation utilities
├── webapp/                         # Spring Boot web application
│   ├── pom.xml                    # Maven configuration
│   └── src/main/
│       ├── java/com/accord/webapp/
│       │   ├── AccordWebApplication.java
│       │   └── controller/        # Web controllers
│       └── resources/
│           ├── templates/         # Thymeleaf HTML templates
│           │   ├── index.html    # Login page
│           │   └── chat.html     # Chat interface
│           └── application.properties
└── frontend/                       # LibGDX frontend (desktop)
    ├── build.gradle               # Root Gradle config
    ├── core/                      # Shared code
    │   └── src/com/accord/
    │       ├── AccordGame.java
    │       ├── config/            # Configuration
    │       ├── screen/            # Login & Chat screens
    │       └── websocket/         # WebSocket client
    └── desktop/                   # Desktop launcher
        └── src/com/accord/desktop/
            └── DesktopLauncher.java
```

## Building from Source

### Backend

```bash
cd backend
mvn clean install
```

### Frontend

```bash
cd frontend
./gradlew build
```

## Configuration

### Backend (`backend/src/main/resources/application.properties`)

```properties
server.port=8080
spring.datasource.url=jdbc:h2:mem:chatdb
spring.h2.console.enabled=true
```

### Frontend

WebSocket endpoint is configured in `ChatScreen.java`:
```java
URI uri = new URI("ws://localhost:8080/ws");
```

To connect to a remote server, change `localhost` to your server's address.

## Development Roadmap

See [MVP.md](MVP.md) for the complete roadmap and architecture details.

### Next Steps
- Multiple chat rooms/channels
- User authentication (password-based)
- Private direct messages
- User online/offline status
- Message search and pagination
- PostgreSQL/MySQL support for production

## Documentation

- **[MVP.md](MVP.md)** - Complete MVP documentation including:
  - Architecture details
  - API documentation
  - Setup instructions
  - Feature roadmap
  - Troubleshooting guide

## Contributing

This is an MVP prototype. Contributions are welcome for:
- Bug fixes
- Performance improvements
- Documentation updates
- Feature implementations from the roadmap

## License

[To be determined]

## Support

For issues or questions, please open an issue on GitHub.

