package com.xarchive.authentication.payload;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}
