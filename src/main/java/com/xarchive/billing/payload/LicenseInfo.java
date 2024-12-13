package com.xarchive.billing.payload;

import lombok.Data;

@Data
public class LicenseInfo {
    private Integer id;
    private PlanInfo licenseType;
}