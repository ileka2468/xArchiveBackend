package com.xarchive.authentication.controller;

import com.xarchive.authentication.entity.Role;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.entity.UserRole;
import com.xarchive.authentication.entity.UserRoleId;
import com.xarchive.authentication.payload.AuthDataResponse;
import com.xarchive.authentication.service.AuthResponse;
import com.xarchive.authentication.payload.LoginRequest;
import com.xarchive.authentication.payload.RegisterRequest;
import com.xarchive.authentication.payload.TokenType;
import com.xarchive.authentication.repository.RoleRepository;
import com.xarchive.authentication.repository.UserRepository;
import com.xarchive.authentication.repository.UserRoleRepository;
import com.xarchive.authentication.security.JwtTokenProvider;
import com.xarchive.authentication.service.CustomUserDetailsService;
import com.xarchive.authentication.util.UserPrincipal;
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


import java.util.Arrays;
import java.util.Optional;

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


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        String firstname = registerRequest.getName().split(" ")[0];
        String lastname = registerRequest.getName().split(" ")[1];

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

}
