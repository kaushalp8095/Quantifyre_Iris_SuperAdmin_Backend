package com.project.admin.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class adminSecurityConfig {

	@Bean
	@Order(1) 
	public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
	    http
	        .securityMatcher("/api/admin/**", "/uploads/**") 
	        .cors(cors -> cors.configurationSource(request -> {
	            CorsConfiguration config = new CorsConfiguration();
	            // Sabhi possible frontend ports add karein
	            config.setAllowedOrigins(List.of("http://127.0.0.1:5500", "http://localhost:5500", "http://127.0.0.1:5501", "http://localhost:5501","https://quantifire-iris-frontend.vercel.app"));
	            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
	            config.setAllowedHeaders(List.of("*"));
	            config.setAllowCredentials(true);
	            return config;
	        }))
	        .csrf(csrf -> csrf.disable())
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers("/api/admin/agencies/**",
	            		         "/api/admin/login/**",
	            		         "/api/admin/campaigns/**",
	            		         "/api/admin/settings/**",
	            		         "/api/admin/top-notifications/**",
	            		         "/uploads/**").permitAll()
	            .anyRequest().authenticated()
	        );
	    return http.build();
	}
}