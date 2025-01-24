package com.devmonk.UserRegistration.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import org.springframework.web.multipart.MultipartFile;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.LeaveBalance;
import com.devmonk.UserRegistration.model.LeaveRequest;
import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.model.WorkFromHomeRequest;
import com.devmonk.UserRegistration.repository.ActivityLogRepository;
import com.devmonk.UserRegistration.repository.EmployeeRepository;
import com.devmonk.UserRegistration.repository.LeaveBalanceRepository;
import com.devmonk.UserRegistration.repository.LeaveRequestRepository;
import com.devmonk.UserRegistration.repository.WorkFromHomeRequestRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;
    
    @Autowired
    private WorkFromHomeRequestRepository wfhRequestRepository;
    
    private final String uploadDir = "uploads/profile_pictures/";

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public void saveEmployee(Employee employee) {
        if (employee.getRating()!= null && !employee.getReview().isEmpty()) { 
            sendEmailNotification(employee.getEmail());
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
        log.setEmployee(new Employee(employeeId));  
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        log.setDescription(description);
        activityLogRepository.save(log);
    }
    
    @Override
    public LeaveRequest applyForLeave(LeaveRequest leaveRequest) {
        leaveRequest.setStatus("Pending");
        leaveRequest.setRequestDate(LocalDate.now());
        return leaveRequestRepository.save(leaveRequest);
    }
    
    @Override
    public LeaveRequest approveLeave(Long leaveRequestId, Long approverId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));
        leaveRequest.setStatus("Approved");
        leaveRequest.setApprovalDate(LocalDate.now());
        leaveRequest.setApprover(new User(approverId));
        leaveRequestRepository.save(leaveRequest);

        // Update Leave Balance
        LeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeAndLeaveType(leaveRequest.getEmployee(), leaveRequest.getLeaveType())
                .orElseThrow(() -> new RuntimeException("Leave Balance not found"));
        int days = (int) ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;
        leaveBalance.setLeavesTaken(leaveBalance.getLeavesTaken() + days);
        leaveBalance.setBalance(leaveBalance.getTotalLeaves() - leaveBalance.getLeavesTaken());
        leaveBalanceRepository.save(leaveBalance);

        return leaveRequest;
    }
    
    @Override
    public LeaveRequest rejectLeave(Long leaveRequestId, Long approverId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));
        leaveRequest.setStatus("Rejected");
        leaveRequest.setApprovalDate(LocalDate.now());
        leaveRequestRepository.save(leaveRequest);

        return leaveRequest;
    }
    
    @Override
    public List<LeaveRequest> findLeaveRequestsByEmployeeId(Long employeeId) {
        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        if (!employeeOptional.isPresent()) {
            throw new EntityNotFoundException("Employee with ID " + employeeId + " not found");
        }
        Employee employee = employeeOptional.get();
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployee(employee);
        return leaveRequests;
    }
    
    @Override
    public boolean hasPendingLeaveRequestsForEmployee(Long employeeId) {
        return leaveRequestRepository.existsByEmployeeIdAndStatus(employeeId, "Pending");
    }
    
    @Override
    public List<LeaveBalance> getLeaveBalancesForEmployee(Long employeeId) {
        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        if (!employeeOptional.isPresent()) {
            throw new EntityNotFoundException("Employee with ID " + employeeId + " not found");
        }
        return leaveBalanceRepository.findByEmployee(employeeOptional.get());
    }
    
    @Override
    public WorkFromHomeRequest applyForWFH(WorkFromHomeRequest wfhRequest) {
        wfhRequest.setStatus("Pending");
        wfhRequest.setRequestDate(LocalDate.now());
        return wfhRequestRepository.save(wfhRequest);
    }
    
    @Override
    public List<WorkFromHomeRequest> getWFHRequestsForEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        return wfhRequestRepository.findByEmployee(employee);
    }
    
    @Override
    public WorkFromHomeRequest approveWFH(Long wfhRequestId, Long approverId) {
        WorkFromHomeRequest request = wfhRequestRepository.findById(wfhRequestId)
                .orElseThrow(() -> new EntityNotFoundException("WFH request not found"));
        request.setStatus("Approved");
        request.setApprovalDate(LocalDate.now());
        request.setApprover(new User(approverId));
        return wfhRequestRepository.save(request);
    }

    @Override
    public WorkFromHomeRequest rejectWFH(Long wfhRequestId, Long approverId) {
        WorkFromHomeRequest request = wfhRequestRepository.findById(wfhRequestId)
                .orElseThrow(() -> new EntityNotFoundException("WFH request not found"));
        request.setStatus("Rejected");
        request.setApprovalDate(LocalDate.now());
        request.setApprover(new User(approverId));
        return wfhRequestRepository.save(request);
    }
    

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    @Override
    public String getProfilePicturePath(Employee employee) {
        String profilePicture = employee.getProfilePicture();
        if (profilePicture != null && !profilePicture.isEmpty()) {
            return "/uploads/profile_pictures/" + profilePicture; // This matches the ResourceHandler URL
        }
        return "/uploads/profile_pictures/default.jpg"; // Default image
    }


	@Override
	public void saveProfilePicture(Long id, MultipartFile file) throws IOException {
		Employee employee = getEmployeeById(id);

        // Ensure the upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save the file to the local filesystem
        String fileName = id + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Update the employee's profile picture field
        employee.setProfilePicture(fileName);
        employeeRepository.save(employee);
	}

}
