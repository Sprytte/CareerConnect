package com.example.careercontactapi.UserManagement.datalayer;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    Boolean existsByUserId(String userId);
}
