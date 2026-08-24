package com.quince.lawyeraiassistant.security.authentication.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.persistence.mybatis.ApplicationUserMapper;
import com.quince.lawyeraiassistant.security.identity.persistence.mybatis.MyBatisApplicationUserRepository;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.security.identity", name = "store", havingValue = "mybatis")
public class MyBatisIdentityConfiguration {

    @Bean
    ApplicationUserRepository applicationUserRepository(
            ApplicationUserMapper userMapper) {

        return new MyBatisApplicationUserRepository(
                userMapper);
    }
}