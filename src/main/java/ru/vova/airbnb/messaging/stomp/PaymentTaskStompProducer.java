package ru.vova.airbnb.messaging.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.vova.airbnb.messaging.dto.PaymentTaskMessage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTaskStompProducer {

    private static final String STOMP_TERMINATOR = "\u0000";

    private final ObjectMapper objectMapper;

    @Value("${app.messaging.rabbit.host}")
    private String host;

    @Value("${app.messaging.rabbit.stomp-port}")
    private int stompPort;

    @Value("${app.messaging.rabbit.username}")
    private String username;

    @Value("${app.messaging.rabbit.password}")
    private String password;

    @Value("${app.messaging.rabbit.virtual-host:/}")
    private String virtualHost;

    @Value("${app.messaging.payment.queue}")
    private String queueName;

    public void send(PaymentTaskMessage taskMessage) {
        String payload = toJson(taskMessage);
        String destination = "/queue/" + queueName;
        String receiptId = "payment-send-" + UUID.randomUUID();

        try (Socket socket = new Socket(host, stompPort);
             BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
             BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream())) {

            writeFrame(outputStream, "CONNECT\n"
                    + "accept-version:1.2\n"
                    + "host:" + virtualHost + "\n"
                    + "login:" + username + "\n"
                    + "passcode:" + password + "\n\n");

            String connectResponse = normalizeFrame(readFrame(inputStream));
            if (!connectResponse.startsWith("CONNECTED")) {
                throw new IllegalStateException("STOMP broker did not acknowledge CONNECT. Response=" + connectResponse);
            }

            writeFrame(outputStream, "SEND\n"
                    + "destination:" + destination + "\n"
                    + "content-type:text/plain\n"
                    + "persistent:true\n"
                    + "receipt:" + receiptId + "\n"
                    + "JMSType:TextMessage\n"
                    + "amqp-content-type:text/plain\n"
                    + "amqp-type:longstr\n\n"
                    + payload);

            String sendReceiptResponse = normalizeFrame(readFrame(inputStream));
            if (!sendReceiptResponse.startsWith("RECEIPT")
                    || !sendReceiptResponse.contains("receipt-id:" + receiptId)) {
                throw new IllegalStateException("STOMP broker did not acknowledge SEND receipt. Response="
                        + sendReceiptResponse);
            }

            writeFrame(outputStream, "DISCONNECT\n\n");
            log.info("Sent payment task to RabbitMQ STOMP queue '{}', bookingId={}, requestId={}",
                    queueName,
                    taskMessage.bookingId(),
                    taskMessage.requestId());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send STOMP message to RabbitMQ", ex);
        }
    }

    private String toJson(PaymentTaskMessage taskMessage) {
        try {
            return objectMapper.writeValueAsString(taskMessage);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize payment task message", ex);
        }
    }

    private void writeFrame(BufferedOutputStream outputStream, String frame) throws IOException {
        outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
        outputStream.write(STOMP_TERMINATOR.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private String readFrame(BufferedInputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        int current;
        while ((current = inputStream.read()) != -1) {
            if (current == 0) {
                break;
            }
            builder.append((char) current);
        }
        return builder.toString();
    }

    private String normalizeFrame(String frame) {
        return frame.replace("\r", "").stripLeading();
    }
}
