/* This is a private project. All rights reserved. */
export function observeOpfsWrites(writeStep) {
  const nativeCreate = FileSystemFileHandle.prototype.createWritable;
  FileSystemFileHandle.prototype.createWritable = async function (...options) {
    const stream = await nativeCreate.apply(this, options);
    const write = stream.write.bind(stream);
    const abort = stream.abort.bind(stream);
    const close = stream.close.bind(stream);
    stream.write = bytes => writeStep(write, bytes, this.name);
    stream.abort = async reason => {
      await abort(reason);
      self.postMessage({probe: 'closed', kind: 'abort'});
    };
    stream.close = async () => {
      await close();
      self.postMessage({probe: 'closed', kind: 'close'});
    };
    return stream;
  };
}
