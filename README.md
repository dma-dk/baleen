# Baleen

S-124 Navigational Warnings Management Platform implementing IHO S-100 series standards.

## Quick Start

### Local Development
Run `BaleenApplication.java` directly. Uses H2 in-memory database with spatial support.

- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:baleen`
- Username: `sa`
- Password: (empty)

### Docker

**H2 (default):**
```bash
docker build -t baleen .
docker run -p 8080:8080 baleen
```

**PostgreSQL with Docker Compose:**
```bash
export DB_PASSWORD=yourpassword
docker-compose up
```

**PostgreSQL standalone:**
```bash
docker run -p 8080:8080 \
  -e DATABASE_TYPE=postgresql \
  -e DATABASE_URL=jdbc:postgresql://your-db:5432/baleen \
  -e DATABASE_USERNAME=user \
  -e DATABASE_PASSWORD=pass \
  baleen
```

## Docker Configuration

### Database

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `DATABASE_TYPE` | `h2` | Database type (`h2` or `postgresql`) |
| `DATABASE_URL` | H2 in-memory | Database connection URL |
| `DATABASE_USERNAME` | `sa` | Database username |
| `DATABASE_PASSWORD` | (empty) | Database password |
| `DATABASE_DRIVER` | H2 driver | JDBC driver class |
| `DDL_AUTO` | `update` | Hibernate DDL mode |

## Build

```bash
mvn clean install
```

## Health Check

http://localhost:8080/actuator/health