package com.ync.lms.service;

import com.ync.lms.exception.BusinessRuleException;
import com.ync.lms.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 업로드 파일 저장/읽기/삭제 (과제 첨부, 제출물 첨부 공통).
 * 파일은 폴더에, DB 에는 이름만 저장한다.
 */
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir); // 폴더 없으면 생성
        } catch (IOException e) {
            throw new IllegalStateException("업로드 폴더를 만들 수 없습니다: " + this.uploadDir, e);
        }
    }

    /** 저장 결과: 원래 이름 + 서버에 저장한 이름 */
    public record StoredFile(String originalName, String storedName) {
    }

    /**
     * 파일 저장. 파일을 선택하지 않았으면 null.
     */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 원래 이름에서 경로 부분 제거 (예: "C:\fake\..\a.pdf" → "a.pdf")
        String originalName = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));

        // 저장 이름 = UUID + 원래 확장자  (겹치지 않고, 경로 조작 불가)
        String extension = StringUtils.getFilenameExtension(originalName);
        String storedName = UUID.randomUUID() + (extension != null ? "." + extension.toLowerCase() : "");

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleException("파일 저장에 실패했습니다. 다시 시도하세요.");
        }
        return new StoredFile(originalName, storedName);
    }

    /**
     * 다운로드용 파일 읽기
     */
    public Resource load(String storedName) {
        Path path = resolveSafely(storedName);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new NotFoundException("파일을 찾을 수 없습니다.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("파일을 찾을 수 없습니다.");
        }
    }

    /**
     * 파일 삭제 (과제 파일 교체/과제 삭제 시). 없으면 무시.
     */
    public void delete(String storedName) {
        if (storedName == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafely(storedName));
        } catch (IOException ignored) {
            // 파일 정리 실패는 기능에 영향 없음
        }
    }

    // 업로드 폴더 밖의 경로는 거부 (../ 공격 방지)
    private Path resolveSafely(String storedName) {
        Path path = uploadDir.resolve(storedName).normalize();
        if (!path.startsWith(uploadDir)) {
            throw new NotFoundException("파일을 찾을 수 없습니다.");
        }
        return path;
    }
}
