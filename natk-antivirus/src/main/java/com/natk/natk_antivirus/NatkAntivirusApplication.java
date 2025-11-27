package com.natk.natk_antivirus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class NatkAntivirusApplication {

	public static void main(String[] args) {
		SpringApplication.run(NatkAntivirusApplication.class, args);
	}

}
