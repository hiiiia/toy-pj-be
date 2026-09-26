package com.yh.toy_pj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.yh.toy_pj.support.PostgresTestContainer;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainer.class)
class ToyPjApplicationTests {

	@Test
	void contextLoads() {
	}
}
