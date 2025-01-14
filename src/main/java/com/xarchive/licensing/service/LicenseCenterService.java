package com.xarchive.licensing.service;

import com.xarchive.authentication.entity.User;
import com.xarchive.billing.payload.PlanInfo;
import com.xarchive.licensing.entity.License;
import com.xarchive.licensing.payload.LicenseCenterInfo;
import com.xarchive.licensing.repository.LicenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseCenterService {

    private final LicenseRepository licenseRepository;

    public LicenseCenterService(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public List<LicenseCenterInfo> getLicenses(User user) {
        List<License> licenses = licenseRepository.findAllByUserId(user.getId());

        if (licenses.isEmpty()) {
            return null;
        }

        return licenses.stream().map((license) -> {
            LicenseCenterInfo licenseCenterInfo = new LicenseCenterInfo();
            licenseCenterInfo.setLicenseNumber(license.getLicenseNumber());
            licenseCenterInfo.setId(license.getId());

            PlanInfo planInfo = new PlanInfo();
            planInfo.setId(license.getLicenseType().getId());
            planInfo.setName(license.getLicenseType().getPlanName());
            planInfo.setBillingCycle(license.getLicenseType().getBillingCycle());

            licenseCenterInfo.setLicenseType(planInfo);
            return licenseCenterInfo;
        }).toList();
    }

}
