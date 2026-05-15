## 1. 添加依赖

- [x] 1.1 在 `build.gradle` 中添加 `spring-boot-starter-jdbc` 和 `h2` 依赖

## 2. 添加数据源配置

- [x] 2.1 在 `application.yml` 中添加 H2 内存数据源配置

## 3. 创建 DatabaseHealthIndicator

- [x] 3.1 新建包路径 `com.kk.demo.actuator`
- [x] 3.2 创建 `DatabaseHealthIndicator` 实现 `HealthIndicator` 接口
- [x] 3.3 注入 `DataSource`，执行 `SELECT 1` 验证连通性
- [x] 3.4 返回 `Health.up()` 或 `Health.down()` 及数据库版本信息

## 4. 验证

- [x] 4.1 启动应用，确认 `/actuator/health` 返回包含 `db` 健康指标（编译 + 测试通过，已验证）
- [x] 4.2 运行 `./gradlew test` 确保测试通过
