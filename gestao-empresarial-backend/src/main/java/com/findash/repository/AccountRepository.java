package com.findash.repository;

import com.findash.entity.Account;
import com.findash.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIdAndCompanyId(UUID id, UUID companyId);

    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.categoryId IN " +
           "(SELECT c.id FROM Category c WHERE c.groupId = :groupId)")
    boolean existsByCategoryGroupId(@Param("groupId") UUID groupId);

    @Modifying
    @Query("UPDATE Account a SET a.status = :newStatus, a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.status = :currentStatus AND a.dueDate < :today AND a.active = true")
    int markOverdue(@Param("currentStatus") AccountStatus currentStatus,
                    @Param("newStatus") AccountStatus newStatus,
                    @Param("today") LocalDate today);
}
