"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { FiLogIn, FiLogOut, FiUser, FiUserPlus, FiX } from "react-icons/fi";
import { authService, setStoredToken } from "@/lib/api";

const INITIAL_FORM = {
    name: "",
    email: "",
    password: "",
};

export default function AuthPanel() {
    const [mode, setMode] = useState("login");
    const [isOpen, setIsOpen] = useState(false);
    const [form, setForm] = useState(INITIAL_FORM);
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(false);
    const [checkingSession, setCheckingSession] = useState(true);
    const [message, setMessage] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadSession() {
            try {
                const currentUser = await authService.me();
                if (!cancelled) {
                    setUser(currentUser);
                }
            } catch {
                setStoredToken(null);
            } finally {
                if (!cancelled) {
                    setCheckingSession(false);
                }
            }
        }

        loadSession();
        return () => {
            cancelled = true;
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

    function updateField(event) {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
    }

    function switchMode(nextMode) {
        setMode(nextMode);
        setMessage("");
    }

    function openAuth(nextMode) {
        switchMode(nextMode);
        setIsOpen(true);
    }

    function closeAuth() {
        if (loading) return;
        setIsOpen(false);
        setMessage("");
    }

    async function submit(event) {
        event.preventDefault();
        setLoading(true);
        setMessage("");

        try {
            const payload = {
                email: form.email,
                password: form.password,
                ...(mode === "register" ? { name: form.name } : {}),
            };
            const response = mode === "register"
                ? await authService.register(payload)
                : await authService.login(payload);

            setStoredToken(response.token);
            setUser(response.user);
            window.dispatchEvent(new CustomEvent("tripify:user-updated", { detail: response.user }));
            setForm(INITIAL_FORM);
            setIsOpen(false);
        } catch (error) {
            setMessage(error.message);
        } finally {
            setLoading(false);
        }
    }

    async function logout() {
        setLoading(true);
        setMessage("");

        try {
            await authService.logout();
        } catch {
            // Local logout should still complete if the server session is already gone.
        } finally {
            setStoredToken(null);
            setUser(null);
            window.dispatchEvent(new CustomEvent("tripify:user-updated", { detail: null }));
            setLoading(false);
        }
    }

    const isRegister = mode === "register";

    return (
        <>
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
                                disabled={loading}
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
                                onClick={() => openAuth("login")}
                                className="flex h-10 items-center justify-center gap-2 rounded-md border border-outline bg-main px-3 text-sm font-semibold transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)]"
                            >
                                <FiLogIn />
                                <span className="hidden sm:inline">Logowanie</span>
                            </button>
                            <button
                                type="button"
                                onClick={() => openAuth("register")}
                                className="flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-3 text-sm font-semibold text-main transition hover:opacity-90"
                            >
                                <FiUserPlus />
                                <span className="hidden sm:inline">Rejestracja</span>
                            </button>
                        </div>
                    )}
                </div>
            </header>

            {isOpen && (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4 py-8 backdrop-blur-sm"
                    onMouseDown={closeAuth}
                    role="presentation"
                >
                    <section
                        className="w-full max-w-sm animate-scale-up rounded-lg border border-outline bg-panel p-5 text-left shadow-panel"
                        onMouseDown={(event) => event.stopPropagation()}
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="auth-dialog-title"
                    >
                        <div className="mb-5 flex items-center justify-between gap-4">
                            <h2 id="auth-dialog-title" className="text-xl font-bold">
                                {isRegister ? "Rejestracja" : "Logowanie"}
                            </h2>
                            <button
                                type="button"
                                onClick={closeAuth}
                                className="grid h-9 w-9 place-items-center rounded-md border border-outline bg-main text-primary transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)]"
                                aria-label="Zamknij"
                                title="Zamknij"
                            >
                                <FiX />
                            </button>
                        </div>

                        <div className="mb-5 grid grid-cols-2 rounded-md border border-outline bg-main p-1">
                            <button
                                type="button"
                                onClick={() => switchMode("login")}
                                className={`flex h-10 items-center justify-center gap-2 rounded-[4px] text-sm font-semibold transition ${!isRegister ? "bg-panel shadow-panel" : "text-muted hover:text-primary"}`}
                            >
                                <FiLogIn />
                                Logowanie
                            </button>
                            <button
                                type="button"
                                onClick={() => switchMode("register")}
                                className={`flex h-10 items-center justify-center gap-2 rounded-[4px] text-sm font-semibold transition ${isRegister ? "bg-panel shadow-panel" : "text-muted hover:text-primary"}`}
                            >
                                <FiUserPlus />
                                Rejestracja
                            </button>
                        </div>

                        <form onSubmit={submit} className="flex flex-col gap-3">
                            {isRegister && (
                                <label className="flex flex-col gap-1 text-sm font-medium">
                                    Imię
                                    <input
                                        name="name"
                                        value={form.name}
                                        onChange={updateField}
                                        minLength={2}
                                        maxLength={80}
                                        required
                                        className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                                        autoComplete="name"
                                    />
                                </label>
                            )}

                            <label className="flex flex-col gap-1 text-sm font-medium">
                                Email
                                <input
                                    name="email"
                                    type="email"
                                    value={form.email}
                                    onChange={updateField}
                                    required
                                    className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                                    autoComplete="email"
                                />
                            </label>

                            <label className="flex flex-col gap-1 text-sm font-medium">
                                Hasło
                                <input
                                    name="password"
                                    type="password"
                                    value={form.password}
                                    onChange={updateField}
                                    minLength={8}
                                    required
                                    className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                                    autoComplete={isRegister ? "new-password" : "current-password"}
                                />
                            </label>

                            {message && (
                                <p className="rounded-md border border-danger-outline bg-danger-panel px-3 py-2 text-sm text-danger">
                                    {message}
                                </p>
                            )}

                            <button
                                type="submit"
                                disabled={loading}
                                className="mt-1 flex h-11 items-center justify-center gap-2 rounded-md bg-primary px-4 font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {isRegister ? <FiUserPlus /> : <FiLogIn />}
                                {loading ? "Przetwarzanie..." : isRegister ? "Utwórz konto" : "Zaloguj"}
                            </button>
                        </form>
                    </section>
                </div>
            )}
        </>
    );
}
