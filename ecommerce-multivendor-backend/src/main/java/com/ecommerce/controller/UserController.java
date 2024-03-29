package com.ecommerce.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.CommonApiResponse;
import com.ecommerce.dto.RegisterUserRequestDto;
import com.ecommerce.dto.UserLoginRequest;
import com.ecommerce.dto.UserLoginResponse;
import com.ecommerce.dto.UserResponseDto;
import com.ecommerce.dto.UserStatusUpdateRequestDto;
import com.ecommerce.resource.UserResource;
import com.ecommerce.service.UserServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("api/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

	private final Logger LOG = LoggerFactory.getLogger(UserController.class); 
	
	@Autowired
	private UserResource userResource;
	
	UserServiceImpl userservice;

	// RegisterUserRequestDto, we will set only email, password & role from UI
	@PostMapping("/admin/register")
	@Operation(summary = "Api to register Admin")
	public ResponseEntity<CommonApiResponse> registerAdmin(@RequestBody RegisterUserRequestDto request) {
	    LOG.info("Received request to register Admin");

	    try {
	        ResponseEntity<CommonApiResponse> responseEntity = userResource.registerAdmin(request);
	        LOG.info("Admin registration request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred during admin registration: {}", e.getMessage(), e);
	        CommonApiResponse errorResponse = new CommonApiResponse();
	        errorResponse.setResponseMessage("Admin registration failed");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	// for customer and seller register
	@PostMapping("register")
	@Operation(summary = "Api to register customer or seller user")
	public ResponseEntity<CommonApiResponse> registerUser(@RequestBody RegisterUserRequestDto request) {
		  LOG.info("Received request to register user");
		  try {
		        ResponseEntity<CommonApiResponse> responseEntity = this.userResource.registerUser(request);
		        LOG.info("User registration completed successfully");
		        return responseEntity;
		    } catch (Exception e) {
		        LOG.error("Error occurred during user registration: {}", e.getMessage(), e);
		        CommonApiResponse errorResponse = new CommonApiResponse();
		        errorResponse.setResponseMessage("User registration failed");
		        errorResponse.setSuccess(false);
		        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		    }
		}
	
	@PostMapping("login")
	@Operation(summary =  "Api to login any User")
	public ResponseEntity<UserLoginResponse> login(@RequestBody UserLoginRequest userLoginRequest) {
	    LOG.info("Received request to login user");

	    try {
	        ResponseEntity<UserLoginResponse> responseEntity = userResource.login(userLoginRequest);
	        LOG.info("User login request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred during user login: {}", e.getMessage(), e);
	        UserLoginResponse errorResponse = new UserLoginResponse();
	        errorResponse.setResponseMessage("User login failed");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@GetMapping("/fetch/role-wise")
	@Operation(summary =  "Api to get Users By Role")
	public ResponseEntity<UserResponseDto> fetchAllUsersByRole(@RequestParam("role") String role) throws JsonProcessingException {
	    LOG.info("Received request to fetch users by role: {}", role);

	    try {
	        ResponseEntity<UserResponseDto> responseEntity = userResource.getUsersByRole(role);
	        LOG.info("User fetch by role request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred while fetching users by role: {}", e.getMessage(), e);
	        // Handle the error and return an appropriate response
	        UserResponseDto errorResponse = new UserResponseDto();
	        errorResponse.setResponseMessage("Failed to fetch users by role");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@GetMapping("/fetch/seller/delivery-person")
	@Operation(summary =  "Api to get Delivery persons by seller")
	public ResponseEntity<UserResponseDto> fetchDeliveryPerson(@RequestParam("sellerId") int sellerId) {
	    LOG.info("Received request to fetch Delivery persons by seller: {}", sellerId);

	    try {
	        ResponseEntity<UserResponseDto> responseEntity = userResource.getDeliveryPersonsBySeller(sellerId);
	        LOG.info("Delivery persons fetch by seller request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred while fetching Delivery persons by seller: {}", e.getMessage(), e);
	        // Handle the error and return an appropriate response
	        UserResponseDto errorResponse = new UserResponseDto();
	        errorResponse.setResponseMessage("Failed to fetch Delivery persons by seller");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PutMapping("update/status")
	@Operation(summary =  "Api to update the user status")
	public ResponseEntity<CommonApiResponse> updateUserStatus(@RequestBody UserStatusUpdateRequestDto request) {
	    LOG.info("Received request to update the user status");

	    try {
	        ResponseEntity<CommonApiResponse> responseEntity = userResource.updateUserStatus(request);
	        LOG.info("User status update request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred while updating the user status: {}", e.getMessage(), e);
	        // Handle the error and return an appropriate response
	        CommonApiResponse errorResponse = new CommonApiResponse();
	        errorResponse.setResponseMessage("Failed to update user status");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	@DeleteMapping("delete/seller")
	@Operation(summary =  "Api to delete a seller")
	public ResponseEntity<CommonApiResponse> deleteSeller(@RequestParam("sellerId") int sellerId) {
	    LOG.info("Received request to delete seller with ID: {}", sellerId);

	    try {
	        ResponseEntity<CommonApiResponse> responseEntity = userResource.deleteSeller(sellerId);
	        LOG.info("Seller deletion request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred while deleting seller with ID: {}", sellerId, e);
	        // Handle the error and return an appropriate response
	        CommonApiResponse errorResponse = new CommonApiResponse();
	        errorResponse.setResponseMessage("Failed to delete seller");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@DeleteMapping("delete/seller/delivery-person")
	@Operation(summary = "Api to delete a delivery person")
	public ResponseEntity<CommonApiResponse> deleteDeliveryPerson(@RequestParam("deliveryId") int deliveryId) {
	    LOG.info("Received request to delete delivery person with ID: {}", deliveryId);

	    try {
	        ResponseEntity<CommonApiResponse> responseEntity = userResource.deleteDeliveryPerson(deliveryId);
	        LOG.info("Delivery person deletion request processed successfully");
	        return responseEntity;
	    } catch (Exception e) {
	        LOG.error("Error occurred while deleting delivery person with ID: {}", deliveryId, e);
	        // Handle the error and return an appropriate response
	        CommonApiResponse errorResponse = new CommonApiResponse();
	        errorResponse.setResponseMessage("Failed to delete delivery person");
	        errorResponse.setSuccess(false);
	        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
}
