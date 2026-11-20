import { z } from 'zod';
import type { Product } from '../../domain/product.js';
import { DataSourceError } from '../../http/errors.js';
import type { ProductSource } from '../ProductSource.js';
import { fakeStoreProductSchema, mapFakeStoreProduct } from './mapFakeStoreProduct.js';

/** Adapter for https://fakestoreapi.com. The only file that knows Fake Store's URLs. */
export class FakeStoreProductSource implements ProductSource {
  constructor(
    private readonly baseUrl: string,
    private readonly fetchFn: typeof fetch = fetch,
    private readonly timeoutMs = 5000,
  ) {}

  async listProducts(): Promise<Product[]> {
    const body = await this.getJson('/products');
    const parsed = z.array(fakeStoreProductSchema).safeParse(body);
    if (!parsed.success) throw new DataSourceError('Unexpected product list shape from Fake Store');
    return parsed.data.map(mapFakeStoreProduct);
  }

  async getProduct(id: string): Promise<Product | undefined> {
    const body = await this.getJson(`/products/${encodeURIComponent(id)}`, { allowNotFound: true });
    // Fake Store answers unknown IDs with 200 and an empty body.
    if (body === undefined || body === null) return undefined;
    const parsed = fakeStoreProductSchema.safeParse(body);
    if (!parsed.success) throw new DataSourceError('Unexpected product shape from Fake Store');
    return mapFakeStoreProduct(parsed.data);
  }

  private async getJson(path: string, opts: { allowNotFound?: boolean } = {}): Promise<unknown> {
    let response: Response;
    try {
      response = await this.fetchFn(new URL(path, this.baseUrl), {
        headers: { accept: 'application/json' },
        signal: AbortSignal.timeout(this.timeoutMs),
      });
    } catch (cause) {
      throw new DataSourceError('Fake Store unreachable', { cause });
    }
    if (opts.allowNotFound && response.status === 404) return undefined;
    if (!response.ok) throw new DataSourceError(`Fake Store responded ${response.status}`);

    const text = await response.text();
    if (text.trim() === '') return undefined;
    try {
      return JSON.parse(text);
    } catch (cause) {
      throw new DataSourceError('Fake Store returned invalid JSON', { cause });
    }
  }
}
