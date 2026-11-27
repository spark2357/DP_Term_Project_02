package com.holub.life.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// [수정] 2026-11-24: read/write(Set<Point>)와 readColored/writeColored(Map<Point, CellColor>) 두 벌을 LiveCells 한 벌로 합침.
// 색을 저장하지 않는 형식은 쓸 때 색을 버리고, 읽을 때 LiveCells.of()로 기본색을 줌
public interface PatternFormat {
    LiveCells read(InputStream in) throws IOException;
    void write(LiveCells cells, OutputStream out) throws IOException;

    // 파일 확장자 (점 없이, 예: "rle"). 파일 이름으로 형식을 고를 때와 파일 창 필터에 씀
    String extension();

    // 파일 창 필터에 보일 이름 (예: "RLE Pattern Files")
    String description();
}
