"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { FiLogIn, FiLogOut, FiUser, FiUserPlus } from "react-icons/fi";
import { authService } from "@/lib/api";

export default function AuthPanel() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(false);
    const [checkingSession, setCheckingSession] = useState(true);

    useEffect(() => {
        let cancelled = false;

        async function loadSession() {
            try {
                const currentUser = await authService.me();
                if (!cancelled) {
                    setUser(currentUser);
                }
            } catch {
                authService.logout();
                if (!cancelled) {
                    setUser(null);
                }
            } finally {
                if (!cancelled) {
                    setCheckingSession(false);
                }
            }
        }

        loadSession();

        function reloadSession() {
            setCheckingSession(true);
            loadSession();
        }

        window.addEventListener("tripify:session-changed", reloadSession);
        return () => {
            cancelled = true;
            window.removeEventListener("tripify:session-changed", reloadSession);
        };
    }, []);

    useEffect(() => {
        function updateUser(event) {
            setUser(event.detail);
        }

        window.addEventListener("tripify:user-updated", updateUser);
        return () => {
            window.removeEventListener("tripify:user-updated", updateUser);
        };
    }, []);

    function startLogin() {
        setLoading(true);
        authService.login();
    }

    function startRegister() {
        setLoading(true);
        authService.register();
    }

    function logout() {
        authService.logout();
        setUser(null);
        window.dispatchEvent(new CustomEvent("tripify:user-updated", { detail: null }));
    }

    return (
        <header className="sticky top-0 z-40 w-full border-b border-outline bg-panel/90 px-4 py-3 shadow-panel backdrop-blur">
            <div className="mx-auto flex w-full max-w-5xl items-center justify-between gap-4">
                <Link
                    href="/"
                    className="text-lg font-bold tracking-tight transition hover:text-muted"
                >
                    Tripify
                </Link>

                {checkingSession ? (
                    <div className="h-10 w-36 animate-pulse rounded-md bg-[var(--color-glow-gradient)]" />
                ) : user ? (
                    <div className="flex min-w-0 items-center gap-3">
                        <div className="hidden min-w-0 text-right sm:block">
                            <p className="truncate text-sm font-semibold">{user.name}</p>
                            <p className="truncate text-xs text-muted">{user.email}</p>
                        </div>
                        <Link
                            href="/account"
                            className="grid h-10 w-10 shrink-0 place-items-center rounded-md border border-outline bg-main text-primary transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)]"
                            aria-label="Panel użytkownika"
                            title="Panel użytkownika"
                        >
                            <FiUser />
                        </Link>
                        <button
                            type="button"
                            onClick={logout}
                            className="grid h-10 w-10 shrink-0 place-items-center rounded-md border border-outline bg-main text-primary transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)] disabled:cursor-not-allowed disabled:opacity-60"
                            aria-label="Wyloguj"
                            title="Wyloguj"
                        >
                            <FiLogOut />
                        </button>
                    </div>
                ) : (
                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            onClick={startLogin}
                            disabled={loading}
                            className="flex h-10 items-center justify-center gap-2 rounded-md border border-outline bg-main px-3 text-sm font-semibold transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)] disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            <FiLogIn />
                            <span className="hidden sm:inline">Logowanie</span>
                        </button>
                        <button
                            type="button"
                            onClick={startRegister}
                            disabled={loading}
                            className="flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-3 text-sm font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            <FiUserPlus />
                            <span className="hidden sm:inline">Rejestracja</span>
                        </button>
                    </div>
                )}
            </div>
        </header>
    );
}
