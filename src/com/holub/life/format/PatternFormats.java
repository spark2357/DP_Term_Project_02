package com.holub.life.format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.holub.life.model.PatternFormat;

/**
 * 지원하는 파일 형식 목록과 확장자 처리(형식 고르기, 파일 창 필터, 메시지용 목록)를 한 곳에 모음.
 * 어떤 형식을 지원할지는 Life.main이 정함
 */
public class PatternFormats {
    private final List<PatternFormat> formats;

    // 이 순서대로 파일 창에 나옴
    public PatternFormats(List<PatternFormat> formats) {
        this.formats = List.copyOf(formats);
    }

    // 파일 이름의 확장자(대소문자 무시)에 맞는 형식
    public Optional<PatternFormat> formatFor(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        for (PatternFormat format : formats) {
            if (name.endsWith("." + format.extension())) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    // Load: 모든 형식을 함께 보여주는 필터가 맨 앞, 그 뒤로 형식마다 하나씩
    public List<ExtensionFilter> loadFilters() {
        List<String> extensions = new ArrayList<>();
        List<String> patterns = new ArrayList<>();
        for (PatternFormat format : formats) {
            extensions.add(format.extension());
            patterns.add("*." + format.extension());
        }
        List<ExtensionFilter> filters = new ArrayList<>();
        filters.add(new ExtensionFilter(
                "Pattern Files (" + String.join(", ", patterns) + ")", extensions));
        filters.addAll(storeFilters());
        return List.copyOf(filters);
    }

    // Store: 형식마다 하나씩
    public List<ExtensionFilter> storeFilters() {
        List<ExtensionFilter> filters = new ArrayList<>();
        for (PatternFormat format : formats) {
            filters.add(filterFor(format));
        }
        return List.copyOf(filters);
    }

    // 형식 하나만 고르는 필터 (예: Stamp는 .rle만)
    public static ExtensionFilter filterFor(PatternFormat format) {
        return new ExtensionFilter(
                format.description() + " (*." + format.extension() + ")", List.of(format.extension()));
    }

    // 메시지용: ".life, .rle"
    public String extensionList() {
        List<String> extensions = new ArrayList<>();
        for (PatternFormat format : formats) {
            extensions.add("." + format.extension());
        }
        return String.join(", ", extensions);
    }
}
