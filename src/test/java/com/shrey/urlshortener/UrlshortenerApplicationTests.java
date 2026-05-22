package com.shrey.urlshortener;

import com.shrey.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootTest(properties = {
		"spring.profiles.active=test",
		"spring.autoconfigure.exclude=" +
				"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
				"org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
				"org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
				"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
				"org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class UrlshortenerApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		ShortUrlRepository shortUrlRepository() {
			return Mockito.mock(ShortUrlRepository.class);
		}

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return Mockito.mock(RedisConnectionFactory.class);
		}
	}
}
