package com.ecommerce.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.ecommerce.dto.CommonApiResponse;
import com.ecommerce.dto.RegisterUserRequestDto;
import com.ecommerce.dto.UserDto;
import com.ecommerce.dto.UserLoginRequest;
import com.ecommerce.dto.UserLoginResponse;
import com.ecommerce.dto.UserResponseDto;
import com.ecommerce.dto.UserStatusUpdateRequestDto;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.UserSaveFailedException;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.UserService;
import com.ecommerce.utility.Constants.ProductStatus;
import com.ecommerce.utility.Constants.UserRole;
import com.ecommerce.utility.Constants.UserStatus;
import com.ecommerce.utility.JwtUtils;

import jakarta.transaction.Transactional;

@Component
@Transactional
public class UserResource {

	private final Logger LOG = LoggerFactory.getLogger(UserResource.class);

	@Autowired
	private UserService userService;

	@Autowired
	private AddressService addressService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private ProductService productService;

	@Autowired
	private JwtUtils jwtUtils;

	public ResponseEntity<CommonApiResponse> registerAdmin(RegisterUserRequestDto registerRequest) {
		LOG.info("Request received for Register Admin");

		CommonApiResponse response = new CommonApiResponse();

		if (registerRequest == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(false);

			LOG.error("Received a null request for Register Admin");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		if (registerRequest.getEmailId() == null || registerRequest.getPassword() == null) {
			response.setResponseMessage("missing input");
			response.setSuccess(false);

			LOG.warn("Received a request with missing input for Register Admin");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmailAndStatus(registerRequest.getEmailId(),
				UserStatus.ACTIVE.value());

		if (existingUser != null) {
			response.setResponseMessage("User already registered with this Email");
			response.setSuccess(false);

			LOG.warn("User already registered with email: {}", registerRequest.getEmailId());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = RegisterUserRequestDto.toUserEntity(registerRequest);

		user.setRole(UserRole.ROLE_ADMIN.value());
		user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
		user.setStatus(UserStatus.ACTIVE.value());

		existingUser = this.userService.addUser(user);

		if (existingUser == null) {
			response.setResponseMessage("Failed to register admin");
			response.setSuccess(false);

			LOG.error("Failed to register admin");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		response.setResponseMessage("Admin registered successfully");
		response.setSuccess(true);

		LOG.info("Admin registered Successfully");

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> registerUser(RegisterUserRequestDto request) {

		LOG.info("Received request for register user");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(false);

			LOG.error("Received a null request for register user");
			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmailAndStatus(request.getEmailId(), UserStatus.ACTIVE.value());

		if (existingUser != null) {
			response.setResponseMessage("User with this Email Id already resgistered!!!");
			response.setSuccess(false);

			LOG.error("User with email '{}' already registered.", request.getEmailId());
			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getRole() == null) {
			response.setResponseMessage("bad request ,Role is missing");
			response.setSuccess(false);

			LOG.error("Received a request with missing Role");

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User user = RegisterUserRequestDto.toUserEntity(request);

		String encodedPassword = passwordEncoder.encode(user.getPassword());

		user.setStatus(UserStatus.ACTIVE.value());
		user.setPassword(encodedPassword);

		// delivery person is for seller, so we need to set Seller
		if (user.getRole().equals(UserRole.ROLE_DELIVERY.value())) {

			User seller = this.userService.getUserById(request.getSellerId());

			if (seller == null) {
				response.setResponseMessage("Seller not found,");
				response.setSuccess(false);

				LOG.error("Seller with ID '{}' not found.", request.getSellerId());

				return new ResponseEntity<CommonApiResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			user.setSeller(seller);

		}

		Address address = new Address();
		address.setCity(request.getCity());
		address.setPincode(request.getPincode());
		address.setStreet(request.getStreet());

		Address savedAddress = this.addressService.addAddress(address);

		if (savedAddress == null) {
			LOG.error("Failed to save address for user registration");
			throw new UserSaveFailedException("Registration Failed because of Technical issue:(");
		}

		user.setAddress(savedAddress);
		existingUser = this.userService.addUser(user);

		if (existingUser == null) {
			LOG.error("Failed to save address for user registration");
			throw new UserSaveFailedException("Registration Failed because of Technical issue:(");
		}

		response.setResponseMessage("User registered Successfully");
		response.setSuccess(true);

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserLoginResponse> login(UserLoginRequest loginRequest) {

		LOG.info("Received request for User Login");

		UserLoginResponse response = new UserLoginResponse();

		if (loginRequest == null) {
			response.setResponseMessage("Missing Input");
			response.setSuccess(false);

			LOG.error("Received a null request for User Login");
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

		String jwtToken = null;
		User user = null;

		List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority(loginRequest.getRole()));

		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmailId(),
					loginRequest.getPassword(), authorities));
		} catch (Exception ex) {
			response.setResponseMessage("Invalid email or password.");
			response.setSuccess(false);
			LOG.error("Authentication failed for email: {}", loginRequest.getEmailId(), ex);
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

		jwtToken = jwtUtils.generateToken(loginRequest.getEmailId());

		user = this.userService.getUserByEmailIdAndRoleAndStatus(loginRequest.getEmailId(), loginRequest.getRole(),
				UserStatus.ACTIVE.value());

		if (user == null) {
			response.setResponseMessage("User not found or inactive.");
			response.setSuccess(false);
			LOG.error("User not found or inactive for email: {}", loginRequest.getEmailId());
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		// user is authenticated

		UserDto userDto = UserDto.toUserDtoEntity(user);

		// user is authenticated
		if (jwtToken != null) {
			response.setUser(userDto);
			response.setResponseMessage("Logged in successful");
			response.setSuccess(true);
			response.setJwtToken(jwtToken);
			LOG.info("User login successful for email: {}", loginRequest.getEmailId());
			return new ResponseEntity<>(response, HttpStatus.OK);
		} else {
			response.setResponseMessage("Failed to login");
			response.setSuccess(false);
			LOG.error("Failed to generate JWT token for email: {}", loginRequest.getEmailId());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<UserResponseDto> getUsersByRole(String role) {
		UserResponseDto response = new UserResponseDto();

		if (role == null) {
			response.setResponseMessage("missing role");
			response.setSuccess(false);

			LOG.warn("Received request with missing role");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		List<User> users = this.userService.getUserByRoleAndStatus(role, UserStatus.ACTIVE.value());

		if (users.isEmpty()) {
			response.setResponseMessage("No Users Found");
			response.setSuccess(false);

			LOG.warn("No users found for role: {}", role);
		}

		List<UserDto> userDtos = new ArrayList<>();

		for (User user : users) {
			UserDto dto = UserDto.toUserDtoEntity(user);

			if (role.equals(UserRole.ROLE_DELIVERY.value())) {
				UserDto sellerDto = UserDto.toUserDtoEntity(user.getSeller());
				dto.setSeller(sellerDto);
			}

			userDtos.add(dto);
		}

		response.setUsers(userDtos);
		response.setResponseMessage("Users Fetched Successfully");
		response.setSuccess(true);

		LOG.info("Users fetched successfully for role: {}", role);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> updateUserStatus(UserStatusUpdateRequestDto request) {
		LOG.info("Received request for updating the user status");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);

			LOG.warn("Received a null request for updating user status");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, user id is missing");
			response.setSuccess(false);

			LOG.warn("Received a request with missing user id for updating user status");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User user = this.userService.getUserById(request.getUserId());

		if (user == null) {
			response.setResponseMessage("User not found with ID: " + request.getUserId());
			response.setSuccess(false);

			LOG.warn("User not found with ID: {}", request.getUserId());
			return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
		}

		user.setStatus(request.getStatus());

		User updatedUser = this.userService.updateUser(user);

		if (updatedUser == null) {
			throw new UserSaveFailedException("Failed to update the User status");
		}

		response.setResponseMessage("User status updated to " + request.getStatus() + " Successfully");
		response.setSuccess(true);

		LOG.info("User status updated successfully for user ID: {}", request.getUserId());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserResponseDto> getDeliveryPersonsBySeller(int sellerId) {

		UserResponseDto response = new UserResponseDto();

		if (sellerId == 0) {
			response.setResponseMessage("missing seller id");
			response.setSuccess(false);
			return new ResponseEntity<UserResponseDto>(response, HttpStatus.BAD_REQUEST);
		}

		User seller = this.userService.getUserById(sellerId);

		if (seller == null) {
			response.setResponseMessage("Seller not found");
			response.setSuccess(false);
			return new ResponseEntity<UserResponseDto>(response, HttpStatus.BAD_REQUEST);
		}

		List<User> users = new ArrayList<>();

		users = this.userService.getUserBySellerAndRoleAndStatusIn(seller, UserRole.ROLE_DELIVERY.value(),
				Arrays.asList(UserStatus.ACTIVE.value()));

		if (users.isEmpty()) {
			response.setResponseMessage("No Delivery Guys Found");
			response.setSuccess(false);
		}

		List<UserDto> userDtos = new ArrayList<>();

		for (User user : users) {

			UserDto dto = UserDto.toUserDtoEntity(user);
			userDtos.add(dto);

		}

		response.setUsers(userDtos);
		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		return new ResponseEntity<UserResponseDto>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> deleteSeller(int sellerId) {
		LOG.info("Received request to delete seller with ID: {}", sellerId);

		CommonApiResponse response = new CommonApiResponse();

		if (sellerId == 0) {
			response.setResponseMessage("missing seller id");
			response.setSuccess(false);

			LOG.warn("Received request with missing seller id");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		User seller = this.userService.getUserById(sellerId);

		if (seller == null) {
			response.setResponseMessage("Seller not found");
			response.setSuccess(false);

			LOG.warn("Seller not found with ID: {}", sellerId);
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		}

		List<User> deliveryPersons = this.userService.getUserBySellerAndRoleAndStatusIn(seller,
				UserRole.ROLE_DELIVERY.value(), Arrays.asList(UserStatus.ACTIVE.value()));
		List<Product> products = this.productService.getAllProductBySellerAndStatusIn(seller,
				Arrays.asList(ProductStatus.ACTIVE.value()));

		seller.setStatus(UserStatus.DEACTIVATED.value());
		User deletedSeller = this.userService.updateUser(seller);

		if (deletedSeller == null) {
			LOG.error("Failed to deactivate the seller with ID: {}", sellerId);
			throw new UserSaveFailedException("Failed to deactivate the seller");
		}

		if (!deliveryPersons.isEmpty()) {
			for (User deliveryPerson : deliveryPersons) {
				deliveryPerson.setStatus(UserStatus.DEACTIVATED.value());
			}

			List<User> deletedDeliveryPersons = this.userService.updateAllUser(deliveryPersons);

			if (CollectionUtils.isEmpty(deletedDeliveryPersons)) {
				LOG.error("Failed to deactivate delivery persons associated with seller with ID: {}", sellerId);
				throw new UserSaveFailedException("Failed to deactivate delivery persons associated with seller");
			}
		}

		if (!products.isEmpty()) {
			for (Product product : products) {
				product.setStatus(ProductStatus.DEACTIVATED.value());
			}

			List<Product> deletedProducts = this.productService.updateAllProduct(products);

			if (CollectionUtils.isEmpty(deletedProducts)) {
				LOG.error("Failed to deactivate products associated with seller with ID: {}", sellerId);
				throw new UserSaveFailedException("Failed to deactivate products associated with seller");
			}
		}

		response.setResponseMessage("Seller Deactivated Successfully");
		response.setSuccess(true);

		LOG.info("Seller with ID: {} deactivated successfully", sellerId);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> deleteDeliveryPerson(int deliveryId) {
	    LOG.info("Received request to delete delivery person with ID: {}", deliveryId);

	    CommonApiResponse response = new CommonApiResponse();

	    if (deliveryId == 0) {
	        response.setResponseMessage("missing delivery person id");
	        response.setSuccess(false);

	        LOG.warn("Received request with missing delivery person id");
	        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	    }

	    User delivery = this.userService.getUserById(deliveryId);

	    if (delivery == null) {
	        response.setResponseMessage("Delivery Person not found");
	        response.setSuccess(false);

	        LOG.warn("Delivery Person not found with ID: {}", deliveryId);
	        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	    }

	    delivery.setStatus(UserStatus.DEACTIVATED.value());
	    User deletedDelivery = this.userService.updateUser(delivery);

	    if (deletedDelivery == null) {
	        LOG.error("Failed to deactivate the delivery person with ID: {}", deliveryId);
	        throw new UserSaveFailedException("Failed to deactivate the delivery person");
	    }

	    response.setResponseMessage("Delivery Person Deactivated Successfully");
	    response.setSuccess(true);

	    LOG.info("Delivery person with ID: {} deactivated successfully", deliveryId);
	    return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
