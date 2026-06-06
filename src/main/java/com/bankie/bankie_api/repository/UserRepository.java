package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.User;
import com.bankie.bankie_api.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByBsn(String bsn);
    Page<User> findAllByApproved(boolean approved, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.approved = false AND u.id NOT IN (SELECT a.user.id FROM Account a)")
    Page<User> findByRoleAndNoAccounts(@Param("role") Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.approved = true AND u.id IN (SELECT a.user.id FROM Account a) AND u.id NOT IN (SELECT a.user.id FROM Account a WHERE a.status <> 'CLOSED')")
    Page<User> findByRoleAndAllAccountsClosed(@Param("role") Role role, Pageable pageable);
}