
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useSession } from '../auth/session'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import { jobRequest, type Job } from './jobApi'
import '../styles/App.css'
import '../styles/Jobs.css'
import '../styles/JobApplicants.css'

import {
  decisions,
  defaultDecision,
  canUpdateStatus,
  updateApplication,
  isValidJobId,
  errorMessage,
  type Application,
  type Decision,
} from './applicantStatus'

export default function JobApplicantsPage() {
  const { jobId } = useParams()
  const session = useSession()

  const [job, setJob] = useState<Job | null>(null)
  const [applications, setApplications] = useState<Application[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [decision, setDecision] = useState<Decision>('Interview')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const validJobId = isValidJobId(jobId)

      useEffect(() => {
    if (session.status !== 'signed-in' || !validJobId) {
      return
    }

    let active = true
    const controller = new AbortController()

    async function load() {
      setLoading(true)
      setError('')

      try {
        const [jobResult, applicationResult] = await Promise.all([
          jobRequest<Job>(
            `/api/v1/account/job-postings/${jobId}`,
            { signal: controller.signal }
          ),
          jobRequest<Application[]>(
            `/api/v1/account/job-postings/${jobId}/applications`,
            { signal: controller.signal }
          ),
        ])

        if (!active) return

        setJob(jobResult)
        setApplications(applicationResult)
      } catch (err) {
        if (active) setError(errorMessage(err))
      } finally {
        if (active) setLoading(false)
      }
    }

    void load()

    return () => {
      active = false
      controller.abort()
    }
  }, [jobId, session.status, validJobId])

  const selected = applications.find(
    application => application.id === selectedId
  )

  function selectApplication(application: Application) {
    if (saving) return

    setSelectedId(application.id)
    setDecision(defaultDecision(application.status))
    setError('')
    setSuccess('')
  }

    async function updateStatus() {
    if (!selected || !validJobId ||
        !canUpdateStatus(selected, decision, saving)) {
      return
    }

    setSaving(true)
    setError('')
    setSuccess('')

    try {
      const updated = await jobRequest<Application>(
        `/api/v1/account/job-postings/${jobId}/applications`,
        {
          method: 'PUT',
          body: JSON.stringify({
            applicationId: selected.id,
            status: decision,
          }),
        }
      )

      setApplications(current => updateApplication(current, updated))

      setSuccess(`Application #${selected.id} updated to ${updated.status}.`)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="site-shell">
      <SiteHeader />

      <div className="jobs-banner">
        <div className="jobs-banner-inner">
          <span className="section-kicker">
            CAREERCONNECT / RECRUITING
          </span>
          <h1>Review applicants.</h1>
          <p>Manage applications and candidate decisions.</p>
        </div>
      </div>

      <main className="jobs-page applicants-page">
        <Link to="/account/jobs" className="applicants-back">
          ← Back to my postings
        </Link>

        {session.status === 'loading' && (
          <p>Checking your session...</p>
        )}

        {session.status === 'signed-out' && (
          <p>Please sign in to review applicants.</p>
        )}

        {session.status === 'unavailable' && (
          <p role="alert">
            Your session could not be verified. Please try again.
          </p>
        )}

        {!validJobId && (
          <p role="alert">Invalid job posting.</p>
        )}

        {session.status === 'signed-in' && validJobId && (
          <>
                        {loading && (
              <p>Loading applications...</p>
            )}


            {!loading && job && (
              <div className="applicants-heading">
                <div>
                  <span className="section-kicker">APPLICATION REVIEW</span>
                  <h2>{job.jobName}</h2>
                  <p>{job.company} · {job.location}</p>
                </div>
                <span className="applicants-count">
                  {applications.length} applications
                </span>
              </div>
            )}

            {error && (
              <p role="alert" className="job-error">{error}</p>
            )}

            {success && (
              <p role="status" className="applicants-success">
                {success}
              </p>
            )}

            {!loading && job && applications.length === 0 && (
              <div className="applicants-panel">
                <p>No applications have been received yet.</p>
              </div>
            )}

            {!loading && job && applications.length > 0 && (
              <div className="applicants-grid">
                <section className="applicants-panel">
                  <h3>Applications</h3>

                  <div className="applicants-list">
                    {applications.map(application => (
                      <button
                        type="button"
                        key={application.id}
                        disabled={saving}
                        className={
                          selectedId === application.id
                            ? 'applicant-item selected'
                            : 'applicant-item'
                        }
                        onClick={() => selectApplication(application)}
                      >
                        <span>
                          <strong>Application #{application.id}</strong>
                          <small>
                            {new Date(application.appliedAt)
                              .toLocaleDateString()}
                          </small>
                        </span>
                        <span className="applicant-status">
                          {application.status}
                        </span>
                      </button>
                    ))}
                  </div>
                </section>

                <section className="applicants-panel">
                  {!selected ? (
                    <div className="applicants-empty">
                      <h3>Application details</h3>
                      <p>Select an application to review its details.</p>
                    </div>
                  ) : (
                    <>
                      <h3>Application #{selected.id}</h3>

                      <dl className="applicant-details">
                        <div>
                          <dt>Applicant ID</dt>
                          <dd>{selected.userId}</dd>
                        </div>
                        <div>
                          <dt>Application date</dt>
                          <dd>
                            {new Date(selected.appliedAt)
                              .toLocaleString()}
                          </dd>
                        </div>
                        <div>
                          <dt>Resume ID</dt>
                          <dd>{selected.resumeId ?? 'Not available'}</dd>
                        </div>
                        <div>
                          <dt>Current status</dt>
                          <dd>{selected.status}</dd>
                        </div>
                      </dl>

                      <div className="applicant-update">
                        <label htmlFor="applicant-decision">
                          Update application status
                        </label>

                        <select
                          id="applicant-decision"
                          value={decision}
                          disabled={saving}
                          onChange={event =>
                            setDecision(event.target.value as Decision)
                          }
                        >
                          {decisions.map(status => (
                            <option key={status} value={status}>
                              {status}
                            </option>
                          ))}
                        </select>

                        <button
                          type="button"
                          className="job-button"
                          disabled={!canUpdateStatus(selected, decision, saving)}
                          onClick={() => void updateStatus()}
                        >
                          {saving ? 'Saving...' : 'Save changes'}
                        </button>
                      </div>
                    </>
                  )}
                </section>
              </div>
            )}
          </>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}
