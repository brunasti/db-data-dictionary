# dbdadi — DB Data Dictionary

A Spring Boot REST API for managing data dictionaries of multiple database models.

## Features

- Manage multiple **database models** (PostgreSQL, MySQL, Oracle, DB2, SQL Server, ...)
- Define **tables**, **columns**, and **relationships** for each model
- Full CRUD REST API with validation and error handling
- OpenAPI / Swagger UI included
- Configurable for multiple DBMS via Spring profiles

## Tech Stack

- Java 21
- Spring Boot 3.4
- Spring Data JPA / Hibernate
- Maven
- PostgreSQL (default), MySQL, Oracle, DB2, H2 supported

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL running locally (or use H2 for dev)

### Run with PostgreSQL (default)

1. Create the database and user:
```sql
CREATE USER dbdadi WITH PASSWORD 'dbdadi';
CREATE DATABASE dbdadi OWNER dbdadi;
```

2. Start the application:
```bash
mvn spring-boot:run
```

### Run with H2 (in-memory, no setup needed)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 console available at: http://localhost:8080/h2-console

### Switch to another database

Use the corresponding Spring profile:

| Database   | Profile    |
|------------|------------|
| PostgreSQL | `postgres` |
| MySQL      | `mysql`    |
| Oracle     | `oracle`   |
| DB2        | `db2`      |
| H2         | `h2`       |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

> **Note:** Oracle and DB2 drivers are not on Maven Central. Add the JDBC driver dependency to `pom.xml` manually before using those profiles.

## API Documentation

Once running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON spec:

```
http://localhost:8080/api-docs
```

## API Endpoints

| Method | Path                              | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | /api/v1/database-models           | List all database models           |
| POST   | /api/v1/database-models           | Create a database model            |
| GET    | /api/v1/database-models/{id}      | Get a database model               |
| PUT    | /api/v1/database-models/{id}      | Update a database model            |
| DELETE | /api/v1/database-models/{id}      | Delete a database model            |
| GET    | /api/v1/tables?databaseModelId=   | List tables (filter by model)      |
| POST   | /api/v1/tables                    | Create a table                     |
| GET    | /api/v1/tables/{id}               | Get a table                        |
| PUT    | /api/v1/tables/{id}               | Update a table                     |
| DELETE | /api/v1/tables/{id}               | Delete a table                     |
| GET    | /api/v1/columns?tableId=          | List columns (filter by table)     |
| POST   | /api/v1/columns                   | Create a column                    |
| GET    | /api/v1/columns/{id}              | Get a column                       |
| PUT    | /api/v1/columns/{id}              | Update a column                    |
| DELETE | /api/v1/columns/{id}              | Delete a column                    |
| GET    | /api/v1/relationships             | List all relationships             |
| POST   | /api/v1/relationships             | Create a relationship              |
| GET    | /api/v1/relationships/{id}        | Get a relationship                 |
| PUT    | /api/v1/relationships/{id}        | Update a relationship              |
| DELETE | /api/v1/relationships/{id}        | Delete a relationship              |

## License

Apache 2.0
