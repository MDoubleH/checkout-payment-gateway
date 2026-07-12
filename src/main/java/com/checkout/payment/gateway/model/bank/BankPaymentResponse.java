package com.checkout.payment.gateway.model.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankPaymentResponse {

  private Boolean authorized;
  private String authorizationCode;

  public BankPaymentResponse() {
  }

  public BankPaymentResponse(Boolean authorized, String authorizationCode) {
    this.authorized = authorized;
    this.authorizationCode = authorizationCode;
  }

  @JsonProperty("authorized")
  public Boolean getAuthorized() {
    return authorized;
  }

  public void setAuthorized(Boolean authorized) {
    this.authorized = authorized;
  }

  @JsonProperty("authorization_code")
  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }
}
