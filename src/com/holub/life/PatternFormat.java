package com.holub.life;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public interface PatternFormat {
    Set<Point> read(InputStream in) throws IOException;
    void write(Set<Point> cells, OutputStream out) throws IOException;

    // 파일 확장자 (점 없이, 예: "rle"). 파일 이름으로 형식을 고를 때와 파일 창 필터에 씀
    String extension();

    // 파일 창 필터에 보일 이름 (예: "RLE Pattern Files")
    String description();
}
