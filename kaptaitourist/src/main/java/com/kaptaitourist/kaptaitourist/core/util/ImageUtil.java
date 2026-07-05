package com.kaptaitourist.kaptaitourist.core.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ImageUtil {

    // Recognized HEIF/HEIC ISO-BMFF brands (the 4 bytes following the "ftyp" box marker).
    private static final Set<String> HEIF_BRANDS = Set.of(
            "heic", "heix", "hevc", "heim", "heis", "hevm", "hevs", "mif1", "msf1", "heif");

    // target: output is ~25% of original file size
    // achieved by scaling dimensions to 70% + quality 0.6
    private static final double DIMENSION_SCALE = 0.70;
    private static final double OUTPUT_QUALITY  = 0.60;
    private static final long   MAX_BYTES       = 4L * 1024 * 1024; // 4 MB hard limit

    private static final int THUMBNAIL_WIDTH  = 400;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final long HEIF_CONVERT_TIMEOUT_SECONDS = 20;

    public ProcessedImage process(byte[] rawBytes, String contentType, String originalFilename,
                                  String identifier, boolean generateThumbnail) throws IOException {

        if (rawBytes == null || rawBytes.length == 0)
            throw new ValidationException("Uploaded image is empty.");

        if (rawBytes.length > MAX_BYTES) {
            double sizeInMB = rawBytes.length / (1024.0 * 1024.0);
            throw new ValidationException(
                    String.format("Image must not exceed 4MB. Received: %.2fMB", sizeInMB));
        }

        // Trust the actual bytes, not the client-supplied Content-Type header. A HEIC photo
        // renamed .jpg arrives as "image/jpeg", and without this it would skip HEIF conversion
        // and hit the JPEG reader → "no suitable ImageReader found" → 500. Detection also rejects
        // non-image uploads (e.g. an .exe labelled image/webp) up front with a clean 400.
        String detectedType = detectImageType(rawBytes);
        if (detectedType == null)
            throw new ValidationException(
                    "Unsupported or corrupt image. Allowed formats: JPEG, PNG, WEBP, HEIC/HEIF.");
        if (contentType != null && !contentType.isBlank())
            log.debug("Upload '{}' declared '{}', detected '{}'", originalFilename, contentType, detectedType);

        String effectiveContentType = detectedType;
        if ("image/heif".equals(effectiveContentType)) {
            rawBytes = convertHeicToJpeg(rawBytes);
            effectiveContentType = "image/jpeg";
            originalFilename = changeExtensionToJpg(originalFilename);
        }

        // Read orientation from whatever bytes are about to be compressed —
        // works uniformly whether this came from a HEIC conversion or a plain
        // JPEG upload. Single, deterministic source of truth; Thumbnailator's
        // own EXIF auto-detection is explicitly disabled below to avoid any
        // chance of it double-applying or silently failing.
        int rotationDegrees = extractOrientationDegrees(rawBytes);

        String extension = switch (effectiveContentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };

        String baseName = UUID.randomUUID().toString();

        byte[] originalBytes = compress(rawBytes, extension, null, null, DIMENSION_SCALE, rotationDegrees);
        String originalFileName = baseName + "." + extension;

        byte[] thumbnailBytes = null;
        String thumbnailFileName = null;
        if (generateThumbnail) {
            thumbnailBytes = compress(rawBytes, extension, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, null, rotationDegrees);
            thumbnailFileName = baseName + "_thumb." + extension;
        }

        String resolvedContentType = switch (extension.toLowerCase()) {
            case "png"  -> "image/png";
            case "webp" -> "image/webp";
            default     -> "image/jpeg";
        };

        return ProcessedImage.builder()
                .originalBytes(originalBytes)
                .originalFileName(originalFileName)
                .thumbnailBytes(thumbnailBytes)
                .thumbnailFileName(thumbnailFileName)
                .contentType(resolvedContentType)
                .build();
    }

    // ─────────────────────────────── Compression ──────────────────────────────

    private byte[] compress(byte[] bytes, String extension, Integer width, Integer height,
                            Double scale, int rotationDegrees) throws IOException {

        // WebP: Thumbnailator cannot encode WebP, pass through as-is.
        // Note: rotation is NOT applied for WebP — pre-existing limitation.
        if (extension.equalsIgnoreCase("webp")) {
            return bytes;
        }

        String format = extension.equalsIgnoreCase("png") ? "png" : "jpeg";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var builder = Thumbnails.of(new ByteArrayInputStream(bytes));

        if (width != null && height != null) {
            builder.size(width, height).keepAspectRatio(true);
        } else {
            // PNG gets more aggressive dimension scaling since it can't use lossy quality
            double effectiveScale = extension.equalsIgnoreCase("png") ? 0.50 : scale;
            builder.scale(effectiveScale);
        }

        // Always disabled — we apply rotation ourselves, deterministically,
        // based on metadata-extractor's read above. Thumbnailator's own
        // built-in EXIF detection is known to be unreliable on some real-world
        // JPEGs, so we don't depend on it at all, for any format.
        builder.useExifOrientation(false);

        try {
            builder.outputFormat(format)
                    .outputQuality(OUTPUT_QUALITY)
                    .toOutputStream(out);
        } catch (IOException e) {
            // Thumbnailator throws UnsupportedFormatException ("No suitable ImageReader found for
            // source data.") when ImageIO cannot decode these bytes. That is bad input, not a
            // server fault — surface it as a 400 instead of falling through to a raw 500.
            log.warn("Failed to decode {} image ({} bytes): {}", format, bytes.length, e.getMessage());
            throw new ValidationException(
                    "The image could not be decoded — it may be corrupt or use an unsupported "
                            + format.toUpperCase() + " encoding.");
        }

        return out.toByteArray();
    }

    // ─────────────────────────────── Orientation ───────────────────────────────
    private int extractOrientationDegrees(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                return switch (orientation) {
                    case 6 -> 90;   // confirmed via exiftool: heif-convert output uses this value
                    case 3 -> 180;
                    case 8 -> 270;
                    default -> 0;  // 1 = normal; mirrored variants (2,4,5,7) not handled
                };
            }
        } catch (Exception e) {
            log.warn("Could not read EXIF orientation, defaulting to no rotation: {}", e.getMessage());
        }
        return 0;
    }


    // ─────────────────────────────── HEIC/HEIF conversion ──────────────────────
    private byte[] convertHeicToJpeg(byte[] heicBytes) throws IOException {
        Path tempHeic = Files.createTempFile("heic_", ".heic");
        Path tempJpeg = Files.createTempFile("jpeg_", ".jpg");
        try {
            Files.write(tempHeic, heicBytes);

            Process process = new ProcessBuilder(
                    "heif-convert", "-q", "90",
                    tempHeic.toString(), tempJpeg.toString()
            ).redirectErrorStream(true).start();

            boolean finished = process.waitFor(HEIF_CONVERT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String processOutput = new String(process.getInputStream().readAllBytes());

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("heif-convert timed out after " + HEIF_CONVERT_TIMEOUT_SECONDS + "s");
            }
            if (process.exitValue() != 0) {
                throw new IOException("heif-convert failed (exit " + process.exitValue() + "): " + processOutput);
            }

            byte[] jpegBytes = Files.readAllBytes(tempJpeg);

            if (jpegBytes.length < 100 || jpegBytes[0] != (byte) 0xFF || jpegBytes[1] != (byte) 0xD8) {
                throw new IOException("heif-convert produced an invalid JPEG (size=" + jpegBytes.length + " bytes)");
            }

            return jpegBytes;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HEIF conversion interrupted", e);
        } finally {
            Files.deleteIfExists(tempHeic);
            Files.deleteIfExists(tempJpeg);
        }
    }

    // ─────────────────────────────── Helpers ───────────────────────────────────

    private String changeExtensionToJpg(String filename) {
        if (filename == null) return "image.jpg";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) + ".jpg" : filename + ".jpg";
    }

    /**
     * Identifies the real image format from magic bytes, independent of the client's Content-Type
     * header or filename extension. Returns a canonical mime type, or {@code null} if the bytes are
     * not a supported image (JPEG, PNG, WEBP, HEIC/HEIF).
     */
    private String detectImageType(byte[] b) {
        if (b == null || b.length < 12) return null;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        // PNG: 89 50 4E 47 (89 'P' 'N' 'G')
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        // WEBP: "RIFF" .... "WEBP"
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        // HEIF/HEIC: ISO-BMFF "ftyp" box at bytes 4..7, brand at 8..11
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String brand = new String(b, 8, 4, StandardCharsets.US_ASCII).toLowerCase();
            if (HEIF_BRANDS.contains(brand)) return "image/heif";
        }
        return null;
    }

    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}