package com.holub.life.support;

import com.holub.life.PatternFormat;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 파일 내용 없이 동작하는 가짜 PatternFormat (테스트 더블)
 * Board, LifeGame이 형식에 무엇을 넘기고 받는지만 확인할 때 씀. 진짜 형식의 규칙(RLE 가운데 놓기 등)에 기대지 않음
 *
 *   - read(in)          : 만들 때 정해 둔 셀을 돌려줌. null로 만들었으면 IOException (입력 스트림은 읽지 않음)
 *   - write(cells, out) : 받은 셀을 written에 기억함 (출력 스트림에는 쓰지 않음)
 */
public class FakeFormat implements PatternFormat {
	private final Set<Point> toRead;
	public Set<Point> written;

	public FakeFormat(Set<Point> toRead) {
		this.toRead = toRead;
	}

	@Override
	public Set<Point> read(InputStream in) throws IOException {
		if (toRead == null) {
			throw new IOException("읽기 실패");
		}
		return new HashSet<>(toRead);
	}

	@Override
	public void write(Set<Point> cells, OutputStream out) {
		written = new HashSet<>(cells);
	}

	@Override
	public String extension() {
		return "fake";
	}

	@Override
	public String description() {
		return "Fake Files";
	}
}
