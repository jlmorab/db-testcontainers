package com.jlmorab.ms.config;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.jlmorab.ms.config.liquibase.LiquibaseMigration;
import com.jlmorab.ms.config.liquibase.LiquibaseMigrationManager;

import jakarta.annotation.PostConstruct;

@SuppressWarnings("resource")
public abstract class ContainerBaseTest { 
	
	@Autowired // NOSONAR
	private DataSource dataSource;
	
	protected ContainerBaseTest() {}

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
        registry.add("spring.liquibase.enabled", () -> "false");
    }//end databaseProperties()
	
	/**
	 * Must be implemented to apply migrations
	 * @return List of migrations to apply
	 */
	protected List<LiquibaseMigration> getMigrations() {
		return Collections.emptyList();
	}//end getMigrations()
	
	/**
	 * Method that configured and execute liquibase migrations 
	 */
	@PostConstruct
	protected void setupDatabase() {
		LiquibaseMigrationManager.resetAndApplyMigration( getMigrations(), dataSource );
	}//end setupDatabase()
	
}
