package com.natk.natk_api.departmentStorage.context.config;

import com.natk.natk_api.departmentStorage.context.DepartmentContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final DepartmentContextInterceptor departmentInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(departmentInterceptor)
                .addPathPatterns("/department-storage/**");
    }
}
