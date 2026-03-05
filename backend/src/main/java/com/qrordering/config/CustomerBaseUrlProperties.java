package com.qrordering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class CustomerBaseUrlProperties {

    /** Base URL of the customer ordering frontend (e.g. http://localhost:5173). Used to build table QR target URL. */
    private String customerBaseUrl = "http://localhost:5173";

    public String getCustomerBaseUrl() {
        return customerBaseUrl;
    }

    public void setCustomerBaseUrl(String customerBaseUrl) {
        this.customerBaseUrl = customerBaseUrl;
    }
}
