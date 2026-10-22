package com.example.careercontactapi.UserManagement;

import com.example.careercontactapi.UserManagement.businesslayer.UserServiceImpl;
import com.example.careercontactapi.UserManagement.datalayer.*;
import com.example.careercontactapi.UserManagement.datamapperlayer.*;
import com.example.careercontactapi.UserManagement.presentationlayer.UserUpdateRequestModel;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.datasource.url=jdbc:sqlite::memory:", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserServiceImpl.class, UserRequestMapperImpl.class, UserResponseMapperImpl.class})
class ProfilePersistenceTest {
    @Autowired UserRepository repository;
    @Autowired UserServiceImpl service;
    @Autowired EntityManager entityManager;

    @Test void editedProfileSurvivesClearingThePersistenceContext() {
        repository.saveAndFlush(User.builder().userId("auth0|one").name("Old name").email("old@example.test").build());
        service.updateUser("auth0|one", new UserUpdateRequestModel("Saved name", "saved@example.test", null));
        entityManager.clear();
        var reloaded = service.getUser("auth0|one");
        assertEquals("Saved name", reloaded.getName()); assertEquals("saved@example.test", reloaded.getEmail());
        assertEquals("auth0|one", reloaded.getUserId());
    }
}
