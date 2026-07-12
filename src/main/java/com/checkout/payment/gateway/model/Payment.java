package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.enums.SupportedCurrency;
import java.util.Objects;
import java.util.UUID;

public record Payment(
    UUID id,
    PaymentStatus status,
    String cardNumberLastFour,
    int expiryMonth,
    int expiryYear,
    SupportedCurrency currency,
    int amount) {

  public Payment {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(cardNumberLastFour, "last four digits must not be null");
    Objects.requireNonNull(currency, "currency must not be null");
    if (!cardNumberLastFour.matches("[0-9]{4}")) {
      throw new IllegalArgumentException("last four digits must contain exactly four digits");
    }
    if (status == PaymentStatus.REJECTED) {
      throw new IllegalArgumentException("rejected requests must not be stored as payments");
    }
  }
}
