import { Router } from 'express';
import { z } from 'zod';
import { PRODUCT_ID_PATTERN } from '../domain/product.js';
import { notFound, validationError } from '../http/errors.js';
import type { CatalogueService } from './catalogueService.js';
import type { ImageFetcher } from './ImageFetcher.js';
import { toProductResponse } from './productResponse.js';

const searchQuerySchema = z.object({
  q: z
    .string({ error: 'Search term is required' })
    .trim()
    .min(1, 'Search term is required')
    .max(100, 'Search term must be 100 characters or fewer'),
});

/** HTTP layer only: parse input → call the service → shape the response. */
export function catalogueRoutes(service: CatalogueService, images: ImageFetcher): Router {
  const router = Router();

  router.get('/products/featured', async (_req, res) => {
    const products = await service.featured();
    res.json({ products: products.map(toProductResponse) });
  });

  router.get('/products', async (req, res) => {
    const parsed = searchQuerySchema.safeParse(req.query);
    if (!parsed.success) throw validationError(parsed.error);
    const products = await service.search(parsed.data.q);
    res.json({ query: parsed.data.q, products: products.map(toProductResponse) });
  });

  router.get('/products/:id', async (req, res) => {
    const product = await findProduct(service, req.params.id);
    res.json({ product: toProductResponse(product) });
  });

  router.get('/products/:id/image', async (req, res) => {
    // Look up by ID and use the source's own URL — never a client-supplied URL (no open proxy / SSRF).
    const product = await findProduct(service, req.params.id);
    const image = await images.fetch(product.imageSourceUrl);
    res
      .set('Content-Type', image.contentType)
      .set('Cache-Control', 'public, max-age=86400')
      .send(image.body);
  });

  router.get('/categories', async (_req, res) => {
    res.json({ categories: await service.categories() });
  });

  router.get('/categories/:slug/products', async (req, res) => {
    const page = await service.categoryPage(req.params.slug);
    if (!page) throw notFound('CATEGORY_NOT_FOUND', 'Category not found');
    res.json({
      category: page.category,
      topRated: page.topRated.map(toProductResponse),
      others: page.others.map(toProductResponse),
    });
  });

  return router;
}

async function findProduct(service: CatalogueService, id: string) {
  if (!PRODUCT_ID_PATTERN.test(id)) throw notFound('PRODUCT_NOT_FOUND', 'Product not found');
  const product = await service.getProduct(id);
  if (!product) throw notFound('PRODUCT_NOT_FOUND', 'Product not found');
  return product;
}

