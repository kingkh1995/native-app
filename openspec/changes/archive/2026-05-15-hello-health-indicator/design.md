## Context

Spring Boot 提供了 `DataSourceHealthIndicator` 作为内置的健康检查实现，但本项目需要自定义 HealthIndicator 来演示扩展模式。使用 H2 内存数据库作为演示数据库，零外部依赖即可启动。

## Goals / Non-Goals

**Goals:**
- 添加 H2 内存数据库，启动即用，无需额外配置
- 创建自定义 `DatabaseHealthIndicator`，执行 `SELECT 1` 检查数据库连通性
- 通过 `/actuator/health` 暴露健康状态

**Non-Goals:**
- 不包含数据库持久化（H2 纯内存模式，重启后数据丢失）
- 不作为业务数据存储使用
- 不对接生产数据库（如 MySQL、PostgreSQL）

## Decisions

### Decision 1: 数据源配置方式

Spring Boot 没有 H2 的自动配置（H2 通常通过 `spring-boot-starter-data-jpa` 或 `spring.datasource.*` 属性自动配置），因此需要手动创建 `DataSource` Bean。

**选型：** 使用 `spring.datasource.*` 属性 + `spring-boot-starter-jdbc`，Spring Boot 会自动创建 `DataSource` Bean。

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:healthdb
    driver-class-name: org.h2.Driver
```

### Decision 2: HealthIndicator 实现方式

**方案对比：**
| 方案 | 说明 |
|------|------|
| 使用 `DataSourceHealthIndicator` | Spring Boot 内置，自动注册 |
| 自定义实现 `HealthIndicator` | 手动控制，可自定义返回数据 |

**选型：** 自定义实现 `HealthIndicator`。原因：
- 演示扩展模式
- 可以自定义返回数据（如数据库版本）
- 依赖注入 `DataSource`，使用 `Statement` 执行 `SELECT 1` 验证

### Decision 3: Spring Boot 4.0.6 模块化变更

**发现：** Spring Boot 4.0.6 中，健康检查 API 从 `spring-boot-starter-actuator` 抽离到了独立的 `spring-boot-health` 模块。包名也发生了变更：

| | Spring Boot 3.x | Spring Boot 4.0.6 |
|---|---|---|
| 模块 | `spring-boot-starter-actuator` | `spring-boot-health`（新增） |
| HealthIndicator | `org.springframework.boot.actuate.health.HealthIndicator` | `org.springframework.boot.health.contributor.HealthIndicator` |
| Health | `org.springframework.boot.actuate.health.Health` | `org.springframework.boot.health.contributor.Health` |

### Decision 4: Native-image 兼容性

H2 JDBC 驱动需要配置反射。通过 Spring Boot 的 GraalVM native 支持，JDBC 驱动通常会自动注册。如果构建失败，需要添加 `@TypeHint` 或 `reachability-metadata.json`。
