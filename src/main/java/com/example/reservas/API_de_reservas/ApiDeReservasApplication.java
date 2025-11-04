package com.example.reservas.API_de_reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.reservas")
@EnableJpaRepositories(basePackages = "com.example.reservas.repo")
@EntityScan(basePackages = "com.example.reservas.domain")
public class ApiDeReservasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiDeReservasApplication.class, args);
	}

}
