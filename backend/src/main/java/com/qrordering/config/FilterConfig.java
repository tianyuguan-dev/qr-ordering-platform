package com.qrordering.config;

import com.qrordering.auth.security.filter.SseTokenQueryFilter;
import com.qrordering.observability.MdcFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Register MdcFilter and SseTokenQueryFilter to run before the Spring Security chain.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter filter) {
        FilterRegistrationBean<MdcFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<SseTokenQueryFilter> sseTokenQueryFilterRegistration(SseTokenQueryFilter filter) {
        FilterRegistrationBean<SseTokenQueryFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return reg;
    }
}
