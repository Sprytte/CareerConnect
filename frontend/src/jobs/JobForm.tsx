import { useState, type FormEvent } from 'react'
import { jobRequest, type Job, type JobInput } from './jobApi'

const emptyForm: JobInput = {
  jobName: '',
  company: '',
  description: '',
  location: '',
  category: '',
  salary: '',
  deadline: '',
}

type JobFormProps = {
  job?: Job
  onSaved: () => void
  onCancel?: () => void
}

function toFormValues(job?: Job): JobInput {
  if (!job) return { ...emptyForm }

  return {
    jobName: job.jobName,
    company: job.company,
    description: job.description,
    location: job.location,
    category: job.category,
    salary: job.salary,
    deadline: job.deadline.slice(0, 16),
  }
}

export default function JobForm({
  job,
  onSaved,
  onCancel,
}: JobFormProps) {
  const [values, setValues] = useState<JobInput>(() => toFormValues(job))
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const editing = Boolean(job)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')

    const deadline = new Date(values.deadline)

    if (!Number.isFinite(deadline.getTime()) || deadline.getTime() <= Date.now()) {
      setError('Deadline must be in the future.')
      return
    }

    setSaving(true)

    try {
      await jobRequest(
        editing
          ? `/api/v1/job-postings/${job!.id}`
          : '/api/v1/job-postings',
        {
          method: editing ? 'PUT' : 'POST',
          body: JSON.stringify({
            ...values,
            deadline: `${values.deadline}:00`,
          }),
        }
      )

      onSaved()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save the job.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="job-form" onSubmit={submit}>
      <h2>{editing ? 'Edit job posting' : 'Create job posting'}</h2>

      {(
        [
          'jobName',
          'company',
          'location',
          'category',
          'salary',
          'deadline',
          'description',
        ] as const
      ).map(field => (
        <label key={field}>
          {field === 'jobName'
            ? 'Job title'
            : field === 'deadline'
              ? 'Application deadline'
              : field.charAt(0).toUpperCase() + field.slice(1)}

          {field === 'description' ? (
            <textarea
              required
              value={values[field]}
              onChange={e =>
                setValues({ ...values, [field]: e.target.value })
              }
            />
          ) : (
            <input
              required
              type={field === 'deadline' ? 'datetime-local' : 'text'}
              value={values[field]}
              onChange={e =>
                setValues({ ...values, [field]: e.target.value })
              }
            />
          )}
        </label>
      ))}

      {error && (
        <p role="alert" className="job-error">
          {error}
        </p>
      )}

      <div className="job-actions">
        <button type="submit" disabled={saving}>
          {saving
            ? 'Saving...'
            : editing
              ? 'Save changes'
              : 'Create job'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={saving}
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}