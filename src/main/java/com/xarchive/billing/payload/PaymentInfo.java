package com.xarchive.billing.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentInfo {
    private BigDecimal price;
    private Date purchaseDate;
    private Integer licenseId;
    private LicenseInfo license;
}