package com.project.admin.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import org.springframework.security.config.Customizer;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class adminSecurityConfig {
	@Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
            "http://127.0.0.1:5500", 
            "http://localhost:5500", 
            "http://127.0.0.1:5501", 
            "http://localhost:5501",
            "https://quantifyre-iris-super-admin.vercel.app", // Correct spelling
            "https://quantifire-iris-frontend.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

	@Bean
	@Order(1) 
	public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
	    http
	        .cors(Customizer.withDefaults())
	        .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
	        .securityMatcher("/api/admin/**", "/uploads/**") 
	        .csrf(csrf -> csrf.disable())
	        .authorizeHttpRequests(auth -> auth
	        	.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
	            .requestMatchers("/api/admin/agencies/**",
	            		         "/api/admin/login/**",
	            		         "/api/admin/campaigns/**",
	            		         "/api/admin/settings/**",
	            		         "/api/admin/top-notifications/**",
	            		         "/api/admin/invoices/**",
	            		         "/api/admin/roles/**",
	            		         "/api/admin/ping",
	            		         "/uploads/**").permitAll()
	            .anyRequest().authenticated()
	        );
	    return http.build();
	}
}