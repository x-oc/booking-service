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
}