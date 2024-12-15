package com.xarchive.licensing.payload;

import com.xarchive.licensing.payload.LicenseCenterInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LicenseCenterResponse {
    List<LicenseCenterInfo> licenses;
}
