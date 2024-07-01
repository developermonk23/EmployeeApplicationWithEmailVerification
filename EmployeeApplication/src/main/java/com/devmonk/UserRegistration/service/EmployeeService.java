package com.devmonk.UserRegistration.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.LeaveRequest;

import jakarta.servlet.http.HttpServletResponse;

public interface EmployeeService {
	List<Employee> getAllEmployees();
	void saveEmployee(Employee employee);
	Employee getEmployeeById(long id);
	void deleteEmployeeById(long id);
	Page<Employee> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection);
	List<Employee> searchEmployees(String keyword);
	void generateExcel(HttpServletResponse response) throws IOException;
	List<ActivityLog> getActivityLogsByEmployeeId(Long employeeId);
	void logActivity(Long employeeId, String action, String description);
	LeaveRequest applyForLeave(LeaveRequest leaveRequest);
	LeaveRequest approveLeave(Long leaveRequestId, Long approverId);
	List<LeaveRequest> findLeaveRequestsByEmployeeId(Long employeeId);
	LeaveRequest rejectLeave(Long leaveRequestId, Long approverId);
	boolean hasPendingLeaveRequestsForEmployee(Long employeeId);
}
