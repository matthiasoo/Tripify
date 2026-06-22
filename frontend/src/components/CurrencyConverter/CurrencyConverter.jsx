"use client";

import { useState, useEffect, useRef } from "react";
import { FaExchangeAlt, FaChevronDown } from "react-icons/fa";

// Quick local service mimicking api.js style
const CONVERTER_API_URL = process.env.NEXT_PUBLIC_CONVERTER_URL || "http://localhost:8083";

async function fetchCurrencies() {
    const res = await fetch(`${CONVERTER_API_URL}/api/v1/currencies`);
    if (!res.ok) throw new Error("Failed to fetch currencies");
    return res.json();
}

async function convertCurrency(from, to, amount) {
    const res = await fetch(`${CONVERTER_API_URL}/api/v1/convert`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ from, to, amount: parseFloat(amount) }),
    });
    
    if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || "Błąd konwersji");
    }
    return res.json();
}

function CustomSelect({ value, options = [], onChange, className }) {
    const [isOpen, setIsOpen] = useState(false);
    const ref = useRef(null);

    useEffect(() => {
        function handleClickOutside(event) {
            if (ref.current && !ref.current.contains(event.target)) {
                setIsOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className={`relative ${className}`} ref={ref}>
            <div 
                className="flex items-center justify-between h-full px-3 py-2 cursor-pointer bg-white/5 hover:bg-white/10 transition-colors"
                onClick={() => setIsOpen(!isOpen)}
            >
                <span className="font-medium text-sm">{value}</span>
                <FaChevronDown className={`w-3 h-3 ml-2 transition-transform duration-200 opacity-70 ${isOpen ? 'rotate-180' : ''}`} />
            </div>
            
            {isOpen && (
                <div className="absolute top-[calc(100%+8px)] left-0 min-w-[120%] max-h-48 overflow-y-auto bg-panel border border-outline rounded-xl shadow-2xl z-[100] flex flex-col backdrop-blur-xl
                                [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-white/20 [&::-webkit-scrollbar-thumb]:rounded-full">
                    {options.map(opt => (
                        <div 
                            key={opt}
                            className={`px-4 py-2 text-sm cursor-pointer transition-colors hover:bg-white/10 ${opt === value ? 'text-[var(--color-glow)] font-bold bg-white/5' : 'text-white/80'}`}
                            onClick={() => {
                                onChange(opt);
                                setIsOpen(false);
                            }}
                        >
                            {opt}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

export default function CurrencyConverter() {
    const [currencies, setCurrencies] = useState([]);
    const [from, setFrom] = useState("EUR");
    const [to, setTo] = useState("PLN");
    const [amount, setAmount] = useState(1);
    const [result, setResult] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        let mounted = true;
        fetchCurrencies()
            .then((data) => {
                if (mounted) {
                    setCurrencies(data);
                    if (!data.includes(from)) setFrom(data[0] || "PLN");
                    if (!data.includes(to)) setTo(data[1] || "EUR");
                }
            })
            .catch(() => {
                if (mounted) setError("Brak dostępu do API kursów walut.");
            });
        return () => { mounted = false; };
    }, []);

    useEffect(() => {
        if (!amount || amount <= 0) {
            setResult(null);
            setError(null);
            return;
        }

        const debounce = setTimeout(() => {
            handleConvert();
        }, 500);

        return () => clearTimeout(debounce);
    }, [from, to, amount]);

    const handleConvert = async () => {
        if (!amount || amount <= 0) return;
        setLoading(true);
        setError(null);
        try {
            const data = await convertCurrency(from, to, amount);
            setResult(data);
        } catch (err) {
            setError(err.message);
            setResult(null);
        } finally {
            setLoading(false);
        }
    };

    const handleSwap = () => {
        setFrom(to);
        setTo(from);
    };

    return (
        <div className="w-full rounded-2xl bg-panel border border-outline relative flex flex-col justify-between p-5 text-white/[0.97]
                        isolate transform-gpu transition-all duration-350 ease-[cubic-bezier(0.16,1,0.3,1)]
                        hover:shadow-glow hover:-translate-y-1">
            <div className="absolute inset-0 z-0 bg-gradient-to-br from-white/[0.03] to-transparent pointer-events-none rounded-2xl" />
            
            <div className="relative z-10 flex flex-col gap-4">
                <h3 className="font-bold text-xl tracking-tight leading-tight drop-shadow-[0_2px_8px_oklch(0_0_0/0.4)]">
                    Przelicznik Walut (NBP)
                </h3>

                {error && (
                    <div className="w-full rounded-lg bg-danger-panel/50 border border-danger-outline flex items-center justify-center text-danger text-sm p-2">
                        {error}
                    </div>
                )}

                <div className="flex items-center justify-between gap-3 relative">
                    {/* From Input Group */}
                    <div className="flex-1 flex flex-col gap-1.5">
                        <label className="text-xs text-white/50 pl-1 uppercase tracking-wider font-semibold">Z</label>
                        <div className="flex bg-black/20 rounded-xl border border-white/10 focus-within:border-white/30 focus-within:ring-1 focus-within:ring-white/20 transition-all">
                            <input 
                                type="number" 
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                                min="0.01"
                                step="0.01"
                                className="w-full bg-transparent outline-none px-3 py-2 text-sm font-medium"
                                placeholder="Kwota"
                            />
                            <CustomSelect 
                                value={from} 
                                options={currencies}
                                onChange={setFrom}
                                className="border-l border-white/10 rounded-r-xl"
                            />
                        </div>
                    </div>

                    {/* Swap Button */}
                    <button 
                        onClick={handleSwap}
                        className="mt-5 p-2 rounded-full bg-white/10 hover:bg-white/20 border border-white/5 transition-colors cursor-pointer self-center"
                        title="Zamień waluty"
                    >
                        <FaExchangeAlt className="w-3.5 h-3.5 opacity-80" />
                    </button>

                    {/* To Input Group */}
                    <div className="flex-1 flex flex-col gap-1.5">
                        <label className="text-xs text-white/50 pl-1 uppercase tracking-wider font-semibold">Na</label>
                        <div className="flex bg-black/20 rounded-xl border border-white/10 focus-within:border-white/30 focus-within:ring-1 focus-within:ring-white/20 transition-all">
                            <CustomSelect 
                                value={to} 
                                options={currencies}
                                onChange={setTo}
                                className="w-full rounded-xl"
                            />
                        </div>
                    </div>
                </div>

                {/* Result Section */}
                <div className="mt-2 h-16 flex flex-col items-center justify-center bg-black/15 rounded-xl border border-white/5">
                    {loading ? (
                        <div className="animate-pulse w-24 h-6 bg-white/10 rounded-md" />
                    ) : result ? (
                        <>
                            <div className="font-bold text-2xl tracking-tight text-white drop-shadow-md">
                                {result.result.toFixed(2)} <span className="text-lg text-white/70">{result.to}</span>
                            </div>
                            <div className="text-[0.65rem] text-white/40 tracking-wider uppercase">
                                1 {result.from} = {result.exchangeRate.toFixed(4)} {result.to}
                            </div>
                        </>
                    ) : (
                        <div className="text-sm text-white/40 italic">Wprowadź kwotę...</div>
                    )}
                </div>
            </div>
        </div>
    );
}
