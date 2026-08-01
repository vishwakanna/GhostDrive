package com.ghostdrive.server.controller;

import com.ghostdrive.server.dto.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${ghostdrive.root-directory}")
    private String rootDirectory;

    @GetMapping("/files")
    public List<FileInfo> listFiles(@RequestParam(defaultValue = "") String path) {
        File targetDir = new File(rootDirectory, path);
        List<FileInfo> result = new ArrayList<>();

        File[] children = targetDir.listFiles();
        if (children == null) return result;

        for (File f : children) {
            String relativePath = path.isEmpty() ? f.getName() : path + "/" + f.getName();
            result.add(new FileInfo(f.getName(), relativePath, f.isDirectory(), f.length()));
        }
        return result;
    }

    @GetMapping("/stream")
    public ResponseEntity<ResourceRegion> streamFile(
            @RequestParam String path,
            @RequestHeader HttpHeaders headers) throws IOException {

        File file = new File(rootDirectory, path);
        if (!file.exists()) return ResponseEntity.notFound().build();

        UrlResource video = new UrlResource(file.toURI());
        long contentLength = video.contentLength();
        ResourceRegion region;

        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            long rangeLength = Math.min(1024 * 1024, contentLength);
            region = new ResourceRegion(video, 0, rangeLength);
        } else {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024, end - start + 1);
            region = new ResourceRegion(video, start, rangeLength);
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) throws IOException {
        File file = new File(rootDirectory, path);
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new UrlResource(file.toURI());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}