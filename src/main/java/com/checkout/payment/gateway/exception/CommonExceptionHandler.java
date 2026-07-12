package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.FieldErrorResponse;
import java.util.Comparator;
import java.util.List;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getAllErrors().stream()
        .map(error -> {
          String field = error instanceof FieldError fieldError
              ? toSnakeCase(fieldError.getField())
              : "expiry_date";
          String message = error.getDefaultMessage();
          return new FieldErrorResponse(field, message);
        })
        .sorted(Comparator.comparing(FieldErrorResponse::field)
            .thenComparing(FieldErrorResponse::message))
        .toList();
    return response(
        HttpStatus.BAD_REQUEST,
        PaymentStatus.REJECTED,
        "PAYMENT_REJECTED",
        "Payment request is invalid",
        fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableRequest() {
    return response(
        HttpStatus.BAD_REQUEST,
        PaymentStatus.REJECTED,
        "PAYMENT_REJECTED",
        "Payment request is malformed",
        List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPaymentId() {
    return response(
        HttpStatus.BAD_REQUEST,
        null,
        "INVALID_PAYMENT_ID",
        "Payment ID must be a valid UUID",
        List.of());
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound() {
    return response(
        HttpStatus.NOT_FOUND,
        null,
        "PAYMENT_NOT_FOUND",
        "Payment was not found",
        List.of());
  }

  @ExceptionHandler(AcquirerException.class)
  public ResponseEntity<ErrorResponse> handleAcquirerError(AcquirerException exception) {
    LOG.warn("event=acquirer_error reason={}", exception.getReason());
    return switch (exception.getReason()) {
      case INVALID_RESPONSE -> response(
          HttpStatus.BAD_GATEWAY,
          null,
          "ACQUIRER_INVALID_RESPONSE",
          "Acquiring bank returned an invalid response",
          List.of());
      case TIMEOUT -> response(
          HttpStatus.GATEWAY_TIMEOUT,
          null,
          "ACQUIRER_TIMEOUT",
          "Acquiring bank request timed out",
          List.of());
      case UNAVAILABLE -> response(
          HttpStatus.SERVICE_UNAVAILABLE,
          null,
          "ACQUIRER_UNAVAILABLE",
          "Acquiring bank is unavailable",
          List.of());
    };
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    LOG.error("event=unexpected_error exception_type={}",
        exception.getClass().getSimpleName(), exception);
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        null,
        "INTERNAL_ERROR",
        "An unexpected error occurred",
        List.of());
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus httpStatus,
      PaymentStatus paymentStatus,
      String code,
      String message,
      List<FieldErrorResponse> fieldErrors) {
    ErrorResponse error = new ErrorResponse(
        paymentStatus, code, message, MDC.get("request_id"), fieldErrors);
    return ResponseEntity.status(httpStatus).body(error);
  }

  private String toSnakeCase(String value) {
    return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }
}
