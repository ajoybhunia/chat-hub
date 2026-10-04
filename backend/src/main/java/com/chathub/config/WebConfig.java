package com.chathub.config;

import com.chathub.common.security.JwtAuthFilter;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Central CORS policy — mirrors the Deno backend's {@code withCors()}:
 * origin {@code *}, methods GET/POST/PUT/DELETE/OPTIONS, headers
 * Content-Type/Authorization. Registered ahead of every other filter so all
 * responses (including 401s written by {@link JwtAuthFilter}) carry CORS headers.
 */
@Configuration
public class WebConfig {

	@Bean
	FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of("*"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
