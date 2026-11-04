package io.github.zerroukzakaria.civicdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CivicDataApplicationTests {

    @Test
    void contextLoads() {
    }
}