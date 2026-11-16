import { defaultSorting } from "./postingApplicationsView"

export type ApplicationStatus = 'Applied' | 'Interview' | 'Offer' | 'Reject'
export type Decision = Exclude<ApplicationStatus, 'Applied'>

export type Application = {
  id: number
  jobPostingId: number
  userId: string
  applicantName?: string | null
  resumeId: number | null
  status: ApplicationStatus
  appliedAt: string
  interviewTimeSlot: string | null
}

export const decisions: Decision[] = ['Interview', 'Offer', 'Reject']

export function isValidJobId(jobId: string | undefined): boolean {
  return jobId !== undefined && /^[1-9]\d*$/.test(jobId)
}

export function defaultDecision(status: ApplicationStatus): Decision {
  return status === 'Applied' ? 'Interview' : status
}

export function canUpdateStatus(
  application: Application | undefined,
  decision: Decision | null,
  interviewTimeSlot: string,
  saving: boolean
): boolean {
  const time = new Date(interviewTimeSlot)
  return application !== undefined && !saving && ((application.status !== decision && ((decision === 'Interview' &&
      isTimeValid(time) && application.interviewTimeSlot !== interviewTimeSlot) || decision !== 'Interview')) || 
      decision === 'Interview' && isTimeValid(time) && application.interviewTimeSlot !== interviewTimeSlot)
}

function isTimeValid(time: Date): boolean {
 return Number.isFinite(time.getTime()) && time.getTime() > Date.now()
}

export function updateApplication(
  applications: Application[],
  updated: Application
): Application[] {
  return applications.map(application =>
    application.id === updated.id
      ? { ...application, ...updated, applicantName: updated.applicantName ?? application.applicantName }
      : application
  )
}

export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : 'Something went wrong. Please try again.'
}