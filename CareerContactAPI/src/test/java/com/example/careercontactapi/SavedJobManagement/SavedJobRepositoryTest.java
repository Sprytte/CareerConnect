package com.example.careercontactapi.SavedJobManagement;

import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJob;
import com.example.careercontactapi.SavedJobManagement.datalayer.SavedJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {"spring.datasource.url=jdbc:sqlite::memory:", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SavedJobRepositoryTest {
    @Autowired SavedJobRepository repository;

    private SavedJob save(String userId, int jobId, LocalDateTime savedAt) {
        return repository.saveAndFlush(SavedJob.builder().userId(userId).jobPostingId(jobId).savedAt(savedAt).build());
    }

    @Test
    void listFiltersOtherAccountsAndSortsMostRecentFirst() {
        var now = LocalDateTime.now();
        var older = save("one", 1, now.minusDays(2));
        save("two", 2, now.plusDays(1));
        var newer = save("one", 3, now);

        assertEquals(List.of(newer.getId(), older.getId()),
                repository.findAllByUserIdOrderBySavedAtDescIdDesc("one").stream().map(SavedJob::getId).toList());
    }

    @Test
    void lookupIsScopedToTheAccountAndTheJob() {
        var now = LocalDateTime.now();
        var mine = save("one", 1, now);
        save("two", 1, now);

        assertEquals(mine.getId(), repository.findByUserIdAndJobPostingId("one", 1).orElseThrow().getId());
        assertTrue(repository.findByUserIdAndJobPostingId("one", 2).isEmpty());
    }
}
