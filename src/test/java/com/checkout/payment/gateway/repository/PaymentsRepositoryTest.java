package com.checkout.payment.gateway.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.enums.SupportedCurrency;
import com.checkout.payment.gateway.model.Payment;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentsRepositoryTest {

  @Test
  void storesAndRetrievesImmutablePayment() {
    PaymentsRepository repository = new PaymentsRepository();
    Payment payment = new Payment(
        UUID.randomUUID(),
        PaymentStatus.DECLINED,
        "0002",
        12,
        2030,
        SupportedCurrency.EUR,
        100);

    repository.add(payment);

    assertThat(repository.get(payment.id())).contains(payment);
  }
}
