package com.quince.lawyeraiassistant.evaluation.live;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LiveEvaluationProperties.class)
public class LiveEvaluationConfiguration {
}