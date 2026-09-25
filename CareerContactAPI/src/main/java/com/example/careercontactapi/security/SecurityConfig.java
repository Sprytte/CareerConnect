package com.example.careercontactapi.security;
//
import com.example.careercontactapi.security.filters.CSRFCustomFilter;
import com.example.careercontactapi.security.filters.SpaCSRFToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Generated
@RequiredArgsConstructor
public class SecurityConfig {
    @Value("${okta.oauth2.issuer}")
    private String issuer;
    @Value("${okta.oauth2.client-id}")
    private String clientId;
    @Value("${auth0.audience}")
    private String audience;

    @Value("${frontend.url}")
    private String frontendDomain;

    @Value("${backend.url}")
    private String backendDomain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        //todo These should be uncommented when the authentication works. It specifies which endpoints can be accessed by who
//                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/v1/suppliers")).hasRole("Admin")
//                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET,"/api/v1/suppliers")).hasAuthority("SCOPE_read:suppliers")
//                        .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET,"https://cc-images.amazonaws.com/**")).permitAll()
                        // .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST,"/api/v1/users/**")).authenticated()
                        // .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET,"/api/v1/users/**")).authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling.authenticationEntryPoint(authenticationEntryPoint());
                })
                .oauth2Login(httpSecurityOAuth2LoginConfigurer -> {
                    httpSecurityOAuth2LoginConfigurer.loginPage("/login/oauth2/code/okta")
                            .defaultSuccessUrl(backendDomain + "api/v1/cc/security/redirect", true)
                            .permitAll();
                })
                .logout(logout -> {
                    logout
                            .logoutUrl("/api/v1/careercontact/logout")
                            .addLogoutHandler(logoutHandler())
                            .logoutSuccessHandler((request, response, authentication) -> {

                                Arrays.stream(request.getCookies()).toList().forEach(cookie -> {
                                    if (!cookie.getName().equals("JSESSIONID")) {
                                        Cookie newCookie = new Cookie(cookie.getName(), "");
                                        newCookie.setMaxAge(0);
                                        newCookie.setPath("/");
                                        newCookie.setDomain(getFormattedDomain());
                                        response.addCookie(newCookie);

                                    }
                                });
                                response.setStatus(HttpStatus.OK.value());
                            });
                })
                .csrf(AbstractHttpConfigurer::disable
//                        (csrf) -> csrf
//                        .csrfTokenRepository(csrfTokenRepository())
//                        .csrfTokenRequestHandler(new SpaCSRFToken())
//                        .ignoringRequestMatchers(
//                                new AntPathRequestMatcher("/api/v1/careercontact/logout", HttpMethod.POST.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/redirect", HttpMethod.GET.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/employees", HttpMethod.GET.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/employees", HttpMethod.POST.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/employees/{id}", HttpMethod.PATCH.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/user-info/{id}", HttpMethod.PATCH.toString()),
//                                new AntPathRequestMatcher("/api/v1/cc/security/deleteAccount/users/{userId}", HttpMethod.DELETE.toString()))
//                        )
              
                )
                .cors(httpSecurityCorsConfigurer -> {
                    final var cors = new CorsConfiguration();
                    cors.setAllowedOrigins(List.of(frontendDomain));
                    cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
                    cors.setAllowedHeaders(Arrays.asList("authorization", "content-type", "xsrf-token", "token", "amount"));
                    cors.setExposedHeaders(List.of("xsrf-token"));
                    cors.setAllowCredentials(true);
                    cors.setMaxAge(3600L);

//                    httpSecurityCorsConfigurer.configurationSource(request -> cors);
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
//                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
//                .addFilterAfter(new CSRFCustomFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");

        config.setAllowedOrigins(Arrays.asList(frontendDomain, backendDomain));
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedHeader("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedHeader("*");
        config.addAllowedHeader("Authorization");
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            final String json;
            final ErrorMessage errorMessage;
            //this is where the error is FUCK YOU NULL ARRAY
//            if (Arrays.stream(request.getCookies()).anyMatch(cookie -> cookie.getName().equals("isAuthenticated"))) {
//
//                errorMessage = ErrorMessage.from("Your session has expired.");
//
//            } else {
                errorMessage = ErrorMessage.from(authException.getMessage());
//            }

            json = objectMapper.writeValueAsString(errorMessage);

            log.error("Error: {}", json);
            log.error("Error: {}", authException.getMessage());
            log.error("Error: {}", authException.getLocalizedMessage());


            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(json);
            response.flushBuffer();
        };
    }

    private LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            List<Cookie> cookies = List.of(request.getCookies());

            cookies.forEach(cookie -> {
                if (!cookie.getName().equals("JSESSIONID")) {
                    Cookie newCookie = new Cookie(cookie.getName(), "");
                    newCookie.setMaxAge(0);
                    newCookie.setPath("/");
                    newCookie.setDomain(getFormattedDomain());
                    response.addCookie(newCookie);
                }
            });

            try {
                response.sendRedirect(issuer + "v2/logout?client_id=" + clientId + "&returnTo=" + frontendDomain);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidation(clientId);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    public String getFormattedDomain() {
        String url = frontendDomain.replace("https://", "").replace("http://", "")
                .split(":")[0].replace("/", "").replace("www.", "");

        log.debug("Formatted domain: " + url);

        return url;
    }
}
