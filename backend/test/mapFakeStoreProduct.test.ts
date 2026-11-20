import { describe, expect, it } from 'vitest';
import { fakeStoreProductSchema, mapFakeStoreProduct, toCategorySlug } from '../src/catalogue/fakestore/mapFakeStoreProduct.js';

const raw = {
  id: 3,
  title: ' Mens Cotton Jacket ',
  price: 55.99,
  description: 'Warm',
  category: "men's clothing",
  image: 'https://fakestoreapi.com/img/3.jpg',
  rating: { rate: 4.7, count: 500 },
};

describe('mapFakeStoreProduct', () => {
  it('maps the provider shape into our Product model', () => {
    expect(mapFakeStoreProduct(fakeStoreProductSchema.parse(raw))).toEqual({
      id: '3',
      title: 'Mens Cotton Jacket',
      description: 'Warm',
      price: { amount: 5599, currency: 'USD' },
      category: { slug: 'mens-clothing', name: "Men's clothing" },
      rating: { average: 4.7, count: 500 },
      imageSourceUrl: 'https://fakestoreapi.com/img/3.jpg',
    });
  });

  it('stores price in cents without floating-point drift', () => {
    expect(mapFakeStoreProduct(fakeStoreProductSchema.parse({ ...raw, price: 0.29 })).price.amount).toBe(29);
  });

  it('defaults a missing rating to zero', () => {
    const { rating: _ignored, ...noRating } = raw;
    expect(mapFakeStoreProduct(fakeStoreProductSchema.parse(noRating)).rating).toEqual({ average: 0, count: 0 });
  });

  it('rejects malformed provider data', () => {
    expect(fakeStoreProductSchema.safeParse({ ...raw, price: 'free' }).success).toBe(false);
  });

  it.each([
    ["men's clothing", 'mens-clothing'],
    ["women's clothing", 'womens-clothing'],
    ['electronics', 'electronics'],
  ])('slugifies %s → %s', (input, expected) => {
    expect(toCategorySlug(input)).toBe(expected);
  });
});
