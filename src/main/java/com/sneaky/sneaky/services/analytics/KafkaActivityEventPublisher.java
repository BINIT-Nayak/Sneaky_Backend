package com.sneaky.sneaky.services.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaActivityEventPublisher implements ActivityEventPublisher {
    private final KafkaTemplate<String, UserActivityEventDTO> kafkaTemplate;
    private final String userActivityTopic;

    public KafkaActivityEventPublisher(
            KafkaTemplate<String, UserActivityEventDTO> kafkaTemplate,
            @Value("${app.kafka.topics.user-activity}") String userActivityTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.userActivityTopic = userActivityTopic;
    }

    @Override
    public void publish(UserActivityEventDTO event) {
        if (event == null || event.getProductId() == null) {
            log.warn("Skipping user activity event because it is missing a product id");
            return;
        }

        try {
            kafkaTemplate.send(userActivityTopic, event.getProductId().toString(), event)
                    .exceptionally(ex -> {
                        log.warn("Failed to publish user activity event {} to Kafka", event.getEventId(), ex);
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("Failed to queue user activity event {} for Kafka publishing", event.getEventId(), ex);
        }
    }
}
