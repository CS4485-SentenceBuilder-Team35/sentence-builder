-- AIDEN MARTINEZ
create database IF NOT EXISTS SentenceBuilder;
use SentenceBuilder;

CREATE TABLE IF NOT EXISTS FILES(
	file_id INT PRIMARY KEY AUTO_INCREMENT,
	file_path VARCHAR(500) UNIQUE,
    word_count INT,
    date_imported DATE
);


CREATE TABLE IF NOT EXISTS WORD(
    word_id INT PRIMARY KEY,
    word_token VARCHAR(50),
    total_count INT,
    start_count INT,
    end_count INT,
    type_ ENUM('alpha', 'misc'),
    CHECK (total_count > 0)
);

CREATE TABLE IF NOT EXISTS WORD_FOLLOW(
	from_word_id INT,
    to_word_id INT,
    total_count INT,
    
    PRIMARY KEY(from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE
);
