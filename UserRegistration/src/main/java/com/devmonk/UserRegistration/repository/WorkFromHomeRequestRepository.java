package com.devmonk.UserRegistration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.WorkFromHomeRequest;

public interface WorkFromHomeRequestRepository extends JpaRepository<WorkFromHomeRequest, Long> {
    List<WorkFromHomeRequest> findByEmployee(Employee employee);
}
