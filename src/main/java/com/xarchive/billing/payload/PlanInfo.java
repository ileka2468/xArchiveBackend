package com.xarchive.billing.payload;

import lombok.Data;

@Data
public class PlanInfo {
    private Integer id;
    private String name;
    private String billingCycle;
}