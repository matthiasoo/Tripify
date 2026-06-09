"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/lib/api";

export default function CallbackPage() {
    const router = useRouter();
    const [error, setError] = useState("");

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const oauthError = params.get("error");
        const code = params.get("code");
        const state = params.get("state");

        if (oauthError) {
            setError("Logowanie zostało przerwane.");
            return;
        }

        if (!code || !state) {
            setError("Brak kodu autoryzacji.");
            return;
        }

        let cancelled = false;

        authService
            .completeLogin(code, state)
            .then(() => {
                if (!cancelled) {
                    window.dispatchEvent(new CustomEvent("tripify:session-changed"));
                    router.replace("/");
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    setError(err.message);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [router]);

    return (
        <main className="grid min-h-[60vh] place-items-center px-4">
            <section className="w-full max-w-sm rounded-lg border border-outline bg-panel p-6 text-center shadow-panel">
                {error ? (
                    <>
                        <h1 className="text-xl font-bold text-danger">Nie udało się zalogować</h1>
                        <p className="mt-2 text-sm text-muted">{error}</p>
                        <button
                            type="button"
                            onClick={() => router.replace("/")}
                            className="mt-5 h-11 w-full rounded-md bg-primary px-4 font-semibold text-main transition hover:opacity-90"
                        >
                            Wróć na stronę główną
                        </button>
                    </>
                ) : (
                    <>
                        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-outline border-t-primary" />
                        <p className="mt-4 text-sm text-muted">Trwa logowanie...</p>
                    </>
                )}
            </section>
        </main>
    );
}
