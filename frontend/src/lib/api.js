const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

const AUTH_TOKEN_KEY = "tripify_auth_token";

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

async function request(path, { params = {}, method = "GET", body, auth = false } = {}) {
  const url = new URL(`${API_BASE_URL}${path}`);

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

export const authService = {
  register(payload) {
    return request("/api/v1/auth/register", {
      method: "POST",
      body: payload,
    });
  },

  login(payload) {
    return request("/api/v1/auth/login", {
      method: "POST",
      body: payload,
    });
  },

  me() {
    return request("/api/v1/auth/me", { auth: true });
  },

  updateProfile(payload) {
    return request("/api/v1/auth/profile", {
      method: "PUT",
      body: payload,
      auth: true,
    });
  },

  changePassword(payload) {
    return request("/api/v1/auth/password", {
      method: "PUT",
      body: payload,
      auth: true,
    });
  },

  logout() {
    return request("/api/v1/auth/logout", {
      method: "POST",
      auth: true,
    });
  },
};

export const tripService = {
  planTrip(city) {
    return request(`/api/v1/trips/plan/${encodeURIComponent(city)}`, { auth: true });
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

  deleteSavedPlan(planId) {
    return request(`/api/v1/trips/mine/${planId}`, {
      method: "DELETE",
      auth: true,
    });
  },
};
