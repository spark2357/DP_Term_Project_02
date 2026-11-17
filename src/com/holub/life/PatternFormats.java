package com.holub.life;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.filechooser.FileNameExtensionFilter;

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
    public FileNameExtensionFilter[] loadFilters() {
        String[] extensions = new String[formats.size()];
        List<String> patterns = new ArrayList<>();
        for (int i = 0; i < extensions.length; i++) {
            extensions[i] = formats.get(i).extension();
            patterns.add("*." + extensions[i]);
        }
        List<FileNameExtensionFilter> filters = new ArrayList<>();
        filters.add(new FileNameExtensionFilter(
                "Pattern Files (" + String.join(", ", patterns) + ")", extensions));
        filters.addAll(List.of(storeFilters()));
        return filters.toArray(new FileNameExtensionFilter[0]);
    }

    // Store: 형식마다 하나씩
    public FileNameExtensionFilter[] storeFilters() {
        List<FileNameExtensionFilter> filters = new ArrayList<>();
        for (PatternFormat format : formats) {
            filters.add(filterFor(format));
        }
        return filters.toArray(new FileNameExtensionFilter[0]);
    }

    // 형식 하나만 고르는 필터 (예: Stamp는 .rle만)
    public static FileNameExtensionFilter filterFor(PatternFormat format) {
        return new FileNameExtensionFilter(
                format.description() + " (*." + format.extension() + ")", format.extension());
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
