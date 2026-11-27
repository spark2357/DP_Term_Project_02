package com.holub.life.format;

import java.util.List;

/**
 * 파일 창에 보여줄 필터 하나: 설명과 확장자 목록(점 없이, 예: "rle").
 * 화면 라이브러리와 상관없는 값이라 모델(PatternFormats)이 만들고, 실제 파일 창 필터로 바꾸는 일은 화면 쪽(SwingDialogs)이 함
 */
public record ExtensionFilter(String description, List<String> extensions) {
    public ExtensionFilter {
        extensions = List.copyOf(extensions);
        if (extensions.isEmpty()) {
            throw new IllegalArgumentException("확장자가 하나 이상 있어야 함");
        }
    }
}
