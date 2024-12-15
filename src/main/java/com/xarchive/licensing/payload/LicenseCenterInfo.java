package com.xarchive.licensing.payload;
import com.xarchive.billing.payload.LicenseInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LicenseCenterInfo extends LicenseInfo {
    private String licenseNumber;
}
