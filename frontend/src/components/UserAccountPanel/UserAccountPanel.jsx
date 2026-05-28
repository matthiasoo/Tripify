"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { FiLock, FiSave, FiUser } from "react-icons/fi";
import { authService, setStoredToken } from "@/lib/api";

const INITIAL_PROFILE = {
    name: "",
    email: "",
};

const INITIAL_PASSWORDS = {
    currentPassword: "",
    newPassword: "",
    repeatPassword: "",
};

export default function UserAccountPanel() {
    const router = useRouter();
    const [profile, setProfile] = useState(INITIAL_PROFILE);
    const [passwords, setPasswords] = useState(INITIAL_PASSWORDS);
    const [loading, setLoading] = useState(true);
    const [savingProfile, setSavingProfile] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [profileMessage, setProfileMessage] = useState("");
    const [passwordMessage, setPasswordMessage] = useState("");
    const [profileError, setProfileError] = useState("");
    const [passwordError, setPasswordError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadUser() {
            try {
                const user = await authService.me();
                if (!cancelled) {
                    setProfile({
                        name: user.name,
                        email: user.email,
                    });
                }
            } catch {
                setStoredToken(null);
                router.replace("/");
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadUser();
        return () => {
            cancelled = true;
        };
    }, [router]);

    useEffect(() => {
        function handleUserUpdate(event) {
            if (event.detail === null) {
                router.replace("/");
            }
        }

        window.addEventListener("tripify:user-updated", handleUserUpdate);
        return () => {
            window.removeEventListener("tripify:user-updated", handleUserUpdate);
        };
    }, [router]);

    function updateProfileField(event) {
        const { name, value } = event.target;
        setProfile((current) => ({ ...current, [name]: value }));
    }

    function updatePasswordField(event) {
        const { name, value } = event.target;
        setPasswords((current) => ({ ...current, [name]: value }));
    }

    async function submitProfile(event) {
        event.preventDefault();
        setSavingProfile(true);
        setProfileError("");
        setProfileMessage("");

        try {
            const updatedUser = await authService.updateProfile(profile);
            setProfile({
                name: updatedUser.name,
                email: updatedUser.email,
            });
            window.dispatchEvent(new CustomEvent("tripify:user-updated", { detail: updatedUser }));
            setProfileMessage("Dane zostały zaktualizowane.");
        } catch (error) {
            setProfileError(error.message);
        } finally {
            setSavingProfile(false);
        }
    }

    async function submitPassword(event) {
        event.preventDefault();
        setSavingPassword(true);
        setPasswordError("");
        setPasswordMessage("");

        if (passwords.newPassword !== passwords.repeatPassword) {
            setPasswordError("Nowe hasła muszą być takie same.");
            setSavingPassword(false);
            return;
        }

        try {
            await authService.changePassword({
                currentPassword: passwords.currentPassword,
                newPassword: passwords.newPassword,
            });
            setPasswords(INITIAL_PASSWORDS);
            setPasswordMessage("Hasło zostało zmienione.");
        } catch (error) {
            setPasswordError(error.message);
        } finally {
            setSavingPassword(false);
        }
    }

    if (loading) {
        return (
            <section className="w-full max-w-3xl rounded-lg border border-outline bg-panel/85 p-6 shadow-panel">
                <div className="h-56 animate-pulse rounded-md bg-[var(--color-glow-gradient)]" />
            </section>
        );
    }

    return (
        <section className="grid w-full max-w-3xl gap-6 text-left">
            <div>
                <h1 className="text-3xl font-bold tracking-tight">Panel uzytkownika</h1>
                <p className="mt-2 text-sm text-muted">
                    Zarzadzaj danymi konta, adresem email i haslem.
                </p>
            </div>

            <form
                onSubmit={submitProfile}
                className="rounded-lg border border-outline bg-panel/85 p-5 shadow-panel"
            >
                <div className="mb-5 flex items-center gap-3">
                    <div className="grid h-10 w-10 place-items-center rounded-md bg-main text-primary">
                        <FiUser />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold">Dane konta</h2>
                        <p className="text-sm text-muted">Zmien nazwe uzytkownika i email.</p>
                    </div>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                    <label className="flex flex-col gap-1 text-sm font-medium">
                        Imie
                        <input
                            name="name"
                            value={profile.name}
                            onChange={updateProfileField}
                            minLength={2}
                            maxLength={80}
                            required
                            className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                            autoComplete="name"
                        />
                    </label>

                    <label className="flex flex-col gap-1 text-sm font-medium">
                        Email
                        <input
                            name="email"
                            type="email"
                            value={profile.email}
                            onChange={updateProfileField}
                            required
                            className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                            autoComplete="email"
                        />
                    </label>
                </div>

                {profileError && (
                    <p className="mt-4 rounded-md border border-danger-outline bg-danger-panel px-3 py-2 text-sm text-danger">
                        {profileError}
                    </p>
                )}
                {profileMessage && (
                    <p className="mt-4 rounded-md border border-outline bg-main px-3 py-2 text-sm text-muted">
                        {profileMessage}
                    </p>
                )}

                <button
                    type="submit"
                    disabled={savingProfile}
                    className="mt-5 flex h-11 items-center justify-center gap-2 rounded-md bg-primary px-4 font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    <FiSave />
                    {savingProfile ? "Zapisywanie..." : "Zapisz dane"}
                </button>
            </form>

            <form
                onSubmit={submitPassword}
                className="rounded-lg border border-outline bg-panel/85 p-5 shadow-panel"
            >
                <div className="mb-5 flex items-center gap-3">
                    <div className="grid h-10 w-10 place-items-center rounded-md bg-main text-primary">
                        <FiLock />
                    </div>
                    <div>
                        <h2 className="text-xl font-bold">Zmiana hasla</h2>
                        <p className="text-sm text-muted">Podaj obecne haslo i ustaw nowe.</p>
                    </div>
                </div>

                <div className="grid gap-4">
                    <label className="flex flex-col gap-1 text-sm font-medium">
                        Obecne haslo
                        <input
                            name="currentPassword"
                            type="password"
                            value={passwords.currentPassword}
                            onChange={updatePasswordField}
                            required
                            className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                            autoComplete="current-password"
                        />
                    </label>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <label className="flex flex-col gap-1 text-sm font-medium">
                            Nowe haslo
                            <input
                                name="newPassword"
                                type="password"
                                value={passwords.newPassword}
                                onChange={updatePasswordField}
                                minLength={8}
                                required
                                className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                                autoComplete="new-password"
                            />
                        </label>

                        <label className="flex flex-col gap-1 text-sm font-medium">
                            Powtorz nowe haslo
                            <input
                                name="repeatPassword"
                                type="password"
                                value={passwords.repeatPassword}
                                onChange={updatePasswordField}
                                minLength={8}
                                required
                                className="h-11 rounded-md border border-outline bg-main px-3 text-primary outline-none transition focus:border-[var(--color-glow)]"
                                autoComplete="new-password"
                            />
                        </label>
                    </div>
                </div>

                {passwordError && (
                    <p className="mt-4 rounded-md border border-danger-outline bg-danger-panel px-3 py-2 text-sm text-danger">
                        {passwordError}
                    </p>
                )}
                {passwordMessage && (
                    <p className="mt-4 rounded-md border border-outline bg-main px-3 py-2 text-sm text-muted">
                        {passwordMessage}
                    </p>
                )}

                <button
                    type="submit"
                    disabled={savingPassword}
                    className="mt-5 flex h-11 items-center justify-center gap-2 rounded-md bg-primary px-4 font-semibold text-main transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    <FiSave />
                    {savingPassword ? "Zapisywanie..." : "Zmien haslo"}
                </button>
            </form>
        </section>
    );
}
