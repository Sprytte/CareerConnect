package com.example.careercontactapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
class CareerContactApiApplicationTests {
    @MockBean JwtDecoder jwtDecoder;
    @MockBean ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void contextLoads() {
    }

}
