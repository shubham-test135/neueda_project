package com.example.FinBuddy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for FinBuddy Application
 * This test verifies that the Spring application context loads successfully
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FinBuddy Application Tests")
class FinBuddyApplicationTests {

	@Test
	@DisplayName("Should load application context successfully")
	void contextLoads() {
		// This test will fail if the application context cannot start
		// It verifies all beans are properly configured and dependencies are satisfied
	}

}
