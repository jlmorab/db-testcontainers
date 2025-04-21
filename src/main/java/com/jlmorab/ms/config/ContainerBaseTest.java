package com.jlmorab.ms.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SuppressWarnings("resource")
public abstract class ContainerBaseTest {
	
	private ContainerBaseTest() {}

	static final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test");
	
	static {
		database.start();
	}
	
	@DynamicPropertySource
    static void databaseProperties( DynamicPropertyRegistry registry ) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
        registry.add("spring.datasource.driver-class-name", database::getDriverClassName);
    }//end databaseProperties()
	
}
