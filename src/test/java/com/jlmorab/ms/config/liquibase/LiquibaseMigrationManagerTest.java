package com.jlmorab.ms.config.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jlmorab.ms.data.TestData;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.integration.spring.SpringLiquibase;

@ExtendWith(MockitoExtension.class)
class LiquibaseMigrationManagerTest {

	private static final Exception TEST_EXCEPTION = new RuntimeException("Test exception");
	
	
	@Mock
	DataSource dataSource;
	
	@Test
	void resetAndApplyMigration_executeMigrations_shouldExecuteNMigrations() {
		List<LiquibaseMigration> migrations = migrationsProvider();
		
		try( MockedConstruction<SpringLiquibase> mockedConstructor = Mockito.mockConstruction( SpringLiquibase.class ) ) {
			LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
			
			List<SpringLiquibase> liquibaseInstances = mockedConstructor.constructed();
			assertThat( liquibaseInstances )
				.hasSize( migrations.size() )
				.allSatisfy( mock -> {
					verify( mock, times(1) ).afterPropertiesSet();
				} );
		}//end try
	}//end resetAndApplyMigration_executeMigrations_shouldExecuteNMigrations
	
	@Test
	void resetAndApplyMigration_sameMigration_shouldRollbackBeforeExecuteAgain() throws Exception {
		List<LiquibaseMigration> migrations = new ArrayList<>();
		migrations.add( new LiquibaseMigration(1, "same-changelog.xml", null) );
		migrations.add( new LiquibaseMigration(2, "same-changelog.xml", null) );
		
		try( MockedConstruction<SpringLiquibase> executeConstructor = Mockito.mockConstruction( SpringLiquibase.class );
			 MockedConstruction<Liquibase> rollbackConstructor = Mockito.mockConstruction( Liquibase.class ) ) {
			
			try( MockedStatic<DatabaseFactory> databaseFactoryMocked = Mockito.mockStatic( DatabaseFactory.class ) ) {
				DatabaseFactory factory = mock( DatabaseFactory.class );
				Database database = mock( Database.class );
				databaseFactoryMocked.when( DatabaseFactory::getInstance ).thenReturn( factory );
				when( factory.findCorrectDatabaseImplementation( any() ) ).thenReturn( database );
				
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
				
				List<SpringLiquibase> executeInstances = executeConstructor.constructed();
				List<Liquibase> rollbackInstances = rollbackConstructor.constructed();
				
				assertThat( executeInstances )
					.hasSize( 2 )
					.allSatisfy( mock -> {
						verify( mock, times(1) ).afterPropertiesSet();
					} );
				assertThat( rollbackInstances )
					.hasSize( 1 )
					.allSatisfy( mock -> {
						verify( mock, times(1) ).rollback( anyInt(), any() );
					} );
			}//end try
		}//end try
	}//end resetAndApplyMigration_sameMigration_shouldRollbackBeforeExecuteAgain()

	private List<LiquibaseMigration> migrationsProvider() {
		int migrations = TestData.getRandom(1, 10);
		
		List<LiquibaseMigration> result = new ArrayList<>();
		
		result.add( new LiquibaseMigration(1, "changelogWithoutContext.xml", null) );
		
		int order = 2;
		for( int i = 1 ; i <= migrations; i++ ) {
			LiquibaseMigration migration = LiquibaseMigration.builder()
					.order( order )
					.changeLogFile( "changelog" + i + ".xml" )
					.context( "context" + i )
					.build();
			result.add( migration );
			order += 1;
		}//end for
		
		return result;
	}//end migrationsProvider()
	
	@ParameterizedTest
	@MethodSource("emptyMigrationsProvider")
	void resetAndApplyMigration_emptyMigrations_shouldntDoNothing( List<LiquibaseMigration> migrations ) {
		try( MockedConstruction<SpringLiquibase> mockedConstruction = Mockito.mockConstruction( SpringLiquibase.class ) ) {
			LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource );
			
			assertTrue( mockedConstruction.constructed().isEmpty(), "Should not have constructed any SpringLiquibase instances" );
		}//end try
	}//end resetAndApplyMigration_emptyMigrations_shouldntDoNothing()
	
	static Stream<Arguments> emptyMigrationsProvider() {
		return Stream.of(
				Arguments.of( (List<LiquibaseMigration>) null ),
				Arguments.of( List.of() )
		);
	}//end emptyMigrationsProvider()
	
	@Test
	void resetAndApplyMigration_updateThrowException() {
		List<LiquibaseMigration> migrations = List.of( new LiquibaseMigration(1, "changelogUpdateException.xml", null) );
		
		try( MockedConstruction<Liquibase> rollbackConstructor = Mockito.mockConstruction( Liquibase.class );
			 MockedConstruction<SpringLiquibase> executeConstructor = Mockito.mockConstruction( SpringLiquibase.class, (mock, context) -> {
			doThrow( TEST_EXCEPTION ).when( mock ).afterPropertiesSet();
		})) {
			Exception actual = assertThrows( RuntimeException.class, () -> 
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource ) );
			
			assertTrue( rollbackConstructor.constructed().isEmpty(), "Should not have constructed for rollback" );
			assertEquals( TEST_EXCEPTION, actual.getCause() );
		}//end try
	}//end resetAndApplyMigration_updateThrowException
	
	@Test
	void resetAndApplyMigration_rollbackThrowException() throws Exception {
		List<LiquibaseMigration> migrations = new ArrayList<>();
		migrations.add( new LiquibaseMigration(1, "changelogRollbackException.xml", null) );
		migrations.add( new LiquibaseMigration(2, "changelogRollbackException.xml", null) );
		
		when( dataSource.getConnection() ).thenThrow( TEST_EXCEPTION );
		
		try( MockedConstruction<SpringLiquibase> executeConstructor = Mockito.mockConstruction( SpringLiquibase.class ); 
			 MockedConstruction<Liquibase> rollbackConstructor = Mockito.mockConstruction( Liquibase.class)) {
			
			Exception actual = assertThrows( RuntimeException.class, () -> 
				LiquibaseMigrationManager.resetAndApplyMigration( migrations, dataSource ) );
			
			List<SpringLiquibase> executeInstances = executeConstructor.constructed();
			
			assertEquals( TEST_EXCEPTION, actual.getCause() );
			
			assertThat( executeInstances )
				.hasSize( 1 )
				.allSatisfy( mock -> {
					verify( mock, times(1) ).afterPropertiesSet();
				} );
		}//end try
	}//end resetAndApplyMigration_rollbackThrowException()
	
}
