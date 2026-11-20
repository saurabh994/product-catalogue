# AI usage

How I used AI to plan and build this, how I directed and reviewed it, and where it got things wrong.
The raw, timestamped record written during the build is in **[notes/ai-log.md](notes/ai-log.md)** — this file summarises it.

## Tools and models
| Tool | Used for |
|---|---|
| **Claude (Anthropic), model Claude Opus 5.5**, in the Claude desktop app connected to this repo folder | Analysing the brief, planning, writing backend and Android code, tests and docs; looking up library compatibility on the web |
| **Android Studio** (wizard, Gradle sync, emulator, previews) | Generating the Android project, every build, running the app and tests |
| **Docker Desktop** | Running the backend + Mailpit and the backend tests |

The AI wrote files directly into the repo; **I ran every build, test and emulator check myself**, and made every commit.
The AI's own environment had no Android SDK and no access to Maven/Google Maven, so it could not compile the Android code — Android Studio on my machine was the first real check every time.

## Agent instruction file
**[CLAUDE.md](CLAUDE.md)** — written at kickoff and updated as I made decisions. Hard rules include: the app never calls Fake Store directly; product source and email sender behind interfaces; theme tokens only; tests for every feature with fakes; no secrets; backend validation; and, after my Android review, clean architecture (`ui → domain ← data`), Hilt for DI, and no hard-coded URLs.

## Plan / spec
1. **Evening before (planning only, no code):** analysed the brief with the AI — what's really assessed, the gaps in each acceptance criterion, stack options, and a phased plan with a test + review + commit checkpoint per phase.
2. **[DECISIONS.md](DECISIONS.md)** as the spec (Phase 1, before any code): assumptions per criterion, the `/v1` API contract with request/response shapes, module structure. The AI marked 6 assumptions ⚑ for my review.
3. **Phases:** 0 scaffolding → 1 decisions → 2 backend → 3a Android foundation → 3b screens → 4 end-to-end check → 5 docs.

## Key prompts and moments that shaped the solution

**1. Switching the client to Android/Kotlin (planning).**
The AI first suggested React + Node. I switched the client to Kotlin: *"also iam android developer so i need to build in kotlin"*. That decision drove everything after — I chose the platform I can defend and change live, and kept a deliberately small Node backend that I review by behaviour and design.

**2. Overruling the AI on product images (Phase 1).**
Of the 6 ⚑ assumptions, I accepted five and rejected one: *"all are fine except 4th route through backend"*. The AI had proposed letting the app load images straight from Fake Store's image host. I wanted **zero** app dependency on the provider. The AI then designed `GET /v1/products/:id/image` with a relative `imageUrl` (the backend can't know the emulator's `10.0.2.2` address) and lookup **by product ID only**, so the endpoint can't be abused as an open proxy (SSRF).

**3. Reviewing the Android foundation as the Android expert (Phase 3a).**
The first version had layers, but the repository interface sat in `data`, DI was hand-written and the base URL lived in Gradle. I asked: *"have we added clean architecture if not add that, and also dont hardcode network urls create a network constant file and also … lets use [hilt or koin]"*, then *"hilt or coin which is better"*, then *"in catalogapi retrofit instance … can be created through module right"*. Result: repository interfaces + use cases in `domain`, `NetworkConstants`, base URLs in `gradle.properties`, **Hilt** (compile-time-checked, `hiltViewModel()`), and Retrofit built step-by-step in `NetworkModule`. Before adding Hilt, the AI checked online that Hilt's Gradle plugin only supports AGP 9 from **Dagger 2.59**, and that AGP 9 requires KSP instead of kapt.

**4. Batching the remaining screens because of time (Phase 3b).**
After Home worked end-to-end, I gave Product detail, Category, Search and Enquiry as one prompt with explicit requirements (search only on button/IME; name + email required; show backend field errors; Thank you **only after a confirmed send**; retry on failure; a Compose UI test for form → Thank you). Trade-off accepted: one build/test pass instead of four, so I relied on the compiler and my emulator walkthrough to catch mistakes — and it did (below).

**5. Emulator review (Phase 3b/4).**
I noticed product cards in the same row had different heights (2-line vs 1-line titles). Fixed with `minLines = 2` in the shared `ProductTile`, so every grid lines up.

**6. Taking the AI's push-back on auto-search (polish).**
After using the app I asked for search to run automatically after a few characters. The AI pointed out that the acceptance criterion says *"WHEN I click the search button"*, and that DECISIONS.md already records "not search-as-you-type". I agreed to keep the button, which meets the criterion exactly instead of adding a deviation. In the same round I raised the ~10s first load: the AI traced it to a cold backend cache plus two simultaneous requests each downloading from Fake Store, and fixed it with a start-up warm-up and request de-duplication, both tested.

## Where the AI got things wrong, and how it was caught

| # | What went wrong | How it was caught | Fix |
|---|---|---|---|
| 1 | **Wrong KSP version.** Pinned `2.2.10-2.0.2` because AGP's docs say AGP aligns KSP to it — but that's a *minimum*. That KSP registers generated code via `kotlin.sourceSets`, which AGP 9's built-in Kotlin rejects. | **My build:** *"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin."* | AI looked up the release notes: AGP 9 support arrived in KSP **2.3.1** → bumped to 2.3.12. I rejected the `android.disallowKotlinSourceSets=false` workaround because it hides the problem. |
| 2 | **`stringResource()` called inside a `LazyVerticalGrid` builder** (not a `@Composable` context) in Home and Category. | **My build:** 3 compile errors. The AI had caught this same class of bug itself in the enquiry form, but missed it here. | Strings resolved in the composable body before the grid. |
| 3 | **Architecture too loose on the first Android pass** — repository interface in `data`, manual DI, base URL in Gradle, Retrofit built inside the API file. | **My code review** (moment 3). | Restructured to clean architecture + Hilt + `NetworkConstants`. |
| 4 | **Product images from the provider's host** — would have kept an app dependency on Fake Store. | **My review** of the ⚑ assumptions (moment 2). | Image proxy endpoint by product ID. |
| 5 | **Featured product IDs picked from the AI's memory** of the Fake Store catalogue. | The AI flagged it as a risk itself. | I checked them against the live API in the browser. |
| 6 | **Uneven card heights** in the grid. | **My emulator check.** | `minLines = 2`. |

The AI also caught several of its own mistakes **before** I built, which I checked when reviewing: a vague backend validation message; a `decodeFromString<T>()` call whose import differs across kotlinx.serialization versions; a non-`@Composable` lambda passed to `supportingText`; missing `remember` in the UI test (state would reset on recomposition); and a Hilt qualifier placed in `di/`, which made `data` depend on `di` (moved into `data`). For the backend it **deliberately broke the code** (removed HTML escaping; cached "not found") to prove the tests catch it — both mutations failed the right test.

### Environment issues (not AI mistakes, but they cost time)
- Docker Desktop wasn't installed — installed during Phase 1.
- I first ran the Docker test command from my home folder (mounted an empty `backend/`) — re-ran from the repo root.
- Running on my phone: `adb reverse` failed with "more than one device" while the emulator was open → `adb -d`.
- **Gradle 9.6 download timed out.** I downloaded the zip manually; the AI verified its **SHA-256 against the wrapper's `distributionSha256Sum`** and found the cache folder only had an empty `.part` file, then told me where to put it.
- **Android Studio on my Windows laptop was very slow** — Gradle sync, the first builds (Hilt/KSP code generation) and the emulator took a large share of the time. On a faster machine the same work would have taken noticeably less.

## How I reviewed AI output
- **Android (my area):** read every file before committing; pushed back on structure, DI, configuration and UI details; built and ran on the emulator after each phase; checked loading / error / empty states, including with the backend stopped.
- **Backend (TypeScript isn't my main language):** reviewed by **behaviour** — every endpoint in the browser, enquiries sent and checked in Mailpit, invalid input, tests run in Docker — and by **design**, with the AI walking me through each file mapped to Android equivalents (`ProductSource` ≈ repository interface, `server.ts` ≈ DI module, zod schemas ≈ form validation). I can explain the design and the security choices (no SSRF, header-injection guard, honeypot, rate limit, HTML escaping), not every line of Express.
- **One checkpoint per phase:** test → my call recorded in the log → commit → push.

## Time spent
Approximate, worked out from the timestamps in `notes/ai-log.md`.

| Activity | Approx. time |
|---|---|
| **Evening before:** planning only (brief analysis, stack, phased plan, repo + tooling prep) | ~2h |
| DECISIONS.md / API contract | ~15m |
| Backend build + testing | ~30m |
| Android build (foundation, clean-architecture/Hilt refactor, five screens, icon/splash, phone validation) | ~1h15m |
| **Environment and slow tooling:** Docker install, Gradle download timeout, **very slow Android Studio sync/builds on my Windows laptop**, emulator | ~50m |
| Testing and fixes (emulator + phone walkthroughs, 3 build failures, UI-test fixes, running on my phone) | ~45m |
| Docs (README, DECISIONS, AI_USAGE, PR) | ~15m |
| **Total** | **~5h50m:** ~2h planning the evening before + ~3h50m build day (10:17am → 2:05pm) |

That's over the 2–3 hour time-box. The main reasons were environment and tooling time (Android Studio was very slow on my Windows laptop; on a faster machine this would have been noticeably quicker), and choosing to finish with a native Android client, a tested backend, and instrumented UI tests running on a real device.

## Test results at submission
- Backend: **66 passed** (`npm test`)
- Android unit tests: **60 passed** (`testDebugUnitTest`)
- Android Compose UI tests: **3 passed** on a physical phone, Samsung SM-S721B / Android 17 (`connectedDebugAndroidTest`)
- Acceptance criteria: **all 5 working** on the emulator and on my phone (Home, Category, Search, Product page, Enquiry → Thank you + email in Mailpit)
