package ru.vova.airbnb.messaging.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.vova.airbnb.messaging.dto.PaymentTaskMessage;
import ru.vova.airbnb.service.BookingService;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.consumer.enabled", havingValue = "true")
@Slf4j
public class PaymentTaskJmsConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Value("${app.messaging.rabbit.host}")
    private String host;

    @Value("${app.messaging.rabbit.amqp-port}")
    private int amqpPort;

    @Value("${app.messaging.rabbit.username}")
    private String username;

    @Value("${app.messaging.rabbit.password}")
    private String password;

    @Value("${app.messaging.rabbit.virtual-host:/}")
    private String virtualHost;

    @Value("${app.messaging.payment.queue}")
    private String queueName;

    @Value("${app.node.id}")
    private String nodeId;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;
    private ExecutorService consumerExecutor;
    private volatile boolean running;

    @jakarta.annotation.PostConstruct
    public void start() {
        try {
            ensureDurableQueueExists();

            RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
            connectionFactory.setHost(host);
            connectionFactory.setPort(amqpPort);
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
            connectionFactory.setVirtualHost(virtualHost);

            connection = connectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            RMQDestination queue = new RMQDestination();
            queue.setQueue(true);
            queue.setDestinationName(queueName);
            queue.setAmqpQueueName(queueName);
            queue.setAmqpRoutingKey(queueName);
            queue.setAmqp(true);
            consumer = session.createConsumer(queue);
            connection.start();
            running = true;
            consumerExecutor = Executors.newSingleThreadExecutor();
            consumerExecutor.submit(this::pollLoop);

            log.info("JMS payment consumer started on node '{}', queue='{}'", nodeId, queueName);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start JMS payment consumer", ex);
        }
    }

    private void ensureDurableQueueExists() throws Exception {
        ConnectionFactory rabbitConnectionFactory = new ConnectionFactory();
        rabbitConnectionFactory.setHost(host);
        rabbitConnectionFactory.setPort(amqpPort);
        rabbitConnectionFactory.setUsername(username);
        rabbitConnectionFactory.setPassword(password);
        rabbitConnectionFactory.setVirtualHost(virtualHost);

        try (com.rabbitmq.client.Connection rabbitConnection = rabbitConnectionFactory.newConnection(
                "queue-declare-" + nodeId);
             Channel channel = rabbitConnection.createChannel()) {
            channel.queueDeclare(queueName, true, false, false, null);
        }
    }

    private void pollLoop() {
        while (running) {
            try {
                Message message = consumer.receive(1000);
                if (message == null) {
                    continue;
                }

                if (!(message instanceof TextMessage textMessage)) {
                    log.warn("Unsupported JMS message type: {}", message.getClass().getName());
                    session.commit();
                    continue;
                }

                String payload = textMessage.getText();
                PaymentTaskMessage taskMessage = objectMapper.readValue(payload, PaymentTaskMessage.class);
                bookingService.processPaymentTask(taskMessage, nodeId);
                session.commit();
            } catch (Exception ex) {
                if (!running) {
                    break;
                }
                log.error("Failed to process payment task on node '{}': {}", nodeId, ex.getMessage(), ex);
                rollbackQuietly();
            }
        }
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        running = false;
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
            try {
                consumerExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        closeQuietly(consumer);
        closeQuietly(session);
        closeQuietly(connection);
    }

    private void rollbackQuietly() {
        if (session == null) {
            return;
        }
        try {
            session.rollback();
        } catch (JMSException ex) {
            log.error("Could not rollback JMS session", ex);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ex) {
            log.warn("Could not close JMS resource cleanly: {}", ex.getMessage());
        }
    }
}
