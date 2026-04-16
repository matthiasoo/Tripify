const OPENWEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5";

export async function GET(request) {
  const apiKey = process.env.OPENWEATHER_API_KEY;

  if (!apiKey) {
    return Response.json(
      { error: "OpenWeather API key is not configured on the server." },
      { status: 500 }
    );
  }

  const { searchParams } = new URL(request.url);

  const type = searchParams.get("type") || "current";
  const city = searchParams.get("city");
  const lat = searchParams.get("lat");
  const lon = searchParams.get("lon");
  const units = searchParams.get("units") || "metric";
  const lang = searchParams.get("lang") || "en";

  const endpoint = type === "forecast" ? "forecast" : "weather";
  const owParams = new URLSearchParams({
    appid: apiKey,
    units,
    lang,
  });

  if (lat && lon) {
    owParams.set("lat", lat);
    owParams.set("lon", lon);
  } else if (city) {
    owParams.set("q", city);
  } else {
    return Response.json(
      { error: "Provide either 'city' or both 'lat' and 'lon' parameters." },
      { status: 400 }
    );
  }

  const url = `${OPENWEATHER_BASE_URL}/${endpoint}?${owParams.toString()}`;

  try {
    const response = await fetch(url, { next: { revalidate: 300 } });
    const data = await response.json();

    if (!response.ok) {
      return Response.json(
        { error: data.message || "OpenWeather API error", code: data.cod },
        { status: response.status }
      );
    }

    return Response.json(data);
  } catch (error) {
    console.error("[WeatherAPI]", error);
    return Response.json(
      { error: "Failed to fetch weather data." },
      { status: 502 }
    );
  }
}
