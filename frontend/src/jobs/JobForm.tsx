import { useState, type FormEvent } from 'react'
import { jobRequest, type JobInput } from './jobApi'

const emptyForm: JobInput = { jobName: '', company: '', description: '', location: '', category: '', salary: '', deadline: '' }

export default function JobForm({ onSaved }: { onSaved: () => void }) {
  const [values, setValues] = useState<JobInput>(emptyForm)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (new Date(values.deadline).getTime() <= Date.now()) {
      setError('Deadline must be in the future.')
      return
    }
    setSaving(true)
    try {
      await jobRequest('/api/v1/job-postings', {
        method: 'POST',
        body: JSON.stringify({ ...values, deadline: `${values.deadline}:00` }),
      })
      onSaved()
    } catch (err) { setError(err instanceof Error ? err.message : 'Could not save the job.') }
    finally { setSaving(false) }
  }

  return <form className="job-form" onSubmit={submit}>
    <h2>Create job posting</h2>
    {(['jobName', 'company', 'location', 'category', 'salary', 'deadline', 'description'] as const).map(field =>
      <label key={field}>{field === 'jobName' ? 'Job title' : field === 'deadline' ? 'Application deadline' : field.charAt(0).toUpperCase() + field.slice(1)}
        {field === 'description' ? <textarea required value={values[field]} onChange={e => setValues({ ...values, [field]: e.target.value })} /> :
          <input required type={field === 'deadline' ? 'datetime-local' : 'text'} value={values[field]} onChange={e => setValues({ ...values, [field]: e.target.value })} />}
      </label>)}
    {error && <p role="alert" className="job-error">{error}</p>}
    <div className="job-actions"><button disabled={saving} type="submit">{saving ? 'Saving...' : 'Create job'}</button>
</div>
  </form>
}
