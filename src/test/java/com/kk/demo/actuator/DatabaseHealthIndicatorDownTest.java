package com.kk.demo.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import com.kk.demo.TestMySqlConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import({TestMySqlConfiguration.class, DatabaseHealthIndicatorDownTest.BrokenJdbcConfig.class})
class DatabaseHealthIndicatorDownTest {

  @Autowired
  private DatabaseHealthIndicator healthIndicator;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  void healthIsDownWhenDatabaseIsUnreachable() {
    var health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  @Test
  void gaugeReportsZeroWhenHealthIsDown() {
    var gauge = meterRegistry.find("health.database.status").gauge();

    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(0.0);
  }

  @TestConfiguration
  static class BrokenJdbcConfig {
    @Bean
    @Primary
    JdbcTemplate brokenJdbcTemplate() throws SQLException {
      var dsMock = Mockito.mock(DataSource.class);
      Mockito.when(dsMock.getConnection()).thenThrow(new SQLException("Connection refused"));

      var mock = Mockito.mock(JdbcTemplate.class);
      Mockito.when(mock.getDataSource()).thenReturn(dsMock);
      return mock;
    }
  }
}
