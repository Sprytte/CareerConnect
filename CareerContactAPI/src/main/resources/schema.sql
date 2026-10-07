CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(36) UNIQUE NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    profile_picture_url VARCHAR(255) NULL,
    email VARCHAR(255) UNIQUE NULL
);

CREATE TABLE IF NOT EXISTS resumes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(64) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS job_postings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id VARCHAR(32) NOT NULL, -- user who created it
    job_name VARCHAR(32) NOT NULL, -- position
    company VARCHAR(32) NOT NULL,
    description VARCHAR(512) NOT NULL,
    location VARCHAR(64) NOT NULL, -- address or remote
    category VARCHAR(64) NOT NULL, -- full-time/part-time/contract/internship
    salary VARCHAR(64) NOT NULL, -- could be a range
    date_created TIMESTAMP NOT NULL,
    deadline TIMESTAMP NOT NULL
);