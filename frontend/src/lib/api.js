const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

async function request(path, params = {}) {
  const url = new URL(`${API_BASE_URL}${path}`);

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
    return request("/api/v1/weather", { ...query, type: "current" });
  },

  getForecast(query) {
    return request("/api/v1/weather", { ...query, type: "forecast" });
  },
};

export const cityImageService = {
  search({ city, page, perPage, orientation }) {
    return request("/api/v1/city-images", {
      city,
      page,
      per_page: perPage,
      orientation,
    });
  },
};
