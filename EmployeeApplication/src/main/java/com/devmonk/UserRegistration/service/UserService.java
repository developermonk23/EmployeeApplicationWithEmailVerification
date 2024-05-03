package com.devmonk.UserRegistration.service;

import com.devmonk.UserRegistration.model.User;

public interface UserService {
	
	void save (User user, String url)throws Exception;
	
	 void sendVerificationEmail(User user, String siteURL) throws Exception;
	 
	 boolean verify (String code);

}
