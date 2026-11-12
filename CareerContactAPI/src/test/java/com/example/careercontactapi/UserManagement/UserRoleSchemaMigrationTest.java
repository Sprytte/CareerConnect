package com.example.careercontactapi.UserManagement;

import com.example.careercontactapi.UserManagement.datalayer.UserRoleSchemaMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRoleSchemaMigrationTest {
    @TempDir Path temporaryDirectory;

    private JdbcTemplate legacyDatabase() {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve("legacy.sqlite"));
        var jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id VARCHAR(36) UNIQUE NOT NULL,
                    "name" VARCHAR(255) NOT NULL,
                    profile_picture_url VARCHAR(255) NULL,
                    email VARCHAR(255) UNIQUE NULL
                )
                """);
        return jdbcTemplate;
    }

    private List<String> columns(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.query(
                "PRAGMA table_info(users)",
                (resultSet, rowNumber) -> resultSet.getString("name")
        );
    }

    @Test
    void addsRoleColumnToAnExistingUsersTableAndIsIdempotent() {
        var jdbcTemplate = legacyDatabase();
        assertTrue(columns(jdbcTemplate).stream().noneMatch("role"::equalsIgnoreCase));

        new UserRoleSchemaMigration(jdbcTemplate)
                .run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, columns(jdbcTemplate).stream().filter("role"::equalsIgnoreCase).count());

        // The migration runs on every startup, so a second run must not fail.
        new UserRoleSchemaMigration(jdbcTemplate)
                .run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1, columns(jdbcTemplate).stream().filter("role"::equalsIgnoreCase).count());
    }
}
