package com.akhil.social.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="social_connections", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","platform"}))
public class SocialConnection {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false, length=30) private String platform;
    @Column(name="external_id", length=255) private String externalId;
    @Column(name="display_name", length=255) private String displayName;
    @Column(name="access_token", length=4000) private String accessToken;
    @Column(name="refresh_token", length=4000) private String refreshToken;
    @Column(name="token_expires_at") private Instant tokenExpiresAt;
    @Column(name="metadata_json", columnDefinition="TEXT") private String metadataJson;
    @Column(nullable=false) private boolean active=true;
    @Column(name="created_at", nullable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at", nullable=false) private Instant updatedAt=Instant.now();
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getPlatform(){return platform;} public void setPlatform(String v){platform=v;}
    public String getExternalId(){return externalId;} public void setExternalId(String v){externalId=v;}
    public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
    public String getAccessToken(){return accessToken;} public void setAccessToken(String v){accessToken=v;}
    public String getRefreshToken(){return refreshToken;} public void setRefreshToken(String v){refreshToken=v;}
    public Instant getTokenExpiresAt(){return tokenExpiresAt;} public void setTokenExpiresAt(Instant v){tokenExpiresAt=v;}
    public String getMetadataJson(){return metadataJson;} public void setMetadataJson(String v){metadataJson=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
