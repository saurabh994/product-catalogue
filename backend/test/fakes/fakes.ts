import type { CatalogueService } from '../../src/catalogue/catalogueService.js';
import type { Curation } from '../../src/catalogue/CurationSource.js';
import { StaticCurationSource } from '../../src/catalogue/CurationSource.js';
import type { FetchedImage, ImageFetcher } from '../../src/catalogue/ImageFetcher.js';
import type { ProductSource } from '../../src/catalogue/ProductSource.js';
import type { Product } from '../../src/domain/product.js';
import type { EmailMessage, EmailSender } from '../../src/enquiries/EmailSender.js';
import { DataSourceError } from '../../src/http/errors.js';
import { fixtureProducts } from './fixtures.js';

/** In-memory ProductSource. Set `failing = true` to simulate the provider being down. */
export class FakeProductSource implements ProductSource {
  failing = false;
  listCalls = 0;
  constructor(public products: Product[] = fixtureProducts) {}

  async listProducts(): Promise<Product[]> {
    this.listCalls++;
    if (this.failing) throw new DataSourceError('fake source down');
    return this.products;
  }

  async getProduct(id: string): Promise<Product | undefined> {
    if (this.failing) throw new DataSourceError('fake source down');
    return this.products.find((p) => p.id === id);
  }
}

/** Records sent emails instead of sending them. Set `failing = true` to simulate the provider failing. */
export class FakeEmailSender implements EmailSender {
  failing = false;
  sent: EmailMessage[] = [];
  async send(message: EmailMessage): Promise<void> {
    if (this.failing) throw new Error('smtp down');
    this.sent.push(message);
  }
}

export class FakeImageFetcher implements ImageFetcher {
  requested: string[] = [];
  async fetch(url: string): Promise<FetchedImage> {
    this.requested.push(url);
    return { contentType: 'image/jpeg', body: Buffer.from('fake-jpeg-bytes') };
  }
}

export const silentLog = { warn: () => {}, info: () => {} };

export const defaultCuration: Curation = {
  featured: ['6', '1', '4', '2', '7'],
  topRatedByCategory: { electronics: ['7'] },
};

export const curationOf = (c: Partial<Curation> = {}) => new StaticCurationSource({ ...defaultCuration, ...c });

export type { CatalogueService };
