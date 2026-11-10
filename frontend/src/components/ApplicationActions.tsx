import { useContext, useEffect, useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { loginUrl } from '../auth/session'
import { ApplicationChangeContext } from '../applications/applicationChangeContext'
import {
  ApplicationChangeError, changeApplicationResume, changeAvailability, changeErrorMessage,
  formatDeadline, loadResumeOptions, resumeFileProblem, uploadResume, withdrawApplication,
} from '../applications/applicationChanges'
import type { ResumeOption } from '../applications/applicationChanges'
import type { JobApplication } from '../applications/applications'

type Problem = { message: string; sessionExpired: boolean }

function toProblem(error: unknown): Problem {
  return {
    message: changeErrorMessage(error),
    sessionExpired: error instanceof ApplicationChangeError && error.kind === 'unauthenticated',
  }
}

function isAbort(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function ProblemMessage({ problem }: { problem: Problem }) {
  return (
    <p className="application-change-error" role="alert">
      {problem.message}
      {problem.sessionExpired && <> <a href={loginUrl}>Sign in</a></>}
    </p>
  )
}

type ResumeChoice = 'existing' | 'upload'
type ResumeList =
  | { status: 'loading' }
  | { status: 'ready'; resumes: ResumeOption[] }
  | { status: 'error'; problem: Problem }

function EditPanel({ application, onSaved, onLocked, onCancel }: {
  application: JobApplication
  onSaved: (message: string) => void
  onLocked: () => void
  onCancel: () => void
}) {
  const id = useId()
  const [list, setList] = useState<ResumeList>({ status: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [choice, setChoice] = useState<ResumeChoice>('existing')
  const [selected, setSelected] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [saving, setSaving] = useState(false)
  const [problem, setProblem] = useState<Problem | null>(null)
  const request = useRef<AbortController | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    loadResumeOptions(controller.signal).then((resumes) => {
      if (controller.signal.aborted) return
      setList({ status: 'ready', resumes })
      const current = resumes.find((resume) => resume.id === application.resumeId)
      setSelected(String((current ?? resumes[0])?.id ?? ''))
      if (resumes.length === 0) setChoice('upload')
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) setList({ status: 'error', problem: toProblem(error) })
    })
    return () => controller.abort()
  }, [application.resumeId, attempt])

  // Stop an in-flight save if the card disappears, for example after a refresh.
  useEffect(() => () => request.current?.abort(), [])

  const resumes = list.status === 'ready' ? list.resumes : []
  const currentResume = resumes.find((resume) => resume.id === application.resumeId)
  const unchanged = choice === 'existing' && selected !== '' && Number(selected) === application.resumeId
  const fileProblem = choice === 'upload' && file ? resumeFileProblem(file) : null
  const incomplete = choice === 'existing' ? selected === '' : file === null

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (saving || unchanged || incomplete || fileProblem) return
    const controller = new AbortController()
    request.current = controller
    setSaving(true)
    setProblem(null)
    try {
      const resume = choice === 'upload'
        ? await uploadResume(file!, controller.signal)
        : resumes.find((item) => item.id === Number(selected))
      if (!resume) throw new ApplicationChangeError('invalid', 'Choose one of your resumes.')
      await changeApplicationResume(application.id, resume.id, controller.signal)
      onSaved(`Your application for ${application.jobTitle} now uses ${resume.fileName}.`)
    } catch (error) {
      if (isAbort(error) || controller.signal.aborted) return
      if (error instanceof ApplicationChangeError && error.kind === 'deadline') onLocked()
      setProblem(toProblem(error))
      setSaving(false)
    }
  }

  return (
    <form className="application-edit" onSubmit={save} aria-labelledby={`${id}-title`}>
      <h4 id={`${id}-title`}>Edit your application</h4>
      <p className="application-edit-hint">
        Choose the resume the employer will see for this application.
        {currentResume && <> Currently attached: <strong>{currentResume.fileName}</strong>.</>}
      </p>

      {list.status === 'loading' && <p role="status">Loading your resumes…</p>}

      {list.status === 'error' && (
        <>
          <ProblemMessage problem={list.problem} />
          {!list.problem.sessionExpired && (
            <button
              className="application-action"
              type="button"
              onClick={() => { setList({ status: 'loading' }); setAttempt((value) => value + 1) }}
            >
              Try again
            </button>
          )}
        </>
      )}

      {list.status === 'ready' && (
        <fieldset className="application-edit-options" disabled={saving}>
          <legend>Resume for this application</legend>

          <label className="application-edit-option">
            <input
              type="radio"
              name={`${id}-choice`}
              checked={choice === 'existing'}
              disabled={resumes.length === 0}
              onChange={() => { setChoice('existing'); setProblem(null) }}
            />
            <span>Use one of my uploaded resumes</span>
          </label>
          {choice === 'existing' && resumes.length > 0 && (
            <div className="application-edit-detail">
              <label htmlFor={`${id}-resume`}>Uploaded resumes</label>
              <select
                id={`${id}-resume`}
                value={selected}
                onChange={(event) => { setSelected(event.target.value); setProblem(null) }}
              >
                {resumes.map((resume) => (
                  <option key={resume.id} value={resume.id}>
                    {resume.fileName}{resume.id === application.resumeId ? ' (current)' : ''}
                  </option>
                ))}
              </select>
              {unchanged && (
                <p className="application-edit-note">
                  This resume is already attached. Pick a different one to save a change.
                </p>
              )}
            </div>
          )}
          {resumes.length === 0 && (
            <p className="application-edit-note">You have no uploaded resumes yet. Upload one below.</p>
          )}

          <label className="application-edit-option">
            <input
              type="radio"
              name={`${id}-choice`}
              checked={choice === 'upload'}
              onChange={() => { setChoice('upload'); setProblem(null) }}
            />
            <span>Upload a new resume</span>
          </label>
          {choice === 'upload' && (
            <div className="application-edit-detail">
              <label htmlFor={`${id}-file`}>Resume file (PDF or DOCX, up to 5 MB)</label>
              <input
                id={`${id}-file`}
                type="file"
                accept=".pdf,.docx"
                aria-describedby={fileProblem ? `${id}-file-problem` : undefined}
                aria-invalid={fileProblem ? true : undefined}
                onChange={(event) => { setFile(event.target.files?.[0] ?? null); setProblem(null) }}
              />
              {fileProblem && (
                <p className="application-change-error" id={`${id}-file-problem`} role="alert">{fileProblem}</p>
              )}
            </div>
          )}
        </fieldset>
      )}

      {problem && <ProblemMessage problem={problem} />}

      <div className="application-edit-buttons">
        {list.status === 'ready' && (
          <button
            className="application-action application-action--primary"
            type="submit"
            disabled={saving || unchanged || incomplete || fileProblem !== null}
          >
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        )}
        <button className="application-action" type="button" onClick={onCancel} disabled={saving}>
          Cancel
        </button>
      </div>
    </form>
  )
}

function WithdrawPanel({ application, onWithdrawn, onLocked, onCancel }: {
  application: JobApplication
  onWithdrawn: (message: string) => void
  onLocked: () => void
  onCancel: () => void
}) {
  const id = useId()
  const [withdrawing, setWithdrawing] = useState(false)
  const [problem, setProblem] = useState<Problem | null>(null)
  const request = useRef<AbortController | null>(null)
  const keep = useRef<HTMLButtonElement>(null)

  // Start on the safe choice so a stray Enter does not withdraw.
  useEffect(() => { keep.current?.focus() }, [])
  useEffect(() => () => request.current?.abort(), [])

  async function withdraw() {
    if (withdrawing) return
    const controller = new AbortController()
    request.current = controller
    setWithdrawing(true)
    setProblem(null)
    try {
      await withdrawApplication(application.id, controller.signal)
      onWithdrawn(`Your application for ${application.jobTitle} at ${application.company} was withdrawn.`)
    } catch (error) {
      if (isAbort(error) || controller.signal.aborted) return
      if (error instanceof ApplicationChangeError && error.kind === 'deadline') onLocked()
      setProblem(toProblem(error))
      setWithdrawing(false)
    }
  }

  return (
    <div className="application-withdraw" role="group" aria-labelledby={`${id}-title`}>
      <h4 id={`${id}-title`}>Withdraw this application?</h4>
      <p>
        Your application for <strong>{application.jobTitle}</strong> at {application.company} will
        be removed and the employer will no longer see it. This cannot be undone.
      </p>
      {problem && <ProblemMessage problem={problem} />}
      <div className="application-edit-buttons">
        <button
          className="application-action application-action--danger"
          type="button"
          onClick={withdraw}
          disabled={withdrawing}
        >
          {withdrawing ? 'Withdrawing…' : 'Withdraw application'}
        </button>
        <button className="application-action" type="button" onClick={onCancel} disabled={withdrawing} ref={keep}>
          Keep application
        </button>
      </div>
    </div>
  )
}

export default function ApplicationActions({ application }: { application: JobApplication }) {
  const onChanged = useContext(ApplicationChangeContext)
  // Read the clock once per card; the server still enforces the deadline.
  const [openedAt] = useState(() => Date.now())
  const [panel, setPanel] = useState<'none' | 'edit' | 'withdraw'>('none')
  const [lockedByServer, setLockedByServer] = useState(false)

  // Without a list to refresh there is nothing safe to offer.
  if (!onChanged) return null

  const availability = lockedByServer ? 'closed' : changeAvailability(application, openedAt)
  const deadline = formatDeadline(application.deadline)

  if (availability === 'closed' && panel === 'none') {
    return (
      <div className="application-actions">
        <p className="application-locked">
          {application.deadline || lockedByServer
            ? <>The deadline{deadline ? <> ({deadline})</> : null} has passed, so this application can no longer be edited or withdrawn.</>
            : 'This job posting is no longer available, so this application can no longer be edited or withdrawn.'}
        </p>
      </div>
    )
  }

  return (
    <div className="application-actions">
      {panel === 'none' && (
        <div className="application-actions-row">
          {deadline && <p className="application-deadline">Changes allowed until {deadline}</p>}
          <div className="application-edit-buttons">
            <button className="application-action" type="button" onClick={() => setPanel('edit')}>
              Edit application
            </button>
            <button
              className="application-action application-action--danger-outline"
              type="button"
              onClick={() => setPanel('withdraw')}
            >
              Withdraw
            </button>
          </div>
        </div>
      )}
      {panel === 'edit' && (
        <EditPanel
          application={application}
          onSaved={onChanged}
          onLocked={() => setLockedByServer(true)}
          onCancel={() => setPanel('none')}
        />
      )}
      {panel === 'withdraw' && (
        <WithdrawPanel
          application={application}
          onWithdrawn={onChanged}
          onLocked={() => setLockedByServer(true)}
          onCancel={() => setPanel('none')}
        />
      )}
    </div>
  )
}
