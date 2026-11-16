package com.example.careercontactapi.ResumeFeedbackManagement.presentationlayer;

import com.example.careercontactapi.ResumeFeedbackManagement.businesslayer.ResumeFeedbackService;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SavedResumeFeedbackControllerTest {

    private final ResumeFeedbackService feedback = mock(ResumeFeedbackService.class);
    private final UserService users = mock(UserService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mvc = MockMvcBuilders.standaloneSetup(new SavedResumeFeedbackController(feedback, users))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(String id) {
        var token = OidcIdToken.withTokenValue("fixture-token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .subject(id)
                .claim("name", "Fixture user")
                .build();
        var user = new DefaultOidcUser(List.of(), token);
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(user, List.of(), "okta"));
    }

    @Test
    void signedOutUserCannotRequestFeedback() throws Exception {
        mvc.perform(post("/api/v1/resumes/1/feedback"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(users, feedback);
    }

    @Test
    void jobSeekerGetsFeedbackForTheirSavedResume() throws Exception {
        signIn("auth0|one");
        when(feedback.requestFeedbackForResume("auth0|one", 1))
                .thenReturn(new ResumeFeedbackResponseModel(
                        "resume.pdf", "Clear and focused.", List.of("Good projects"), List.of("Add metrics")));

        mvc.perform(post("/api/v1/resumes/1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("resume.pdf"))
                .andExpect(jsonPath("$.summary").value("Clear and focused."))
                .andExpect(jsonPath("$.strengths[0]").value("Good projects"))
                .andExpect(jsonPath("$.improvements[0]").value("Add metrics"));

        verify(users).requireRole("auth0|one", UserRole.JOB_SEEKER);
        verify(feedback).requestFeedbackForResume("auth0|one", 1);
    }

    @Test
    void accountScopedPathIsAlsoSupported() throws Exception {
        signIn("auth0|one");
        when(feedback.requestFeedbackForResume("auth0|one", 2))
                .thenReturn(new ResumeFeedbackResponseModel(
                        "cv.docx", "Solid.", List.of(), List.of()));

        mvc.perform(post("/api/v1/account/resumes/2/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("cv.docx"));

        verify(feedback).requestFeedbackForResume("auth0|one", 2);
    }

    @Test
    void recruiterIsRejectedBeforeAnyResumeIsRead() throws Exception {
        signIn("auth0|one");
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Job seeker account required"))
                .when(users).requireRole("auth0|one", UserRole.JOB_SEEKER);

        mvc.perform(post("/api/v1/resumes/1/feedback"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedback);
    }

    @Test
    void missingResumeIsNotFound() throws Exception {
        signIn("auth0|one");
        when(feedback.requestFeedbackForResume(any(), any(Integer.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));

        mvc.perform(post("/api/v1/resumes/99/feedback"))
                .andExpect(status().isNotFound());
    }
}
