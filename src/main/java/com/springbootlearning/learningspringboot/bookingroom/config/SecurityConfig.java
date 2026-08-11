package com.springbootlearning.learningspringboot.bookingroom.config;

import com.springbootlearning.learningspringboot.bookingroom.security.LoginAttemptService;
import com.springbootlearning.learningspringboot.bookingroom.security.LoginRateLimitFilter;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationFailureHandler;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;

    public SecurityConfig(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .addFilterBefore(new LoginRateLimitFilter(loginAttemptService), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )
                .authorizeHttpRequests(
                auth ->auth
                        .requestMatchers("/login","/register", "/", "/css/**", "/images/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/booking/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
        )
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .successHandler(new RateLimitingAuthenticationSuccessHandler(loginAttemptService))
                                .failureHandler(new RateLimitingAuthenticationFailureHandler(loginAttemptService))
                                .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );
        return http.build();
    }
}
