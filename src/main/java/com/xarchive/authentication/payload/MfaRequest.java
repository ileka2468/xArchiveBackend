package com.xarchive.authentication.payload;

import lombok.Data;

@Data
public class MfaRequest {
    private String mfaCode;
}
