package com.akhil.social.repository;
import com.akhil.social.entity.SocialConnection;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SocialConnectionRepository extends JpaRepository<SocialConnection,Long>{
 Optional<SocialConnection> findByUserIdAndPlatform(Long userId,String platform);
 List<SocialConnection> findAllByUserId(Long userId);
}
