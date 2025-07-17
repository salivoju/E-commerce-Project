# Discovery Server (Eureka)

The Discovery Server is a **Netflix Eureka Server** that provides service registration and discovery capabilities for the E-commerce microservices platform.

## 🎯 **Purpose**

The Discovery Server acts as a centralized registry where all microservices:
- **Register themselves** upon startup
- **Discover other services** they need to communicate with
- **Monitor service health** and availability
- **Enable load balancing** across multiple service instances

## 🏗️ **Architecture Role**

```
┌─────────────────────────────────────────────────────────────┐
│                    Discovery Server                         │
│                     (Port: 8761)                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │             Service Registry                        │    │
│  │                                                     │    │ 
│  │  • user-service (8081)                              │    │
│  │  • product-catalog-service (8090)                   │    │
│  │  • order-service (8092)                             │    │
│  │  • cart-service (8093)                              │    │
│  │  • inventory-service (8094)                         │    │
│  │  • api-gateway (8080)                               │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 **Quick Start**

### Local Development

```bash
# Navigate to discovery-server directory
cd discovery-server

# Run the application
./mvnw spring-boot:run

# Or run with Maven
mvn spring-boot:run
```

### Docker Deployment

```bash
# Build Docker image
docker build -t discovery-server:latest .

# Run container
docker run -p 8761:8761 discovery-server:latest

# Or use docker-compose (recommended)
docker-compose up discovery-server
```

## 🔧 **Configuration**

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8761` | Server port (standard Eureka port) |
| `eureka.client.register-with-eureka` | `false` | Server doesn't register with itself |
| `eureka.client.fetch-registry` | `false` | Server doesn't fetch registry from itself |
| `eureka.server.enable-self-preservation` | `false` (dev), `true` (prod) | Self-preservation mode |

### Profiles

- **`default`**: Local development with detailed logging
- **`docker`**: Production-ready settings for containers

## 📊 **Monitoring & Health**

### Health Check Endpoints

- **Health**: `http://localhost:8761/actuator/health`
- **Info**: `http://localhost:8761/actuator/info`
- **Metrics**: `http://localhost:8761/actuator/metrics`
- **Prometheus**: `http://localhost:8761/actuator/prometheus`

### Eureka Dashboard

Access the Eureka dashboard at: `http://localhost:8761/`

The dashboard shows:
- Registered services and their instances
- Service health status
- Registry statistics
- Instance availability

## 🔗 **Service Registration**

### How Services Register

Services register by including these dependencies and configuration:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Service Discovery

Services discover each other using:

```java
@Autowired
private DiscoveryClient discoveryClient;

// Get service instances
List<ServiceInstance> instances = discoveryClient.getInstances("product-catalog-service");
```

## 🐳 **Docker Configuration**

### Dockerfile Features

- **Multi-stage build** for optimized image size
- **Non-root user** for security
- **Health checks** for container orchestration
- **Proper JVM tuning** for containers

### Docker Compose Integration

```yaml
discovery-server:
  build:
    context: ./discovery-server
    dockerfile: Dockerfile
  container_name: discovery-server
  ports:
    - "8761:8761"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 60s
```

## 🛠️ **Development**

### Project Structure

```
discovery-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecommerce/discovery_server/
│   │   │       └── DiscoveryServerApplication.java
│   │   └── resources/
│   │       └── application.yml
├── Dockerfile
├── pom.xml
└── README.md
```

### Key Dependencies

- **Spring Boot Starter**: Core Spring Boot functionality
- **Eureka Server**: Netflix Eureka service discovery
- **Spring Boot Actuator**: Monitoring and health checks

## 🔍 **Troubleshooting**

### Common Issues

1. **Service registration failures**
    - Check network connectivity
    - Verify Eureka client configuration
    - Ensure Discovery Server is running

2. **Self-preservation mode**
    - Normal in development (disabled)
    - Prevents eviction of services during network issues

3. **Port conflicts**
    - Ensure port 8761 is available
    - Check for other Eureka instances

### Debug Logging

Enable debug logging:

```yaml
logging:
  level:
    com.netflix.eureka: DEBUG
    com.netflix.discovery: DEBUG
```

## 🚀 **Production Considerations**

### High Availability

For production, run multiple Eureka instances:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8761/eureka/
```

### Security

- Use HTTPS in production
- Implement proper authentication
- Network segmentation

### Monitoring

- Set up alerts for service registration/deregistration
- Monitor registry size and health
- Track response times

## 📝 **API Documentation**

### Eureka REST API

- `GET /eureka/apps` - All registered applications
- `GET /eureka/apps/{appName}` - Specific application instances

### Management Endpoints

- `GET /actuator/health` - Health status
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

---

## 🔄 **Related Services**

- **API Gateway**: Uses Discovery Server for service routing
- **All Microservices**: Register with Discovery Server
- **Load Balancers**: Use service registry for routing decisions

---

*This Discovery Server is a critical component of the E-commerce microservices platform. It should be the first service started in the system.*