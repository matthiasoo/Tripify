const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

const AUTH_BASE_URL =
  process.env.NEXT_PUBLIC_AUTH_URL || "http://localhost:9000";

const OAUTH_CLIENT_ID = "tripify-web";
const OAUTH_SCOPE = "openid profile";

const AUTH_TOKEN_KEY = "tripify_auth_token";
const PKCE_VERIFIER_KEY = "tripify_pkce_verifier";
const OAUTH_STATE_KEY = "tripify_oauth_state";

function getStoredToken() {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(AUTH_TOKEN_KEY);
}

export function setStoredToken(token) {
  if (typeof window === "undefined") {
    return;
  }

  if (token) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token);
  } else {
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
  }
}

async function request(path, { params = {}, method = "GET", body, auth = false, base = API_BASE_URL } = {}) {
  const url = new URL(`${base}${path}`);

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, String(value));
    }
  });

  const headers = {};
  if (body) {
    headers["Content-Type"] = "application/json";
  }

  if (auth) {
    const token = getStoredToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const contentType = response.headers.get("content-type") || "";
  const data = response.status === 204
    ? null
    : contentType.includes("application/json")
      ? await response.json()
      : { error: await response.text() };

  if (!response.ok) {
    throw new Error(data?.error || `Request failed (${response.status})`);
  }

  return data;
}

/* ----------------------------- PKCE / OAuth2 ----------------------------- */

function base64UrlEncode(bytes) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function randomString(length = 64) {
  const array = new Uint8Array(length);
  window.crypto.getRandomValues(array);
  return base64UrlEncode(array).slice(0, length);
}

async function pkceChallenge(verifier) {
  const digest = await window.crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(digest));
}

function redirectUri() {
  return `${window.location.origin}/callback`;
}

export const authService = {
  /**
   * Starts the Authorization Code + PKCE flow by redirecting the browser to the
   * authorization server's login page.
   */
  async login() {
    const verifier = randomString();
    const state = randomString(32);
    const challenge = await pkceChallenge(verifier);

    window.sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
    window.sessionStorage.setItem(OAUTH_STATE_KEY, state);

    const authorizeUrl = new URL(`${AUTH_BASE_URL}/oauth2/authorize`);
    authorizeUrl.searchParams.set("response_type", "code");
    authorizeUrl.searchParams.set("client_id", OAUTH_CLIENT_ID);
    authorizeUrl.searchParams.set("scope", OAUTH_SCOPE);
    authorizeUrl.searchParams.set("redirect_uri", redirectUri());
    authorizeUrl.searchParams.set("code_challenge", challenge);
    authorizeUrl.searchParams.set("code_challenge_method", "S256");
    authorizeUrl.searchParams.set("state", state);

    window.location.assign(authorizeUrl.toString());
  },

  /** Sends the user to the authorization server's registration page. */
  register() {
    window.location.assign(`${AUTH_BASE_URL}/register`);
  },

  /**
   * Exchanges the authorization code for an access token. Called from /callback.
   * Returns the access token on success.
   */
  async completeLogin(code, state) {
    const expectedState = window.sessionStorage.getItem(OAUTH_STATE_KEY);
    const verifier = window.sessionStorage.getItem(PKCE_VERIFIER_KEY);

    if (!verifier || !state || state !== expectedState) {
      throw new Error("Nieprawidłowy stan logowania. Spróbuj ponownie.");
    }

    const params = new URLSearchParams();
    params.set("grant_type", "authorization_code");
    params.set("code", code);
    params.set("redirect_uri", redirectUri());
    params.set("client_id", OAUTH_CLIENT_ID);
    params.set("code_verifier", verifier);

    const response = await fetch(`${AUTH_BASE_URL}/oauth2/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: params.toString(),
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data?.error_description || data?.error || "Nie udało się dokończyć logowania.");
    }

    window.sessionStorage.removeItem(PKCE_VERIFIER_KEY);
    window.sessionStorage.removeItem(OAUTH_STATE_KEY);
    setStoredToken(data.access_token);
    return data.access_token;
  },

  me() {
    return request("/api/v1/account/me", { auth: true, base: AUTH_BASE_URL });
  },

  updateProfile(payload) {
    return request("/api/v1/account/profile", {
      method: "PUT",
      body: payload,
      auth: true,
      base: AUTH_BASE_URL,
    });
  },

  changePassword(payload) {
    return request("/api/v1/account/password", {
      method: "PUT",
      body: payload,
      auth: true,
      base: AUTH_BASE_URL,
    });
  },

  logout() {
    setStoredToken(null);
  },
};

export const weatherService = {
  getCurrent(query) {
    return request("/api/v1/weather", { params: { ...query, type: "current" } });
  },

  getForecast(query) {
    return request("/api/v1/weather", { params: { ...query, type: "forecast" } });
  },
};

export const cityImageService = {
  search({ city, page, perPage, orientation }) {
    return request("/api/v1/city-images", {
      params: {
        city,
        page,
        per_page: perPage,
        orientation,
      },
    });
  },
};

export const tripService = {
  planTrip(city, days, pace) {
    return request(`/api/v1/trips/plan/${encodeURIComponent(city)}`, {
      auth: true,
      params: { days, pace }
    });
  },

  getSavedPlans() {
    return request("/api/v1/trips/mine", { auth: true });
  },

  savePlan(plan) {
    return request("/api/v1/trips/mine", {
      method: "POST",
      body: plan,
      auth: true,
    });
  },

  regeneratePlan(planId, { days, pace }) {
    return request(`/api/v1/trips/mine/${planId}/regenerate`, {
      method: "POST",
      auth: true,
      params: { days, pace },
    });
  },

  deleteSavedPlan(planId) {
    return request(`/api/v1/trips/mine/${planId}`, {
      method: "DELETE",
      auth: true,
    });
  },
};
