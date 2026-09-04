package com.akhil.social.repository;

import com.akhil.social.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByOwnerIdOrderByNameAsc(Long ownerId);
}
