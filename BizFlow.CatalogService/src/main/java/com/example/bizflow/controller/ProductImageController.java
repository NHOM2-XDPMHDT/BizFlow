package com.example.bizflow.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/product-images")
public class ProductImageController {

    private static final Path ASSETS_ROOT = Paths.get(
            Optional.ofNullable(System.getenv("BIZFLOW_ASSETS_DIR")).orElse("FE/assets"));
    private static final Path IMAGE_DIR = ASSETS_ROOT.resolve(Paths.get("img", "img_sanpham"));
    private static final Path IMAGE_INDEX = ASSETS_ROOT.resolve(Paths.get("data", "product-image-files.json"));
    private static final String WEB_PREFIX = "/assets/img/img_sanpham/";

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> uploadProductImage(@RequestParam("file") MultipartFile file,
            @RequestParam("productName") String productName) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image file is required"));
        }
        String originalName = file.getOriginalFilename();
        String sanitizedName = sanitizeFilename(originalName);
        String baseName = normalizeFileBase(productName);
        if (baseName.isEmpty()) {
            baseName = "product";
        }
        String ext = getExtension(originalName);
        if (ext.isEmpty()) {
            ext = "jpg";
        }

        try {
            Files.createDirectories(IMAGE_DIR);
            Files.createDirectories(IMAGE_INDEX.getParent());
            String filename = sanitizedName;
            if (filename.isEmpty()) {
                filename = baseName + "." + ext;
            } else if (!filename.contains(".")) {
                filename = filename + "." + ext;
            }
            Path target = IMAGE_DIR.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String webPath = WEB_PREFIX + target.getFileName().toString();
            updateImageIndex(webPath);
            String aliasFilename = baseName + "." + ext;
            if (!aliasFilename.equalsIgnoreCase(target.getFileName().toString())) {
                Path aliasTarget = IMAGE_DIR.resolve(aliasFilename);
                Files.copy(target, aliasTarget, StandardCopyOption.REPLACE_EXISTING);
                String aliasPath = WEB_PREFIX + aliasTarget.getFileName().toString();
                updateImageIndex(aliasPath);
            }
            return ResponseEntity.ok(Map.of("path", webPath));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save image"));
        }
    }

    private void updateImageIndex(String webPath) throws IOException {
        List<String> entries = readIndexEntriesSafe();
        boolean exists = entries.stream().anyMatch(path -> path.equalsIgnoreCase(webPath));
        if (!exists) {
            entries.add(webPath);
            writeIndexEntries(entries);
        }
    }

    private List<String> readIndexEntriesSafe() {
        if (!Files.exists(IMAGE_INDEX)) {
            return new ArrayList<>();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(IMAGE_INDEX.toFile(), new TypeReference<List<String>>() {});
        } catch (IOException ex) {
            // If the index is missing, empty, or corrupted, recover gracefully.
            return new ArrayList<>();
        }
    }

    private void writeIndexEntries(List<String> entries) throws IOException {
        Files.createDirectories(IMAGE_INDEX.getParent());
        ObjectMapper mapper = new ObjectMapper();
        Path tmp = Files.createTempFile(IMAGE_INDEX.getParent(), "product-image-files", ".json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), entries);
        try {
            Files.move(tmp, IMAGE_INDEX, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, IMAGE_INDEX, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private String normalizeFileBase(String name) {
        if (name == null) {
            return "";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String cleaned = normalized.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return cleaned;
    }

    private String sanitizeFilename(String name) {
        if (name == null) {
            return "";
        }
        String justName = Paths.get(name).getFileName().toString().trim();
        if (justName.isEmpty()) {
            return "";
        }
        return justName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
