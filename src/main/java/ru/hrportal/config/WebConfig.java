package ru.hrportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(
                        new org.springframework.web.servlet.HandlerInterceptor() {
                            @Override
                            public boolean preHandle(
                                    jakarta.servlet.http.HttpServletRequest req,
                                    jakarta.servlet.http.HttpServletResponse res,
                                    Object handler) {
                                res.setHeader("Cache-Control", "no-store");
                                return true;
                            }
                        })
                .addPathPatterns("/api/**");
    }
}
