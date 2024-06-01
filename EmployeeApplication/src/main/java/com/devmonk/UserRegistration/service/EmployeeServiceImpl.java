package com.devmonk.UserRegistration.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.repository.ActivityLogRepository;
import com.devmonk.UserRegistration.repository.EmployeeRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeRepository employeeRepository;
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
    private ActivityLogRepository activityLogRepository;

	@Override
	public List<Employee> getAllEmployees() {
		return employeeRepository.findAll();
	}

	@Override
	public void saveEmployee(Employee employee) {
		if(employee.getRating() != null || employee.getReview() !=null) {
			sendEmailNotification (employee.getEmail());
		}
		this.employeeRepository.save(employee);
		logActivity(employee.getId(), "Save", "Employee details saved");
	}
	
	public void sendEmailNotification(String email) {
		String Login = "http://localhost:8080/login";
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlMsg = "<p>To see the review, click here: <a href=\"" + Login + "\">Login</a></p>";
        try {
        	helper.setTo(email);
            helper.setSubject("Review is updated");
            helper.setText(htmlMsg, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        
    }

	@Override
	public Employee getEmployeeById(long id) {
		Optional<Employee> optional = employeeRepository.findById(id);
		Employee employee = null;
		if (optional.isPresent()) {
			employee = optional.get();
		} else {
			throw new RuntimeException(" Employee not found for id :: " + id);
		}
		return employee;
	}

	@Override
	public void deleteEmployeeById(long id) {
		this.employeeRepository.deleteById(id);
		logActivity(id, "Delete", "Employee deleted");
	}

	@Override
	public Page<Employee> findPaginated(int pageNo, int pageSize, String sortField, String sortDirection) {
		Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() :
			Sort.by(sortField).descending();
		
		Pageable pageable = PageRequest.of(pageNo - 1, pageSize, sort);
		return this.employeeRepository.findAll(pageable);
	}
	@Override
	public List<Employee> searchEmployees(String keyword) {
	    return employeeRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(keyword, keyword);
	}

	@Override
	public void generateExcel(HttpServletResponse response) throws IOException {
		List<Employee> employeeList = employeeRepository.findAll();

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Employee Info");
		HSSFRow row = sheet.createRow(0);

		row.createCell(0).setCellValue("First Name");
		row.createCell(1).setCellValue("Last Name");
		row.createCell(2).setCellValue("Email");

		int dataRowIndex = 1;

		for (Employee employee : employeeList) {
			HSSFRow dataRow = sheet.createRow(dataRowIndex);
			dataRow.createCell(0).setCellValue(employee.getFirstName());
			dataRow.createCell(1).setCellValue(employee.getLastName());
			dataRow.createCell(2).setCellValue(employee.getEmail());
			dataRowIndex++;
		}

		ServletOutputStream ops = response.getOutputStream();
		workbook.write(ops);
		workbook.close();
		ops.close();
	}
	@Override
	public List<ActivityLog> getActivityLogsByEmployeeId(Long employeeId) {
        return activityLogRepository.findByEmployeeId(employeeId);
    }
	
	@Override
	public void logActivity(Long employeeId, String action, String description) {
        ActivityLog log = new ActivityLog();
        log.setEmployee(new Employee(employeeId));  // Assuming Employee has a constructor with id
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        log.setDescription(description);
        activityLogRepository.save(log);
    }


}
