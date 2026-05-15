## 1. 修改 DatabaseHealthIndicator

- [x] 1.1 将依赖注入从 `DataSource` 改为 `JdbcTemplate`
- [x] 1.2 将 `SELECT 1` 探测从原生 JDBC 改为 `jdbcTemplate.queryForObject`
- [x] 1.3 元数据获取改为通过 `jdbcTemplate.getDataSource()` 获取连接
- [x] 1.4 移除 `java.sql.Statement`、`javax.sql.DataSource` 等不需要的导入

## 2. 验证

- [x] 2.1 编译通过
- [x] 2.2 运行 `./gradlew test --tests "com.kk.demo.DemoApplicationTests"` 确认测试通过
