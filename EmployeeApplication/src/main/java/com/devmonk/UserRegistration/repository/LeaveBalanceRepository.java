package com.devmonk.UserRegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.devmonk.UserRegistration.model.LeaveBalance;
import com.devmonk.UserRegistration.model.Employee;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeAndLeaveType(Employee employee, String leaveType);
}
