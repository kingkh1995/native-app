# db-health Specification

## Purpose
TBD - created by archiving change hello-health-indicator. Update Purpose after archive.
## Requirements
### Requirement: Database Connectivity

The system SHALL validate that the H2 in-memory database is reachable via a `SELECT 1` probe.

#### Scenario: 数据库可正常连接

- **WHEN** H2 数据源配置正确且数据库已初始化
- **THEN** 健康检查状态为 `UP`
- **AND** 返回数据中包含 H2 数据库版本号

#### Scenario: 数据库连接失败

- **WHEN** 数据库不可用或连接超时
- **THEN** 健康检查状态为 `DOWN`
- **AND** 返回数据中包含失败原因

