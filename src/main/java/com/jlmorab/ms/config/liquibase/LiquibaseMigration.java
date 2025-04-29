package com.jlmorab.ms.config.liquibase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquibaseMigration {
	
	private int order;
	private String changeLogFile;
	private String context;

}
