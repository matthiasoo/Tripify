import CitySlider from "@/components/CitySlider/CitySlider";

const CITIES = ["Warsaw", "Porto", "Dubai", "Vienna", "New York"];

export default function Home() {
    return (
        <div className="flex flex-col items-center gap-12 py-16 w-full px-4">
            <div className="flex flex-col items-center gap-2">
                <h1 className="text-5xl font-bold tracking-tight">Tripify</h1>
                <span className="font-mono font-light text-muted">
                    Travels made simple.
                </span>
            </div>

            <CitySlider cities={CITIES} />
        </div>
    );
}
