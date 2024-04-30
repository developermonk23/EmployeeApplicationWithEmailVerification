package com.devmonk.UserRegistration.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.devmonk.UserRegistration.dto.UserDto;
import com.devmonk.UserRegistration.model.User;

import jakarta.mail.MessagingException;

public interface UserService {
	
	void save (User user, String url)throws Exception;
	
	 void sendVerificationEmail(User user, String siteURL) throws Exception;
	 
	 boolean verify (String code);

}
