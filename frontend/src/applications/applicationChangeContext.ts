import { createContext } from 'react'

// Lets an application card report a saved edit or a withdrawal to the list
// that owns the data, which then re-reads it from the server.
export type ApplicationChangeHandler = (message: string) => void

export const ApplicationChangeContext = createContext<ApplicationChangeHandler | undefined>(undefined)
