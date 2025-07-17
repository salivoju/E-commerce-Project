# User Service

The User Service is a **Spring Boot microservice** that handles user management and JWT-based authentication for the E-commerce platform.

## 🎯 **Purpose**

The User Service provides:
- **User Registration & Management**: Create, read, update user profiles
- **Authentication**: JWT-based login system with role claims
- **Authorization**: Role-based access control (USER, ADMIN)
- **Security**: Password encryption, input validation, secure endpoints
- **Profile Management**: Extended user profiles with contact information

## 🏗️ **Architecture Role**

```
┌─────────────────────────────────────────────────────────────┐
│                    User Service                             │
│                     (Port: 8081)                            │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Core Features                          │    │
│  │                                                     │    │
│  │  • User Registration (Public)                       │    │
│  │  • JWT Authentication with Role Claims              │    │
│  │  • Extended User Profile Management                 │    │
│  │  • Role-based Authorization (USER/ADMIN)            │    │
│  │  • Password Encryption (BCrypt)                     │    │
│  │  • Admin User Management                            │    │
│  │  • Account Status Management                        │    │
│  │  • Comprehensive Error Handling                     │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 **Quick Start**

### Prerequisites
- Docker and Docker Compose
- PostgreSQL database
- API Gateway service running

### Docker Deployment

```bash
# Build and start services
docker-compose up user-service

# Or rebuild after changes
docker-compose stop user-service
docker-compose rm -f user-service
docker rmi e-commerce-project-user-service:latest
docker-compose build user-service
docker-compose up -d user-service

# Check logs
docker logs user-service -f
```

## 🔧 **Configuration**

### Database Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/user_service_db` | Database URL |
| `spring.datasource.username` | `ecommerce_user` | Database username |
| `spring.datasource.password` | `ecommerce_pass` | Database password |

### JWT Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | `5367566B59...` | JWT signing secret (Base64 encoded) |
| `jwt.expiration` | `86400000` | Token expiration (24 hours in milliseconds) |

### Service Discovery

| Property | Default | Description |
|----------|---------|-------------|
| `eureka.client.service-url.defaultZone` | `http://discovery-server:8761/eureka/` | Eureka server URL |
| `eureka.instance.hostname` | `user-service` | Service hostname |

## 📊 **API Endpoints**

### Public Endpoints (No Authentication Required)

#### Health Check
```http
GET /api/v1/users/health
```
**Response:**
```json
{
  "status": "UP",
  "service": "user-service",
  "timestamp": "1752692416816"
}
```

#### User Registration
```http
POST /api/v1/users/register
Content-Type: application/json

{
  "name": "John Smith",
  "email": "john.smith@email.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "+1-555-0101"
}
```

#### User Authentication
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john.smith@email.com",
  "password": "password123"
}
```
**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9..."
}
```

#### Token Validation
```http
POST /api/v1/auth/validate
Authorization: Bearer <jwt-token>
```

#### Logout
```http
POST /api/v1/auth/logout
Authorization: Bearer <jwt-token>
```

### Protected Endpoints (Require Authentication)

> **Note:** All protected endpoints receive user information via API Gateway headers:
> - `X-Authenticated-User-Username`: User email from JWT
> - `X-Authenticated-User-Roles`: User roles from JWT

#### Get All Users (Admin Only)
```http
GET /api/v1/users
Authorization: Bearer <admin-jwt-token>
```

#### Get User by ID (Own Profile or Admin)
```http
GET /api/v1/users/{id}
Authorization: Bearer <jwt-token>
```

#### Update User Profile (Own Profile or Admin)
```http
PUT /api/v1/users/{id}
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Updated Name",
  "firstName": "Updated",
  "lastName": "Name",
  "phoneNumber": "+1-555-9999"
}
```

#### Update User Role (Admin Only)
```http
PUT /api/v1/users/{id}
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "role": "ROLE_ADMIN",
  "enabled": true
}
```

#### Create User (Admin Only)
```http
POST /api/v1/users
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "name": "New User",
  "email": "new.user@email.com",
  "password": "password123",
  "role": "ROLE_USER",
  "enabled": true
}
```

## 🔐 **Security Features**

### Authentication & Authorization
- **JWT-based Authentication**: Stateless token with embedded user claims
- **Role-based Access Control**: USER and ADMIN roles with different permissions
- **Password Encryption**: BCrypt with salt rounds
- **Token Claims**: Includes role, email, and name in JWT payload

### Permission Matrix

| Action | Regular User | Admin |
|--------|-------------|-------|
| Register | ✅ Public | ✅ Public |
| Login | ✅ Own account | ✅ Own account |
| View all users | ❌ Forbidden | ✅ Allowed |
| View own profile | ✅ Allowed | ✅ Allowed |
| View other profiles | ❌ Forbidden | ✅ Allowed |
| Update own profile | ✅ Allowed | ✅ Allowed |
| Update other profiles | ❌ Forbidden | ✅ Allowed |
| Update user roles | ❌ **Error Message** | ✅ Allowed |
| Update account status | ❌ **Error Message** | ✅ Allowed |
| Create users | ❌ Forbidden | ✅ Allowed |

### Input Validation & Security
- **Email Format Validation**: Proper email format checking
- **Password Requirements**: Minimum length requirements
- **Role Update Protection**: Non-admins get clear error messages
- **SQL Injection Prevention**: JPA parameterized queries
- **Sensitive Data Protection**: Passwords removed from responses

## 🗄️ **Database Schema**

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    is_account_non_expired BOOLEAN NOT NULL DEFAULT true,
    is_account_non_locked BOOLEAN NOT NULL DEFAULT true,
    is_credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    phone_number VARCHAR(20),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_user_email UNIQUE (email)
);
```

## 🛠️ **Development**

### Project Structure
```
user-service/
├── src/main/java/com/ecommerce/user_service/
│   ├── controller/
│   │   ├── AuthController.java          # Authentication endpoints
│   │   └── UserController.java          # User management endpoints
│   ├── model/
│   │   ├── User.java                    # User entity with validation
│   │   └── Role.java                    # Role enum (USER, ADMIN)
│   ├── dto/
│   │   ├── AuthRequest.java             # Login request DTO
│   │   └── JwtResponse.java             # JWT response DTO
│   ├── service/
│   │   └── JwtService.java              # JWT token management
│   ├── repository/
│   │   └── UserRepository.java          # JPA repository
│   ├── config/security/
│   │   ├── SecurityConfig.java          # Spring Security config
│   │   ├── UserInfoDetails.java         # UserDetails implementation
│   │   └── UserInfoService.java         # UserDetailsService implementation
│   └── UserServiceApplication.java      # Main application class
├── src/main/resources/
│   └── application.yml                  # Configuration
├── Dockerfile
├── pom.xml
└── README.md
```

### Key Dependencies
- **Spring Boot 3.x**: Latest Spring Boot with Jakarta EE
- **Spring Security 6.x**: Modern security framework
- **Spring Data JPA**: Database operations
- **Spring Cloud Eureka**: Service discovery
- **JWT Library**: Token generation and validation
- **PostgreSQL Driver**: Database connectivity
- **Lombok**: Code generation
- **Jakarta Validation**: Input validation

## 🧪 **Testing**

### Test Credentials
All test users use password: `password123`

| User | Email | Role | Status |
|------|-------|------|--------|
| John Smith | `john.smith@email.com` | USER | Enabled |
| Admin User | `admin@company.com` | ADMIN | Enabled |
| Jane Doe | `jane.doe@email.com` | USER | Enabled |
| Disabled User | `disabled@email.com` | USER | Disabled |

### Debug Commands
```bash
# Check service logs
docker logs user-service -f

# Check API Gateway logs
docker logs api-gateway -f

# Verify database connection
docker exec -it postgres-container psql -U ecommerce_user -d user_service_db

# List all users
SELECT id, email, role, is_enabled FROM users;
```

### Error Messages & Meanings

| Error | Meaning | Solution |
|-------|---------|----------|
| `401 Unauthorized` | Missing or invalid JWT token | Login again or check token format |
| `403 Forbidden` | Insufficient permissions | Check user role or endpoint requirements |
| `409 Conflict` | Email already exists | Use different email for registration |
| `"Only administrators can update user roles"` | Non-admin tried role update | ✅ **Working as intended** |

## 🐳 **Docker Configuration**

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Service port | `8081` |
| `DATABASE_URL` | PostgreSQL connection | Container network |
| `EUREKA_URL` | Discovery server URL | `http://discovery-server:8761/eureka/` |
| `JWT_SECRET` | Token signing key | Pre-configured for development |
| `JWT_EXPIRATION` | Token validity period | `86400000` (24 hours) |

### Docker Compose Integration
```yaml
user-service:
  build: ./user-service
  ports:
    - "8081:8081"
  depends_on:
    - postgres
    - discovery-server
  environment:
    - SPRING_PROFILES_ACTIVE=docker
```

## 🚀 **Production Considerations**

### Security Checklist
- [ ] Use strong JWT secrets (environment variables)
- [ ] Enable HTTPS in production
- [ ] Implement rate limiting at API Gateway
- [ ] Regular security audits
- [ ] Monitor authentication failure patterns
- [ ] Set up alerts for admin privilege escalation attempts

### Performance Optimization
- [ ] Database connection pooling
- [ ] JWT token caching strategy
- [ ] Database indexing on email column
- [ ] JVM heap size tuning for containers
- [ ] Health check optimization

### Monitoring & Alerting
- [ ] Track authentication success/failure rates
- [ ] Monitor JWT token expiration patterns
- [ ] Alert on suspicious admin activities
- [ ] Database performance metrics
- [ ] Service availability monitoring

## 📈 **Integration with Other Services**

### API Gateway Integration
- **JWT Validation**: Gateway validates tokens and forwards user info via headers
- **Route Protection**: Gateway applies authentication rules before forwarding
- **Header Injection**: `X-Authenticated-User-Username` and `X-Authenticated-User-Roles`

### Service Dependencies
- **Discovery Server**: For service registration and discovery
- **Database**: PostgreSQL for persistent user data
- **Future Services**: Order Service, Cart Service will use user authentication

---

## 🔄 **Recent Updates & Fixes**

### Fixed Issues ✅
- **Health Endpoint**: Now accessible without authentication
- **Role Update Security**: Non-admins receive clear error messages
- **Registration Validation**: Proper 400 error responses for invalid input
- **Admin Detection**: Enhanced debugging for role-based operations

### Latest Features
- **Extended User Profiles**: firstName, lastName, phoneNumber support
- **Account Status Management**: enabled, expired, locked flags
- **Comprehensive Error Handling**: Detailed error messages and logging
- **JWT Claims Enhancement**: Role, email, and name embedded in tokens

---

*The User Service serves as the authentication and authorization foundation for the entire E-commerce platform, providing secure user management with role-based access control.*