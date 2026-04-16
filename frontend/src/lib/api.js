const API_BASE_URL = "/api";

async function request(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, String(value));
    }
  });

  const response = await fetch(url.toString());
  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`);
  }

  return data;
}

export const weatherService = {
  getCurrent(query) {
    return request("/weather", { ...query, type: "current" });
  },

  getForecast(query) {
    return request("/weather", { ...query, type: "forecast" });
  },
};

export const cityImageService = {
  search({ city, page, perPage, orientation }) {
    return request("/city-images", {
      city,
      page,
      per_page: perPage,
      orientation,
    });
  },
};
