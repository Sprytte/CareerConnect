package com.example.careercontactapi.UserManagement.datalayer;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

public final class UserConstraintViolations {
    private UserConstraintViolations() {
    }

    public static boolean isUniqueViolation(Throwable failure) {
        // Hibernate can wrap SQLite uniqueness errors in JpaSystemException
        // rather than Spring's DataIntegrityViolationException.
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLiteException sqlite
                    && sqlite.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
                return true;
            }
        }
        return false;
    }
}
