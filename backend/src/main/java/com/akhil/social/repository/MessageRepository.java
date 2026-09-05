package com.akhil.social.repository;

import com.akhil.social.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender JOIN FETCH m.conversation WHERE m.conversation.id = :cid ORDER BY m.createdAt DESC")
    List<Message> findRecentByConversation(@Param("cid") Long conversationId);
}
