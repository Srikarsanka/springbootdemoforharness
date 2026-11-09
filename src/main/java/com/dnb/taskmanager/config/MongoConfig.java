package com.dnb.taskmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
@Profile("!test")
public class MongoConfig {
    // MongoDB auditing is enabled to support @CreatedDate and @LastModifiedDate annotations.
    // MongoTemplate is auto-configured by Spring Boot's spring-boot-starter-data-mongodb.
}
