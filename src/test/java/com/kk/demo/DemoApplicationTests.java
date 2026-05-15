package com.kk.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestMySqlConfiguration.class)
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
