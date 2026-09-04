package com.akhil.social.repository;

import com.akhil.social.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByPlatformIgnoreCase(String platform);
    Optional<Conversation> findByNameAndPlatformIgnoreCase(String name, String platform);
}
