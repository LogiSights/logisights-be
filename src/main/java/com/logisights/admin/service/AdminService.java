package com.logisights.admin.service;

import com.logisights.admin.dto.*;
import com.logisights.auth.dto.UserDto;
import com.logisights.auth.entity.UserEntity;
import com.logisights.auth.repository.UserRepository;
import com.logisights.common.ApiException;
import com.logisights.common.UserRole;
import com.logisights.common.UserStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminService {

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager entityManager;

    public List<UserDto> listUsers() {
        return userRepository.listAll().stream().map(UserDto::from).collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUserStatus(java.util.UUID userId, UserStatus status) {
        UserEntity user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.status = status;
        return UserDto.from(user);
    }

    public AdminStatsSummaryDto summary() {
        long totalUsers = userRepository.count();
        long activeDrivers = userRepository.count("role = ?1 and status = ?2", UserRole.DRIVER, UserStatus.ACTIVE);

        long parcelsToday = ((Number) entityManager.createNativeQuery(
                        "select count(*) from parcels where created_at::date = current_date")
                .getSingleResult()).longValue();

        Object revenueTodayRaw = entityManager.createNativeQuery(
                        "select coalesce(sum(amount_kes), 0) from payments where status = 'SUCCESS' and created_at::date = current_date")
                .getSingleResult();
        BigDecimal revenueToday = revenueTodayRaw instanceof BigDecimal bd ? bd : new BigDecimal(revenueTodayRaw.toString());

        return new AdminStatsSummaryDto(totalUsers, activeDrivers, parcelsToday, revenueToday);
    }

    @SuppressWarnings("unchecked")
    public List<DeliveryTrendPointDto> deliveryTrend(int days) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select created_at::date as day, count(*) from parcels " +
                                "where status = 'DELIVERED' and created_at >= current_date - :days " +
                                "group by day order by day")
                .setParameter("days", days)
                .getResultList();
        return rows.stream()
                .map(r -> new DeliveryTrendPointDto(((java.sql.Date) r[0]).toLocalDate(), ((Number) r[1]).longValue()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<StatusBreakdownDto> statusBreakdown() {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select status, count(*) from parcels group by status")
                .getResultList();
        return rows.stream()
                .map(r -> new StatusBreakdownDto(
                        com.logisights.common.ParcelStatus.valueOf((String) r[0]),
                        ((Number) r[1]).longValue()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<TopCityDto> topCities() {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select city, count(*) from parcels group by city order by count(*) desc")
                .getResultList();
        return rows.stream()
                .map(r -> new TopCityDto(
                        com.logisights.common.ParcelCity.valueOf((String) r[0]),
                        ((Number) r[1]).longValue()))
                .collect(Collectors.toList());
    }
}
