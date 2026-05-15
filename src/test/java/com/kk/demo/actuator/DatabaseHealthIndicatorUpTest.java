package com.kk.demo.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import com.kk.demo.TestMySqlConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestMySqlConfiguration.class)
class DatabaseHealthIndicatorUpTest {

  @Autowired
  private DatabaseHealthIndicator healthIndicator;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  void healthIsUpWhenMySqlIsReachable() {
    var health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("database", "MySQL");
  }

  @Test
  void gaugeReportsOneWhenHealthIsUp() {
    var gauge = meterRegistry.find("health.database.status").gauge();

    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(1.0);
  }
}
