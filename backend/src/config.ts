import path from 'node:path';
import { z } from 'zod';

/**
 * All runtime configuration comes from environment variables, validated at startup.
 * Bad config fails fast with a readable message instead of failing on the first request.
 */
const configSchema = z.object({
  PORT: z.coerce.number().int().positive().default(3000),
  FAKESTORE_BASE_URL: z.url().default('https://fakestoreapi.com'),
  PRODUCT_CACHE_TTL_SECONDS: z.coerce.number().int().nonnegative().default(300),
  CURATION_FILE: z.string().min(1).default(path.resolve('config/curation.json')),
  SMTP_HOST: z.string().min(1).default('localhost'),
  SMTP_PORT: z.coerce.number().int().positive().default(1025),
  SALES_INBOX: z.email().default('sales@example.com'),
  ENQUIRY_FROM: z.email().default('no-reply@catalogue.local'),
  ENQUIRY_RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(5),
});

export type Config = z.infer<typeof configSchema>;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  const result = configSchema.safeParse(env);
  if (!result.success) {
    throw new Error(`Invalid configuration:\n${z.prettifyError(result.error)}`);
  }
  return result.data;
}
