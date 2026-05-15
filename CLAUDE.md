# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.6 脚手架项目，使用 GraalVM native-image 构建为 Docker 镜像运行。集成了基于 OpenTelemetry 的完整可观测性体系（指标、链路追踪、日志）。

- Java 25, Gradle 9.5.0, Spring Boot 4.0.6
- Base package: `com.kk.demo`
- Native image 通过 Paketo buildpacks 在 Docker 容器内构建，无需本地安装 GraalVM
- 基础镜像: `paketobuildpacks/builder-noble-java-tiny`（无 shell，体积小）
- 使用 Lombok（`@Slf4j`, `@RequiredArgsConstructor` 等），IDE 需启用 annotation processing

## Build Commands

```bash
# 构建 native image Docker 镜像（镜像名 native-app/demo:0.0.1-SNAPSHOT）
./gradlew bootBuildImage

# 运行测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.kk.demo.DemoApplicationTests"

# 本地开发运行（自动激活 dev profile）
./gradlew bootRun
```

## Running the Application

```bash
# 1. 本地开发（dev profile，本地日志路径为./logs/demo.log），无需手动启动可观测性基础设施（spring-boot-docker-compose组件会在bootRun运行时启动可观测性基础设施），
./gradlew clean bootRun

# 2. Docker native image（default profile）
# 2a. 手动启动可观测性基础设施（OTel Collector, Prometheus, Tempo, Loki, Grafana）
docker compose up -d
# 2b. 构建native镜像并发布到本地docker镜像仓库
./gradlew bootBuildImage
# 2c. 通过docker运行镜像（与 compose 服务同网络，通过容器名解析）
docker run --rm --user=root --network=native-app_default -p 8080:8080 native-app/demo:0.0.1-SNAPSHOT
```

dev 模式下 `spring-boot-docker-compose`（developmentOnly 依赖）会自动检测 `compose.yaml` 并连接可观测性服务，无需手动配置连接地址。

## Testing HTTP Endpoints

应用启动后，在 Windows 终端 (cmd) 下测试接口：

```cmd
:: 测试 Hello 接口
curl http://localhost:8080/hello

:: 健康检查
curl http://localhost:8080/actuator/health

:: 查看自定义指标
curl http://localhost:8080/actuator/metrics/hello.count
```

预期响应：`Hello World!`

## Two Startup Modes

| | 本地开发 (`./gradlew bootRun`) | Docker native image |
|---|---|---|
| **Profile** | `dev`（由 build.gradle bootRun 任务自动传入） | `default`（无 profile） |
| **日志路径** | `./logs/demo.log` | `/var/logs/demo.log` |
| **配置文件** | `application.yml` + `application-dev.yml` | `application.yml` + `application-default.yml` |

## Observability Stack

所有遥测数据通过 OTLP 协议发送到 OTel Collector，由 Collector 分发到三个后端组件，最后在 Grafana 统一查看：

```
App → OTLP HTTP (4318) → OTel Collector
                              ├→ Prometheus (8889, Prometheus exporter) — 指标
                              ├→ Tempo (5317, OTLP gRPC)                — 链路追踪
                              └→ Loki (3100, OTLP HTTP)                 — 日志
                                                                      └→ Grafana (3500) 统一可视化
```

- Grafana: http://localhost:3500 (admin/123456)
- Prometheus: http://localhost:9090
- Tempo: http://localhost:3200
- Actuator: http://localhost:8080/actuator/metrics

### Spring Boot 4.0 OTLP 属性前缀

三个信号使用不同的属性前缀，不要混淆：

| 信号 | 属性前缀 | 机制 |
|------|---------|------|
| Metrics | `management.otlp.metrics.export.*` | Micrometer OtlpMeterRegistry |
| Traces | `management.opentelemetry.tracing.export.otlp.*` | OTel SDK via `spring-boot-starter-opentelemetry` |
| Logs | `management.opentelemetry.logging.export.otlp.*` | OTel SDK via `spring-boot-starter-opentelemetry` |

### logback-spring.xml

自定义 logback 配置包含三个 appender：OTEL（OTel 日志导出）、CONSOLE（控制台）、FILE（文件输出）。

- CONSOLE 和 FILE 的日志格式包含 `[%X{traceId:-},%X{spanId:-}]`，支持通过 traceId 在 Grafana 中关联日志与链路
- FILE appender 通过 `<springProperty>` 读取 `logging.file.name`
- `logging.file.name` 分别在 profile 配置中定义：`application-dev.yml`（`./logs/demo.log`）供本地开发，`application-default.yml`（`/var/logs/demo.log`）供 Docker native image。`application.yml` 本身不设置此属性，避免两个路径同时生效
- **初始化顺序**：`DemoApplication.main()` 必须在 Spring 上下文启动后才能获取 `OpenTelemetrySdk` bean 并调用 `OpenTelemetryAppender.install()`。OTEL appender 在 install 之前的日志不会被导出

## Native Image Build Bindings

`bindings/` 目录通过 Paketo buildpack bindings 机制，将 `ext-files/` 中的本地文件注入构建容器，用于替换构建时的依赖下载源：
- `bellsoft-jdk-config`: 指向本地 Bellsoft Liberica JDK 25 tarball
- `syft-config`: 指向本地 Syft 工具包（用于 SBOM 生成）

`ext-files/` 目录不纳入 Git（体积大），需手动维护。

## Notes

- Maven 仓库使用华为云镜像，Gradle 分发包使用腾讯云镜像
- native image 容器启用了远程调试（8000 端口），JVM 堆限制 256MB
