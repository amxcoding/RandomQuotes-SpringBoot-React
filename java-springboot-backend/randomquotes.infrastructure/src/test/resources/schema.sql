-- src/test/resources/schema.sql (H2 compatible)

-- Drop tables in reverse order of dependency, if they exist
DROP TABLE IF EXISTS quote_like;
DROP TABLE IF EXISTS quotes;

-- Create quotes table using H2 syntax for auto-increment
CREATE TABLE quotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author VARCHAR(125) NOT NULL,
    text TEXT NOT NULL,
    likes INT DEFAULT 0 NOT NULL,
    text_author_hash VARCHAR(255) NOT NULL UNIQUE
);

-- Create quote_like table using H2 syntax
CREATE TABLE quote_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    quote_id BIGINT NOT NULL,
    liked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- H2 uses TIMESTAMP
    CONSTRAINT fk_quote FOREIGN KEY(quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_quote UNIQUE (user_id, quote_id)
);

-- Create index
CREATE INDEX idx_quote_like_user_id ON quote_like(user_id);