package com.flab.tiple;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.flab.tiple.global.auth.aop.LoginCheckAspect;
import com.flab.tiple.global.security.JwtTokenProvider;

@ActiveProfiles("test")
@SpringBootTest
class TipleApplicationTests {

	@Test
	void contextLoads() {
	}

}
