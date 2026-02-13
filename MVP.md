# Accord Chat MVP

## Overview
A self-hosted real-time chat application with a Spring Boot WebSocket backend and LibGDX frontend. This MVP demonstrates core chat functionality with a single chat room.

## Architecture

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.x
- **Communication**: WebSocket (STOMP protocol)
- **Database**: H2 (in-memory for MVP, file-based option available)
- **Persistence**: Spring Data JPA
- **Port**: 8080

#### Components:
1. **WebSocket Configuration**: Configures STOMP endpoints and message broker
2. **Domain Models**:
   - `User`: Represents chat users (id, username, joinedAt)
   - `ChatMessage`: Represents messages (id, username, content, timestamp)
3. **Repositories**: JPA repositories for data persistence
4. **Controllers**:
   - `UserController`: REST API for user login/registration
   - `ChatController`: WebSocket message handler
5. **Services**: Business logic for user and message management

### Frontend (LibGDX)
- **Framework**: LibGDX (cross-platform game/UI framework)
- **WebSocket Client**: Java WebSocket client
- **Screens**:
   - `LoginScreen`: Username entry
   - `ChatScreen`: Message display and input
- **UI Components**: Scene2D for UI elements

### Communication Flow
```
Client (LibGDX) <--> WebSocket <--> Spring Boot Server <--> H2 Database

1. User enters username → POST /api/users/login → Server creates/validates user
2. Client connects to WebSocket → /ws endpoint
3. User sends message → /app/chat.send → Server processes → /topic/messages
4. All connected clients receive message via subscription to /topic/messages
```

## Features

### Current MVP Features
- ✅ Single chat room (global)
- ✅ Username-based login (no password required for MVP)
- ✅ Real-time message broadcasting via WebSocket
- ✅ Message persistence in H2 database
- ✅ Message history on login
- ✅ Timestamp for each message
- ✅ User join/leave notifications
- ✅ Simple LibGDX UI with message list and input field

### MVP Limitations
- Single room only (no multiple channels)
- No authentication/authorization
- No user avatars
- No private messages
- No message editing/deletion
- No file uploads
- No emoji support
- In-memory H2 database (resets on restart by default)

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6+ (for backend)
- Gradle 7+ (for frontend)

### Backend Setup

1. **Navigate to backend directory**:
   ```bash
   cd backend
   ```

2. **Build the project**:
   ```bash
   mvn clean install
   ```

3. **Run the server**:
   ```bash
   mvn spring-boot:run
   ```
   
   The server will start on `http://localhost:8080`

4. **Access H2 Console** (optional):
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:chatdb`
   - Username: `sa`
   - Password: (leave empty)

### Frontend Setup

1. **Navigate to frontend directory**:
   ```bash
   cd frontend
   ```

2. **Build the project**:
   ```bash
   ./gradlew desktop:dist
   ```

3. **Run the application**:
   ```bash
   ./gradlew desktop:run
   ```

### Quick Start (Both Services)

From the root directory:

```bash
# Terminal 1 - Start backend
cd backend && mvn spring-boot:run

# Terminal 2 - Start frontend
cd frontend && ./gradlew desktop:run
```

## Configuration

### Backend Configuration (`application.properties`)
```properties
# Server
server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:chatdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# CORS Configuration (set to specific origins in production)
app.cors.allowed-origins=*

# Message Content Validation
app.message.max-length=1000

# Username Validation
app.username.max-length=50
app.username.min-length=3
```

**Security Note**: For production deployment, replace `app.cors.allowed-origins=*` with specific trusted domains (e.g., `https://yourdomain.com`).

### Frontend Configuration
- **WebSocket endpoint**: Configurable via `AppConfig.getWebSocketUrl()`
  - Default: `ws://localhost:8080/ws`
  - Override with system property: `-Daccord.websocket.url=ws://yourserver.com:8080/ws`
- **Window size**: 800x600
- **Title**: "Accord Chat"
- **Username constraints**: 3-50 characters, alphanumeric and underscore only
- **Message length**: Maximum 1000 characters

## Project Structure

```
accord-prototype/
├── MVP.md                          # This file
├── README.md                       # Project overview
├── backend/                        # Spring Boot backend
│   ├── pom.xml                    # Maven configuration
│   └── src/
│       └── main/
│           ├── java/com/accord/
│           │   ├── AccordApplication.java
│           │   ├── config/
│           │   │   └── WebSocketConfig.java
│           │   ├── controller/
│           │   │   ├── ChatController.java
│           │   │   └── UserController.java
│           │   ├── model/
│           │   │   ├── ChatMessage.java
│           │   │   └── User.java
│           │   ├── repository/
│           │   │   ├── ChatMessageRepository.java
│           │   │   └── UserRepository.java
│           │   └── service/
│           │       ├── ChatService.java
│           │       └── UserService.java
│           └── resources/
│               └── application.properties
└── frontend/                       # LibGDX frontend
    ├── build.gradle               # Root Gradle config
    ├── settings.gradle
    ├── core/                      # Shared code
    │   └── src/com/accord/
    │       ├── AccordGame.java
    │       ├── screen/
    │       │   ├── LoginScreen.java
    │       │   └── ChatScreen.java
    │       └── websocket/
    │           └── WebSocketClient.java
    └── desktop/                   # Desktop launcher
        └── src/com/accord/desktop/
            └── DesktopLauncher.java
```

## API Documentation

### REST Endpoints

#### User Login
- **POST** `/api/users/login`
- **Body**: `{ "username": "string" }`
- **Response**: `{ "id": 1, "username": "string", "joinedAt": "2024-01-01T12:00:00" }`

#### Get Messages
- **GET** `/api/messages`
- **Query Params**: `limit` (optional, default: 50)
- **Response**: Array of ChatMessage objects

### WebSocket Endpoints

#### Connect
- **Endpoint**: `/ws`
- **Protocol**: STOMP over WebSocket

#### Send Message
- **Destination**: `/app/chat.send`
- **Payload**: `{ "username": "string", "content": "string" }`

#### Subscribe to Messages
- **Destination**: `/topic/messages`
- **Receives**: `{ "id": 1, "username": "string", "content": "string", "timestamp": "2024-01-01T12:00:00" }`

## Development Roadmap

### Phase 1: MVP (Current)
- [x] Basic project structure
- [x] Spring Boot WebSocket backend
- [x] LibGDX frontend with basic UI
- [x] Single chat room
- [x] Username login
- [x] Real-time messaging
- [x] H2 database integration
- [x] Message persistence and history

### Phase 2: Enhanced Features
- [ ] Multiple chat rooms/channels
- [ ] User authentication (password-based)
- [ ] Private direct messages
- [ ] User online/offline status
- [ ] Typing indicators
- [ ] Message timestamps in UI
- [ ] User list panel

### Phase 3: Advanced Features
- [ ] File/image uploads
- [ ] Emoji picker and reactions
- [ ] Message search
- [ ] User profiles and avatars
- [ ] User roles and permissions
- [ ] Message editing and deletion
- [ ] Voice channels (stretch goal)

### Phase 4: Production Ready
- [ ] PostgreSQL/MySQL database option
- [ ] Docker containerization
- [ ] User registration with email
- [ ] Password reset functionality
- [ ] Rate limiting and security
- [ ] Mobile support (Android/iOS via LibGDX)
- [ ] Comprehensive logging and monitoring
- [ ] Backup and restore functionality

## Testing

### Backend Testing
```bash
cd backend
mvn test
```

### Frontend Testing
```bash
cd frontend
./gradlew test
```

### Manual Testing Checklist
1. Start backend server
2. Verify H2 console access
3. Start frontend application
4. Enter username and login (test validation: min 3 chars, max 50 chars, alphanumeric + underscore)
5. Send a test message
6. Test message length limit (1000 characters)
7. Test invalid username characters (should be rejected)
8. Open second client instance
9. Verify message appears in both clients
10. Close and reopen client
11. Verify message history loads
12. Test connection error handling (disconnect server)

## Security

### MVP Security Features
- ✅ **Input Validation**: Username and message content validation
  - Username: 3-50 characters, alphanumeric and underscore only
  - Message: Maximum 1000 characters
- ✅ **CORS Configuration**: Configurable via environment variables
- ✅ **Database Constraints**: Length limits enforced at database level
- ✅ **Request Limits**: Message history API limited to 500 messages max
- ✅ **Error Handling**: Proper exception handling with user-friendly messages
- ✅ **Logging**: Structured logging with java.util.logging

### Security Limitations (MVP)
- ⚠️ **No Authentication**: Username-only login (no passwords)
- ⚠️ **No Authorization**: All users can see all messages
- ⚠️ **No Encryption**: Messages sent in plain text over WebSocket
- ⚠️ **No Rate Limiting**: No protection against spam or DoS
- ⚠️ **CORS Default**: Allows all origins by default (must configure for production)

### Production Security Recommendations
1. **Enable HTTPS/WSS**: Use TLS for all connections
2. **Configure CORS**: Set `app.cors.allowed-origins` to specific trusted domains
3. **Add Authentication**: Implement password-based or OAuth authentication
4. **Add Authorization**: Implement role-based access control
5. **Enable Rate Limiting**: Prevent abuse and DoS attacks
6. **Input Sanitization**: Additional validation for XSS prevention
7. **Security Headers**: Add security headers (CSP, HSTS, etc.)
8. **Audit Logging**: Log security-relevant events
9. **Regular Updates**: Keep dependencies up to date
10. **Security Scanning**: Run regular vulnerability scans

## Troubleshooting

### Backend Issues

**Port 8080 already in use**:
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

**Database connection errors**:
- Check H2 console configuration
- Verify JDBC URL matches application.properties
- Ensure H2 dependency is included

### Frontend Issues

**WebSocket connection failed**:
- Verify backend is running on localhost:8080
- Check firewall settings
- Review browser console for CORS errors

**LibGDX won't start**:
- Verify Java version (17+)
- Check Gradle wrapper permissions: `chmod +x gradlew`
- Clear Gradle cache: `./gradlew clean`

### Common Issues

**Messages not appearing**:
- Check WebSocket connection status
- Verify STOMP subscription to /topic/messages
- Check browser/application console for errors

**Login not working**:
- Verify backend API is accessible: `curl http://localhost:8080/api/users/login`
- Check network tab for API response
- Ensure username is not empty

## Technology Stack

### Backend
- **Java 17+**: Programming language
- **Spring Boot 3.x**: Application framework
- **Spring WebSocket**: Real-time communication
- **Spring Data JPA**: Data persistence
- **H2 Database**: In-memory database
- **Maven**: Build tool
- **Lombok** (optional): Reduce boilerplate code

### Frontend
- **Java 17+**: Programming language
- **LibGDX**: Cross-platform UI framework
- **Scene2D**: UI toolkit
- **Java-WebSocket**: WebSocket client library
- **Gradle**: Build tool

## Performance Considerations

### MVP Performance Profile
- **Expected Users**: 5-10 concurrent users
- **Message Throughput**: ~100 messages/minute
- **Database**: In-memory (fast, but not persistent)
- **Memory Usage**: ~200MB backend, ~150MB frontend

### Scalability Notes
For production use beyond MVP:
1. Switch to persistent database (PostgreSQL/MySQL)
2. Implement connection pooling
3. Add Redis for session management
4. Implement load balancing for multiple servers
5. Add message pagination/lazy loading
6. Optimize database queries with indexes

## Contributing

This is an MVP prototype. Contributions welcome for:
- Bug fixes
- Performance improvements
- Documentation updates
- Feature implementations from roadmap

## License

[Specify license here]

## Contact

[Project maintainer contact information]
