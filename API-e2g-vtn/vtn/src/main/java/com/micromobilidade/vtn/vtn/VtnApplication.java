package com.micromobilidade.vtn.vtn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VtnApplication {

	public static void main(String[] args) {
		SpringApplication.run(VtnApplication.class, args);
	}

}
