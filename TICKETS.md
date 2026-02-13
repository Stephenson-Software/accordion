# Accord Chat Development Tickets

This document breaks down the development roadmap from [MVP.md](MVP.md) into actionable tickets. Each ticket represents a specific feature or improvement that can be tracked and implemented.

## Ticket Status Legend

- [COMPLETED] **Completed** - Feature is implemented and tested
- [IN_PROGRESS] **In Progress** - Currently being worked on
- [PLANNED] **Planned** - Ready to be picked up
- [FUTURE] **Future** - Planned for later phases

---

## Phase 1: MVP (Current) - [COMPLETED] COMPLETED

All Phase 1 tickets have been completed. See [MVP.md](MVP.md) for current functionality.

### [COMPLETED] TICKET-001: Project Structure Setup
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Infrastructure

**Description:**
Set up the basic project structure with backend and frontend directories, build configurations, and version control.

**Acceptance Criteria:**
- [x] Create Maven project structure for Spring Boot backend
- [x] Create Gradle project structure for LibGDX frontend
- [x] Configure build files (pom.xml, build.gradle)
- [x] Initialize git repository
- [x] Add .gitignore for Java/Maven/Gradle

### [COMPLETED] TICKET-002: Spring Boot WebSocket Backend
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend

**Description:**
Implement Spring Boot backend with WebSocket support for real-time communication.

**Acceptance Criteria:**
- [x] Configure Spring Boot 3.x application
- [x] Set up WebSocket with STOMP protocol
- [x] Create WebSocketConfig class
- [x] Configure message broker and endpoints
- [x] Test WebSocket connection manually

### [COMPLETED] TICKET-003: H2 Database Integration
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, Database

**Description:**
Integrate H2 in-memory database with Spring Data JPA for data persistence.

**Acceptance Criteria:**
- [x] Add H2 and Spring Data JPA dependencies
- [x] Configure H2 database in application.properties
- [x] Enable H2 console for debugging
- [x] Test database connection and console access

### [COMPLETED] TICKET-004: User Domain Model and Repository
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, Data Model

**Description:**
Create User entity with JPA annotations and repository for user management.

**Acceptance Criteria:**
- [x] Create User entity class (id, username, joinedAt)
- [x] Add JPA annotations (@Entity, @Id, @GeneratedValue)
- [x] Create UserRepository extending JpaRepository
- [x] Add unique constraint on username
- [x] Test basic CRUD operations

### [COMPLETED] TICKET-005: ChatMessage Domain Model and Repository
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, Data Model

**Description:**
Create ChatMessage entity with JPA annotations and repository for message persistence.

**Acceptance Criteria:**
- [x] Create ChatMessage entity (id, username, content, timestamp)
- [x] Add JPA annotations and validation constraints
- [x] Create ChatMessageRepository with custom queries
- [x] Add method to retrieve recent messages
- [x] Test message persistence and retrieval

### [COMPLETED] TICKET-006: User Service and REST API
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, API

**Description:**
Implement user service layer and REST endpoints for user login/registration.

**Acceptance Criteria:**
- [x] Create UserService with business logic
- [x] Implement username validation (3-50 chars, alphanumeric + underscore)
- [x] Create UserController with POST /api/users/login endpoint
- [x] Return user object with id, username, and joinedAt
- [x] Add error handling for invalid usernames
- [x] Test API with curl or Postman

### [COMPLETED] TICKET-007: Chat Service and WebSocket Controller
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, WebSocket

**Description:**
Implement chat service and WebSocket controller for message handling and broadcasting.

**Acceptance Criteria:**
- [x] Create ChatService for message management
- [x] Implement message validation (max 1000 chars)
- [x] Create ChatController with @MessageMapping
- [x] Handle messages from /app/chat.send
- [x] Broadcast messages to /topic/messages
- [x] Persist messages to database
- [x] Test WebSocket message flow

### [COMPLETED] TICKET-008: Message History API
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, API

**Description:**
Create REST endpoint to retrieve message history for new users joining the chat.

**Acceptance Criteria:**
- [x] Add GET /api/messages endpoint
- [x] Support limit query parameter (default: 50, max: 500)
- [x] Return messages ordered by timestamp descending
- [x] Include username, content, and timestamp
- [x] Test with various limit values

### [COMPLETED] TICKET-009: LibGDX Project Setup
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Frontend

**Description:**
Set up LibGDX project with desktop launcher and core module structure.

**Acceptance Criteria:**
- [x] Create LibGDX project using gdx-setup
- [x] Configure desktop launcher
- [x] Set up core module for shared code
- [x] Add Scene2D dependency for UI
- [x] Test basic window rendering

### [COMPLETED] TICKET-010: WebSocket Client Implementation
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Frontend, WebSocket

**Description:**
Implement Java WebSocket client for connecting to Spring Boot backend.

**Acceptance Criteria:**
- [x] Add Java-WebSocket library dependency
- [x] Create WebSocketClient class
- [x] Implement connection to ws://localhost:8080/ws
- [x] Implement STOMP protocol handling
- [x] Add message sending and receiving methods
- [x] Handle connection errors and reconnection
- [x] Test connection and message exchange

### [COMPLETED] TICKET-011: Login Screen UI
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Frontend, UI

**Description:**
Create login screen with username input and validation using Scene2D.

**Acceptance Criteria:**
- [x] Create LoginScreen class extending Screen
- [x] Add username text field with Scene2D
- [x] Add login button
- [x] Implement client-side username validation
- [x] Display error messages for invalid input
- [x] Transition to chat screen on successful login
- [x] Style UI elements appropriately

### [COMPLETED] TICKET-012: Chat Screen UI
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Frontend, UI

**Description:**
Create chat screen with message list, message input, and send button.

**Acceptance Criteria:**
- [x] Create ChatScreen class extending Screen
- [x] Add scrollable message list display
- [x] Add message input text field
- [x] Add send button (also support Enter key)
- [x] Display username with each message
- [x] Display timestamps for messages
- [x] Auto-scroll to latest message
- [x] Connect to WebSocket on screen show
- [x] Load message history on connection

### [COMPLETED] TICKET-013: Real-time Message Display
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Frontend, WebSocket

**Description:**
Implement real-time message reception and display in the chat screen.

**Acceptance Criteria:**
- [x] Subscribe to /topic/messages on WebSocket connection
- [x] Parse incoming JSON messages
- [x] Display received messages in the message list
- [x] Update UI on LibGDX render thread (gdx.app.postRunnable)
- [x] Test with multiple client instances

### [COMPLETED] TICKET-014: Input Validation and Error Handling
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Backend, Frontend

**Description:**
Implement comprehensive input validation and error handling across the application.

**Acceptance Criteria:**
- [x] Backend: Validate username (3-50 chars, alphanumeric + underscore)
- [x] Backend: Validate message content (max 1000 chars)
- [x] Backend: Return meaningful error responses
- [x] Frontend: Validate input before sending
- [x] Frontend: Display error messages to user
- [x] Add logging for debugging

### [COMPLETED] TICKET-015: MVP Documentation
**Status:** [COMPLETED] Completed  
**Priority:** P0  
**Component:** Documentation

**Description:**
Create comprehensive documentation for MVP including setup, usage, and API reference.

**Acceptance Criteria:**
- [x] Create MVP.md with architecture overview
- [x] Document setup instructions for backend and frontend
- [x] Document API endpoints and WebSocket protocol
- [x] Add troubleshooting section
- [x] Include testing checklist
- [x] Document security considerations
- [x] Add development roadmap

---

## Phase 2: Enhanced Features - [PLANNED] PLANNED

### [PLANNED] TICKET-101: Multiple Chat Rooms/Channels
**Status:** [PLANNED] Planned  
**Priority:** P1  
**Component:** Backend, Frontend, Database

**Description:**
Extend the application to support multiple chat rooms/channels. Users can create channels and switch between them.

**Acceptance Criteria:**
- [ ] Create Channel/Room entity (id, name, description, createdAt, createdBy)
- [ ] Create ChannelRepository
- [ ] Add REST endpoints for creating and listing channels
- [ ] Modify ChatMessage to include channelId
- [ ] Update WebSocket destinations to include channel ID (/topic/messages/{channelId})
- [ ] Create ChannelService for channel management
- [ ] Backend: Update message routing to channel-specific topics
- [ ] Frontend: Add channel list UI component
- [ ] Frontend: Add channel creation dialog
- [ ] Frontend: Allow switching between channels
- [ ] Frontend: Display current channel name
- [ ] Update message history to be channel-specific
- [ ] Test multi-channel messaging with multiple clients

**Estimated Effort:** 3-5 days

**Dependencies:** None

### [PLANNED] TICKET-102: User Authentication System
**Status:** [PLANNED] Planned  
**Priority:** P1  
**Component:** Backend, Security

**Description:**
Implement password-based authentication with Spring Security for secure user access.

**Acceptance Criteria:**
- [ ] Add Spring Security dependency
- [ ] Add password field to User entity (hashed with BCrypt)
- [ ] Create authentication configuration class
- [ ] Implement user registration endpoint with password
- [ ] Implement login endpoint returning JWT token
- [ ] Add password validation (min length, complexity)
- [ ] Protect WebSocket connections with authentication
- [ ] Update UserController with registration and login
- [ ] Add password reset mechanism (stretch)
- [ ] Test authentication flow end-to-end

**Estimated Effort:** 3-4 days

**Dependencies:** None

### [PLANNED] TICKET-103: Private Direct Messages
**Status:** [PLANNED] Planned  
**Priority:** P1  
**Component:** Backend, Frontend

**Description:**
Implement one-on-one direct messaging between users separate from public channels.

**Acceptance Criteria:**
- [ ] Create DirectMessage entity (id, senderId, recipientId, content, timestamp, read)
- [ ] Create DirectMessageRepository
- [ ] Add REST endpoints for sending and retrieving DMs
- [ ] Implement user-specific WebSocket destinations (/user/{userId}/queue/messages)
- [ ] Backend: Route DMs to specific user subscriptions
- [ ] Frontend: Add user list panel
- [ ] Frontend: Add click handler to start DM conversation
- [ ] Frontend: Create DM view/tab for conversations
- [ ] Frontend: Display unread message indicators
- [ ] Implement message read receipts
- [ ] Test DM delivery with multiple users

**Estimated Effort:** 4-5 days

**Dependencies:** TICKET-102 (User Authentication)

### [PLANNED] TICKET-104: User Online/Offline Status
**Status:** [PLANNED] Planned  
**Priority:** P2  
**Component:** Backend, Frontend, WebSocket

**Description:**
Track and display user online/offline status in real-time using WebSocket lifecycle events.

**Acceptance Criteria:**
- [ ] Add online status field to User entity
- [ ] Track WebSocket connections/disconnections
- [ ] Broadcast user status changes to /topic/user-status
- [ ] Send initial user list on connection
- [ ] Frontend: Subscribe to user status updates
- [ ] Frontend: Display user list with online indicators (green dot)
- [ ] Frontend: Update UI when users connect/disconnect
- [ ] Handle reconnection scenarios
- [ ] Test status updates with multiple clients

**Estimated Effort:** 2-3 days

**Dependencies:** None

### [PLANNED] TICKET-105: Typing Indicators
**Status:** [PLANNED] Planned  
**Priority:** P2  
**Component:** Backend, Frontend, WebSocket

**Description:**
Show typing indicators when users are composing messages in a channel.

**Acceptance Criteria:**
- [ ] Create typing indicator message type
- [ ] Send typing events when user types (debounced)
- [ ] Backend: Broadcast typing events to /topic/typing/{channelId}
- [ ] Frontend: Subscribe to typing events
- [ ] Frontend: Display "User is typing..." indicator
- [ ] Implement timeout to clear stale typing indicators
- [ ] Don't show typing indicator for the current user
- [ ] Test with multiple concurrent typers

**Estimated Effort:** 1-2 days

**Dependencies:** TICKET-101 (Multiple Channels)

### [PLANNED] TICKET-106: Enhanced Message Timestamps in UI
**Status:** [PLANNED] Planned  
**Priority:** P2  
**Component:** Frontend

**Description:**
Improve timestamp display in the UI with relative times and formatting.

**Acceptance Criteria:**
- [ ] Display timestamps in "Just now", "5 minutes ago" format
- [ ] Show full timestamp on hover
- [ ] Add date separators (e.g., "Today", "Yesterday", "May 10")
- [ ] Format timestamps based on locale
- [ ] Update relative times periodically
- [ ] Test across different timezones

**Estimated Effort:** 1-2 days

**Dependencies:** None

### [PLANNED] TICKET-107: User List Panel
**Status:** [PLANNED] Planned  
**Priority:** P2  
**Component:** Frontend

**Description:**
Add a user list panel showing all users in the current channel with their online status.

**Acceptance Criteria:**
- [ ] Create user list UI component (right sidebar)
- [ ] Display all channel members
- [ ] Show online/offline status indicators
- [ ] Sort online users at the top
- [ ] Add user count display ("5 members")
- [ ] Make panel collapsible/expandable
- [ ] Add click actions for user profiles/DMs
- [ ] Test with varying numbers of users

**Estimated Effort:** 2-3 days

**Dependencies:** TICKET-104 (Online Status), TICKET-101 (Channels)

---

## Phase 3: Advanced Features - [FUTURE] FUTURE

### [FUTURE] TICKET-201: File and Image Uploads
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Backend, Frontend, Storage

**Description:**
Enable users to upload and share files and images in chat messages.

**Acceptance Criteria:**
- [ ] Add file storage service (local filesystem or S3)
- [ ] Create FileAttachment entity (id, filename, fileType, size, url, uploadedBy, uploadedAt)
- [ ] Add file upload endpoint with multipart/form-data
- [ ] Validate file types and size limits (e.g., max 10MB)
- [ ] Generate thumbnails for images
- [ ] Link attachments to ChatMessage
- [ ] Frontend: Add file picker button
- [ ] Frontend: Display image previews inline
- [ ] Frontend: Add download links for non-image files
- [ ] Implement virus scanning for uploads (stretch)
- [ ] Test upload and display of various file types

**Estimated Effort:** 5-7 days

**Dependencies:** TICKET-102 (Authentication)

### [FUTURE] TICKET-202: Emoji Picker and Reactions
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Frontend

**Description:**
Add emoji picker for message composition and emoji reactions to messages.

**Acceptance Criteria:**
- [ ] Integrate emoji picker library or create custom picker
- [ ] Add emoji button to message input field
- [ ] Parse and display emoji in message content
- [ ] Create MessageReaction entity (id, messageId, userId, emoji)
- [ ] Add reaction endpoints (POST/DELETE /api/messages/{id}/reactions)
- [ ] Frontend: Add reaction button to each message
- [ ] Frontend: Display reaction counts and user lists
- [ ] Backend: Broadcast reaction updates via WebSocket
- [ ] Test emoji rendering and reactions

**Estimated Effort:** 3-4 days

**Dependencies:** TICKET-102 (Authentication)

### [FUTURE] TICKET-203: Message Search
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Backend, Frontend

**Description:**
Implement full-text search for messages within channels and DMs.

**Acceptance Criteria:**
- [ ] Add search endpoint GET /api/messages/search?q={query}&channelId={id}
- [ ] Implement full-text search (using JPA or Elasticsearch)
- [ ] Support search filters (by user, date range, channel)
- [ ] Highlight matching text in results
- [ ] Frontend: Add search bar UI component
- [ ] Frontend: Display search results with context
- [ ] Frontend: Navigate to message on result click
- [ ] Implement pagination for search results
- [ ] Test search performance with large message volumes

**Estimated Effort:** 4-5 days

**Dependencies:** None

### [FUTURE] TICKET-204: User Profiles and Avatars
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Backend, Frontend

**Description:**
Add user profiles with customizable avatars, display names, and status messages.

**Acceptance Criteria:**
- [ ] Extend User entity (displayName, bio, statusMessage, avatarUrl)
- [ ] Add profile update endpoint PUT /api/users/profile
- [ ] Add avatar upload endpoint
- [ ] Generate default avatars or use Gravatar
- [ ] Frontend: Create profile settings screen
- [ ] Frontend: Display user avatars in message list
- [ ] Frontend: Display user avatars in user list
- [ ] Frontend: Show profile popup on avatar click
- [ ] Add profile visibility controls (public/private)
- [ ] Test profile updates and avatar display

**Estimated Effort:** 3-5 days

**Dependencies:** TICKET-201 (File Uploads for avatars)

### [FUTURE] TICKET-205: User Roles and Permissions
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Backend, Security

**Description:**
Implement role-based access control with admin, moderator, and member roles.

**Acceptance Criteria:**
- [ ] Create Role entity (id, name, permissions)
- [ ] Add UserRole relationship (many-to-many)
- [ ] Define permissions (CREATE_CHANNEL, DELETE_MESSAGE, BAN_USER, etc.)
- [ ] Implement permission checks in controllers
- [ ] Add admin endpoints for user management
- [ ] Backend: Restrict channel creation to authorized users
- [ ] Frontend: Hide/show features based on user permissions
- [ ] Add audit logging for admin actions
- [ ] Test permission enforcement

**Estimated Effort:** 4-5 days

**Dependencies:** TICKET-102 (Authentication)

### [FUTURE] TICKET-206: Message Editing and Deletion
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Backend, Frontend

**Description:**
Allow users to edit and delete their own messages (with optional time limits).

**Acceptance Criteria:**
- [ ] Add edited flag and editedAt timestamp to ChatMessage
- [ ] Add PUT /api/messages/{id} endpoint for editing
- [ ] Add DELETE /api/messages/{id} endpoint (soft delete or hard delete)
- [ ] Validate user owns the message
- [ ] Implement edit time window (e.g., 5 minutes)
- [ ] Broadcast message updates via WebSocket
- [ ] Frontend: Add edit button to user's own messages
- [ ] Frontend: Add delete button to user's own messages
- [ ] Frontend: Show "edited" indicator on edited messages
- [ ] Frontend: Show "deleted" placeholder for deleted messages
- [ ] Test editing and deletion flow

**Estimated Effort:** 3-4 days

**Dependencies:** TICKET-102 (Authentication)

### [FUTURE] TICKET-207: Voice Channels (Stretch Goal)
**Status:** [FUTURE] Future  
**Priority:** P3  
**Component:** Backend, Frontend, WebRTC

**Description:**
Add voice chat capability using WebRTC for real-time audio communication.

**Acceptance Criteria:**
- [ ] Research WebRTC integration options
- [ ] Set up signaling server for WebRTC
- [ ] Create VoiceChannel entity
- [ ] Implement voice channel joining/leaving
- [ ] Add audio streaming with WebRTC
- [ ] Frontend: Add voice controls (mute, deafen)
- [ ] Frontend: Display active speakers
- [ ] Implement push-to-talk option
- [ ] Handle network quality and reconnection
- [ ] Test voice quality with multiple participants

**Estimated Effort:** 10-15 days (complex feature)

**Dependencies:** TICKET-101 (Channels)

---

## Phase 4: Production Ready - [FUTURE] FUTURE

### [FUTURE] TICKET-301: PostgreSQL/MySQL Database Migration
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, Database

**Description:**
Replace H2 in-memory database with PostgreSQL or MySQL for production persistence.

**Acceptance Criteria:**
- [ ] Add PostgreSQL/MySQL driver dependency
- [ ] Create production database schema
- [ ] Update application.properties with production database config
- [ ] Add Flyway or Liquibase for database migrations
- [ ] Create migration scripts for initial schema
- [ ] Configure connection pooling (HikariCP)
- [ ] Test database connectivity and performance
- [ ] Document database setup instructions
- [ ] Add database backup strategy

**Estimated Effort:** 2-3 days

**Dependencies:** None

### [FUTURE] TICKET-302: Advanced Docker Configuration
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** DevOps, Docker

**Description:**
Enhance Docker setup with multi-stage builds, health checks, and volume management.

**Acceptance Criteria:**
- [ ] Implement multi-stage Docker builds
- [ ] Add health check endpoints to backend
- [ ] Configure Docker health checks
- [ ] Set up persistent volumes for database
- [ ] Create production-ready docker-compose.yml
- [ ] Add environment-specific configurations (dev, staging, prod)
- [ ] Configure resource limits (memory, CPU)
- [ ] Add container orchestration (Docker Swarm or Kubernetes configs)
- [ ] Document deployment procedures
- [ ] Test deployment to cloud environment

**Estimated Effort:** 3-4 days

**Dependencies:** TICKET-301 (Production Database)

### [FUTURE] TICKET-303: User Registration with Email
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, Frontend, Email

**Description:**
Implement full user registration flow with email verification.

**Acceptance Criteria:**
- [ ] Add email field to User entity
- [ ] Add email validation and uniqueness constraint
- [ ] Integrate email service (SMTP or SendGrid)
- [ ] Create email verification token system
- [ ] Add registration endpoint with email verification
- [ ] Send verification email on registration
- [ ] Add email verification endpoint
- [ ] Frontend: Create registration form
- [ ] Frontend: Add email verification success page
- [ ] Add resend verification email option
- [ ] Test registration and verification flow

**Estimated Effort:** 3-4 days

**Dependencies:** TICKET-102 (Authentication)

### [FUTURE] TICKET-304: Password Reset Functionality
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, Frontend, Email

**Description:**
Implement secure password reset flow with email-based token verification.

**Acceptance Criteria:**
- [ ] Create password reset token system
- [ ] Add "Forgot Password" endpoint
- [ ] Send password reset email with token
- [ ] Add token verification and password reset endpoint
- [ ] Implement token expiration (e.g., 1 hour)
- [ ] Frontend: Add "Forgot Password" link on login
- [ ] Frontend: Create password reset form
- [ ] Frontend: Show success/error messages
- [ ] Hash new passwords securely
- [ ] Test password reset flow

**Estimated Effort:** 2-3 days

**Dependencies:** TICKET-303 (Email Registration)

### [FUTURE] TICKET-305: Rate Limiting and Security Hardening
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, Security

**Description:**
Implement rate limiting and additional security measures to prevent abuse and attacks.

**Acceptance Criteria:**
- [ ] Add rate limiting library (e.g., Bucket4j)
- [ ] Implement rate limits on API endpoints (e.g., 100 req/min)
- [ ] Add rate limiting on message sending (e.g., 10 msg/min)
- [ ] Implement IP-based rate limiting
- [ ] Add CAPTCHA for registration (optional)
- [ ] Configure security headers (CSP, HSTS, X-Frame-Options)
- [ ] Add input sanitization to prevent XSS
- [ ] Implement SQL injection prevention (parameterized queries)
- [ ] Add brute force protection for login
- [ ] Configure CORS for specific domains only
- [ ] Test rate limiting and security measures

**Estimated Effort:** 4-5 days

**Dependencies:** TICKET-302 (Production Deployment)

### [FUTURE] TICKET-306: Mobile Support (Android/iOS)
**Status:** [FUTURE] Future  
**Priority:** P2  
**Component:** Frontend, Mobile

**Description:**
Add Android and iOS support using LibGDX's cross-platform capabilities.

**Acceptance Criteria:**
- [ ] Set up LibGDX Android project
- [ ] Set up LibGDX iOS project (RoboVM or MOE)
- [ ] Adapt UI for mobile screens (responsive layout)
- [ ] Implement touch controls and gestures
- [ ] Add mobile-specific features (push notifications)
- [ ] Configure mobile app permissions
- [ ] Build and test on Android device
- [ ] Build and test on iOS device
- [ ] Optimize performance for mobile
- [ ] Publish to app stores (optional)

**Estimated Effort:** 7-10 days

**Dependencies:** None (can be done in parallel)

### [FUTURE] TICKET-307: Comprehensive Logging and Monitoring
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, DevOps

**Description:**
Implement comprehensive logging and monitoring for production observability.

**Acceptance Criteria:**
- [ ] Configure structured logging (JSON format)
- [ ] Add request/response logging with correlation IDs
- [ ] Implement error tracking (e.g., Sentry)
- [ ] Add application metrics (Micrometer/Prometheus)
- [ ] Create monitoring dashboards (Grafana)
- [ ] Add alerting for critical errors
- [ ] Log security events (auth failures, suspicious activity)
- [ ] Implement log aggregation (ELK stack or similar)
- [ ] Add performance monitoring (APM)
- [ ] Document logging strategy

**Estimated Effort:** 4-5 days

**Dependencies:** TICKET-302 (Production Deployment)

### [FUTURE] TICKET-308: Backup and Restore Functionality
**Status:** [FUTURE] Future  
**Priority:** P1  
**Component:** Backend, Database, DevOps

**Description:**
Implement automated backup strategy and restore procedures for data protection.

**Acceptance Criteria:**
- [ ] Create database backup scripts
- [ ] Implement automated daily backups
- [ ] Store backups securely (encrypted, off-site)
- [ ] Implement backup retention policy (e.g., 30 days)
- [ ] Create restore procedure documentation
- [ ] Add backup verification process
- [ ] Implement file storage backups (user uploads)
- [ ] Test full restore from backup
- [ ] Add backup monitoring and alerts
- [ ] Document disaster recovery plan

**Estimated Effort:** 3-4 days

**Dependencies:** TICKET-301 (Production Database)

---

## Quick Reference

### Priority Levels
- **P0**: Critical - MVP requirements
- **P1**: High - Core features needed for production
- **P2**: Medium - Important but not critical
- **P3**: Low - Nice to have / stretch goals

### Component Categories
- **Backend**: Spring Boot server-side code
- **Frontend**: LibGDX desktop client and web UI
- **Database**: Data persistence and schema
- **Security**: Authentication, authorization, and security measures
- **DevOps**: Deployment, monitoring, and operations
- **Infrastructure**: Project setup and build configuration
- **Documentation**: User and developer documentation

### Estimation Guidelines
- 1-2 days: Small feature or enhancement
- 3-5 days: Medium feature
- 5-7 days: Large feature
- 7+ days: Complex/multi-component feature

---

## Notes

- This document is a living document and will be updated as features are completed or priorities change.
- Each ticket should be reviewed and refined before implementation begins.
- Tickets may be broken down into smaller sub-tasks during implementation.
- See [MVP.md](MVP.md) for detailed architecture and technical documentation.
- Dependencies between tickets should be respected when planning sprints.

---

**Last Updated:** 2026-02-13  
**Document Version:** 1.0
