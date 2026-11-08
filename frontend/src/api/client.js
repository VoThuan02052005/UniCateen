// Goi REST API; phien dang nhap nam o cookie HttpOnly do may chu cap.
export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Lỗi ${status}`);
    this.status = status;
    this.code = body?.code;
    this.fields = body?.fields;
  }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

async function request(method, url, body, headers = {}) {
  const opts = { method, credentials: 'same-origin', headers: { ...headers } };
  if (body instanceof FormData) {
    opts.body = body;
  } else if (body !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(url, opts);
  if (res.status === 204) return null;
  const type = res.headers.get('Content-Type') || '';
  const data = type.includes('application/json') ? await res.json() : await res.text();
  if (!res.ok) {
    if (res.status === 401 && !url.endsWith('/auth/login')) onUnauthorized();
    throw new ApiError(res.status, typeof data === 'object' ? data : { message: data });
  }
  return data;
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body, headers) => request('POST', url, body, headers),
  put: (url, body) => request('PUT', url, body),
  patch: (url, body) => request('PATCH', url, body),
  del: (url) => request('DELETE', url),
};

/** NFR06: moi lan bam thanh toan sinh mot khoa chong lap; gui lai cung khoa khi thu lai. */
export function newIdempotencyKey() {
  return (crypto.randomUUID && crypto.randomUUID()) || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/** Tai tep bao cao (Excel, PDF). */
export function download(url) {
  window.location.href = url;
}
