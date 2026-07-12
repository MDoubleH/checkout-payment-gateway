package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class BankPaymentRequest {

  private final String cardNumber;
  private final String expiryDate;
  private final String currency;
  private final int amount;
  private final String cvv;

  public BankPaymentRequest(
      String cardNumber, String expiryDate, String currency, int amount, String cvv) {
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  @JsonProperty("card_number")
  public String getCardNumber() {
    return cardNumber;
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return expiryDate;
  }

  @JsonProperty("currency")
  public String getCurrency() {
    return currency;
  }

  @JsonProperty("amount")
  public int getAmount() {
    return amount;
  }

  @JsonProperty("cvv")
  public String getCvv() {
    return cvv;
  }
}
