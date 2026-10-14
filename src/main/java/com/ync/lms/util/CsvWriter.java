package com.ync.lms.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 엑셀에서 바로 열 수 있는 CSV 만들기.
 * - UTF-8 BOM: 엑셀이 한글을 깨지지 않게 읽도록
 * - 쉼표·따옴표·줄바꿈이 있는 값은 "..." 로 감싸고 " 는 "" 로
 * - =, +, -, @ 로 시작하는 값은 앞에 ' 를 붙여 수식 실행 방지 (CSV 인젝션)
 */
public final class CsvWriter {

    private static final String BOM = "﻿";

    private CsvWriter() {
    }

    public static byte[] toBytes(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder(BOM);
        appendLine(sb, header);
        for (List<String> row : rows) {
            appendLine(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendLine(StringBuilder sb, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values.get(i)));
        }
        sb.append("\r\n"); // 엑셀 표준 줄바꿈
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = value;

        // CSV 인젝션 방지: 수식으로 해석될 수 있는 첫 글자
        char first = v.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            v = "'" + v;
        }

        // 쉼표, 따옴표, 줄바꿈이 있으면 따옴표로 감싸기
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
