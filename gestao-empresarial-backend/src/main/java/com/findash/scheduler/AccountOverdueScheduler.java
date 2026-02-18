package com.findash.scheduler;

import com.findash.entity.AccountStatus;
import com.findash.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class AccountOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountOverdueScheduler.class);

    private final AccountRepository accountRepository;

    public AccountOverdueScheduler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void markOverdueAccounts() {
        int updated = accountRepository.markOverdue(
            AccountStatus.PENDING,
            AccountStatus.OVERDUE,
            LocalDate.now()
        );
        if (updated > 0) {
            log.info("Marked {} accounts as OVERDUE", updated);
        }
    }
}
