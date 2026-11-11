// Unit tests for the filter and sort rules behind the recruiter
// "Applications received" page (issue #50, task 11.3).
// Run with: npm test   (Node 22.6 or newer; no packages are needed)
import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import type { Job } from '../src/jobs/jobApi.ts'
import {
  buildView, dateRangeProblem, defaultFilters, defaultSorting, hasActiveFilters,
  normalizeStatus, postingStatus,
} from '../src/jobs/postingApplicationsView.ts'
import type {
  ApplicationSort, PostingEntry, PostingSort, View,
} from '../src/jobs/postingApplicationsView.ts'

const now = new Date('2026-11-09T12:00:00').getTime()

function job(id: number, dateCreated: string, deadline: string): Job {
  return {
    id, userId: 'recruiter', jobName: `Job ${id}`, company: 'Fixture employer', description: '',
    location: '', category: '', salary: '', dateCreated, deadline,
  }
}

function received(...applications: [number, string | null, string | null][]): PostingEntry['received'] {
  return {
    status: 'ready',
    applications: applications.map(([id, status, appliedAt]) => ({ id, status, appliedAt })),
  }
}

// Posting 1: open, three applications. Posting 2: closed, two applications.
// Posting 3: open, none. Posting 4: open, applications failed to load.
const entries: PostingEntry[] = [
  {
    job: job(1, '2026-10-28T09:00:00', '2026-11-18T17:00:00'),
    received: received(
      [11, 'Applied', '2026-11-08T10:00:00'],
      [12, 'Interview', '2026-11-03T10:00:00'],
      [13, 'Rejected', '2026-11-01T10:00:00'],
    ),
  },
  {
    job: job(2, '2026-09-30T09:00:00', '2026-11-04T17:00:00'),
    received: received([21, 'Offered', '2026-10-20T10:00:00'], [22, 'Applied', '2026-10-10T10:00:00']),
  },
  { job: job(3, '2026-11-07T09:00:00', '2026-12-04T17:00:00'), received: received() },
  { job: job(4, '2026-11-01T09:00:00', '2026-11-30T17:00:00'), received: { status: 'error' } },
]

const postingIds = (view: View) => view.postings.map((posting) => posting.job.id)
const applicationIds = (view: View, jobId: number) =>
  view.postings.find((posting) => posting.job.id === jobId)?.applications.map((item) => item.id)

describe('normalizeStatus', () => {
  it('maps stored and older values to the labels job seekers see', () => {
    assert.deepEqual(['SUBMITTED', 'applied', 'Interview', 'Offer', 'OFFERED', 'Reject', 'rejected'].map(normalizeStatus), ['Applied', 'Applied', 'Interview', 'Offered', 'Offered', 'Rejected', 'Rejected'])
  })

  it('keeps unknown statuses and treats missing ones as null', () => {
    assert.equal(normalizeStatus('Screening'), 'Screening')
    assert.deepEqual(['', '  ', null, undefined, 7].map(normalizeStatus), [null, null, null, null, null])
  })
})

describe('postingStatus', () => {
  it('is Open until the deadline and Closed from the deadline on', () => {
    assert.equal(postingStatus(entries[0].job, now), 'Open')
    assert.equal(postingStatus(entries[1].job, now), 'Closed')
    assert.equal(postingStatus(job(9, '2026-11-01T00:00:00', '2026-11-09T12:00:00'), now), 'Closed')
  })

  it('treats an unreadable deadline as Closed', () => {
    assert.equal(postingStatus(job(9, '2026-11-01T00:00:00', 'not a date'), now), 'Closed')
  })
})

describe('dateRangeProblem', () => {
  it('accepts empty, one-sided and same-day ranges', () => {
    assert.equal(dateRangeProblem({ from: '', to: '' }), null)
    assert.equal(dateRangeProblem({ from: '2026-11-01', to: '' }), null)
    assert.equal(dateRangeProblem({ from: '2026-11-01', to: '2026-11-01' }), null)
  })

  it('reports a start date after the end date and malformed dates', () => {
    assert.match(dateRangeProblem({ from: '2026-11-10', to: '2026-11-01' }) ?? '', /on or before/)
    assert.match(dateRangeProblem({ from: '11/10/2026', to: '' }) ?? '', /valid start date/)
    assert.match(dateRangeProblem({ from: '', to: 'tomorrow' }) ?? '', /valid end date/)
  })
})

describe('buildView filters', () => {
  it('shows every posting, newest first, with all applications by default', () => {
    const view = buildView(entries, defaultFilters, defaultSorting, now)

    assert.deepEqual(postingIds(view), [3, 4, 1, 2])
    assert.equal(view.totalPostings, 4)
    assert.equal(view.totalApplications, 5)
    assert.equal(view.visibleApplications, 5)
    assert.deepEqual(applicationIds(view, 1), [11, 12, 13])
  })

  it('filters by posting status', () => {
    assert.deepEqual(postingIds(buildView(entries, { ...defaultFilters, postingStatus: 'Open' }, defaultSorting, now)), [3, 4, 1])
    assert.deepEqual(postingIds(buildView(entries, { ...defaultFilters, postingStatus: 'Closed' }, defaultSorting, now)), [2])
  })

  it('filters by application status and hides postings with no match', () => {
    const view = buildView(entries, { ...defaultFilters, applicationStatus: 'Applied' }, defaultSorting, now)

    assert.deepEqual(postingIds(view), [4, 1, 2])
    assert.deepEqual(applicationIds(view, 1), [11])
    assert.deepEqual(applicationIds(view, 2), [22])
    assert.equal(view.visibleApplications, 2)
    assert.equal(view.totalApplications, 5)
  })

  it('keeps a posting whose applications failed to load so the failure stays visible', () => {
    const view = buildView(entries, { ...defaultFilters, applicationStatus: 'Offered' }, defaultSorting, now)

    assert.deepEqual(postingIds(view), [4, 2])
    assert.equal(view.postings[0].total, null)
  })

  it('filters postings by date posted with inclusive bounds', () => {
    const range = { ...defaultFilters, dateField: 'posted' as const, from: '2026-11-01', to: '2026-11-07' }
    assert.deepEqual(postingIds(buildView(entries, range, defaultSorting, now)), [3, 4])

    const until = { ...defaultFilters, dateField: 'posted' as const, to: '2026-10-28' }
    assert.deepEqual(postingIds(buildView(entries, until, defaultSorting, now)), [1, 2])
  })

  it('filters postings by deadline', () => {
    const filters = { ...defaultFilters, dateField: 'deadline' as const, from: '2026-11-30' }
    assert.deepEqual(postingIds(buildView(entries, filters, defaultSorting, now)), [3, 4])
  })

  it('filters applications by date applied', () => {
    const filters = { ...defaultFilters, dateField: 'applied' as const, from: '2026-11-01', to: '2026-11-03' }
    const view = buildView(entries, filters, defaultSorting, now)

    assert.deepEqual(postingIds(view), [4, 1])
    assert.deepEqual(applicationIds(view, 1), [12, 13])
  })

  it('combines posting status, application status and date', () => {
    const filters = {
      postingStatus: 'Open' as const, applicationStatus: 'Applied' as const,
      dateField: 'applied' as const, from: '2026-11-05', to: '',
    }
    const view = buildView(entries, filters, defaultSorting, now)

    assert.deepEqual(postingIds(view), [4, 1])
    assert.deepEqual(applicationIds(view, 1), [11])
  })

  it('does not apply an invalid date range', () => {
    const filters = { ...defaultFilters, from: '2026-11-10', to: '2026-11-01' }
    assert.deepEqual(postingIds(buildView(entries, filters, defaultSorting, now)), [3, 4, 1, 2])
  })
})

describe('buildView sorting', () => {
  const sortedPostings = (postings: PostingSort) =>
    postingIds(buildView(entries, defaultFilters, { ...defaultSorting, postings }, now))
  const sortedApplications = (applications: ApplicationSort) =>
    applicationIds(buildView(entries, defaultFilters, { ...defaultSorting, applications }, now), 1)

  it('sorts postings by date posted', () => {
    assert.deepEqual(sortedPostings('posted-desc'), [3, 4, 1, 2])
    assert.deepEqual(sortedPostings('posted-asc'), [2, 1, 4, 3])
  })

  it('sorts postings by deadline', () => {
    assert.deepEqual(sortedPostings('deadline-asc'), [2, 1, 4, 3])
    assert.deepEqual(sortedPostings('deadline-desc'), [3, 4, 1, 2])
  })

  it('sorts postings by status, newest first within a status', () => {
    assert.deepEqual(sortedPostings('status-open'), [3, 4, 1, 2])
    assert.deepEqual(sortedPostings('status-closed'), [2, 3, 4, 1])
  })

  it('sorts postings by number of applications, unknown counts last', () => {
    assert.deepEqual(sortedPostings('applications-desc'), [1, 2, 3, 4])
  })

  it('sorts applications by date applied', () => {
    assert.deepEqual(sortedApplications('applied-desc'), [11, 12, 13])
    assert.deepEqual(sortedApplications('applied-asc'), [13, 12, 11])
  })

  it('sorts applications by status in pipeline order, unknown statuses last', () => {
    const mixed: PostingEntry[] = [{
      job: job(5, '2026-11-01T00:00:00', '2026-12-01T00:00:00'),
      received: received(
        [1, 'Rejected', '2026-11-05T00:00:00'],
        [2, null, '2026-11-06T00:00:00'],
        [3, 'Applied', '2026-11-01T00:00:00'],
        [4, 'Offered', null],
      ),
    }]
    const view = buildView(mixed, defaultFilters, { ...defaultSorting, applications: 'status' }, now)

    assert.deepEqual(applicationIds(view, 5), [3, 4, 1, 2])
  })
})

describe('hasActiveFilters', () => {
  it('is false for the defaults and for a date field with no dates', () => {
    assert.equal(hasActiveFilters(defaultFilters), false)
    assert.equal(hasActiveFilters({ ...defaultFilters, dateField: 'applied' }), false)
  })

  it('is true once a status or a date is chosen', () => {
    assert.equal(hasActiveFilters({ ...defaultFilters, postingStatus: 'Open' }), true)
    assert.equal(hasActiveFilters({ ...defaultFilters, applicationStatus: 'Interview' }), true)
    assert.equal(hasActiveFilters({ ...defaultFilters, to: '2026-01-01' }), true)
  })
})

describe('buildView', () => {
  it('does not change the data it is given', () => {
    const before = JSON.stringify(entries)
    buildView(entries, { ...defaultFilters, applicationStatus: 'Applied' },
      { postings: 'posted-asc', applications: 'applied-asc' }, now)

    assert.equal(JSON.stringify(entries), before)
  })
})
