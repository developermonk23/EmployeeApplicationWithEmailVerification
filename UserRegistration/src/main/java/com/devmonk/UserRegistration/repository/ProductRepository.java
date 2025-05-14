package com.devmonk.UserRegistration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devmonk.UserRegistration.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
