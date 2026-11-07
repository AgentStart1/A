/* This is a private project. All rights reserved. */
// Compose UI tests load translations through HTTP just like the browser application.
const resourceFs = require('fs');
const resourcePath = require('path');
const resourceDirectory = resourcePath.join(config.basePath, 'kotlin', 'composeResources');
if (resourceFs.existsSync(resourceDirectory)) {
    const normalized = resourceDirectory.replace(/\\/g, '/');
    config.files.push({ pattern: `${normalized}/**/*`, included: false, served: true, watched: false });
    for (const namespace of resourceFs.readdirSync(resourceDirectory)) {
        config.proxies[`/composeResources/${namespace}/`] = `/absolute${normalized}/${namespace}/`;
    }
}
