package com.devmonk.UserRegistration.service;

import com.devmonk.UserRegistration.model.User;

public interface UserService {
	
	void save (User user, String url)throws Exception;
	
	 void sendVerificationEmail(User user, String siteURL) throws Exception;
	 
	 boolean verify (String code);

	void sendPasswordResetEmail(String email, String resetUrl);

	void saveUser(User user) throws Exception;

	User findByEmail(String email);

	User findByResetToken(String token);

	void sendTwoFactorCode(String toEmail, String twoFactorCode);

}
