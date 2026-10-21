package com.example.careercontactapi.UserManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Boolean existsByUserId(String userId);
    Optional<User> findByUserId(String userId);
}
