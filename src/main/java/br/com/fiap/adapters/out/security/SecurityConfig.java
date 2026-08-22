package br.com.fiap.adapters.out.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Libera as rotas da documentação do Swagger
                    req.requestMatchers(
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-ui/index.html",
                        "/swagger-ui/index.html/**",
                        "/webjars/**"
                    ).permitAll();
                    
                    // Libera a rota de autenticação/login
                    req.requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll();
                    
                    // Libera a página pública de acompanhamento/aprovação/avaliação de OS (chave de acesso própria da OS)
                    req.requestMatchers(HttpMethod.GET, "/acompanhamento/*").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/acompanhamento/*/aprovar").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/acompanhamento/*/avaliar").permitAll();
                    
                    // Exige autenticação (token JWT) para todas as outras rotas
                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
