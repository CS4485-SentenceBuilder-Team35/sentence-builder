-- AIDEN MARTINEZ
create database SentenceBuilder;

use SentenceBuilder;

CREATE TABLE IF NOT EXISTS FILES(
	file_id INT PRIMARY KEY AUTO_INCREMENT,
	file_path VARCHAR(50) UNIQUE,
    word_count INT,
    date_imported DATE
);

CREATE TABLE IF NOT EXISTS FILE_TOKEN(
	file_id INT,
    word_id VARCHAR(50),
    word_count INT,
    start_count INT,
    end_count INT,
    type_ ENUM('alpha', 'numeric', 'punc', 'misc'),
    
    PRIMARY KEY(file_id, word_id),
    FOREIGN KEY (file_id) REFERENCES FILES(file_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS WORD(
    word_id VARCHAR(50) PRIMARY KEY UNIQUE,
    total_count INT,
    start_count INT,
    end_count INT,
    CHECK (total_count > 0)
);

CREATE TABLE IF NOT EXISTS FILE_WORD_FOLLOW(
	file_id INT,
    from_word_id VARCHAR(50),
    to_word_id VARCHAR(50),
    total_count INT,
    
    PRIMARY KEY(file_id, from_word_id, to_word_id),
    FOREIGN KEY (file_id) REFERENCES FILES(file_id) ON DELETE CASCADE,
    FOREIGN KEY (from_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS WORD_FOLLOW(
	from_word_id VARCHAR(50),
    to_word_id VARCHAR(50),
    total_count INT,
    
    PRIMARY KEY(from_word_id, to_word_id),
    FOREIGN KEY (from_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE,
    FOREIGN KEY (to_word_id) REFERENCES WORD(word_id) ON DELETE CASCADE
);

-- Trigger to update WORD when values are inserted into FILE_WORDS
DELIMITER $$
CREATE TRIGGER WORD_INSERT_TRIGGER 
AFTER INSERT ON FILE_TOKEN
FOR EACH ROW
BEGIN
	IF EXISTS(
		  SELECT word_id 
			FROM WORD 
            WHERE word_id = NEW.word_id
             )
	THEN
		UPDATE WORD
			SET total_count = total_count + NEW.word_count, start_count = start_count + NEW.start_count, end_count = end_count + NEW.end_count
            WHERE word_id = NEW.word_id;
		
	ELSEIF NEW.type_ = 'alpha'
    THEN
		INSERT INTO WORD(word_id, total_count, start_count, end_count)
			VALUES(NEW.word_id, NEW.word_count, NEW.start_count, NEW.end_count);
END IF;

END $$ 
DELIMITER ;

-- Trigger to update WORD_FOLLOWS when values are inserted into FILE_WORD_FOLLOWS
DELIMITER $$
CREATE TRIGGER WORD_FOLLOWS_INSERT_TRIGGER
AFTER INSERT ON FILE_WORD_FOLLOW
FOR EACH ROW
BEGIN
	IF EXISTS(
		  SELECT from_word_id, to_word_id
			FROM WORD_FOLLOW
            WHERE from_word_id = NEW.from_word_id AND to_word_id = NEW.to_word_id
            )
	THEN 
		UPDATE WORD_FOLLOW
			SET total_count = total_count + NEW.total_count
            WHERE from_word_id = NEW.from_word_id AND to_word_id = NEW.to_word_id;
	ELSE
		INSERT WORD_FOLLOW(from_word_id, to_word_id, total_count)
			VALUES(NEW.from_word_id, NEW.to_word_id, NEW.total_count);
    END IF;
    
END $$
DELIMITER ;

-- Trigger to update FILE_WORDS and FILE_WORDS_FOLLOWS on FILE delete
DELIMITER $$
CREATE TRIGGER FILE_DELETE_TRIGGER
BEFORE DELETE ON FILES
FOR EACH ROW
BEGIN
	DELETE FROM FILE_WORD_FOLLOW
		WHERE file_id = OLD.file_id;
	DELETE FROM FILE_TOKEN
		WHERE file_id = OLD.file_id;
	

END $$ 
DELIMITER ;

-- Trigger to Update WORD when values in FILE_WORDS are deleted
DELIMITER $$
CREATE TRIGGER WORD_DELETE_TRIGGER
AFTER DELETE ON FILE_TOKEN
FOR EACH ROW
BEGIN
	IF EXISTS(
		  SELECT word_id 
			FROM WORD 
            WHERE word_id = OLD.word_id AND (total_count - OLD.word_count <= 0)
             )
	THEN
		DELETE FROM WORD
            WHERE word_id = OLD.word_id;
		
	ELSE
		UPDATE WORD
			SET total_count = total_count - OLD.word_count, start_count = start_count - OLD.start_count, end_count = end_count - OLD.end_count
            WHERE word_id = OLD.word_id;
	END IF;

END $$ 
DELIMITER ;

-- Trigger to update WORD_FOLLOWS when values are deleted from FILE_WORD_FOLLOWS
DELIMITER $$
CREATE TRIGGER WORD_FOLLOWS_DELETE_TRIGGER
AFTER DELETE ON FILE_WORD_FOLLOW
FOR EACH ROW
BEGIN
	IF EXISTS(
		  SELECT from_word_id, to_word_id
			FROM WORD_FOLLOW
            WHERE from_word_id = OLD.from_word_id AND to_word_id = OLD.to_word_id AND total_count = OLD.total_count
            )
	THEN 
		DELETE FROM WORD_FOLLOW
            WHERE from_word_id = OLD.from_word_id AND to_word_id = OLD.to_word_id;

	ELSE
		UPDATE WORD_FOLLOW
			SET total_count = total_count - OLD.total_count
            WHERE from_word_id = OLD.from_word_id AND to_word_id = OLD.to_word_id;
    END IF;
    
END $$
DELIMITER ;
