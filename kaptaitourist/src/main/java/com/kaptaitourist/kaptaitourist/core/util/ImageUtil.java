package com.kaptaitourist.kaptaitourist.core.util;

import com.kaptaitourist.kaptaitourist.core.exception.ValidationException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class ImageUtil {
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "image/heic", "image/heif"
    );
    private static final List<String> HEIC_TYPES = List.of("image/heic", "image/heif");

    // target: output is ~25% of original file size
    // achieved by scaling dimensions to 70% + quality 0.6
    private static final double DIMENSION_SCALE = 0.70;
    private static final double OUTPUT_QUALITY  = 0.60;
    private static final long   MAX_BYTES       = 4L * 1024 * 1024; // 4 MB hard limit

    private static final int THUMBNAIL_WIDTH  = 400;
    private static final int THUMBNAIL_HEIGHT = 300;

    public ProcessedImage process(byte[] rawBytes, String contentType, String originalFilename,
                                  String identifier, boolean generateThumbnail) throws IOException {

        // Validate content type first
        validateContentType(contentType);

        // Hard limit: reject anything over 4MB before we even try to process
        if (rawBytes.length > MAX_BYTES)
            throw new ValidationException("Image must not exceed 4MB. Received: "
                    + (rawBytes.length / 1024) + "KB");

        // normalize HEIC/HEIF to JPEG before anything else touches it
        String effectiveContentType = contentType.toLowerCase();
        if (HEIC_TYPES.contains(effectiveContentType)) {
            rawBytes = convertHeicToJpeg(rawBytes);
            effectiveContentType = "image/jpeg";
        }

        String safeFilename  = originalFilename != null ? sanitize(originalFilename) : "image.jpg";
        String extension     = getExtension(safeFilename);
//        String baseName      = String.valueOf(UUID.randomUUID());
        String baseName      = originalFilename;


        // Compress original (70% dimensions + 0.6 quality → ~25% of raw size)
        byte[] originalBytes    = compress(rawBytes, extension, null, null, DIMENSION_SCALE);
        String originalFileName = baseName + "." + extension;

        // Thumbnail variant — fixed 400×300 crop (only if requested)
        byte[] thumbnailBytes    = null;
        String thumbnailFileName = null;
        if (generateThumbnail) {
            thumbnailBytes    = compress(rawBytes, extension, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, null);
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

    private byte[] compress(byte[] bytes, String extension, Integer width, Integer height, Double scale)
            throws IOException {

        // WebP: Thumbnailator cannot encode WebP, pass through as-is
        // PNG: cannot do lossy quality reduction, so lower dimensions more aggressively (50% instead of 70%)
        if (extension.equalsIgnoreCase("webp")) {
            return bytes;
        }

        String format = extension.equalsIgnoreCase("png") ? "png" : "jpeg";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var builder = Thumbnails.of(new ByteArrayInputStream(bytes));

        if (width != null && height != null) {
//            builder.size(width, height).crop(Positions.CENTER);
            builder.size(width, height).keepAspectRatio(true);
        } else {
            // PNG gets more aggressive dimension scaling since it can't use lossy quality
            double effectiveScale = extension.equalsIgnoreCase("png") ? 0.50 : scale;
            builder.scale(effectiveScale);
        }

        builder.outputFormat(format)
                .outputQuality(OUTPUT_QUALITY)
                .toOutputStream(out);

        return out.toByteArray();
    }

    // convertHeicToJpeg
    private byte[] convertHeicToJpeg(byte[] heicBytes) throws IOException {
        try (var bais = new java.io.ByteArrayInputStream(heicBytes);
             var iis = javax.imageio.ImageIO.createImageInputStream(bais)) {

            var readers = javax.imageio.ImageIO.getImageReadersByFormatName("HEIF");
            if (!readers.hasNext()) {
                throw new IOException("No HEIF reader found — check TwelveMonkeys HEIF dependency is on the classpath.");
            }

            var reader = readers.next();
            reader.setInput(iis);
            var image = reader.read(0);

            var out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "jpeg", out);
            return out.toByteArray();
        }
    }

    private void validateContentType(String contentType) {
        String ct = contentType != null ? contentType.toLowerCase() : "";
        if (!ALLOWED_TYPES.contains(ct))
            throw new ValidationException(
                    "Only JPEG, PNG, WEBP, and HEIC images are allowed. Received: " + ct);
    }

    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
