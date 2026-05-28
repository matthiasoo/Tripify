import CitySlider from "@/components/CitySlider/CitySlider";
import AuthPanel from "@/components/AuthPanel/AuthPanel";

const CITIES = ["Warsaw", "Porto", "Dubai", "Vienna", "New York"];

export default function Home() {
    return (
        <div className="flex min-h-screen w-full flex-col items-center">
            <AuthPanel />

            <main className="flex w-full flex-1 flex-col items-center gap-12 px-4 py-16">
                <div className="flex flex-col items-center gap-2">
                    <h1 className="text-5xl font-bold tracking-tight">Tripify</h1>
                    <span className="font-mono font-light text-muted">
                        Travels made simple.
                    </span>
                </div>

                <CitySlider cities={CITIES} />
            </main>
        </div>
    );
}
