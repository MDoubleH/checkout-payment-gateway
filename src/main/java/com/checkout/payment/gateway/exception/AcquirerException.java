package com.checkout.payment.gateway.exception;

public class AcquirerException extends RuntimeException {

  private final Reason reason;

  public AcquirerException(Reason reason) {
    super("Acquiring bank request failed: " + reason.name());
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }

  public enum Reason {
    INVALID_RESPONSE,
    TIMEOUT,
    UNAVAILABLE
  }
}
