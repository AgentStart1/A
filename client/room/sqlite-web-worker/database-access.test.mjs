/* This is a private project. All rights reserved. */
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { DatabaseAccess } from './database-access.js'

function deferred() {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}

test('a transaction queues another connection until its finalizer releases ownership', async () => {
  const first = new DatabaseAccess('queue.db')
  const second = new DatabaseAccess('/queue.db')
  const entered = deferred()
  const queued = deferred()
  let transaction = true
  await first.run(() => entered.resolve(), () => transaction)
  await entered.promise
  const waiting = second.run(() => queued.resolve())
  const locks = await navigator.locks.query()
  assert(locks.pending.some(lock => lock.name === first.name))
  transaction = false
  await first.run(() => {})
  await waiting
  await queued.promise
  assert(!(await navigator.locks.query()).held.some(lock => lock.name === first.name))
})

test('a failed statement retains transaction ownership until rollback without replaying it', async () => {
  const first = new DatabaseAccess('rollback.db')
  const second = new DatabaseAccess('rollback.db')
  let attempts = 0
  await first.run(() => {}, () => true)
  await assert.rejects(first.run(() => {
    attempts++
    throw new Error('invalid statement')
  }, () => true), /invalid statement/)
  const waiting = second.run(() => 'next connection')
  assert((await navigator.locks.query()).pending.some(lock => lock.name === first.name))
  await first.run(() => {})
  assert.equal(await waiting, 'next connection')
  assert.equal(attempts, 1)
})

test('failure outside a transaction releases ownership and databases remain independent', async () => {
  const first = new DatabaseAccess('failure.db')
  const other = new DatabaseAccess('other.db')
  await first.run(() => {}, () => true)
  assert.equal(await other.run(() => 'independent'), 'independent')
  await assert.rejects(first.run(() => { throw new Error('failed') }), /failed/)
  assert.equal(await new DatabaseAccess('failure.db').run(() => 'recovered'), 'recovered')
})
