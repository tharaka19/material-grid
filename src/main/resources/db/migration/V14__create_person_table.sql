CREATE TABLE person (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_code     VARCHAR(20)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    person_type     VARCHAR(30)  NOT NULL,
    created_by        VARCHAR(50),
    created_date      DATETIME(6)  NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,

    CONSTRAINT uk_person_person_code UNIQUE (person_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- person_code is already unique-indexed above, which also covers lookup by code.
CREATE INDEX idx_person_name ON person (name);

-- Seed the code-sequence counter for Person, using the same mechanism
-- already established for ROUTE_CODE/LICENSE_CODE (see V4 migration and
-- CodeGeneratorService). The generator only ever SELECTs ... FOR UPDATE and
-- UPDATEs an existing row - it never inserts - so this row must exist
-- before the first Person code is issued.
INSERT INTO code_sequences (sequence_name, next_value) VALUES ('PERSON_CODE', 1);