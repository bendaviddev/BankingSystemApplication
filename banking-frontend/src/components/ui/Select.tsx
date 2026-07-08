import type { ReactNode, SelectHTMLAttributes } from "react";

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  id: string;
  children: ReactNode;
  hint?: string;
}

export function Select({ label, id, children, hint, className = "", ...rest }: SelectProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <select id={id} className={className} aria-describedby={hint ? `${id}-hint` : undefined} {...rest}>
        {children}
      </select>
      {hint && (
        <p id={`${id}-hint`} className="field-hint">
          {hint}
        </p>
      )}
    </div>
  );
}
