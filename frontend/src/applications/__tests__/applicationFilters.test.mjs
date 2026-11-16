import test from 'node:test'
import assert from 'node:assert/strict'
import {
  countApplications,
  filterApplications,
  toggleApplicationFilter,
  applicationStatuses,
} from '../applications.ts'

const applications = [
  { id: '1', jobTitle: 'Developer', company: 'A', status: 'Applied' },
  { id: '2', jobTitle: 'Engineer', company: 'B', status: 'Applied' },
  { id: '3', jobTitle: 'Intern', company: 'C', status: 'Interview' },
  { id: '4', jobTitle: 'Designer', company: 'D', status: 'Offered' },
  { id: '5', jobTitle: 'Analyst', company: 'E', status: 'Rejected' },
  { id: '6', jobTitle: 'Tester', company: 'F', status: null },
  { id: '7', jobTitle: 'Assistant', company: 'G', status: 'Unknown' },
]

test('all application statuses are available', () => {
  assert.deepEqual(applicationStatuses, [
    'Applied', 'Interview', 'Offered', 'Rejected',
  ])
})

test('counts applications for every status', () => {
  assert.equal(countApplications(applications, 'All'), 7)
  assert.equal(countApplications(applications, 'Applied'), 2)
  assert.equal(countApplications(applications, 'Interview'), 1)
  assert.equal(countApplications(applications, 'Offered'), 1)
  assert.equal(countApplications(applications, 'Rejected'), 1)
})

test('filters applications by selected status', () => {
  const filtered = filterApplications(applications, 'Applied')
  assert.deepEqual(filtered.map((item) => item.id), ['1', '2'])
})

test('clicking the active status deselects it', () => {
  assert.equal(toggleApplicationFilter('Interview', 'Interview'), 'All')
  assert.equal(toggleApplicationFilter('Rejected', 'Rejected'), 'All')
})

test('clicking another status selects it', () => {
  assert.equal(toggleApplicationFilter('Applied', 'Offered'), 'Offered')
  assert.equal(toggleApplicationFilter('All', 'Interview'), 'Interview')
})

test('All displays every application, including unknown statuses', () => {
  assert.equal(filterApplications(applications, 'All').length, 7)
  assert.equal(countApplications(applications, 'All'), 7)
})

test('handles empty application lists', () => {
  assert.equal(countApplications([], 'Applied'), 0)
  assert.deepEqual(filterApplications([], 'Interview'), [])
})

test('status counts do not change when filtering', () => {
  const visible = filterApplications(applications, 'Interview')
  assert.equal(visible.length, 1)
  assert.equal(countApplications(applications, 'Applied'), 2)
})
