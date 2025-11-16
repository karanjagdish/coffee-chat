CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL CHECK (sender IN ('USER', 'AI')),
    content TEXT NOT NULL,
    context JSONB,
    message_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

COMMENT ON TABLE chat_messages IS 'Individual messages within sessions';
COMMENT ON COLUMN chat_messages.context IS 'RAG context stored as JSONB';
