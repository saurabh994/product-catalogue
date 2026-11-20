/**
 * Our own product model. Every data source adapter maps into this shape,
 * so nothing outside an adapter knows which provider we use.
 */
export interface Price {
  /** Minor units (cents) to avoid floating-point rounding errors. */
  amount: number;
  currency: string;
}

export interface Category {
  slug: string;
  name: string;
}

export interface Rating {
  average: number;
  count: number;
}

export interface Product {
  id: string;
  title: string;
  description: string;
  price: Price;
  category: Category;
  rating: Rating;
  /** Where the source keeps the image. Internal only: never sent to clients (they use our image endpoint). */
  imageSourceUrl: string;
}

/** Product IDs we accept from clients. Anything else is treated as "not found". */
export const PRODUCT_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
