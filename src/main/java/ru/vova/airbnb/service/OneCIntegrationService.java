package ru.vova.airbnb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.vova.airbnb.controller.dto.OneCPaymentRequest;
import ru.vova.airbnb.controller.dto.OneCPaymentResponse;
import ru.vova.airbnb.exception.BookingException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OneCIntegrationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.1c.base-url:http://localhost/airbnb}")
    private String oneCBaseUrl;

    public String testConnection() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(oneCBaseUrl).build();

            String response = webClient.get()
                    .uri("/hs/airbnb/payment")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new BookingException("1C error: " + error)))
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            log.info("1C test connection response: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Failed to connect to 1C", e);
            throw new BookingException("Failed to connect to 1C: " + e.getMessage());
        }
    }

    public OneCPaymentResponse sendPaymentTo1C(OneCPaymentRequest request) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(oneCBaseUrl).build();

            OneCPaymentResponse response = webClient.post()
                    .uri("/hs/airbnb/payment")
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new BookingException("1C error: " + error)))
                    )
                    .bodyToMono(OneCPaymentResponse.class)
                    .block(Duration.ofSeconds(5));

            log.info("Payment sent to 1C successfully: bookingId={}, documentId={}",
                    request.getExternalId(), response != null ? response.getDocumentId() : "null");

            return response;

        } catch (Exception e) {
            log.error("Failed to send payment to 1C", e);
            throw new BookingException("Failed to send payment to 1C: " + e.getMessage());
        }
    }

    public OneCPaymentResponse sendPaymentTo1CWithRetry(OneCPaymentRequest request,
                                                        int maxAttempts,
                                                        Duration delayBetweenAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    log.warn("Retrying 1C payment send, attempt {}/{} for bookingId={}",
                            attempt, maxAttempts, request.getExternalId());
                    sleepQuietly(delayBetweenAttempts);
                }

                return sendPaymentTo1C(request);
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("1C payment send attempt {}/{} failed for bookingId={}: {}",
                        attempt,
                        maxAttempts,
                        request.getExternalId(),
                        ex.getMessage());
            }
        }

        throw new BookingException("Failed to send payment to 1C after " + maxAttempts + " attempts: "
                + (lastError != null ? lastError.getMessage() : "unknown error"));
    }

    private void sleepQuietly(Duration delayBetweenAttempts) {
        if (delayBetweenAttempts == null || delayBetweenAttempts.isZero() || delayBetweenAttempts.isNegative()) {
            return;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(delayBetweenAttempts.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BookingException("Interrupted while waiting before 1C retry");
        }
    }
}