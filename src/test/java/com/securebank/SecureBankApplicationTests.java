package com.securebank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecureBankApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring Boot application context loads without errors
    }
}
