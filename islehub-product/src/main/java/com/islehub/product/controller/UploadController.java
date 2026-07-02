package com.islehub.product.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.islehub.common.enums.RCode;
import com.islehub.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器，处理图片等文件的上传操作。
 */
@Tag(name = "管理-上传", description = "图片上传")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @SaCheckRole(value = {"admin", "operator"}, mode = SaMode.OR)
    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail(RCode.BAD_REQUEST, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(RCode.BAD_REQUEST, "只允许上传图片文件");
        }

        File baseDir = new File(uploadPath);
        if (!baseDir.isAbsolute()) {
            baseDir = new File(System.getProperty("user.dir"), uploadPath);
        }
        File productDir = new File(baseDir, "products");
        if (!productDir.exists()) {
            productDir.mkdirs();
        }

        String originalName = file.getOriginalFilename();
        String ext = extractExt(originalName);
        if (!ALLOWED_IMAGE_EXTS.contains(ext.toLowerCase())) {
            return Result.fail(RCode.BAD_REQUEST, "不支持的文件类型，仅允许 " + String.join(",", ALLOWED_IMAGE_EXTS));
        }
        String filename = UUID.randomUUID().toString() + "." + ext;

        Path targetPath = Paths.get(productDir.getAbsolutePath(), filename);
        Files.copy(file.getInputStream(), targetPath);

        String url = "/uploads/products/" + filename;
        return Result.ok(url);
    }

    private static String extractExt(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1);
    }
}
