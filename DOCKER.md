# Docker Deployment Guide

This guide covers deploying the Accord Chat backend using Docker and Docker Compose.

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Start the backend
docker compose up -d

# View logs
docker compose logs -f backend

# Stop the service
docker compose down
```

The backend will be available at `http://localhost:8080`.

## Docker Compose Configuration

The `compose.yml` file provides a complete configuration for running the backend:

- **Port Mapping**: Backend accessible on `http://localhost:8080`
- **Health Checks**: Automatic health monitoring
- **Environment Variables**: Configurable via environment section
- **Restart Policy**: Automatically restarts unless stopped manually
- **Network**: Isolated bridge network for future service expansion

## Manual Docker Commands

### Building the Image

```bash
docker build -t accord-backend:latest .
```

### Running the Container

```bash
docker run -d \
  -p 8080:8080 \
  --name accord-backend \
  accord-backend:latest
```

### Viewing Logs

```bash
# Follow logs
docker logs -f accord-backend

# View last 100 lines
docker logs --tail 100 accord-backend
```

### Stopping and Removing

```bash
# Stop the container
docker stop accord-backend

# Remove the container
docker rm accord-backend

# Remove the image
docker rmi accord-backend:latest
```

## Environment Variables

Configure the application using environment variables:

### CORS Configuration

```bash
docker run -d \
  -p 8080:8080 \
  -e APP_CORS_ALLOWED_ORIGINS="https://yourdomain.com,https://app.yourdomain.com" \
  --name accord-backend \
  accord-backend:latest
```

### Validation Configuration

```bash
docker run -d \
  -p 8080:8080 \
  -e APP_USERNAME_MIN_LENGTH=5 \
  -e APP_USERNAME_MAX_LENGTH=30 \
  -e APP_MESSAGE_MAX_LENGTH=500 \
  --name accord-backend \
  accord-backend:latest
```

### All Available Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8080` | Server port |
| `APP_CORS_ALLOWED_ORIGINS` | `*` | Allowed CORS origins (use specific domains in production) |
| `APP_USERNAME_MIN_LENGTH` | `3` | Minimum username length |
| `APP_USERNAME_MAX_LENGTH` | `50` | Maximum username length |
| `APP_MESSAGE_MAX_LENGTH` | `1000` | Maximum message content length |
| `SPRING_JPA_SHOW_SQL` | `false` | Show SQL queries in logs |
| `SPRING_H2_CONSOLE_ENABLED` | `true` | Enable H2 console |

## Health Checks

The container includes automatic health checks that ping the `/api/messages` endpoint every 30 seconds. The container is considered healthy when this endpoint responds successfully.

Check health status:

```bash
docker inspect --format='{{.State.Health.Status}}' accord-backend
```

## Production Deployment

### Security Recommendations

1. **Set Specific CORS Origins**:
   ```yaml
   environment:
     - APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com
   ```

2. **Use Secrets for Sensitive Data**: For production databases, use Docker secrets or environment files.

3. **Run Behind Reverse Proxy**: Use Nginx or Traefik for SSL termination and load balancing.

4. **Resource Limits**: Add resource constraints to prevent resource exhaustion:
   ```yaml
   services:
     backend:
       deploy:
         resources:
           limits:
             cpus: '1.0'
             memory: 512M
           reservations:
             cpus: '0.5'
             memory: 256M
   ```

### Using with Persistent Database

For production, replace H2 with PostgreSQL or MySQL:

```yaml
services:
  backend:
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/accorddb
      - SPRING_DATASOURCE_USERNAME=accord
      - SPRING_DATASOURCE_PASSWORD=secure_password
      - SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
    depends_on:
      - postgres
  
  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=accorddb
      - POSTGRES_USER=accord
      - POSTGRES_PASSWORD=secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## Networking

### Connecting Frontend to Dockerized Backend

When running the frontend on the host machine and backend in Docker:

```bash
# Frontend connects to backend
./gradlew desktop:run -Daccord.websocket.url=ws://localhost:8080/ws
```

### Running Multiple Services

The compose configuration uses a bridge network (`accord-network`) that allows for future service expansion:

```yaml
services:
  backend:
    networks:
      - accord-network
  
  # Add more services here
  # frontend-web:
  #   networks:
  #     - accord-network
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker compose logs backend

# Check container status
docker compose ps
```

### Port Already in Use

```bash
# Change port mapping in compose.yml
ports:
  - "8081:8080"  # Map to 8081 on host
```

### Health Check Failing

```bash
# Increase start period if application takes longer to start
healthcheck:
  start_period: 60s
```

### Build Issues

```bash
# Clean build
docker compose build --no-cache

# Build with verbose output
docker compose build --progress=plain
```

## Development Workflow

### Live Development with Docker

For development, mount the source code as a volume:

```bash
# Build and run with hot reload (requires spring-boot-devtools)
docker run -d \
  -p 8080:8080 \
  -v $(pwd)/backend/src:/app/src \
  --name accord-backend-dev \
  accord-backend:latest
```

### Debugging

Run the container with debug port exposed:

```bash
docker run -d \
  -p 8080:8080 \
  -p 5005:5005 \
  -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  --name accord-backend \
  accord-backend:latest
```

Connect your IDE debugger to `localhost:5005`.

## Image Optimization

The Dockerfile uses multi-stage builds to create a minimal runtime image:

- **Build Stage**: Uses `maven:3.9-eclipse-temurin-17-alpine` (~400MB)
- **Runtime Stage**: Uses `eclipse-temurin:17-jre-alpine` (~170MB)
- **Final Image Size**: ~250MB (includes application)

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Build Docker Image
  run: docker build -t accord-backend:${{ github.sha }} .

- name: Push to Registry
  run: |
    docker tag accord-backend:${{ github.sha }} ghcr.io/username/accord-backend:latest
    docker push ghcr.io/username/accord-backend:latest
```

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
