package com.holub.life.format;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.holub.life.model.CellColor;
import com.holub.life.model.LiveCells;

/**
 * LifeFormatAdapter 단위 테스트: 원본 .life 저장 형식을 PatternFormat으로 감싼 Adapter
 *
 *   메서드
 *   - read(in)          : 원본 메멘토 형식을 읽어 산 셀 좌표 집합을 돌려줌. 잘못된 입력은 IOException
 *                         색도 읽음. 색 표가 없는 예전 파일은 기본색(빨강)
 *   - write(cells, out) : 산 셀 좌표를 원본 메멘토 형식으로 씀. 색도 Memento에 기록해 씀
 *   - extension()       : "life"
 *
 * [테스트 시나리오]
 *   1. 왕복: 쓴 것을 다시 읽으면 같은 셀이 나옴 (보드 꼭짓점 포함)
 *   2. 빈 보드: 빈 집합을 쓰고 읽어도 빈 집합
 *   3. 잘못된 입력: .life가 아닌 내용은 IOException
 *   4. 확장자: "life"
 *   5. 색 왕복: 여러 색의 셀을 쓰고 다시 읽으면 위치와 색이 모두 같음
 *   6. 예전 파일: 색 표가 없는 원본 형식 파일은 모든 셀을 기본색으로 읽음
 *
 * [커버리지]
 *   - LifeFormatAdapterTest.java 테스트 대상: read, write, extension, read(색), write(색)
 *   - 테스트 제외 대상
 *     - 원본 프로그램이 저장한 파일 읽기: StoreLoadContractTest(loadsFileStoredByOriginal)에서 확인
 *     - 잘리거나 잘못된 색 표: NeighborhoodStateTest에서 확인 (Adapter는 Memento에 그대로 맡김)
 */
class LifeFormatAdapterTest {
	private final LifeFormatAdapter format = new LifeFormatAdapter(LifeTestDriver.SIZE);

	@Test
	@Tag("original")
	@DisplayName("write(), read(): 쓴 셀을 다시 읽으면 같은 셀이 나온다")
	void roundTripKeepsCells() throws IOException {
		// Given: 보드 꼭짓점과 블록 경계의 셀
		Set<Point> cells = Cells.fromRowColumn("0,0", "0,63", "63,0", "63,63", "7,8", "30,31");

		// When, Then
		assertEquals(cells, read(write(cells)));
	}

	@Test
	@Tag("original")
	@DisplayName("write(), read(): 빈 집합을 쓰고 읽어도 빈 집합이다")
	void emptyRoundTrip() throws IOException {
		assertEquals(Set.of(), read(write(Set.of())));
	}

	@Test
	@Tag("original")
	@DisplayName("read(): .life 형식이 아닌 내용을 읽으면 IOException이 난다")
	void invalidInputThrowsIOException() {
		// Given
		byte[] text = "this is not a life file".getBytes(StandardCharsets.UTF_8);

		// When, Then
		assertThrows(IOException.class, () -> format.read(new ByteArrayInputStream(text)));
	}

	@Test
	@Tag("original")
	@DisplayName("extension(): 확장자는 life다")
	void extensionIsLife() {
		assertEquals("life", format.extension());
	}

	@Test
	@Tag("extension")
	@DisplayName("write(), read(): 여러 색의 셀을 쓰고 다시 읽으면 위치와 색이 모두 같다")
	void roundTripKeepsColors() throws IOException{
		// Given: 보드 꼭짓점과 블록 경계에 서로 다른 색
		Map<Point, CellColor> colored = new HashMap<>();
		colored.put(new Point(0, 0), new CellColor(0, 0, 255));
		colored.put(new Point(63, 0), new CellColor(0, 128, 0));
		colored.put(new Point(0, 63), new CellColor(255, 255, 0));
		colored.put(new Point(63, 63), CellColor.DEFAULT);
		colored.put(new Point(8, 7), new CellColor(1, 2, 3));
		LiveCells cells = new LiveCells(colored);

		// When
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		format.write(cells, out);
		LiveCells read = format.read(new ByteArrayInputStream(out.toByteArray()));

		// Then
		assertEquals(cells, read);
	}

	@Test
	@Tag("extension")
	@DisplayName("read(): 색 표가 없는 예전 파일은 모든 셀을 기본색(빨강)으로 읽는다")
	void oldFileReadsAsDefault() throws IOException{
		// Given
		Point[] positions = { new Point(0, 0), new Point(10, 20), new Point(63, 63) };
		byte[] oldFile = LifeFileBytes.oldFormat(positions);

		// When
		LiveCells read = format.read(new ByteArrayInputStream(oldFile));

		// Then
		Map<Point, CellColor> expected = new HashMap<>();
		for (Point p : positions) {
			expected.put(p, CellColor.DEFAULT);
		}
		assertEquals(new LiveCells(expected), read);
	}

	// ---- 헬퍼 메서드

	private byte[] write(Set<Point> cells) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		format.write(LiveCells.of(cells), out);
		return out.toByteArray();
	}

	private Set<Point> read(byte[] bytes) throws IOException {
		return format.read(new ByteArrayInputStream(bytes)).positions();
	}
}
