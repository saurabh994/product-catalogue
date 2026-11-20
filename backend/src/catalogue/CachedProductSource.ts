import type { Product } from '../domain/product.js';
import type { ProductSource } from './ProductSource.js';

interface Entry<T> {
  value: T;
  expiresAt: number;
}

/**
 * Decorator that adds a TTL cache to any ProductSource.
 * If the source fails and we have an expired copy, we serve the stale copy rather than an error.
 * "Not found" results are not cached, so random IDs can't grow the cache without bound.
 */
export class CachedProductSource implements ProductSource {
  private readonly cache = new Map<string, Entry<unknown>>();

  constructor(
    private readonly inner: ProductSource,
    private readonly ttlMs: number,
    private readonly now: () => number = Date.now,
    private readonly log: Pick<Console, 'warn'> = console,
  ) {}

  listProducts(): Promise<Product[]> {
    return this.cached('products', () => this.inner.listProducts());
  }

  getProduct(id: string): Promise<Product | undefined> {
    return this.cached(`product:${id}`, () => this.inner.getProduct(id));
  }

  private async cached<T>(key: string, load: () => Promise<T>): Promise<T> {
    const hit = this.cache.get(key) as Entry<T> | undefined;
    if (hit && hit.expiresAt > this.now()) return hit.value;

    try {
      const value = await load();
      if (value !== undefined) this.cache.set(key, { value, expiresAt: this.now() + this.ttlMs });
      return value;
    } catch (error) {
      if (hit) {
        this.log.warn(`[cache] source failed for "${key}", serving stale copy`);
        return hit.value;
      }
      throw error;
    }
  }
}
