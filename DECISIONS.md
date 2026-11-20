# Decisions and assumptions

Status: **draft from Phase 1** (planning). Items marked **⚑ Confirm** were AI proposals I reviewed explicitly.
Sections 6–8 are completed at the end of the build.

---

## 1. Stack

| Layer | Choice | Why |
|---|---|---|
| Client | **Android, Kotlin, Jetpack Compose, MVVM** | My core skill — I can own, defend and change it live. Compose + ViewModel is current Android standard. |
| Backend | **Node.js + TypeScript, Express** | Allowed option; TypeScript gives typed boundaries. Express is the most widely known Node framework, so the code is easy for any reviewer to read. |
| Validation | **zod** (backend) | One schema = runtime validation + TypeScript types. |
| Email (local) | **Mailpit** via SMTP (nodemailer) | Free, runs in Docker, has a web inbox — reviewer sees the sales email without any account. |
| Infra | **Docker Compose** (api + mailpit) | One command for the backend. Containers map directly to AWS ECS/Fargate later. |

---

## 2. Assumptions per acceptance criterion

Reviewed 10:38am: ⚑ items 1, 2, 3, 5, 6 accepted as proposed; item 4 (images) changed by me — see 2.4.

### 2.1 Home — "I have chosen 5 products to be top rated"
- **"Chosen" means curated by the business, not computed.** Fake Store ratings are a poor proxy for what a merchant wants to promote.
- The curated list lives in a **config file in the backend** (`curation.json`), behind a `CurationSource` interface, so a CMS/admin screen can replace it without touching routes or the app.
- Exactly 5 IDs. If a configured ID no longer exists in the data source, it is **skipped and logged** (home shows 4, not an error). ⚑ Confirm
- Initial 5 = picked by me from the Fake Store data (documented in the config).

### 2.2 Category page — "top rated products for that category AND additional products"
- Categories come **from the data source** (Fake Store has 4), exposed with our own **slug** (`mens-clothing`), not the provider's raw string (`men's clothing`).
- **Top rated for a category** = the curated per-category list in `curation.json` if present; otherwise **fall back to the 2 highest-rated** products in that category. ⚑ Confirm
  - Why 2: Fake Store categories have only 4–6 products; showing more "top rated" than "others" would look wrong.
- **Additional products** = every other product in the category, no duplicates with the top-rated section, ordered by rating (desc).
- Category links appear on the Home screen as chips.

### 2.3 Search — "only products that have text matching the search term"
- Search runs on the **backend** (Fake Store has no search endpoint; keeps provider logic out of the app).
- Runs when the **search button is pressed** (or keyboard "search" action) — not search-as-you-type, matching the criterion.
- Matching: case-insensitive; term is split into words, and **every word must appear** in title, description or category name. ⚑ Confirm
  - Why: "mens jacket" should find "Mens Cotton Jacket". Plain whole-phrase matching would miss it.
- Empty/whitespace term: the app disables the button; the backend rejects it with `400` anyway. Max length 100.
- No results → friendly "No products match 'x'" state (not an error).

### 2.4 Product page — "click a product tile → details"
- Any product tile (home, category, search) opens the detail page: image, title, price, category, rating, description, **Enquire** button.
- Unknown product ID → backend `404`, app shows "Product not found".
- **Product images are served through our backend** (my decision — changed from the AI's proposal to load them from Fake Store's image host). The app never contacts Fake Store at all, and swapping provider or moving images to a CDN changes nothing in the app.
  - `Product.imageUrl` is a **relative path** (`/v1/products/3/image`); the app resolves it against its `BASE_URL`. The backend can't know the address the client uses (`10.0.2.2` on the emulator), so relative avoids hard-coding hosts.
  - The backend looks up the product **by ID** and fetches that product's image — it never accepts a URL from the client, so it can't be abused as an open proxy (SSRF).
  - Only `image/*` responses up to 5 MB are passed through; responses get `Cache-Control: public, max-age=86400`.

### 2.5 Enquiry — "sales inbox receives an email AND customer sees Thank you"
- Form fields: **name** (required), **email** (required), **phone** (optional), **message** (optional). ⚑ Confirm
- The app sends only `productId` + contact details; the **backend looks up the product** and puts its title/price in the email, so the email can't be spoofed with fake product details.
- Sales inbox address comes from config (`SALES_INBOX`), never from the client.
- **"Thank you" is shown only after the backend confirms the email was sent.** If sending fails, the user sees an error with a retry — we don't show a false thank-you. ⚑ Confirm
- No confirmation email to the customer (criterion only asks for the sales inbox). Listed as a next step.
- Security basics: server-side validation, HTML-escaping of user input in the email, rate limit (5/min per IP), hidden honeypot field to silently drop bots, body size limit.

---

## 3. API (backend → Android)

Base path **`/v1`** (versioned so the app and API can evolve separately). JSON only.

### Shared shapes
```jsonc
// Product
{
  "id": "3",                         // string: future providers may not use numbers
  "title": "Mens Cotton Jacket",
  "description": "…",
  "price": { "amount": 5599, "currency": "USD" }, // minor units (cents) — no float rounding errors
  "imageUrl": "/v1/products/3/image",   // relative — resolve against API base URL
  "category": { "slug": "mens-clothing", "name": "Men's clothing" },
  "rating": { "average": 4.7, "count": 500 }
}

// Category
{ "slug": "electronics", "name": "Electronics" }

// Error (every non-2xx response)
{ "error": { "code": "VALIDATION_FAILED", "message": "…", "details": [ { "field": "email", "message": "Invalid email" } ] } }
```

### Endpoints
| Method & path | Purpose | Success response | Errors |
|---|---|---|---|
| `GET /health` | Liveness for Docker/AWS | `200 { "status": "ok" }` | — |
| `GET /v1/products/featured` | Home: 5 curated top-rated | `200 { "products": Product[] }` | `502` data source down |
| `GET /v1/categories` | Category links | `200 { "categories": Category[] }` | `502` |
| `GET /v1/categories/:slug/products` | Category page | `200 { "category": Category, "topRated": Product[], "others": Product[] }` | `404` unknown slug, `502` |
| `GET /v1/products?q=term` | Search | `200 { "query": "term", "products": Product[] }` | `400` empty/too long, `502` |
| `GET /v1/products/:id` | Product page | `200 { "product": Product }` | `404`, `502` |
| `GET /v1/products/:id/image` | Product image (proxied from data source) | `200` image bytes, `Cache-Control` 1 day | `404`, `502` |
| `POST /v1/enquiries` | Enquiry | `201 { "enquiryId": "uuid", "status": "sent" }` | `400` validation, `404` unknown product, `429` rate limited, `502` email failed |

`POST /v1/enquiries` request:
```json
{ "productId": "3", "name": "Asha Rao", "email": "asha@example.com", "phone": "+91 98765 43210", "message": "Do you ship to Pune?", "website": "" }
```
(`website` is the honeypot — must be empty; real users never see it.)

### Data source resilience
- Responses from Fake Store are **cached in memory** (TTL 5 min, configurable). If Fake Store fails and we have a cached copy, we **serve the stale copy** rather than erroring.
- Fake Store's response shape is mapped to our `Product` **inside the adapter only**.

---

## 4. Module structure

### 4.1 Backend (`backend/`)
```
src/
  server.ts                 # starts HTTP server
  app.ts                    # builds the Express app; wires dependencies (composition root)
  config.ts                 # reads + validates env vars (zod) — fails fast on bad config
  domain/
    product.ts              # Product, Category, Price types (our model)
  catalogue/
    ProductSource.ts        # interface: listProducts(), getProduct(id), listCategories()
    fakestore/
      FakeStoreProductSource.ts   # adapter: HTTP calls to Fake Store
      mapFakeStoreProduct.ts      # provider shape → our Product
    CachedProductSource.ts  # decorator: TTL cache + stale-on-error around any ProductSource
    CurationSource.ts       # interface + JSON-file implementation (featured / per-category IDs)
    curation.json
    catalogueService.ts     # featured, category split, search — pure logic, easy to test
    catalogueRoutes.ts      # HTTP layer only: parse → call service → respond
    imageRoutes.ts          # GET /v1/products/:id/image — fetch by product ID, stream image
  enquiries/
    EmailSender.ts          # interface: send(message)
    SmtpEmailSender.ts      # nodemailer → Mailpit (or any SMTP / SES SMTP)
    enquirySchema.ts        # zod schema for POST body
    enquiryEmail.ts         # builds the email (escaped)
    enquiryService.ts       # validate product exists → build → send
    enquiryRoutes.ts
  http/
    errors.ts               # AppError types + error-handler middleware → error JSON shape
    rateLimit.ts
test/
  fakes/                    # FakeProductSource, FakeEmailSender, fixture products
  catalogueService.test.ts
  catalogueRoutes.test.ts
  enquiryRoutes.test.ts
  mapFakeStoreProduct.test.ts
  CachedProductSource.test.ts
Dockerfile
```
Libraries: express, zod, nodemailer, express-rate-limit, helmet; dev: typescript, vitest, supertest, tsx.

Rule: **routes know HTTP, services know business rules, adapters know providers.** Nothing above an adapter knows Fake Store exists.

### 4.2 Android (`android/`)
Single `:app` module, organised by layer then feature (multi-module would be overkill for 5 screens; see growth).
```
com.<package>.catalogue/
  CatalogueApp.kt           # Application; creates AppContainer
  di/AppContainer.kt        # manual DI: OkHttp, Retrofit, repositories
  domain/model/             # Product, Category, Price, EnquiryRequest (app's own models)
  data/
    remote/
      CatalogueApi.kt       # Retrofit interface mirroring section 3
      dto/                  # @Serializable DTOs
      mappers.kt            # DTO → domain
    ProductRepository.kt    # interface used by ViewModels
    EnquiryRepository.kt    # interface
    RemoteProductRepository.kt / RemoteEnquiryRepository.kt
    Result handling: errors mapped to a small AppError (Network, NotFound, Validation, Server)
  ui/
    theme/                  # design tokens: colours, type, spacing, shapes
    common/                 # UiState<T>, ProductTile, LoadingView, ErrorView (retry)
    home/                   # HomeScreen + HomeViewModel (featured + category chips + search bar)
    category/
    search/
    product/
    enquiry/                # form + ThankYou
  navigation/AppNavHost.kt  # routes: home, category/{slug}, search?q=, product/{id}, enquiry/{productId}, thank-you
res/xml/network_security_config.xml  # debug only: cleartext allowed for 10.0.2.2
```
Libraries: Compose (BOM), Navigation Compose, lifecycle-viewmodel-compose, Retrofit + kotlinx-serialization converter, OkHttp, Coil. Tests: JUnit4, kotlinx-coroutines-test, MockWebServer, Compose UI test.
- `BASE_URL` via `BuildConfig` (debug: `http://10.0.2.2:3000/`).
- Manual DI over Hilt: fewer build steps and faster setup for a reviewer; easy to move to Hilt when the app grows.

---

## 5. Out of scope for the PoC (initial list — finalised at the end)
- No database: enquiries are emailed, not stored.
- No pagination (Fake Store has 20 products).
- No admin UI for curation (config file instead).
- No customer confirmation email.
- No offline cache in the app.
- English only, no accessibility audit beyond Compose defaults.

## 6. Requirements dropped or simplified
_(completed in Phase 5)_

## 7. How the design supports growth
_(completed in Phase 5 — headline: provider, email and look-and-feel are each one interface/implementation swap)_

## 8. What I'd do next with another day
_(completed in Phase 5)_
