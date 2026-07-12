package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.AcquirerException;
import com.checkout.payment.gateway.exception.AcquirerException.Reason;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class AcquiringBankClient {

  private final RestTemplate restTemplate;

  public AcquiringBankClient(
      RestTemplateBuilder builder,
      @Value("${bank.base-url}") String baseUrl,
      @Value("${bank.connect-timeout}") Duration connectTimeout,
      @Value("${bank.read-timeout}") Duration readTimeout) {
    this.restTemplate = builder
        .rootUri(baseUrl)
        .setConnectTimeout(connectTimeout)
        .setReadTimeout(readTimeout)
        .build();
  }

  public BankPaymentResponse authorize(PostPaymentRequest paymentRequest) {
    BankPaymentRequest bankRequest = new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        String.format("%02d/%04d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()),
        paymentRequest.getCurrency().name(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv());

    try {
      ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
          "/payments", bankRequest, BankPaymentResponse.class);
      if (response.getStatusCode() != HttpStatus.OK) {
        throw new AcquirerException(Reason.INVALID_RESPONSE);
      }
      BankPaymentResponse body = response.getBody();
      if (body == null || body.getAuthorized() == null || body.getAuthorizationCode() == null
          || (body.getAuthorized() && body.getAuthorizationCode().isBlank())) {
        throw new AcquirerException(Reason.INVALID_RESPONSE);
      }
      return body;
    } catch (HttpServerErrorException.ServiceUnavailable exception) {
      throw new AcquirerException(Reason.UNAVAILABLE);
    } catch (ResourceAccessException exception) {
      if (hasCause(exception, SocketTimeoutException.class)) {
        throw new AcquirerException(Reason.TIMEOUT);
      }
      if (hasCause(exception, ConnectException.class)) {
        throw new AcquirerException(Reason.UNAVAILABLE);
      }
      throw new AcquirerException(Reason.UNAVAILABLE);
    } catch (RestClientResponseException exception) {
      throw new AcquirerException(Reason.INVALID_RESPONSE);
    } catch (AcquirerException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new AcquirerException(Reason.INVALID_RESPONSE);
    }
  }

  private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
    Throwable current = throwable;
    while (current != null) {
      if (causeType.isInstance(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
