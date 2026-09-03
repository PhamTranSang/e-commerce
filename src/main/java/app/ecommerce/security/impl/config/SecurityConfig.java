package app.ecommerce.security.impl.config;

import app.ecommerce.security.impl.filter.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(//
                                                   final HttpSecurity http, //
                                                   final JwtAuthenticationFilter jwtAuthenticationFilter, //
                                                   final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint, //
                                                   final ProblemDetailAccessDeniedHandler accessDeniedHandler //
    ) {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/actuator/health/**", "/api/auth/login", "/api/public/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMIN", "STAFF")
                    .anyRequest()
                    .authenticated()
                )
                .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }
}