"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { FiAlertCircle, FiLayers, FiMapPin, FiSave, FiSearch } from "react-icons/fi";
import CitySlider from "@/components/CitySlider/CitySlider";
import { authService, cityImageService, tripService } from "@/lib/api";

const CITIES = ["Warsaw", "Porto", "Dubai", "Vienna", "New York"];

export default function Home() {
    const [cityInput, setCityInput] = useState("");
    const [days, setDays] = useState(3);
    const [pace, setPace] = useState("relaxed");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [tripPlan, setTripPlan] = useState(null);
    const [cityImageUrl, setCityImageUrl] = useState(null);
    const [user, setUser] = useState(null);
    const [checkingSession, setCheckingSession] = useState(true);
    const [savingPlan, setSavingPlan] = useState(false);
    const [saveMessage, setSaveMessage] = useState("");
    const [savedPlanId, setSavedPlanId] = useState(null);
    const resultsRef = useRef(null);

    useEffect(() => {
        let cancelled = false;

        async function loadUser() {
            try {
                const currentUser = await authService.me();
                if (!cancelled) {
                    setUser(currentUser);
                }
            } catch {
                if (!cancelled) {
                    setUser(null);
                }
            } finally {
                if (!cancelled) {
                    setCheckingSession(false);
                }
            }
        }

        loadUser();
        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        function handleUserUpdate(event) {
            setUser(event.detail);
        }

        window.addEventListener("tripify:user-updated", handleUserUpdate);
        return () => {
            window.removeEventListener("tripify:user-updated", handleUserUpdate);
        };
    }, []);

    useEffect(() => {
        if (tripPlan && resultsRef.current) {
            resultsRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    }, [tripPlan]);

    async function handlePlanTrip(city) {
        if (!city || city.trim() === "") return;
        if (!user) {
            setError("Zaloguj się, aby generować i zapisywać plany podróży.");
            return;
        }

        try {
            setLoading(true);
            setError(null);
            setSaveMessage("");
            setSavedPlanId(null);

            const [planData, imageData] = await Promise.all([
                tripService.planTrip(city, days, pace),
                cityImageService.search({ city, perPage: 1 }).catch(() => null),
            ]);

            setTripPlan(planData);
            setCityImageUrl(imageData?.photos?.[0]?.urls?.regular || null);
        } catch (err) {
            console.error("Error generating trip plan:", err);
            setError(err.message || "Wystąpił nieoczekiwany błąd przy generowaniu planu.");
        } finally {
            setLoading(false);
        }
    }

    async function handleSavePlan() {
        if (!tripPlan || savedPlanId) return;

        try {
            setSavingPlan(true);
            setSaveMessage("");
            const savedPlan = await tripService.savePlan(tripPlan);
            setSavedPlanId(savedPlan.id);
            setSaveMessage("Plan został zapisany w profilu.");
            window.dispatchEvent(new CustomEvent("tripify:trip-plan-saved"));
        } catch (err) {
            setSaveMessage(err.message || "Nie udało się zapisać planu.");
        } finally {
            setSavingPlan(false);
        }
    }

    function handleSearchSubmit(event) {
        event.preventDefault();
        handlePlanTrip(cityInput);
    }

    function renderInlineFormatting(text) {
        const parts = text.split(/(\*\*.*?\*\*)/g);
        return parts.map((part, index) => {
            if (part.startsWith("**") && part.endsWith("**")) {
                return <strong key={index} className="font-bold text-primary">{part.slice(2, -2)}</strong>;
            }
            return part;
        });
    }

    function parseMarkdown(markdown) {
        if (!markdown) return null;
        return markdown.split("\n").map((line, index) => {
            const trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return <h2 key={index} className="my-6 border-b border-outline pb-2 text-3xl font-extrabold tracking-tight text-primary">{trimmed.slice(2)}</h2>;
            }
            if (trimmed.startsWith("## ")) {
                return <h3 key={index} className="my-4 mt-8 text-2xl font-bold tracking-tight text-primary">{trimmed.slice(3)}</h3>;
            }
            if (trimmed.startsWith("### ")) {
                return <h4 key={index} className="my-3 mt-6 flex items-center gap-2 text-xl font-bold tracking-tight text-primary">{trimmed.slice(4)}</h4>;
            }
            if (trimmed.startsWith("#### ")) {
                return <h5 key={index} className="my-2 mt-4 text-lg font-semibold tracking-tight text-primary">{trimmed.slice(5)}</h5>;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                return (
                    <li key={index} className="my-2 ml-6 list-disc leading-relaxed text-muted">
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
                <p key={index} className="my-2.5 leading-relaxed text-muted">
                    {renderInlineFormatting(trimmed)}
                </p>
            );
        });
    }

    return (
        <div className="mx-auto flex w-full max-w-4xl flex-col items-center gap-12 px-4 py-16">
            <div className="flex flex-col items-center gap-3">
                <h1 className="bg-gradient-to-r from-primary to-[var(--color-glow)] bg-clip-text text-6xl font-extrabold tracking-tight text-transparent">
                    Tripify
                </h1>
                <span className="mt-3 font-mono text-sm font-light text-muted">
                    Travels made simple.
                </span>
            </div>

            <div className="flex w-full flex-col items-center gap-2">
                <span className="mb-2 text-xs font-mono uppercase tracking-wider text-muted">
                    Kliknij kartę, aby zaplanować podróż
                </span>
                <CitySlider cities={CITIES} onSelectCity={handlePlanTrip} />
            </div>

            <form onSubmit={handleSearchSubmit} className="flex w-full max-w-2xl animate-fade-in flex-col gap-4">
                <div className="flex w-full items-center gap-2">
                    <div className="relative flex-1">
                        <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-lg text-muted" />
                        <input
                            type="text"
                            placeholder="Wpisz dowolne miasto na świecie..."
                            value={cityInput}
                            onChange={(event) => setCityInput(event.target.value)}
                            className="w-full rounded-xl border border-outline bg-panel py-3 pl-12 pr-4 text-primary shadow-panel outline-none transition-all duration-300 placeholder:text-muted focus:border-[var(--color-glow)] focus:shadow-hover"
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={checkingSession || loading || !cityInput.trim() || !user}
                        className="cursor-pointer rounded-xl bg-[var(--color-glow)] px-6 py-3 font-semibold text-[var(--color-main)] shadow-panel transition-all duration-250 ease-out hover:scale-105 active:scale-95 disabled:pointer-events-none disabled:opacity-50"
                    >
                        Zaplanuj
                    </button>
                </div>

                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-center">
                    <div className="flex items-center gap-2">
                        <label htmlFor="days-select" className="text-sm font-medium text-muted">Długość pobytu:</label>
                        <select
                            id="days-select"
                            value={days}
                            onChange={(e) => setDays(Number(e.target.value))}
                            className="rounded-xl border border-outline bg-panel px-4 py-2 text-sm text-primary shadow-panel outline-none transition-all duration-300 focus:border-[var(--color-glow)]"
                        >
                            {[1, 2, 3, 4, 5, 6, 7, 10, 14].map((d) => (
                                <option key={d} value={d}>
                                    {d} {d === 1 ? "dzień" : d < 5 ? "dni" : "dni"}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="flex items-center gap-2">
                        <label htmlFor="pace-select" className="text-sm font-medium text-muted">Tempo wycieczki:</label>
                        <select
                            id="pace-select"
                            value={pace}
                            onChange={(e) => setPace(e.target.value)}
                            className="rounded-xl border border-outline bg-panel px-4 py-2 text-sm text-primary shadow-panel outline-none transition-all duration-300 focus:border-[var(--color-glow)]"
                        >
                            <option value="relaxed">Luźne / Spokojne</option>
                            <option value="intense">Intensywne / Aktywne</option>
                        </select>
                    </div>
                </div>
            </form>

            {!checkingSession && !user && (
                <div className="w-full max-w-md rounded-2xl border border-outline bg-panel px-5 py-4 text-sm text-muted shadow-panel">
                    Zaloguj się, aby generować plany podróży. Po wygenerowaniu możesz zapisać plan w swoim profilu.
                </div>
            )}

            {loading && (
                <div className="flex w-full animate-scale-up flex-col items-center gap-4 rounded-2xl border border-outline bg-panel p-8">
                    <div className="relative h-16 w-16">
                        <div className="absolute inset-0 rounded-full border-4 border-outline" />
                        <div className="absolute inset-0 animate-spin rounded-full border-4 border-t-[var(--color-glow)]" />
                    </div>
                    <span className="animate-pulse font-mono text-sm text-muted">
                        Pobieranie danych pogodowych i atrakcji...
                    </span>
                </div>
            )}

            {error && (
                <div className="flex w-full animate-scale-up items-center gap-4 rounded-2xl border border-danger-outline bg-danger-panel p-6 text-danger">
                    <FiAlertCircle className="shrink-0 text-2xl" />
                    <div className="flex flex-col gap-0.5">
                        <span className="font-bold">Błąd generowania planu</span>
                        <span className="text-sm opacity-90">{error}</span>
                    </div>
                </div>
            )}

            {tripPlan && !loading && (
                <div ref={resultsRef} className="flex w-full animate-scale-up flex-col gap-6">
                    <div className="relative h-[240px] w-full overflow-hidden rounded-3xl border border-white/[0.08] shadow-panel">
                        {cityImageUrl ? (
                            <Image
                                src={cityImageUrl}
                                alt={tripPlan.city}
                                fill
                                className="object-cover brightness-[0.55]"
                                priority
                            />
                        ) : (
                            <div className="h-full w-full bg-gradient-to-r from-[var(--color-glow-gradient)] to-panel" />
                        )}
                        <div className="absolute inset-0 bg-gradient-to-t from-panel via-panel/40 to-transparent" />

                        <div className="absolute bottom-6 left-6 right-6 flex items-end justify-between">
                            <div className="flex flex-col gap-1">
                                <span className="text-xs font-mono font-semibold uppercase tracking-widest text-[var(--color-glow)]">
                                    Twój plan podróży
                                </span>
                                <h2 className="text-4xl font-extrabold capitalize leading-none text-white drop-shadow-md">
                                    {tripPlan.city}
                                </h2>
                            </div>

                            <div className="flex items-center gap-3 rounded-2xl border border-outline bg-panel/85 px-4 py-2 shadow-panel backdrop-blur">
                                <div className="flex flex-col items-end">
                                    <span className="text-lg font-bold leading-none text-primary">
                                        {Math.round(tripPlan.weather.temperature)}°C
                                    </span>
                                    <span className="mt-1 text-xs font-medium capitalize text-muted">
                                        {tripPlan.weather.description}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
                        <div className="flex flex-col gap-4 text-left md:col-span-1">
                            <div className="flex items-center gap-2 border-b border-outline pb-2 text-lg font-bold text-primary">
                                <FiMapPin className="text-[var(--color-glow)]" />
                                <span>Atrakcje (Foursquare)</span>
                            </div>

                            <div className="flex flex-col gap-3">
                                {tripPlan.places && tripPlan.places.length > 0 ? (
                                    tripPlan.places.map((place, index) => (
                                        <div key={`${place.name}-${index}`} className="flex flex-col gap-1 rounded-2xl border border-outline bg-panel p-4 shadow-panel transition-all hover:border-[var(--color-glow)]">
                                            <span className="text-sm font-bold leading-tight text-primary">{place.name}</span>
                                            <span className="mt-1 self-start rounded-md bg-outline px-2 py-0.5 font-mono text-xs capitalize text-[var(--color-glow)]">{place.category}</span>
                                            <span className="mt-2 text-xs leading-tight text-muted">{place.address}</span>
                                        </div>
                                    ))
                                ) : (
                                    <div className="rounded-2xl border border-outline bg-panel p-4 text-center text-sm text-muted">
                                        Brak znalezionych atrakcji w pobliżu.
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="flex flex-col rounded-3xl border border-outline bg-panel p-6 text-left shadow-panel md:col-span-2 md:p-8">
                            <div className="mb-6 flex flex-col gap-4 border-b border-outline pb-4 sm:flex-row sm:items-center sm:justify-between">
                                <div className="flex items-center gap-2 text-lg font-bold text-primary">
                                    <FiLayers className="text-[var(--color-glow)]" />
                                    <span>Wygenerowany plan</span>
                                </div>
                                <button
                                    type="button"
                                    onClick={handleSavePlan}
                                    disabled={savingPlan || Boolean(savedPlanId)}
                                    className="flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    <FiSave />
                                    {savedPlanId ? "Zapisano" : savingPlan ? "Zapisywanie..." : "Zapisz plan"}
                                </button>
                            </div>

                            {saveMessage && (
                                <p className="mb-4 rounded-md border border-outline bg-main px-3 py-2 text-sm text-muted">
                                    {saveMessage}
                                </p>
                            )}

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
