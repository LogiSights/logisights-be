package com.logisights.auth.repository;

import com.logisights.auth.entity.UserEntity;
import com.logisights.common.UserRole;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the %test datasource points at the isolated logisights_test database
 * (see application.properties) and never the dev database docker-compose brings
 * up on :5435/logisights.
 */
@QuarkusTest
class UserRepositoryTest {

    @Inject
    UserRepository userRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void persistsAndFindsUserInIsolatedTestDatabase() {
        String currentDb = (String) entityManager.createNativeQuery("select current_database()").getSingleResult();
        assertThat(currentDb).isEqualTo("logisights_test");

        UserEntity user = new UserEntity();
        user.name = "Test User";
        user.email = "test-user@example.com";
        user.passwordHash = "hash";
        user.role = UserRole.SENDER;
        userRepository.persist(user);

        assertThat(userRepository.findByEmail("test-user@example.com")).isPresent();
    }
}
