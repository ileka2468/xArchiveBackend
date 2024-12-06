package com.xarchive.authentication.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
}
