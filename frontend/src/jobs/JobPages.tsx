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
  return <div className="site-shell"><SiteHeader />
    <div className="jobs-banner"><div className="jobs-banner-inner"><span className="section-kicker">CAREERCONNECT / RECRUITING</span><h1>Manage your opportunities.</h1><p>Create job postings and view the opportunities you have published.</p></div></div>
    <main className="jobs-page">{children}</main><SiteFooter />
  </div>
}

export function NewJobPage() {
  const session = useSession()
  const [saved, setSaved] = useState(false)
  return <JobLayout><p className="job-links"><Link to="/">Home</Link><Link to="/jobs">Find jobs</Link>{session.status === 'signed-in' && session.user.role === 'RECRUITER' && <Link to="/account/jobs">My postings</Link>}</p>
    {session.status !== 'signed-in' ? <p>Please sign in with a recruiter account to create a job.</p> :
      session.user.role === null ? <p>Choose your account type first. <Link to="/onboarding/role">Choose a role</Link></p> :
      session.user.role !== 'RECRUITER' ? <p>Recruiter accounts create job postings. <Link to="/jobs">Browse jobs instead</Link></p> :
      saved ? <p>Job created. <Link to="/account/jobs">View your postings</Link></p> :
      <JobForm onSaved={() => setSaved(true)} />}
  </JobLayout>
}

export function MyJobsPage() {
  const session = useSession()
  const [jobs, setJobs] = useState<Job[]>([])
  const [error, setError] = useState('')
  useEffect(() => {
    if (session.status !== 'signed-in' || session.user.role !== 'RECRUITER') return
    jobRequest<Job[]>('/api/v1/account/job-postings').then(setJobs)
      .catch(err => setError(err instanceof Error ? err.message : 'Could not load jobs.'))
  }, [session])
  return <JobLayout><div className="jobs-heading"><div><span className="section-kicker">YOUR WORKSPACE</span><h2>My job postings</h2></div>{session.status === 'signed-in' && session.user.role === 'RECRUITER' && <Link className="job-button" to="/jobs/new">Create a job</Link>}</div>
    <p className="job-links"><Link to="/">Home</Link><Link to="/jobs">Find jobs</Link></p>
    {session.status !== 'signed-in' ? <p>Please sign in with a recruiter account to view your jobs.</p> :
      session.user.role === null ? <p>Choose your account type first. <Link to="/onboarding/role">Choose a role</Link></p> :
      session.user.role !== 'RECRUITER' ? <p>My postings is available to recruiter accounts. <Link to="/jobs">Browse jobs instead</Link></p> : <>
      {error && <p role="alert">{error}</p>}
      {jobs.map(job => <article className="job-card" key={job.id}>
        <h2>{job.jobName}</h2><p>{job.company} · {job.location}</p><p>{job.description}</p>
        <p>{job.category} · {job.salary}</p><p>Deadline: {new Date(job.deadline).toLocaleString()}</p>
      </article>)}
      {!error && jobs.length === 0 && <p>You have not created any job postings.</p>}
    </>}
  </JobLayout>
}
