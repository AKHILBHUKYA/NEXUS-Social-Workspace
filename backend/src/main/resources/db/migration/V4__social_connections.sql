CREATE TABLE IF NOT EXISTS social_connections (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 platform VARCHAR(30) NOT NULL,
 external_id VARCHAR(255),
 display_name VARCHAR(255),
 access_token VARCHAR(4000),
 refresh_token VARCHAR(4000),
 token_expires_at TIMESTAMP WITH TIME ZONE,
 metadata_json TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uk_social_user_platform UNIQUE(user_id, platform)
);
CREATE INDEX IF NOT EXISTS idx_social_connections_user ON social_connections(user_id);
