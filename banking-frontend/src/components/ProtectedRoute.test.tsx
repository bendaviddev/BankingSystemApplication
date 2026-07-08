import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { ProtectedRoute } from "./ProtectedRoute";

const useAuthMock = vi.fn();

vi.mock("../context/AuthContext", () => ({
  useAuth: () => useAuthMock(),
}));

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={["/app"]}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route
          path="/app"
          element={
            <ProtectedRoute>
              <div>Protected content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  it("redirects to /login when there is no session", () => {
    useAuthMock.mockReturnValue({ session: null, loading: false });
    renderProtected();
    expect(screen.getByText("Login page")).toBeInTheDocument();
    expect(screen.queryByText("Protected content")).not.toBeInTheDocument();
  });

  it("shows a loading state while the session is hydrating", () => {
    useAuthMock.mockReturnValue({ session: { token: "t" }, loading: true });
    renderProtected();
    expect(screen.getByText(/loading your session/i)).toBeInTheDocument();
  });

  it("renders children once authenticated", () => {
    useAuthMock.mockReturnValue({ session: { token: "t" }, loading: false });
    renderProtected();
    expect(screen.getByText("Protected content")).toBeInTheDocument();
  });
});
