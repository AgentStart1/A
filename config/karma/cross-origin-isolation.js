/* This is a private project. All rights reserved. */
// Production browser storage uses SQLite OPFS and requires cross-origin isolation.
config.customHeaders = (config.customHeaders || []).concat([
    { match: '.*', name: 'Cross-Origin-Opener-Policy', value: 'same-origin' },
    { match: '.*', name: 'Cross-Origin-Embedder-Policy', value: 'require-corp' },
]);

// Storage integration deliberately holds a transaction for four real seconds to verify queued access.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 15000 });
