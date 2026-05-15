## 1. Update build.gradle Dependencies

- [x] 1.1 Remove `runtimeOnly 'com.h2database:h2'`
- [x] 1.2 Add `runtimeOnly 'com.mysql:mysql-connector-j'`
- [x] 1.3 Add `testImplementation 'org.springframework.boot:spring-boot-testcontainers'`
- [x] 1.4 Add `testImplementation 'org.testcontainers:mysql'`
- [x] 1.5 Add `testImplementation 'org.testcontainers:junit-jupiter'`

## 2. Add MySQL Service to compose.yaml

- [x] 2.1 Add `mysql:8.0` service definition with environment variables (root password, database, user, password)
- [x] 2.2 Add `healthcheck` using `mysqladmin ping`
- [x] 2.3 Add named volume `mysql-data` for data persistence
- [x] 2.4 Map port `3306:3306`

## 3. Update DataSource Configuration

- [x] 3.1 Change `application.yml` datasource driver to `com.mysql.cj.jdbc.Driver`
- [x] 3.2 Change `application.yml` datasource URL to `jdbc:mysql://localhost:3306/demo`
- [x] 3.3 Add `username: demo` and `password: demopass` to datasource config
- [x] 3.4 Remove any H2-specific configuration references

## 4. Create TestContainers Test Configuration

- [x] 4.1 Create `src/test/java/com/kk/demo/TestMySqlConfiguration.java` with `@TestConfiguration`
- [x] 4.2 Define `MySQLContainer<?> mysql()` bean annotated with `@ServiceConnection`
- [x] 4.3 Set MySQL Docker image tag to `mysql:8.0`

## 5. Create DatabaseHealthIndicator Tests

- [x] 5.1 Create `DatabaseHealthIndicatorUpTest.java` — test health UP with TestContainers MySQL
- [x] 5.2 Create `DatabaseHealthIndicatorDownTest.java` — test health DOWN with mocked JdbcTemplate

## 6. Verify Local Development (bootRun)

> **Requires:** Docker daemon running locally, no other process on port 3306

- [x] 6.1 Run `./gradlew bootRun` and confirm MySQL container auto-starts via docker-compose
- [x] 6.2 Verify `GET /actuator/health` returns `"status": "UP"` with datasource component

## 7. Verify Tests

> **Requires:** Docker daemon running locally (TestContainers needs it)

- [x] 7.1 Run `./gradlew test` and confirm tests pass with TestContainers MySQL
- [x] 7.2 Run `DemoApplicationTests` contextLoads — confirm context initializes with MySQL
- [x] 7.3 Run `DatabaseHealthIndicatorUpTest` — verify health UP with MySQL
- [x] 7.4 Run `DatabaseHealthIndicatorDownTest` — verify health DOWN with mocked JDBC

## 8. Verify Native Image Build

> **Requires:** Docker daemon running locally, MYSQL accessible for runtime verification

- [x] 8.1 Run `./gradlew bootBuildImage` and confirm build succeeds with mysql-connector-j
- [x] 8.2 Run native container and confirm MySQL connection via health check
