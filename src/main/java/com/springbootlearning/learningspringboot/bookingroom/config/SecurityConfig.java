package com.springbootlearning.learningspringboot.bookingroom.config;

import com.springbootlearning.learningspringboot.bookingroom.security.LoginAttemptService;
import com.springbootlearning.learningspringboot.bookingroom.security.LoginRateLimitFilter;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationFailureHandler;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

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
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository) throws Exception{
        http
                .addFilterBefore(new LoginRateLimitFilter(loginAttemptService), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/login")
                )
                .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
                .exceptionHandling(ex -> ex
                        // API : reponse JSON 401, jamais de redirection HTML.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                        // Pages HTML : redirection vers /login.
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new NegatedRequestMatcher(new AntPathRequestMatcher("/api/**"))))
                .authorizeHttpRequests(
                auth ->auth
                        .requestMatchers("/login","/register", "/", "/css/**", "/images/**", "/js/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/csrf").permitAll()
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
