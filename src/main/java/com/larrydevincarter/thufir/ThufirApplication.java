package com.larrydevincarter.thufir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ThufirApplication {

	public static void main(String[] args) {
		System.setProperty("user.timezone", "America/Chicago");
		SpringApplication.run(ThufirApplication.class, args);
	}

}
