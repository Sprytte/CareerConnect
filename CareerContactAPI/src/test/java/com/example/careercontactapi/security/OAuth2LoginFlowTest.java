package com.example.careercontactapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite::memory:",
        "okta.oauth2.issuer=https://identity.example.test/",
        "okta.oauth2.client-id=fixture-client",
        "okta.oauth2.client-secret=unused-test-secret",
        "auth0.audience=https://api.example.test/",
        "frontend.url=http://localhost:3000",
        "backend.url=http://localhost:8080/"
})
@AutoConfigureMockMvc
class OAuth2LoginFlowTest {
    @Autowired private MockMvc mvc;
    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private ClientRegistrationRepository registrations;

    @BeforeEach
    void registerFixtureProvider() {
        when(registrations.findByRegistrationId("okta")).thenReturn(
                ClientRegistration.withRegistrationId("okta")
                        .clientId("fixture-client")
                        .clientSecret("unused-test-secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost:8080/login/oauth2/code/okta")
                        .scope("openid")
                        .authorizationUri("https://identity.example.test/authorize")
                        .tokenUri("https://identity.example.test/token")
                        .jwkSetUri("https://identity.example.test/jwks")
                        .userNameAttributeName("sub")
                        .clientName("Fixture provider")
                        .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/login/oauth2/code/okta",
            "/login/oauth2/code/okta?error",
            "/login/oauth2/code/okta?code=expired-code&state=expired-state",
            "/login/oauth2/code/okta?error=access_denied&state=invalid-test-state"
    })
    void incompleteOrExpiredCallbackReturnsToWelcomePage(String callback) throws Exception {
        mvc.perform(get(callback))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:3000/?signin=failed"));
    }

    @Test
    void providerDenialReturnsToWelcomePageWithoutExposingErrorDetails() throws Exception {
        var authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://identity.example.test/authorize")
                .clientId("fixture-client")
                .redirectUri("http://localhost:8080/login/oauth2/code/okta")
                .scope("openid")
                .state("fixture-state")
                .attributes(attributes -> attributes.put("registration_id", "okta"))
                .build();
        var request = new MockHttpServletRequest();
        new HttpSessionOAuth2AuthorizationRequestRepository().saveAuthorizationRequest(
                authorizationRequest, request, new MockHttpServletResponse());

        mvc.perform(get("/login/oauth2/code/okta")
                        .session((MockHttpSession) request.getSession())
                        .param("error", "access_denied")
                        .param("error_description", "Private provider error details")
                        .param("state", "fixture-state"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:3000/?signin=failed"));
    }

    @Test
    void retryStartsANewProviderAuthorizationWithTheExistingCallback() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/okta"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", startsWith("https://identity.example.test/authorize?")))
                .andExpect(header().string("Location", containsString("state=")))
                .andReturn();
        var callback = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
                .build().getQueryParams().getFirst("redirect_uri");
        assertNotNull(callback);
        assertEquals("http://localhost:8080/login/oauth2/code/okta",
                URLDecoder.decode(callback, StandardCharsets.UTF_8));
    }

    @Test
    void signedOutSessionApiStillReturns401WithoutRedirecting() throws Exception {
        mvc.perform(get("/api/v1/cc/security/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
}
