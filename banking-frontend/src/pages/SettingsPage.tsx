import { type FormEvent, useEffect, useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useToast } from "../context/ToastContext";
import { checkPasswordStrength } from "../lib/format";
import { Card } from "../components/ui/Card";
import { Button } from "../components/ui/Button";
import { Input } from "../components/ui/Input";

export function SettingsPage() {
  const { profile, refreshProfile } = useAuth();
  const { theme, setTheme } = useTheme();
  const { toast } = useToast();

  const [profileForm, setProfileForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phoneNumber: "",
    address: "",
  });
  const [profileSubmitting, setProfileSubmitting] = useState(false);
  const [profileError, setProfileError] = useState("");

  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordError, setPasswordError] = useState("");

  useEffect(() => {
    if (profile) {
      setProfileForm({
        firstName: profile.firstName ?? "",
        lastName: profile.lastName ?? "",
        email: profile.email ?? "",
        phoneNumber: profile.phoneNumber ?? "",
        address: profile.address ?? "",
      });
    }
  }, [profile]);

  async function handleProfileSubmit(e: FormEvent) {
    e.preventDefault();
    setProfileError("");
    setProfileSubmitting(true);
    try {
      await api.updateProfile(profileForm);
      await refreshProfile();
      toast("Profile updated.", "success");
    } catch (err) {
      setProfileError(err instanceof Error ? err.message : "Failed to update profile.");
    } finally {
      setProfileSubmitting(false);
    }
  }

  const strength = checkPasswordStrength(passwordForm.newPassword);

  async function handlePasswordSubmit(e: FormEvent) {
    e.preventDefault();
    setPasswordError("");

    if (!strength.valid) {
      setPasswordError("New password doesn't meet the strength requirements below.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirm) {
      setPasswordError("New password and confirmation don't match.");
      return;
    }

    setPasswordSubmitting(true);
    try {
      await api.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm({ currentPassword: "", newPassword: "", confirm: "" });
      toast("Password updated.", "success");
    } catch (err) {
      setPasswordError(err instanceof Error ? err.message : "Failed to update password.");
    } finally {
      setPasswordSubmitting(false);
    }
  }

  return (
    <div className="page">
      <h1>Settings</h1>

      <Card>
        <h2>Profile</h2>
        <form onSubmit={handleProfileSubmit} noValidate>
          {profileError && <div className="alert error">{profileError}</div>}
          <div className="field-row">
            <Input
              id="settings-firstName"
              label="First name"
              value={profileForm.firstName}
              onChange={(e) => setProfileForm((p) => ({ ...p, firstName: e.target.value }))}
              required
            />
            <Input
              id="settings-lastName"
              label="Last name"
              value={profileForm.lastName}
              onChange={(e) => setProfileForm((p) => ({ ...p, lastName: e.target.value }))}
              required
            />
          </div>
          <Input
            id="settings-email"
            label="Email"
            type="email"
            value={profileForm.email}
            onChange={(e) => setProfileForm((p) => ({ ...p, email: e.target.value }))}
          />
          <Input
            id="settings-phone"
            label="Phone"
            type="tel"
            value={profileForm.phoneNumber}
            onChange={(e) => setProfileForm((p) => ({ ...p, phoneNumber: e.target.value }))}
          />
          <Input
            id="settings-address"
            label="Address"
            value={profileForm.address}
            onChange={(e) => setProfileForm((p) => ({ ...p, address: e.target.value }))}
          />
          <Button type="submit" loading={profileSubmitting}>
            Save profile
          </Button>
        </form>
      </Card>

      <Card>
        <h2>Change password</h2>
        <form onSubmit={handlePasswordSubmit} noValidate>
          {passwordError && <div className="alert error">{passwordError}</div>}
          <Input
            id="settings-current-password"
            label="Current password"
            type="password"
            value={passwordForm.currentPassword}
            onChange={(e) => setPasswordForm((p) => ({ ...p, currentPassword: e.target.value }))}
            autoComplete="current-password"
            required
          />
          <Input
            id="settings-new-password"
            label="New password"
            type="password"
            value={passwordForm.newPassword}
            onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
            autoComplete="new-password"
            required
          />
          <ul className="password-requirements">
            <li className={strength.minLength ? "met" : ""}>8+ characters</li>
            <li className={strength.hasUppercase ? "met" : ""}>Uppercase letter</li>
            <li className={strength.hasNumber ? "met" : ""}>Number</li>
            <li className={strength.hasSpecial ? "met" : ""}>Special character</li>
          </ul>
          <Input
            id="settings-confirm-password"
            label="Confirm new password"
            type="password"
            value={passwordForm.confirm}
            onChange={(e) => setPasswordForm((p) => ({ ...p, confirm: e.target.value }))}
            autoComplete="new-password"
            required
          />
          <Button type="submit" loading={passwordSubmitting}>
            Update password
          </Button>
        </form>
      </Card>

      <Card>
        <h2>Appearance</h2>
        <div className="theme-radio-group" role="radiogroup" aria-label="Theme">
          <label className="theme-radio">
            <input
              type="radio"
              name="theme"
              value="light"
              checked={theme === "light"}
              onChange={() => setTheme("light")}
            />
            Light
          </label>
          <label className="theme-radio">
            <input
              type="radio"
              name="theme"
              value="dark"
              checked={theme === "dark"}
              onChange={() => setTheme("dark")}
            />
            Dark
          </label>
        </div>
      </Card>

      <Card className="demo-notice-card">
        <h2>About this app</h2>
        <p>
          Ben Banking is a demo project built for portfolio purposes. All accounts, balances, and transfers are
          simulated — no real money moves and no real payment rails are involved.
        </p>
      </Card>
    </div>
  );
}
