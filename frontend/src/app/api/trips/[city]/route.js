export async function GET(request, { params }) {
  const resolvedParams = await params;
  const city = resolvedParams.city;
  const backendUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081";

  if (!city) {
    return Response.json(
      { error: "The 'city' parameter is required." },
      { status: 400 }
    );
  }

  try {
    const url = `${backendUrl}/api/v1/trips/plan/${encodeURIComponent(city)}`;
    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    const data = await response.json();

    if (!response.ok) {
      return Response.json(
        { error: data.message || "Failed to fetch trip plan from backend" },
        { status: response.status }
      );
    }

    return Response.json(data);
  } catch (error) {
    console.error("[TripPlanProxyAPI]", error);
    return Response.json(
      { error: "Failed to connect to backend trip planning service." },
      { status: 502 }
    );
  }
}
