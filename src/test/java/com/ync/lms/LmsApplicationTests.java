package com.ync.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// 테스트는 메모리 DB 사용 (data/ 의 실제 데이터를 건드리지 않게)
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class LmsApplicationTests {

	@Test
	void contextLoads() {
	}

}
