import React from 'react';

const AuthCard = ({ title, subtitle, children }) => {
  return (
    <div className="w-full max-w-md p-8 space-y-6 bg-[var(--color-panel)] rounded-3xl shadow-[var(--shadow-panel)] border border-[var(--color-outline)] animate-[var(--animate-scale-up)] backdrop-blur-xl bg-opacity-80">
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-bold tracking-tight text-[var(--color-primary)]">
          {title}
        </h1>
        {subtitle && (
          <p className="text-[var(--color-muted)] text-sm">
            {subtitle}
          </p>
        )}
      </div>
      {children}
    </div>
  );
};

export default AuthCard;
