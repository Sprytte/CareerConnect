import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useSession } from '../auth/session'
import { backendUrl } from '../constants'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import { jobRequest, type Job } from './jobApi'
import '../styles/App.css'
import '../styles/Jobs.css'

type Resume = {
  id: number
  userId: string
  fileName: string
  contentType: string
  sizeBytes: number
  uploadedAt: string
}

type ResumeChoice = 'recent' | 'existing' | 'upload'

export default function ApplyJobPage() {
  const { jobId } = useParams()
  const session = useSession()

  const [job, setJob] = useState<Job | null>(null)
  const [resumes, setResumes] = useState<Resume[]>([])
  const [choice, setChoice] = useState<ResumeChoice>('recent')
  const [selectedResumeId, setSelectedResumeId] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function loadJob() {
      try {
        if (!jobId || !/^\d+$/.test(jobId)) {
          throw new Error('Invalid job posting.')
        }

        const result = await jobRequest<Job>(
          `/api/v1/job-postings/${jobId}`
        )

        if (active) setJob(result)
      } catch (err) {
        if (active) {
          setError(
            err instanceof Error ? err.message : 'Could not load job.'
          )
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    void loadJob()

    return () => {
      active = false
    }
  }, [jobId])

  useEffect(() => {
  if (session.status !== 'signed-in') return

  const userId = encodeURIComponent(session.user.userId)
  let active = true

  async function loadResumes() {
    try {

        const response = await fetch(
          `${backendUrl}/api/v1/resumes/users/${userId}`,
          { credentials: 'include' }
        )

        if (!response.ok) {
          throw new Error('Could not load your resumes.')
        }

        const result: Resume[] = await response.json()

        if (active) {
          setResumes(result)
          if (result.length > 0) {
            setSelectedResumeId(String(result[0].id))
          }
        }
      } catch (err) {
        if (active) {
          setError(
            err instanceof Error
              ? err.message
              : 'Could not load your resumes.'
          )
        }
      }
    }

    void loadResumes()

    return () => {
      active = false
    }
  }, [session.status, session.status === 'signed-in'
    ? session.user.userId
    : null])

  async function uploadResume(userId: string): Promise<number> {
    if (!file) {
      throw new Error('Please select a resume to upload.')
    }

    const extension = file.name.toLowerCase()

    if (!extension.endsWith('.pdf') && !extension.endsWith('.docx')) {
      throw new Error('Only PDF and DOCX resumes are supported.')
    }

    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(
      `${backendUrl}/api/v1/resumes/users/${encodeURIComponent(userId)}`,
      {
        method: 'POST',
        credentials: 'include',
        body: formData,
      }
    )

    if (!response.ok) {
      throw new Error('Resume upload failed. Please try again.')
    }

    const uploaded: Resume = await response.json()
    return uploaded.id
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (session.status !== 'signed-in' || !jobId || !job) {
      setError('Please sign in and select a valid job.')
      return
    }

    if (new Date(job.deadline).getTime() <= Date.now()) {
      setError('The application deadline has passed.')
      return
    }

    setError('')
    setSubmitting(true)

    try {
      let resumeId: number | null = null

      if (choice === 'existing') {
        resumeId = Number(selectedResumeId)

        if (!selectedResumeId || !resumes.some(r => r.id === resumeId)) {
          throw new Error('Please select one of your resumes.')
        }
      }

      if (choice === 'upload') {
        resumeId = await uploadResume(session.user.userId)
      }

      if (choice === 'recent' && resumes.length === 0) {
        throw new Error('Please upload a resume before applying.')
      }

      await jobRequest(
        `/api/v1/job-postings/${jobId}/apply`,
        {
          method: 'POST',
          body: JSON.stringify({ resumeId }),
        }
      )

      setSuccess(true)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Could not submit your application.'
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="site-shell">
      <SiteHeader />

      <main className="jobs-page">
        <p className="section-kicker">CAREERCONNECT / APPLICATIONS</p>
        <h1>Apply for a job</h1>

        <p className="job-links">
          <Link to="/jobs">Browse jobs</Link>
          <Link to="/account/resume">Manage resumes</Link>
        </p>

        {loading && <p>Loading job posting...</p>}

        {job && (
          <article className="job-card">
            <h2>{job.jobName}</h2>
            <p>{job.company} · {job.location}</p>
            <p>{job.description}</p>
            <p>Deadline: {new Date(job.deadline).toLocaleString()}</p>
          </article>
        )}

        {session.status === 'loading' && (
          <p>Checking your account...</p>
        )}

        {session.status === 'signed-out' && (
          <p>Please sign in to submit an application.</p>
        )}

        {session.status === 'unavailable' && (
          <p>Your account information is currently unavailable.</p>
        )}

        {error && (
          <p role="alert" className="job-error">{error}</p>
        )}

        {success ? (
          <section className="job-card" role="status">
            <h2>Application submitted!</h2>
            <p>Your application was successfully received.</p>
            <Link to="/account/applications">
              View my applications
            </Link>
          </section>
        ) : (
          session.status === 'signed-in' && job && (
            <form className="job-form apply-form" onSubmit={submit}>
  <div className="apply-form-heading">
    <span className="section-kicker">YOUR APPLICATION</span>
    <h2>Choose your resume</h2>
    <p>
      Select how you would like to submit your resume
      for this opportunity.
    </p>
  </div>

  <div className="apply-resume-options">
    <label
      className={`apply-resume-option ${
        choice === 'recent' ? 'selected' : ''
      } ${resumes.length === 0 ? 'disabled' : ''}`}
    >
      <input
        type="radio"
        name="resumeChoice"
        value="recent"
        checked={choice === 'recent'}
        onChange={() => setChoice('recent')}
        disabled={resumes.length === 0}
      />

      <span className="apply-option-content">
        <strong>Use my latest resume</strong>
        <small>
          Apply with the most recently uploaded resume
          from your account.
        </small>
      </span>
    </label>

    <label
      className={`apply-resume-option ${
        choice === 'existing' ? 'selected' : ''
      } ${resumes.length === 0 ? 'disabled' : ''}`}
    >
      <input
        type="radio"
        name="resumeChoice"
        value="existing"
        checked={choice === 'existing'}
        onChange={() => setChoice('existing')}
        disabled={resumes.length === 0}
      />

      <span className="apply-option-content">
        <strong>Choose an existing resume</strong>
        <small>
          Select one of the resumes you have already uploaded.
        </small>
      </span>
    </label>

    {choice === 'existing' && (
      <div className="apply-resume-detail">
        <label htmlFor="selectedResume">Your uploaded resumes</label>

        <select
          id="selectedResume"
          required
          value={selectedResumeId}
          onChange={e => setSelectedResumeId(e.target.value)}
        >
          {resumes.map(resume => (
            <option key={resume.id} value={resume.id}>
              {resume.fileName}
            </option>
          ))}
        </select>
      </div>
    )}

    <label
      className={`apply-resume-option ${
        choice === 'upload' ? 'selected' : ''
      }`}
    >
      <input
        type="radio"
        name="resumeChoice"
        value="upload"
        checked={choice === 'upload'}
        onChange={() => setChoice('upload')}
      />

      <span className="apply-option-content">
        <strong>Upload a new resume</strong>
        <small>
          Upload a PDF or DOCX specifically for this application.
        </small>
      </span>
    </label>

    {choice === 'upload' && (
      <div className="apply-resume-detail">
        <label htmlFor="newResume">Select your resume file</label>

        <input
          id="newResume"
          type="file"
          accept=".pdf,.docx"
          required
          onChange={e => setFile(e.target.files?.[0] ?? null)}
        />

        <small>Accepted formats: PDF, DOCX. Maximum size: 5 MB.</small>
      </div>
    )}
  </div>

  {resumes.length === 0 && choice !== 'upload' && (
    <p className="apply-hint">
      You have no uploaded resumes yet. Select
      "Upload a new resume" to continue.
    </p>
  )}

  <div className="apply-form-footer">
    <p>
      Your resume will be attached to your application
      when you submit.
    </p>

    <button
      type="submit"
      disabled={
        submitting ||
        (choice === 'recent' && resumes.length === 0)
      }
    >
      {submitting ? 'Submitting...' : 'Submit Application'}
    </button>
  </div>
</form>
          )
        )}
      </main>

      <SiteFooter />
    </div>
  )
}