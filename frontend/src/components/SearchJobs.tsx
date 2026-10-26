import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import '../styles/SearchJobs.css'
import SiteHeader from './SiteHeader'
import SiteFooter from './SiteFooter'
import { useSession } from '../auth/session'
import { jobRequest, type Job } from '../jobs/jobApi'

export default function SearchJobs() {
  const session = useSession()

  const [jobs, setJobs] = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [keyword, setKeyword] = useState('')
  const [location, setLocation] = useState('')
  const [category, setCategory] = useState('')
  const [expandedJobId, setExpandedJobId] = useState<number | null>(null)

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

  const categories = useMemo(
    () => Array.from(new Set(jobs.map(job => job.category))),
    [jobs]
  )

  const filteredJobs = useMemo(() => {
    const keywordValue = keyword.trim().toLowerCase()
    const locationValue = location.trim().toLowerCase()

    return jobs.filter(job => {
      const matchesKeyword =
        keywordValue === '' ||
        job.jobName.toLowerCase().includes(keywordValue) ||
        job.company.toLowerCase().includes(keywordValue) ||
        job.description.toLowerCase().includes(keywordValue)

      const matchesLocation =
        locationValue === '' ||
        job.location.toLowerCase().includes(locationValue)

      const matchesCategory =
        category === '' || job.category === category

      return matchesKeyword && matchesLocation && matchesCategory
    })
  }, [jobs, keyword, location, category])

  const clearFilters = () => {
    setKeyword('')
    setLocation('')
    setCategory('')
  }

  const formatDate = (date: string) =>
    new Date(date).toLocaleDateString()

  return (
    <div className="search-jobs-page">
      <SiteHeader />

      <main className="search-jobs-main">
        <section className="search-jobs-heading">
          <span className="search-jobs-kicker">
            FIND YOUR NEXT MOVE
          </span>

          <h1>Search for Jobs</h1>

          <p>
            Explore available opportunities and narrow your search
            by keyword, location, or category.
          </p>

          {session.status === 'loading' && (
            <p className="search-jobs-message">
              Checking account...
            </p>
          )}

          {session.status === 'unavailable' && (
            <p className="search-jobs-message">
              Account information is currently unavailable,
              but you can still browse jobs.
            </p>
          )}
        </section>

        <section className="job-search-filters">
          <div className="job-search-field">
            <label htmlFor="job-keyword">Keyword</label>
            <input
              id="job-keyword"
              type="text"
              placeholder="Job title, company, or keyword"
              value={keyword}
              onChange={event => setKeyword(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-location">Location</label>
            <input
              id="job-location"
              type="text"
              placeholder="Montreal, Toronto..."
              value={location}
              onChange={event => setLocation(event.target.value)}
            />
          </div>

          <div className="job-search-field">
            <label htmlFor="job-category">Category</label>
            <select
              id="job-category"
              value={category}
              onChange={event => setCategory(event.target.value)}
            >
              <option value="">All categories</option>
              {categories.map(jobCategory => (
                <option key={jobCategory} value={jobCategory}>
                  {jobCategory}
                </option>
              ))}
            </select>
          </div>

          <button
            className="clear-filters-button"
            type="button"
            onClick={clearFilters}
          >
            Clear filters
          </button>
        </section>

        {loading && <p>Loading job postings...</p>}

        {error && (
          <p role="alert" className="search-jobs-message">
            {error}
          </p>
        )}

        {!loading && !error && (
          <>
            <div className="search-results-heading">
              <h2>Available Jobs</h2>
              <span>
                {filteredJobs.length}{' '}
                {filteredJobs.length === 1 ? 'result' : 'results'}
              </span>
            </div>

            {filteredJobs.length === 0 ? (
              <section className="search-jobs-empty">
                <h2>No jobs found</h2>
                <p>
                  Try changing your filters or check back later.
                </p>
              </section>
            ) : (
              <section className="search-jobs-grid">
                {filteredJobs.map(job => {
                  const isExpanded = expandedJobId === job.id
                  const expired =
                    new Date(job.deadline).getTime() <= Date.now()

                  return (
                    <article className="search-job-card" key={job.id}>
                      <span className="search-job-category">
                        {job.category}
                      </span>

                      <h2>{job.jobName}</h2>

                      <div className="search-job-details">
                        <span>{job.company}</span>
                        <span>•</span>
                        <span>{job.location}</span>
                      </div>

                      <p
                        className={
                          isExpanded
                            ? 'search-job-description expanded'
                            : 'search-job-description'
                        }
                      >
                        {job.description}
                      </p>

                      <div className="search-job-footer">
                        <div className="search-job-dates">
                          <span>
                            Posted {formatDate(job.dateCreated)}
                          </span>
                          <span>
                            Deadline {formatDate(job.deadline)}
                          </span>
                        </div>

                        <button
                          className="view-details-button"
                          type="button"
                          onClick={() =>
                            setExpandedJobId(
                              isExpanded ? null : job.id
                            )
                          }
                        >
                          {isExpanded ? 'Hide details' : 'View details'}
                        </button>

                        {expired ? (
                          <span>Applications closed</span>
                        ) : (
                          <Link
                            className="view-details-button"
                            to={`/jobs/${job.id}/apply`}
                          >
                            Apply Now
                          </Link>
                        )}
                      </div>
                    </article>
                  )
                })}
              </section>
            )}
          </>
        )}
      </main>

      <SiteFooter />
    </div>
  )
}