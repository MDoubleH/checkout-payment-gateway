package com.checkout.payment.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.exception.AcquirerException;
import com.checkout.payment.gateway.exception.AcquirerException.Reason;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @MockBean
  private PaymentsRepository paymentsRepository;

  @Test
  void authorizedPaymentIsCreatedAndStored() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankPaymentResponse(true, "authorization-code"));

    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest("4111111111111111")))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(header().exists("X-Request-ID"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("1111"))
        .andExpect(jsonPath("$.expiry_month").value(12))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentsRepository).add(paymentCaptor.capture());
    Payment storedPayment = paymentCaptor.getValue();
    assertThat(storedPayment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(storedPayment.cardNumberLastFour()).isEqualTo("1111");
  }

  @Test
  void declinedPaymentIsCreatedAndStored() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankPaymentResponse(false, ""));

    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest("4111111111111112")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.card_number_last_four").value("1112"));

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentsRepository).add(paymentCaptor.capture());
    assertThat(paymentCaptor.getValue().status()).isEqualTo(PaymentStatus.DECLINED);
  }

  @Test
  void storedPaymentCanBeRetrieved() throws Exception {
    UUID id = UUID.randomUUID();
    Payment payment = new Payment(
        id, PaymentStatus.AUTHORIZED, "0042", 12, 2030, SupportedCurrency.USD, 250);
    when(paymentsRepository.get(id)).thenReturn(Optional.of(payment));

    mvc.perform(get("/payments/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("0042"))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(250));

    verifyNoInteractions(acquiringBankClient);
  }

  @Test
  void rejectedPaymentDoesNotCallBankOrWriteRepository() throws Exception {
    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(header().exists("X-Request-ID"))
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED"))
        .andExpect(jsonPath("$.request_id").isNotEmpty())
        .andExpect(jsonPath("$.field_errors[0].field").value("amount"));

    verifyNoInteractions(acquiringBankClient);
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void invalidCurrencyJsonIsRejectedBeforeControllerExecution() throws Exception {
    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest("4111111111111111").replace("GBP", "AUD")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED"));

    verifyNoInteractions(acquiringBankClient);
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void malformedPaymentIdReturnsSanitizedBadRequest() throws Exception {
    mvc.perform(get("/payments/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAYMENT_ID"))
        .andExpect(jsonPath("$.message").value("Payment ID must be a valid UUID"))
        .andExpect(jsonPath("$.request_id").isNotEmpty());
  }

  @Test
  void unknownPaymentReturnsSanitizedNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    mvc.perform(get("/payments/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Payment was not found"));
  }

  @Test
  void unavailableBankReturnsServiceUnavailableWithoutStorage() throws Exception {
    assertBankFailure(Reason.UNAVAILABLE, 503, "ACQUIRER_UNAVAILABLE");
  }

  @Test
  void bankTimeoutReturnsGatewayTimeoutWithoutStorage() throws Exception {
    assertBankFailure(Reason.TIMEOUT, 504, "ACQUIRER_TIMEOUT");
  }

  @Test
  void invalidBankResponseReturnsBadGatewayWithoutStorage() throws Exception {
    assertBankFailure(Reason.INVALID_RESPONSE, 502, "ACQUIRER_INVALID_RESPONSE");
  }

  private void assertBankFailure(Reason reason, int expectedStatus, String expectedCode)
      throws Exception {
    when(acquiringBankClient.authorize(any())).thenThrow(new AcquirerException(reason));

    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest("4111111111111111")))
        .andExpect(status().is(expectedStatus))
        .andExpect(jsonPath("$.code").value(expectedCode))
        .andExpect(jsonPath("$.request_id").isNotEmpty());

    verify(paymentsRepository, never()).add(any());
  }

  private String validRequest(String cardNumber) {
    return """
        {
          "card_number": "%s",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 100,
          "cvv": "123"
        }
        """.formatted(cardNumber);
  }

  private String invalidRequest() {
    return """
        {
          "card_number": "4111111111111111",
          "expiry_month": 12,
          "expiry_year": 2030,
          "currency": "GBP",
          "amount": 0,
          "cvv": "123"
        }
        """;
  }
}
