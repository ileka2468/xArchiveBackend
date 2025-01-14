package com.xarchive.licensing.controller;

import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.security.JwtTokenProvider;
import com.xarchive.authentication.service.CustomUserDetailsService;
import com.xarchive.licensing.entity.License;
import com.xarchive.licensing.payload.LicenseCenterInfo;
import com.xarchive.licensing.payload.LicenseCenterResponse;
import com.xarchive.licensing.service.LicenseCenterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/licensing")
public class LicenseCenterController {

    private final LicenseCenterService licenseCenterService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public LicenseCenterController(LicenseCenterService licenseCenterService, CustomUserDetailsService customUserDetailsService, JwtTokenProvider jwtTokenProvider) {
        this.licenseCenterService = licenseCenterService;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/license")
    public ResponseEntity<?> getLicenses(HttpServletRequest request) {
        String jwt = jwtTokenProvider.getJwtFromRequest(request);
        String username = jwtTokenProvider.getUsernameFromJWT(jwt);
        User user = customUserDetailsService.getUserByUsername(username);

        List<LicenseCenterInfo> licenses = licenseCenterService.getLicenses(user);
        if (licenses== null ) {
            return ResponseEntity.ok(new LicenseCenterResponse(null));
        }
        return ResponseEntity.ok(new LicenseCenterResponse(licenses));
    }
}
