CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    session_name VARCHAR(255) NOT NULL,
    is_favorite BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

COMMENT ON TABLE chat_sessions IS 'User chat sessions';
COMMENT ON COLUMN chat_sessions.user_id IS 'Foreign key to users.id (logical, not enforced)';
