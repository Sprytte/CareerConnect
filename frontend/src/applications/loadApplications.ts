import type { ApplicationsLoader } from './applications'

// Main currently has no application records or status endpoint. Keep this
// boundary explicit until the backend contract exists; never substitute sample
// applications or a guessed URL. A real adapter must use the authenticated
// session, forward cancellation, and map errors and records to the UI model.
export const loadApplications: ApplicationsLoader = async () => ({ status: 'unavailable' })
