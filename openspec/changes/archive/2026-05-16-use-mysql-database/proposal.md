## Why

项目当前使用 H2 内存数据库作为 DataSource，仅用于通过 Actuator 健康检查。实际生产部署将使用 MySQL，但本地开发和测试阶段缺乏真实数据库验证，H2 与 MySQL 的 SQL 方言差异可能导致测试通过而生产失败。需要统一三个阶段（本地开发、测试、生产）的数据库为 MySQL，消除环境不一致带来的风险。

## What Changes

- **移除 H2 依赖**：删除 `runtimeOnly 'com.h2database:h2'`
- **新增 MySQL 依赖**：添加 `com.mysql:mysql-connector-j` 用于 JDBC 连接
- **新增 TestContainers 依赖**：`spring-boot-testcontainers`、`testcontainers:mysql`、`testcontainers:junit-jupiter`，用于测试阶段自动管理 MySQL 容器
- **compose.yaml 增加 MySQL service**：本地开发（bootRun）和 native 容器部署共用同一个 compose 中的 MySQL
- **测试配置**：创建 `@TestConfiguration` 配合 `@ServiceConnection` 管理测试用 MySQL 容器
- **更新 DataSource 配置**：`application.yml` 切换为 MySQL 驱动和默认连接信息
- **H2 相关代码清理**：移除 H2 相关的任何配置引用

## Capabilities

### New Capabilities
- `database-access`: MySQL 数据源的全环境统一管理——开发环境通过 Docker Compose 自动启动，测试环境通过 TestContainers 自动管理容器生命周期，生产环境通过环境变量注入连接信息

### Modified Capabilities
<!-- 无现有能力需要修改 -->

## Impact

| 影响范围 | 说明 |
|---|---|
| **build.gradle** | 删除 H2，新增 mysql-connector-j 和 TestContainers 测试依赖 |
| **compose.yaml** | 新增 mysql service（含健康检查、持久化 volume） |
| **application.yml** | DataSource 改为 MySQL 驱动和连接信息 |
| **application-dev.yml / application-default.yml** | 无需修改（datasource 统一由 application.yml 管理） |
| **测试代码** | 新增 `TestMySqlConfiguration.java`，使用 `@ServiceConnection` 注入 MySQL 容器 |
| **GraalVM native-image** | mysql-connector-j 需确认反射/JDBC 自动注册支持 |
| **Docker 部署流程** | 运行 native 容器前需确保 MySQL 已启动（compose.yaml 中已有） |
