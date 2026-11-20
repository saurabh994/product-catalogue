import type { Category, Price, Product, Rating } from '../domain/product.js';

/** Public API shape of a product (see DECISIONS.md §3). */
export interface ProductResponse {
  id: string;
  title: string;
  description: string;
  price: Price;
  imageUrl: string;
  category: Category;
  rating: Rating;
}

/** Hides the source image URL; clients load images through our own endpoint (relative to the API base URL). */
export function toProductResponse(p: Product): ProductResponse {
  return {
    id: p.id,
    title: p.title,
    description: p.description,
    price: p.price,
    imageUrl: `/v1/products/${encodeURIComponent(p.id)}/image`,
    category: p.category,
    rating: p.rating,
  };
}
