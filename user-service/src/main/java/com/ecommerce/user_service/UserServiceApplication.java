package com.ecommerce.user_service;

import com.ecommerce.user_service.model.Role;
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
			// Create an ADMIN user
			if (userRepository.findByEmail("admin@example.com").isEmpty()) {
				User adminUser = new User();
				adminUser.setName("Admin User");
				adminUser.setEmail("admin@example.com");
				adminUser.setPassword(passwordEncoder.encode("password"));
				adminUser.setRole(Role.ROLE_ADMIN);
				userRepository.save(adminUser);
				System.out.println(">>> Admin user created!");
			}

			// Create a REGULAR user
			if (userRepository.findByEmail("user@example.com").isEmpty()) {
				User regularUser = new User();
				regularUser.setName("Regular User");
				regularUser.setEmail("user@example.com");
				regularUser.setPassword(passwordEncoder.encode("password"));
				regularUser.setRole(Role.ROLE_USER);
				userRepository.save(regularUser);
				System.out.println(">>> Regular user created!");
			}
		};
	}

}
