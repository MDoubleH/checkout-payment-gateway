package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    PaymentStatus status,
    @JsonProperty("card_number_last_four") String cardNumberLastFour,
    @JsonProperty("expiry_month") int expiryMonth,
    @JsonProperty("expiry_year") int expiryYear,
    SupportedCurrency currency,
    int amount) {

  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.id(),
        payment.status(),
        payment.cardNumberLastFour(),
        payment.expiryMonth(),
        payment.expiryYear(),
        payment.currency(),
        payment.amount());
  }
}
