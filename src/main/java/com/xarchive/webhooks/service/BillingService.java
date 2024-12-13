package com.xarchive.webhooks.service;

import com.xarchive.billing.entity.Payment;
import com.xarchive.billing.payload.LicenseInfo;
import com.xarchive.billing.payload.PaymentCenterResponse;
import com.xarchive.billing.payload.PaymentInfo;
import com.xarchive.billing.payload.PlanInfo;

import com.xarchive.billing.repository.PaymentRepository;
import com.xarchive.billing.payload.PaymentCenterResponse.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BillingService {

    private final PaymentRepository paymentRepository;

    public BillingService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentCenterResponse getPayments(Integer userId) {
        List<Payment> entityPayments = paymentRepository.findAllByUserId(userId);

        List<PaymentInfo> payments = entityPayments.stream().map(entityPayment -> {
            PaymentInfo payment = new PaymentInfo();
            payment.setPrice(entityPayment.getAmount());
            payment.setPurchaseDate(Date.from(entityPayment.getPaymentDate()));
            payment.setLicenseId(entityPayment.getLicense().getId());

            LicenseInfo licenseInfo = new LicenseInfo();
            licenseInfo.setId(entityPayment.getLicense().getId());

            PlanInfo planInfo = new PlanInfo();
            planInfo.setId(entityPayment.getLicense().getLicenseType().getId());
            planInfo.setName(entityPayment.getLicense().getLicenseType().getPlanName());
            planInfo.setBillingCycle(entityPayment.getLicense().getLicenseType().getBillingCycle());
            licenseInfo.setLicenseType(planInfo);

            payment.setLicense(licenseInfo);

            log.info("Mapped Payment with Plan Name: {}", planInfo.getName());

            return payment;
        }).collect(Collectors.toList());

        PaymentCenterResponse response = new PaymentCenterResponse();
        response.setPayments(payments);
        return response;
    }
}
