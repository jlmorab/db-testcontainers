package com.jlmorab.ms.config.liquibase;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class LiquibaseMigrationManager {

	private static final Set<String> MIGRATION_APPLIED = ConcurrentHashMap.newKeySet();
	
	public static void resetAndApplyMigration( List<LiquibaseMigration> migrations, DataSource dataSoruce ) {
		if( migrations != null && !migrations.isEmpty() ) {
			List<LiquibaseMigration> sortedMigrations = migrations.stream()
					.sorted( Comparator.comparingInt( LiquibaseMigration::getOrder ) )
					.toList();
			sortedMigrations.forEach( migration -> {
				try {
					log.info( "Applying Liquibase migration: {}", migration.getChangeLogFile() );
					
					if( MIGRATION_APPLIED.contains( migration.getChangeLogFile() ) ) {
						performRollback( migration, dataSoruce );
					}//end if
					
					performUpdate( migration, dataSoruce );
					
					log.info( "Successfully applied migration: {}", migration.getChangeLogFile() );
				} catch( Exception e ) {
					log.error( "Error to apply migration: {}", migration.getChangeLogFile(), e );
					throw new RuntimeException( "Failed to apply Liquibase migration", e ); //NOSONAR
				}//end try
			});
		}//end if
	}//end resetAndApplyMigration()
	
	public static void performRollback( LiquibaseMigration migration, DataSource dataSource ) throws Exception {
		try {
			Connection connection = dataSource.getConnection();
			Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation( new JdbcConnection( connection ) );
			ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
			
			try( Liquibase liquibase = new Liquibase( migration.getChangeLogFile(), resourceAccessor, database ) ) {
				liquibase.rollback( Integer.MAX_VALUE, null);
			}//end try
			
			log.info("Rollback migration: {}", migration.getChangeLogFile() );
		} catch( Exception e ) {
			log.warn("Rollback failed for migration: {}", migration.getChangeLogFile());
			throw e;
		}//end try
	}//end performRollback()
	
	public static void performUpdate( LiquibaseMigration migration, DataSource dataSource ) throws Exception {
		try {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource( dataSource );
			liquibase.setChangeLog( migration.getChangeLogFile() );
			
			if( StringUtils.isNotBlank( migration.getContext() ) ) {
				liquibase.setContexts( migration.getContext() );
			}//end if
			
			liquibase.afterPropertiesSet();
			
			MIGRATION_APPLIED.add( migration.getChangeLogFile() );
			
			log.info( "Successfully applied migration: {}", migration.getChangeLogFile() );
		} catch( Exception e ) {
			log.warn("Failed to apply migration: {}", migration.getChangeLogFile());
			throw e;
		}//end try
	}//end performUpdate()
	
}
