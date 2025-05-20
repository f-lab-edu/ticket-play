package com.flab.tiple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TipleApplication {

	public static void main(String[] args) {
		SpringApplication.run(TipleApplication.class, args);
	}

}
