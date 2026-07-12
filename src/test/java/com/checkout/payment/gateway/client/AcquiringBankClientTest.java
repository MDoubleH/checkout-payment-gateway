package com.checkout.payment.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.exception.AcquirerException;
import com.checkout.payment.gateway.exception.AcquirerException.Reason;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(AcquiringBankClient.class)
@TestPropertySource(properties = {
    "bank.base-url=http://bank.test",
    "bank.connect-timeout=2s",
    "bank.read-timeout=5s"
})
class AcquiringBankClientTest {

  @Autowired
  private AcquiringBankClient acquiringBankClient;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void sendsExactBankRequestAndReadsAuthorizedResponse() {
    server.expect(requestTo("/payments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("""
            {
              "card_number": "4111111111111111",
              "expiry_date": "07/2030",
              "currency": "GBP",
              "amount": 100,
              "cvv": "012"
            }
            """))
        .andRespond(withSuccess("""
            {"authorized":true,"authorization_code":"bank-code"}
            """, MediaType.APPLICATION_JSON));

    BankPaymentResponse response = acquiringBankClient.authorize(validRequest());

    assertThat(response.getAuthorized()).isTrue();
    assertThat(response.getAuthorizationCode()).isEqualTo("bank-code");
    server.verify();
  }

  @Test
  void mapsBank503ToUnavailableWithoutRetry() {
    server.expect(requestTo("/payments"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    AcquirerException exception = catchThrowableOfType(
        () -> acquiringBankClient.authorize(validRequest()), AcquirerException.class);

    assertThat(exception.getReason()).isEqualTo(Reason.UNAVAILABLE);
    server.verify();
  }

  @Test
  void mapsSocketTimeoutToTimeoutWithoutRetry() {
    server.expect(requestTo("/payments"))
        .andRespond(request -> {
          throw new SocketTimeoutException("simulated timeout");
        });

    AcquirerException exception = catchThrowableOfType(
        () -> acquiringBankClient.authorize(validRequest()), AcquirerException.class);

    assertThat(exception.getReason()).isEqualTo(Reason.TIMEOUT);
    server.verify();
  }

  @Test
  void mapsConnectionFailureToUnavailableWithoutRetry() {
    server.expect(requestTo("/payments"))
        .andRespond(request -> {
          throw new ConnectException("simulated connection failure");
        });

    AcquirerException exception = catchThrowableOfType(
        () -> acquiringBankClient.authorize(validRequest()), AcquirerException.class);

    assertThat(exception.getReason()).isEqualTo(Reason.UNAVAILABLE);
    server.verify();
  }

  @Test
  void rejectsMalformedBankResponseWithoutRetry() {
    server.expect(requestTo("/payments"))
        .andRespond(withSuccess("""
            {"authorization_code":"bank-code"}
            """, MediaType.APPLICATION_JSON));

    AcquirerException exception = catchThrowableOfType(
        () -> acquiringBankClient.authorize(validRequest()), AcquirerException.class);

    assertThat(exception.getReason()).isEqualTo(Reason.INVALID_RESPONSE);
    server.verify();
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(7);
    request.setExpiryYear(2030);
    request.setCurrency(SupportedCurrency.GBP);
    request.setAmount(100);
    request.setCvv("012");
    return request;
  }
}
