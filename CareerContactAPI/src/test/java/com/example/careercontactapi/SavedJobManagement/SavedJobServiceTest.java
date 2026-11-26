package com.example.careercontactapi.SavedJobManagement;

import com.example.careercontactapi.JobPostingManagement.datalayer.JobPosting;
import com.example.careercontactapi.JobPostingManagement.datalayer.JobPostingRepository;
import com.example.careercontactapi.SavedJobManagement.businesslayer.SavedJobService;
import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJob;
import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJobRepository;
import com.example.careercontactapi.SavedJobManagement.presentationlayer.SavedJobController;
import com.example.careercontactapi.UserManagement.businesslayer.UserService;
import com.example.careercontactapi.UserManagement.datalayer.UserRole;
import com.example.careercontactapi.UserManagement.presentationlayer.UserRequestModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserResponseModel;
import com.example.careercontactapi.UserManagement.presentationlayer.UserUpdateRequestModel;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Task 9.3: saved jobs are listed, saved and removed for the signed-in user only. */
class SavedJobServiceTest {
    private final SavedJobRepository savedJobs = mock(SavedJobRepository.class);
    private final JobPostingRepository postings = mock(JobPostingRepository.class);
    private final SavedJobService service = new SavedJobService(savedJobs, postings);

    private static final LocalDateTime SAVED_AT = LocalDateTime.of(2026, 11, 9, 18, 0);

    private SavedJob saved(int id, int jobId) {
        return SavedJob.builder().id(id).userId("user|one").jobPostingId(jobId).savedAt(SAVED_AT).build();
    }

    private JobPosting posting(int id) {
        return JobPosting.builder().id(id).userId("recruiter|one").jobName("Developer").company("Fixture employer")
                .location("Montreal").deadline(LocalDateTime.of(2026, 12, 1, 17, 0)).build();
    }

    private int statusOf(Runnable call) {
        return assertThrows(ResponseStatusException.class, call::run).getStatusCode().value();
    }

    @Test
    void listsOnlyTheAuthenticatedUsersSavedJobsWithPostingDetails() {
        when(savedJobs.findAllByUserIdOrderBySavedAtDescIdDesc("user|one")).thenReturn(List.of(saved(1, 20)));
        when(postings.findAllById(anyList())).thenReturn(List.of(posting(20)));

        var result = service.getSavedJobs("user|one");

        assertEquals(1, result.size());
        assertEquals(20, result.get(0).jobId());
        assertEquals(SAVED_AT, result.get(0).savedAt());
        assertEquals("Developer", result.get(0).job().getJobName());
        assertEquals("Fixture employer", result.get(0).job().getCompany());
        verify(savedJobs).findAllByUserIdOrderBySavedAtDescIdDesc("user|one");
        verify(postings).findAllById(List.of(20));
    }

    @Test
    void aDeletedPostingStaysInTheListWithoutDetails() {
        when(savedJobs.findAllByUserIdOrderBySavedAtDescIdDesc("user|one")).thenReturn(List.of(saved(1, 20)));
        when(postings.findAllById(anyList())).thenReturn(List.of());

        var result = service.getSavedJobs("user|one");

        assertEquals(20, result.get(0).jobId());
        assertNull(result.get(0).job());
    }

    @Test
    void anAccountWithNothingSavedGetsAnEmptyList() {
        when(savedJobs.findAllByUserIdOrderBySavedAtDescIdDesc("user|one")).thenReturn(List.of());
        when(postings.findAllById(anyList())).thenReturn(List.of());

        assertTrue(service.getSavedJobs("user|one").isEmpty());
    }

    @Test
    void savingStoresTheJobForTheAuthenticatedUser() {
        when(postings.findById(20)).thenReturn(Optional.of(posting(20)));
        when(savedJobs.findByUserIdAndJobPostingId("user|one", 20)).thenReturn(Optional.empty());
        when(savedJobs.save(any(SavedJob.class))).thenReturn(saved(1, 20));

        var result = service.saveJob("user|one", 20);

        assertEquals(20, result.jobId());
        assertEquals("Developer", result.job().getJobName());
        verify(savedJobs).save(any(SavedJob.class));
    }

    @Test
    void savingTwiceKeepsTheOriginalEntry() {
        when(postings.findById(20)).thenReturn(Optional.of(posting(20)));
        when(savedJobs.findByUserIdAndJobPostingId("user|one", 20)).thenReturn(Optional.of(saved(1, 20)));

        var result = service.saveJob("user|one", 20);

        assertEquals(SAVED_AT, result.savedAt());
        verify(savedJobs, never()).save(any(SavedJob.class));
    }

    @Test
    void anArchivedPostingCannotBeSaved() {
        JobPosting archived = posting(20);
        archived.setArchived(true);
        when(postings.findById(20)).thenReturn(Optional.of(archived));

        assertEquals(400, statusOf(() -> service.saveJob("user|one", 20)));
        verify(savedJobs, never()).save(any(SavedJob.class));
    }

    @Test
    void anArchivedPostingStaysInTheListAndIsMarkedArchived() {
        JobPosting archived = posting(20);
        archived.setArchived(true);
        when(savedJobs.findAllByUserIdOrderBySavedAtDescIdDesc("user|one")).thenReturn(List.of(saved(1, 20)));
        when(postings.findAllById(anyList())).thenReturn(List.of(archived));

        var result = service.getSavedJobs("user|one");

        assertEquals(20, result.get(0).jobId());
        assertTrue(result.get(0).job().isArchived());
    }

    @Test
    void anOpenPostingIsNotMarkedArchived() {
        when(postings.findById(20)).thenReturn(Optional.of(posting(20)));
        when(savedJobs.findByUserIdAndJobPostingId("user|one", 20)).thenReturn(Optional.of(saved(1, 20)));

        assertFalse(service.saveJob("user|one", 20).job().isArchived());
    }

    @Test
    void savingAJobThatDoesNotExistIsNotFound() {
        when(postings.findById(99)).thenReturn(Optional.empty());

        assertEquals(404, statusOf(() -> service.saveJob("user|one", 99)));
        verify(savedJobs, never()).save(any(SavedJob.class));
    }

    @Test
    void removingDeletesOnlyTheAuthenticatedUsersEntry() {
        when(savedJobs.findByUserIdAndJobPostingId("user|one", 20)).thenReturn(Optional.of(saved(1, 20)));

        service.removeSavedJob("user|one", 20);

        verify(savedJobs).findByUserIdAndJobPostingId("user|one", 20);
        verify(savedJobs).delete(any(SavedJob.class));
    }

    @Test
    void removingAJobThatIsNotSavedSucceedsWithoutDeleting() {
        when(savedJobs.findByUserIdAndJobPostingId("user|one", 20)).thenReturn(Optional.empty());

        service.removeSavedJob("user|one", 20);

        verify(savedJobs, never()).delete(any(SavedJob.class));
    }

    @Test
    void absentIdentityCannotReachTheRepositories() {
        assertEquals(401, statusOf(() -> service.getSavedJobs(null)));
        assertEquals(401, statusOf(() -> service.saveJob(" ", 20)));
        assertEquals(401, statusOf(() -> service.removeSavedJob(null, 20)));
        verifyNoInteractions(savedJobs, postings);
    }

    @Test
    void aMissingJobIdIsRejected() {
        assertEquals(400, statusOf(() -> service.saveJob("user|one", null)));
        assertEquals(400, statusOf(() -> service.removeSavedJob("user|one", null)));
        verifyNoInteractions(savedJobs, postings);
    }

    /** Records role checks and refuses every account except "user|one", like a recruiter would be. */
    private static final class FakeUserService implements UserService {
        final List<String> checked = new ArrayList<>();

        @Override
        public void requireRole(String userId, UserRole role) {
            checked.add(userId + ":" + role);
            if (!"user|one".equals(userId) || role != UserRole.JOB_SEEKER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This feature is for job seekers");
            }
        }

        @Override public UserResponseModel addUser(UserRequestModel request) { throw new UnsupportedOperationException(); }
        @Override public UserResponseModel getUser(String userId) { throw new UnsupportedOperationException(); }
        @Override public UserResponseModel updateUser(String userId, UserUpdateRequestModel request) { throw new UnsupportedOperationException(); }
        @Override public UserResponseModel setRole(String userId, UserRole role) { throw new UnsupportedOperationException(); }
        @Override public boolean userExists(String userId) { throw new UnsupportedOperationException(); }
    }

    @Test
    void controllerRefusesAccountsThatAreNotJobSeekers() {
        var backend = mock(SavedJobService.class);
        var users = new FakeUserService();
        var controller = new SavedJobController(backend, users);
        var recruiter = mock(OidcUser.class);
        when(recruiter.getSubject()).thenReturn("recruiter|one");

        assertEquals(403, statusOf(() -> controller.getSavedJobs(recruiter)));
        assertEquals(403, statusOf(() -> controller.saveJob(20, recruiter)));
        assertEquals(403, statusOf(() -> controller.removeSavedJob(20, recruiter)));
        assertEquals(3, users.checked.size());
        verifyNoInteractions(backend);
    }

    @Test
    void controllerRequiresASessionAndDerivesIdentityFromThePrincipal() {
        var backend = mock(SavedJobService.class);
        var users = new FakeUserService();
        var controller = new SavedJobController(backend, users);

        assertEquals(401, controller.getSavedJobs(null).getStatusCode().value());
        assertEquals(401, controller.saveJob(20, null).getStatusCode().value());
        assertEquals(401, controller.removeSavedJob(20, null).getStatusCode().value());
        verifyNoInteractions(backend);

        var principal = mock(OidcUser.class);
        when(principal.getSubject()).thenReturn("user|one");
        when(backend.getSavedJobs("user|one")).thenReturn(List.of());

        var list = controller.getSavedJobs(principal);
        assertEquals(200, list.getStatusCode().value());
        assertEquals("no-store", list.getHeaders().getCacheControl());
        assertEquals(200, controller.saveJob(20, principal).getStatusCode().value());
        assertEquals(204, controller.removeSavedJob(20, principal).getStatusCode().value());
        verify(backend).getSavedJobs("user|one");
        verify(backend).saveJob("user|one", 20);
        verify(backend).removeSavedJob("user|one", 20);
        assertEquals(List.of("user|one:JOB_SEEKER", "user|one:JOB_SEEKER", "user|one:JOB_SEEKER"), users.checked);
    }
}
