package com.sneaky.sneaky;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SneakyApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SneakyApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Sneaky Application Started!");
	}

}
