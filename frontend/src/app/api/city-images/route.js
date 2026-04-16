const UNSPLASH_BASE_URL = "https://api.unsplash.com";

export async function GET(request) {
  const accessKey = process.env.UNSPLASH_ACCESS_KEY;

  if (!accessKey) {
    return Response.json(
      { error: "Unsplash access key is not configured on the server." },
      { status: 500 }
    );
  }

  const { searchParams } = new URL(request.url);

  const city = searchParams.get("city");
  const page = searchParams.get("page") || "1";
  const perPage = searchParams.get("per_page") || "10";
  const orientation = searchParams.get("orientation") || "landscape";

  if (!city) {
    return Response.json(
      { error: "The 'city' parameter is required." },
      { status: 400 }
    );
  }

  const unsplashParams = new URLSearchParams({
    query: `${city} city`,
    page,
    per_page: perPage,
    orientation,
    content_filter: "high",
    order_by: "relevant",
  });

  const url = `${UNSPLASH_BASE_URL}/search/photos?${unsplashParams.toString()}`;

  try {
    const response = await fetch(url, {
      headers: {
        Authorization: `Client-ID ${accessKey}`,
        "Accept-Version": "v1",
      },
      next: { revalidate: 3600 },
    });

    const data = await response.json();

    if (!response.ok) {
      return Response.json(
        { error: data.errors?.[0] || "Unsplash API error", code: response.status },
        { status: response.status }
      );
    }

    const photos = data.results.map((photo) => ({
      id: photo.id,
      description: photo.description || photo.alt_description,
      urls: {
        raw: photo.urls.raw,
        regular: photo.urls.regular,
        small: photo.urls.small,
        thumb: photo.urls.thumb,
      },
      author: {
        name: photo.user.name,
        username: photo.user.username,
        profileUrl: photo.user.links.html,
      },
      blurHash: photo.blur_hash,
      color: photo.color,
      width: photo.width,
      height: photo.height,
    }));

    return Response.json({
      total: data.total,
      totalPages: data.total_pages,
      photos,
    });
  } catch (error) {
    console.error("[CityImagesAPI]", error);
    return Response.json(
      { error: "Failed to fetch city images." },
      { status: 502 }
    );
  }
}
