package com.myledger.repository;

import com.myledger.entity.Role;
import com.myledger.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByIdAsc();

    long countByRole(Role role);

    long countByRoleAndEnabledTrue(Role role);
}
