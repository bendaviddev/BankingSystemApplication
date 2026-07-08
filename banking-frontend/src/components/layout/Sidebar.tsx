import { NavLink } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

const NAV_ITEMS = [
  { to: "/app", label: "Dashboard", icon: "⌂", end: true },
  { to: "/app/accounts", label: "Accounts", icon: "▤" },
  { to: "/app/transfer", label: "Transfer", icon: "⇄" },
  { to: "/app/transactions", label: "Transactions", icon: "≡" },
  { to: "/app/analytics", label: "Analytics", icon: "◔" },
  { to: "/app/alerts", label: "Alerts", icon: "◈" },
  { to: "/app/settings", label: "Settings", icon: "⚙" },
];

export function Sidebar({ open, onClose }: SidebarProps) {
  const { session } = useAuth();

  return (
    <>
      {open && <div className="sidebar-scrim" onClick={onClose} aria-hidden="true" />}
      <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="sidebar-brand">
          <div className="brand-icon">🏦</div>
          <span>Ben Banking</span>
        </div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={onClose}
              className={({ isActive }) => `sidebar-link ${isActive ? "active" : ""}`}
            >
              <span className="sidebar-icon" aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
          {session?.role === "ADMIN" && (
            <NavLink
              to="/app/admin"
              onClick={onClose}
              className={({ isActive }) => `sidebar-link ${isActive ? "active" : ""}`}
            >
              <span className="sidebar-icon" aria-hidden="true">★</span>
              Admin
            </NavLink>
          )}
        </nav>
        <div className="sidebar-footer">
          <span className="demo-tag">Demo project — no real money</span>
        </div>
      </aside>
    </>
  );
}
