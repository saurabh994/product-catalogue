import { z } from 'zod';
import { PRODUCT_ID_PATTERN } from '../domain/product.js';

const noLineBreaks = (value: string) => !/[\r\n]/.test(value);

/** Validation for POST /v1/enquiries. The client is never trusted; this is the source of truth. */
export const enquirySchema = z.object({
  productId: z.string({ error: 'Product is required' }).regex(PRODUCT_ID_PATTERN, 'Invalid product'),
  name: z
    .string({ error: 'Name is required' })
    .trim()
    .min(1, 'Name is required')
    .max(100, 'Name must be 100 characters or fewer')
    .refine(noLineBreaks, 'Name must be a single line'), // blocks email header injection via the reply-to name
  email: z
    .string({ error: 'Email is required' })
    .trim()
    .max(254, 'Email is too long')
    .pipe(z.email('Enter a valid email address')),
  phone: z
    .string()
    .trim()
    .max(30, 'Phone must be 30 characters or fewer')
    .regex(/^[+0-9 ()-]*$/, 'Phone can only contain digits, spaces and + ( ) -')
    .optional()
    .transform((v) => v || undefined),
  message: z
    .string()
    .trim()
    .max(1000, 'Message must be 1000 characters or fewer')
    .optional()
    .transform((v) => v || undefined),
  /** Honeypot: hidden from real users. Bots fill it in. */
  website: z.string().max(200).optional(),
});

export type EnquiryInput = z.infer<typeof enquirySchema>;
