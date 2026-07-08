import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { api } from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";

interface TopbarProps {
  onMenuClick: () => void;
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { session, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function poll() {
      try {
        const res = await api.getAlerts();
        if (!cancelled) setUnreadCount(res.unreadCount);
      } catch {
        /* ignore transient errors, keep last known count */
      }
    }
    void poll();
    const interval = setInterval(poll, 60000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [location.pathname]);

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <header className="topbar">
      <button type="button" className="hamburger" aria-label="Open menu" onClick={onMenuClick}>
        ☰
      </button>

      <div className="topbar-right">
        <button
          type="button"
          className="icon-button"
          aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
          onClick={toggleTheme}
        >
          {theme === "dark" ? "☀" : "☾"}
        </button>

        <Link to="/app/alerts" className="icon-button bell-button" aria-label={`Alerts, ${unreadCount} unread`}>
          🔔
          {unreadCount > 0 && <span className="bell-badge">{unreadCount > 9 ? "9+" : unreadCount}</span>}
        </Link>

        <div className="user-menu">
          <button
            type="button"
            className="user-chip"
            onClick={() => setMenuOpen((v) => !v)}
            aria-haspopup="true"
            aria-expanded={menuOpen}
          >
            {session?.username}
            {session?.role === "ADMIN" && " · Admin"}
          </button>
          {menuOpen && (
            <div className="user-menu-dropdown" onMouseLeave={() => setMenuOpen(false)}>
              <Link to="/app/settings" onClick={() => setMenuOpen(false)}>
                Settings
              </Link>
              <button type="button" onClick={handleLogout}>
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
