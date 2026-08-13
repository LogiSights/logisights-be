package com.logisights.admin.service;

import com.logisights.admin.dto.AdminStatsSummaryDto;
import com.logisights.admin.dto.DeliveryTrendPointDto;
import com.logisights.admin.dto.StatusBreakdownDto;
import com.logisights.admin.dto.TopCityDto;
import com.logisights.auth.dto.UserDto;
import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.ParcelCity;
import com.logisights.common.ParcelStatus;
import com.logisights.common.UserStatus;
import com.logisights.notification.MailSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    EntityManager entityManager;
    @Mock
    MailSender mailSender;
    @Mock
    Query query;

    AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService();
        adminService.userRepository = userRepository;
        adminService.entityManager = entityManager;
        adminService.mailSender = mailSender;
    }

    @Test
    void listUsersMapsAllUsersToDto() {
        UserEntity user = new UserEntity();
        user.name = "Jane";
        when(userRepository.listAll()).thenReturn(List.of(user));

        List<UserDto> result = adminService.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Jane");
    }

    @Test
    void updateUserStatusThrowsWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserStatus(userId, UserStatus.SUSPENDED))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void updateUserStatusSendsSuspensionEmailOnlyOnTransition() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.email = "jane@example.com";
        user.name = "Jane";
        user.status = UserStatus.ACTIVE;
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.of(user));

        adminService.updateUserStatus(userId, UserStatus.SUSPENDED);

        assertThat(user.status).isEqualTo(UserStatus.SUSPENDED);
        verify(mailSender).sendAccountSuspended("jane@example.com", "Jane");
    }

    @Test
    void updateUserStatusDoesNotResendEmailWhenAlreadySuspended() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.status = UserStatus.SUSPENDED;
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.of(user));

        adminService.updateUserStatus(userId, UserStatus.SUSPENDED);

        verifyNoInteractions(mailSender);
    }

    @Test
    void summaryAggregatesCountsAndRevenue() {
        when(userRepository.count()).thenReturn(42L);
        when(userRepository.count(anyString(), any(), any())).thenReturn(5L);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(7, new BigDecimal("1500.00"));

        AdminStatsSummaryDto result = adminService.summary();

        assertThat(result.totalUsers()).isEqualTo(42L);
        assertThat(result.activeDrivers()).isEqualTo(5L);
        assertThat(result.parcelsToday()).isEqualTo(7L);
        assertThat(result.revenueToday()).isEqualByComparingTo("1500.00");
    }

    @Test
    void deliveryTrendMapsRows() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        Object[] row = {Date.valueOf(LocalDate.of(2026, 1, 5)), 3};
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(row));

        List<DeliveryTrendPointDto> result = adminService.deliveryTrend(14);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).day()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(result.get(0).deliveries()).isEqualTo(3L);
    }

    @Test
    void statusBreakdownMapsRows() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        Object[] row = {"DELIVERED", 9};
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(row));

        List<StatusBreakdownDto> result = adminService.statusBreakdown();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ParcelStatus.DELIVERED);
        assertThat(result.get(0).count()).isEqualTo(9L);
    }

    @Test
    void topCitiesMapsRows() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        Object[] row = {"NAIROBI", 20};
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(row));

        List<TopCityDto> result = adminService.topCities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).city()).isEqualTo(ParcelCity.NAIROBI);
        assertThat(result.get(0).volume()).isEqualTo(20L);
    }
}
