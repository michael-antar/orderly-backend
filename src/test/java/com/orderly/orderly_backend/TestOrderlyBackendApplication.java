package com.orderly.orderly_backend;

import org.springframework.boot.SpringApplication;

public class TestOrderlyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderlyBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
