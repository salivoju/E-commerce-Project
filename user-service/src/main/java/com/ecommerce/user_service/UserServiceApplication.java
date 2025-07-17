package com.ecommerce.user_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(UserServiceApplication.class);

	public static void main(String[] args) {
		logger.info("Starting User Service Application...");
		SpringApplication.run(UserServiceApplication.class, args);
		logger.info("User Service Application started successfully!");
	}
}