package com.devmonk.UserRegistration.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.devmonk.UserRegistration.model.User;
import com.devmonk.UserRegistration.repository.UserRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class UserServiceImpl implements UserService {
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private JavaMailSender mailSender;

	@Override
	public void save(User user, String siteURL) throws Exception{
		String encodedPassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		String randomCode = UUID.randomUUID().toString();
		user.setVerificationCode(randomCode);
		user.setEnabled(false);

		userRepository.save(user);
		
		sendVerificationEmail(user, siteURL);
	}
	
	@Override
	public void sendVerificationEmail(User user, String siteURL) 
			throws Exception {
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

	/* if user is null or is already verified it returns false else true */
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
	public void saveUser(User user) throws Exception{

		userRepository.save(user);
		
	}

	@Override
	public User findByEmail(String email) {
		User user = userRepository.findByEmail(email);
		return user;
	}

	@Override
	public User findByResetToken(String token) {
		User user = userRepository.findByResetToken(token);
		return user;
	}
 
}
