import { describe, it } from 'node:test'
import assert from 'node:assert/strict'

import {
  isValidJobId,
  defaultDecision,
  canUpdateStatus,
  updateApplication,
  errorMessage,
  type Application,
} from '../src/jobs/applicantStatus.ts'

const application: Application = {
  id: 12,
  jobPostingId: 5,
  userId: 'applicant-123',
  resumeId: 8,
  status: 'Applied',
  appliedAt: '2026-11-10T12:00:00Z',
  interviewTimeSlot: null
}

describe('Applicant status management', () => {
  it('accepts valid job posting IDs', () => {
    assert.equal(isValidJobId('1'), true)
    assert.equal(isValidJobId('123'), true)
  })

  it('rejects invalid job posting IDs', () => {
    for (const id of [undefined, '', '0', '-1', 'abc', '1.5']) {
      assert.equal(isValidJobId(id), false)
    }
  })

  it('defaults new applications to Interview', () => {
    assert.equal(defaultDecision('Applied'), 'Interview')
  })

  it('preserves an existing decision', () => {
    assert.equal(defaultDecision('Offered'), 'Offered')
    assert.equal(defaultDecision('Reject'), 'Reject')
    assert.equal(defaultDecision('Interview'), 'Interview')
  })

  it('prevents changing an application status without valid interview time', () => {
    assert.equal(canUpdateStatus(application, 'Interview', '', false), false)
  })

  it('prevents saving when no application is selected', () => {
    assert.equal(canUpdateStatus(undefined, 'Interview', '', false), false)
  })

  it('prevents saving while another update is running', () => {
    assert.equal(canUpdateStatus(application, 'Interview', '', true), false)
  })

  it('prevents saving the same status', () => {
    const interviewed: Application = {
      ...application,
      status: 'Interview',
    }

    assert.equal(canUpdateStatus(interviewed, 'Interview', '', false), false)
  })

  it('updates only the selected application', () => {
    const other: Application = { ...application, id: 13 }
    const updated: Application = { ...application, status: 'Offered' }

    const result = updateApplication([application, other], updated)

    assert.equal(result[0].status, 'Offered')
    assert.equal(result[1].status, 'Applied')
  })

  it('does not mutate the original applications array', () => {
    const original = [application]
    const updated: Application = { ...application, status: 'Reject' }

    const result = updateApplication(original, updated)

    assert.notStrictEqual(result, original)
    assert.equal(original[0].status, 'Applied')
    assert.equal(result[0].status, 'Reject')
  })

  it('preserves applications when the ID does not match', () => {
    const updated: Application = {
      ...application,
      id: 999,
      status: 'Offered',
    }

    const result = updateApplication([application], updated)

    assert.deepEqual(result, [application])
  })

  it('returns the message from an Error', () => {
    assert.equal(errorMessage(new Error('Access denied')), 'Access denied')
  })

  it('returns a fallback message for unknown errors', () => {
    assert.equal(
      errorMessage(null),
      'Something went wrong. Please try again.'
    )
  })
})