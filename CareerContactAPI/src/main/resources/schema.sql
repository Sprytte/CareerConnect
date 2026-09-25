-- DROP TABLE IF EXISTS users CASCADE;

-- TODO surround field names with "" when it's a reserved keyword
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(36) UNIQUE NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    profile_picture_url VARCHAR(255) NULL,
    email VARCHAR(255) UNIQUE NULL
);
