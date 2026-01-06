package com.springbootlearning.learningspringboot.bookingroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http
                .authorizeHttpRequests(
                auth ->auth
                        .requestMatchers("/login","/register", "/", "/css/**", "/images/**", "/js/**").permitAll()
                        .requestMatchers("/dashboard").hasRole("USER")
                        .anyRequest().authenticated()
        )
                .formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/dashboard", true)
                                .failureUrl("/login?error")
                                .permitAll()
                );
        return http.build();
    }
}
