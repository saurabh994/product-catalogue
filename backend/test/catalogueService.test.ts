import { describe, expect, it } from 'vitest';
import { CatalogueService } from '../src/catalogue/catalogueService.js';
import { curationOf, FakeProductSource, silentLog } from './fakes/fakes.js';

const service = (curation = curationOf()) => new CatalogueService(new FakeProductSource(), curation, silentLog);
const ids = (products: { id: string }[]) => products.map((p) => p.id);

describe('featured (home page)', () => {
  it('returns the 5 chosen products in the chosen order', async () => {
    expect(ids(await service().featured())).toEqual(['6', '1', '4', '2', '7']);
  });

  it('skips chosen IDs that no longer exist instead of failing', async () => {
    const result = await service(curationOf({ featured: ['6', 'gone', '1'] })).featured();
    expect(ids(result)).toEqual(['6', '1']);
  });
});

describe('categories', () => {
  it('lists each category once, sorted by name', async () => {
    const result = await service().categories();
    expect(result.map((c) => c.slug)).toEqual(['electronics', 'jewelery', 'mens-clothing']);
  });
});

describe('categoryPage', () => {
  it('uses curated top-rated picks when the category has them', async () => {
    const page = await service().categoryPage('electronics');
    expect(ids(page!.topRated)).toEqual(['7']);
    expect(ids(page!.others)).toEqual(['6', '8']); // the rest, by rating
  });

  it('falls back to the 2 highest-rated when nothing is curated', async () => {
    const page = await service().categoryPage('mens-clothing');
    expect(ids(page!.topRated)).toEqual(['1', '2']);
    expect(ids(page!.others)).toEqual(['3']);
  });

  it('never shows a product in both sections', async () => {
    const page = await service().categoryPage('jewelery');
    const top = new Set(ids(page!.topRated));
    expect(page!.others.some((p) => top.has(p.id))).toBe(false);
  });

  it('ignores curated IDs that belong to another category', async () => {
    const page = await service(curationOf({ topRatedByCategory: { electronics: ['1'] } })).categoryPage('electronics');
    expect(ids(page!.topRated)).toEqual(['6', '7']); // fell back
  });

  it('returns undefined for an unknown category', async () => {
    expect(await service().categoryPage('nope')).toBeUndefined();
  });
});

describe('search', () => {
  it('matches case-insensitively on title', async () => {
    expect(ids(await service().search('JACKET'))).toEqual(['1']);
  });

  it('requires every word to match, in any order', async () => {
    expect(ids(await service().search('jacket cotton'))).toEqual(['1']);
    expect(ids(await service().search('jacket monitor'))).toEqual([]);
  });

  it('matches description and category name', async () => {
    expect(ids(await service().search('outerwear'))).toEqual(['1']);
    expect(ids(await service().search('electronics'))).toEqual(['6', '7', '8']);
  });

  it("ignores apostrophes so 'mens' finds Men's clothing", async () => {
    expect(ids(await service().search('mens'))).toEqual(['1', '2', '3']);
  });

  it('returns nothing for a blank term', async () => {
    expect(await service().search('   ')).toEqual([]);
  });
});
