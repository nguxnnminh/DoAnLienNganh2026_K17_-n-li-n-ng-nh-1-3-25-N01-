package com.shop.clothingstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClothingstoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClothingstoreApplication.class, args);
	}

}
