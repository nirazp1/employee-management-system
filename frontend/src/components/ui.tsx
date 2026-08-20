import type { ButtonHTMLAttributes, InputHTMLAttributes, LabelHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";

// Small shared UI kit: pill radius on every button/input/tag, soft-meadow
// card surfaces, no shadows, yellow reserved for the one primary action per view.

export function Button({
  variant = "primary",
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "secondary" | "ghost" | "danger" }) {
  const base = "inline-flex items-center justify-center gap-2 rounded-[1440px] px-6 py-3 text-body font-medium transition-opacity disabled:opacity-50 disabled:cursor-not-allowed";
  const variants: Record<string, string> = {
    primary: "bg-hi-yellow text-deep-ink hover:opacity-90",
    secondary: "bg-deep-ink text-white hover:opacity-90",
    ghost: "bg-transparent text-deep-ink border border-deep-ink hover:bg-soft-meadow",
    danger: "bg-white text-deep-ink border border-deep-ink hover:bg-soft-meadow",
  };
  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />;
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-[24px] bg-soft-meadow p-6 sm:p-8 ${className}`}>{children}</div>;
}

export function Input({ className = "", ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={`w-full rounded-[1440px] border border-deep-ink bg-white px-5 py-3 text-body text-deep-ink placeholder:text-slate focus:outline-none focus:ring-2 focus:ring-hi-yellow ${className}`}
      {...props}
    />
  );
}

export function Select({ className = "", children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={`w-full rounded-[1440px] border border-deep-ink bg-white px-5 py-3 text-body text-deep-ink focus:outline-none focus:ring-2 focus:ring-hi-yellow ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}

export function Label({ className = "", ...props }: LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={`mb-1 block text-caption font-medium uppercase tracking-[-0.02em] text-slate ${className}`}
      {...props}
    />
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <Label>{label}</Label>
      {children}
    </div>
  );
}

const badgeTones: Record<string, string> = {
  neutral: "bg-white text-deep-ink border border-deep-ink/20",
  positive: "bg-soft-meadow text-deep-ink border border-deep-ink/10",
  attention: "bg-hi-yellow text-deep-ink",
  strong: "bg-deep-ink text-white",
};

export function Badge({ children, tone = "neutral" }: { children: ReactNode; tone?: keyof typeof badgeTones }) {
  return (
    <span
      className={`inline-flex items-center rounded-[1440px] px-3 py-1 text-caption font-medium uppercase tracking-[-0.02em] ${badgeTones[tone]}`}
    >
      {children}
    </span>
  );
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle?: string; action?: ReactNode }) {
  return (
    <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="font-display text-heading text-deep-ink">{title}</h1>
        {subtitle && <p className="mt-1 text-body text-slate">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Spinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-deep-ink/20 border-t-deep-ink" />
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="rounded-[24px] border border-deep-ink/20 bg-white px-5 py-4 text-body-sm text-deep-ink">
      {message}
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return <p className="py-12 text-center text-body text-slate">{message}</p>;
}
