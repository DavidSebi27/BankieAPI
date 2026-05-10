package com.bankie.bankie_api.repository;


import com.bankie.bankie_api.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Page<Account> findByUserId(Long id, Pageable pageable);
}