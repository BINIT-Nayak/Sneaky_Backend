package com.sneaky.sneaky.dto.user;

import java.util.List;

import com.sneaky.sneaky.dto.wishlist.WishlistItemDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileSummaryDTO {
    private long wishlistCount;
    private long cartCount;
    private List<WishlistItemDTO> recentWishlist;
}
