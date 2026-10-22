package com.example.careercontactapi.JobApplicationManagement;

import com.example.careercontactapi.JobApplicationManagement.datalayer.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.datasource.url=jdbc:sqlite::memory:", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountApplicationRepositoryTest {
    @Autowired JobApplicationRepository repository;

    @Test void queryFiltersOtherAccountsAndSortsMostRecentFirst() {
        var now = LocalDateTime.now();
        var older = repository.saveAndFlush(JobApplication.builder().jobPostingId(1).userId("one").resumeId(1).status("SUBMITTED").appliedAt(now.minusDays(2)).build());
        repository.saveAndFlush(JobApplication.builder().jobPostingId(2).userId("two").resumeId(2).status("Interview").appliedAt(now.plusDays(1)).build());
        var newer = repository.saveAndFlush(JobApplication.builder().jobPostingId(3).userId("one").resumeId(1).status("Offered").appliedAt(now).build());
        assertEquals(java.util.List.of(newer.getId(), older.getId()), repository.findAllByUserIdOrderByAppliedAtDescIdDesc("one").stream().map(JobApplication::getId).toList());
    }
}
