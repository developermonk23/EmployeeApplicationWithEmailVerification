package com.devmonk.UserRegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.devmonk.UserRegistration.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>{

}
