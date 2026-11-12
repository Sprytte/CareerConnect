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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:role-enforcement-tests?mode=memory&cache=shared",
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
class RoleEnforcementTest {
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

    private void profile(String subject, UserRole role) {
        users.saveAndFlush(User.builder()
                .userId(subject)
                .name("Fixture user")
                .email(subject.replace('|', '@') + ".example.test")
                .role(role)
                .build());
    }

    @Test
    void recruiterOnlyEndpointRejectsJobSeeker() throws Exception {
        profile("auth0|one", UserRole.JOB_SEEKER);
        var session = signedInSession("auth0|one");

        mvc.perform(post("/api/v1/job-postings")
                        .session(session)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void jobSeekerOnlyEndpointRejectsRecruiter() throws Exception {
        profile("auth0|one", UserRole.RECRUITER);
        var session = signedInSession("auth0|one");

        mvc.perform(get("/api/v1/account/applications").session(session))
                .andExpect(status().isForbidden());
    }
}
