package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Process and retrieve card payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  @Operation(summary = "Process a card payment")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment authorized or declined",
          content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
      @ApiResponse(responseCode = "400", description = "Payment request rejected",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "502", description = "Invalid acquiring-bank response",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "503", description = "Acquiring bank unavailable",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "504", description = "Acquiring-bank timeout",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<PaymentResponse> createPayment(
      @Valid @RequestBody PostPaymentRequest request) {
    Payment payment = paymentGatewayService.processPayment(request);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(payment.id())
        .toUri();
    return ResponseEntity.created(location).body(PaymentResponse.from(payment));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Retrieve a stored payment")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found",
          content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
      @ApiResponse(responseCode = "400", description = "Payment ID is not a UUID",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "Payment not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    Payment payment = paymentGatewayService.getPaymentById(id);
    return ResponseEntity.ok(PaymentResponse.from(payment));
  }
}
