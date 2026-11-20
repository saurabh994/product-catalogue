import { readFileSync } from 'node:fs';
import { z } from 'zod';

/** Which products the business has chosen to promote. */
export interface Curation {
  /** Home page "top rated" — the business chooses 5. */
  featured: string[];
  /** Optional per-category "top rated" choices, keyed by category slug. */
  topRatedByCategory: Record<string, string[]>;
}

/** Boundary so a CMS / admin tool can replace the JSON file later. */
export interface CurationSource {
  getCuration(): Promise<Curation>;
}

const curationSchema = z.object({
  featured: z.array(z.string().min(1)).min(1).max(5),
  topRatedByCategory: z.record(z.string(), z.array(z.string().min(1))).default({}),
});

export class StaticCurationSource implements CurationSource {
  constructor(private readonly curation: Curation) {}
  async getCuration(): Promise<Curation> {
    return this.curation;
  }
}

/** Reads and validates the curation file once at startup (fails fast if it is wrong). */
export function loadCurationFile(filePath: string): StaticCurationSource {
  const parsed = curationSchema.safeParse(JSON.parse(readFileSync(filePath, 'utf8')));
  if (!parsed.success) {
    throw new Error(`Invalid curation file ${filePath}:\n${z.prettifyError(parsed.error)}`);
  }
  if (parsed.data.featured.length !== 5) {
    console.warn(`[curation] expected 5 featured products, found ${parsed.data.featured.length}`);
  }
  return new StaticCurationSource(parsed.data);
}
