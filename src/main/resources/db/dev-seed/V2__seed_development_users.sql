INSERT INTO
    application_user (
        id,
        tenant_id,
        username,
        password_hash,
        enabled
    )
VALUES
    (
        'user-001',
        'tenant-a',
        'quince',
        '{bcrypt}$2a$12$iv9dOEz6HbxR0c4pLXrMse59JCtu.A24UwwYsUp9We8xD4NSF.wjW',
        TRUE
    ),
    (
        'user-002',
        'tenant-a',
        'director',
        '{bcrypt}$2a$12$iv9dOEz6HbxR0c4pLXrMse59JCtu.A24UwwYsUp9We8xD4NSF.wjW',
        TRUE
    ),
    (
        'user-003',
        'tenant-b',
        'alice',
        '{bcrypt}$2a$12$iv9dOEz6HbxR0c4pLXrMse59JCtu.A24UwwYsUp9We8xD4NSF.wjW',
        TRUE
    );

INSERT INTO
    application_user_role (user_id, role)
VALUES
    ('user-001', 'LAWYER'),
    ('user-002', 'DIRECTOR'),
    ('user-003', 'LAWYER');