package com.checkout.payment.gateway;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
    title = "Payment Gateway API",
    version = "1.0",
    description = "Validates card payments, forwards them to an acquiring bank and stores safe results."))
public class PaymentGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentGatewayApplication.class, args);
  }

}
