package com.devmonk.UserRegistration.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.repository.EmployeeRepository;
import com.devmonk.UserRegistration.repository.UserRepository;
import com.devmonk.UserRegistration.service.EmployeeService;
import com.devmonk.UserRegistration.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UserController {
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private  EmployeeService employeeService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	 @Autowired
	 private UserRepository userRepository;
	 
	 @Autowired
	 private EmployeeRepository employeeRepository;
	 
	 private final LocaleResolver localeResolver;
	 
	public UserController(UserService userService, EmployeeService employeeService, UserDetailsService userDetailsService,
			LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
		this.userService = userService;
        this.employeeService = employeeService;
        this.userDetailsService = userDetailsService;
    }
	
    @GetMapping("/change-language")
    public String changeLanguage(@RequestParam("lang") String lang, HttpServletRequest request, HttpServletResponse response) {
        Locale locale = new Locale(lang);
        localeResolver.setLocale(request, response, locale);
        return "redirect:" + request.getHeader("Referer");
    }
	
	//to populate the registration page
    @GetMapping("/registration")
    public String getRegistrationPage(@ModelAttribute("user") User user) {
        return "register";
    }

    //to save the user details on registration
    @PostMapping("/registration")
    public String saveUser(@ModelAttribute("user") User user, Model model, HttpServletRequest request) throws Exception{
    	String siteURL = request.getRequestURL().toString();
    	userService.save(user,siteURL);
        userService.sendVerificationEmail(user, siteURL);
        model.addAttribute("message", "Registered Successfully!");
        return "register_sucess";
    }

    //to populate login page
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    //populate user page
    @GetMapping("user/{id}")
    public String userPage(@PathVariable("id") Long employeeId ,Model model, Principal principal) {
        String username = principal.getName();
        // Fetch the user details based on the username
        User user = userRepository.findByEmail(username);
        employeeService.logActivity(employeeId, "Login", "User logged in");
        // Fetch the employee details associated with the user
        Employee employee = user.getEmployee();
        model.addAttribute("employee", employee);
        return "user";
    }
    
    @GetMapping("user/{id}/editDetails")
    public String userUpdateDetailsPage(Model model, Principal principal) {
        String username = principal.getName();
        // Fetch the user details based on the username
        User user = userRepository.findByEmail(username);
        // Fetch the employee details associated with the user
        Employee employee = user.getEmployee();
        model.addAttribute("employee", employee);
        return "edit_personal_details";
    }
    
    //update employee for each users
    @PostMapping("/updateEmployee/{id}")
    public String updateEmployee(@PathVariable("id") Long id, @ModelAttribute("employee") Employee employee, RedirectAttributes redirectAttributes) throws Exception {
        Optional<Employee> optionalEmployee = employeeRepository.findById(id);

        if (optionalEmployee.isPresent()) {
            Employee existingEmployee = optionalEmployee.get();
            existingEmployee.setFirstName(employee.getFirstName());
            existingEmployee.setLastName(employee.getLastName());
            existingEmployee.setEmail(employee.getEmail());
            existingEmployee.setAddress(employee.getAddress());
            existingEmployee.setPhoneNumber(employee.getPhoneNumber());
            existingEmployee.setCountry(employee.getCountry());
            employeeRepository.save(existingEmployee);
            employeeService.logActivity(id, "Update", "Employee details updated");
            //this will show the sucess message in redirect page instead of same page
            redirectAttributes.addFlashAttribute("successMessage", "Employee details updated successfully.");
        } else {
            throw new Exception("Employee details not found..!");
        }
        return "redirect:/user/{id}";
    }
    
    @GetMapping("user/{id}/viewReviews")
    public String viewReviews(@PathVariable("id") Long id, Model model) {
        // Fetch the employee details based on the id
        Optional<Employee> optionalEmployee = employeeRepository.findById(id);
        if (optionalEmployee.isPresent()) {
            Employee employee = optionalEmployee.get();
            model.addAttribute("employee", employee);
            return "view_reviews";
        } else {
            return "error";
        }
    }


    
    //populate admin page
    @GetMapping("admin-page")
    public String viewHomePage(Model model) {
        return findPaginated(1, "firstName", "asc", model);
    }
    
    //populate add employee 
    @GetMapping("/showNewEmployeeForm")
    public String showNewEmployeeForm(Model model) {
        Employee employee = new Employee();
        model.addAttribute("employee", employee);
        return "new_employee";
    }

    //to save new employee details
    @PostMapping("/saveEmployee")
    public String saveEmployee(@ModelAttribute("employee") Employee employee) {
        employeeService.saveEmployee(employee);
        return "redirect:admin-page";
    }

    //to edit employee details
    @GetMapping("/showFormForUpdate/{id}")
    public String showFormForUpdate(@PathVariable(value = "id") long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "update_employee";
    }

    //to delete user
    @GetMapping("/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") long id) {
        employeeService.deleteEmployeeById(id);
        return "redirect:/admin-page";
    }
    
    //pagination
    @GetMapping("/page/{pageNo}")
    public String findPaginated(@PathVariable(value = "pageNo") int pageNo,
                                @RequestParam("sortField") String sortField,
                                @RequestParam("sortDir") String sortDir,
                                Model model) {
        int pageSize = 5;
        Page<Employee> page = employeeService.findPaginated(pageNo, pageSize, sortField, sortDir);
        List<Employee> listEmployees = page.getContent();
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("listEmployees", listEmployees);
        return "admin";
    }
    
    //method to verify if email verification is done by the registered user
    @GetMapping("/registration/verify")
	public String verifyUser(@Param("code") String code) {
		if (userService.verify(code)) {
			return "verify_success";
		} else {
			return "verify_fail";
		}
	}
    
    //search functionality
    @GetMapping("/search")
    public String searchEmployees(@RequestParam("keyword") String keyword, Model model) {
        List<Employee> searchResults = employeeService.searchEmployees(keyword);
        model.addAttribute("listEmployees", searchResults);
        return "admin";
    }
    
    //export file functionality
    @GetMapping("/excel")
	public void generateExcelReport(HttpServletResponse response) throws Exception{
		
		response.setContentType("application/octet-stream");
		
		String headerKey = "Content-Disposition";
		String headerValue = "attachment;filename=employee_details.xls";

		response.setHeader(headerKey, headerValue);
		
		employeeService.generateExcel(response);
		
		response.flushBuffer();
	}
    
    @GetMapping("/password-recovery")
    public String showPasswordRecoveryPage() {
        return "password_recovery";
    }
    
    //forgot password functionality
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email, Model model) throws Exception {
        User user = userService.findByEmail(email);
        if (user == null) {
            model.addAttribute("errorMessage", "User not found");
        } else {
            String token = generateToken();
            user.setResetToken(token);
            user.setResetTokenExpiry(24); // token expire in hours
            userService.saveUser(user);

            String resetUrl = "http://localhost:8080/reset-password?token=" + token;
            userService.sendPasswordResetEmail(user.getEmail(), resetUrl);

            model.addAttribute("successMessage", "Password reset instructions sent to your email");
        }
        return "password_recovery"; 
    }

    
    private String generateToken() {
    	
    	return UUID.randomUUID().toString();
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset_password";
    }
    
    //reset password functionality
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam("token") String token,
                                                @RequestParam("password") String password) throws Exception {
        User user = userService.findByResetToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }

        if (user.getResetTokenExpiry() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token expired");
        }
        String encodedPassword = passwordEncoder.encode(password);
		user.setPassword(encodedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(0);
        userService.saveUser(user);

        return ResponseEntity.ok("Password reset successfully");
    }
    
    @GetMapping("/employeeDetails/{id}")
    public String showEmployeeDetails(@PathVariable Long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "employeeActionView"; // View name for the employee details page
    }
    
    @GetMapping("/activityStatus/{id}")
    public String viewActivityStatus(@PathVariable("id") Long employeeId, Model model) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        List<ActivityLog> activityLogs = employeeService.getActivityLogsByEmployeeId(employeeId);

        model.addAttribute("employee", employee);
        model.addAttribute("activityLogs", activityLogs);
        
        return "activity-status";
    }

}
