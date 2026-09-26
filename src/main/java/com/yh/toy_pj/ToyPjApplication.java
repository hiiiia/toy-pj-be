package com.yh.toy_pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ToyPjApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyPjApplication.class, args);
	}
}
