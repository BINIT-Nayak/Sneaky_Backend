# Product Catalog Seeding

`ProductCatalogSeeder` creates a larger product pool for local development and recommendation testing.

By default, it seeds up to `300` active products across multiple brands, categories, colors, sizes, and price bands.

Configure the minimum active product count with:

```bash
APP_SEED_PRODUCTS_MINIMUM_COUNT=300
```

Disable product seeding with:

```bash
APP_SEED_PRODUCTS_ENABLED=false
```

Current seed data includes:

- 20 brands
- 14 product categories
- Dummy merchant partners with links like `https://partners.sneaky.test/amazon`
- Budget, mid-range, and premium price bands
- Multiple size and color sets

Products also support merchant metadata:

- `merchantName`
- `merchantUrl`

If a product has no merchant URL, the backend falls back to `https://www.google.com/`.
