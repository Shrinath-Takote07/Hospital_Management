package com.hms.hospital.repository;

import com.hms.hospital.entity.Role;
import com.hms.hospital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    long countByRole(Role role);
}