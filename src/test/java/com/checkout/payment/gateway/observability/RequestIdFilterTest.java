package com.checkout.payment.gateway.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void generatesServerRequestIdAndClearsMdc() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/id");
    request.addHeader(RequestIdFilter.HEADER_NAME, "client-supplied-id");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> observedMdcValue = new AtomicReference<>();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
        observedMdcValue.set(MDC.get(RequestIdFilter.MDC_KEY)));

    String responseRequestId = response.getHeader(RequestIdFilter.HEADER_NAME);
    assertThat(responseRequestId).isEqualTo(observedMdcValue.get());
    assertThat(responseRequestId).isNotEqualTo("client-supplied-id");
    assertThat(UUID.fromString(responseRequestId)).isNotNull();
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void clearsMdcWhenRequestFails() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/payments");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThatThrownBy(() -> filter.doFilter(request, response,
        (ignoredRequest, ignoredResponse) -> {
          throw new ServletException("simulated failure");
        })).isInstanceOf(ServletException.class);

    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }
}
