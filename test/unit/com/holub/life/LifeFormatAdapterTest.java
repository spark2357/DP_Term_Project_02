package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * LifeFormatAdapter 단위 테스트: 원본 .life 저장 형식을 PatternFormat으로 감싼 Adapter
 *
 *   메서드
 *   - read(in)          : 원본 메멘토 형식을 읽어 산 셀 좌표 집합을 돌려줌. 잘못된 입력은 IOException
 *   - write(cells, out) : 산 셀 좌표를 원본 메멘토 형식으로 씀
 *   - extension()       : "life"
 *
 * [테스트 시나리오]
 *   1. 왕복: 쓴 것을 다시 읽으면 같은 셀이 나옴 (보드 꼭짓점 포함)
 *   2. 빈 보드: 빈 집합을 쓰고 읽어도 빈 집합
 *   3. 잘못된 입력: .life가 아닌 내용은 IOException
 *   4. 확장자: "life"
 *
 * [커버리지]
 *   - LifeFormatAdapterTest.java 테스트 대상: read, write, extension
 *   - 테스트 제외 대상
 *     - 원본 프로그램이 저장한 파일 읽기: StoreLoadContractTest(loadsFileStoredByOriginal)에서 확인
 */
@Tag("original")
class LifeFormatAdapterTest {
	private final LifeFormatAdapter format = new LifeFormatAdapter(LifeTestDriver.SIZE);

	@Test
	@DisplayName("write(), read(): 쓴 셀을 다시 읽으면 같은 셀이 나온다")
	void roundTripKeepsCells() throws IOException {
		// Given: 보드 꼭짓점과 블록 경계의 셀
		Set<Point> cells = Cells.fromRowColumn("0,0", "0,63", "63,0", "63,63", "7,8", "30,31");

		// When, Then
		assertEquals(cells, read(write(cells)));
	}

	@Test
	@DisplayName("write(), read(): 빈 집합을 쓰고 읽어도 빈 집합이다")
	void emptyRoundTrip() throws IOException {
		assertEquals(Set.of(), read(write(Set.of())));
	}

	@Test
	@DisplayName("read(): .life 형식이 아닌 내용을 읽으면 IOException이 난다")
	void invalidInputThrowsIOException() {
		// Given
		byte[] text = "this is not a life file".getBytes(StandardCharsets.UTF_8);

		// When, Then
		assertThrows(IOException.class, () -> format.read(new ByteArrayInputStream(text)));
	}

	@Test
	@DisplayName("extension(): 확장자는 life다")
	void extensionIsLife() {
		assertEquals("life", format.extension());
	}

	// ---- 헬퍼 메서드

	private byte[] write(Set<Point> cells) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		format.write(cells, out);
		return out.toByteArray();
	}

	private Set<Point> read(byte[] bytes) throws IOException {
		return format.read(new ByteArrayInputStream(bytes));
	}
}
