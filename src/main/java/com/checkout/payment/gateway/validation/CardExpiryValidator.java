package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.YearMonth;

public class CardExpiryValidator
    implements ConstraintValidator<ValidCardExpiry, PostPaymentRequest> {

  private final Clock clock;

  public CardExpiryValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean isValid(
      PostPaymentRequest request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }

    Integer expiryMonth = request.getExpiryMonth();
    Integer expiryYear = request.getExpiryYear();

    if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12) {
      return true;
    }

    YearMonth expiry;
    try {
      expiry = YearMonth.of(expiryYear, expiryMonth);
    } catch (DateTimeException exception) {
      return false;
    }

    YearMonth current = YearMonth.now(clock);
    return !expiry.isBefore(current);
  }
}
