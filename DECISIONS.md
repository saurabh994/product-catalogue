# Decisions and assumptions

Assumptions I made where the acceptance criteria were open, the trade-offs behind them, what I cut, and how the design grows.
Items marked **⚑** were AI proposals I reviewed explicitly (see the review notes in §2 and in AI_USAGE.md).

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
- **Phone: digits only, at most 10** (my decision, made after testing on my phone). Assumes local numbers (e.g. Indian mobiles) with no country code or separators. The app filters the input as you type and shows a number pad; the backend enforces the same rule. For international customers this would become an E.164 field with a country picker.
- The app sends only `productId` + contact details; the **backend looks up the product** and puts its title/price in the email, so the email can't be spoofed with fake product details.
- Sales inbox address comes from config (`SALES_INBOX`), never from the client.
- **"Thank you" is shown only after the backend confirms the email was sent.** If sending fails, the user sees an error with a retry — we don't show a false thank-you. ⚑ Confirm
- The app validates the form **before** sending (same rules as the backend, in `EnquiryValidator`) for instant feedback; the backend remains the source of truth and its field errors are shown on the same fields.
- After "Thank you", Back does not return to the sent form (the form is removed from the back stack); "Back to products" returns to Home.
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
config/
  curation.json             # business-chosen featured / per-category top-rated IDs (outside src so it can be edited without a rebuild)
src/
  server.ts                 # composition root: picks concrete implementations, starts the server
  app.ts                    # builds the Express app from its dependencies (tests pass fakes)
  config.ts                 # reads + validates env vars (zod) — fails fast on bad config
  domain/
    product.ts              # Product, Category, Price types (our model) + accepted ID pattern
  catalogue/
    ProductSource.ts        # interface: listProducts(), getProduct(id)
    fakestore/
      FakeStoreProductSource.ts   # adapter: HTTP calls to Fake Store (the only file that knows its URLs)
      mapFakeStoreProduct.ts      # validates provider data, maps it to our Product
    CachedProductSource.ts  # decorator: TTL cache + stale-on-error around any ProductSource
    CurationSource.ts       # interface + JSON-file implementation
    ImageFetcher.ts         # interface + HTTP implementation (image/* only, ≤5 MB, timeout)
    catalogueService.ts     # featured, categories, category split, search — pure logic
    productResponse.ts      # internal Product → public API shape (hides source image URL)
    catalogueRoutes.ts      # HTTP layer only: parse → call service → respond (incl. image proxy)
  enquiries/
    EmailSender.ts          # interface: send(message)
    SmtpEmailSender.ts      # nodemailer → Mailpit (or any SMTP, e.g. AWS SES SMTP)
    enquirySchema.ts        # zod schema for the POST body
    enquiryEmail.ts         # builds the email (HTML-escaped)
    enquiryService.ts       # honeypot → product lookup → build → send
    enquiryRoutes.ts
  http/
    errors.ts               # AppError / DataSourceError / EmailDeliveryError + error-handler → one JSON error shape
    rateLimit.ts            # per-IP limit for enquiries
test/
  fakes/                    # FakeProductSource, FakeEmailSender, FakeImageFetcher, fixtures, buildTestApp()
  catalogueService.test.ts  mapFakeStoreProduct.test.ts  FakeStoreProductSource.test.ts
  CachedProductSource.test.ts  catalogueRoutes.test.ts  enquiryRoutes.test.ts
Dockerfile                  # multi-stage, production deps only, runs as non-root, healthcheck
```
Libraries: express 5, zod 4, nodemailer, express-rate-limit, helmet; dev: typescript, vitest, supertest, tsx. Node 22.

Rule: **routes know HTTP, services know business rules, adapters know providers.** Nothing above an adapter knows Fake Store exists.

### 4.2 Android (`android/`) — clean architecture + Hilt
Single `:app` module, **layered by clean architecture** (multi-module per layer/feature is the growth path). Generated with Android Studio's wizard — AGP 9.5, Gradle 9.6, Kotlin 2.2, compileSdk 37, minSdk 24.

Dependency rule: **ui → domain ← data**. `domain` is plain Kotlin (only `javax.inject` annotations) and imports nothing from `data`, `ui` or `di`.
```
com.saurabh.catalogue/
  CatalogueApp.kt           # @HiltAndroidApp; provides Coil's ImageLoader using the injected OkHttp client
  MainActivity.kt           # @AndroidEntryPoint; CatalogueTheme { AppNavHost() }
  domain/                   # business layer — no Android, no Retrofit
    model/Product.kt        # Product, Price (minor units), Category, Rating, CategoryProducts
    common/Outcome.kt       # Outcome<T> (Success/Failure) + AppError sealed type
    repository/ProductRepository.kt   # interface (owned by domain, implemented by data)
    usecase/ProductUseCases.kt        # GetFeaturedProducts, GetCategories, GetCategoryProducts, GetProduct, SearchProducts
  data/                     # implements domain interfaces
    remote/
      NetworkConstants.kt   # base URL (from BuildConfig), endpoint paths, params, timeouts, content type
      CatalogueApi.kt       # Retrofit interface only (endpoints from NetworkConstants)
      ApiCall.kt            # apiCall {}: HTTP/IO/parse errors → AppError, cancellation re-thrown
      ApiBaseUrl.kt         # Hilt qualifier
      dto/CatalogueDtos.kt  # @Serializable wire shapes
    mapper/ProductMapper.kt # DTO → domain; resolves relative image URLs
    repository/ProductRepositoryImpl.kt
  di/                       # Hilt modules — the only place interfaces meet implementations
    NetworkModule.kt        # base URL → Json → OkHttp → Retrofit → CatalogueApi, one @Provides each
    RepositoryModule.kt     # @Binds ProductRepository → ProductRepositoryImpl
  ui/
    theme/                  # tokens: Color.kt (BrandColors), Tokens.kt (spacing, shapes), Type.kt, Theme.kt
    common/                 # UiState, ProductTile, LoadingView / ErrorView / MessageView
    home/ category/ search/ product/ enquiry/   # screens + @HiltViewModel ViewModels that call use cases (Phase 3b)
  navigation/               # Routes.kt (@Serializable type-safe routes), AppNavHost.kt
src/main/res/xml/network_security_config.xml   # HTTPS only
src/debug/res/xml/network_security_config.xml  # debug override: cleartext only for 10.0.2.2 / localhost
```
Libraries: Compose (BOM), Navigation Compose (type-safe), lifecycle ViewModel/Compose, **Hilt 2.59.2 (KSP)** + hilt-navigation-compose, Retrofit 3 + kotlinx-serialization converter, OkHttp 4.12, **Coil 3** (images). Tests: JUnit4, kotlinx-coroutines-test, MockWebServer, Compose UI test.

Decisions:
- **Clean architecture** (my call): the repository interface lives in `domain`, so swapping our API for a cache/DB/other backend is a `data` change only. Use cases are thin today but are where business rules go (e.g. `SearchProductsUseCase` already validates/trims input before any network call).
- **Hilt over manual DI and Koin** (my call; replaces the earlier manual-DI proposal): compile-time-checked graph, Google-recommended, `hiltViewModel()` integrates with Navigation Compose. Koin is simpler but fails at runtime. Hilt **2.59+** is required: it's the first release whose Gradle plugin supports AGP 9. Uses **KSP** (kapt is incompatible with AGP 9's built-in Kotlin); KSP `2.2.10-2.0.2` matches AGP 9's bundled Kotlin 2.2.10.
- **No hard-coded URLs** (my call): base URLs are Gradle properties in `android/gradle.properties` (`catalogue.apiBaseUrl.debug` / `.release`) → `BuildConfig.API_BASE_URL` → `NetworkConstants.BASE_URL`. Endpoint paths and params are `const`s in `NetworkConstants` and used directly in Retrofit annotations. The only literal hosts left are in the debug network-security XML (Android requires them there).
- **Coil 3** for images, sharing the API's OkHttp client.
- OkHttp 4.12 (not 5.x): it's what Retrofit 3 is built on, and MockWebServer's classic API is stable.
- Dynamic (wallpaper) colour off: the brand's look must be consistent and fully controlled by tokens.
- Errors are a sealed `AppError` mapped once in the data layer; screens never see HTTP codes or exceptions.

---

## 5. Requirements dropped or simplified

| What | Simplification | Why it's acceptable for the PoC |
|---|---|---|
| Enquiry storage | **Emailed only, not stored.** If the email provider is down, the user sees an error and retries — nothing is queued. | The criterion is "sales inbox receives an email". A database + outbox is the first thing I'd add (§7). |
| Curating "top rated" | A **JSON config file** (`backend/config/curation.json`) behind a `CurationSource` interface; no admin UI. | The entrepreneur "chose" the products; a file is the simplest honest version, and the interface lets a CMS replace it. |
| Customer confirmation | No email to the customer — only the "Thank you" screen. | Not in the criteria. |
| Search quality | Case-insensitive word matching over title, description and category; no fuzzy matching or relevance ranking (sorted by rating). | Fake Store has 20 products; a search engine would be premature. Lives behind the backend, so it can be swapped. |
| Pagination | None. | 20 products. The `/v1` API can add cursors without breaking clients. |
| Cache and rate limit | **In-memory**, per process. | Correct for one instance; needs a shared store (e.g. Redis) when scaled out. |
| Images | Proxied through the backend by product ID; not resized or cached server-side (clients cache via `Cache-Control` and Coil). | Keeps the app independent of the provider; a CDN is the growth step. |
| Release build | `catalogue.apiBaseUrl.release` is a placeholder (`https://api.example.invalid/`). | Nothing is deployed yet; release builds are HTTPS-only by config. |
| App offline support | None — errors with retry instead. | Not required for a PoC. |
| Android modules | Single `:app` module, layered by package. | Five screens; per-feature/per-layer modules are the growth path. |
| Accessibility / i18n | Headings, content descriptions, TalkBack live region for send errors, string resources — but no full audit; English only. Client-side validation messages are English strings in the domain layer. | Time-box; strings are already externalised for the UI. |
| Auth, CI, deployment | None. | Out of scope for a locally-run PoC; see §7. |

Also left as-is: the wizard's `ExampleUnitTest` / `ExampleInstrumentedTest`; minSdk 24 (wizard default).

## 6. How the design supports growth

The brief says the **product data source, look and feel and email provider will all change**. Each is one swap:

| Will change | Where it's isolated | What changing it means |
|---|---|---|
| **Product data source** (Fake Store → PIM / Shopify / own DB) | Backend `ProductSource` interface; `FakeStoreProductSource` is the only file that knows Fake Store; mapped to our own `Product` model. Caching is a decorator around any source. | Write a new adapter, change one line in `server.ts`. **No change** to routes, the API contract or the app. |
| **Email provider** (Mailpit → SES / SendGrid / Resend) | `EmailSender` interface; `SmtpEmailSender` already works with any SMTP server, including AWS SES SMTP. | Point `SMTP_HOST` at SES, or add one class for an HTTP API provider. |
| **Look and feel** (creative agency) | Android design tokens: `BrandColors`, `Spacing`, shapes, `Typography`; screens never use raw colours/sizes; dynamic colour off so the brand is consistent. | Replace token values (and the font) — every screen updates. |
| **Curation** (file → CMS) | `CurationSource` interface. | New implementation; nothing else changes. |
| **Product images** (proxy → CDN) | App receives `imageUrl` from our API and never contacts the provider. | Backend returns CDN URLs; the app is unchanged. |

Further structure that keeps it growable:
- **Clear boundaries:** Android app ↔ our versioned **`/v1` API** ↔ backend ↔ provider. The app has zero knowledge of Fake Store.
- **Android clean architecture:** `ui → domain ← data`. Repository interfaces live in `domain`; ViewModels call **use cases**; Hilt modules are the only place implementations are chosen. A local cache (Room) or a different API is a `data`-layer change.
- **One error model** on each side (`AppError` JSON on the backend, sealed `AppError` in the app), so new failure modes are handled consistently.
- **AWS-ready:** the backend is a stateless container (non-root, healthcheck) → ECS/Fargate or App Runner; SES for email; ElastiCache for shared cache/rate limiting; CloudFront for images. All config is environment variables.
- **Tests at each boundary** with fakes (backend services/routes, Android repository via MockWebServer, use cases, ViewModels, Compose UI) give confidence to change internals.

## 7. What I'd do next with another day
1. **Persist enquiries** (Postgres/DynamoDB) with an outbox + retry so no enquiry is lost if email is down; send the customer a confirmation email.
2. **CI** (GitHub Actions): backend tests + typecheck, Android unit tests + lint, and the Compose UI test on an emulator.
3. **Shared API contract:** generate an OpenAPI spec from the zod schemas and Kotlin DTOs from it, so app and backend can't drift.
4. **Deploy to AWS** (Fargate + SES + HTTPS) and set the real release base URL.
5. **Shared cache and rate limiting** (Redis) for multiple instances; images via CDN.
6. **Admin/CMS for curation** instead of the JSON file.
7. **App polish:** offline cache (Room) and pull-to-refresh, screenshot tests, an accessibility audit, localisation (move validation messages to resources).
8. **Hilt-based UI tests** for full-navigation end-to-end flows against the dockerised backend.
