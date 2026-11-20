import { Router, type RequestHandler } from 'express';
import { validationError } from '../http/errors.js';
import { enquirySchema } from './enquirySchema.js';
import type { EnquiryService } from './enquiryService.js';

export function enquiryRoutes(service: EnquiryService, rateLimiter: RequestHandler): Router {
  const router = Router();

  router.post('/enquiries', rateLimiter, async (req, res) => {
    const parsed = enquirySchema.safeParse(req.body ?? {});
    if (!parsed.success) throw validationError(parsed.error);
    const result = await service.submit(parsed.data);
    res.status(201).json({ enquiryId: result.enquiryId, status: 'sent' });
  });

  return router;
}
