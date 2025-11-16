-- Session indexes
CREATE INDEX idx_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_sessions_created_at ON chat_sessions(created_at DESC);
CREATE INDEX idx_sessions_updated_at ON chat_sessions(updated_at DESC);
CREATE INDEX idx_sessions_favorite ON chat_sessions(is_favorite) 
    WHERE is_favorite = TRUE;

-- Message indexes
CREATE INDEX idx_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_messages_order ON chat_messages(session_id, message_order);
CREATE INDEX idx_messages_context ON chat_messages USING GIN(context);

COMMENT ON INDEX idx_messages_context IS 'GIN index for JSONB queries';
