"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import { weatherService, cityImageService } from "@/lib/api";

const CACHE_TTL = 10 * 60 * 1000;
const cache = new Map();

async function fetchWithCache(key, fetcher) {
    const cached = cache.get(key);
    if (cached && Date.now() - cached.ts < CACHE_TTL) {
        return cached.data;
    }
    const data = await fetcher();
    cache.set(key, { data, ts: Date.now() });
    return data;
}

function getLocalTime(timezoneOffset) {
    const now = new Date();
    const utc = now.getTime() + now.getTimezoneOffset() * 60000;
    const local = new Date(utc + timezoneOffset * 1000);
    return local.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
    });
}

export default function WeatherCard({ city }) {
    const [weather, setWeather] = useState(null);
    const [imageUrl, setImageUrl] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                setLoading(true);
                setError(null);

                const [weatherData, imageData] = await Promise.all([
                    fetchWithCache(`weather:${city}`, () =>
                        weatherService.getCurrent({ city })
                    ),
                    fetchWithCache(`image:${city}`, () =>
                        cityImageService.search({ city, perPage: 1 })
                    ),
                ]);

                if (cancelled) return;

                setWeather(weatherData);
                if (imageData.photos?.[0]?.urls?.regular) {
                    setImageUrl(imageData.photos[0].urls.regular);
                }
            } catch (err) {
                if (!cancelled) setError(err.message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }

        load();
        return () => { cancelled = true; };
    }, [city]);

    if (loading) {
        return (
            <div className="w-full h-[200px] rounded-2xl bg-panel border border-outline relative overflow-hidden">
                <div className="absolute inset-0 animate-pulse bg-gradient-to-r from-transparent via-white/5 to-transparent" />
            </div>
        );
    }

    if (error || !weather) {
        return (
            <div className="w-full h-[200px] rounded-2xl bg-danger-panel border border-danger-outline flex items-center justify-center text-danger text-sm px-5">
                {error || "Nie udało się załadować danych"}
            </div>
        );
    }

    const temp = Math.round(weather.main.temp);
    const desc = weather.weather?.[0]?.description || "";
    const iconCode = weather.weather?.[0]?.icon || "01d";
    const localTime = getLocalTime(weather.timezone);

    return (
        <div className="group relative w-full h-[200px] rounded-2xl overflow-hidden cursor-pointer
                        isolate transform-gpu
                        transition-all duration-350 ease-[cubic-bezier(0.16,1,0.3,1)]
                        hover:-translate-y-1 hover:shadow-glow">

            {imageUrl && (
                <div className="absolute inset-0 z-0">
                    <Image
                        src={imageUrl}
                        alt={city}
                        fill
                        sizes="(max-width: 640px) 100vw, 400px"
                        className="object-cover object-center brightness-[0.65] saturate-110
                                   transition-all duration-600 ease-out
                                   group-hover:brightness-[0.55] group-hover:saturate-[1.2] group-hover:scale-105"
                        priority
                    />
                    <div className="absolute inset-0 z-1
                                    bg-gradient-to-r from-transparent via-[oklch(0.12_0.02_230/0.55)] to-[oklch(0.12_0.02_230/0.9)]
                                    [via-percentage:55%] [from-percentage:20%]" />
                </div>
            )}

            <div className="absolute inset-0 z-2 rounded-2xl
                            border border-white/[0.06]" />

            <div className="relative z-3 flex flex-col justify-between h-full p-5 text-white/[0.97]">
                <div className="flex justify-between items-start">
                    <span className="font-bold text-xl tracking-tight leading-tight
                                     drop-shadow-[0_2px_8px_oklch(0_0_0/0.4)]">
                        {weather.name}
                    </span>
                    <span className="font-mono text-[0.8rem] font-normal opacity-80
                                     drop-shadow-[0_1px_4px_oklch(0_0_0/0.5)]">
                        {localTime}
                    </span>
                </div>

                <div className="flex justify-between items-end">
                    <span className="font-bold text-[2.5rem] leading-none tracking-[-0.04em]
                                     drop-shadow-[0_2px_12px_oklch(0_0_0/0.4)]">
                        {temp}
                        <span className="text-lg font-normal opacity-70 align-super ml-0.5">°C</span>
                    </span>

                    <div className="flex flex-col items-end gap-0.5">
                        <img
                            src={`https://openweathermap.org/img/wn/${iconCode}@2x.png`}
                            alt={desc}
                            className="w-10 h-10 drop-shadow-[0_2px_4px_oklch(0_0_0/0.3)]"
                        />
                        <span className="text-[0.78rem] font-medium capitalize opacity-85
                                         drop-shadow-[0_1px_4px_oklch(0_0_0/0.5)]">
                            {desc}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}
