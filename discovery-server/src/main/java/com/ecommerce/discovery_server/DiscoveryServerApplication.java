package com.ecommerce.discovery_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Server Application
 *
 * This is the Netflix Eureka Discovery Server that enables service registration
 * and discovery for the E-commerce microservices platform.
 *
 * Key Features:
 * - Service Registration: Microservices register themselves with this server
 * - Service Discovery: Services can discover and communicate with each other
 * - Health Monitoring: Tracks the health status of registered services
 * - Load Balancing: Provides service instance information for load balancing
 *
 * Port: 8761 (Standard Eureka port)
 *
 */
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscoveryServerApplication.class, args);
	}
}