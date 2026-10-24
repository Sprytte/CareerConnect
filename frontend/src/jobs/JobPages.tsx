import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useSession } from '../auth/session'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import JobForm from './JobForm'
import { jobRequest, type Job } from './jobApi'
import '../styles/App.css'
import '../styles/Jobs.css'

function JobLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="site-shell">
      <SiteHeader />
      <div className="jobs-banner">
        <div className="jobs-banner-inner">
          <span className="section-kicker">CAREERCONNECT / RECRUITING</span>
          <h1>Manage your opportunities.</h1>
          <p>Create, edit and manage your job postings.</p>
        </div>
      </div>
      <main className="jobs-page">{children}</main>
      <SiteFooter />
    </div>
  )
}

export function NewJobPage() {
  const session = useSession()
  const [saved, setSaved] = useState(false)

  return (
    <JobLayout>
      <p className="job-links">
        <Link to="/">Home</Link>
        <Link to="/account/jobs">My postings</Link>
      </p>

      {session.status !== 'signed-in' ? (
        <p>Please sign in to create a job.</p>
      ) : saved ? (
        <p>
          Job created. <Link to="/account/jobs">View your postings</Link>
        </p>
      ) : (
        <JobForm onSaved={() => setSaved(true)} />
      )}
    </JobLayout>
  )
}

export function MyJobsPage() {
  const session = useSession()
  const [jobs, setJobs] = useState<Job[]>([])
  const [editingJob, setEditingJob] = useState<Job | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  async function loadJobs() {
    setLoading(true)
    setError('')

    try {
      const result = await jobRequest<Job[]>('/api/v1/account/job-postings')
      setJobs(result)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load jobs.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (session.status !== 'signed-in') return

    let active = true

    jobRequest<Job[]>('/api/v1/account/job-postings')
      .then(result => {
        if (active) setJobs(result)
      })
      .catch(err => {
        if (active) {
          setError(err instanceof Error ? err.message : 'Could not load jobs.')
        }
      })

    return () => {
      active = false
    }
  }, [session.status])

  async function deleteJob(job: Job) {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${job.jobName}"? This cannot be undone.`
    )

    if (!confirmed) return

    setDeletingId(job.id)
    setError('')

    try {
      await jobRequest(`/api/v1/job-postings/${job.id}`, {
        method: 'DELETE',
      })

      setJobs(current => current.filter(item => item.id !== job.id))

      if (editingJob?.id === job.id) {
        setEditingJob(null)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete job.')
    } finally {
      setDeletingId(null)
    }
  }

  async function handleEdited() {
    setEditingJob(null)
    await loadJobs()
  }

  return (
    <JobLayout>
      <div className="jobs-heading">
        <div>
          <span className="section-kicker">YOUR WORKSPACE</span>
          <h2>My job postings</h2>
        </div>
        <Link className="job-button" to="/jobs/new">
          Create a job
        </Link>
      </div>

      <p className="job-links">
        <Link to="/">Home</Link>
      </p>

      {session.status !== 'signed-in' ? (
        <p>Please sign in to view your jobs.</p>
      ) : (
        <>
          {error && <p role="alert" className="job-error">{error}</p>}

          {loading && <p>Loading job postings...</p>}

          {jobs.map(job => (
            <article className="job-card" key={job.id}>
              {editingJob?.id === job.id ? (
                <JobForm
                  key={job.id}
                  job={job}
                  onSaved={handleEdited}
                  onCancel={() => setEditingJob(null)}
                />
              ) : (
                <>
                  <h2>{job.jobName}</h2>
                  <p>{job.company} · {job.location}</p>
                  <p>{job.description}</p>
                  <p>{job.category} · {job.salary}</p>
                  <p>
                    Deadline: {new Date(job.deadline).toLocaleString()}
                  </p>

                  <div className="job-actions">
                    <button
                      type="button"
                      onClick={() => setEditingJob(job)}
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      disabled={deletingId === job.id}
                      onClick={() => deleteJob(job)}
                    >
                      {deletingId === job.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </>
              )}
            </article>
          ))}

          {!loading && !error && jobs.length === 0 && (
            <p>You have not created any job postings.</p>
          )}
        </>
      )}
    </JobLayout>
  )
}