package com.kk.demo;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

  public static void main(String[] args) {
    var context = SpringApplication.run(DemoApplication.class, args);
    Optional.of(context.getBean(OpenTelemetrySdk.class)).ifPresent(OpenTelemetryAppender::install);
  }
}
