package com.devmonk.UserRegistration.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;

import com.devmonk.UserRegistration.model.Employee;

import jakarta.servlet.http.HttpServletResponse;

public interface EmployeeService {
	List<Employee> getAllEmployees();
	void saveEmployee(Employee employee);
	Employee getEmployeeById(long id);
	void deleteEmployeeById(long id);
	Page<Employee> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection);
	List<Employee> searchEmployees(String keyword);
	void generateExcel(HttpServletResponse response) throws IOException;
}
