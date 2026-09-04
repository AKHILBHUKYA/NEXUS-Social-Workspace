package com.akhil.social.controller;

import com.akhil.social.entity.Contact;
import com.akhil.social.entity.User;
import com.akhil.social.exception.ApiException;
import com.akhil.social.repository.ContactRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private final ContactRepository contactRepository;
    public ContactController(ContactRepository contactRepository) { this.contactRepository = contactRepository; }

    public record ContactRequest(@NotBlank String name, String initials, String preview, String platform) {}
    public record ContactResponse(Long id, String name, String initials, String preview, String platform) {}

    @GetMapping
    public List<ContactResponse> list(@AuthenticationPrincipal User user) {
        return contactRepository.findByOwnerIdOrderByNameAsc(user.getId()).stream()
                .map(c -> new ContactResponse(c.getId(), c.getName(), c.getInitials(), c.getPreview(), c.getPlatform()))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ContactResponse create(@RequestBody ContactRequest req, @AuthenticationPrincipal User user) {
        Contact c = new Contact();
        c.setOwner(user);
        c.setName(req.name().trim());
        c.setInitials(req.initials() != null ? req.initials() : initials(req.name()));
        c.setPreview(req.preview());
        c.setPlatform(req.platform() != null ? req.platform() : "whatsapp");
        contactRepository.save(c);
        return new ContactResponse(c.getId(), c.getName(), c.getInitials(), c.getPreview(), c.getPlatform());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Contact c = contactRepository.findById(id)
                .orElseThrow(() -> new ApiException("Contact not found", HttpStatus.NOT_FOUND));
        if (!c.getOwner().getId().equals(user.getId())) {
            throw new ApiException("Not authorized", HttpStatus.FORBIDDEN);
        }
        contactRepository.delete(c);
        return Map.of("success", true);
    }

    private String initials(String name) {
        String[] p = name.trim().split("\\s+");
        if (p.length >= 2) return ("" + p[0].charAt(0) + p[1].charAt(0)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
