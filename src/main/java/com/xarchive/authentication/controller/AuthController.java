package com.xarchive.authentication.controller;

import com.xarchive.authentication.entity.*;
import com.xarchive.authentication.payload.*;
import com.xarchive.authentication.repository.AuthpinRepository;
import com.xarchive.authentication.service.AuthResponse;
import com.xarchive.authentication.repository.RoleRepository;
import com.xarchive.authentication.repository.UserRepository;
import com.xarchive.authentication.repository.UserRoleRepository;
import com.xarchive.authentication.security.JwtTokenProvider;
import com.xarchive.authentication.service.AuthpinService;
import com.xarchive.authentication.service.CustomUserDetailsService;
import com.xarchive.authentication.util.UserPrincipal;
import com.xarchive.emails.service.EmailService;
import com.xarchive.invalidatedtokens.service.InvalidateTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Log4j2
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    InvalidateTokenService invalidateTokenService;


    @Value("${app.refreshtokenExpirationInMs}")
    private int refreshtokenExpirationInMs;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AuthpinService authpinService;
    @Autowired
    private AuthpinRepository authpinRepository;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        String firstname, lastname;
        try {
             firstname = registerRequest.getName().split(" ")[0];
             lastname = registerRequest.getName().split(" ")[1];
        } catch ( IndexOutOfBoundsException e ) {
            return ResponseEntity.badRequest().body("Must have first and last name!");
        }


        user.setFirstName(firstname);
        user.setLastName(lastname);
        user.setEnabled(true);

        userRepository.save(user);
        Optional<User> savedUser = userRepository.findByUsername(user.getUsername());
        if (savedUser.isPresent()) {
            user = savedUser.get();
        } else {
            throw new IllegalStateException("User was not saved successfully.");
        }

        Role userRole = roleRepository.findByRoleName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setRoleName("ROLE_USER");
            roleRepository.save(userRole);
            userRole = roleRepository.findByRoleName("ROLE_USER");
            if (userRole == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to create role.");
            }
        }

        try {
            UserRole userRoleLink = new UserRole();
            UserRoleId userRoleId = new UserRoleId();
            userRoleId.setUserId(user.getId());
            userRoleId.setRoleId(userRole.getId());
            userRoleLink.setId(userRoleId);
            userRoleLink.setUser(user);
            userRoleLink.setRole(userRole);
            userRoleRepository.save(userRoleLink);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to assign role to user.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            registerRequest.getUsername(),
                            registerRequest.getPassword()
                    )

            );

            AuthResponse authResponse = tokenProvider.getTokens(authentication);
            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getAccessToken());
            response.addCookie(authResponse.getRefreshCookie());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to authenticate user after registration.");
        }

        return ResponseEntity.ok(new AuthDataResponse(user.getUsername(), user.getFirstName(), user.getLastName()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            AuthResponse authResponse = tokenProvider.getTokens(authentication);
            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getAccessToken());
            response.addCookie(authResponse.getRefreshCookie());
            User user = customUserDetailsService.getUserByUsername(loginRequest.getUsername());
            return ResponseEntity.ok(new AuthDataResponse(user.getUsername(), user.getFirstName(), user.getLastName()));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletResponse response, HttpServletRequest request) {
        if (request.getCookies() == null || request.getCookies().length == 0) {
            return ResponseEntity.badRequest().body("Refresh token is empty");
        }

        String refreshToken = tokenProvider.getRefreshTokenFromCookies(request);

        if (refreshToken != null) {
            boolean isValid = tokenProvider.validateToken(refreshToken);
            if (isValid) {
                try {
                    String usernameFromJWT = tokenProvider.getUsernameFromJWT(refreshToken);
                    UserPrincipal userDetails = (UserPrincipal) userDetailsService.loadUserByUsername(usernameFromJWT);
                    String newAccessToken = tokenProvider.generateToken(userDetails.getUsername(), userDetails.getAuthorities(), TokenType.JWT);
                    response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);
                    return ResponseEntity.ok().build();
                } catch (AuthenticationException e) {
                    log.error("e: ", e);
                }
            } else {
                log.warn("Invalid refresh token: {}", refreshToken);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String refreshToken = tokenProvider.getRefreshTokenFromCookies(request);
        String accessToken = tokenProvider.getJwtFromRequest(request);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Access token not provided.");
        }
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token not provided.");
        }

        boolean accessTokenValid = tokenProvider.validateToken(accessToken);
        boolean refreshTokenValid = tokenProvider.validateToken(refreshToken);

        if (!accessTokenValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid access token");
        }
        if (!refreshTokenValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        boolean tokensDeactivated = invalidateTokenService.invalidateTokens(accessToken, refreshToken);
        if (tokensDeactivated) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAuthedUserData(HttpServletRequest request) {
        String accessToken = tokenProvider.getJwtFromRequest(request);
        String username = tokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to retrieve user.");
        }

        return ResponseEntity.ok(new AuthDataResponse(user.getUsername(), user.getFirstName(), user.getLastName()));
    }

    @PostMapping("/changeEmail")
    public ResponseEntity<?> changeEmail(HttpServletRequest request, @RequestBody EmailResetRequest emailResetRequest) {
        String accessToken = tokenProvider.getJwtFromRequest(request);
        String username = tokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);
        String newEmail = emailResetRequest.getEmail();

        String pin = getPin();
        authpinService.createPin(user, pin, Instant.now().plusMillis(900000), "CHANGE_EMAIL", newEmail);

        boolean sent = emailService.sendPinEmail(user.getUsername(), "xArchive Email Reset Pin", pin);

        if (!sent) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unable to send email.");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Please check " + username + " for an MFA pin.");
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(HttpServletRequest request, @RequestBody PasswordResetRequest passwordResetRequest) {
        String accessToken = tokenProvider.getJwtFromRequest(request);
        String username = tokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);

        String newPassword = passwordResetRequest.getNewPassword();
        String confirmNewPassword = passwordResetRequest.getConfirmNewPassword();

        if (!newPassword.equals(confirmNewPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("New passwords do not match.");
        }

        // Password validation
        if (!isPasswordValid(newPassword)) {
            // Use a helper method to figure out missing requirements
            String missingRequirements = getMissingRequirements(newPassword);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    " Password missing " + missingRequirements
            );
        }

        String pin = getPin();
        authpinService.createPin(user, pin, Instant.now().plusMillis(900000), "CHANGE_PASSWORD", newPassword);

        boolean sent = emailService.sendPinEmail(user.getUsername(), "xArchive Password Reset Pin", pin);

        if (!sent) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unable to send email.");
        }
        return ResponseEntity.status(HttpStatus.OK).body("Please check " + username + " for an MFA pin.");
    }


    private boolean isPasswordValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        // password policy:
        // Minimum 5 characters, at least one uppercase letter, and at least one number.
        String passwordRegex = "^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{5,}$";
        return password.matches(passwordRegex);
    }

    private String getMissingRequirements(String password) {
        StringBuilder missingRequirements = new StringBuilder();

        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            missingRequirements.append("at least one uppercase letter, ");
        }

        // Check for at least one number
        if (!password.matches(".*\\d.*")) {
            missingRequirements.append("at least one number, ");
        }

        // Check for minimum length of 5 characters
        if (password.length() < 5) {
            missingRequirements.append("minimum length of 5 characters, ");
        }

        // Remove trailing comma and space
        if (!missingRequirements.isEmpty()) {
            missingRequirements.setLength(missingRequirements.length() - 2);
        }

        return missingRequirements.toString();
    }


    @PostMapping("/changeName")
    public ResponseEntity<?> changeEmail (HttpServletRequest request, @RequestBody NameChangeRequest nameChangeRequest) {
        String accessToken = tokenProvider.getJwtFromRequest(request);
        String username = tokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);

        String newFirstName = nameChangeRequest.getFirstName() != null ? nameChangeRequest.getFirstName().trim() : null;
        String newLastName = nameChangeRequest.getLastName() != null ? nameChangeRequest.getLastName().trim() : null;

        if (newFirstName == null || newLastName == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("First name and last name are required.");
        }

        if (newFirstName.length() < 3 || newLastName.length() < 3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("First and last name must be at least 3 characters.");
        }

        // Check for changes
        if (newFirstName.equals(user.getFirstName()) && newLastName.equals(user.getLastName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You must supply a different name than the current one.");
        }

        user.setFirstName(newFirstName);
        user.setLastName(newLastName);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.OK).body("Name successfully updated.");
    }



    @PostMapping("/verifyMfa")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaRequest mfaRequest, HttpServletRequest request) {
        String accessToken = tokenProvider.getJwtFromRequest(request);
        String username = tokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);

        boolean pinValid = authpinService.validatePin(user, mfaRequest.getMfaCode());
        if (!pinValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid pin");
        }

        // Retrieve the verified PIN to check its intent
        Optional<Authpin> verifiedPin = authpinRepository.findTopByUserAndPinCodeAndVerifiedTrueOrderByCreatedAtDesc(user, mfaRequest.getMfaCode());
        if (verifiedPin.isPresent()) {
            Authpin pin = verifiedPin.get();
            if ("CHANGE_EMAIL".equals(pin.getIntent())) {
                // Perform email change
                String newEmail = pin.getData();
                user.setUsername(newEmail);
                userRepository.save(user);
                return ResponseEntity.status(HttpStatus.OK).body("Email updated successfully.");
            } else if ("CHANGE_PASSWORD".equals(pin.getIntent())) {
                String newPassword = pin.getData();
                if (newPassword != null) {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(user);
                    return ResponseEntity.status(HttpStatus.OK).body("Password updated successfully.");
                }

            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No action performed for the provided PIN.");
    }

    private static String getPin() {
        Random random = new Random();
        String pin = String.format("%04d", random.nextInt(10000));
        return pin;
    }

}
