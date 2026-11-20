# Product catalogue — proof of concept

A product catalogue MVP that customers can browse, search and send enquiries from — built as a **foundation to grow**, not a throwaway prototype.

- **Client:** Android app — Kotlin, Jetpack Compose, MVVM + clean architecture, Hilt
- **Backend:** Node.js + TypeScript (Express) — the only thing that talks to the product data source (Fake Store API)
- **Email:** sent over SMTP to **Mailpit**, a local inbox you can open in the browser (no accounts, no paid services)

```
 Android app ──HTTP──▶ Backend API ──▶ ProductSource (Fake Store adapter + cache)
 (Compose,            (/v1/...)    └─▶ EmailSender (SMTP → Mailpit)
  Hilt, Retrofit)                  └─▶ Curation (config/curation.json)
```

Why these choices, and every assumption made where the acceptance criteria were open: **[DECISIONS.md](DECISIONS.md)**.
How AI was used and reviewed: **[AI_USAGE.md](AI_USAGE.md)**.

---

## Prerequisites

| Tool | Notes |
|---|---|
| **Docker Desktop** (with Docker Compose v2) | Runs the backend and Mailpit. Must be running (*Engine running*). |
| **Android Studio** (current stable, AGP 9 support) | Includes the JDK. Install **Android SDK Platform 37** if prompted. |
| **An Android emulator** | Any recent Pixel image (Google APIs, x86_64 / arm64 to match your machine). |

No accounts, API keys or `.env` file are needed — all configuration has safe defaults.

---

## Run it (≈10 minutes, most of it first-time downloads)

### 1. Start the backend and the mail inbox
From the repository root:
```bash
docker compose up --build
```
- API: http://localhost:3000/health → `{"status":"ok"}`
- Mailpit (the "sales inbox"): **http://localhost:8025**

### 2. Run the Android app
1. In Android Studio choose **Open** and select the **`android/`** folder (not the repo root).
2. Wait for Gradle sync (the first sync downloads dependencies and can take several minutes).
3. Start an emulator and press **Run ▶** (`app` configuration).

The debug build calls the backend at `http://10.0.2.2:3000/` — the emulator's address for your machine's `localhost`.

### 3. Try the acceptance criteria
| Criterion | Try this |
|---|---|
| **Home** | App opens on 5 "Top rated" products (chosen in `backend/config/curation.json`). |
| **Category** | Tap a category chip → "Top rated" and "Other products" sections. |
| **Search** | Type `jacket`, tap the 🔍 button → only matching products (`zzz` → no-results message). |
| **Product page** | Tap any product tile → details with an **Enquire** button. |
| **Enquiry** | Enquire → name + email → **Send enquiry** → **Thank you** screen, and the email appears in **Mailpit (http://localhost:8025)**. |

Failure handling worth trying: submit with an invalid email (field errors); or run `docker compose stop api`, then use the app (friendly error + **Try again**; no false "Thank you") and `docker compose start api` to recover.

---

## Running the tests

### Backend (66 tests — no network, no SMTP: uses in-memory fakes)
Without installing Node (from the repo root):
```bash
docker run --rm -v "$PWD/backend:/app" -w /app node:22-alpine sh -c "npm ci && npm test"
```
Windows PowerShell: use `"${PWD}\backend:/app"`.

With Node 22+ installed: `cd backend && npm ci && npm test`

### Android (from `android/`)
```bash
./gradlew testDebugUnitTest          # 60 JVM unit tests: repository (MockWebServer), use cases, ViewModels
./gradlew connectedDebugAndroidTest  # 3 Compose UI tests (enquiry form → Thank you); needs an unlocked emulator or phone
```
Windows: `.\gradlew.bat …`

---

## Configuration

**Backend** — environment variables with defaults in `docker-compose.yml`; documented in [`.env.example`](.env.example). Copy to `.env` only to override.

| Variable | Default | Purpose |
|---|---|---|
| `FAKESTORE_BASE_URL` | `https://fakestoreapi.com` | Product data source |
| `PRODUCT_CACHE_TTL_SECONDS` | `300` | In-memory cache; stale copy served if the source is down |
| `SALES_INBOX` | `sales@example.com` | Where enquiries are emailed |
| `ENQUIRY_FROM` | `no-reply@catalogue.local` | Sender address |
| `ENQUIRY_RATE_LIMIT_PER_MINUTE` | `5` | Per-IP limit on enquiries |

Featured / "top rated" products: edit `backend/config/curation.json` and restart the API.

**Android** — base URLs are Gradle properties in `android/gradle.properties` (`catalogue.apiBaseUrl.debug`, `catalogue.apiBaseUrl.release`), exposed as `BuildConfig.API_BASE_URL`.
### Emulator vs physical phone

| Running on | Base URL | What to do |
|---|---|---|
| **Android emulator** (default) | `http://10.0.2.2:3000/` | Nothing — works out of the box. `10.0.2.2` is the emulator's alias for your computer. |
| **Physical phone** (USB) | `http://localhost:3000/` | Change one property and forward the port (below). `10.0.2.2` does not exist on a real phone (the app would time out after 10 s). |

**Physical phone steps:**
1. On the phone enable *Developer options* → *USB debugging*; connect by USB and accept the prompt.
2. Forward the backend port over USB (re-run after reconnecting; `adb` is in `<Android SDK>/platform-tools`):
   ```bash
   adb -d reverse tcp:3000 tcp:3000      # -d = the USB device (needed if an emulator is also running)
   ```
   Check: open `http://localhost:3000/health` in the phone's browser → `{"status":"ok"}`.
3. Point debug builds at `localhost`. Either:
   - **Per machine (recommended):** add to your user Gradle file `~/.gradle/gradle.properties` (Windows: `C:\Users\<you>\.gradle\gradle.properties` — make sure it isn't saved as `.txt`):
     ```properties
     catalogue.apiBaseUrl.debug=http://localhost:3000/
     ```
   - **Or quickly:** change the same line in `android/gradle.properties` — and **change it back to `http://10.0.2.2:3000/` before committing**.
4. **Sync → Build › Clean Project → Run** on the phone (the URL is compiled into `BuildConfig`, so it needs a fresh build).
   Logcat (filter `okhttp`) should show `--> GET http://localhost:3000/v1/products/featured` and `<-- 200`.

To go back to the emulator, undo step 3 (or run `adb -e reverse tcp:3000 tcp:3000` so the emulator also understands `localhost`).

Over Wi-Fi instead of USB: use your computer's LAN IP in that property, allow that IP in `android/app/src/debug/res/xml/network_security_config.xml`, and open port 3000 in the firewall.

---

## Project layout
```
backend/        Node + TypeScript API (src/, test/, config/curation.json, Dockerfile)
android/        Android app — open this folder in Android Studio
  app/src/main/java/com/saurabh/catalogue/
    domain/     models, Outcome/AppError, repository interfaces, use cases (plain Kotlin)
    data/       Retrofit API, DTOs, mappers, repository implementations, NetworkConstants
    di/         Hilt modules
    ui/         theme tokens, shared components, screens + ViewModels
    navigation/ type-safe routes and NavHost
docker-compose.yml   api + mailpit
DECISIONS.md    assumptions, trade-offs, growth path, next steps
AI_USAGE.md     tools, prompts, where the AI was wrong, how it was reviewed
CLAUDE.md       rules given to the AI agent for this repo
notes/ai-log.md raw running log written during the build
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App shows "Can't reach the catalogue" | Is `docker compose up` running? Open http://localhost:3000/health on your machine. The emulator must use `10.0.2.2`, not `localhost`. |
| Port 3000 or 8025 already in use | Stop the other process, or set `PORT=3001` in `.env` and update `catalogue.apiBaseUrl.debug`. |
| Gradle sync: `Could not install Gradle distribution … timed out` | Slow/filtered network. Retry, or download the zip from the URL in `android/gradle/wrapper/gradle-wrapper.properties` and place it in `~/.gradle/wrapper/dists/gradle-9.6.0-bin/<hash>/` (see AI_USAGE.md). |
| `docker` not recognised | Install/start Docker Desktop, then open a **new** terminal. |
| First Android build is slow | Expected: Gradle, Hilt/KSP and libraries download and generate code once; later builds are much faster. |
