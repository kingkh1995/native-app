## ADDED Requirements

### Requirement: MySQL DataSource configured in application.yml

The `application.yml` SHALL configure a MySQL DataSource as the default, with driver class, JDBC URL, username, and password. These values SHALL be overridable by higher-priority mechanisms:

- Dev (`bootRun`, `dev` profile): `application.yml` base config with `localhost:3306` (Docker port mapping)
- Test: `@ServiceConnection` from TestContainers (overrides DataSource entirely)
- Production (native Docker, `default` profile): `application-default.yml` overrides with container name `mysql:3306`

#### Scenario: Default configuration loads on startup

- **WHEN** the application starts with `dev` profile active
- **THEN** the DataSource SHALL connect to `jdbc:mysql://localhost:3306/native-app` with user `root` and password `123456`

#### Scenario: Dev override via spring-boot-docker-compose

- **WHEN** the application starts via `./gradlew bootRun`
- **THEN** the DataSource connection info SHALL be provided by the running MySQL container from `compose.yaml`

#### Scenario: Production override via application-default.yml

- **WHEN** the application starts with no active profile (native Docker deployment)
- **THEN** `application-default.yml` SHALL override the DataSource URL to `jdbc:mysql://mysql:3306/native-app`
- **AND** all OTLP endpoints SHALL be overridden to use `otel-collector:4318`

---

### Requirement: Health indicator uses MySQL

The Actuator health endpoint SHALL report MySQL DataSource connectivity instead of the previous H2 in-memory database.

#### Scenario: Health check with MySQL reachable

- **WHEN** the application is running and MySQL is reachable
- **THEN** `GET /actuator/health` SHALL return `"status": "UP"` with `"datasource"` component showing `"status": "UP"`

#### Scenario: Health check with MySQL unreachable

- **WHEN** the application is running but MySQL is not reachable
- **THEN** `GET /actuator/health` SHALL return `"status": "DOWN"`

#### Scenario: UP test with TestContainers MySQL

- **WHEN** `DatabaseHealthIndicator.health()` is invoked with TestContainers-managed MySQL
- **THEN** status SHALL be `UP`
- **AND** details SHALL contain `"database": "MySQL"`

#### Scenario: DOWN test with mocked JDBC failure

- **WHEN** `DatabaseHealthIndicator.health()` is invoked with a `JdbcTemplate` that throws `DataAccessException`
- **THEN** status SHALL be `DOWN`
- **AND** details SHALL contain `"error"` key with the exception message

---

### Requirement: Dev environment MySQL lifecycle managed by Docker Compose

The `compose.yaml` SHALL include a MySQL 8.4 LTS service definition. The `spring-boot-docker-compose` dependency (`developmentOnly`) SHALL auto-start this service during `bootRun`.

#### Scenario: bootRun auto-starts MySQL

- **WHEN** running `./gradlew bootRun`
- **THEN** a MySQL 8.4 LTS container SHALL be started automatically, with a health check ensuring it is ready before the application connects

#### Scenario: MySQL container has persisted data

- **WHEN** the MySQL container restarts
- **THEN** data SHALL persist via a named Docker volume

---

### Requirement: Test environment MySQL lifecycle managed by TestContainers

The test infrastructure SHALL use `MySQLContainer` to start a MySQL 8.4 LTS instance per test configuration. The container connection info SHALL be injected into the Spring context via `@ServiceConnection`.

#### Scenario: Test MySQL starts before test execution

- **WHEN** a `@SpringBootTest` with `TestMySqlConfiguration` starts
- **THEN** a MySQL 8.4 LTS container SHALL start on a random available port before the Spring context initializes

#### Scenario: Test MySQL uses isolated container

- **WHEN** a test completes
- **THEN** the MySQL container SHALL be stopped and destroyed
- **AND** no data SHALL persist between test runs

#### Scenario: Test can perform JDBC operations

- **WHEN** a test creates tables and inserts data via `JdbcTemplate`
- **THEN** the operations SHALL succeed against the TestContainers-managed MySQL

---

### Requirement: Database driver compatible with GraalVM native-image

The `mysql-connector-j` dependency SHALL be compatible with GraalVM native-image builds. The build SHALL NOT require manual reflection configuration for JDBC driver registration.

#### Scenario: Native image builds with MySQL driver

- **WHEN** running `./gradlew bootBuildImage`
- **THEN** the resulting native image SHALL include the MySQL JDBC driver
- **AND** `DataSource` SHALL initialize successfully at runtime

---

### Requirement: H2 dependency removed

The H2 database dependency SHALL be removed from `build.gradle`. No runtime code SHALL reference H2 driver or connection.

#### Scenario: No H2 on classpath

- **WHEN** inspecting runtime dependencies via `actuator/conditions`
- **THEN** `spring.datasource` SHALL show `com.mysql.cj.jdbc.Driver` as the driver, not H2

#### Scenario: H2 configuration absent

- **WHEN** searching the project for `h2database` or `org.h2.Driver`
- **THEN** no references SHALL be found in `build.gradle` or any `application-*.yml`
