## Context

当前的 `DatabaseHealthIndicator` 使用原生 JDBC 执行健康检查。`spring-boot-starter-jdbc` 已经提供了 `JdbcTemplate`，且该依赖已在上一轮变更中添加。本次变更是对已存在功能的内部实现优化。

## Goals / Non-Goals

**Goals:**
- 将数据库健康检查从原生 JDBC 改为 JdbcTemplate
- 减少样板代码（try-with-resources、手动关闭连接）
- 保持相同的健康检查行为（SELECT 1 探测，返回 UP/DOWN）
- 保持相同的返回数据（数据库产品名、版本号）

**Non-Goals:**
- 不改变健康检查的外部行为
- 不修改测试或其他文件

## Decisions

### Decision 1：JdbcTemplate 注入替代 DataSource

**方案对比：**

| 方案 | 说明 |
|------|------|
| 注入 `DataSource` + 原生 JDBC（当前） | 需要手动管理 `Connection`、`Statement` 资源 |
| 注入 `JdbcTemplate` | 自动管理连接，`queryForObject` 简洁 |

**选择：** 注入 `JdbcTemplate`。

- `JdbcTemplate` 由 `spring-boot-starter-jdbc` 自动配置
- `queryForObject("SELECT 1", Integer.class)` 一行完成探测
- 异常统一由 `DataAccessException` 层次表示

### Decision 2：数据库元数据获取

原生 JDBC 使用 `DatabaseMetaData` 获取产品名和版本号。JdbcTemplate 不直接提供此功能，需要通过 `DataSource` 获取。

**方案对比：**

| 方案 | 说明 |
|------|------|
| 注入 `DataSource` + JdbcTemplate | 同时注入两个，增加复杂度 |
| 从 JdbcTemplate.getDataSource() 获取 | JdbcTemplate 持有 DataSource 引用，可直接获取 |

**选择：** `jdbcTemplate.getDataSource().getConnection()` 仅在元数据获取时使用，并通过 JDBC `DatabaseMetaData` 获取产品名和版本。这是合理的最小侵入方式。

```java
// 健康检查主流程使用 JdbcTemplate
jdbcTemplate.queryForObject("SELECT 1", Integer.class);

// 元数据通过 DataSource 获取（JdbcTemplate 内部持有）
try (var connection = jdbcTemplate.getDataSource().getConnection()) {
    var metaData = connection.getMetaData();
    // ...
}
```

## Risks / Trade-offs

- [Low] `jdbcTemplate.getDataSource()` 可能返回 null：Spring Boot 自动配置的 `JdbcTemplate` 始终会关联 `DataSource`
- [Low] 元数据获取那一小段仍然是原生 JDBC：这是必要的折中，因为 JdbcTemplate 不提供元数据 API
