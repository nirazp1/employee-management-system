import type { ApiResponse, ErrorResponse, LoginResponse, PagedResponse } from "./types";
import { tokenStorage } from "./tokenStorage";

// Same-origin "/api/v1" by default - the Vite dev proxy (see vite.config.ts)
// forwards that to the backend, so we never have to deal with CORS locally.
// In production, point this at wherever the backend actually lives.
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

export class ApiError extends Error {
  code: string;
  status: number;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

/** Thrown when a request fails auth even after a refresh attempt - callers
 * (AuthContext) use this specifically to know "log the user out now". */
export class SessionExpiredError extends ApiError {
  constructor() {
    super(401, "SESSION_EXPIRED", "Your session has expired. Please log in again.");
  }
}

export function isApiError(err: unknown): err is ApiError {
  return err instanceof ApiError;
}

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
  /** Internal: set on the retry attempt after a token refresh, to stop infinite loops. */
  _isRetry?: boolean;
};

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = new URL(API_BASE + path, window.location.origin);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.pathname + url.search;
}

// Multiple components can 401 around the same time (e.g. a page that fires
// several requests on mount); without this they'd each kick off their own
// refresh call and race to overwrite the token pair. Sharing one in-flight
// promise means only the first 401 actually calls /auth/refresh - everyone
// else just waits on it.
let refreshInFlight: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new SessionExpiredError();
  }

  refreshInFlight = fetch(buildUrl("/auth/refresh"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (res) => {
      if (!res.ok) {
        throw new SessionExpiredError();
      }
      const body = (await res.json()) as ApiResponse<LoginResponse>;
      tokenStorage.setTokens(body.data.accessToken, body.data.refreshToken);
      return body.data.accessToken;
    })
    .finally(() => {
      refreshInFlight = null;
    });

  return refreshInFlight;
}

async function rawRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const accessToken = tokenStorage.getAccessToken();
  const headers: Record<string, string> = {};
  if (accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`;
  }
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(buildUrl(path, options.query), {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  // Auth endpoints intentionally excluded from the retry-after-refresh dance
  // below - a 401 on /auth/login just means "wrong password," not "expired
  // session," and refreshing there would be nonsensical (and could loop).
  const isAuthEndpoint = path.startsWith("/auth/");

  if (response.status === 401 && !isAuthEndpoint && !options._isRetry) {
    await refreshAccessToken();
    return rawRequest<T>(path, { ...options, _isRetry: true });
  }

  if (!response.ok) {
    let errorBody: ErrorResponse | null = null;
    try {
      errorBody = await response.json();
    } catch {
      // Response wasn't JSON (network-level failure, proxy error page, etc.)
    }
    throw new ApiError(
      response.status,
      errorBody?.error?.code ?? "UNKNOWN_ERROR",
      errorBody?.error?.message ?? `Request failed with status ${response.status}`,
    );
  }

  return response.json() as Promise<T>;
}

/** For endpoints that return the standard { success, data, message } envelope. */
async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const body = await rawRequest<ApiResponse<T>>(path, options);
  return body.data;
}

/** For paginated list endpoints - returns the envelope as-is (data + pagination),
 * matching the backend's PagedResponse shape 1:1 rather than reshaping it. */
async function apiRequestPaged<T>(
  path: string,
  query?: RequestOptions["query"],
): Promise<PagedResponse<T>> {
  return rawRequest<PagedResponse<T>>(path, { method: "GET", query });
}

export const api = {
  get: <T>(path: string, query?: RequestOptions["query"]) =>
    apiRequest<T>(path, { method: "GET", query }),
  post: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: "POST", body }),
  put: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: "PUT", body }),
  patch: <T>(path: string, body?: unknown) => apiRequest<T>(path, { method: "PATCH", body }),
  delete: <T>(path: string) => apiRequest<T>(path, { method: "DELETE" }),
  getPaged: apiRequestPaged,
};
