"use client";

import { useState } from "react";
import { FiChevronDown, FiClock, FiRefreshCw, FiThermometer, FiTrash2 } from "react-icons/fi";
import PlanContent from "@/components/PlanContent/PlanContent";

const DAY_OPTIONS = [1, 2, 3, 4, 5, 6, 7, 10, 14];

export default function SavedPlanCard({ plan, onDelete, deleting, onRegenerate }) {
    const [expanded, setExpanded] = useState(false);
    const [showRegenerate, setShowRegenerate] = useState(false);
    const [days, setDays] = useState(3);
    const [pace, setPace] = useState("relaxed");
    const [regenerating, setRegenerating] = useState(false);
    const [regenerateError, setRegenerateError] = useState("");

    const formattedDate = new Date(plan.createdAt).toLocaleDateString("pl-PL", {
        day: "numeric",
        month: "long",
        year: "numeric",
    });

    async function handleRegenerate() {
        setRegenerating(true);
        setRegenerateError("");

        try {
            await onRegenerate(plan.id, { days, pace });
            setShowRegenerate(false);
            setExpanded(true);
        } catch (error) {
            setRegenerateError(error.message || "Nie udało się ponownie wygenerować planu.");
        } finally {
            setRegenerating(false);
        }
    }

    return (
        <article className="flex flex-col overflow-hidden rounded-2xl border border-outline bg-panel shadow-panel transition-all hover:border-[var(--color-glow)] hover:shadow-hover">
            <div className="flex items-start justify-between gap-3 bg-gradient-to-br from-[var(--color-glow-gradient)] to-panel p-5">
                <div className="flex flex-col gap-1.5 text-left">
                    <h3 className="text-xl font-extrabold capitalize leading-none text-primary">{plan.city}</h3>
                    <span className="flex items-center gap-1.5 text-xs text-muted">
                        <FiClock />
                        {formattedDate}
                    </span>
                </div>

                <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1.5 rounded-xl border border-outline bg-panel/80 px-3 py-2 backdrop-blur">
                        <FiThermometer className="text-[var(--color-glow)]" />
                        <div className="flex flex-col items-start leading-none">
                            <span className="text-sm font-bold text-primary">
                                {Math.round(plan.weather.temperature)}°C
                            </span>
                            <span className="mt-0.5 text-[11px] capitalize text-muted">
                                {plan.weather.description}
                            </span>
                        </div>
                    </div>

                    <button
                        type="button"
                        onClick={() => onDelete(plan.id)}
                        disabled={deleting}
                        className="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-danger-outline bg-danger-panel text-danger transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-60"
                        aria-label="Usuń plan"
                        title="Usuń plan"
                    >
                        <FiTrash2 />
                    </button>
                </div>
            </div>

            {plan.places?.length > 0 && (
                <div className="flex flex-wrap gap-2 px-5 pt-4">
                    {plan.places.slice(0, 4).map((place) => (
                        <span
                            key={`${plan.id}-${place.name}`}
                            className="rounded-full border border-outline bg-main px-3 py-1 text-xs text-muted"
                        >
                            {place.name}
                        </span>
                    ))}
                    {plan.places.length > 4 && (
                        <span className="rounded-full border border-outline bg-main px-3 py-1 text-xs text-muted">
                            +{plan.places.length - 4}
                        </span>
                    )}
                </div>
            )}

            <div className="flex flex-col gap-3 px-5 pb-5 pt-4">
                <div className="flex flex-wrap gap-2">
                    <button
                        type="button"
                        onClick={() => setExpanded((value) => !value)}
                        className="flex flex-1 cursor-pointer items-center justify-between rounded-xl border border-outline bg-main px-4 py-2.5 text-sm font-semibold text-primary transition hover:border-[var(--color-glow)]"
                        aria-expanded={expanded}
                    >
                        <span>{expanded ? "Ukryj plan" : "Pokaż plan"}</span>
                        <FiChevronDown className={`transition-transform duration-300 ${expanded ? "rotate-180" : ""}`} />
                    </button>

                    <button
                        type="button"
                        onClick={() => setShowRegenerate((value) => !value)}
                        disabled={regenerating}
                        className="flex cursor-pointer items-center gap-2 rounded-xl border border-outline bg-main px-4 py-2.5 text-sm font-semibold text-primary transition hover:border-[var(--color-glow)] disabled:cursor-not-allowed disabled:opacity-60"
                        aria-expanded={showRegenerate}
                    >
                        <FiRefreshCw className={regenerating ? "animate-spin" : ""} />
                        <span>Regeneruj</span>
                    </button>
                </div>

                {showRegenerate && (
                    <div className="flex animate-fade-in flex-col gap-3 rounded-xl border border-outline bg-main p-4 text-left">
                        <p className="text-xs text-muted">
                            Wygeneruj plan dla <span className="font-semibold capitalize text-primary">{plan.city}</span> od
                            nowa. Obecny zapisany plan zostanie nadpisany.
                        </p>

                        <div className="flex flex-col gap-3 sm:flex-row">
                            <label className="flex flex-1 flex-col gap-1 text-xs font-medium text-muted">
                                Długość pobytu
                                <select
                                    value={days}
                                    onChange={(event) => setDays(Number(event.target.value))}
                                    disabled={regenerating}
                                    className="rounded-lg border border-outline bg-panel px-3 py-2 text-sm text-primary outline-none transition focus:border-[var(--color-glow)] disabled:opacity-60"
                                >
                                    {DAY_OPTIONS.map((option) => (
                                        <option key={option} value={option}>
                                            {option} {option === 1 ? "dzień" : "dni"}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label className="flex flex-1 flex-col gap-1 text-xs font-medium text-muted">
                                Tempo wycieczki
                                <select
                                    value={pace}
                                    onChange={(event) => setPace(event.target.value)}
                                    disabled={regenerating}
                                    className="rounded-lg border border-outline bg-panel px-3 py-2 text-sm text-primary outline-none transition focus:border-[var(--color-glow)] disabled:opacity-60"
                                >
                                    <option value="relaxed">Luźne / Spokojne</option>
                                    <option value="intense">Intensywne / Aktywne</option>
                                </select>
                            </label>
                        </div>

                        {regenerateError && (
                            <p className="rounded-lg border border-danger-outline bg-danger-panel px-3 py-2 text-sm text-danger">
                                {regenerateError}
                            </p>
                        )}

                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={handleRegenerate}
                                disabled={regenerating}
                                className="flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                <FiRefreshCw className={regenerating ? "animate-spin" : ""} />
                                {regenerating ? "Generowanie..." : "Wygeneruj ponownie"}
                            </button>
                            <button
                                type="button"
                                onClick={() => setShowRegenerate(false)}
                                disabled={regenerating}
                                className="cursor-pointer rounded-lg border border-outline bg-panel px-4 py-2.5 text-sm font-semibold text-primary transition hover:border-[var(--color-glow)] disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                Anuluj
                            </button>
                        </div>
                    </div>
                )}

                {expanded && (
                    <div className="animate-fade-in">
                        <PlanContent markdown={plan.plan} />
                    </div>
                )}
            </div>
        </article>
    );
}
