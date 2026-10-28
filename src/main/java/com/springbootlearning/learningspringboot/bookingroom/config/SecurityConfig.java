package com.springbootlearning.learningspringboot.bookingroom.config;

import com.springbootlearning.learningspringboot.bookingroom.security.LoginAttemptService;
import com.springbootlearning.learningspringboot.bookingroom.security.LoginRateLimitFilter;
import com.springbootlearning.learningspringboot.bookingroom.security.LoginRateLimiter;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationFailureHandler;
import com.springbootlearning.learningspringboot.bookingroom.security.RateLimitingAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginAttemptService loginAttemptService;
    private final LoginRateLimiter loginRateLimiter;
    private final boolean cookieSecure;
    private final boolean apiDocsEnabled;

    public SecurityConfig(LoginAttemptService loginAttemptService,
                          LoginRateLimiter loginRateLimiter,
                          @Value("${app.security.cookie-secure:false}") boolean cookieSecure,
                          @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) {
        this.loginAttemptService = loginAttemptService;
        this.loginRateLimiter = loginRateLimiter;
        this.cookieSecure = cookieSecure;
        this.apiDocsEnabled = apiDocsEnabled;
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
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .secure(cookieSecure)
                .httpOnly(false)
                .sameSite("Lax"));
        return repository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository,
                                           CookieCsrfTokenRepository csrfTokenRepository) throws Exception{
        http
                .addFilterBefore(new LoginRateLimitFilter(loginAttemptService, loginRateLimiter),
                        UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
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
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/login", "/register", "/", "/css/**", "/images/**", "/js/**",
                                    "/actuator/health", "/actuator/info").permitAll()
                            .requestMatchers("/api/auth/login", "/api/csrf").permitAll();

                    if (apiDocsEnabled) {
                        auth.requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll();
                    }

                    auth.requestMatchers("/api/**").authenticated()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .requestMatchers("/dashboard", "/booking/**").hasAnyRole("USER", "ADMIN")
                            .anyRequest().authenticated();
                })
        .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; "
                                + "script-src 'self'; "
                                + "style-src 'self'; "
                                + "img-src 'self' data:; "
                                + "font-src 'self'; "
                                + "connect-src 'self'; "
                                + "form-action 'self'; "
                                + "frame-ancestors 'self'; "
                                + "base-uri 'self'; "
                                + "object-src 'none'"))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // HSTS n'est emis que sur les requetes HTTPS (profil prod derriere TLS).
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31_536_000))
                .permissionsPolicy(permissions -> permissions
                        .policy("camera=(), microphone=(), geolocation=()")))
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
