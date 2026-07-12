package com.checkout.payment.gateway.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@Import(PostPaymentRequestValidationTest.FixedClockConfiguration.class)
class PostPaymentRequestValidationTest {

  private static final Clock FIXED_CLOCK = Clock.fixed(
      Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC);

  @Autowired
  private Validator validator;

  @Test
  void validRequestHasNoViolations() {
    assertThat(validator.validate(validRequest())).isEmpty();
  }

  @Test
  void missingCardNumberIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(null);

    assertThat(violationsFor(request, "cardNumber")).isNotEmpty();
  }

  @Test
  void missingExpiryMonthIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(null);

    assertThat(violationsFor(request, "expiryMonth")).isNotEmpty();
  }

  @Test
  void missingExpiryYearIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(null);

    assertThat(violationsFor(request, "expiryYear")).isNotEmpty();
  }

  @Test
  void missingCurrencyIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setCurrency(null);

    assertThat(violationsFor(request, "currency")).isNotEmpty();
  }

  @Test
  void missingAmountIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setAmount(null);

    assertThat(violationsFor(request, "amount")).isNotEmpty();
  }

  @Test
  void missingCvvIsRejected() {
    PostPaymentRequest request = validRequest();
    request.setCvv(null);

    assertThat(violationsFor(request, "cvv")).isNotEmpty();
  }

  @Test
  void blankCardNumberAndCvvAreRejected() {
    assertThat(cardNumberViolations(" ")).isNotEmpty();
    assertThat(cvvViolations(" ")).isNotEmpty();
  }

  @Test
  void cardNumberLengthBoundariesAndDigitsAreValidated() {
    assertThat(cardNumberViolations("1".repeat(13))).isNotEmpty();
    assertThat(cardNumberViolations("1".repeat(14))).isEmpty();
    assertThat(cardNumberViolations("1".repeat(19))).isEmpty();
    assertThat(cardNumberViolations("1".repeat(20))).isNotEmpty();
    assertThat(cardNumberViolations("1".repeat(13) + "a")).isNotEmpty();
  }

  @Test
  void expiryMonthBoundariesAreValidated() {
    assertThat(expiryMonthViolations(0)).isNotEmpty();
    assertThat(expiryMonthViolations(1)).isEmpty();
    assertThat(expiryMonthViolations(12)).isEmpty();
    assertThat(expiryMonthViolations(13)).isNotEmpty();
  }

  @Test
  void amountMustBePositive() {
    assertThat(amountViolations(-1)).isNotEmpty();
    assertThat(amountViolations(0)).isNotEmpty();
    assertThat(amountViolations(1)).isEmpty();
  }

  @Test
  void cvvLengthDigitsAndLeadingZeroAreValidated() {
    assertThat(cvvViolations("12")).isNotEmpty();
    assertThat(cvvViolations("123")).isEmpty();
    assertThat(cvvViolations("1234")).isEmpty();
    assertThat(cvvViolations("12345")).isNotEmpty();
    assertThat(cvvViolations("12a")).isNotEmpty();
    assertThat(cvvViolations("012")).isEmpty();
  }

  @Test
  void previousCurrentAndFutureExpiryMonthsAreValidated() {
    assertThat(cardExpiryViolations(6, 2026)).isNotEmpty();
    assertThat(cardExpiryViolations(7, 2026)).isEmpty();
    assertThat(cardExpiryViolations(8, 2026)).isEmpty();
  }

  @Test
  void previousAndFutureExpiryYearsAreValidated() {
    assertThat(cardExpiryViolations(12, 2025)).isNotEmpty();
    assertThat(cardExpiryViolations(1, 2027)).isEmpty();
  }

  @Test
  void missingExpiryComponentsAreLeftToFieldConstraints() {
    PostPaymentRequest missingMonth = validRequest();
    missingMonth.setExpiryMonth(null);
    assertThat(cardExpiryViolations(missingMonth)).isEmpty();
    assertThat(violationsFor(missingMonth, "expiryMonth")).isNotEmpty();

    PostPaymentRequest missingYear = validRequest();
    missingYear.setExpiryYear(null);
    assertThat(cardExpiryViolations(missingYear)).isEmpty();
    assertThat(violationsFor(missingYear, "expiryYear")).isNotEmpty();
  }

  @Test
  void invalidExpiryMonthsAreLeftToFieldConstraints() {
    PostPaymentRequest monthBelowRange = validRequest();
    monthBelowRange.setExpiryMonth(0);
    assertThat(cardExpiryViolations(monthBelowRange)).isEmpty();
    assertThat(violationsFor(monthBelowRange, "expiryMonth")).isNotEmpty();

    PostPaymentRequest monthAboveRange = validRequest();
    monthAboveRange.setExpiryMonth(13);
    assertThat(cardExpiryViolations(monthAboveRange)).isEmpty();
    assertThat(violationsFor(monthAboveRange, "expiryMonth")).isNotEmpty();
  }

  @Test
  void unrepresentableExpiryYearIsRejectedWithoutThrowing() {
    assertThat(cardExpiryViolations(12, Integer.MAX_VALUE)).isNotEmpty();
  }

  private Set<ConstraintViolation<PostPaymentRequest>> cardNumberViolations(String cardNumber) {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(cardNumber);
    return violationsFor(request, "cardNumber");
  }

  private Set<ConstraintViolation<PostPaymentRequest>> expiryMonthViolations(Integer expiryMonth) {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(expiryMonth);
    return violationsFor(request, "expiryMonth");
  }

  private Set<ConstraintViolation<PostPaymentRequest>> amountViolations(Integer amount) {
    PostPaymentRequest request = validRequest();
    request.setAmount(amount);
    return violationsFor(request, "amount");
  }

  private Set<ConstraintViolation<PostPaymentRequest>> cvvViolations(String cvv) {
    PostPaymentRequest request = validRequest();
    request.setCvv(cvv);
    return violationsFor(request, "cvv");
  }

  private Set<ConstraintViolation<PostPaymentRequest>> cardExpiryViolations(
      Integer expiryMonth, Integer expiryYear) {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(expiryMonth);
    request.setExpiryYear(expiryYear);
    return cardExpiryViolations(request);
  }

  private Set<ConstraintViolation<PostPaymentRequest>> cardExpiryViolations(
      PostPaymentRequest request) {
    return validator.validate(request).stream()
        .filter(violation -> violation.getConstraintDescriptor().getAnnotation()
            .annotationType().equals(ValidCardExpiry.class))
        .collect(Collectors.toSet());
  }

  private Set<ConstraintViolation<PostPaymentRequest>> violationsFor(
      PostPaymentRequest request, String propertyName) {
    return validator.validate(request).stream()
        .filter(violation -> violation.getPropertyPath().toString().equals(propertyName))
        .collect(Collectors.toSet());
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);
    request.setCurrency(SupportedCurrency.GBP);
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {

    @Bean
    @Primary
    Clock fixedClock() {
      return FIXED_CLOCK;
    }
  }
}
