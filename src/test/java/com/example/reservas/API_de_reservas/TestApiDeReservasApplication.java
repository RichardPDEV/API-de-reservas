package com.example.reservas.API_de_reservas;

import org.springframework.boot.SpringApplication;

public class TestApiDeReservasApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiDeReservasApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
