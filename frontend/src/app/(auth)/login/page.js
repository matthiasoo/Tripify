"use client";

import React from 'react';
import Link from 'next/link';
import { FaGoogle, FaFacebook, FaEnvelope, FaLock } from 'react-icons/fa';
import AuthCard from '@/components/AuthCard';

export default function LoginPage() {
  return (
    <main className="min-h-screen w-full flex items-center justify-center p-4 bg-cover bg-center bg-no-repeat relative" 
          style={{ backgroundImage: "url('/auth-bg.png')" }}>
      {/* Overlay to ensure readability */}
      <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]"></div>
      
      <div className="relative z-10 w-full flex justify-center">
        <AuthCard 
          title="Welcome Back" 
          subtitle="Continue your journey with Tripify"
        >
          <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
            <div className="space-y-2 text-left">
              <label className="text-sm font-medium text-[var(--color-primary)] ml-1">Email</label>
              <div className="relative group">
                <FaEnvelope className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--color-muted)] group-focus-within:text-[var(--color-primary)] transition-colors" />
                <input 
                  type="email" 
                  placeholder="name@example.com"
                  className="w-full pl-12 pr-4 py-3 bg-[var(--color-main)] border border-[var(--color-outline)] rounded-2xl focus:ring-2 focus:ring-[var(--color-glow)] focus:border-transparent outline-none transition-all text-[var(--color-primary)]"
                />
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
              <div className="flex justify-end">
                <button type="button" className="text-xs text-[var(--color-muted)] hover:text-[var(--color-primary)] transition-colors">
                  Forgot password?
                </button>
              </div>
            </div>

            <button 
              type="submit"
              className="w-full py-3 bg-[var(--color-primary)] text-[var(--color-panel)] font-semibold rounded-2xl shadow-[var(--shadow-glow)] hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 cursor-pointer"
            >
              Sign In
            </button>

            <div className="relative flex items-center py-2">
              <div className="flex-grow border-t border-[var(--color-outline)]"></div>
              <span className="flex-shrink mx-4 text-[var(--color-muted)] text-xs uppercase tracking-wider">Or continue with</span>
              <div className="flex-grow border-t border-[var(--color-outline)]"></div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <button type="button" className="flex items-center justify-center gap-2 py-3 bg-[var(--color-panel)] border border-[var(--color-outline)] rounded-2xl hover:bg-[var(--color-main)] transition-all cursor-pointer">
                <FaGoogle className="text-red-500" />
                <span className="text-sm font-medium">Google</span>
              </button>
              <button type="button" className="flex items-center justify-center gap-2 py-3 bg-[var(--color-panel)] border border-[var(--color-outline)] rounded-2xl hover:bg-[var(--color-main)] transition-all cursor-pointer">
                <FaFacebook className="text-blue-600" />
                <span className="text-sm font-medium">Facebook</span>
              </button>
            </div>
          </form>

          <p className="text-center text-sm text-[var(--color-muted)] pt-2">
            Don&apos;t have an account?{' '}
            <Link href="/register" className="text-[var(--color-primary)] font-semibold hover:underline">
              Sign up
            </Link>
          </p>
        </AuthCard>
      </div>
    </main>
  );
}
