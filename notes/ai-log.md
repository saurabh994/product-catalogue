# AI log

Running record of how AI (Claude, Opus 5.5, via the Claude app connected to this repo) was directed.
Each entry: prompt, what the AI produced, and **my call** (what I accepted, changed or rejected).

---

## Pre-build planning session — Thu 19 Nov, ~7:00–9:00pm IST (evening before)

Not part of the build, but it shaped it, so it's recorded here and counted in time spent.

- **Prompt:** shared the challenge PDF — "analyse this and tell me your thoughts and how fast can you build this".
- **AI output:** identified what's actually assessed (judgement, swappable boundaries, ownership), listed the gaps in the acceptance criteria (who "chooses" top-rated, thin categories in Fake Store, no search endpoint, email without paid services → Mailpit, Fake Store reliability → cache + own model). Proposed React + Node.
- **AI output:** estimated ~1–1.5h for AI to build alone, but recommended staged building with my review so the AI_USAGE notes are genuine and I can make a live change in the interview.
- **My call:** **switched the client to Android/Kotlin** — I'm an Android developer, I can defend and change that code live. Kept Node + TypeScript backend.
- **My call:** one repo with `android/` and `backend/` (one PR, one README, one `docker compose up`).
- **My call:** build in phases with a test + commit after each; Android screen by screen, foundation first.
- **My call:** create the Android project with **Android Studio's wizard** rather than having the AI write Gradle files from memory — AI-guessed build tool versions are a likely source of mismatch.

---

## Phase 0 — Kickoff — Fri 20 Nov, start 10:17am IST

- **Prompt:** "Starting now at 10:17AM. I've created the branch feature/catalogue-poc. Create a .gitignore for Android + Node, notes/ai-log.md, and a CLAUDE.md with the project rules: Kotlin/Compose client, Node TS backend, client never calls Fake Store directly, product source and email sender behind interfaces, tests required for every feature. Also set up docker-compose.yml with the backend and Mailpit, plus .env.example, and give me the commands to verify Docker is working."
- **AI output:**
  - `.gitignore` covering Android, Node, secrets (`.env` ignored, `.env.example` kept).
  - `CLAUDE.md` with 8 hard rules (no direct Fake Store calls, `ProductSource` + `EmailSender` interfaces, theme tokens, tests with fakes, no secrets, backend validation, no paid services) and conventions.
  - `docker-compose.yml`: `api` (built from `backend/`, not created yet) + `mailpit` (UI 8025, SMTP 1025).
  - `.env.example` documenting every config value.
- **AI decision to review:** compose uses `${VAR:-default}` defaults so the reviewer does **not** have to copy `.env` first — keeps setup to one command. `.env` is only for overrides.
- **Limitation noted:** the `api` service can't start until Phase 2 creates `backend/`, so Phase 0 verification runs Mailpit only.
- **Setup issue (10:20am):** `docker` not recognised in my terminal — Docker Desktop wasn't installed. Committed Phase 0 anyway (files don't depend on it); installing Docker in parallel with Phase 1 and running the Mailpit check after.
- **My call:** Accepted. Agree with compose defaults so a reviewer needs no `.env` copy step. Docker wasn't installed on my machine; installed it during Phase 1.

---

## Phase 1 — Plan — Fri 20 Nov, 10:31am IST

- **Prompt:** "Draft DECISIONS.md: the assumptions for each acceptance criterion's gaps, the API endpoints with request/response shapes, and the module structure for both the backend and the Android app. No code yet."
- **AI output:** `DECISIONS.md` draft — stack table, assumptions for all 5 criteria, versioned `/v1` API with shapes and error format, backend + Android module structure.
- **Key AI proposals (marked ⚑ in DECISIONS.md) for me to confirm or change:**
  1. Missing curated IDs are skipped, not an error.
  2. Category "top rated" = curated per-category list, fallback to 2 highest-rated.
  3. Search: all words must match (not whole phrase).
  4. Product image URLs still point at Fake Store's image host.
  5. Enquiry fields: name + email required, phone + message optional.
  6. "Thank you" only after the email actually sends; otherwise error + retry.
- **Other AI choices:** Express over Fastify (readability); price in cents, IDs as strings; stale-on-error cache; manual DI over Hilt; single Android module.
- **My call:** Accepted proposals 1, 2, 3, 5 and 6 as written. Changed 4: images go through our backend so the app has zero dependency on Fake Store. Kept the Android choices (manual DI, single module, Retrofit + kotlinx.serialization).
- **Review (10:38am):** accepted proposals 1, 2, 3, 5, 6. **Rejected 4** — I want product images routed through our backend too, so the app has zero dependency on Fake Store (not even its image host) and a provider/CDN change never touches the app.
- **AI follow-up on 4:** image endpoint `GET /v1/products/:id/image`; `imageUrl` returned as a relative path (backend can't know the emulator's `10.0.2.2` address); backend fetches by product ID only — never a client-supplied URL — to avoid an open proxy/SSRF; only `image/*` ≤5 MB, cached 1 day.

---

## Phase 2 — Backend — Fri 20 Nov, 10:44am IST

- **Prompt:** "Build the backend in backend/: ProductSource interface + Fake Store adapter with caching, curated top-rated config, search, image proxy, enquiry endpoint with zod validation, rate limiting and an SMTP EmailSender for Mailpit. Add a Dockerfile and wire it into docker-compose. Write tests using a fake product source. Then give me the commands to test it and walk me through the design using Android comparisons."
- **AI output:** Express 5 + TypeScript backend (~20 source files) matching DECISIONS.md §3–4; 61 tests across 6 files using fakes (no network); multi-stage Dockerfile.
- **How the AI checked its own work:**
  - Typecheck + all 61 tests passed on the first run — so it **deliberately broke the code** (removed HTML escaping; made the cache store "not found") to prove the tests catch it. Both mutations failed the right test.
  - Sent a real email through `SmtpEmailSender` to a local SMTP sink: correct To / Reply-To / Subject, HTML escaped.
  - No Docker daemon in the AI's workspace, so it replayed the Dockerfile steps by hand (clean `npm ci`, build, prod-only install, start) — server healthy. **Real `docker compose up` still to be verified on my machine.**
- **AI caught and fixed:** a missing `productId` gave zod's generic "expected string, received undefined" → changed to "Product is required".
- **Design points the AI added beyond the prompt (for me to understand and defend):**
  - "Not found" is **not cached** — otherwise requests for random IDs could grow the cache without limit.
  - Name field rejects line breaks — blocks **email header injection** via the Reply-To name.
  - Honeypot returns 201 but sends nothing, so bots learn nothing.
  - Request body limit 10 KB; malformed JSON → 400, too large → 413, unknown errors → 500 without leaking internals.
  - Provider data is **validated** with zod in the adapter — Fake Store is treated as untrusted input.
  - Curation file lives in `backend/config/` (outside `src/`) so it can be edited without a rebuild.
- **Risk flagged by AI:** the 5 featured IDs in `curation.json` (3, 5, 11, 18, 1) were chosen from the AI's memory of Fake Store's catalogue — verify against the live API.
- **Setup issue (11:16am):** my first test run failed — I ran the Docker test command from my home folder, so it mounted an empty `backend/`. Re-ran from the repo root.
- **My call:** Reviewed by behaviour and a design walkthrough rather than line by line, since TypeScript isn't my main language. Ran `docker compose up`, checked all endpoints in the browser, sent an enquiry and saw it in Mailpit, ran the tests in Docker: 61 passed. Featured products from `curation.json` look sensible. Agree with not caching "not found" and blocking line breaks in the name.

---

## Phase 3a — Android foundation — Fri 20 Nov, 11:23am–12:15pm IST

### Setup before the prompt
- Created the project with Android Studio's wizard (Empty Activity, Kotlin DSL, package `com.saurabh.catalogue`). It generated AGP 9.5.1, Gradle 9.6.0, Kotlin 2.2.10, compileSdk 37, **minSdk 24** (wizard default — kept).
- **First sync failed:** `Could not install Gradle distribution … SocketTimeoutException: Connect timed out`. Raising the wrapper `networkTimeout` (10s → 60s) didn't help. I downloaded the zip manually in the browser; the AI checked its **SHA-256 against `distributionSha256Sum` in `gradle-wrapper.properties` (exact match)**, found the wrapper cache only had an empty `.part` file, worked out the cache folder name (hash of the distribution URL) and gave me the copy command. Sync then worked.
- Committed the untouched wizard output as its own commit, so the AI's changes are a separate, reviewable diff.

### Prompt
"I've created the Android project with the wizard in android/. Add the foundation: dependencies, BuildConfig base URL for 10.0.2.2, debug-only cleartext network config, Product model, Retrofit API, ProductRepository interface + implementation, a shared UiState sealed class, theme with design tokens, navigation shell, and a ProductTile component. Add a MockWebServer test for the API layer."

### AI output
- Version catalog + app Gradle: Retrofit 3 + kotlinx.serialization converter, OkHttp 4.12 + logging (debug only), Coil 3, Navigation Compose (type-safe routes), lifecycle ViewModel/Compose; `buildConfig = true`.
- `API_BASE_URL`: debug `http://10.0.2.2:3000/`, overridable with the `catalogueApiBaseUrl` Gradle property (physical device) without code changes; release placeholder `https://api.example.invalid/`.
- Network security: main config = HTTPS only; **`src/debug/` overrides it** to allow cleartext only for `10.0.2.2` and `localhost`. Release can't use HTTP.
- Layers: `domain/` (Product, Price in minor units, `Outcome`, `AppError`) → `data/remote/` (DTOs, `CatalogueApi`, `apiCall` error mapping, mappers) → `data/ProductRepository` (interface + Remote impl) → `di/AppContainer` (manual DI) → `ui/`.
- `AppError` sealed type: Network / NotFound / RateLimited / Validation(field errors) / Server / Unexpected — mapped once in `apiCall`, cancellation re-thrown.
- Relative image URLs from the backend are resolved to absolute in the mapper; Coil shares the API's OkHttp client.
- Theme: brand colour, spacing, shape and type tokens; **dynamic colour turned off** (brand consistency). `CatalogueTheme.spacing` / `.extendedColors` accessors.
- `UiState` (Loading/Success/Error) + `LoadingView`, `ErrorView` (retry), `MessageView`; user-facing error strings in `strings.xml`.
- Navigation shell with all 6 routes and placeholder screens; `ProductTile` with preview.
- Tests (JVM): `RemoteProductRepositoryTest` — 13 MockWebServer tests (endpoint paths, query encoding, image URL resolution, unknown fields ignored, 400/404/429/502/503 mapping, dropped connection → Network, bad JSON → Unexpected, enquiry POST body omits nulls); `PriceTest`; `UiStateTest`.

### AI limitations / decisions to review
- The AI's workspace **could not reach Maven or Google Maven**, so it couldn't check the latest library versions — it pinned versions it knows exist and are compatible with Kotlin 2.2.10. Android Studio may suggest upgrades.
- It **could not compile** the Android code (no Android SDK). **First real build is mine.**
- Chose **OkHttp 4.12 + classic MockWebServer** over OkHttp 5 / `mockwebserver3`, because that's what Retrofit 3 is built on and its testing API is stable; it couldn't verify the newer API offline.
- Used a "★" text glyph instead of `Icons.Filled.Star`, because recent Material3 no longer pulls in the icons artifact.
- Self-review caught a `decodeFromString<T>()` call whose import differs across kotlinx.serialization versions → switched to the explicit-serializer overload.
- Left the wizard's `ExampleUnitTest` / `ExampleInstrumentedTest` in place.

- **My review (12:09pm) — three changes I asked for:**
  1. **Clean architecture.** The AI's first version had layers but the repository interface sat in `data` and ViewModels would have called it directly. I asked for proper clean architecture.
  2. **No hard-coded network URLs** — I wanted a network constants file.
  3. **Use a DI library** (Hilt or Koin) instead of the hand-written `AppContainer`.
- **Prompt:** "2 things have we added clean architecture if not add that, and also dont hardcode network urls create a netwokr constant file and also have we created depndnecy like coil or hilt lets use one of that" → follow-up: "hilt or coin which is better do according to that".
- **AI response:**
  - Clarified that Coil (images, already used) and Hilt (DI) aren't alternatives; compared **Hilt vs Koin** and recommended Hilt (compile-time safety, Google-recommended, `hiltViewModel()`).
  - **Checked compatibility on the web before adding it:** Hilt's Gradle plugin only supports AGP 9 from **Dagger 2.59**; kapt doesn't work with AGP 9's built-in Kotlin, so **KSP** is required, and AGP 9 aligns KSP to `2.2.10-2.0.2`. Pinned Hilt 2.59.2 + KSP 2.2.10-2.0.2.
  - Restructured: `domain/{model,common,repository,usecase}`, `data/{remote,mapper,repository}`, `di/{NetworkModule,RepositoryModule}`. Removed `AppContainer`. Repository interface moved to `domain`.
  - Added 5 use cases; `SearchProductsUseCase` now trims and rejects blank/over-long terms **before** any network call (moved out of the repository).
  - `NetworkConstants`: endpoint paths, params, timeouts, content type; Retrofit annotations use the constants. Base URLs moved from `build.gradle.kts` into `gradle.properties`.
  - Self-review: the `@ApiBaseUrl` qualifier was first put in `di/`, making `data` import `di` → moved it into `data/remote` so dependencies point the right way. Verified with grep that `domain/` imports nothing from `data`/`ui`/`di`.
  - Tests: repository test renamed to `ProductRepositoryImplTest`; added `FakeProductRepository`, `SearchProductsUseCaseTest` (5), `ProductUseCasesTest` (4).
- **My call:** Reviewed the foundation as the Android developer and asked for three changes: proper clean architecture (repository interface in `domain`, ViewModels go through use cases), a `NetworkConstants` file with no hard-coded URLs, and a real DI library. Chose **Hilt over Koin** after the comparison — compile-time safety and `hiltViewModel()` matter more to me than Koin's simpler setup. Kept the AI's other choices: OkHttp 4.12 + classic MockWebServer, dynamic colour off, sealed `AppError`, minSdk 24 from the wizard.
- **My review (12:16pm):** "also in catalogapi retrofit instnace that can be created though module irght" — the Retrofit instance was still built by a helper in `CatalogueApi.kt`, mixing the API definition with its setup.
  - **AI fix:** `CatalogueApi.kt` now only declares endpoints. `NetworkModule` provides each step separately — `@ApiBaseUrl HttpUrl` → `Json` → `OkHttpClient` → **`Retrofit`** → `CatalogueApi` — so any piece can be swapped on its own. The repository test now builds its API with `NetworkModule.provideRetrofit(...)` / `provideCatalogueApi(...)`, i.e. the exact production configuration.
- **Build failure 1 (12:19pm) — AI got the KSP version wrong:** sync failed with *"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin. Kotlin source set 'debug' contains: …/generated/ksp/debug/…"*.
  - Cause: the AI had pinned KSP `2.2.10-2.0.2` because AGP's docs say AGP aligns KSP to that version — but that only means it's the *minimum*. That KSP line still registers generated code through `kotlin.sourceSets`, which AGP 9's built-in Kotlin forbids. AGP 9 support only arrived in **KSP 2.3.1**, and 2.3.10 fixed R-class resolution under built-in Kotlin.
  - Fix: bumped to **KSP 2.3.12** (KSP 2.3.x is no longer tied to the Kotlin version). Rejected the alternative `android.disallowKotlinSourceSets=false` workaround — it hides the problem instead of fixing it.
- **Build + tests:** after the KSP fix the project builds and runs on the emulator. Unit test result: _(to add)_

---

## Phase 3b.1 — Home screen — Fri 20 Nov, 12:33pm IST

- **Context:** first build of the foundation was still running (slow first run: Hilt/KSP/Retrofit/Coil downloads). I committed locally as a checkpoint and held the push until the build passes; the AI wrote Home in parallel, and I don't start a new build until the current one finishes.
- **Prompt:** "Build the Home screen showing the 5 top-rated products, with its ViewModel handling loading, success and error. Add ViewModel tests with a fake repository."
- **AI output:**
  - `HomeViewModel` (`@HiltViewModel`): depends on `GetFeaturedProductsUseCase` (not the repository), exposes `StateFlow<UiState<List<Product>>>`, loads in `init`, `retry()` cancels any in-flight load before starting a new one.
  - `HomeScreen` (stateful, `hiltViewModel()` + `collectAsStateWithLifecycle`) → `HomeContent` (stateless, previewable). Adaptive `LazyVerticalGrid` (2 columns on a phone, more on tablets), "Top rated" heading with accessibility `heading()` semantics, Loading / Error-with-retry / empty states, previews for success and error.
  - Navigation: Home tile tap → `ProductRoute(id)`; screens get callbacks, not the `NavController`.
  - Tests: `MainDispatcherRule` (StandardTestDispatcher so Loading is observable) + `HomeViewModelTest` (5): starts Loading, success shows 5 products in order, error shown, retry → Loading → Success, empty list is a success not an error.
- **Scope kept tight:** category links and the search bar on Home are added with the Category and Search screens, not here.
- **Emulator check (12:46pm):** Home loads the 5 featured products from the local backend.
- **My call:** _(fill in)_

---

## Phase 3b.2 — Product detail, Category, Search, Enquiry (batched) — Fri 20 Nov, 12:47pm IST

- **My decision:** time was short, so I gave the remaining four screens as **one prompt** instead of one at a time. Trade-off accepted: one build/test/emulator pass at the end instead of per screen; the AI reviewed each screen as it wrote it, and I commit per screen afterwards.
- **Prompt:** "Time is short, so build the remaining screens in one go, in this order, each with ViewModel tests using the fake repository: 1. Product detail — opened by tapping a product tile: image, title, price, category, rating, description, and an Enquire button. 2. Category — category links (chips) on Home; Category screen shows a 'Top rated' section and an 'Other products' section. 3. Search — search bar on Home; search runs only when the search button (or keyboard search action) is pressed; results screen with empty-term and no-results states. 4. Enquiry — form from the detail screen: name and email required, phone and message optional, field validation (show backend field errors too), submit to the backend, Thank-you screen only after a successful send, error with retry if sending fails. Add a Compose UI test for form → Thank you. Then tell me how to test all five acceptance criteria and give me the commit messages."
- **AI output:**
  - **Domain/data for enquiries:** `EnquiryRequest`, `EnquiryRepository` (+ `EnquiryRepositoryImpl`, bound in `RepositoryModule`), `SubmitEnquiryUseCase` (trims, blank optionals → null, validates before sending), `EnquiryValidator` mirroring the backend's zod rules (incl. no line breaks in name).
  - **Shared UI:** `AppTopBar` (back button), `SearchField` (runs only on button / IME search; disabled when blank), `ProductGrid` + `productSection` helpers reused by Home/Category/Search; back/search icons as **vector drawables** (no icons library).
  - **Home:** now also loads categories in parallel (`HomeUiState`); search bar + category chips stay usable while featured products load or fail; category failure just hides the chips.
  - **Product detail:** image, category, title, price, rating, description; Enquire button pinned at the bottom.
  - **Category:** title shown immediately from the route; "Top rated" + "Other products" sections (with an empty state).
  - **Search:** results screen with its own search field to refine; blank → "Enter a search term"; no matches → "No products match …"; newest search cancels an older one.
  - **Enquiry:** field errors from local validation **and** backend 400s (non-form fields like `productId` shown as a send error); send error keeps the input and the button becomes "Try again"; double-tap guarded; error announced to TalkBack (live region). **Thank you replaces the form in the back stack** (`popUpTo<EnquiryRoute> { inclusive = true }`) so Back can't return to a sent form; "Back to products" pops to Home.
  - Route args are read from `SavedStateHandle` by key (`RouteArgs`) rather than `toRoute()`, so ViewModels are testable on the JVM without Robolectric.
- **Tests added:** `HomeViewModelTest` (updated, 5), `ProductViewModelTest` (3), `CategoryViewModelTest` (4), `SearchViewModelTest` (5), `EnquiryViewModelTest` (9), `EnquiryValidatorTest` (5), `FakeEnquiryRepository`; **Compose UI test** `EnquiryFlowTest` (3, instrumented): form → Thank you, missing fields → errors and no send, failed send → error and no Thank you.
- **AI self-review caught before I built:**
  - `supportingText = error?.let { { Text(...) } }` would not compile — the lambda isn't inferred as `@Composable` → rewritten as `if/else`.
  - In the UI test, the "sent" state wasn't wrapped in `remember`, so a recomposition would have reset it → fixed.
- **My call:** _(fill in after build + emulator walkthrough)_

