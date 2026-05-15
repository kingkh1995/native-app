package com.kk.demo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestMySqlConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysql() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
