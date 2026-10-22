package com.example.careercontactapi.UserManagement;

import com.example.careercontactapi.UserManagement.businesslayer.UserServiceImpl;
import com.example.careercontactapi.UserManagement.datalayer.*;
import com.example.careercontactapi.UserManagement.datamapperlayer.*;
import com.example.careercontactapi.UserManagement.presentationlayer.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserResponseMapper mapper = mock(UserResponseMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(repository, mock(UserRequestMapper.class), mapper);

    @Test void trimsAndPersistsBothEditableFields() {
        var user = User.builder().userId("auth0|one").name("Old").email("old@example.test").build();
        when(repository.findByUserId("auth0|one")).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);
        service.updateUser("auth0|one", new UserUpdateRequestModel(" New ", " new@example.test ", null));
        assertEquals("New", user.getName()); assertEquals("new@example.test", user.getEmail());
        verify(repository).saveAndFlush(user);
    }
    @Test void invalidEmailIsRejectedBeforeAnyWrite() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser("auth0|one", new UserUpdateRequestModel("New", "not-an-email", null)));
        assertEquals(400, error.getStatusCode().value()); verifyNoInteractions(repository);
    }
    @Test void passwordInProfileRequestCannotBeSilentlyIgnored() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser("auth0|one", new UserUpdateRequestModel("New", "new@example.test", "LongFixturePassword!7")));
        assertEquals(400, error.getStatusCode().value()); verifyNoInteractions(repository);
    }
    @Test void duplicateEmailReturnsConflict() {
        var user = User.builder().userId("auth0|one").build();
        when(repository.findByUserId("auth0|one")).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("fixture constraint"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.updateUser("auth0|one", new UserUpdateRequestModel("New", "taken@example.test", null)));
        assertEquals(409, error.getStatusCode().value());
    }
}
