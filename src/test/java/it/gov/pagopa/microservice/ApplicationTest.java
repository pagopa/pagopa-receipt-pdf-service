package it.gov.pagopa.microservice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTest {

  @Test
  void contextLoads() {
    // check only if the context is loaded
    assertTrue(true);
  }
}
