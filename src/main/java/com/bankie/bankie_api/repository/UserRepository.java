package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByBsn(String bsn);
    Page findAll(Pageable pageable);
    Page<User> findAllByApproved(boolean approved, Pageable pageable);
}