#!/usr/bin/env node

const API_BASE_URL = requiredEnv("SNEAKY_API_BASE_URL").replace(/\/$/, "");
const ADMIN_EMAIL = requiredEnv("SNEAKY_ADMIN_EMAIL");
const ADMIN_PASSWORD = requiredEnv("SNEAKY_ADMIN_PASSWORD");
const TARGET_PRODUCT_COUNT = Number.parseInt(process.env.SNEAKY_TARGET_PRODUCT_COUNT ?? "80", 10);
const DRY_RUN = process.env.SNEAKY_DRY_RUN === "true";

const BRAND_NAMES = [
  "Nike", "Adidas", "Puma", "Reebok", "New Balance", "Asics", "Converse", "Vans",
  "Under Armour", "Skechers", "Fila", "Saucony", "Brooks", "Hoka", "On", "Mizuno",
  "Salomon", "Jordan", "Li-Ning", "Anta",
];

const MODEL_NAMES = [
  "Air Pulse Runner", "Court Legacy Low", "Street Glide", "Retro Sprint", "Cloud Tempo",
  "Metro Flex", "Daily Lift", "Urban Trail", "Prime Step", "Classic Wave",
  "Velocity Knit", "Summit Runner", "Canvas Deck", "Arc Trainer", "Nova Court",
  "Rush Runner", "Ease Walk", "Shadow Low", "Orbit Lace", "Fresh Foam",
  "City Runner", "Skate Core", "Runner Pro", "Leather Court", "Aero Glide",
  "Terrace Low", "Track Lite", "Studio Step", "Rapid Mesh", "Heritage High",
  "Neo Runner", "Balance Court", "Flex Runner", "Street Runner", "Tonal Low",
  "Motion Max", "Court Prime", "Lite Runner", "Sprint Deck", "Gel Street",
  "Daily Court", "Active Mesh", "Urban Runner", "Cloud Lift", "Skate Low",
  "Retro High", "Training Plus", "Runner Elite", "Court Soft", "Trail City",
  "Tempo Forge", "Marathon Edge", "Hoop Street", "Trail Ridge", "Court Rally",
  "Boardwalk Slip", "Gym Drive", "Rain Shield", "Pebble Runner", "Carbon Pace",
  "Studio Flow", "Heritage Suede", "Grip Trek", "Campus Knit", "Flex Court",
  "Apex Trainer", "Recovery Slide", "City Hiker", "Pro Bounce", "Wave Runner",
];

const CATEGORY_NAMES = [
  "Running", "Lifestyle", "Training", "Skate", "Basketball", "Tennis", "Trail",
  "Walking", "Football", "Outdoor", "Court", "Slip-On", "Premium", "Recovery",
];

const MERCHANTS = [
  ["Amazon", "https://www.amazon.in/s?k=sneakers"],
  ["Myntra", "https://www.myntra.com/sneakers"],
  ["AJIO", "https://www.ajio.com/search/?text=sneakers"],
  ["Nike", "https://www.nike.com/in/w/shoes"],
  ["Puma", "https://in.puma.com/in/en/mens/shoes"],
  ["Adidas", "https://www.adidas.co.in/shoes"],
];

const IMAGE_URLS = [
  "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1549298916-b41d501d3772?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1543508282-6319a3e2621f?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1491553895911-0055eca6402d?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1607522370275-f14206abe5d3?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?auto=format&fit=crop&w=900&q=80",
  "https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&w=900&q=80",
];

const SIZE_SETS = [
  ["UK 5", "UK 6", "UK 7", "UK 8"],
  ["UK 6", "UK 7", "UK 8", "UK 9", "UK 10"],
  ["UK 7", "UK 8", "UK 9", "UK 10", "UK 11"],
  ["UK 4", "UK 5", "UK 6", "UK 7"],
  ["UK 8", "UK 9", "UK 10", "UK 11", "UK 12"],
];

const COLOR_SETS = [
  [{ name: "Black", value: "#17151d" }, { name: "Ivory", value: "#eee4cf" }],
  [{ name: "White", value: "#f8f7f2" }, { name: "Green", value: "#2f6f4e" }],
  [{ name: "Navy", value: "#1c2942" }, { name: "Grey", value: "#8f949b" }],
  [{ name: "Clay", value: "#c27a58" }, { name: "Cream", value: "#f3dfbd" }],
  [{ name: "Red", value: "#bb2f36" }, { name: "Black", value: "#151515" }],
  [{ name: "Blue", value: "#2f5f9f" }, { name: "Silver", value: "#c8c9cb" }],
  [{ name: "Tan", value: "#b88a5a" }, { name: "Brown", value: "#6a4a32" }],
  [{ name: "Lilac", value: "#a99ad6" }, { name: "White", value: "#fafafa" }],
];

const PRICE_BASES = [2499, 3299, 4499, 5499, 6999, 8499, 9999, 11999, 14999, 17999, 21999, 26999];

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exitCode = 1;
});

async function main() {
  if (!Number.isInteger(TARGET_PRODUCT_COUNT) || TARGET_PRODUCT_COUNT < 1) {
    throw new Error("SNEAKY_TARGET_PRODUCT_COUNT must be a positive integer");
  }

  console.log(`Connecting to ${API_BASE_URL}`);
  const accessToken = await login();
  const stats = await request("/api/admin/dashboard/stats", { accessToken });
  const existingProducts = await getExistingProducts(accessToken);
  const existingNames = new Set(existingProducts.map((product) => product.name.toLowerCase()));

  console.log(`Current products: ${stats.totalProducts}. Target products: ${TARGET_PRODUCT_COUNT}.`);
  if (stats.totalProducts >= TARGET_PRODUCT_COUNT) {
    console.log("Nothing to import. Product count already meets the target.");
    return;
  }

  const brands = await ensureBrands(accessToken);
  const productsToCreate = buildCatalog(TARGET_PRODUCT_COUNT * 2, brands)
    .filter((product) => !existingNames.has(product.name.toLowerCase()))
    .slice(0, TARGET_PRODUCT_COUNT - stats.totalProducts);

  console.log(`Products to create through admin API: ${productsToCreate.length}`);
  if (DRY_RUN) {
    productsToCreate.forEach((product) => console.log(`[dry-run] ${product.name}`));
    return;
  }

  for (const [index, product] of productsToCreate.entries()) {
    await request("/api/admin/products", {
      accessToken,
      method: "POST",
      body: product,
    });
    console.log(`Created ${index + 1}/${productsToCreate.length}: ${product.name}`);
  }

  console.log("Admin product import completed.");
}

async function login() {
  const response = await request("/api/auth/login", {
    method: "POST",
    body: {
      email: ADMIN_EMAIL,
      password: ADMIN_PASSWORD,
    },
  });

  if (!response.accessToken) {
    throw new Error("Login response did not include an access token");
  }

  return response.accessToken;
}

async function ensureBrands(accessToken) {
  const brands = await request("/api/brands", { accessToken });
  const brandByName = new Map(brands.map((brand) => [brand.name.toLowerCase(), brand]));

  for (const name of BRAND_NAMES) {
    if (brandByName.has(name.toLowerCase())) {
      continue;
    }

    const created = await request("/api/admin/brands", {
      accessToken,
      method: "POST",
      body: { name },
    });
    brandByName.set(created.name.toLowerCase(), created);
    console.log(`Created brand: ${created.name}`);
  }

  return BRAND_NAMES.map((name) => brandByName.get(name.toLowerCase())).filter(Boolean);
}

async function getExistingProducts(accessToken) {
  const page = await request("/api/admin/products?size=500", { accessToken });
  return Array.isArray(page.content) ? page.content : [];
}

function buildCatalog(count, brands) {
  return Array.from({ length: count }, (_, index) => {
    const brand = brands[index % brands.length];
    const model = MODEL_NAMES[index % MODEL_NAMES.length];
    const dropNumber = Math.floor(index / MODEL_NAMES.length) + 1;
    const category = CATEGORY_NAMES[index % CATEGORY_NAMES.length];
    const [merchantName, merchantUrl] = MERCHANTS[index % MERCHANTS.length];
    const basePrice = PRICE_BASES[index % PRICE_BASES.length];
    const price = basePrice + (index % BRAND_NAMES.length) * 180 + (index % CATEGORY_NAMES.length) * 120 + Math.floor(index / MODEL_NAMES.length) * 650;

    return {
      name: dropNumber === 1 ? model : `${model} Drop ${dropNumber}`,
      description: `A ${category.toLowerCase()} sneaker tuned for comfort, grip, and everyday rotation with materials chosen for its price tier.`,
      price,
      imageUrl: IMAGE_URLS[index % IMAGE_URLS.length],
      category,
      merchantName,
      merchantUrl,
      sizes: SIZE_SETS[index % SIZE_SETS.length],
      colors: COLOR_SETS[index % COLOR_SETS.length],
      stockStatus: index % 7 === 0 ? "Only a few left" : index % 3 === 0 ? "Selling fast" : "In stock",
      brandId: brand.id,
      isActive: true,
    };
  });
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.accessToken ? { Authorization: `Bearer ${options.accessToken}` } : {}),
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${options.method ?? "GET"} ${path} failed with ${response.status}: ${message}`);
  }

  if (response.status === 204) {
    return undefined;
  }

  return response.json();
}

function requiredEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} is required`);
  }
  return value;
}
