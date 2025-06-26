package com.flab.tiple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.flab.tiple")
public class TipleTicketApplication {

	public static void main(String[] args) {
		SpringApplication.run(TipleTicketApplication.class, args);
	}

}
