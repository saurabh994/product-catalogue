import { describe, expect, it } from 'vitest';
import { CachedProductSource } from '../src/catalogue/CachedProductSource.js';
import { DataSourceError } from '../src/http/errors.js';
import { FakeProductSource, silentLog } from './fakes/fakes.js';

function setup() {
  let time = 0;
  const inner = new FakeProductSource();
  const cached = new CachedProductSource(inner, 1000, () => time, silentLog);
  return { inner, cached, advance: (ms: number) => (time += ms) };
}

describe('CachedProductSource', () => {
  it('serves repeat calls from cache within the TTL', async () => {
    const { inner, cached } = setup();
    await cached.listProducts();
    await cached.listProducts();
    expect(inner.listCalls).toBe(1);
  });

  it('refreshes after the TTL expires', async () => {
    const { inner, cached, advance } = setup();
    await cached.listProducts();
    advance(1001);
    await cached.listProducts();
    expect(inner.listCalls).toBe(2);
  });

  it('serves the stale copy when the source fails after expiry', async () => {
    const { inner, cached, advance } = setup();
    const first = await cached.listProducts();
    advance(1001);
    inner.failing = true;
    expect(await cached.listProducts()).toEqual(first);
  });

  it('propagates the error when there is nothing cached', async () => {
    const { inner, cached } = setup();
    inner.failing = true;
    await expect(cached.listProducts()).rejects.toBeInstanceOf(DataSourceError);
  });

  it('does not cache "not found", so a product added later appears', async () => {
    const { inner, cached } = setup();
    expect(await cached.getProduct('new')).toBeUndefined();
    inner.products = [...inner.products, { ...inner.products[0]!, id: 'new' }];
    expect((await cached.getProduct('new'))?.id).toBe('new');
  });

  it('shares one upstream request between concurrent callers (no stampede)', async () => {
    const { inner, cached } = setup();
    const [a, b] = await Promise.all([cached.listProducts(), cached.listProducts()]);
    expect(inner.listCalls).toBe(1);
    expect(a).toBe(b);
  });

  it('lets the next caller retry after a shared request fails', async () => {
    const { inner, cached } = setup();
    inner.failing = true;
    await expect(Promise.all([cached.listProducts(), cached.listProducts()])).rejects.toBeInstanceOf(DataSourceError);
    expect(inner.listCalls).toBe(1);
    inner.failing = false;
    await expect(cached.listProducts()).resolves.toHaveLength(inner.products.length);
  });
});
