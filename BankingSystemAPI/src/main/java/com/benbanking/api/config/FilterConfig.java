package com.benbanking.api.config;

import com.benbanking.api.auth.SessionAuthFilter;
import com.benbanking.api.auth.SessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            @Value("${app.trust-proxy:false}") boolean trustProxy,
            @Value("${app.rate-limit.max-requests:10}") int maxRequests
    ) {
        return new RateLimitFilter(trustProxy, maxRequests);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/auth/login", "/api/auth/register", "/api/accounts/lookup");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    @Bean
    public SessionAuthFilter sessionAuthFilter(SessionService sessionService) {
        return new SessionAuthFilter(sessionService);
    }

    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilterRegistration(SessionAuthFilter filter) {
        FilterRegistrationBean<SessionAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
