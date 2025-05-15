package com.devmonk.UserRegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devmonk.UserRegistration.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	
}

