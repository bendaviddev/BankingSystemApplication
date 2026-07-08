import type { ToastTone } from "../../context/ToastContext";

export interface ToastItemData {
  id: number;
  message: string;
  tone: ToastTone;
}

interface ToastStackProps {
  items: ToastItemData[];
  onDismiss: (id: number) => void;
}

export function ToastStack({ items, onDismiss }: ToastStackProps) {
  return (
    <div className="toast-stack" role="status" aria-live="polite">
      {items.map((item) => (
        <div key={item.id} className={`toast toast-${item.tone}`}>
          <span>{item.message}</span>
          <button
            type="button"
            className="toast-dismiss"
            aria-label="Dismiss notification"
            onClick={() => onDismiss(item.id)}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
