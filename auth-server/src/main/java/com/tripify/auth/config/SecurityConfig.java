package com.tripify.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String frontendOrigin;

    public SecurityConfig(@Value("${app.frontend-origin}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    /**
     * Resource Server chain for the account REST API (profile / password management).
     * Protected by the same JWT the authorization server issues.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain accountApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/account/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * OAuth2 / OIDC protocol endpoints (authorize, token, jwks, userinfo, ...).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                .cors(Customizer.withDefaults())
                // Redirect unauthenticated browser requests to the custom login page,
                // or to the registration page when the authorize request asked for it
                // (?screen=register). The saved authorize request is preserved either
                // way, so after "register -> login" the code flow resumes correctly.
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        screenAwareAuthenticationEntryPoint(),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
                // Accept the JWT access token on protected resources (e.g. userinfo).
                .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Default chain: custom form login + registration page.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register", "/error", "/css/**", "/webjars/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.loginPage("/login").permitAll());
        return http.build();
    }

    /**
     * Chooses between the login and registration pages based on the {@code screen}
     * query parameter on the authorize request. The Spring Security saved-request
     * mechanism stores the original authorize request before this runs, so resuming
     * the flow after authentication works regardless of which page was shown.
     */
    private AuthenticationEntryPoint screenAwareAuthenticationEntryPoint() {
        LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
        LoginUrlAuthenticationEntryPoint registerEntryPoint = new LoginUrlAuthenticationEntryPoint("/register");
        return (request, response, authException) -> {
            if ("register".equals(request.getParameter("screen"))) {
                registerEntryPoint.commence(request, response, authException);
            } else {
                loginEntryPoint.commence(request, response, authException);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
