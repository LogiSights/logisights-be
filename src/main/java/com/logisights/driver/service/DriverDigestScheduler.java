package com.logisights.driver.service;

import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.UserRole;
import com.logisights.common.UserStatus;
import com.logisights.driver.entity.DriverEarningsEntity;
import com.logisights.driver.repository.DriverEarningsRepository;
import com.logisights.notification.MailSender;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sends each active driver a summary of the earnings they posted in the past week.
 */
@ApplicationScoped
public class DriverDigestScheduler {

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d");

    @Inject
    UserRepository userRepository;

    @Inject
    DriverEarningsRepository driverEarningsRepository;

    @Inject
    MailSender mailSender;

    @Scheduled(cron = "0 0 8 ? * MON")
    void sendWeeklyDigests() {
        List<UserEntity> drivers = userRepository.list("role = ?1 and status = ?2", UserRole.DRIVER, UserStatus.ACTIVE);

        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(7);
        String weekLabel = weekStart.format(LABEL_FORMAT) + " - " + weekEnd.format(LABEL_FORMAT);

        for (UserEntity driver : drivers) {
            List<DriverEarningsEntity> weekEarnings = driverEarningsRepository.list(
                    "driverId = ?1 and createdAt >= ?2", driver.id, weekStart.atStartOfDay());

            if (weekEarnings.isEmpty()) {
                continue;
            }

            BigDecimal total = weekEarnings.stream()
                    .map(e -> e.amountKes)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            mailSender.sendWeeklyEarningsDigest(driver.email, driver.name, total, weekEarnings.size(), weekLabel);
        }
    }
}
