import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { buildTestApp } from './fakes/testApp.js';

describe('GET /v1/products/featured', () => {
  it('returns the 5 featured products in the public shape', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products/featured').expect(200);
    expect(res.body.products).toHaveLength(5);
    expect(res.body.products[0]).toEqual({
      id: '6',
      title: 'Portable SSD 1TB',
      description: 'Fast external storage',
      price: { amount: 1999, currency: 'USD' },
      imageUrl: '/v1/products/6/image',
      category: { slug: 'electronics', name: 'Electronics' },
      rating: { average: 4.8, count: 100 },
    });
  });

  it('never exposes the source image URL', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products/featured');
    expect(JSON.stringify(res.body)).not.toContain('images.example.test');
  });

  it('returns 502 with the error shape when the data source is down', async () => {
    const { app, products } = buildTestApp();
    products.failing = true;
    const res = await request(app).get('/v1/products/featured').expect(502);
    expect(res.body.error.code).toBe('DATA_SOURCE_UNAVAILABLE');
  });
});

describe('GET /v1/products?q=', () => {
  it('returns only matching products', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products').query({ q: 'jacket' }).expect(200);
    expect(res.body.query).toBe('jacket');
    expect(res.body.products.map((p: { id: string }) => p.id)).toEqual(['1']);
  });

  it('returns 200 with an empty list when nothing matches', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products').query({ q: 'zzz' }).expect(200);
    expect(res.body.products).toEqual([]);
  });

  it.each([[''], ['   ']])('rejects a blank term %j with 400', async (q) => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products').query({ q }).expect(400);
    expect(res.body.error.code).toBe('VALIDATION_FAILED');
  });

  it('rejects a missing or over-long term with 400', async () => {
    const { app } = buildTestApp();
    await request(app).get('/v1/products').expect(400);
    await request(app).get('/v1/products').query({ q: 'x'.repeat(101) }).expect(400);
  });
});

describe('GET /v1/products/:id', () => {
  it('returns the product', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/products/1').expect(200);
    expect(res.body.product.title).toBe('Mens Cotton Jacket');
  });

  it.each([['999'], ['bad id!']])('returns 404 for %j', async (id) => {
    const { app } = buildTestApp();
    const res = await request(app).get(`/v1/products/${encodeURIComponent(id)}`).expect(404);
    expect(res.body.error.code).toBe('PRODUCT_NOT_FOUND');
  });
});

describe('GET /v1/products/:id/image', () => {
  it("streams the product's image fetched from its source URL, with caching headers", async () => {
    const { app, images } = buildTestApp();
    const res = await request(app).get('/v1/products/1/image').expect(200);
    expect(res.headers['content-type']).toBe('image/jpeg');
    expect(res.headers['cache-control']).toBe('public, max-age=86400');
    expect(images.requested).toEqual(['https://images.example.test/1.jpg']);
  });

  it('returns 404 and fetches nothing for an unknown product', async () => {
    const { app, images } = buildTestApp();
    await request(app).get('/v1/products/999/image').expect(404);
    expect(images.requested).toEqual([]);
  });
});

describe('categories', () => {
  it('GET /v1/categories lists categories', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/categories').expect(200);
    expect(res.body.categories).toContainEqual({ slug: 'mens-clothing', name: "Men's clothing" });
  });

  it('GET /v1/categories/:slug/products returns top rated and others', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/categories/mens-clothing/products').expect(200);
    expect(res.body.category.slug).toBe('mens-clothing');
    expect(res.body.topRated.map((p: { id: string }) => p.id)).toEqual(['1', '2']);
    expect(res.body.others.map((p: { id: string }) => p.id)).toEqual(['3']);
  });

  it('returns 404 for an unknown category', async () => {
    const { app } = buildTestApp();
    const res = await request(app).get('/v1/categories/nope/products').expect(404);
    expect(res.body.error.code).toBe('CATEGORY_NOT_FOUND');
  });
});

describe('misc', () => {
  it('GET /health is ok', async () => {
    await request(buildTestApp().app).get('/health').expect(200, { status: 'ok' });
  });

  it('unknown routes return the JSON error shape', async () => {
    const res = await request(buildTestApp().app).get('/v1/nope').expect(404);
    expect(res.body.error.code).toBe('NOT_FOUND');
  });
});
