# HEIF/HEIC Image Upload & Conversion — Implementation Notes

> Project: **kaptaitourist** (Hotel Management System) <br>
> Code: `com.kaptaitourist.kaptaitourist.core.util.ImageUtil`

This document explains how the backend accepts **HEIF/HEIC** uploads (the
default photo format on iPhones) alongside existing JPEG/PNG/WebP support,
converts them to JPEG using **libheif**, fixes orientation/rotation correctly
using **EXIF** metadata, feeds them into the existing compression pipeline
unchanged, and how to install the required native tooling on a **local**
machine and on the **server (Docker)**.

---

## 1. Goal

Allow users to upload HEIC/HEIF images alongside existing JPEG/PNG/WebP
support, convert them to JPEG on the backend, fix orientation/rotation
correctly, and feed them into the existing compression pipeline unchanged.

---

## 2. The big picture

```
 upload (HEIC/HEIF)
        │
        ▼
 [1] validate content-type + size
        │
        ▼
 [2] convert HEIC → JPEG      ── libheif (heif-convert CLI, via ProcessBuilder)
        │
        ▼
 [3] read EXIF orientation    ── metadata-extractor (pure Java)
        │
        ▼
 [4] compress + rotate        ── Thumbnailator
        │
        ▼
 ProcessedImage (JPEG bytes + optional thumbnail)
```

Three distinct pieces of native/3rd-party tech are involved, and they solve
**different** problems:

| Concern | Tool | Type | Where it lives |
|---|---|---|---|
| Decode HEIC/HEIF → JPEG | **libheif** (`heif-convert`) | Native system binary | Installed on the OS / Docker image |
| Read EXIF orientation | **metadata-extractor** | Pure-Java library | Gradle dependency in the jar |
| Resize / compress / rotate | **Thumbnailator** | Pure-Java library | Gradle dependency in the jar |

---

## 3. Why this needs a native library (not pure Java)

HEIC/HEIF files store their image data with the **HEVC (H.265) video codec**
internally. There is no mature, production-grade **pure-Java** HEVC decoder —
this codec is complex enough that even major platforms (Apple, Google) rely on
native or hardware decoders for it. Java's built-in `ImageIO` has no HEIC/HEIF
reader out of the box, and no Java imaging library adds one without ultimately
calling into native code somewhere.

So the real choice was never "native vs. pure Java" — it was **where** the
native code lives:

- **(A)** A native library installed on the host system, called via a
  command-line tool. ← **what we chose**
- **(B)** A native library bundled inside a Java dependency jar (e.g.
  JavaCV/FFmpeg, or NightMonkeys which uses libheif via JDK 21's Foreign
  Function & Memory API).

We evaluated both and went with **(A)**: shelling out to the `heif-convert`
command-line tool from the `libheif` project. Reasons:

- **Simplest mental model:** one subprocess call, no JVM-version-specific
  native bindings, no Foreign Function API quirks.
- `libheif` is the **reference, most battle-tested** HEIC decoder available
  on Linux — the same library that handles HEIC almost everywhere else
  (browsers, image viewers, etc.).
- **Avoids inflating the deployable artifact size** (JavaCV/FFmpeg bundles are
  very large; NightMonkeys avoids that but needs JDK 21+ and is a newer,
  less battle-tested project).

Because we use `ProcessBuilder` (not direct native memory access), **no JVM
flags** such as `--enable-native-access` are required — that flag is only
needed for libraries like NightMonkeys that use the JDK 21 Foreign Function &
Memory API directly. Since we shell out to a CLI tool, the JVM never touches
native memory directly.

> ⚠️ **A wrong turn we took and corrected:** We initially tried
> `com.twelvemonkeys.imageio:imageio-heif` — this dependency **does not
> exist**. TwelveMonkeys has never shipped HEIC/HEIF support (open feature
> request since 2018, still unresolved). The `imageio-core` artifact some
> early code referenced is just shared base classes for TwelveMonkeys' other
> plugins (JPEG, TIFF, WebP) and contains no format readers of its own. These
> were removed from `build.gradle` — they did nothing for HEIC.

---

## 4. Installing libheif

`libheif` is a **native system library** — NOT a Java/Gradle dependency. It
must be installed separately on **every** machine that runs the app: your
local dev machine **AND** the production/server machine. Installing it in one
place does not make it available in the other.

The package we need provides the **`heif-convert`** binary.

### 4.1 Local machine

**Ubuntu / Debian Linux**
```bash
sudo apt-get update
sudo apt-get install -y libheif-examples
```

**macOS (Homebrew)**
```bash
brew install libheif
```

**Windows**
Native libheif does not install cleanly on plain Windows. Do HEIC testing
inside **WSL2 (Ubuntu)** using the Ubuntu/Debian command above, or skip local
testing of this specific feature and verify only in the Docker container (most
Spring Boot apps deploy to Linux anyway).

### 4.2 Server / production (Docker)

Add the installation step to your `Dockerfile` so the binary is baked into the
image:

```dockerfile
FROM eclipse-temurin:21-jre

# Install the heif-convert CLI from libheif
RUN apt-get update && \
    apt-get install -y libheif-examples && \
    rm -rf /var/lib/apt/lists/*

COPY build/libs/kaptaitourist-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> The `rm -rf /var/lib/apt/lists/*` keeps the image small. No special JVM
> flags (like `--enable-native-access`) are required for this CLI-based
> approach — see section 3.

### 4.3 Verify the installation (any environment)

```bash
heif-convert --help
```

If this prints usage info instead of `command not found`, libheif is installed
correctly.

---

## 5. Gradle dependencies (`build.gradle`)

libheif is **not** here — only the pure-Java libraries are. The final, correct
dependency list relevant to this feature (in `kaptaitourist/build.gradle`):

```gradle
// Image compression — used for ALL formats, including converted HEIC
implementation 'net.coobird:thumbnailator:0.4.21'

// EXIF orientation reading — used to manually fix image rotation
implementation 'com.drewnoakes:metadata-extractor:2.19.0'
```

| Dependency | Role in HEIF flow |
|---|---|
| `net.coobird:thumbnailator` | Resize + re-encode the JPEG produced from HEIC; applies our computed rotation. (Already existed.) |
| `com.drewnoakes:metadata-extractor` | Reads the EXIF `Orientation` tag so we can rotate deterministically. (Added for this feature.) |

**Removed (did nothing for HEIC, were dead weight):**

- `com.twelvemonkeys.imageio:imageio-core` ← no HEIC support exists
- `com.twelvemonkeys.imageio:imageio-jpeg` ← unrelated to HEIC
- `org.sejda.imageio:webp-imageio` ← unrelated to HEIC

**Not used (alternatives considered, not chosen — see section 3):**

- `com.github.gotson.nightmonkeys:imageio-heif` ← needs JDK 21 FFM API
- `org.bytedeco:javacv-platform` ← bundles full FFmpeg, large artifact size

---

## 6. The `ImageUtil.process()` flow, step by step

### 6.1 Accepted types

```java
private static final List<String> ALLOWED_TYPES = List.of(
        "image/jpeg", "image/jpg", "image/png", "image/webp",
        "image/heic", "image/heif"
);
private static final List<String> HEIF_TYPES = List.of("image/heic", "image/heif");
```

### Step 1 — Validate content type

Reject anything not in: `jpeg`, `jpg`, `png`, `webp`, `heic`, `heif`.
**Why:** fail fast on garbage input before doing any expensive work.

### Step 2 — Enforce 4 MB hard limit

Reject files over 4 MB before any processing.
**Why:** avoid wasting CPU/memory on oversized uploads; cheap check done first.

### Step 3 — Convert HEIC/HEIF → JPEG (only if content type is heic/heif)

Implemented in `convertHeicToJpeg()`, which shells out to `heif-convert`:

```java
Process process = new ProcessBuilder(
        "heif-convert", "-q", "90",
        tempHeic.toString(), tempJpeg.toString()
).redirectErrorStream(true).start();
```

- **Temp files, not streams** — writes the uploaded bytes to a temp file
  (`heif-convert` needs a real file path, not a stream).
- Runs: `heif-convert -q 90 <input.heic> <output.jpg>`.
- **20-second timeout** — forcibly kills the process (`destroyForcibly()`) if
  it hangs, preventing one bad upload from blocking a thread forever.
- **Exit-code check** — non-zero means conversion failed, and we throw a clear
  error rather than silently continuing.
- **Output validation** — validates the output is a real JPEG by checking its
  size and **magic bytes** (`0xFF 0xD8`) before trusting it.
  > **Why we deliberately do NOT use the `--strict=false` flag:** that flag can
  > make `heif-convert` exit `0` (success) while writing a truncated or
  > corrupted JPEG for borderline-malformed HEIC files. A loud, clear failure
  > here is much better than a silent garbage file being passed downstream —
  > which is exactly what caused a confusing generic *"No suitable ImageReader
  > found for source data"* error deep inside the compression step during
  > development.
- **Cleanup** — both temp files are deleted in a `finally` block regardless of
  success or failure, so no leftover files accumulate on disk.

After conversion the content type becomes `image/jpeg` and the filename
extension is rewritten to `.jpg`.

### Step 4 — Read orientation (for ALL formats, not just HEIC)

iPhone photos are frequently stored "sideways" with an EXIF **Orientation**
tag telling viewers how to rotate them. `heif-convert` preserves that tag, so
after conversion we read it ourselves and rotate explicitly. Implemented in
`extractOrientationDegrees()`, using metadata-extractor:

```
ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
int orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
return switch (orientation) {
    case 6 -> 90;   // rotate 90° CW  (the common iPhone-portrait case)
    case 3 -> 180;
    case 8 -> 270;
    default -> 0;   // 1 = normal; mirrored variants (2,4,5,7) not handled
};
```

- Reads the EXIF "Orientation" tag from the image bytes.
- Maps EXIF orientation values to clockwise rotation degrees:
  `6 -> 90`, `3 -> 180`, `8 -> 270`, anything else `-> 0`. (Mirrored variants
  2/4/5/7 are intentionally not handled — rare in real camera/phone output.)
- Returns `0` (no rotation) if anything goes wrong reading metadata, rather
  than throwing — a missing/corrupt EXIF block should never block an
  otherwise-valid upload.

**Why this step exists and why it's done this way:** Thumbnailator (the
compression library) has a built-in `useExifOrientation` feature that defaults
to **TRUE** — it tries to auto-detect and correct rotation on its own. During
development this caused real problems:

- It is documented as unreliable on certain real-world JPEG structures
  (multiple open issues on the Thumbnailator project about it silently failing
  to detect orientation).
- This produced inconsistent, hard-to-debug rotation bugs (90° wrong, then
  180° wrong after a different attempted fix layered another rotation on top of
  one Thumbnailator was already silently getting wrong).

**The fix:** stop trusting Thumbnailator's automatic detection entirely. We
read the orientation ourselves with a dedicated, reliable library
(metadata-extractor), and apply it explicitly and deterministically. This was
verified empirically: running `exiftool` on a real `heif-convert` JPEG output
confirmed an `Orientation: Rotate 90 CW` tag (EXIF value `6`), confirming both
the tag's existence and that our mapping (`6 -> rotate 90° clockwise`) matches
Thumbnailator's own `rotate()` convention (which also rotates clockwise).

### Step 5 — Compress (`compress()` method)

```
// Always disabled — exactly one rotation mechanism exists in the pipeline: ours.
builder.useExifOrientation(false);
```

- **WebP** passes through unchanged — Thumbnailator cannot encode WebP, and
  (pre-existing limitation) rotation is not applied to WebP files.
- **PNG and JPEG** go through Thumbnailator:
  - Resized either to fixed thumbnail dimensions (400×300, aspect ratio kept)
    or scaled by a percentage for the compressed "original" (70% dimensions
    normally, 50% for PNG since PNG can't use lossy quality reduction the way
    JPEG can).
  - `useExifOrientation(false)` is **ALWAYS** set, explicitly, for every format
    — not just HEIC-derived images. This guarantees Thumbnailator's own
    (unreliable) detection can never run, so there is exactly one rotation
    mechanism in the whole pipeline: our own, from Step 4. No possibility of
    two mechanisms stacking and producing a wrong-by-180° result like before.
  - Our computed rotation (from Step 4) is then applied explicitly via
    `builder.rotate(degrees)`, only if non-zero.
  - Output quality fixed at `0.60` (JPEG) to hit the ~25% file-size target.

### Step 6 — Build result

Returns a `ProcessedImage` containing the compressed "original" bytes,
optional thumbnail bytes, resolved filenames (with extension forced to `.jpg`
for HEIC-derived files), and the resolved content type.

---

## 7. Security / robustness notes

- All file size and content-type checks happen **BEFORE** the expensive native
  process invocation — a malicious or broken upload is rejected cheaply.
- The **20-second timeout** on `heif-convert` prevents a single malformed or
  adversarial file from hanging a server thread indefinitely.
- **Output validation** (size + JPEG magic bytes) after conversion prevents
  corrupted intermediate files from silently propagating into storage.
- **Temp files are always cleaned up** via try/finally, regardless of success
  or failure — no leftover files accumulating on disk.
- No client-supplied data is trusted for filenames beyond sanitization
  (`sanitize()` strips anything outside `[a-zA-Z0-9._-]`).

---

## 8. End-to-end checklist

1. **Gradle** — `thumbnailator` + `metadata-extractor` present in
   `build.gradle`. ✅ (already added)
2. **System package (native, install separately on every machine):**
   `libheif-examples` — provides the `heif-convert` CLI binary.
   - Local: `sudo apt-get install -y libheif-examples` (or `brew install
     libheif` / WSL2). Verify with `heif-convert --help`.
   - Server: installed in the `Dockerfile`. Verify `heif-convert --help`
     inside the running container.
3. **No JVM flags** required (this approach uses `ProcessBuilder`, not direct
   native memory access from the JVM).

If HEIC uploads fail in production with a conversion error, the **first** thing
to check is whether `heif-convert` exists on that machine — that single native
binary is the only piece not shipped inside the jar.
