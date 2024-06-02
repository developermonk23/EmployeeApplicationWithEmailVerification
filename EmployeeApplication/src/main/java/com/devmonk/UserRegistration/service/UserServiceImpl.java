package com.devmonk.UserRegistration.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.repository.ActivityLogRepository;
import com.devmonk.UserRegistration.repository.EmployeeRepository;
import com.devmonk.UserRegistration.repository.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;


@Service
public class UserServiceImpl implements EmployeeService, UserService {
	
	@Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public void saveEmployee(Employee employee) {
        if (employee.getRating() != null || employee.getReview() != null) {
            sendEmailNotification(employee.getEmail());
        }
        this.employeeRepository.save(employee);
        logActivity(employee.getId(), "Save", "Employee details saved");
    }

    public void sendEmailNotification(String email) {
        String login = "http://localhost:8080/login";
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlMsg = "<p>To see the review, click here: <a href=\"" + login + "\">Login</a></p>";
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
        log.setEmployee(new Employee(employeeId));
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        log.setDescription(description);
        activityLogRepository.save(log);
    }

    @Override
    public void save(User user, String siteURL) throws Exception {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        String randomCode = UUID.randomUUID().toString();
        user.setVerificationCode(randomCode);
        user.setEnabled(false);

        userRepository.save(user);

        // Every registered user is an employee
        Employee employee = new Employee();
        String[] nameParts = user.getFullname().split(" ");
        String firstName = nameParts[0];
        String lastName = (nameParts.length > 1) ? nameParts[1] : "";
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(user.getEmail());
        // Set other Employee attributes as needed
        employeeRepository.save(employee);

        sendVerificationEmail(user, siteURL);
    }

    @Override
    public void sendVerificationEmail(User user, String siteURL) throws Exception {
        String toAddress = user.getEmail();
        String fromAddress = "emailaddress";
        String senderName = "User Verification Team";
        String subject = "Please verify your registration";
        String content = "Dear [[name]],<br>"
                + "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you,<br>"
                + "Your company name.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getFullname());
        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();

        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(message);

        System.out.println("Email has been sent");
    }

    @Override
    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);

        if (user == null || user.isEnabled()) {
            return false;
        } else {
            user.setVerificationCode(null);
            user.setEnabled(true);
            user.setRole("USER");
            userRepository.save(user);

            return true;
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset");
        message.setText("To reset your password, click here: " + resetUrl);
        mailSender.send(message);
    }

    @Override
    public void saveUser(User user) throws Exception {
        userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User findByResetToken(String token) {
        return userRepository.findByResetToken(token);
    }
}