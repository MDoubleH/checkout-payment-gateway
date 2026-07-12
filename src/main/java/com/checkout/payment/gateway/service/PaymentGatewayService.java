package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.Payment;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.bank.BankPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;

  public PaymentGatewayService(
      PaymentsRepository paymentsRepository, AcquiringBankClient acquiringBankClient) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
  }

  public Payment getPaymentById(UUID id) {
    LOG.debug("event=payment_lookup payment_id={}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public Payment processPayment(PostPaymentRequest paymentRequest) {
    BankPaymentResponse bankResponse = acquiringBankClient.authorize(paymentRequest);
    PaymentStatus status = bankResponse.getAuthorized()
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;
    String cardNumber = paymentRequest.getCardNumber();
    Payment payment = new Payment(
        UUID.randomUUID(),
        status,
        cardNumber.substring(cardNumber.length() - 4),
        paymentRequest.getExpiryMonth(),
        paymentRequest.getExpiryYear(),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount());

    paymentsRepository.add(payment);
    LOG.info("event=payment_processed payment_id={} status={}", payment.id(), payment.status());
    return payment;
  }
}
