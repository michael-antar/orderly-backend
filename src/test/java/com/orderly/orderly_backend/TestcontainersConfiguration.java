package com.orderly.orderly_backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	@SuppressWarnings("resource") // Suppresses IDE false positive for unclosed AutoCloseable
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
				.waitingFor(
						new WaitAllStrategy()
								.withStrategy(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
								.withStrategy(Wait.forListeningPort())
								.withStartupTimeout(Duration.ofSeconds(60))
				);
	}

}
