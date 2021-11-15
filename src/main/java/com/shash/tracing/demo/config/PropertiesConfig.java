package com.shash.tracing.demo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class PropertiesConfig {
    @Value("${topic.input}")
    public String inputTopic;

    @Value("${topic.output}")
    public String outputTopic;
}
