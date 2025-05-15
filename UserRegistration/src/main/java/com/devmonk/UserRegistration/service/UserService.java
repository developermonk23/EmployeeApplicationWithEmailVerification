package com.devmonk.UserRegistration.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.devmonk.UserRegistration.model.Product;
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

	void saveProduct(Product product);

	List<Product> getAllProducts();

	Product findById(Long productId);

	Page<Product> findAllProducts(Pageable pageable);

	Product getProductById(Long productId);

}
