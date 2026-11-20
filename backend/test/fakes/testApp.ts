import { createApp } from '../../src/app.js';
import { CatalogueService } from '../../src/catalogue/catalogueService.js';
import { EnquiryService } from '../../src/enquiries/enquiryService.js';
import { curationOf, FakeEmailSender, FakeImageFetcher, FakeProductSource, silentLog } from './fakes.js';

export const SALES_INBOX = 'sales@example.test';

/** The real app, wired with fakes — no network, no SMTP. */
export function buildTestApp(opts: { rateLimit?: number } = {}) {
  const products = new FakeProductSource();
  const email = new FakeEmailSender();
  const images = new FakeImageFetcher();
  const app = createApp({
    catalogue: new CatalogueService(products, curationOf(), silentLog),
    enquiries: new EnquiryService(products, email, { salesInbox: SALES_INBOX, from: 'no-reply@example.test' }, silentLog),
    images,
    enquiryRateLimitPerMinute: opts.rateLimit ?? 100,
  });
  return { app, products, email, images };
}
