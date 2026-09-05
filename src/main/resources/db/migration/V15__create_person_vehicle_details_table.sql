CREATE TABLE person_vehicle_details (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    detail_date     DATE          NOT NULL,
    person_id       BIGINT        NOT NULL,
    vehicle_id      BIGINT        NOT NULL,
    file_history_id BIGINT        NULL,
    created_by      VARCHAR(50),
    created_date    DATETIME(6)   NOT NULL,
    modified_by     VARCHAR(50),
    modified_date   DATETIME(6)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    deleted         TINYINT(1)    NOT NULL DEFAULT 0,

    CONSTRAINT fk_pvd_person FOREIGN KEY (person_id) REFERENCES person (id),
    CONSTRAINT fk_pvd_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (id),
    CONSTRAINT fk_pvd_file_history FOREIGN KEY (file_history_id) REFERENCES file_history (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pvd_person ON person_vehicle_details (person_id);
CREATE INDEX idx_pvd_vehicle ON person_vehicle_details (vehicle_id);
CREATE INDEX idx_pvd_date ON person_vehicle_details (detail_date);
CREATE INDEX idx_pvd_deleted ON person_vehicle_details (deleted);
CREATE INDEX idx_pvd_file_history ON person_vehicle_details (file_history_id);