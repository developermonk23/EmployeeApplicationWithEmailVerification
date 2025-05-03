package com.devmonk.UserRegistration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.repository.UserRepository;

//to log the event when the user log out

@Component
public class LogoutEventListener implements ApplicationListener<LogoutSuccessEvent> {

	@Autowired
    private EmployeeService employeeService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onApplicationEvent(LogoutSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            String username = authentication.getName();
            User user = userRepository.findByEmail(username);

            if (user != null) {
                Long employeeId = user.getEmployee().getId();
                employeeService.logActivity(employeeId, "Logout", "User logged out");
            }
        }
    }
}
