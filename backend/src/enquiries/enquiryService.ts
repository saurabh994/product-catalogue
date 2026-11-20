import { randomUUID } from 'node:crypto';
import type { ProductSource } from '../catalogue/ProductSource.js';
import { EmailDeliveryError, notFound } from '../http/errors.js';
import type { EmailSender } from './EmailSender.js';
import { buildEnquiryEmail } from './enquiryEmail.js';
import type { EnquiryInput } from './enquirySchema.js';

export interface EnquiryResult {
  enquiryId: string;
}

export class EnquiryService {
  constructor(
    private readonly products: ProductSource,
    private readonly email: EmailSender,
    private readonly settings: { salesInbox: string; from: string },
    private readonly log: Pick<Console, 'warn' | 'info'> = console,
  ) {}

  async submit(input: EnquiryInput): Promise<EnquiryResult> {
    const enquiryId = randomUUID();

    // Honeypot filled → almost certainly a bot. Pretend success so it learns nothing; send nothing.
    if (input.website) {
      this.log.warn(`[enquiry] honeypot triggered, dropped ${enquiryId}`);
      return { enquiryId };
    }

    const product = await this.products.getProduct(input.productId);
    if (!product) throw notFound('PRODUCT_NOT_FOUND', 'Product not found');

    const message = buildEnquiryEmail({
      enquiryId,
      product,
      enquiry: input,
      to: this.settings.salesInbox,
      from: this.settings.from,
    });

    try {
      await this.email.send(message);
    } catch (cause) {
      throw new EmailDeliveryError(`Failed to send enquiry ${enquiryId}`, { cause });
    }
    this.log.info(`[enquiry] sent ${enquiryId} for product ${product.id}`);
    return { enquiryId };
  }
}
