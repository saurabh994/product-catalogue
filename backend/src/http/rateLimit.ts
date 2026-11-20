import { rateLimit } from 'express-rate-limit';

/** Per-IP limit for POST /v1/enquiries to stop abuse of the sales inbox. In-memory: fine for one instance. */
export function enquiryRateLimit(limitPerMinute: number) {
  return rateLimit({
    windowMs: 60_000,
    limit: limitPerMinute,
    standardHeaders: 'draft-8',
    legacyHeaders: false,
    handler: (_req, res) => {
      res.status(429).json({ error: { code: 'RATE_LIMITED', message: 'Too many enquiries. Please try again in a minute.' } });
    },
  });
}
