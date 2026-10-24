import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import { jobRequest, type Job } from './jobApi'
import '../styles/App.css'
import '../styles/Jobs.css'

export default function BrowseJobsPage() {
  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    jobRequest<Job[]>('/api/v1/job-postings')
      .then(result => {
        if (active) setJobs(result)
      })
      .catch(err => {
        if (active) {
          setError(
            err instanceof Error ? err.message : 'Could not load jobs.'
          )
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <div className="site-shell">
      <SiteHeader />

      <main className="jobs-page">
        <p className="section-kicker">CAREERCONNECT / OPPORTUNITIES</p>
        <h1>Explore job opportunities</h1>
        <p>Find a position and apply with your resume.</p>

        {loading && <p>Loading job postings...</p>}
        {error && <p role="alert" className="job-error">{error}</p>}

        {!loading && !error && jobs.length === 0 && (
          <p>No job postings are available yet.</p>
        )}

        {jobs.map(job => {
          const expired = new Date(job.deadline).getTime() <= Date.now()

          return (
            <article className="job-card" key={job.id}>
              <h2>{job.jobName}</h2>
              <p>{job.company} · {job.location}</p>
              <p>{job.description}</p>
              <p>{job.category} · {job.salary}</p>
              <p>
                Deadline: {new Date(job.deadline).toLocaleString()}
              </p>

              {expired ? (
                <p>Applications closed</p>
              ) : (
                <Link
                  className="job-button"
                  to={`/jobs/${job.id}/apply`}
                >
                  Apply Now
                </Link>
              )}
            </article>
          )
        })}
      </main>

      <SiteFooter />
    </div>
  )
}