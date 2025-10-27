-- AIDEN MARTINEZ
drop database IF EXISTS SentenceBuilder;
create database IF NOT EXISTS SentenceBuilder;
use SentenceBuilder;
CREATE TABLE IF NOT EXISTS FILES(
    file_id INT PRIMARY KEY AUTO_INCREMENT,
    file_path VARCHAR(500) UNIQUE,
    word_count INT,
    date_imported DATE
);
CREATE TABLE IF NOT EXISTS WORD(
    word_id INT PRIMARY KEY AUTO_INCREMENT,
    word_token VARCHAR(50) NOT NULL UNIQUE,
    total_count INT NOT NULL DEFAULT 0,
    start_count INT NOT NULL DEFAULT 0,
    end_count INT NOT NULL DEFAULT 0,
    type_ ENUM('alpha', 'misc') NOT NULL
);
CREATE TABLE IF NOT EXISTS WORD_FOLLOW(
    from_word_id INT NOT NULL,
    to_word_id INT NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY(from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE
);