import type { Product } from '../domain/product.js';

/**
 * Boundary to wherever product data lives (Fake Store today; a PIM, Shopify or a database later).
 * Implementations throw DataSourceError when the source is unavailable.
 */
export interface ProductSource {
  listProducts(): Promise<Product[]>;
  /** Returns undefined when the product does not exist. */
  getProduct(id: string): Promise<Product | undefined>;
}
