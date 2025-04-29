package com.jlmorab.ms.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.context.DynamicPropertyRegistry;

import com.jlmorab.ms.config.liquibase.LiquibaseMigration;
import com.jlmorab.ms.config.liquibase.LiquibaseMigrationManager;

class ContainerBaseTestTest {
	
	class WithoutMigrationsTestClass extends ContainerBaseTest {}
	
	class WithMigrationsTestClass extends ContainerBaseTest {
		@Override
		protected List<LiquibaseMigration> getMigrations() {
			return List.of( new LiquibaseMigration( 1, "changelogTest.xml", null ) );
		}//end getMigrations()
	}//end TestClass

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
	@Test
	void implementationWithoutMigrations_shouldntDoNothing() {
		try( MockedStatic<LiquibaseMigrationManager> mocked = Mockito.mockStatic(LiquibaseMigrationManager.class) ) {
			WithoutMigrationsTestClass implementationOne = new WithoutMigrationsTestClass();
			WithMigrationsTestClass implementationTwo = new WithMigrationsTestClass();
			
			implementationOne.setupDatabase();
			implementationTwo.setupDatabase();
			
			mocked.verify( () -> LiquibaseMigrationManager.resetAndApplyMigration( anyList(), any() ), times(2) );
		}//end try
	}//end implementationWithoutMigrations_shouldntDoNothing()
	
	

}
