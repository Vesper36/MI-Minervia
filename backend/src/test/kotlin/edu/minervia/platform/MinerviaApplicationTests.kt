package edu.minervia.platform

import edu.minervia.platform.config.TestR2Config
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestR2Config::class)
class MinerviaApplicationTests {

    @Test
    fun contextLoads() {
    }
}
