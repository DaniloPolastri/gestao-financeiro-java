package com.findash.repository;

import com.findash.entity.Account;
import com.findash.entity.AccountStatus;
import com.findash.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsBySupplierId(UUID supplierId);

    boolean existsByClientId(UUID clientId);

    boolean existsByCompanyIdAndDueDateAndAmountAndDescription(
        UUID companyId, java.time.LocalDate dueDate,
        java.math.BigDecimal amount, String description);

    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.categoryId IN " +
           "(SELECT c.id FROM Category c WHERE c.groupId = :groupId)")
    boolean existsByCategoryGroupId(@Param("groupId") UUID groupId);

    @Modifying
    @Query("UPDATE Account a SET a.status = :newStatus, a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.status = :currentStatus AND a.dueDate < :today AND a.active = true")
    int markOverdue(@Param("currentStatus") AccountStatus currentStatus,
                    @Param("newStatus") AccountStatus newStatus,
                    @Param("today") LocalDate today);

    // --- Dashboard aggregation queries ---

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Account a " +
           "WHERE a.companyId = :companyId AND a.type = :type " +
           "AND a.status IN :statuses AND a.active = true " +
           "AND a.dueDate BETWEEN :from AND :to")
    BigDecimal sumByTypeAndStatuses(
        @Param("companyId") UUID companyId,
        @Param("type") AccountType type,
        @Param("statuses") List<AccountStatus> statuses,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("SELECT FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM') as month, " +
           "a.type as type, COALESCE(SUM(a.amount), 0) as total " +
           "FROM Account a " +
           "WHERE a.companyId = :companyId AND a.active = true " +
           "AND a.dueDate BETWEEN :from AND :to " +
           "AND a.status IN :statuses " +
           "GROUP BY FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM'), a.type " +
           "ORDER BY FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM')")
    List<Object[]> findMonthlyTotalsByType(
        @Param("companyId") UUID companyId,
        @Param("statuses") List<AccountStatus> statuses,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("SELECT c.name as categoryName, g.name as groupName, " +
           "a.type as type, COALESCE(SUM(a.amount), 0) as total " +
           "FROM Account a " +
           "JOIN Category c ON c.id = a.categoryId " +
           "JOIN CategoryGroup g ON g.id = c.groupId " +
           "WHERE a.companyId = :companyId AND a.active = true " +
           "AND a.dueDate BETWEEN :from AND :to " +
           "AND a.status IN :statuses " +
           "GROUP BY c.name, g.name, a.type " +
           "ORDER BY total DESC")
    List<Object[]> findRevenueExpenseByCategory(
        @Param("companyId") UUID companyId,
        @Param("statuses") List<AccountStatus> statuses,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);
}
