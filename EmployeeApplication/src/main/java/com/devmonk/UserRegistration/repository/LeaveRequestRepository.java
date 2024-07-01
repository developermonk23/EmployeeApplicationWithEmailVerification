package com.devmonk.UserRegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.devmonk.UserRegistration.model.LeaveRequest;
import com.devmonk.UserRegistration.model.Employee;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployee(Employee employee);
    
    boolean existsByEmployeeIdAndStatus(Long employeeId, String status);
}
