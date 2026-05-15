package com.kk.demo.actuator;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

  private final JdbcTemplate jdbcTemplate;

  public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
    this.jdbcTemplate = jdbcTemplate;

    Gauge.builder("health.database.status", this::measureHealth)
        .description("Database health status (1=UP, 0=DOWN)")
        .register(meterRegistry);
  }

  @Override
  public Health health() {
    try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      return Health.up()
          .withDetail("database", metaData.getDatabaseProductName())
          .withDetail("version", metaData.getDatabaseProductVersion())
          .build();
    } catch (Exception e) {
      log.warn("Database health check failed", e);
      return Health.down()
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private double measureHealth() {
    return health().getStatus() == Status.UP ? 1.0 : 0.0;
  }
}
