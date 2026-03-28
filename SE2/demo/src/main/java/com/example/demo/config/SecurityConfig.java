package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/register", "/login").permitAll()
                
                // Admin-only routes
                .requestMatchers("/users/**", "/users").hasRole("ADMIN")
                .requestMatchers("/admin/reports/**", "/admin/reports").hasRole("ADMIN")
                
                // Admin + Academic Staff routes
                .requestMatchers("/enrollments/**", "/enrollments").hasAnyRole("ADMIN", "ACADEMIC_STAFF")
                .requestMatchers("/assessments/**", "/assessments").hasAnyRole("ADMIN", "ACADEMIC_STAFF")
                .requestMatchers("/errors/**", "/errors").hasAnyRole("ADMIN", "ACADEMIC_STAFF")
                .requestMatchers("/students/**", "/students").hasAnyRole("ADMIN", "ACADEMIC_STAFF")
                
                // Portal (Student) routes
                .requestMatchers("/portal/**", "/portal", "/student/**", "/student").hasRole("STUDENT")
                
                // General authenticated routes
                .requestMatchers("/courses/**", "/courses", "/profile/**", "/profile").authenticated()
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/logout")
            )
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()))
            .authenticationProvider(authenticationProvider());
            
        return http.build();
    }
}
