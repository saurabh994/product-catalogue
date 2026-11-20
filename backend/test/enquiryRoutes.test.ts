import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { buildTestApp, SALES_INBOX } from './fakes/testApp.js';

const valid = {
  productId: '1',
  name: 'Asha Rao',
  email: 'asha@example.com',
  phone: '+91 98765 43210',
  message: 'Do you ship to Pune?',
};

describe('POST /v1/enquiries', () => {
  it('emails the sales inbox and returns 201', async () => {
    const { app, email } = buildTestApp();
    const res = await request(app).post('/v1/enquiries').send(valid).expect(201);

    expect(res.body).toEqual({ enquiryId: expect.any(String), status: 'sent' });
    expect(email.sent).toHaveLength(1);
    const sent = email.sent[0]!;
    expect(sent.to).toBe(SALES_INBOX);
    expect(sent.replyTo).toEqual({ name: 'Asha Rao', address: 'asha@example.com' });
    expect(sent.subject).toBe('Product enquiry: Mens Cotton Jacket');
    expect(sent.text).toContain('Do you ship to Pune?');
    expect(sent.text).toContain(res.body.enquiryId);
  });

  it('accepts an enquiry with only the required fields', async () => {
    const { app, email } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ productId: '1', name: 'Asha', email: 'a@b.co' }).expect(201);
    expect(email.sent[0]!.text).toContain('Phone: —');
  });

  it('uses product details from the data source, not the request', async () => {
    const { app, email } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, productTitle: 'FREE STUFF' }).expect(201);
    expect(email.sent[0]!.subject).toBe('Product enquiry: Mens Cotton Jacket');
  });

  it('escapes HTML in user input', async () => {
    const { app, email } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, message: '<script>alert(1)</script>' }).expect(201);
    expect(email.sent[0]!.html).not.toContain('<script>');
    expect(email.sent[0]!.html).toContain('&lt;script&gt;');
  });

  it('returns field-level validation errors and sends nothing', async () => {
    const { app, email } = buildTestApp();
    const res = await request(app).post('/v1/enquiries').send({ productId: '1', name: '', email: 'not-an-email' }).expect(400);
    expect(res.body.error.code).toBe('VALIDATION_FAILED');
    expect(res.body.error.details.map((d: { field: string }) => d.field).sort()).toEqual(['email', 'name']);
    expect(email.sent).toHaveLength(0);
  });

  it('rejects line breaks in the name (email header injection)', async () => {
    const { app } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, name: 'Asha\r\nBcc: victim@example.com' }).expect(400);
  });

  it('rejects an invalid phone number', async () => {
    const { app } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, phone: 'call me maybe' }).expect(400);
  });

  it('returns 404 for an unknown product', async () => {
    const { app, email } = buildTestApp();
    const res = await request(app).post('/v1/enquiries').send({ ...valid, productId: '999' }).expect(404);
    expect(res.body.error.code).toBe('PRODUCT_NOT_FOUND');
    expect(email.sent).toHaveLength(0);
  });

  it('pretends success but sends nothing when the honeypot is filled', async () => {
    const { app, email } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, website: 'http://spam.example' }).expect(201);
    expect(email.sent).toHaveLength(0);
  });

  it('returns 502 when the email provider fails, so the app does not show a false "Thank you"', async () => {
    const { app, email } = buildTestApp();
    email.failing = true;
    const res = await request(app).post('/v1/enquiries').send(valid).expect(502);
    expect(res.body.error.code).toBe('EMAIL_DELIVERY_FAILED');
  });

  it('rate-limits repeated submissions with 429', async () => {
    const { app } = buildTestApp({ rateLimit: 2 });
    await request(app).post('/v1/enquiries').send(valid).expect(201);
    await request(app).post('/v1/enquiries').send(valid).expect(201);
    const res = await request(app).post('/v1/enquiries').send(valid).expect(429);
    expect(res.body.error.code).toBe('RATE_LIMITED');
  });

  it('returns 400 for malformed JSON', async () => {
    const { app } = buildTestApp();
    const res = await request(app).post('/v1/enquiries').set('Content-Type', 'application/json').send('{"name":').expect(400);
    expect(res.body.error.code).toBe('INVALID_JSON');
  });

  it('returns 413 for an oversized body', async () => {
    const { app } = buildTestApp();
    await request(app).post('/v1/enquiries').send({ ...valid, message: 'x'.repeat(20_000) }).expect(413);
  });
});
