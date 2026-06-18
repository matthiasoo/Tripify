"use client";

import { FiCalendar, FiInfo, FiMoon, FiSun, FiSunrise, FiSunset } from "react-icons/fi";

function renderInline(text, keyPrefix) {
    const parts = text.split(/(\*\*.*?\*\*|\*[^*]+?\*)/g);
    return parts.map((part, index) => {
        if (part.startsWith("**") && part.endsWith("**")) {
            return (
                <strong key={`${keyPrefix}-b-${index}`} className="font-semibold text-primary">
                    {part.slice(2, -2)}
                </strong>
            );
        }
        if (part.length > 2 && part.startsWith("*") && part.endsWith("*")) {
            return (
                <em key={`${keyPrefix}-i-${index}`} className="italic text-muted">
                    {part.slice(1, -1)}
                </em>
            );
        }
        return part;
    });
}

function sectionIcon(title) {
    const value = title.toLowerCase();
    if (value.includes("rano") || value.includes("poranek")) return FiSunrise;
    if (value.includes("popołud") || value.includes("poludn") || value.includes("południ")) return FiSun;
    if (value.includes("wieczór") || value.includes("wieczor")) return FiSunset;
    if (value.includes("noc")) return FiMoon;
    return null;
}

function renderBlocks(lines, keyPrefix) {
    const blocks = [];
    let bullets = [];

    const flushBullets = () => {
        if (bullets.length === 0) return;
        const items = bullets;
        bullets = [];
        blocks.push(
            <ul key={`${keyPrefix}-ul-${blocks.length}`} className="flex flex-col gap-1.5">
                {items.map((item, index) => (
                    <li key={index} className="flex gap-2.5 text-left text-sm leading-relaxed text-muted">
                        <span className="mt-[7px] h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-glow)]" />
                        <span>{renderInline(item, `${keyPrefix}-li-${index}`)}</span>
                    </li>
                ))}
            </ul>
        );
    };

    lines.forEach((rawLine, index) => {
        const line = rawLine.trim();

        if (line.startsWith("- ") || line.startsWith("* ")) {
            bullets.push(line.slice(2));
            return;
        }

        flushBullets();

        if (line === "" || line === "---") return;

        if (line.startsWith("#### ")) {
            blocks.push(
                <h5 key={index} className="mt-2 text-left text-sm font-semibold text-primary">
                    {renderInline(line.slice(5), `${keyPrefix}-h5-${index}`)}
                </h5>
            );
            return;
        }

        if (line.startsWith("### ")) {
            const title = line.slice(4);
            const Icon = sectionIcon(title);
            blocks.push(
                <h4
                    key={index}
                    className="mt-3 flex items-center gap-2 text-left text-xs font-bold uppercase tracking-wider text-primary"
                >
                    {Icon && <Icon className="shrink-0 text-sm text-[var(--color-glow)]" />}
                    <span>{renderInline(title, `${keyPrefix}-h4-${index}`)}</span>
                </h4>
            );
            return;
        }

        blocks.push(
            <p key={index} className="text-left text-sm leading-relaxed text-muted">
                {renderInline(line, `${keyPrefix}-p-${index}`)}
            </p>
        );
    });

    flushBullets();
    return blocks;
}

/**
 * Renders a trip plan written in Markdown as a sequence of styled cards:
 * an optional title, an intro block (everything before the first `## ` heading)
 * and one card per day (each `## ` heading). Used both for freshly generated
 * plans and for saved plans so they always look identical.
 */
export default function PlanContent({ markdown, className = "" }) {
    if (!markdown) return null;

    const lines = markdown.split("\n");
    let title = null;
    const intro = [];
    const days = [];
    let target = intro;

    lines.forEach((rawLine) => {
        const line = rawLine.trim();

        if (line.startsWith("## ")) {
            const day = { title: line.slice(3), lines: [] };
            days.push(day);
            target = day.lines;
            return;
        }

        if (line.startsWith("# ")) {
            title = line.slice(2);
            return;
        }

        target.push(rawLine);
    });

    const hasIntro = intro.some((line) => line.trim() !== "");

    return (
        <div className={`flex flex-col gap-4 ${className}`}>
            {title && (
                <h3 className="text-left text-xl font-extrabold tracking-tight text-primary">
                    {renderInline(title, "title")}
                </h3>
            )}

            {hasIntro && (
                <div className="flex flex-col gap-2 rounded-2xl border border-outline bg-main/60 p-4">
                    {renderBlocks(intro, "intro")}
                </div>
            )}

            {days.map((day, index) => {
                const isDay = /dzie|day/i.test(day.title);
                const BadgeIcon = isDay ? FiCalendar : FiInfo;
                return (
                    <div
                        key={index}
                        className="flex flex-col gap-2 rounded-2xl border border-outline bg-main/60 p-4 shadow-panel"
                    >
                        <div className="flex items-center gap-3">
                            <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-[var(--color-glow)] text-[var(--color-main)]">
                                <BadgeIcon className="text-sm" />
                            </span>
                            <h4 className="text-left text-base font-bold text-primary">
                                {renderInline(day.title, `day-${index}`)}
                            </h4>
                        </div>
                        {renderBlocks(day.lines, `day-${index}`)}
                    </div>
                );
            })}
        </div>
    );
}
