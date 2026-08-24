CREATE TABLE application_user (
    id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_application_user PRIMARY KEY (id),
    CONSTRAINT uk_application_user_username UNIQUE (username),
    INDEX idx_application_user_tenant_id (tenant_id)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_as_cs;

CREATE TABLE application_user_role (
    user_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_application_user_role PRIMARY KEY (user_id, role),
    CONSTRAINT fk_application_user_role_user FOREIGN KEY (user_id) REFERENCES application_user (id) ON DELETE CASCADE,
    CONSTRAINT chk_application_user_role CHECK (role IN ('LAWYER', 'DIRECTOR', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_as_cs;