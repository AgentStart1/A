/* This is a private project. All rights reserved. */

/** Coordinate database operations, not browser windows. A lease spans BEGIN through its finalizer. */
export class DatabaseAccess {
  constructor(fileName, locks = globalThis.navigator?.locks) {
    this.name = `alphaheart.sqlite:${new URL(fileName, 'file:///').pathname}`
    this.locks = locks
    this.release = null
  }

  async run(operation, retain = () => false) {
    if (this.locks && !this.release) this.release = await this.acquire()
    try {
      return await operation()
    } finally {
      if (!retain()) await this.unlock()
    }
  }

  acquire() {
    return new Promise((resolve, reject) => {
      const completion = this.locks.request(this.name, { mode: 'exclusive' }, () =>
        new Promise(done => {
          resolve(async () => {
            done()
            await completion
          })
        })
      )
      completion.catch(reject)
    })
  }

  async unlock() {
    const release = this.release
    this.release = null
    if (release) await release()
  }
}
