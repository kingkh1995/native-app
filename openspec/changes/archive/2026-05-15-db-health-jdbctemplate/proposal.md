## Why

当前的 `DatabaseHealthIndicator` 使用原生 JDBC API（`Connection`、`Statement`）执行数据库健康检查，需要手动管理连接资源。改用 Spring 的 `JdbcTemplate` 可以简化代码、自动处理资源释放，并提供一致的异常层次。

## What Changes

- 将 `DatabaseHealthIndicator` 的依赖注入从 `DataSource` 改为 `JdbcTemplate`
- 将 `SELECT 1` 探测从原生 JDBC 改为 `JdbcTemplate.queryForObject`
- 移除 `java.sql.Connection`、`Statement`、`DatabaseMetaData` 等原生 JDBC 导入
- 不修改外部行为：返回的健康数据内容保持一致

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

（无 — 纯实现变更，外部行为不变）

## Impact

- `src/main/java/com/kk/demo/actuator/DatabaseHealthIndicator.java`：实现方式从原生 JDBC 改为 JdbcTemplate
