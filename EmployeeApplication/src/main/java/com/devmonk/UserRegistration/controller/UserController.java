package com.devmonk.UserRegistration.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.service.EmployeeService;
import com.devmonk.UserRegistration.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UserController {
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private  EmployeeService employeeService;
	
	
	public UserController(UserService userService, EmployeeService employeeService, UserDetailsService userDetailsService) {
        this.userService = userService;
        this.employeeService = employeeService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/registration")
    public String getRegistrationPage(@ModelAttribute("user") User user) {
        return "register";
    }

    @PostMapping("/registration")
    public String saveUser(@ModelAttribute("user") User user, Model model, HttpServletRequest request) throws Exception{
    	String siteURL = request.getRequestURL().toString();
    	userService.save(user,siteURL);
        userService.sendVerificationEmail(user, siteURL);
        model.addAttribute("message", "Registered Successfully!");
        return "register_sucess";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("user-page")
    public String userPage(Model model, Principal principal) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(principal.getName());
        model.addAttribute("user", userDetails);
        // Call the viewHomePage method to populate the user page
        viewHomePage(model);
        // Return the user page view
        return "user";
    }

    @GetMapping("admin-page")
    public String viewHomePage(Model model) {
        return findPaginated(1, "firstName", "asc", model);
    }

    @GetMapping("/showNewEmployeeForm")
    public String showNewEmployeeForm(Model model) {
        Employee employee = new Employee();
        model.addAttribute("employee", employee);
        return "new_employee";
    }

    @PostMapping("/saveEmployee")
    public String saveEmployee(@ModelAttribute("employee") Employee employee) {
        employeeService.saveEmployee(employee);
        return "redirect:admin-page";
    }

    @GetMapping("/showFormForUpdate/{id}")
    public String showFormForUpdate(@PathVariable(value = "id") long id, Model model) {
        Employee employee = employeeService.getEmployeeById(id);
        model.addAttribute("employee", employee);
        return "update_employee";
    }

    @GetMapping("/deleteEmployee/{id}")
    public String deleteEmployee(@PathVariable(value = "id") long id) {
        employeeService.deleteEmployeeById(id);
        return "redirect:/admin-page";
    }

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
    
    @GetMapping("/registration/verify")
	public String verifyUser(@Param("code") String code) {
		if (userService.verify(code)) {
			return "verify_success";
		} else {
			return "verify_fail";
		}
	}
    
    @GetMapping("/search")
    public String searchEmployees(@RequestParam("keyword") String keyword, Model model) {
        List<Employee> searchResults = employeeService.searchEmployees(keyword);
        model.addAttribute("listEmployees", searchResults);
        return "admin";
    }


}
