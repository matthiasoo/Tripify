"use client";

import { useState } from "react";
import { FiChevronDown, FiClock, FiThermometer, FiTrash2 } from "react-icons/fi";
import PlanContent from "@/components/PlanContent/PlanContent";

export default function SavedPlanCard({ plan, onDelete, deleting }) {
    const [expanded, setExpanded] = useState(false);

    const formattedDate = new Date(plan.createdAt).toLocaleDateString("pl-PL", {
        day: "numeric",
        month: "long",
        year: "numeric",
    });

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

            <div className="px-5 pb-5 pt-4">
                <button
                    type="button"
                    onClick={() => setExpanded((value) => !value)}
                    className="flex w-full cursor-pointer items-center justify-between rounded-xl border border-outline bg-main px-4 py-2.5 text-sm font-semibold text-primary transition hover:border-[var(--color-glow)]"
                    aria-expanded={expanded}
                >
                    <span>{expanded ? "Ukryj plan" : "Pokaż plan"}</span>
                    <FiChevronDown className={`transition-transform duration-300 ${expanded ? "rotate-180" : ""}`} />
                </button>

                {expanded && (
                    <div className="mt-4 animate-fade-in">
                        <PlanContent markdown={plan.plan} />
                    </div>
                )}
            </div>
        </article>
    );
}
