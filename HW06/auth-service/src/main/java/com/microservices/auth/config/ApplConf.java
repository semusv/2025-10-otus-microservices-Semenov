package com.microservices.auth.config;

import com.microservices.auth.config.properties.SecurityProperties;
import com.microservices.auth.filter.MdcFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@RequiredArgsConstructor
public class ApplConf {

    private final SecurityProperties securityProperties;

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> mdcFilterRegistrationBean() {
        var registrationBean = new FilterRegistrationBean<OncePerRequestFilter>();
        registrationBean.setFilter(new MdcFilter(securityProperties));
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
