package com.example.careercontactapi.UserManagement.datalayer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adds the nullable role column to databases created before account roles
 * existed. Hibernate runs with {@code ddl-auto=none}, so schema.sql handles new
 * databases while this small migration upgrades an existing SQLite file.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserRoleSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer userTableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'users'",
                    Integer.class
            );
            if (userTableCount == null || userTableCount == 0) {
                // A brand-new database may not have been initialized yet. schema.sql
                // creates the users table with the role column in that case.
                return;
            }
        } catch (DataAccessException exception) {
            log.debug("Could not inspect the users table during role migration", exception);
            return;
        }

        List<String> columns;
        try {
            columns = jdbcTemplate.query(
                    "PRAGMA table_info(users)",
                    (resultSet, rowNumber) -> resultSet.getString("name")
            );
        } catch (DataAccessException exception) {
            log.debug("Users table is not available during role migration", exception);
            return;
        }

        if (columns.stream().anyMatch("role"::equalsIgnoreCase)) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN role VARCHAR(32) NULL");
            log.info("Added role column to the users table");
        } catch (DataAccessException exception) {
            // Another instance may have added the column concurrently, or the
            // database may be read-only. A missing column will surface clearly
            // when the first role query runs, so do not hide the rest of startup.
            log.warn("Could not add role column to the users table", exception);
        }
    }
}
