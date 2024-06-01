package com.devmonk.UserRegistration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devmonk.UserRegistration.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
	    List<ActivityLog> findByEmployeeId(Long employeeId);
}
