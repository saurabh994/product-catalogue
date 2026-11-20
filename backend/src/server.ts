import { createApp } from './app.js';
import { CachedProductSource } from './catalogue/CachedProductSource.js';
import { CatalogueService } from './catalogue/catalogueService.js';
import { loadCurationFile } from './catalogue/CurationSource.js';
import { FakeStoreProductSource } from './catalogue/fakestore/FakeStoreProductSource.js';
import { HttpImageFetcher } from './catalogue/ImageFetcher.js';
import { loadConfig } from './config.js';
import { EnquiryService } from './enquiries/enquiryService.js';
import { SmtpEmailSender } from './enquiries/SmtpEmailSender.js';

// Composition root: the only place that picks concrete implementations.
// Swapping the product provider or email provider means changing a line here, nothing else.
const config = loadConfig();

const productSource = new CachedProductSource(
  new FakeStoreProductSource(config.FAKESTORE_BASE_URL),
  config.PRODUCT_CACHE_TTL_SECONDS * 1000,
);
const curation = loadCurationFile(config.CURATION_FILE);
const emailSender = new SmtpEmailSender({ host: config.SMTP_HOST, port: config.SMTP_PORT });

const app = createApp({
  catalogue: new CatalogueService(productSource, curation),
  enquiries: new EnquiryService(productSource, emailSender, {
    salesInbox: config.SALES_INBOX,
    from: config.ENQUIRY_FROM,
  }),
  images: new HttpImageFetcher(),
  enquiryRateLimitPerMinute: config.ENQUIRY_RATE_LIMIT_PER_MINUTE,
});

const server = app.listen(config.PORT, () => {
  console.info(`Catalogue API listening on http://localhost:${config.PORT}`);
  // Warm the cache so the first customer doesn't wait for the (slow) upstream source.
  // Failure is harmless: the first real request simply fetches again.
  productSource
    .listProducts()
    .then((products) => console.info(`[cache] warmed with ${products.length} products`))
    .catch((error: unknown) => console.warn('[cache] warm-up failed, will load on first request:', String(error)));
});

for (const signal of ['SIGTERM', 'SIGINT'] as const) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
