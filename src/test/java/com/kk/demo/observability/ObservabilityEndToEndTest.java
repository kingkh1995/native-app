package com.kk.demo.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.kk.demo.TestMySqlConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

/**
 * 端到端可观测性数据流转验证。
 * <p>
 * 测试流程：
 * <ol>
 *   <li>调用应用 /hello 接口（触发 Controller 代码执行）</li>
 *   <li>查询 Prometheus API，验证 hello.* 指标已到达</li>
 *   <li>查询 Tempo API，验证 demo 服务的链路 Span 已到达</li>
 *   <li>查询 Loki API，验证日志（含 trace_id 结构化元数据）已到达</li>
 * </ol>
 * <p>
 * 前提条件：Docker 可观测性栈（otel-collector + prometheus + tempo + loki）必须已在运行。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import({TestMySqlConfiguration.class, ObservabilityEndToEndTest.OtelAppenderConfig.class})
class ObservabilityEndToEndTest {

  // Test-specific configuration: install the OpenTelemetry log appender after the
  // Spring context is ready, so that logback OTel appender is connected to the SDK.
  // (In production this is done by DemoApplication.main(); during @SpringBootTest it's not.)
  @TestConfiguration
  static class OtelAppenderConfig {
    @Bean
    SmartInitializingSingleton installOtelAppender(OpenTelemetrySdk otelSdk) {
      return () -> OpenTelemetryAppender.install(otelSdk);
    }
  }

  private static final String PROMETHEUS = "http://localhost:9090";
  private static final String TEMPO = "http://localhost:3200";
  private static final String LOKI = "http://localhost:3100";

  @LocalServerPort
  private int appPort;

  private final RestClient http = RestClient.builder().build();
  private final ObjectMapper json = new ObjectMapper();

  @BeforeAll
  static void checkInfrastructureRunning() {
    var probe = RestClient.builder().build();
    try {
      assertThat(probe.get().uri(PROMETHEUS + "/metrics").retrieve().toBodilessEntity()
          .getStatusCode().is2xxSuccessful())
          .as("Prometheus not reachable on " + PROMETHEUS + ". Run: docker compose up -d")
          .isTrue();
      assertThat(probe.get().uri(TEMPO + "/metrics").retrieve().toBodilessEntity()
          .getStatusCode().is2xxSuccessful())
          .as("Tempo not reachable on " + TEMPO + ". Run: docker compose up -d")
          .isTrue();
      assertThat(probe.get().uri(LOKI + "/metrics").retrieve().toBodilessEntity()
          .getStatusCode().is2xxSuccessful())
          .as("Loki not reachable on " + LOKI + ". Run: docker compose up -d")
          .isTrue();
    } catch (Exception e) {
      throw new AssertionError(
          "Docker infrastructure not reachable. Run 'docker compose up -d' first.\n" +
              "Prometheus: " + PROMETHEUS + " , Tempo: " + TEMPO + " , Loki: " + LOKI, e);
    }
  }

  @Test
  void metricsReachPrometheusAfterCallingHello() throws Exception {
    callHello();

    var metricFound = pollUntil(() -> {
      // Use URI directly to avoid RestClient's {var} template processing
      String query = UriUtils.encodeQueryParam("{__name__=~\"hello.*\"}", StandardCharsets.UTF_8);
      URI uri = URI.create(PROMETHEUS + "/api/v1/query?query=" + query);
      var resp = http.get().uri(uri).retrieve().toEntity(String.class);
      var tree = json.readTree(resp.getBody());
      return tree.at("/data/result").isArray()
          && tree.at("/data/result").iterator().hasNext();
    }, Duration.ofSeconds(35));

    assertThat(metricFound)
        .as("hello.* metric must appear in Prometheus within 35s (scrape interval 20s + margin)")
        .isTrue();
  }

  @Test
  void tracesReachTempoAfterCallingHello() throws Exception {
    callHello();

    // Poll Tempo search for any trace from service "demo"
    var traceFound = pollUntil(() -> {
      URI uri = URI.create(TEMPO + "/api/search?service.name=demo");
      var resp = http.get().uri(uri).retrieve().toEntity(String.class);
      var tree = json.readTree(resp.getBody());
      JsonNode traces = tree.get("traces");
      if (traces == null || !traces.isArray() || !traces.iterator().hasNext()) {
        return false;
      }
      // Check that at least one trace belongs to the demo service
      for (JsonNode t : traces) {
        if ("demo".equals(t.path("rootServiceName").asString(""))) {
          return true;
        }
      }
      return false;
    }, Duration.ofSeconds(15));

    assertThat(traceFound)
        .as("Traces from service 'demo' must appear in Tempo within 15s")
        .isTrue();
  }

  @Test
  void logsReachLokiAfterCallingHello() throws Exception {
    callHello();

    var logFound = pollUntil(() -> {
      String encodedQuery = UriUtils.encodeQueryParam("{service_name=\"demo\"}", StandardCharsets.UTF_8);
      URI uri = URI.create(LOKI + "/loki/api/v1/query_range?query=" + encodedQuery + "&limit=20");
      var resp = http.get().uri(uri).retrieve().toEntity(String.class);
      var tree = json.readTree(resp.getBody());
      JsonNode result = tree.at("/data/result");
      return result.isArray() && result.iterator().hasNext();
    }, Duration.ofSeconds(15));

    assertThat(logFound)
        .as("Logs with service_name=demo must appear in Loki within 15s")
        .isTrue();
  }

  @Test
  void logsContainTraceIdStructuredMetadata() throws Exception {
    callHello();

    var metadataFound = pollUntil(() -> {
      String encodedQuery = UriUtils.encodeQueryParam("{service_name=\"demo\"}", StandardCharsets.UTF_8);
      URI uri = URI.create(LOKI + "/loki/api/v1/query_range?query=" + encodedQuery + "&limit=50");
      var resp = http.get().uri(uri).retrieve().toEntity(String.class);
      var tree = json.readTree(resp.getBody());
      JsonNode result = tree.at("/data/result");
      if (!result.isArray()) {
        return false;
      }
      // Walk all log streams and their values to find trace_id structured metadata
      for (JsonNode stream : result) {
        JsonNode values = stream.get("values");
        if (values == null) {
          continue;
        }
        for (JsonNode entry : values) {
          if (entry.isArray() && entry.size() >= 3) {
            JsonNode metadata = entry.get(2);
            if (metadata != null && metadata.has("trace_id")) {
              String tid = metadata.get("trace_id").asString("");
              if (!tid.isEmpty() && !tid.equals("00000000000000000000000000000000")) {
                return true;
              }
            }
          }
          // Also check if trace_id appears in the stream's labels
          JsonNode streamLabels = stream.get("stream");
          if (streamLabels != null && streamLabels.has("trace_id")) {
            return true;
          }
        }
      }
      return false;
    }, Duration.ofSeconds(20));

    assertThat(metadataFound)
        .as("Log entries must contain trace_id structured metadata for Tempo correlation")
        .isTrue();
  }

  // ---------------------------------------------------------------
  //  helpers
  // ---------------------------------------------------------------

  private void callHello() {
    var resp = http.get()
        .uri("http://localhost:" + appPort + "/hello")
        .retrieve().toEntity(String.class);
    assertThat(resp.getBody()).isEqualTo("Hello World!");
  }

  @FunctionalInterface
  private interface Condition {
    boolean test() throws Exception;
  }

  private static boolean pollUntil(Condition condition, Duration timeout) throws Exception {
    long deadline = System.nanoTime() + timeout.toNanos();
    long pollInterval = 500;
    while (System.nanoTime() < deadline) {
      try {
        if (condition.test()) {
          return true;
        }
      } catch (Exception ignored) {
        // Transient network errors or not-yet-ready: retry on next iteration
      }
      Thread.sleep(pollInterval);
    }
    // one last try (exception propagates if this fails)
    return condition.test();
  }
}
