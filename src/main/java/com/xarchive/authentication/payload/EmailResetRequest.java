package com.xarchive.authentication.payload;

import lombok.Data;

@Data
public class EmailResetRequest {
    private String email;
}
