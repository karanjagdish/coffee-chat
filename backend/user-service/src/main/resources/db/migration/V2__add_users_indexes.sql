CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_api_key ON users(api_key);
CREATE INDEX idx_users_is_active ON users(is_active) WHERE is_active = TRUE;

COMMENT ON INDEX idx_users_api_key IS 'Fast lookup for API key authentication';
