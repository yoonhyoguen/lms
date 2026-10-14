package com.ync.lms.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    @Test
    void 평범한_값은_그대로() {
        assertThat(CsvWriter.escape("홍길동")).isEqualTo("홍길동");
        assertThat(CsvWriter.escape("45")).isEqualTo("45");
    }

    @Test
    void 빈값과_null은_빈칸() {
        assertThat(CsvWriter.escape(null)).isEmpty();
        assertThat(CsvWriter.escape("")).isEmpty();
    }

    @Test
    void 쉼표가_있으면_따옴표로_감싼다() {
        assertThat(CsvWriter.escape("잘했음, 다만")).isEqualTo("\"잘했음, 다만\"");
    }

    @Test
    void 따옴표는_두번_쓰고_감싼다() {
        assertThat(CsvWriter.escape("\"최고\"")).isEqualTo("\"\"\"최고\"\"\"");
    }

    @Test
    void 줄바꿈이_있으면_따옴표로_감싼다() {
        assertThat(CsvWriter.escape("1줄\n2줄")).isEqualTo("\"1줄\n2줄\"");
    }

    @Test
    void 수식으로_시작하면_작은따옴표를_붙여_실행을_막는다() {
        assertThat(CsvWriter.escape("=1+1")).isEqualTo("'=1+1");
        assertThat(CsvWriter.escape("+82")).isEqualTo("'+82");
        assertThat(CsvWriter.escape("-5")).isEqualTo("'-5");
        assertThat(CsvWriter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        // 수식 + 쉼표 → 둘 다 처리
        assertThat(CsvWriter.escape("=HYPERLINK(\"x\",\"y\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"");
    }

    @Test
    void 파일_맨앞에_BOM이_있고_줄은_CRLF로_끝난다() {
        byte[] bytes = CsvWriter.toBytes(List.of("학번", "이름"), List.of(List.of("2026001", "홍길동")));
        String text = new String(bytes, StandardCharsets.UTF_8);

        assertThat(bytes[0]).isEqualTo((byte) 0xEF); // UTF-8 BOM = EF BB BF
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
        assertThat(text).isEqualTo("﻿학번,이름\r\n2026001,홍길동\r\n");
    }
}
