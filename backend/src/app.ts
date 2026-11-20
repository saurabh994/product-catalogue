import express from 'express';
import helmet from 'helmet';
import { catalogueRoutes } from './catalogue/catalogueRoutes.js';
import type { CatalogueService } from './catalogue/catalogueService.js';
import type { ImageFetcher } from './catalogue/ImageFetcher.js';
import { enquiryRoutes } from './enquiries/enquiryRoutes.js';
import type { EnquiryService } from './enquiries/enquiryService.js';
import { errorHandler, routeNotFound } from './http/errors.js';
import { enquiryRateLimit } from './http/rateLimit.js';

export interface AppDependencies {
  catalogue: CatalogueService;
  enquiries: EnquiryService;
  images: ImageFetcher;
  enquiryRateLimitPerMinute: number;
}

/** Builds the HTTP app from its dependencies. Tests pass fakes; server.ts passes the real ones. */
export function createApp(deps: AppDependencies) {
  const app = express();
  app.use(helmet());
  app.use(express.json({ limit: '10kb' }));

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use('/v1', catalogueRoutes(deps.catalogue, deps.images));
  app.use('/v1', enquiryRoutes(deps.enquiries, enquiryRateLimit(deps.enquiryRateLimitPerMinute)));

  app.use(routeNotFound);
  app.use(errorHandler);
  return app;
}
