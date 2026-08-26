package com.quince.lawyeraiassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "app.cache.backend=CAFFEINE",
                "app.security.identity.store=in-memory",
                "app.rag.knowledge-base.enabled=false",
                "spring.ai.mcp.client.enabled=false",
                "spring.ai.mcp.server.enabled=false",
                "management.health.redis.enabled=false"
        })
class LawyerAiAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
