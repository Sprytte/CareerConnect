package com.example.careercontactapi.UserManagement;

import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.presentationlayer.UserController;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import com.example.careercontactapi.security.SecurityController;
import com.example.careercontactapi.security.service.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.List;
import org.springframework.mock.web.MockHttpSession;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileControllerTest {
    private final UserService users = mock(UserService.class);
    private final Auth0PasswordService passwords = mock(Auth0PasswordService.class);
    private final Auth0ManagementService management = mock(Auth0ManagementService.class);
    private MockMvc mvc;

    @BeforeEach void setUp() {
        SecurityContextHolder.clearContext();
        mvc = MockMvcBuilders.standaloneSetup(new UserController(users, passwords),
                new SecurityController(mock(Auth0LoginService.class), management, users, "http://localhost:8080/"))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void signIn(String id) {
        var token = OidcIdToken.withTokenValue("fixture-token").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).subject(id).claim("name", "Stale token name").build();
        var user = new DefaultOidcUser(List.of(), token);
        SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(user, List.of(), "okta"));
    }

    @Test void signedOutCannotReadProfile() throws Exception {
        mvc.perform(get("/api/v1/users/auth0|one")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }
    @Test void signedOutCannotUpdateProfile() throws Exception {
        mvc.perform(put("/api/v1/users/auth0|one").contentType("application/json")
                .content("{\"name\":\"New\",\"email\":\"new@example.test\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }
    @Test void anotherAccountCannotReadOrWriteProfile() throws Exception {
        signIn("auth0|one");
        mvc.perform(get("/api/v1/users/auth0|two")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/auth0|two").contentType("application/json")
                .content("{\"name\":\"New\",\"email\":\"new@example.test\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }
    @Test void ownerCanLoadAndUpdateProfile() throws Exception {
        signIn("auth0|one");
        var saved = UserResponseModel.builder().userId("auth0|one").name("New name").email("new@example.test").build();
        when(users.getUser("auth0|one")).thenReturn(saved);
        when(users.updateUser(eq("auth0|one"), any())).thenReturn(saved);
        mvc.perform(get("/api/v1/users/auth0|one")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New name"));
        mvc.perform(put("/api/v1/users/auth0|one").contentType("application/json")
                .content("{\"name\":\"New name\",\"email\":\"new@example.test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.test"));
    }
    @Test void meUsesPersistedProfileInsteadOfStaleTokenClaims() throws Exception {
        signIn("auth0|one");
        when(users.getUser("auth0|one")).thenReturn(UserResponseModel.builder()
                .userId("auth0|one").name("Saved name").email("saved@example.test").build());
        mvc.perform(get("/api/v1/cc/security/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Saved name")).andExpect(jsonPath("$.email").value("saved@example.test"));
    }
    @Test void passwordChangeRequiresOwnerAndNeverReturnsPassword() throws Exception {
        signIn("auth0|one");
        mvc.perform(put("/api/v1/users/auth0|two/password").contentType("application/json")
                .content("{\"password\":\"LongFixturePassword!7\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(passwords);
        var session = new MockHttpSession();
        mvc.perform(put("/api/v1/users/auth0|one/password").session(session).contentType("application/json")
                .content("{\"password\":\"LongFixturePassword!7\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(passwords).changePassword("auth0|one", "LongFixturePassword!7");
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }
    @Test void legacyProfileRoutesCannotBypassOwnership() throws Exception {
        signIn("auth0|one");
        mvc.perform(get("/api/v1/cc/security/user-info/auth0|two")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/cc/security/deleteAccount/users/auth0|two")).andExpect(status().isForbidden());
        verifyNoInteractions(management);
    }
    @Test void ordinaryUserCannotUseLegacyEmployeeAdministration() throws Exception {
        signIn("auth0|one");
        mvc.perform(get("/api/v1/cc/security/employees")).andExpect(status().isForbidden());
        verifyNoInteractions(management);
    }
    @Test void loginRedirectDoesNotPutIdentityTokenIntoReadableCookie() {
        signIn("auth0|one");
        var principal = (org.springframework.security.oauth2.core.oidc.user.OidcUser)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var response = new Auth0LoginService().getVoidResponseEntity(principal);
        var cookies = response.getHeaders().get(org.springframework.http.HttpHeaders.SET_COOKIE);
        org.junit.jupiter.api.Assertions.assertTrue(cookies.stream().noneMatch(cookie -> cookie.contains("fixture-token")));
        org.junit.jupiter.api.Assertions.assertTrue(cookies.stream().anyMatch(cookie -> cookie.startsWith("id_token=") && cookie.contains("Max-Age=0") && cookie.contains("HttpOnly")));
    }
}
