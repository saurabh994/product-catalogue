import type { ErrorRequestHandler, RequestHandler } from 'express';
import type { z } from 'zod';

export interface FieldError {
  field: string;
  message: string;
}

/** An error we expect and can describe to the client. */
export class AppError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly details?: FieldError[],
  ) {
    super(message);
  }
}

/** The product data source (or its image host) is down or returned something unusable. */
export class DataSourceError extends Error {}

/** The email provider refused or failed to send. */
export class EmailDeliveryError extends Error {}

export const notFound = (code: string, message: string) => new AppError(404, code, message);

export function validationError(error: z.ZodError): AppError {
  const details = error.issues.map((issue) => ({
    field: issue.path.join('.') || '(body)',
    message: issue.message,
  }));
  return new AppError(400, 'VALIDATION_FAILED', 'Some fields are invalid', details);
}

export const routeNotFound: RequestHandler = (_req, res) => {
  res.status(404).json({ error: { code: 'NOT_FOUND', message: 'Route not found' } });
};

/** Turns every error into the single JSON error shape. Never leaks stack traces or internals. */
export const errorHandler: ErrorRequestHandler = (err, _req, res, _next) => {
  if (err instanceof AppError) {
    res.status(err.status).json({ error: { code: err.code, message: err.message, details: err.details } });
    return;
  }
  if (err instanceof DataSourceError) {
    console.error('[data-source]', err.message);
    res.status(502).json({ error: { code: 'DATA_SOURCE_UNAVAILABLE', message: 'Products are temporarily unavailable' } });
    return;
  }
  if (err instanceof EmailDeliveryError) {
    console.error('[email]', err.message);
    res.status(502).json({ error: { code: 'EMAIL_DELIVERY_FAILED', message: 'We could not send your enquiry. Please try again.' } });
    return;
  }
  // Body-parser errors (malformed JSON, body too large) carry a `type`.
  const type = (err as { type?: string }).type;
  if (type === 'entity.parse.failed') {
    res.status(400).json({ error: { code: 'INVALID_JSON', message: 'Request body is not valid JSON' } });
    return;
  }
  if (type === 'entity.too.large') {
    res.status(413).json({ error: { code: 'PAYLOAD_TOO_LARGE', message: 'Request body is too large' } });
    return;
  }
  console.error('[unhandled]', err);
  res.status(500).json({ error: { code: 'INTERNAL_ERROR', message: 'Something went wrong' } });
};
