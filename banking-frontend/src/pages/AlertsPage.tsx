import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";
import type { Alert } from "../types";
import { ALERT_SEVERITY_ICON, ALERT_SEVERITY_TONE, ALERT_TYPE_LABELS, formatDateTime } from "../lib/format";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Spinner } from "../components/ui/Spinner";
import { ErrorState } from "../components/ui/ErrorState";
import { EmptyState } from "../components/ui/EmptyState";
import { useToast } from "../context/ToastContext";

export function AlertsPage() {
  const { toast } = useToast();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.getAlerts();
      setAlerts(res.items);
      setUnreadCount(res.unreadCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load alerts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleMarkRead(alert: Alert) {
    if (alert.read) return;
    setAlerts((prev) => prev.map((a) => (a.alertId === alert.alertId ? { ...a, read: true } : a)));
    setUnreadCount((c) => Math.max(0, c - 1));
    try {
      await api.markAlertRead(alert.alertId);
    } catch {
      toast("Could not mark alert as read.", "error");
      void load();
    }
  }

  async function handleMarkAllRead() {
    const previous = alerts;
    setAlerts((prev) => prev.map((a) => ({ ...a, read: true })));
    setUnreadCount(0);
    try {
      await api.markAllAlertsRead();
      toast("All alerts marked as read.", "success");
    } catch {
      setAlerts(previous);
      toast("Could not mark alerts as read.", "error");
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Alerts</h1>
          <p className="muted">{unreadCount} unread</p>
        </div>
        {unreadCount > 0 && (
          <Button variant="secondary" onClick={handleMarkAllRead}>
            Mark all as read
          </Button>
        )}
      </div>

      <Card>
        {loading ? (
          <Spinner label="Loading alerts…" />
        ) : error ? (
          <ErrorState message={error} onRetry={load} />
        ) : alerts.length === 0 ? (
          <EmptyState icon="✅" title="No alerts" description="You're all caught up." />
        ) : (
          <ul className="alert-list">
            {alerts.map((a) => (
              <li key={a.alertId}>
                <button
                  type="button"
                  className={`alert-list-item ${a.read ? "" : "unread"}`}
                  onClick={() => handleMarkRead(a)}
                >
                  <span className={`alert-icon tone-${ALERT_SEVERITY_TONE[a.severity]}`} aria-hidden="true">
                    {ALERT_SEVERITY_ICON[a.severity]}
                  </span>
                  <div className="alert-list-body">
                    <div className="alert-list-title">
                      {ALERT_TYPE_LABELS[a.alertType]}
                      {!a.read && <span className="unread-dot" aria-label="Unread" />}
                    </div>
                    <div className="muted">{a.message}</div>
                    <div className="alert-list-date">{formatDateTime(a.createdAt)}</div>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
