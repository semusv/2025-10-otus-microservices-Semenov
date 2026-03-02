package com.microservices.gateway.config;

import com.microservices.gateway.config.properties.SecurityProperties;
import com.microservices.gateway.config.properties.ServicesProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppProperties {
    private ServicesProperties services;

    private SecurityProperties security;
}
