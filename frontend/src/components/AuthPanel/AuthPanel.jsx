"use client";

import { useEffect, useState } from "react";
import { FiLogIn, FiLogOut, FiUserPlus } from "react-icons/fi";
import { authService, setStoredToken } from "@/lib/api";

const INITIAL_FORM = {
    name: "",
    email: "",
    password: "",
};

export default function AuthPanel() {
    const [mode, setMode] = useState("login");
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

    function updateField(event) {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
    }

    function switchMode(nextMode) {
        setMode(nextMode);
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
            setForm(INITIAL_FORM);
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
            setLoading(false);
        }
    }

    if (checkingSession) {
        return (
            <section className="w-full max-w-sm rounded-lg border border-outline bg-panel/80 p-5 shadow-panel">
                <div className="h-24 animate-pulse rounded-md bg-[var(--color-glow-gradient)]" />
            </section>
        );
    }

    if (user) {
        return (
            <section className="w-full max-w-sm rounded-lg border border-outline bg-panel/85 p-5 shadow-panel">
                <div className="flex items-center justify-between gap-4 text-left">
                    <div className="min-w-0">
                        <p className="text-sm text-muted">Zalogowano jako</p>
                        <p className="truncate text-lg font-semibold">{user.name}</p>
                        <p className="truncate text-sm text-muted">{user.email}</p>
                    </div>
                    <button
                        type="button"
                        onClick={logout}
                        disabled={loading}
                        className="grid h-11 w-11 shrink-0 place-items-center rounded-md border border-outline bg-main text-primary transition hover:border-[var(--color-glow)] hover:bg-[var(--color-glow-gradient)] disabled:cursor-not-allowed disabled:opacity-60"
                        aria-label="Wyloguj"
                        title="Wyloguj"
                    >
                        <FiLogOut />
                    </button>
                </div>
            </section>
        );
    }

    const isRegister = mode === "register";

    return (
        <section className="w-full max-w-sm rounded-lg border border-outline bg-panel/85 p-5 shadow-panel">
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

            <form onSubmit={submit} className="flex flex-col gap-3 text-left">
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
    );
}
