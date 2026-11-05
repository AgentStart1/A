/* This is a private project. All rights reserved. */
export function cacheParts(path) {
  const values = path.split('/');
  if (values[0] !== 'downloads' || values.some(p => !p || p === '.' || p === '..' || /[\\:\0]/.test(p))) {
    throw new Error('Invalid download-cache location');
  }
  return values;
}

export function archiveName(name) {
  const parts = name.replaceAll('\\', '/').split('/');
  if (parts.some(p => !p || p === '.' || p === '..' || /[:\0]/.test(p))) {
    throw new Error('Unsafe archive path');
  }
  return parts.join('/');
}

export async function cacheFile(path, create = false) {
  const values = cacheParts(path);
  try {
    let directory = await navigator.storage.getDirectory();
    for (const value of values.slice(0, -1)) directory = await directory.getDirectoryHandle(value, {create});
    return await directory.getFileHandle(values.at(-1), {create});
  } catch (failure) {
    throw new Error('Unable to ' + (create ? 'create' : 'read') + ' cached file: ' + path + ': ' + failure.message);
  }
}
