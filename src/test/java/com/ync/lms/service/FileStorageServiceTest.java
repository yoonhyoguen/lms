package com.ync.lms.service;

import com.ync.lms.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir; // 테스트마다 새로 만들어지고 끝나면 지워지는 임시 폴더

    FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(tempDir.toString());
    }

    @Test
    void 파일을_UUID_이름으로_저장하고_원래_이름을_기억한다() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "1주차 과제.PDF", "application/pdf", "hello".getBytes(StandardCharsets.UTF_8));

        FileStorageService.StoredFile stored = storage.store(file);

        assertThat(stored.originalName()).isEqualTo("1주차 과제.PDF");
        assertThat(stored.storedName()).matches("[0-9a-f-]{36}\\.pdf"); // UUID + 소문자 확장자
        assertThat(Files.readString(tempDir.resolve(stored.storedName()))).isEqualTo("hello");
    }

    @Test
    void 같은_이름의_파일을_두번_올려도_덮어쓰지_않는다() {
        MockMultipartFile a = new MockMultipartFile("file", "과제.txt", "text/plain", "A".getBytes());
        MockMultipartFile b = new MockMultipartFile("file", "과제.txt", "text/plain", "B".getBytes());

        String nameA = storage.store(a).storedName();
        String nameB = storage.store(b).storedName();

        assertThat(nameA).isNotEqualTo(nameB);
    }

    @Test
    void 파일을_선택하지_않으면_null() {
        MockMultipartFile empty = new MockMultipartFile("file", "", "application/octet-stream", new byte[0]);

        assertThat(storage.store(empty)).isNull();
        assertThat(storage.store(null)).isNull();
    }

    @Test
    void 원래_이름에_경로가_섞여_있으면_파일명만_남긴다() {
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/evil.txt", "text/plain", "x".getBytes());

        FileStorageService.StoredFile stored = storage.store(file);

        assertThat(stored.originalName()).isEqualTo("evil.txt");
        assertThat(Files.exists(tempDir.resolve(stored.storedName()))).isTrue(); // 업로드 폴더 안에 저장됨
    }

    @Test
    void 저장한_파일을_다시_읽을_수_있다() throws Exception {
        String storedName = storage.store(
                new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes())).storedName();

        Resource resource = storage.load(storedName);

        assertThat(resource.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("data");
    }

    @Test
    void 업로드_폴더_밖의_파일은_읽을_수_없다() {
        assertThatThrownBy(() -> storage.load("../build.gradle"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 삭제하면_파일이_사라진다() {
        String storedName = storage.store(
                new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes())).storedName();

        storage.delete(storedName);

        assertThat(Files.exists(tempDir.resolve(storedName))).isFalse();
    }
}
