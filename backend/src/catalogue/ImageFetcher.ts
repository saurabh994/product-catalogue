import { DataSourceError } from '../http/errors.js';

export interface FetchedImage {
  contentType: string;
  body: Buffer;
}

/** Fetches a product image from wherever the data source stores it. */
export interface ImageFetcher {
  fetch(url: string): Promise<FetchedImage>;
}

export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

export class HttpImageFetcher implements ImageFetcher {
  constructor(
    private readonly fetchFn: typeof fetch = fetch,
    private readonly timeoutMs = 5000,
  ) {}

  async fetch(url: string): Promise<FetchedImage> {
    let response: Response;
    try {
      response = await this.fetchFn(url, { signal: AbortSignal.timeout(this.timeoutMs) });
    } catch (cause) {
      throw new DataSourceError('Image source unreachable', { cause });
    }
    if (!response.ok) throw new DataSourceError(`Image source responded ${response.status}`);

    const contentType = response.headers.get('content-type') ?? '';
    if (!contentType.startsWith('image/')) throw new DataSourceError('Image source returned a non-image');

    const declaredLength = Number(response.headers.get('content-length') ?? 0);
    if (declaredLength > MAX_IMAGE_BYTES) throw new DataSourceError('Image too large');

    const body = Buffer.from(await response.arrayBuffer());
    if (body.byteLength > MAX_IMAGE_BYTES) throw new DataSourceError('Image too large');

    return { contentType, body };
  }
}
