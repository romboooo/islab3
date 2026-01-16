CREATE SEQUENCE IF NOT EXISTS dragon_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS person_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS import_history_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS person (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('person_id_seq'),
    name VARCHAR(255) NOT NULL,
    eye_color VARCHAR(50),
    hair_color VARCHAR(50) NOT NULL,
    location_x BIGINT,
    location_y DOUBLE PRECISION,
    location_z BIGINT,
    passport_id VARCHAR(255) NOT NULL UNIQUE,
    nationality VARCHAR(50)
    );

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'person' AND column_name = 'eye_color') THEN
ALTER TABLE person ADD COLUMN eye_color VARCHAR(50);
END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'person' AND column_name = 'location_z') THEN
ALTER TABLE person ADD COLUMN location_z BIGINT;
END IF;

END $$;

CREATE TABLE IF NOT EXISTS dragon (
                                      id BIGINT PRIMARY KEY DEFAULT nextval('dragon_id_seq'),
    name VARCHAR(255) NOT NULL UNIQUE,
    coordinates_x BIGINT,
    coordinates_y DOUBLE PRECISION,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    number_of_treasures BIGINT,
    killer_id BIGINT,
    age BIGINT NOT NULL,
    weight FLOAT NOT NULL,
    color VARCHAR(50),
    dragon_character VARCHAR(50),
    tooth_count FLOAT,
    CONSTRAINT fk_killer FOREIGN KEY (killer_id) REFERENCES person(id) ON DELETE SET NULL,
    CONSTRAINT check_age CHECK (age > 0),
    CONSTRAINT check_weight CHECK (weight > 0)
    );

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'dragon' AND column_name = 'tooth_count') THEN
ALTER TABLE dragon ADD COLUMN tooth_count FLOAT;
END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'dragon' AND column_name = 'dragon_character') THEN
ALTER TABLE dragon ADD COLUMN dragon_character VARCHAR(50);
END IF;

BEGIN
ALTER TABLE dragon ALTER COLUMN age SET NOT NULL;
EXCEPTION
        WHEN others THEN
END;

BEGIN
ALTER TABLE dragon ALTER COLUMN weight SET NOT NULL;
EXCEPTION
        WHEN others THEN
END;
END $$;

CREATE TABLE IF NOT EXISTS import_history (
                                              id BIGINT PRIMARY KEY DEFAULT nextval('import_history_id_seq'),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    records_processed INTEGER,
    filename VARCHAR(255),
    error_message VARCHAR(1000),
    file_object_key VARCHAR(255),
    file_size BIGINT,
    file_url VARCHAR(1000)
    );

CREATE INDEX IF NOT EXISTS idx_person_name ON person(name);
CREATE INDEX IF NOT EXISTS idx_person_passport ON person(passport_id);
CREATE INDEX IF NOT EXISTS idx_dragon_name ON dragon(name);
CREATE INDEX IF NOT EXISTS idx_dragon_weight ON dragon(weight);
CREATE INDEX IF NOT EXISTS idx_dragon_killer ON dragon(killer_id);
CREATE INDEX IF NOT EXISTS idx_import_history_status ON import_history(status);
CREATE INDEX IF NOT EXISTS idx_import_history_filename ON import_history(filename);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE table_name = 'dragon'
                   AND constraint_name = 'fk_killer'
                   AND constraint_type = 'FOREIGN KEY') THEN
ALTER TABLE dragon
    ADD CONSTRAINT fk_killer
        FOREIGN KEY (killer_id) REFERENCES person(id) ON DELETE SET NULL;
END IF;
END $$;

COMMIT;

SELECT
    'Current database state:' as message,
    (SELECT COUNT(*) FROM person) as person_count,
    (SELECT COUNT(*) FROM dragon) as dragon_count,
    (SELECT COUNT(*) FROM import_history) as import_history_count;

-- Вставляем данные в таблицу person (людей)
INSERT INTO person (name, eye_color, hair_color, location_x, location_y, location_z, passport_id, nationality)
VALUES
    ('Иван Петров', 'BLUE', 'BLOND', 100, 200.5, 300, 'AB123456', 'RUSSIAN'),
    ('Мария Сидорова', 'GREEN', 'BLACK', 150, 250.7, 350, 'CD789012', 'GERMAN'),
    ('Алексей Иванов', 'BROWN', 'BROWN', 200, 300.9, 400, 'EF345678', 'FRENCH'),
    ('Екатерина Смирнова', 'BLUE', 'RED', 250, 350.2, 450, 'GH901234', 'JAPANESE'),
    ('Дмитрий Козлов', 'GREEN', 'WHITE', 300, 400.4, 500, 'IJ567890', 'CHINESE'),
    ('Анна Федорова', null, 'BLACK', 350, 450.6, 550, 'KL123456', 'RUSSIAN'),
    ('Сергей Николаев', 'BROWN', 'BLOND', 400, 500.8, 600, 'MN789012', 'SPANISH'),
    ('Ольга Павлова', 'BLUE', 'BROWN', 450, 550.1, 650, 'OP345678', 'ITALIAN'),
    ('Павел Орлов', null, 'RED', 500, 600.3, 700, 'QR901234', 'AMERICAN'),
    ('Татьяна Воробьева', 'GREEN', 'WHITE', 550, 650.5, 750, 'ST567890', 'BRITISH')
    ON CONFLICT (passport_id) DO NOTHING;

-- Вставляем данные в таблицу dragon (драконов)
INSERT INTO dragon (name, coordinates_x, coordinates_y, number_of_treasures, killer_id, age, weight, color, dragon_character, tooth_count)
SELECT
    d.name,
    d.coordinates_x,
    d.coordinates_y,
    d.number_of_treasures,
    p.id as killer_id,
    d.age,
    d.weight,
    d.color,
    d.dragon_character,
    d.tooth_count
FROM (VALUES
          ('Смог', 50, 100.5, 1000, 'AB123456', 150, 2500.5, 'RED', 'CHAOTIC', 120.5),
          ('Фафнир', 100, 150.7, 5000, 'CD789012', 500, 5000.0, 'BLACK', 'WISE', 200.0),
          ('Дрогон', 150, 200.9, 2500, 'EF345678', 80, 1800.3, 'GREEN', 'EVIL', 90.7),
          ('Визерион', 200, 250.2, 3000, 'GH901234', 120, 2200.8, 'WHITE', 'GOOD', 110.2),
          ('Рейгал', 250, 300.4, 1500, 'IJ567890', 60, 1500.6, 'BLUE', 'CHAOTIC_EVIL', 85.3),
          ('Шру', 300, 350.6, 800, 'KL123456', 40, 1200.4, 'YELLOW', 'FICKLE', 70.1),
          ('Торн', 350, 400.8, 3500, null, 200, 3000.2, 'BLACK', 'WISE', 150.0),
          ('Мерион', 400, 450.1, 4200, 'OP345678', 180, 2800.7, 'RED', 'CHAOTIC', 135.8),
          ('Нидхегг', 450, 500.3, 6000, null, 700, 6500.9, 'BROWN', 'EVIL', 250.5),
          ('Глаурунг', 500, 550.5, 5500, 'ST567890', 650, 6200.4, 'GREEN', 'CHAOTIC_EVIL', 230.7)
     ) AS d(name, coordinates_x, coordinates_y, number_of_treasures, killer_passport, age, weight, color, dragon_character, tooth_count)
         LEFT JOIN person p ON p.passport_id = d.killer_passport
    ON CONFLICT (name) DO NOTHING;

-- Вставляем данные в таблицу import_history (историю импорта)
INSERT INTO import_history (start_time, end_time, status, records_processed, filename, error_message, file_object_key, file_size, file_url)
VALUES
    ('2024-01-15 10:00:00', '2024-01-15 10:05:23', 'SUCCESS', 100, 'dragons_20240115.csv', null, 'imports/dragons_20240115.csv', 10240, 'https://storage.example.com/imports/dragons_20240115.csv'),
    ('2024-01-15 11:30:00', '2024-01-15 11:32:45', 'SUCCESS', 50, 'persons_20240115.csv', null, 'imports/persons_20240115.csv', 5120, 'https://storage.example.com/imports/persons_20240115.csv'),
    ('2024-01-15 14:20:00', '2024-01-15 14:22:10', 'SUCCESS', 75, 'mixed_data.csv', null, 'imports/mixed_data.csv', 7680, 'https://storage.example.com/imports/mixed_data.csv'),
    ('2024-01-15 16:00:00', null, 'PROCESSING', null, 'new_data.csv', null, 'imports/new_data.csv', 15360, 'https://storage.example.com/imports/new_data.csv'),
    ('2024-01-14 09:15:00', '2024-01-14 09:15:30', 'FAILED', 0, 'corrupted.csv', 'Invalid file format', 'imports/corrupted.csv', 2048, 'https://storage.example.com/imports/corrupted.csv'),
    ('2024-01-13 13:45:00', '2024-01-13 13:50:15', 'SUCCESS', 200, 'archive_data.csv', null, 'imports/archive_data.csv', 20480, 'https://storage.example.com/imports/archive_data.csv'),
    ('2024-01-12 10:30:00', '2024-01-12 10:35:40', 'SUCCESS', 30, 'small_batch.csv', null, 'imports/small_batch.csv', 3072, 'https://storage.example.com/imports/small_batch.csv'),
    ('2024-01-11 15:20:00', '2024-01-11 15:20:05', 'FAILED', 5, 'partial.csv', 'Database connection lost', 'imports/partial.csv', 1024, 'https://storage.example.com/imports/partial.csv');

-- Дополнительные драконы без убийц (для тестирования NULL значений)
INSERT INTO dragon (name, coordinates_x, coordinates_y, number_of_treasures, age, weight, color, dragon_character, tooth_count)
VALUES
    ('Беззубик', 600, 650.7, 800, 35, 900.5, 'PURPLE', 'GOOD', 65.2),
    ('Изумруд', 650, 700.9, 1200, 45, 1100.3, 'GREEN', 'FICKLE', 72.8),
    ('Огнедышащий', 700, 750.2, 2500, 90, 1900.6, 'ORANGE', 'CHAOTIC', 95.4)
    ON CONFLICT (name) DO NOTHING;

-- Дополнительные люди с различными национальностями
INSERT INTO person (name, eye_color, hair_color, location_x, location_y, location_z, passport_id, nationality)
VALUES
    ('John Smith', 'BLUE', 'BLOND', 600, 700.5, 800, 'UV123456', 'AMERICAN'),
    ('Hans Müller', 'GREEN', 'BROWN', 650, 750.7, 850, 'WX789012', 'GERMAN'),
    ('Pierre Dubois', 'BROWN', 'BLACK', 700, 800.9, 900, 'YZ345678', 'FRENCH'),
    ('Yuki Tanaka', 'BROWN', 'BLACK', 750, 850.2, 950, 'AB987654', 'JAPANESE'),
    ('Wei Chen', 'BLACK', 'BLACK', 800, 900.4, 1000, 'CD543210', 'CHINESE')
    ON CONFLICT (passport_id) DO NOTHING;

CREATE SEQUENCE IF NOT EXISTS distributed_transaction_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE IF NOT EXISTS distributed_transaction (
                                                       id BIGINT PRIMARY KEY DEFAULT nextval('distributed_transaction_id_seq'),
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    metadata TEXT
    );
CREATE INDEX IF NOT EXISTS idx_transaction_status ON distributed_transaction(status);
CREATE INDEX IF NOT EXISTS idx_transaction_created ON distributed_transaction(created_at);