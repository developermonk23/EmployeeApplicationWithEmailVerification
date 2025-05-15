package com.devmonk.UserRegistration.controller;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.devmonk.UserRegistration.model.ActivityLog;
import com.devmonk.UserRegistration.model.CartItem;
import com.devmonk.UserRegistration.model.Employee;
import com.devmonk.UserRegistration.model.LeaveBalance;
import com.devmonk.UserRegistration.model.LeaveRequest;
import com.devmonk.UserRegistration.model.Product;
import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.model.WorkFromHomeRequest;
import com.devmonk.UserRegistration.repository.EmployeeRepository;
import com.devmonk.UserRegistration.repository.ProductRepository;
import com.devmonk.UserRegistration.repository.UserRepository;
import com.devmonk.UserRegistration.service.EmployeeService;
import com.devmonk.UserRegistration.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
	 
	 @Autowired
	 private ProductRepository productRepository;
	 
	 private final LocaleResolver localeResolver;
	 
	 @Value("${product.images.path}")
	 private String imageUploadPath;

	 
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
    
 // Redirect to 2FA page after login
    @GetMapping("/2fa")
    public String showTwoFactorPage(HttpSession session, Principal principal) {
        String username = principal.getName();
        String generatedCode = generateRandomCode(); // Generate 2FA code
        
        session.setAttribute("twoFactorCode", generatedCode);

        userService.sendTwoFactorCode(username, generatedCode);

        return "two_factor_auth";
    }
    
    public String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates a random 6-digit number
        return String.valueOf(code); // Convert to String to send to the user
    }

    // Process 2FA code
    @PostMapping("/verify-2fa")
    public String verifyTwoFactorCode(@RequestParam("code") String code, HttpSession session, Principal principal) {
        String sessionCode = (String) session.getAttribute("twoFactorCode");
        if (sessionCode != null && sessionCode.equals(code)) {
            session.removeAttribute("twoFactorCode");
            session.setAttribute("twoFactorAuthenticated", true);
            
            String username = principal.getName();
            User user = userRepository.findByEmail(username);

            Long employeeId = user.getEmployee().getId();

            // Redirect to the user page with the employeeId as path variable
            return "redirect:/user/" + employeeId;
            
        }

          return "redirect:/2fa?error"; // Redirect back to 2FA page with an error
    }
    
    //populate user page
    @GetMapping("/user/{id}")
    public String userPage(@PathVariable("id") Long employeeId, Model model, Principal principal, HttpSession session) {
        // Check if 2FA is completed
        Boolean twoFactorAuthenticated = (Boolean) session.getAttribute("twoFactorAuthenticated");
		/*
		 * if (twoFactorAuthenticated == null || !twoFactorAuthenticated) { return
		 * "redirect:/2fa"; // Redirect to 2FA if not authenticated }
		 */

        // After 2FA is successful, proceed to fetch user info
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        Employee employee = user.getEmployee();
        String profilePicturePath = employeeService.getProfilePicturePath(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("profilePicturePath", profilePicturePath); 

        return "user"; // Render user page
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
        boolean hasPendingLeaveRequests = employeeService.hasPendingLeaveRequestsForEmployee(id);
        model.addAttribute("employee", employee);
        model.addAttribute("hasPendingLeaveRequests", hasPendingLeaveRequests);
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
    
    @GetMapping("/leave/apply")
    public String showLeaveApplicationForm(Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        Employee employee = user.getEmployee();

        // Fetch leave balances for the logged-in employee
        List<LeaveBalance> leaveBalances = employeeService.getLeaveBalancesForEmployee(employee.getId());

        // Add attributes to the model
        model.addAttribute("leaveBalances", leaveBalances);
        model.addAttribute("leaveRequest", new LeaveRequest());
        
        return "apply_leave";
    }


    @PostMapping("/leave/apply")
    public String applyForLeave(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        Employee employee = user.getEmployee();
        leaveRequest.setEmployee(employee);
        employeeService.applyForLeave(leaveRequest);
        return "redirect:/user/" + employee.getId();
    }

    @GetMapping("/leave/approve/{id}")
    @ResponseBody
    public ResponseEntity<String> approveLeave(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        employeeService.approveLeave(id, user.getId());
        return ResponseEntity.ok("Leave request approved");
    }
    
    @GetMapping("/leave/reject/{id}")
    @ResponseBody
    public ResponseEntity<String> rejectLeave(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        employeeService.rejectLeave(id, user.getId());
        return ResponseEntity.ok("Leave request rejected successfully.");
    }
    
    @GetMapping("/leave/view/{id}")
    public String viewLeaveRequests(Model model, Principal principal, @PathVariable("id") Long employeeId) {
    	List<LeaveRequest> leaveRequests = employeeService.findLeaveRequestsByEmployeeId(employeeId);
        model.addAttribute("leaveRequests", leaveRequests);
        return "view_leave_requests"; 
    }
    
    @GetMapping("/wfh/apply")
    public String showWFHForm(Model model, Principal principal) {
        model.addAttribute("wfhRequest", new WorkFromHomeRequest());
        return "apply_wfh";
    }

    
    @PostMapping("/apply")
    public String applyForWFH(@ModelAttribute("wfhRequest") WorkFromHomeRequest wfhRequest, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByEmail(username);
        Employee employee = user.getEmployee();
        wfhRequest.setEmployee(employee);
        employeeService.applyForWFH(wfhRequest);
        return "redirect:/user/" + employee.getId();
    }
    
    @GetMapping("/wfh/view/{id}")
    public String viewWFHRequests(@PathVariable("id") Long employeeId, Model model, Principal principal) {
        List<WorkFromHomeRequest> wfhRequests = employeeService.getWFHRequestsForEmployee(employeeId);
        model.addAttribute("wfhRequests", wfhRequests);
        return "view_wfh_requests";
    }
    
    @PostMapping("/approve/{id}")
    @ResponseBody
    public ResponseEntity<String> approveWFH(@PathVariable("id") Long wfhRequestId, Principal principal) {
    	String username = principal.getName();
    	User user = userRepository.findByEmail(username);
        employeeService.approveWFH(wfhRequestId, user.getId());
        return ResponseEntity.ok("WFH request accepted successfully.");
    }

    @PostMapping("/reject/{id}")
    @ResponseBody
    public ResponseEntity<String> rejectWFH(@PathVariable("id") Long wfhRequestId, Principal principal) {
    	String username = principal.getName();
    	User user = userRepository.findByEmail(username);
        employeeService.rejectWFH(wfhRequestId, user.getId());
        return ResponseEntity.ok("WFH request rejected successfully.");
    }
    
 // Upload profile picture
    @PostMapping("user/{id}/uploadProfilePicture")
    public String uploadProfilePicture(@PathVariable Long id,
                                        @RequestParam("profilePicture") MultipartFile file,
                                        Model model) {
        try {
            employeeService.saveProfilePicture(id, file);
            model.addAttribute("successMessage", "Profile picture uploaded successfully!");
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Failed to upload profile picture. Please try again.");
        }
        return "redirect:/user/" + id;
    }
    
    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "product_list";
    }
    
    @GetMapping("/productsUser")
    public String listProductsUser(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "product_list_user";
    }

    @GetMapping("/add")
    public String showAddProductPage(Model model) {
        model.addAttribute("product", new Product());
        List<Product> productList = userService.getAllProducts();
        model.addAttribute("products", productList);
        return "add_product";
    }

    @PostMapping("/addProduct")
    public String saveProduct(String name, String description, MultipartFile image, Double price) throws IOException {
        // Ensure directory exists
        String uploadDir = imageUploadPath; // Inject this from application.properties
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }

        // Save image
        String fileName = image.getOriginalFilename();
        String filePath = uploadDir + fileName;
        File dest = new File(filePath);
        image.transferTo(dest);

        // Save product in DB
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setImageUrl("/images/" + fileName); // URL for accessing the image
        product.setPrice(price);

        productRepository.save(product);
        return "redirect:/products";
    }
    
    @PostMapping("/editProduct/{id}")
    public String updateProduct(@PathVariable("id") Long id, 
                                @ModelAttribute("product") Product product, 
                                RedirectAttributes redirectAttributes) throws Exception {
        Optional<Product> optionalProduct = productRepository.findById(id);

        if (optionalProduct.isPresent()) {
            Product existingProduct = optionalProduct.get();
            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            
            productRepository.save(existingProduct);
            
            redirectAttributes.addFlashAttribute("successMessage", "Product details updated successfully.");
        } else {
            throw new Exception("Product not found!");
        }

        return "redirect:/products";
    }



    @PostMapping("/deleteProduct/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return "redirect:/products";
    }
    
    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
        }

        double total = cart.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();

        model.addAttribute("cartItems", cart);
        model.addAttribute("totalPrice", total);

        return "cart"; // cart.html Thymeleaf view
    }
    
	/*
	 * @GetMapping("/api/cart")
	 * 
	 * @ResponseBody public List<CartItem> getCartItems(HttpSession session) {
	 * List<CartItem> cart = (List<CartItem>) session.getAttribute("cart"); if (cart
	 * == null) { cart = new ArrayList<>(); } return cart; }
	 */


	/*
	 * @PostMapping("/addToCart") public String addToCart(@RequestParam Long
	 * productId,
	 * 
	 * @RequestParam int quantity, HttpSession session) { // Fetch product details
	 * (from DB or service) Product product = userService.getProductById(productId);
	 * 
	 * if (product == null) { return "redirect:/productPage"; // or show error }
	 * 
	 * // Retrieve cart from session or create new one List<CartItem> cart =
	 * (List<CartItem>) session.getAttribute("cart"); if (cart == null) { cart = new
	 * ArrayList<>(); }
	 * 
	 * // Check if product already in cart boolean itemExists = false; for (CartItem
	 * item : cart) { if (item.getProductId().equals(productId)) {
	 * item.setQuantity(item.getQuantity() + quantity); itemExists = true; break; }
	 * }
	 * 
	 * // If not exists, add new CartItem if (!itemExists) { CartItem newItem = new
	 * CartItem(); newItem.setProductId(product.getId());
	 * newItem.setName(product.getName());
	 * newItem.setImageUrl(product.getImageUrl());
	 * newItem.setPrice(product.getPrice()); newItem.setQuantity(quantity);
	 * cart.add(newItem); }
	 * 
	 * // Update session attribute session.setAttribute("cart", cart);
	 * 
	 * // Redirect back to product catalog return "redirect:/productPage"; }
	 */
    
	/*
	 * @PostMapping("/api/addToCart")
	 * 
	 * @ResponseBody public List<CartItem> addToCartAjax(@RequestParam Long
	 * productId,
	 * 
	 * @RequestParam int quantity, HttpSession session) { Product product =
	 * userService.getProductById(productId); if (product == null) { return
	 * Collections.emptyList(); }
	 * 
	 * List<CartItem> cart = (List<CartItem>) session.getAttribute("cart"); if (cart
	 * == null) { cart = new ArrayList<>(); }
	 * 
	 * boolean itemExists = false; for (CartItem item : cart) { if
	 * (item.getProductId().equals(productId)) { item.setQuantity(item.getQuantity()
	 * + quantity); itemExists = true; break; } }
	 * 
	 * if (!itemExists) { CartItem newItem = new CartItem();
	 * newItem.setProductId(product.getId()); newItem.setName(product.getName());
	 * newItem.setImageUrl(product.getImageUrl());
	 * newItem.setPrice(product.getPrice()); newItem.setQuantity(quantity);
	 * cart.add(newItem); }
	 * 
	 * session.setAttribute("cart", cart);
	 * 
	 * return cart; // Return updated cart }
	 */

    
	/*
	 * @GetMapping("/productPage") public String showProductPage(Model model) { //
	 * If needed, add product list or other attributes to model here return
	 * "product_list_user"; // this should match your Thymeleaf HTML template name }
	 */


	/*
	 * @GetMapping("/productPage/{pageNumber}") public String
	 * viewProductPage(@PathVariable("pageNumber") int pageNumber,
	 * 
	 * @RequestParam(value = "sortField", required = false, defaultValue = "name")
	 * String sortField,
	 * 
	 * @RequestParam(value = "sortDir", required = false, defaultValue = "asc")
	 * String sortDir, Model model) { int pageSize = 6; // You can adjust the number
	 * of products per page
	 * 
	 * Sort sort = Sort.by(sortField); sort = sortDir.equals("asc") ?
	 * sort.ascending() : sort.descending();
	 * 
	 * Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);
	 * Page<Product> page = userService.findAllProducts(pageable); // Assuming a
	 * method in your UserService
	 * 
	 * List<Product> products = page.getContent(); long totalElements =
	 * page.getTotalElements(); int totalPages = page.getTotalPages();
	 * 
	 * model.addAttribute("products", products); model.addAttribute("currentPage",
	 * pageNumber); model.addAttribute("totalPages", totalPages);
	 * model.addAttribute("totalItems", totalElements);
	 * model.addAttribute("sortField", sortField); model.addAttribute("sortDir",
	 * sortDir); model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc"
	 * : "asc");
	 * 
	 * return "product_list_user"; // Make sure this matches the name of your HTML
	 * file }
	 */

	/*
	 * @GetMapping("/add/{productId}") public String addToCart(@PathVariable Long
	 * productId, Principal principal) { String username = principal.getName();
	 * Product product = productRepository.findById(productId).orElseThrow();
	 * CartItem item = new CartItem(); item.setProduct(product);
	 * item.setQuantity(1); item.setUsername(username);
	 * cartItemRepository.save(item); return "redirect:/cart"; }
	 * 
	 * @GetMapping("/remove/{id}") public String removeFromCart(@PathVariable Long
	 * id) { cartItemRepository.deleteById(id); return "redirect:/cart"; }
	 */
}
