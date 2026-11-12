export const USER_ROLES = ['JOB_SEEKER', 'RECRUITER'] as const

export type UserRole = (typeof USER_ROLES)[number]

export const roleLabels: Record<UserRole, string> = {
  JOB_SEEKER: 'Job seeker',
  RECRUITER: 'Recruiter',
}

export const roleDescriptions: Record<UserRole, string> = {
  JOB_SEEKER:
    'Search and apply for jobs, manage your resumes, and track your applications.',
  RECRUITER:
    'Create and manage job postings, and review the people who apply.',
}

export function isUserRole(value: unknown): value is UserRole {
  return (
    typeof value === 'string' &&
    (USER_ROLES as readonly string[]).includes(value)
  )
}
