package com.akhil.social.service;

import com.akhil.social.dto.MessageDtos.*;
import com.akhil.social.entity.*;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository, ConversationRepository conversationRepository,
                          ContactRepository contactRepository, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.contactRepository = contactRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageResponse send(SendMessageRequest req, User user) {
        Conversation conv = conversationRepository
                .findByNameAndPlatformIgnoreCase(req.conversation(), req.platform())
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setName(req.conversation());
                    c.setPlatform(req.platform().toLowerCase());
                    c.setCreatedBy(user);
                    return conversationRepository.save(c);
                });

        Message m = new Message();
        m.setConversation(conv);
        m.setSender(user);
        m.setContent(req.content().trim());
        m.setMediaUrl(req.mediaUrl());
        messageRepository.save(m);

        MessageResponse resp = toResponse(m, user);
        try {
            messagingTemplate.convertAndSend(
                    "/topic/messages/" + req.platform().toLowerCase() + "/" + conv.getId(), resp);
        } catch (Exception ignored) {}
        return resp;
    }

    public List<MessageResponse> list(String platform, String conversationName, User user) {
        Conversation conv = conversationRepository
                .findByNameAndPlatformIgnoreCase(conversationName, platform)
                .orElse(null);
        if (conv == null) return List.of();
        return messageRepository.findRecentByConversation(conv.getId()).stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(m -> toResponse(m, user))
                .collect(Collectors.toList());
    }

    public Page<MessageResponse> listPaged(Long conversationId, int page, int size, User user) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(page, size))
                .map(m -> toResponse(m, user));
    }

    @Transactional
    public void delete(Long id, User user) {
        Message m = messageRepository.findById(id)
                .orElseThrow(() -> new ApiException("Message not found", HttpStatus.NOT_FOUND));
        if (!m.getSender().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new ApiException("Not authorized", HttpStatus.FORBIDDEN);
        }
        messageRepository.delete(m);
    }

    private MessageResponse toResponse(Message m, User current) {
        boolean mine = current != null && m.getSender().getId().equals(current.getId());
        return new MessageResponse(m.getId(), m.getConversation().getId(), m.getSender().getId(),
                m.getSender().getDisplayName(), m.getContent(), m.getMediaUrl(), mine, m.getCreatedAt());
    }
}
