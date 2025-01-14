package com.xarchive.billing.payload;

import lombok.Data;

import java.util.List;

@Data
public class PaymentCenterResponse {
    private List<PaymentInfo> payments;
}
