import type { HTMLAttributes, ReactNode } from "react";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export function Card({ children, className = "", ...rest }: CardProps) {
  return (
    <div className={`card ${className}`} {...rest}>
      {children}
    </div>
  );
}

export function CardHeader({ children, className = "", ...rest }: CardProps) {
  return (
    <div className={`card-header ${className}`} {...rest}>
      {children}
    </div>
  );
}
