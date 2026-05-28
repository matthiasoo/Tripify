"use client";

import { useState, useRef, useEffect } from "react";
import CitySlider from "@/components/CitySlider/CitySlider";
import { tripService, cityImageService } from "@/lib/api";
import Image from "next/image";
import { FiSearch, FiMapPin, FiLayers, FiAlertCircle } from "react-icons/fi";

const CITIES = ["Warsaw", "Porto", "Dubai", "Vienna", "New York"];

export default function Home() {
    const [cityInput, setCityInput] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [tripPlan, setTripPlan] = useState(null);
    const [cityImageUrl, setCityImageUrl] = useState(null);
    
    const resultsRef = useRef(null);

    const handlePlanTrip = async (city) => {
        if (!city || city.trim() === "") return;
        
        try {
            setLoading(true);
            setError(null);
            
            const [planData, imageData] = await Promise.all([
                tripService.planTrip(city),
                cityImageService.search({ city, perPage: 1 }).catch(() => null)
            ]);

            setTripPlan(planData);
            if (imageData && imageData.photos?.[0]?.urls?.regular) {
                setCityImageUrl(imageData.photos[0].urls.regular);
            } else {
                setCityImageUrl(null);
            }
        } catch (err) {
            console.error("Error generating trip plan:", err);
            setError(err.message || "Wystąpił nieoczekiwany błąd przy generowaniu planu.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (tripPlan && resultsRef.current) {
            resultsRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }, [tripPlan]);

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        handlePlanTrip(cityInput);
    };

    const renderInlineFormatting = (text) => {
        const parts = text.split(/(\*\*.*?\*\*)/g);
        return parts.map((part, i) => {
            if (part.startsWith("**") && part.endsWith("**")) {
                return <strong key={i} className="font-bold text-primary">{part.slice(2, -2)}</strong>;
            }
            return part;
        });
    };

    const parseMarkdown = (md) => {
        if (!md) return null;
        return md.split("\n").map((line, index) => {
            const trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return <h2 key={index} className="text-3xl font-extrabold my-6 text-primary border-b border-outline pb-2 tracking-tight">{trimmed.slice(2)}</h2>;
            }
            if (trimmed.startsWith("## ")) {
                return <h3 key={index} className="text-2xl font-bold my-4 text-primary mt-8 tracking-tight">{trimmed.slice(3)}</h3>;
            }
            if (trimmed.startsWith("### ")) {
                return <h4 key={index} className="text-xl font-bold my-3 text-primary mt-6 tracking-tight flex items-center gap-2">{trimmed.slice(4)}</h4>;
            }
            if (trimmed.startsWith("#### ")) {
                return <h5 key={index} className="text-lg font-semibold my-2 text-primary mt-4 tracking-tight">{trimmed.slice(5)}</h5>;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                return (
                    <li key={index} className="ml-6 list-disc my-2 text-muted leading-relaxed">
                        {renderInlineFormatting(trimmed.slice(2))}
                    </li>
                );
            }
            if (trimmed === "---") {
                return <hr key={index} className="my-6 border-outline" />;
            }
            if (trimmed === "") {
                return <div key={index} className="h-2" />;
            }
            return (
                <p key={index} className="my-2.5 text-muted leading-relaxed">
                    {renderInlineFormatting(trimmed)}
                </p>
            );
        });
    };

    return (
        <div className="flex flex-col items-center gap-12 py-16 w-full px-4 max-w-4xl mx-auto">
            {/* Header */}
            <div className="flex flex-col items-center gap-3">
                <h1 className="text-6xl font-extrabold tracking-tight bg-gradient-to-r from-primary to-[var(--color-glow)] bg-clip-text text-transparent">Tripify</h1>
                <span className="font-mono font-light text-muted text-sm">
                    Travels made simple.
                </span>
            </div>

            {/* Slider */}
            <div className="flex flex-col items-center gap-2 w-full">
                <span className="text-xs font-mono uppercase tracking-wider text-muted mb-2">Kliknij kartę, aby zaplanować podróż</span>
                <CitySlider cities={CITIES} onSelectCity={handlePlanTrip} />
            </div>

            {/* Custom Search Input */}
            <form onSubmit={handleSearchSubmit} className="w-full max-w-md flex items-center gap-2 animate-fade-in">
                <div className="relative flex-1">
                    <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-muted text-lg" />
                    <input
                        type="text"
                        placeholder="Wpisz dowolne miasto na świecie..."
                        value={cityInput}
                        onChange={(e) => setCityInput(e.target.value)}
                        className="w-full pl-12 pr-4 py-3 rounded-xl border border-outline bg-panel text-primary placeholder-muted
                                   outline-none shadow-panel transition-all duration-300
                                   focus:border-[var(--color-glow)] focus:shadow-hover"
                    />
                </div>
                <button
                    type="submit"
                    disabled={loading || !cityInput.trim()}
                    className="px-6 py-3 rounded-xl bg-[var(--color-glow)] text-[var(--color-main)] font-semibold shadow-panel
                               transition-all duration-250 ease-out hover:scale-105 active:scale-95 cursor-pointer
                               disabled:opacity-50 disabled:pointer-events-none"
                >
                    Zaplanuj
                </button>
            </form>

            {/* Loading Indicator */}
            {loading && (
                <div className="w-full rounded-2xl bg-panel border border-outline p-8 flex flex-col items-center gap-4 animate-scale-up">
                    <div className="relative w-16 h-16">
                        <div className="absolute inset-0 rounded-full border-4 border-outline"></div>
                        <div className="absolute inset-0 rounded-full border-4 border-t-[var(--color-glow)] animate-spin"></div>
                    </div>
                    <span className="text-muted text-sm font-mono animate-pulse">
                        Pobieranie danych pogodowych i atrakcji...
                    </span>
                </div>
            )}

            {/* Error Message */}
            {error && (
                <div className="w-full rounded-2xl bg-danger-panel border border-danger-outline p-6 flex items-center gap-4 text-danger animate-scale-up">
                    <FiAlertCircle className="text-2xl shrink-0" />
                    <div className="flex flex-col gap-0.5">
                        <span className="font-bold">Błąd generowania planu</span>
                        <span className="text-sm opacity-90">{error}</span>
                    </div>
                </div>
            )}

            {/* Trip Plan Results */}
            {tripPlan && !loading && (
                <div ref={resultsRef} className="w-full flex flex-col gap-6 animate-scale-up">
                    {/* Hero section */}
                    <div className="relative w-full h-[240px] rounded-3xl overflow-hidden border border-white/[0.08] shadow-panel">
                        {cityImageUrl ? (
                            <Image
                                src={cityImageUrl}
                                alt={tripPlan.city}
                                fill
                                className="object-cover brightness-[0.55]"
                                priority
                            />
                        ) : (
                            <div className="w-full h-full bg-gradient-to-r from-[var(--color-glow-gradient)] to-panel" />
                        )}
                        <div className="absolute inset-0 bg-gradient-to-t from-panel via-panel/40 to-transparent" />
                        
                        <div className="absolute bottom-6 left-6 right-6 flex justify-between items-end">
                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-mono uppercase tracking-widest text-[var(--color-glow)] font-semibold">Twój plan podróży</span>
                                <h2 className="text-4xl font-extrabold text-white leading-none drop-shadow-md capitalize">
                                    {tripPlan.city}
                                </h2>
                            </div>
                            
                            {/* Weather indicator on hero */}
                            <div className="flex items-center gap-3 bg-panel/85 backdrop-blur border border-outline px-4 py-2 rounded-2xl shadow-panel">
                                <div className="flex flex-col items-end">
                                    <span className="text-lg font-bold leading-none text-primary">{Math.round(tripPlan.weather.temperature)}°C</span>
                                    <span className="text-xs text-muted font-medium capitalize mt-1">{tripPlan.weather.description}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Content Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        {/* Attractions list */}
                        <div className="md:col-span-1 flex flex-col gap-4 text-left">
                            <div className="flex items-center gap-2 text-primary font-bold text-lg border-b border-outline pb-2">
                                <FiMapPin className="text-[var(--color-glow)]" />
                                <span>Atrakcje (Foursquare)</span>
                            </div>
                            
                            <div className="flex flex-col gap-3">
                                {tripPlan.places && tripPlan.places.length > 0 ? (
                                    tripPlan.places.map((place, idx) => (
                                        <div key={idx} className="bg-panel border border-outline rounded-2xl p-4 flex flex-col gap-1 shadow-panel transition-all hover:border-[var(--color-glow)]">
                                            <span className="font-bold text-sm text-primary leading-tight">{place.name}</span>
                                            <span className="text-xs font-mono text-[var(--color-glow)] bg-outline px-2 py-0.5 rounded-md self-start mt-1 capitalize">{place.category}</span>
                                            <span className="text-xs text-muted mt-2 leading-tight">{place.address}</span>
                                        </div>
                                    ))
                                ) : (
                                    <div className="bg-panel border border-outline rounded-2xl p-4 text-center text-muted text-sm">
                                        Brak znalezionych atrakcji w pobliżu.
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Itinerary details */}
                        <div className="md:col-span-2 bg-panel border border-outline rounded-3xl p-6 md:p-8 shadow-panel text-left flex flex-col">
                            <div className="flex items-center gap-2 text-primary font-bold text-lg border-b border-outline pb-2 mb-6">
                                <FiLayers className="text-[var(--color-glow)]" />
                                <span>Wygenerowany Plan</span>
                            </div>
                            
                            <div className="prose prose-sm prose-invert max-w-none">
                                {parseMarkdown(tripPlan.plan)}
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
