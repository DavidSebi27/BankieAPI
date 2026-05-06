package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByOwner(User owner);
}