CREATE DATABASE IF NOT EXISTS databaseproject CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE databaseproject;

CREATE TABLE IF NOT EXISTS messages (
 id BIGINT NOT NULL AUTO_INCREMENT,
 platform VARCHAR(40) NOT NULL,
 conversation VARCHAR(120) NOT NULL,
 sender VARCHAR(120) NOT NULL,
 content VARCHAR(4000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 mine BIT(1) NOT NULL,
 PRIMARY KEY (id),
 INDEX idx_messages_conversation(platform, conversation, created_at)
);

CREATE TABLE IF NOT EXISTS posts (
 id BIGINT NOT NULL AUTO_INCREMENT,
 platform VARCHAR(40) NOT NULL,
 author VARCHAR(120) NOT NULL,
 content VARCHAR(5000) NOT NULL,
 likes INT NOT NULL DEFAULT 0,
 comments INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY (id),
 INDEX idx_posts_platform(platform, created_at)
);

CREATE TABLE IF NOT EXISTS contacts (
 id BIGINT NOT NULL AUTO_INCREMENT,
 name VARCHAR(120) NOT NULL,
 initials VARCHAR(2) NOT NULL,
 preview VARCHAR(255),
 unread_count INT NOT NULL DEFAULT 0,
 PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS social_actions (
 id BIGINT NOT NULL AUTO_INCREMENT,
 platform VARCHAR(30) NOT NULL,
 entity_type VARCHAR(40) NOT NULL,
 entity_id VARCHAR(100) NOT NULL,
 action_type VARCHAR(40) NOT NULL,
 actor VARCHAR(120) NOT NULL,
 metadata VARCHAR(1000),
 created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY(id), INDEX idx_action_entity(platform,entity_type,entity_id)
);

CREATE TABLE IF NOT EXISTS comments (
 id BIGINT NOT NULL AUTO_INCREMENT,
 post_id BIGINT NOT NULL,
 author VARCHAR(120) NOT NULL,
 content VARCHAR(2000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY(id), INDEX idx_comments_post(post_id,created_at)
);
