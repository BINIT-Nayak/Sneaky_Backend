package com.sneaky.sneaky.services.analytics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.dto.analytics.UserEventRequest;
import com.sneaky.sneaky.repository.ProductsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserEventTrackingService {
    private final ProductsRepository productsRepository;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserActivityEventFactory activityEventFactory;

    public void track(UUID userId, UserEventRequest request) {
        if (!productsRepository.existsById(request.productId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        publish(
                request.type(),
                userId,
                request.productId(),
                request.quantity(),
                buildMetadata(request));
    }

    public void publish(
            UserActivityEventType type,
            UUID userId,
            UUID productId,
            Integer quantity,
            Map<String, Object> metadata) {
        activityEventPublisher.publish(
                activityEventFactory.create(type, userId, productId, quantity, metadata));
    }

    private static Map<String, Object> buildMetadata(UserEventRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }

        if (request.source() != null && !request.source().isBlank()) {
            metadata.put("source", request.source());
        }

        if (request.position() != null) {
            metadata.put("position", request.position());
        }

        return metadata;
    }
}
