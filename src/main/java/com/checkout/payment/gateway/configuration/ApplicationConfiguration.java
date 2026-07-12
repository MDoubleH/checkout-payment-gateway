package com.checkout.payment.gateway.configuration;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
