package com.example.careercontactapi.security;

import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:profile-sign-in-tests?mode=memory&cache=shared",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.sql.init.mode=always",
        "spring.jpa.hibernate.ddl-auto=none",
        "okta.oauth2.issuer=https://identity.example.test/",
        "okta.oauth2.client-id=fixture-client",
        "okta.oauth2.client-secret=unused-test-secret",
        "auth0.audience=https://api.example.test/",
        "frontend.url=http://localhost:3000",
        "backend.url=http://localhost:8080/"
})
@AutoConfigureMockMvc
class ProfileSignInTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private ClientRegistrationRepository registrations;

    @BeforeEach
    void clearFixtureProfiles() {
        users.deleteAll();
    }

    private MockHttpSession signedInSession(String subject, String email) {
        var token = OidcIdToken.withTokenValue("fixture-token")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .subject(subject).claim("email", email).claim("name", "Token name")
                .claim("picture", "https://example.test/avatar.png").build();
        var principal = new DefaultOidcUser(List.of(), token);
        var authentication = new OAuth2AuthenticationToken(principal, List.of(), "okta");
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        return session;
    }

    private User existingProfile(String subject, String email) {
        return users.saveAndFlush(User.builder().userId(subject)
                .name("Saved profile name").email(email).build());
    }

    @Test
    void anotherIdentityWithTheSameEmailCannotTakeOverTheExistingProfile() throws Exception {
        var original = existingProfile("google-oauth2|original", "shared@example.test");
        var session = signedInSession("auth0|separate", "shared@example.test");

        mvc.perform(get("/api/v1/cc/security/redirect").session(session))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:3000/?signin=profile-unavailable"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));

        assertTrue(session.isInvalid());
        assertEquals(1, users.count());
        assertTrue(users.findByUserId("auth0|separate").isEmpty());
        var preserved = users.findByUserId("google-oauth2|original").orElseThrow();
        assertEquals(original.getId(), preserved.getId());
        assertEquals("Saved profile name", preserved.getName());
        assertEquals("shared@example.test", preserved.getEmail());
        mvc.perform(get("/api/v1/cc/security/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void differentEmailsCreateSeparateProfiles() throws Exception {
        var original = existingProfile("google-oauth2|original", "first@example.test");
        var session = signedInSession("auth0|separate", "second@example.test");

        mvc.perform(get("/api/v1/cc/security/redirect").session(session))
                .andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost:3000/"));

        assertFalse(session.isInvalid());
        assertEquals(2, users.count());
        var second = users.findByUserId("auth0|separate").orElseThrow();
        assertNotEquals(original.getId(), second.getId());
        assertEquals("second@example.test", second.getEmail());
        mvc.perform(get("/api/v1/cc/security/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("auth0|separate"));
    }

    @Test
    void returningIdentityKeepsItsSavedContactDetails() throws Exception {
        existingProfile("google-oauth2|original", "edited-contact@example.test");
        var session = signedInSession("google-oauth2|original", "old-token-email@example.test");

        mvc.perform(get("/api/v1/cc/security/redirect").session(session))
                .andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost:3000/"));

        assertEquals(1, users.count());
        mvc.perform(get("/api/v1/cc/security/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Saved profile name"))
                .andExpect(jsonPath("$.email").value("edited-contact@example.test"));
    }

    @Test
    void duplicateContactEmailUpdateReturnsConflictAndPreservesBothProfiles() throws Exception {
        existingProfile("google-oauth2|original", "taken@example.test");
        existingProfile("auth0|separate", "own@example.test");
        var session = signedInSession("auth0|separate", "own@example.test");

        mvc.perform(put("/api/v1/users/{userId}", "auth0|separate").session(session)
                        .contentType("application/json")
                        .content("{\"name\":\"Unsaved name\",\"email\":\"taken@example.test\"}"))
                .andExpect(status().isConflict());

        assertEquals(2, users.count());
        assertEquals("taken@example.test", users.findByUserId("google-oauth2|original").orElseThrow().getEmail());
        mvc.perform(get("/api/v1/cc/security/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Saved profile name"))
                .andExpect(jsonPath("$.email").value("own@example.test"));
    }
}
