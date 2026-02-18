package com.findash.repository;

import com.findash.entity.Account;
import com.findash.entity.AccountStatus;
import com.findash.entity.AccountType;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AccountSpecifications {

    private AccountSpecifications() {}

    public static Specification<Account> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Account> hasCompanyId(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<Account> hasType(AccountType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Account> hasStatusIn(List<AccountStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Account> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Account> hasSupplierId(UUID supplierId) {
        return (root, query, cb) -> cb.equal(root.get("supplierId"), supplierId);
    }

    public static Specification<Account> hasClientId(UUID clientId) {
        return (root, query, cb) -> cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<Account> dueDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    public static Specification<Account> dueDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }

    public static Specification<Account> descriptionContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%");
    }
}
