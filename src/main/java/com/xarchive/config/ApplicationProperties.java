package com.xarchive.config;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ApplicationProperties {
    private String dataSourceUrl;
    private String dataSourceUsername;
    private String dataSourcePassword;
    private int hikariIdleTimeout;
    private int hikariMaxLifetime;
    private int hikariMinimumIdleConnections;
    private int hikariMaximumPoolSize;
    private String jwtSecret;
    private int jwtExpirationInMs;
    private int refreshTokenExpirationInMs;
    private String env;
}
