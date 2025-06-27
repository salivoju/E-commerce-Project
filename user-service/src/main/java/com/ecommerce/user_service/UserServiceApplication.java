package com.ecommerce.user_service;

import com.ecommerce.user_service.model.User;
import com.ecommerce.user_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner createTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Check if the test user already exists to avoid creating it every time
			if (userRepository.findByEmail("test@example.com").isEmpty()) {
				System.out.println("Creating test user...");
				User testUser = new User();
				testUser.setName("Test User");
				testUser.setEmail("test@example.com");
				// IMPORTANT: Encode the password before saving
				testUser.setPassword(passwordEncoder.encode("password"));
				userRepository.save(testUser);
				System.out.println("Test user created!");
			} else {
				System.out.println("Test user already exists.");
			}
		};
	}

}
