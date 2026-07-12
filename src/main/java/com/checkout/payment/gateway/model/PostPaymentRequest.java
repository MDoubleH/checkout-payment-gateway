package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.validation.ValidCardExpiry;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@ValidCardExpiry
public class PostPaymentRequest {

  @JsonProperty("card_number")
  @NotBlank(message = "card number is required")
  @Size(min = 14, max = 19, message = "card number must be between 14 and 19 characters")
  @Pattern(regexp = "[0-9]+", message = "card number must contain only digits")
  private String cardNumber;
  @JsonProperty("expiry_month")
  @NotNull(message = "expiry month is required")
  @Min(value = 1, message = "expiry month must be at least 1")
  @Max(value = 12, message = "expiry month must be at most 12")
  private Integer expiryMonth;
  @JsonProperty("expiry_year")
  @NotNull(message = "expiry year is required")
  private Integer expiryYear;
  @JsonProperty("currency")
  @NotNull(message = "currency is required")
  private SupportedCurrency currency;
  @JsonProperty("amount")
  @NotNull(message = "amount is required")
  @Positive(message = "amount must be positive")
  private Integer amount;
  @JsonProperty("cvv")
  @NotBlank(message = "cvv is required")
  @Size(min = 3, max = 4, message = "cvv must be between 3 and 4 characters")
  @Pattern(regexp = "[0-9]+", message = "cvv must contain only digits")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public SupportedCurrency getCurrency() {
    return currency;
  }

  public void setCurrency(SupportedCurrency currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }
}
