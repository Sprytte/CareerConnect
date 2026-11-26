import '../styles/SavedJobs.css'

type Props = {
  jobName: string
  saved: boolean
  pending: boolean
  disabled?: boolean
  onToggle: () => void
}

// A toggle button: the accessible name stays the same and aria-pressed
// carries the state, while the visible text shows it to sighted users.
export default function SaveJobButton({ jobName, saved, pending, disabled = false, onToggle }: Props) {
  return (
    <button
      className={`save-job-button${saved ? ' save-job-button--saved' : ''}`}
      type="button"
      aria-pressed={saved}
      aria-label={`Save ${jobName}`}
      disabled={pending || disabled}
      onClick={onToggle}
    >
      <span aria-hidden="true">{saved ? '★' : '☆'}</span>
      {pending ? (saved ? 'Removing…' : 'Saving…') : saved ? 'Saved' : 'Save job'}
    </button>
  )
}
