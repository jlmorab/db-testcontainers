package com.jlmorab.ms.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;

class ContainerBaseTestTest {

	@Test
	void testcontainersIsRunning() {
		assertTrue( ContainerBaseTest.database.isRunning() );
	}//end testcontainersIsRunning()
	
	@Test
	void testDatabaseProperties() {
		DynamicPropertyRegistry registry = mock( DynamicPropertyRegistry.class );
		
		ContainerBaseTest.databaseProperties( registry );
		
		verify( registry ).add(eq("spring.datasource.url"), any());
        verify( registry ).add(eq("spring.datasource.username"), any());
        verify( registry ).add(eq("spring.datasource.password"), any());
        verify( registry ).add(eq("spring.datasource.driver-class-name"), any());
	}//end testDatabaseProperties()

}
