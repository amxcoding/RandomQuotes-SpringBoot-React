package com.amxcoding.randomquotes.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@SpringBootApplication
@ComponentScan(basePackages = "com.amxcoding.randomquotes")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);

	}

}
