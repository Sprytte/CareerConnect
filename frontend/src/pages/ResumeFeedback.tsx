import { useEffect, useId, useRef, useState } from 'react'
import type { ChangeEvent, DragEvent, FormEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { loginUrl, useSession } from '../auth/session'
import SiteHeader from '../components/SiteHeader'
import SiteFooter from '../components/SiteFooter'
import {
  feedbackErrorMessage, feedbackTimeoutMs, formatFileSize, requestResumeFeedback,
  ResumeFeedbackError, resumeFileProblem,
} from '../resumeFeedback/resumeFeedbackApi'
import type { FeedbackRequester, ResumeFeedback as Feedback } from '../resumeFeedback/resumeFeedbackApi'
import '../styles/App.css'
import '../styles/ResumeFeedback.css'

// Task 10.2: a page that accepts a resume file, shows a loading screen while
// the AI generates a response, and displays the feedback.

type Props = { requester?: FeedbackRequester }

type Problem = { message: string; sessionExpired: boolean }

type Stage =
  | { name: 'form'; problem: Problem | null }
  | { name: 'loading'; fileName: string }
  | { name: 'done'; feedback: Feedback }

const progressSteps = [
  { after: 0, text: 'Uploading your resume…' },
  { after: 3, text: 'Reading your resume…' },
  { after: 8, text: 'Writing your feedback…' },
  { after: 25, text: 'Still working. Longer resumes can take up to a minute.' },
]

function Notice({ title, children, action, error = false }: {
  title: string
  children: ReactNode
  action?: ReactNode
  error?: boolean
}) {
  return (
    <div className="feedback-card feedback-notice" role={error ? 'alert' : 'status'}>
      <h2>{title}</h2>
      <p>{children}</p>
      {action && <div className="feedback-actions">{action}</div>}
    </div>
  )
}

function LoadingScreen({ fileName, onCancel }: { fileName: string; onCancel: () => void }) {
  const [seconds, setSeconds] = useState(0)
  const heading = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    heading.current?.focus()
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const step = [...progressSteps].reverse().find((item) => seconds >= item.after) ?? progressSteps[0]

  return (
    <section className="feedback-card feedback-loading" aria-labelledby="feedback-loading-title" aria-busy="true">
      <div className="feedback-spinner" aria-hidden="true">
        <span /><span /><span />
      </div>
      <h2 id="feedback-loading-title" tabIndex={-1} ref={heading}>Generating your feedback</h2>
      <p className="feedback-loading-file">{fileName}</p>
      <p className="feedback-loading-step" role="status">{step.text}</p>
      <p className="feedback-loading-hint">Keep this page open. This usually takes less than a minute.</p>
      <button className="feedback-button feedback-button--plain" type="button" onClick={onCancel}>
        Cancel
      </button>
    </section>
  )
}

function FeedbackList({ title, items, empty, tone }: {
  title: string
  items: string[]
  empty: string
  tone: 'strength' | 'improvement'
}) {
  return (
    <section className={`feedback-section feedback-section--${tone}`}>
      <h3>{title}</h3>
      {items.length === 0
        ? <p className="feedback-empty">{empty}</p>
        : <ul>{items.map((item, index) => <li key={index}>{item}</li>)}</ul>}
    </section>
  )
}

function FeedbackResult({ feedback, onRestart }: { feedback: Feedback; onRestart: () => void }) {
  const heading = useRef<HTMLHeadingElement>(null)
  useEffect(() => { heading.current?.focus() }, [])

  return (
    <article className="feedback-card feedback-result" aria-labelledby="feedback-result-title">
      <p className="section-kicker">AI-GENERATED FEEDBACK</p>
      <h2 id="feedback-result-title" tabIndex={-1} ref={heading}>Feedback on {feedback.fileName}</h2>
      <p className="feedback-summary">{feedback.summary}</p>

      <div className="feedback-sections">
        <FeedbackList
          title="What works well"
          items={feedback.strengths}
          empty="No specific strengths were listed for this file."
          tone="strength"
        />
        <FeedbackList
          title="What to improve"
          items={feedback.improvements}
          empty="No specific improvements were suggested for this file."
          tone="improvement"
        />
      </div>

      <p className="feedback-disclaimer">
        This feedback was written by an AI model and can be wrong or incomplete.
        Treat it as suggestions and use your own judgment before changing your resume.
      </p>

      <div className="feedback-actions">
        <button className="feedback-button" type="button" onClick={onRestart}>
          Review another resume
        </button>
        <Link className="feedback-button feedback-button--plain" to="/account/resume">
          Manage my resumes
        </Link>
      </div>
    </article>
  )
}

function FeedbackForm({ problem, onSubmit }: {
  problem: Problem | null
  onSubmit: (file: File) => void
}) {
  const id = useId()
  const [file, setFile] = useState<File | null>(null)
  const [dragging, setDragging] = useState(false)
  const [touched, setTouched] = useState(false)
  const fileProblem = file ? resumeFileProblem(file) : touched ? 'Choose a resume file first.' : null

  function choose(next: File | null | undefined) {
    setFile(next ?? null)
    setTouched(true)
  }

  function onDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setDragging(false)
    choose(event.dataTransfer.files?.[0])
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTouched(true)
    if (file && !resumeFileProblem(file)) onSubmit(file)
  }

  return (
    <form className="feedback-card feedback-form" onSubmit={submit} noValidate>
      <h2>Choose a resume to review</h2>
      <p className="feedback-form-intro">
        Upload a PDF or DOCX file up to 5 MB. You will get a short summary,
        what already works, and specific things to improve.
      </p>

      <div
        className={`feedback-drop${dragging ? ' feedback-drop--active' : ''}`}
        onDragOver={(event) => { event.preventDefault(); setDragging(true) }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
      >
        <label htmlFor={`${id}-file`}>Drag and drop your resume here, or select a file</label>
        <input
          id={`${id}-file`}
          type="file"
          accept=".pdf,.docx"
          aria-describedby={`${id}-hint${fileProblem ? ` ${id}-problem` : ''}`}
          aria-invalid={fileProblem ? true : undefined}
          onChange={(event: ChangeEvent<HTMLInputElement>) => choose(event.target.files?.[0])}
        />
        <p className="feedback-drop-hint" id={`${id}-hint`}>Accepted formats: PDF, DOCX. Maximum size: 5 MB.</p>
      </div>

      {file && !fileProblem && (
        <p className="feedback-selected">
          Selected: <strong>{file.name}</strong> ({formatFileSize(file.size)})
        </p>
      )}
      {fileProblem && <p className="feedback-error" id={`${id}-problem`} role="alert">{fileProblem}</p>}

      {problem && (
        <p className="feedback-error" role="alert">
          {problem.message}
          {problem.sessionExpired && <> <a href={loginUrl}>Sign in</a></>}
        </p>
      )}

      <p className="feedback-privacy">
        Your resume is sent to an external AI service to generate this feedback.
        The file you choose here is not saved to your CareerConnect account.
      </p>

      <div className="feedback-actions">
        <button className="feedback-button" type="submit">Get AI feedback</button>
      </div>
    </form>
  )
}

function FeedbackFlow({ requester }: { requester: FeedbackRequester }) {
  const [stage, setStage] = useState<Stage>({ name: 'form', problem: null })
  const request = useRef<AbortController | null>(null)

  // Leaving the page stops the request instead of updating a removed screen.
  useEffect(() => () => request.current?.abort(), [])

  async function start(file: File) {
    request.current?.abort()
    const controller = new AbortController()
    request.current = controller
    let timedOut = false
    const timer = window.setTimeout(() => { timedOut = true; controller.abort() }, feedbackTimeoutMs)
    setStage({ name: 'loading', fileName: file.name })

    try {
      const feedback = await requester(file, controller.signal)
      if (!controller.signal.aborted) setStage({ name: 'done', feedback })
    } catch (error) {
      if (request.current !== controller) return
      if (timedOut) {
        setStage({ name: 'form', problem: {
          message: 'The AI service took too long to respond. Please try again.', sessionExpired: false,
        } })
      } else if (!controller.signal.aborted) {
        setStage({ name: 'form', problem: {
          message: feedbackErrorMessage(error),
          sessionExpired: error instanceof ResumeFeedbackError && error.kind === 'unauthenticated',
        } })
      }
    } finally {
      window.clearTimeout(timer)
    }
  }

  function cancel() {
    request.current?.abort()
    request.current = null
    setStage({ name: 'form', problem: null })
  }

  if (stage.name === 'loading') return <LoadingScreen fileName={stage.fileName} onCancel={cancel} />
  if (stage.name === 'done') {
    return <FeedbackResult feedback={stage.feedback} onRestart={() => setStage({ name: 'form', problem: null })} />
  }
  return <FeedbackForm problem={stage.problem} onSubmit={start} />
}

export default function ResumeFeedback({ requester = requestResumeFeedback }: Props) {
  const session = useSession()
  return (
    <div className="site-shell feedback-shell">
      <SiteHeader />
      <main className="feedback-page" id="main-content">
        <nav className="feedback-nav" aria-label="Account navigation">
          <Link to="/account/resume">Resumes</Link>
          <span aria-current="page">AI feedback</span>
          <Link to="/account/applications">Applications</Link>
        </nav>
        <div className="feedback-title">
          <p className="section-kicker">AI-ASSISTED RESUME FEEDBACK</p>
          <h1>Get feedback on your resume<span aria-hidden="true">.</span></h1>
          <p>Find out what is working and what to improve before you apply.</p>
        </div>

        {session.status === 'loading' && <Notice title="Checking your account">Please wait while we check your session…</Notice>}
        {session.status === 'signed-out' && <Notice title="Sign in to get resume feedback" action={
          <a className="feedback-button" href={loginUrl}>Sign in</a>
        }>
          AI feedback is available to signed-in CareerConnect users.
        </Notice>}
        {session.status === 'unavailable' && <Notice title="We couldn’t check your account" error action={
          <button className="feedback-button" type="button" onClick={() => window.location.reload()}>Try again</button>
        }>
          Please check your connection and try again.
        </Notice>}
        {session.status === 'signed-in' && <FeedbackFlow key={session.user.userId} requester={requester} />}
      </main>
      <SiteFooter />
    </div>
  )
}
