package com.sneaky.sneaky.dto.analytics;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductAnalyticsDTO {
    private UUID productId;
    private long views;
    private long cartAdds;
    private long wishlistAdds;
}
