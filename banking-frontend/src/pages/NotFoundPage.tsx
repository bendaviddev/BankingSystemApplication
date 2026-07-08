import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="notfound-page">
      <div className="notfound-code">404</div>
      <h1>Page not found</h1>
      <p className="muted">The page you're looking for doesn't exist or has moved.</p>
      <Link to="/" className="btn btn-primary">
        Back to home
      </Link>
    </div>
  );
}
