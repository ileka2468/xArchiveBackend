package com.xarchive.authentication.payload;

import lombok.Data;

@Data
public class NameChangeRequest {
    private String firstName;
    private String lastName;
}
