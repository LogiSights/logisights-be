package com.logisights.driver.service;

import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.UserRole;
import com.logisights.driver.entity.DriverEarningsEntity;
import com.logisights.driver.repository.DriverEarningsRepository;
import com.logisights.notification.MailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverDigestSchedulerTest {

    @Mock
    UserRepository userRepository;
    @Mock
    DriverEarningsRepository driverEarningsRepository;
    @Mock
    MailSender mailSender;

    DriverDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DriverDigestScheduler();
        scheduler.userRepository = userRepository;
        scheduler.driverEarningsRepository = driverEarningsRepository;
        scheduler.mailSender = mailSender;
    }

    @Test
    void skipsDriversWithNoEarningsThisWeek() {
        UserEntity driver = new UserEntity();
        driver.id = UUID.randomUUID();
        driver.role = UserRole.DRIVER;
        when(userRepository.list(anyString(), any(UserRole.class), any(com.logisights.common.UserStatus.class)))
                .thenReturn(List.of(driver));
        when(driverEarningsRepository.list(anyString(), any(UUID.class), any(java.time.LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.sendWeeklyDigests();

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendsDigestForDriverWithEarnings() {
        UserEntity driver = new UserEntity();
        driver.id = UUID.randomUUID();
        driver.email = "dan@example.com";
        driver.name = "Dan";
        driver.role = UserRole.DRIVER;
        when(userRepository.list(anyString(), any(UserRole.class), any(com.logisights.common.UserStatus.class)))
                .thenReturn(List.of(driver));

        DriverEarningsEntity earning = new DriverEarningsEntity();
        earning.amountKes = new BigDecimal("150.00");
        when(driverEarningsRepository.list(anyString(), any(UUID.class), any(java.time.LocalDateTime.class)))
                .thenReturn(List.of(earning));

        scheduler.sendWeeklyDigests();

        verify(mailSender).sendWeeklyEarningsDigest(eq("dan@example.com"), eq("Dan"),
                eq(new BigDecimal("150.00")), eq(1L), anyString());
    }
}
