// Public configuration only. Auth0 client secrets belong on the backend.
export const backendUrl = (
  import.meta.env.VITE_BACKEND_URL || "http://localhost:8080"
).replace(/\/$/, "");