import axios, { type InternalAxiosRequestConfig } from 'axios';

export const ACCESS_TOKEN_KEY = 'accessToken';
/** Epoch milliseconds at which the stored access token stops being valid. */
export const ACCESS_TOKEN_EXPIRY_KEY = 'accessTokenExpiresAt';

export const http = axios.create({
  // Empty by default so paths stay relative and go through the dev-server proxy
  // (see each app's vite.config.ts). Set VITE_API_BASE_URL to call a backend directly,
  // which then requires CORS to be configured on that backend.
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: false
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  return config;
});

let unauthorizedHandler: (() => void) | null = null;

/** Let the app react when the backend rejects the session (401) — e.g. clear it and redirect. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  unauthorizedHandler = handler;
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url: string = error?.config?.url ?? '';
    // A 401 on the login call is just bad credentials — let the form handle it.
    const isLoginCall = url.endsWith('/api/auth/login');
    if (status === 401 && !isLoginCall) {
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  }
);
