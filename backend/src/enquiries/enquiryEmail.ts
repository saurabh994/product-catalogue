import type { Product } from '../domain/product.js';
import type { EmailMessage } from './EmailSender.js';
import type { EnquiryInput } from './enquirySchema.js';

export function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

const formatPrice = (p: Product) => `${(p.price.amount / 100).toFixed(2)} ${p.price.currency}`;

/** Builds the sales email. Product details come from our data source, never from the request. */
export function buildEnquiryEmail(args: {
  enquiryId: string;
  product: Product;
  enquiry: EnquiryInput;
  to: string;
  from: string;
}): EmailMessage {
  const { enquiryId, product, enquiry } = args;
  const rows: [string, string][] = [
    ['Product', `${product.title} (ID ${product.id})`],
    ['Price', formatPrice(product)],
    ['Name', enquiry.name],
    ['Email', enquiry.email],
    ['Phone', enquiry.phone ?? '—'],
    ['Message', enquiry.message ?? '—'],
    ['Enquiry ID', enquiryId],
  ];

  const text = rows.map(([k, v]) => `${k}: ${v}`).join('\n');
  const html = `<h2>New product enquiry</h2><table>${rows
    .map(([k, v]) => `<tr><th align="left">${escapeHtml(k)}</th><td>${escapeHtml(v).replace(/\n/g, '<br>')}</td></tr>`)
    .join('')}</table>`;

  return {
    to: args.to,
    from: args.from,
    replyTo: { name: enquiry.name, address: enquiry.email },
    subject: `Product enquiry: ${product.title}`,
    text,
    html,
  };
}
