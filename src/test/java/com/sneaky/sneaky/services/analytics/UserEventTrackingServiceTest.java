package com.sneaky.sneaky.services.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sneaky.sneaky.dto.analytics.UserActivityEventDTO;
import com.sneaky.sneaky.dto.analytics.UserActivityEventType;
import com.sneaky.sneaky.dto.analytics.UserEventRequest;
import com.sneaky.sneaky.repository.ProductsRepository;

@ExtendWith(MockitoExtension.class)
class UserEventTrackingServiceTest {
    @Mock
    private ProductsRepository productsRepository;

    @Mock
    private ActivityEventPublisher activityEventPublisher;

    private final UserActivityEventFactory activityEventFactory = new UserActivityEventFactory();

    @Test
    void trackMergesRequestMetadataAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UserEventTrackingService service =
                new UserEventTrackingService(productsRepository, activityEventPublisher, activityEventFactory);

        when(productsRepository.existsById(productId)).thenReturn(true);

        service.track(
                userId,
                new UserEventRequest(
                        productId,
                        UserActivityEventType.WISHLIST,
                        "DISCOVERY_FEED",
                        4,
                        null,
                        Map.of("surface", "HOME")));

        ArgumentCaptor<UserActivityEventDTO> captor = ArgumentCaptor.forClass(UserActivityEventDTO.class);
        verify(activityEventPublisher).publish(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getProductId()).isEqualTo(productId);
        assertThat(captor.getValue().getEventType()).isEqualTo(UserActivityEventType.WISHLIST);
        assertThat(captor.getValue().getMetadata())
                .containsEntry("source", "DISCOVERY_FEED")
                .containsEntry("position", 4)
                .containsEntry("surface", "HOME");
    }
}
