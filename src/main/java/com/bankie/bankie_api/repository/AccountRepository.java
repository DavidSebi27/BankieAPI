package com.bankie.bankie_api.repository;

import com.bankie.bankie_api.entity.Account;
import com.bankie.bankie_api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByUser(User user);

    Page<Account> findByUserId(Long id, Pageable pageable);
    boolean existsByUser(User user);
    boolean existsByIban(String iban);

    @Query("SELECT a FROM Account a WHERE " +
            "(:first IS NULL OR LOWER(a.user.firstName) LIKE LOWER(CONCAT('%', :first, '%'))) AND " +
            "(:last IS NULL OR LOWER(a.user.lastName) LIKE LOWER(CONCAT('%', :last, '%')))")
    Page<Account> searchByOwnerNames(@Param("first") String firstName,
                                     @Param("last") String lastName,
                                     Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.user.approved = true AND " +
            "(:first IS NULL OR LOWER(a.user.firstName) LIKE LOWER(CONCAT('%', :first, '%'))) AND " +
            "(:last IS NULL OR LOWER(a.user.lastName) LIKE LOWER(CONCAT('%', :last, '%')))")
    Page<Account> searchApprovedByOwnerNames(@Param("first") String firstName,
                                             @Param("last") String lastName,
                                             Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.iban = :iban " +
            "AND a.status = AccountStatus.ACTIVE " +
            "AND a.user.role = Role.CUSTOMER " +
            "AND a.user.approved = true " +
            "AND LOWER(a.user.firstName) = LOWER(:first) " +
            "AND LOWER(a.user.lastName) = LOWER(:last)")
    Optional<Account> findVerifiedRecipient(@Param("iban") String iban,
                                            @Param("first") String first,
                                            @Param("last") String last);
}
