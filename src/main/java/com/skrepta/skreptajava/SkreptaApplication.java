package com.skrepta.skreptajava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SkreptaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkreptaApplication.class, args);
	}

}
