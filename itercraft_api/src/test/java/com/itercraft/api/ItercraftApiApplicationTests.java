package com.itercraft.api;

import com.itercraft.api.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSecurityConfig.class)
class ItercraftApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
