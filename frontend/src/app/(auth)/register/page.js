"use client";

import React from 'react';
import Link from 'next/link';
import { FaUser, FaEnvelope, FaLock, FaGlobe } from 'react-icons/fa';
import AuthCard from '@/components/AuthCard';

export default function RegisterPage() {
  return (
    <main className="min-h-screen w-full flex items-center justify-center p-4 bg-cover bg-center bg-no-repeat relative" 
          style={{ backgroundImage: "url('/auth-bg.png')" }}>
      {/* Overlay to ensure readability */}
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]"></div>
      
      <div className="relative z-10 w-full flex justify-center">
        <AuthCard 
          title="Create Account" 
          subtitle="Join our community of travelers"
        >
          <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2 text-left">
                <label className="text-sm font-medium text-[var(--color-primary)] ml-1">Full Name</label>
                <div className="relative group">
                  <FaUser className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-muted)] group-focus-within:text-[var(--color-primary)] transition-colors" />
                  <input 
                    type="text" 
                    placeholder="John Doe"
                    className="w-full pl-12 pr-4 py-3 bg-[var(--color-main)] border border-[var(--color-outline)] rounded-2xl focus:ring-2 focus:ring-[var(--color-glow)] focus:border-transparent outline-none transition-all text-[var(--color-primary)]"
                  />
                </div>
              </div>
              <div className="space-y-2 text-left">
                <label className="text-sm font-medium text-[var(--color-primary)] ml-1">Email</label>
                <div className="relative group">
                  <FaEnvelope className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-muted)] group-focus-within:text-[var(--color-primary)] transition-colors" />
                  <input 
                    type="email" 
                    placeholder="john@example.com"
                    className="w-full pl-12 pr-4 py-3 bg-[var(--color-main)] border border-[var(--color-outline)] rounded-2xl focus:ring-2 focus:ring-[var(--color-glow)] focus:border-transparent outline-none transition-all text-[var(--color-primary)]"
                  />
                </div>
              </div>
            </div>
            
            <div className="space-y-2 text-left">
              <label className="text-sm font-medium text-[var(--color-primary)] ml-1">Password</label>
              <div className="relative group">
                <FaLock className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-muted)] group-focus-within:text-[var(--color-primary)] transition-colors" />
                <input 
                  type="password" 
                  placeholder="••••••••"
                  className="w-full pl-12 pr-4 py-3 bg-[var(--color-main)] border border-[var(--color-outline)] rounded-2xl focus:ring-2 focus:ring-[var(--color-glow)] focus:border-transparent outline-none transition-all text-[var(--color-primary)]"
                />
              </div>
            </div>

            <div className="space-y-2 text-left">
              <label className="text-sm font-medium text-[var(--color-primary)] ml-1">Confirm Password</label>
              <div className="relative group">
                <FaLock className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-muted)] group-focus-within:text-[var(--color-primary)] transition-colors" />
                <input 
                  type="password" 
                  placeholder="••••••••"
                  className="w-full pl-12 pr-4 py-3 bg-[var(--color-main)] border border-[var(--color-outline)] rounded-2xl focus:ring-2 focus:ring-[var(--color-glow)] focus:border-transparent outline-none transition-all text-[var(--color-primary)]"
                />
              </div>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <input type="checkbox" id="terms" className="w-4 h-4 rounded border-[var(--color-outline)] text-[var(--color-primary)] focus:ring-[var(--color-glow)] cursor-pointer" />
              <label htmlFor="terms" className="text-xs text-[var(--color-muted)] cursor-pointer">
                I agree to the <span className="text-[var(--color-primary)] hover:underline">Terms of Service</span> and <span className="text-[var(--color-primary)] hover:underline">Privacy Policy</span>
              </label>
            </div>

            <button 
              type="submit"
              className="w-full py-3 bg-[var(--color-primary)] text-[var(--color-panel)] font-semibold rounded-2xl shadow-[var(--shadow-glow)] hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 cursor-pointer"
            >
              Create Account
            </button>
          </form>

          <p className="text-center text-sm text-[var(--color-muted)] pt-2">
            Already have an account?{' '}
            <Link href="/login" className="text-[var(--color-primary)] font-semibold hover:underline">
              Sign in
            </Link>
          </p>
        </AuthCard>
      </div>
    </main>
  );
}
