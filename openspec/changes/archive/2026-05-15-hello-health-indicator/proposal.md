## Why

项目目前缺少对数据库可用性的健康检查。添加一个基于 H2 内存数据库的自定义 HealthIndicator，可以验证数据库连接是否正常，为线上故障排查提供关键信号。同时演示 Spring Boot 扩展健康检查的常见模式。

## What Changes

- 在 `build.gradle` 中添加 H2 运行时依赖和 `spring-boot-starter-jdbc` 依赖
- 在 `application.yml` 中配置 H2 内存数据源
- 新建 `DatabaseHealthIndicator` 类，实现 `HealthIndicator` 接口，通过执行 `SELECT 1` 探测数据库连通性

## Capabilities

### New Capabilities
- `db-health`: 自定义数据库健康指标，验证 H2 数据库连接可用性

### Modified Capabilities

（无）

## Impact

- `build.gradle`：添加 `spring-boot-starter-jdbc` 和 `h2` 依赖
- `application.yml`：添加 H2 数据源配置
- `src/main/java/com/kk/demo/actuator/DatabaseHealthIndicator.java`（新建）
