"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { FiChevronLeft, FiChevronRight } from "react-icons/fi";
import WeatherCard from "@/components/WeatherCard/WeatherCard";

const AUTO_SCROLL_INTERVAL = 5000;

export default function CitySlider({ cities, onSelectCity }) {
    const [current, setCurrent] = useState(0);
    const [paused, setPaused] = useState(false);
    const timeoutRef = useRef(null);

    const goTo = useCallback((index) => {
        const wrapped = ((index % cities.length) + cities.length) % cities.length;
        setCurrent(wrapped);
    }, [cities.length]);

    const prev = () => goTo(current - 1);
    const next = () => goTo(current + 1);

    useEffect(() => {
        if (paused) return;

        timeoutRef.current = setTimeout(() => {
            goTo(current + 1);
        }, AUTO_SCROLL_INTERVAL);

        return () => clearTimeout(timeoutRef.current);
    }, [current, paused, goTo]);

    const handleMouseEnter = () => setPaused(true);
    const handleMouseLeave = () => setPaused(false);

    return (
        <div
            className="flex flex-col items-center gap-5 w-full max-w-sm"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
        >
            <div className="relative w-full h-[200px]">
                {cities.map((city, i) => (
                    <div
                        key={city}
                        onClick={() => onSelectCity && onSelectCity(city)}
                        className={`absolute inset-0 transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)]
                                    ${i === current
                                        ? "opacity-100 scale-100 pointer-events-auto cursor-pointer"
                                        : "opacity-0 scale-95 pointer-events-none"
                                    }`}
                    >
                        <WeatherCard city={city} />
                    </div>
                ))}
            </div>

            <div className="flex items-center gap-4">
                <button
                    onClick={prev}
                    className="w-9 h-9 rounded-full border border-outline bg-panel text-primary flex items-center justify-center text-lg shadow-panel cursor-pointer transition-all duration-250 ease-out hover:bg-[var(--color-glow-gradient)] hover:border-[var(--color-glow)] hover:shadow-hover hover:scale-108 active:scale-95"
                    aria-label="Previous city"
                >
                    <FiChevronLeft />
                </button>

                <div className="flex items-center gap-2">
                    {cities.map((city, i) => (
                        <button
                            key={city}
                            onClick={() => goTo(i)}
                            aria-label={`Go to ${city}`}
                            className={`h-2 rounded-full border-none cursor-pointer
                                        transition-all duration-300 ease-out
                                        ${i === current
                                            ? "w-6 bg-[var(--color-glow)]"
                                            : "w-2 bg-outline hover:bg-muted"
                                        }`}
                        />
                    ))}
                </div>

                <button
                    onClick={next}
                    className="w-9 h-9 rounded-full border border-outline bg-panel text-primary flex items-center justify-center text-lg shadow-panel cursor-pointer transition-all duration-250 ease-out hover:bg-[var(--color-glow-gradient)] hover:border-[var(--color-glow)] hover:shadow-hover hover:scale-108 active:scale-95"
                    aria-label="Next city"
                >
                    <FiChevronRight />
                </button>
            </div>
        </div>
    );
}
