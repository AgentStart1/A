/* This is a private project. All rights reserved. */
import { MAX_VIDEO_LATENESS } from "./video-sample.mjs";
const MAX_AUDIO_NODES = 64;

// No DOM widgets or Compose types. The owner must call close on removal.
export function createHost(
  url,
  onState,
  onFrame,
  autoStart = true,
  path = "rgba-copy",
) {
  return new MediaHost(url, onState, onFrame, { autoStart, path });
}

export class MediaHost {
  constructor(url, onState, onFrame, dependencies = {}) {
    this.path = dependencies.path ?? "rgba-copy";
    if (!["rgba-copy", "gpu-texture"].includes(this.path))
      throw new Error("Unknown media path");
    this.url = url;
    this.onState = onState;
    this.onFrame = onFrame;
    this.createWorker =
      dependencies.createWorker ??
      (() =>
        new Worker(new URL("./decode-worker.mjs", import.meta.url), {
          type: "module",
        }));
    this.createAudio =
      dependencies.createAudio ??
      (() => new AudioContext({ latencyHint: "playback" }));
    this.now = dependencies.now ?? (() => performance.now() / 1000);
    this.scheduler = dependencies.scheduler ?? {
      setTimeout: (callback, ms) => setTimeout(callback, ms),
      clearTimeout: (timer) => clearTimeout(timer),
    };
    this.sleepers = new Map();
    this.worker = null;
    this.pending = new Map();
    this.nodes = new Set();
    this.position = 0;
    this.duration = 0;
    this.status = "idle";
    this.intent = "paused";
    this.errorMessage = "";
    this.volume = 1;
    this.muted = false;
    this.generation = 0;
    this.request = 0;
    this.frame = null;
    this.metrics = {
      decoded: 0,
      acceptedFrames: 0,
      backpressureWaits: 0,
      dropped: 0,
      skippedBeforeConversion: 0,
      copiedBytes: 0,
      readbackMs: 0,
      maxSkew: 0,
      lateAudioSamples: 0,
    };
    this.visibility = () => {
      if (document.hidden && this.intent === "playing") this.pause();
    };
    this.ready = Promise.resolve();
    if (dependencies.autoStart !== false) this.start();
  }
  start(waitForPlayback = false) {
    if (this.status !== "idle" && !(waitForPlayback && this.status === "error"))
      return this.ready;
    if (typeof document !== "undefined")
      document.addEventListener("visibilitychange", this.visibility);
    this.ready = this.open(0, waitForPlayback);
    return this.ready;
  }
  publish(error = this.errorMessage, force = true) {
    if (this.status === "closed") return;
    const now = this.now();
    if (
      !force &&
      this.lastStatus === this.status &&
      now - this.lastPublish < 0.1
    )
      return;
    this.lastStatus = this.status;
    this.lastPublish = now;
    this.onState(
      Object.freeze({
        status: this.status,
        position: this.currentTime(),
        duration: this.duration,
        volume: this.volume,
        muted: this.muted,
        error,
        path: this.path,
        ...this.metrics,
      }),
    );
  }
  // Audio scheduling and starvation checks stay on the context's render timeline.
  renderTime() {
    if (!["playing", "buffering"].includes(this.status)) return this.position;
    const elapsed = this.context
      ? this.context.currentTime - this.clockStart
      : this.now() - this.clockStart;
    return Math.min(this.duration, this.position + Math.max(0, elapsed));
  }
  // Video, progress and end detection follow the audio reaching the output device.
  currentTime() {
    if (!this.context || !["playing", "buffering"].includes(this.status))
      return this.renderTime();
    const context = this.context;
    const stamp = context.getOutputTimestamp?.();
    const validStamp =
      Number.isFinite(stamp?.contextTime) &&
      stamp.contextTime >= 0 &&
      Number.isFinite(stamp?.performanceTime) &&
      stamp.performanceTime > 0;
    const latency = (value) =>
      Number.isFinite(value) && value > 0 ? value : 0;
    const output = validStamp
      ? stamp.contextTime + this.now() - stamp.performanceTime / 1000
      : context.currentTime -
        latency(context.baseLatency) - latency(context.outputLatency);
    // A suspended render clock caps extrapolation while already-queued sound drains.
    const outputTime = Math.max(0, Math.min(context.currentTime, output));
    const position = Math.min(
      this.duration, this.position + Math.max(0, outputTime - this.clockStart),
    );
    // Refined timestamps or a changing output latency may hold video, never rewind it.
    this.outputPosition = Math.max(this.outputPosition ?? this.position, position);
    return this.outputPosition;
  }
  rpc(op, values = {}) {
    const id = ++this.request;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject, op });
      this.worker.postMessage({ id, op, ...values });
    });
  }
  cancelGeneration() {
    this.generation++;
    this.scheduler.clearTimeout(this.timer);
    for (const [timer, resolve] of this.sleepers) {
      this.scheduler.clearTimeout(timer);
      resolve();
    }
    this.sleepers.clear();
    this.worker?.terminate();
    this.worker = null;
    for (const pending of this.pending.values())
      pending.reject(new DOMException("Superseded", "AbortError"));
    this.pending.clear();
    for (const node of this.nodes) {
      node.onended = null;
      node.stop();
      node.disconnect();
    }
    this.nodes.clear();
    this.frame?.frame?.close();
    this.frame = null;
    this.firstAudio = null;
  }
  async open(time, waitForPlayback = false) {
    this.cancelGeneration();
    const generation = this.generation;
    this.position = time;
    this.outputPosition = time;
    this.status = "loading";
    this.errorMessage = "";
    this.publish();
    try {
      this.worker = this.createWorker();
      this.worker.onmessage = ({ data }) => {
        const pending = this.pending.get(data.id);
        if (!pending) {
          data.result?.frame?.close();
          return;
        }
        this.pending.delete(data.id);
        if (pending.op === "video" && data.result) {
          this.metrics.skippedBeforeConversion +=
            data.result.skippedBeforeConversion ?? 0;
          if (!data.result.ended) {
            this.metrics.decoded++;
            this.metrics.copiedBytes += data.result.pixels?.byteLength ?? 0;
            this.metrics.readbackMs += data.result.readbackMs;
          }
        }
        if (data.error) pending.reject(new Error(data.error));
        else
          pending.resolve(
            pending.op === "video" && data.result?.ended ? null : data.result,
          );
      };
      this.worker.onerror = () =>
        this.fail(
          new Error(
            "Media decoder worker failed. Check browser support and CSP.",
          ),
          generation,
        );
      const metadata = await this.rpc("open", {
        url: this.url,
        time,
        path: this.path,
      });
      if (generation !== this.generation) return;
      if (!Number.isFinite(metadata.duration) || metadata.duration <= 0)
        throw new Error("This experiment requires finite, seekable media.");
      const firstAudio = metadata.audio ? await this.rpc("audio") : null;
      if (generation !== this.generation) return;
      const frame = metadata.video ? await this.rpc("video", { time }) : null;
      if (generation !== this.generation) {
        frame?.frame?.close();
        return;
      }
      this.metadata = metadata;
      this.duration = metadata.duration;
      this.videoEnded = !metadata.video;
      this.audioEnded = !metadata.audio;
      this.audioEnd = time;
      this.firstAudio = firstAudio;
      this.frame = null;
      if (frame) await this.presentPreview(frame, generation);
      if (generation !== this.generation) return;
      if (!waitForPlayback) {
        this.status = "paused";
        this.publish();
      }
    } catch (error) {
      this.fail(error, generation);
    }
  }
  fail(error, generation) {
    if (generation !== this.generation || this.status === "closed") return;
    this.position = this.currentTime();
    this.cancelGeneration();
    this.status = "error";
    this.intent = "paused";
    this.audioClosing = this.closeAudio();
    this.audioClosing.catch(() => {});
    // Do not disclose a signed media URL from a fetch exception.
    this.errorMessage =
      "Media playback failed. Check HTTPS, CORS, container and browser codec support. " +
      (error?.message?.startsWith("Unsupported browser codec:")
        ? error.message
        : "");
    this.publish();
  }
  async togglePlayback() {
    if (this.intent === "playing") this.pause();
    else await this.play();
  }
  async togglePlaybackOnDemand() {
    if (this.status !== "idle" && this.status !== "error")
      return this.togglePlayback();

    let generation = this.generation;
    try {
      // Request audio activation on the click stack before awaiting media reads.
      this.prepareAudio();
      const activation = this.context.resume();
      const ready = this.start(true);
      generation = this.generation;
      await Promise.all([activation, ready]);
      if (generation !== this.generation || this.status !== "loading") return;
      this.status = "paused";
      await this.play();
    } catch (error) {
      this.fail(error, generation);
    }
  }
  prepareAudio() {
    this.context = this.createAudio();
    if (this.context.destination.maxChannelCount === 0)
      throw new Error("No audio output device is available.");
    this.context.onstatechange = () => {
      if (this.context.state === "suspended" && this.status === "playing")
        this.pause();
    };
    this.gain = this.context.createGain();
    this.gain.connect(this.context.destination);
    this.gain.gain.value = this.muted ? 0 : this.volume;
  }
  toggleMute() {
    this.setVolume(this.volume, !this.muted);
  }
  async play() {
    if (!["paused", "ended"].includes(this.status)) return;
    this.intent = "playing";
    if (this.status === "ended") {
      await this.seek(0);
      return;
    }
    this.status = "starting";
    const generation = this.generation;
    try {
      // Called synchronously by the Compose button to retain user activation.
      if (this.metadata.audio && !this.context) this.prepareAudio();
      if (this.context) await this.context.resume();
      if (generation !== this.generation) return;
      this.clockStart = this.context
        ? this.context.currentTime + 0.08
        : this.now();
      this.status = "playing";
      this.publish();
      this.audioTask = this.pumpAudio(generation);
      this.videoTask = this.pumpVideo(generation);
      this.tick(generation);
    } catch (error) {
      this.fail(error, generation);
    }
  }
  async pumpAudio(generation) {
    try {
      while (
        generation === this.generation &&
        ["playing", "buffering"].includes(this.status) &&
        !this.audioEnded
      ) {
        if (
          this.audioEnd - this.renderTime() > 0.5 ||
          this.nodes.size >= MAX_AUDIO_NODES
        ) {
          await this.wait(10, generation);
          continue;
        }
        const sample = this.firstAudio ?? (await this.rpc("audio"));
        this.firstAudio = null;
        if (generation !== this.generation) return;
        if (!sample) {
          this.audioEnded = true;
          await this.resumeBuffer(generation);
          return;
        }
        const buffer = this.context.createBuffer(
          sample.channels.length,
          sample.channels[0].length,
          sample.sampleRate,
        );
        sample.channels.forEach((channel, i) =>
          buffer.copyToChannel(channel, i),
        );
        const offset = Math.max(0, this.renderTime() - sample.timestamp);
        this.audioEnd = sample.timestamp + sample.duration;
        const seekOffset = Math.max(0, this.position - sample.timestamp);
        if (offset > seekOffset + 0.002) this.metrics.lateAudioSamples++;
        if (offset >= sample.duration) continue;
        const node = this.context.createBufferSource();
        node.buffer = buffer;
        node.connect(this.gain);
        this.nodes.add(node);
        node.onended = () => {
          this.nodes.delete(node);
          node.disconnect();
        };
        const when =
          this.clockStart + sample.timestamp + offset - this.position;
        node.start(Math.max(this.context.currentTime, when), offset);
        if (
          this.audioEnd - this.renderTime() >= 0.3 ||
          this.nodes.size >= MAX_AUDIO_NODES
        )
          await this.resumeBuffer(generation);
      }
    } catch (error) {
      this.fail(error, generation);
    }
  }
  async resumeBuffer(generation) {
    if (this.status !== "buffering" || generation !== this.generation) return;
    await this.bufferingTask;
    if (generation !== this.generation) return;
    await this.context.resume();
    if (
      generation === this.generation &&
      this.status === "buffering" &&
      this.intent === "playing"
    )
      this.status = "playing";
  }
  wait(ms, generation) {
    if (generation !== this.generation) return Promise.resolve();
    return new Promise((resolve) => {
      const timer = this.scheduler.setTimeout(() => {
        this.sleepers.delete(timer);
        resolve();
      }, ms);
      this.sleepers.set(timer, resolve);
    });
  }
  async pumpVideo(generation) {
    try {
      while (
        generation === this.generation &&
        ["playing", "buffering"].includes(this.status) &&
        !this.videoEnded
      ) {
        if (this.frame) {
          await this.wait(5, generation);
          continue;
        }
        const frame = await this.rpc("video", { time: this.currentTime() });
        if (generation !== this.generation) {
          frame?.frame?.close();
          return;
        }
        if (!frame) {
          this.videoEnded = true;
          return;
        }
        this.frame = frame;
      }
    } catch (error) {
      this.fail(error, generation);
    }
  }
  // A false result retains ownership here; true/undefined transfers it to UI.
  present(frame, generation = this.generation) {
    const accepted =
      this.onFrame(
        Object.freeze({
          ...frame,
          generation,
          onConsumed:
            this.path === "gpu-texture"
              ? () => this.wakeVideo(generation)
              : undefined,
        }),
      ) !== false;
    if (accepted) this.metrics.acceptedFrames++;
    else this.metrics.backpressureWaits++;
    return accepted;
  }
  wakeVideo(generation) {
    if (
      generation !== this.generation ||
      !["playing", "buffering"].includes(this.status)
    )
      return;
    this.scheduler.clearTimeout(this.timer);
    this.timer = this.scheduler.setTimeout(() => this.tick(generation), 0);
  }
  async presentPreview(frame, generation) {
    let transferred = false;
    try {
      while (generation === this.generation && this.status !== "closed") {
        transferred = this.present(frame, generation);
        if (transferred) return;
        await this.wait(5, generation);
      }
    } finally {
      if (!transferred) frame.frame?.close();
    }
  }
  tick(generation) {
    if (
      generation !== this.generation ||
      !["playing", "buffering"].includes(this.status)
    )
      return;
    const time = this.currentTime();
    const rendered = this.renderTime();
    if (
      this.status === "playing" &&
      this.context &&
      !this.audioEnded &&
      this.audioEnd > this.position + 0.1 &&
      this.audioEnd - rendered < 0.04
    ) {
      this.status = "buffering";
      this.bufferingTask = this.context.suspend();
      this.bufferingTask.catch((error) => this.fail(error, generation));
    }
    if (
      this.frame &&
      this.frame.timestamp <= time
    ) {
      const frame = this.frame;
      if (frame.timestamp + frame.duration < time - MAX_VIDEO_LATENESS) {
        this.frame = null;
        this.metrics.dropped++;
        frame.frame?.close();
      } else {
        try {
          if (this.present(frame, generation)) {
            this.frame = null;
            this.metrics.maxSkew = Math.max(
              this.metrics.maxSkew,
              Math.abs(time - frame.timestamp),
            );
          }
        } catch (error) {
          this.fail(error, generation);
          return;
        }
      }
    }
    if (time >= this.duration && this.videoEnded && this.audioEnded) {
      this.position = this.duration;
      this.status = "ended";
      this.intent = "paused";
    }
    this.publish("", false);
    if (["playing", "buffering"].includes(this.status))
      this.timer = this.scheduler.setTimeout(() => this.tick(generation), 10);
  }
  pause() {
    if (this.intent !== "playing") return;
    const position = this.currentTime();
    this.intent = "paused";
    // A new decoder generation also discards all previously scheduled audio.
    this.ready = this.open(position);
  }
  async seek(time) {
    if (this.status === "closed") return;
    if (this.status === "ended") this.intent = "playing";
    this.ready = this.open(Math.max(0, Math.min(this.duration, time)));
    const generation = this.generation;
    await this.ready;
    if (
      generation === this.generation &&
      this.intent === "playing" &&
      this.status === "paused"
    )
      await this.play();
  }
  setVolume(volume, muted = this.muted) {
    this.volume = Math.max(0, Math.min(1, volume));
    this.muted = muted;
    if (this.gain) this.gain.gain.value = muted ? 0 : this.volume;
    this.publish();
  }
  failRendering() {
    this.fail(new Error("Media texture import failed"), this.generation);
  }
  async closeAudio() {
    const context = this.context;
    this.context = null;
    this.gain?.disconnect();
    this.gain = null;
    if (context) {
      context.onstatechange = null;
      await context.close();
    }
  }
  async close() {
    if (this.status === "closed") return;
    this.status = "closed";
    this.intent = "paused";
    this.cancelGeneration();
    if (typeof document !== "undefined")
      document.removeEventListener("visibilitychange", this.visibility);
    await this.closeAudio();
    await Promise.allSettled([
      this.audioTask,
      this.videoTask,
      this.ready,
      this.audioClosing,
    ]);
    this.onFrame = () => {};
    this.onState = () => {};
  }
}

// Top-level Kotlin/Wasm @JsModule functions bind to the module's default export.
export default createHost;
