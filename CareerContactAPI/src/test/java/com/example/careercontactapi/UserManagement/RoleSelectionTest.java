package com.example.careercontactapi.UserManagement;

import com.example.careercontactapi.UserManagement.datalayer.User;
import com.example.careercontactapi.UserManagement.datalayer.UserRepository;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:role-selection-tests?mode=memory&cache=shared",
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
class RoleSelectionTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private ClientRegistrationRepository registrations;

    @BeforeEach
    void clearFixtureProfiles() {
        users.deleteAll();
    }

    private MockHttpSession signedInSession(String subject) {
        var token = OidcIdToken.withTokenValue("fixture-token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject(subject)
                .claim("email", subject.replace('|', '@') + ".example.test")
                .claim("name", "Fixture user")
                .build();
        var principal = new DefaultOidcUser(List.of(), token);
        var authentication = new OAuth2AuthenticationToken(principal, List.of(), "okta");
        var session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication)
        );
        return session;
    }

    private User profile(String subject, UserRole role) {
        return users.saveAndFlush(User.builder()
                .userId(subject)
                .name("Fixture user")
                .email(subject.replace('|', '@') + ".example.test")
                .role(role)
                .build());
    }

    @Test
    void ownerChoosesRoleOnceAndReceivesPermissions() throws Exception {
        profile("auth0|one", null);
        var session = signedInSession("auth0|one");

        mvc.perform(put("/api/v1/users/auth0|one/role")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"role\":\"JOB_SEEKER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("auth0|one"))
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"))
                .andExpect(jsonPath("$.permissions", hasItem("applications:manage")))
                .andExpect(jsonPath("$.permissions", hasItem("resumes:manage")));

        assertEquals(UserRole.JOB_SEEKER, users.findByUserId("auth0|one").orElseThrow().getRole());

        mvc.perform(get("/api/v1/cc/security/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"))
                .andExpect(jsonPath("$.permissions", hasItem("applications:manage")));

        // Repeating the same choice is harmless, but switching is not allowed.
        mvc.perform(put("/api/v1/users/auth0|one/role")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"role\":\"job seeker\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"));

        mvc.perform(put("/api/v1/users/auth0|one/role")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"role\":\"RECRUITER\"}"))
                .andExpect(status().isConflict());

        assertEquals(UserRole.JOB_SEEKER, users.findByUserId("auth0|one").orElseThrow().getRole());
    }

    @Test
    void anotherAccountCannotChooseSomeonesRole() throws Exception {
        profile("auth0|one", null);
        profile("auth0|two", null);
        var session = signedInSession("auth0|one");

        mvc.perform(put("/api/v1/users/auth0|two/role")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"role\":\"RECRUITER\"}"))
                .andExpect(status().isForbidden());

        assertNull(users.findByUserId("auth0|two").orElseThrow().getRole());
    }

    @Test
    void invalidRoleIsRejectedWithoutWriting() throws Exception {
        profile("auth0|one", null);
        var session = signedInSession("auth0|one");

        mvc.perform(put("/api/v1/users/auth0|one/role")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"role\":\"admin\"}"))
                .andExpect(status().isBadRequest());

        assertNull(users.findByUserId("auth0|one").orElseThrow().getRole());
    }

    @Test
    void signedOutCannotChooseARole() throws Exception {
        profile("auth0|one", null);

        mvc.perform(put("/api/v1/users/auth0|one/role")
                        .contentType("application/json")
                        .content("{\"role\":\"RECRUITER\"}"))
                .andExpect(status().isUnauthorized());

        assertNull(users.findByUserId("auth0|one").orElseThrow().getRole());
    }

}
