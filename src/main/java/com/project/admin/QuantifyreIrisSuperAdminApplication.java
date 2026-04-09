package com.project.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.project.admin", "com.project.common"})
@EntityScan(basePackages = {"com.project.common.models"}) 

// FIX: Only point to the JPA-specific package
@EnableJpaRepositories(basePackages = "com.project.common.repository.jpa")

// FIX: Only point to the MongoDB-specific package
@EnableMongoRepositories(basePackages = "com.project.common.repository.mongodb")
public class QuantifyreIrisSuperAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantifyreIrisSuperAdminApplication.class, args);
    }
}