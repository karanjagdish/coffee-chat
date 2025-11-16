CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

COMMENT ON TABLE users IS 'Application users with authentication';
COMMENT ON COLUMN users.created_at IS 'Managed by JPA @CreatedDate';
COMMENT ON COLUMN users.updated_at IS 'Managed by JPA @LastModifiedDate';
