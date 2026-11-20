import { z } from 'zod';
import type { Category, Product } from '../../domain/product.js';

/** Shape of a product as Fake Store returns it. Treated as untrusted input and validated. */
export const fakeStoreProductSchema = z.object({
  id: z.number().int(),
  title: z.string(),
  price: z.number().nonnegative(),
  description: z.string(),
  category: z.string().min(1),
  image: z.url(),
  rating: z.object({ rate: z.number(), count: z.number().int() }).optional(),
});

export type FakeStoreProduct = z.infer<typeof fakeStoreProductSchema>;

/** "men's clothing" -> "mens-clothing" */
export function toCategorySlug(raw: string): string {
  return raw
    .toLowerCase()
    .replace(/['’]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

/** "men's clothing" -> "Men's clothing" */
export function toCategoryName(raw: string): string {
  const trimmed = raw.trim();
  return trimmed.charAt(0).toUpperCase() + trimmed.slice(1);
}

export function toCategory(raw: string): Category {
  return { slug: toCategorySlug(raw), name: toCategoryName(raw) };
}

export function mapFakeStoreProduct(raw: FakeStoreProduct): Product {
  return {
    id: String(raw.id),
    title: raw.title.trim(),
    description: raw.description.trim(),
    price: { amount: Math.round(raw.price * 100), currency: 'USD' },
    category: toCategory(raw.category),
    rating: { average: raw.rating?.rate ?? 0, count: raw.rating?.count ?? 0 },
    imageSourceUrl: raw.image,
  };
}
