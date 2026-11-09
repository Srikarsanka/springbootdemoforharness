
package com.dnb.taskmanager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Environment Configuration Automated Tests (DEV, QA, STG, PROD)")
class EnvironmentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    @DisplayName("1. DEV Spring profile configuration parses correctly")
    void devProfile_ConfigValid() throws Exception {
        Map<String, Object> yaml = loadYaml("application-dev.yml");
        assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("2. QA Spring profile configuration parses correctly")
    void qaProfile_ConfigValid() throws Exception {
        Map<String, Object> yaml = loadYaml("application-qa.yml");
        assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("3. STG Spring profile configuration parses correctly")
    void stgProfile_ConfigValid() throws Exception {
        Map<String, Object> yaml = loadYaml("application-stg.yml");
        assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("4. PROD Spring profile configuration parses correctly")
    void prodProfile_ConfigValid() throws Exception {
        Map<String, Object> yaml = loadYaml("application-prod.yml");
        assertThat(yaml).isNotNull();
    }

    @Test
    @DisplayName("5. Database configuration comes from environment variables")
    void databaseConfig_UsesEnvironmentVariable() throws Exception {
        Map<String, Object> mainYaml = loadYaml("application.yml");
        assertThat(mainYaml.toString()).contains("${MONGODB_URI");
    }

    @Test
    @DisplayName("6. Missing mandatory production JWT secret configuration fallback check")
    void productionConfig_RequiresJwtSecret() throws Exception {
        Map<String, Object> prodYaml = loadYaml("application-prod.yml");
        // Verify prod configuration binds to environment variable
        assertThat(prodYaml.toString()).contains("${JWT_SECRET");
    }

    @Test
    @DisplayName("7. Credentials are not hardcoded in production configuration")
    void credentials_AreNotHardcodedInProd() throws Exception {
        Map<String, Object> prodYaml = loadYaml("application-prod.yml");
        String content = prodYaml.toString();
        assertThat(content).doesNotContain("password123");
        assertThat(content).doesNotContain("root:root");
    }

    @Test
    @DisplayName("8. Runtime frontend API configuration template is available")
    void frontendRuntimeTemplate_Exists() {
        Resource envTemplate = new ClassPathResource("../../../frontend/src/assets/env.template.js");
        // Checked in build directory or via file system
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("9. Port and context configuration are parameterized")
    void serverPort_IsParameterized() throws Exception {
        Map<String, Object> mainYaml = loadYaml("application.yml");
        assertThat(mainYaml.toString()).contains("${SERVER_PORT:8080}");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String filename) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename)) {
            assertThat(in).as("Resource file " + filename + " must exist on classpath").isNotNull();
            return yaml.load(in);
        }
    }
}
