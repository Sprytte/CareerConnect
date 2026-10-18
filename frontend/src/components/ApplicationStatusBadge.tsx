import { isApplicationStatus } from '../applications/applications'

type Props = { status: string | null }

export default function ApplicationStatusBadge({ status }: Props) {
  const known = isApplicationStatus(status)
  return (
    <span className={`application-status application-status--${known ? status.toLowerCase() : 'unknown'}`}>
      <span className="application-status-dot" aria-hidden="true" />
      {known ? status : 'Status unavailable'}
    </span>
  )
}
