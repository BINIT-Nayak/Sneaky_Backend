# Commerce APIs

## Merchant Checkout

Sneaky does not process payments. Cart items include product merchant fields, and the frontend groups cart items by merchant so one outbound button appears per partner:

- Amazon items share one Amazon button
- Myntra items share one Myntra button
- AJIO, Nike, Puma, Adidas, and other partners follow the same pattern

The merchant buttons open the product partner site in a new tab.

## Wishlist API

Wishlist endpoints require authentication:

```http
GET /api/wishlist
POST /api/wishlist
POST /api/wishlist/{productId}/move-to-cart
DELETE /api/wishlist/{productId}
DELETE /api/wishlist
```

`DELETE /api/wishlist` clears every wishlist item for the current user.

`POST /api/wishlist/{productId}/move-to-cart` adds or increments the product in the cart and removes it from the wishlist in one transaction. The endpoint returns the updated cart item.
