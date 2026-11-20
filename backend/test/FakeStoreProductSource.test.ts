import { describe, expect, it } from 'vitest';
import { FakeStoreProductSource } from '../src/catalogue/fakestore/FakeStoreProductSource.js';
import { DataSourceError } from '../src/http/errors.js';

const item = {
  id: 1, title: 'Backpack', price: 109.95, description: 'Bag', category: "men's clothing",
  image: 'https://fakestoreapi.com/img/1.jpg', rating: { rate: 3.9, count: 120 },
};

/** Stub fetch that answers one request. */
const stubFetch = (status: number, body: string): typeof fetch =>
  (async () => new Response(body, { status })) as typeof fetch;

const source = (f: typeof fetch) => new FakeStoreProductSource('https://fakestore.test', f);

describe('FakeStoreProductSource', () => {
  it('lists and maps products', async () => {
    const products = await source(stubFetch(200, JSON.stringify([item]))).listProducts();
    expect(products.map((p) => p.id)).toEqual(['1']);
  });

  it('returns undefined for an unknown ID (Fake Store sends 200 with an empty body)', async () => {
    expect(await source(stubFetch(200, '')).getProduct('999')).toBeUndefined();
  });

  it('throws DataSourceError when Fake Store errors', async () => {
    await expect(source(stubFetch(503, 'down')).listProducts()).rejects.toBeInstanceOf(DataSourceError);
  });

  it('throws DataSourceError when Fake Store is unreachable', async () => {
    const failing = (async () => { throw new TypeError('fetch failed'); }) as typeof fetch;
    await expect(source(failing).listProducts()).rejects.toBeInstanceOf(DataSourceError);
  });

  it('throws DataSourceError on an unexpected response shape', async () => {
    await expect(source(stubFetch(200, JSON.stringify([{ nope: true }]))).listProducts()).rejects.toBeInstanceOf(DataSourceError);
  });
});
