import type { Category, Product } from '../domain/product.js';
import type { CurationSource } from './CurationSource.js';
import type { ProductSource } from './ProductSource.js';

/** When a category has no curated picks, show this many of its highest-rated products as "top rated". */
export const TOP_RATED_FALLBACK_COUNT = 2;

export interface CategoryPage {
  category: Category;
  topRated: Product[];
  others: Product[];
}

const byRatingDesc = (a: Product, b: Product) => b.rating.average - a.rating.average;

/** Business rules for browsing. Pure logic over our own model — no HTTP, no provider details. */
export class CatalogueService {
  constructor(
    private readonly products: ProductSource,
    private readonly curation: CurationSource,
    private readonly log: Pick<Console, 'warn'> = console,
  ) {}

  /** The business-chosen top-rated products, in the chosen order. Missing IDs are skipped, not fatal. */
  async featured(): Promise<Product[]> {
    const [all, curation] = await Promise.all([this.products.listProducts(), this.curation.getCuration()]);
    const byId = new Map(all.map((p) => [p.id, p]));
    const missing = curation.featured.filter((id) => !byId.has(id));
    if (missing.length > 0) this.log.warn(`[curation] featured IDs not found in source: ${missing.join(', ')}`);
    return curation.featured.flatMap((id) => byId.get(id) ?? []);
  }

  async categories(): Promise<Category[]> {
    const all = await this.products.listProducts();
    const bySlug = new Map(all.map((p) => [p.category.slug, p.category]));
    return [...bySlug.values()].sort((a, b) => a.name.localeCompare(b.name));
  }

  /** Returns undefined for an unknown category. */
  async categoryPage(slug: string): Promise<CategoryPage | undefined> {
    const [all, curation] = await Promise.all([this.products.listProducts(), this.curation.getCuration()]);
    const inCategory = all.filter((p) => p.category.slug === slug);
    const category = inCategory[0]?.category;
    if (!category) return undefined;

    const curatedIds = curation.topRatedByCategory[slug] ?? [];
    const curated = curatedIds.flatMap((id) => inCategory.find((p) => p.id === id) ?? []);
    const topRated =
      curated.length > 0 ? curated : [...inCategory].sort(byRatingDesc).slice(0, TOP_RATED_FALLBACK_COUNT);

    const topIds = new Set(topRated.map((p) => p.id));
    const others = inCategory.filter((p) => !topIds.has(p.id)).sort(byRatingDesc);
    return { category, topRated, others };
  }

  /** Every word of the term must appear in the title, description or category name (case-insensitive). */
  async search(term: string): Promise<Product[]> {
    const words = normalise(term).split(/\s+/).filter(Boolean);
    if (words.length === 0) return [];
    const all = await this.products.listProducts();
    return all
      .filter((p) => {
        const haystack = normalise(`${p.title} ${p.description} ${p.category.name}`);
        return words.every((w) => haystack.includes(w));
      })
      .sort(byRatingDesc);
  }

  getProduct(id: string): Promise<Product | undefined> {
    return this.products.getProduct(id);
  }
}

/** Lower-case and drop apostrophes so "mens" matches "Men's". */
function normalise(text: string): string {
  return text.toLowerCase().replace(/['’]/g, '');
}
