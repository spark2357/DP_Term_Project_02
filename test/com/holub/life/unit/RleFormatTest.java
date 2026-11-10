package com.holub.life.unit;

import com.holub.life.*;
import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RleFormat 단위 테스트: RLE 패턴 형식 (PatternFormat 구현)
 *
 *   형식
 *   - # 으로 시작하는 줄은 주석
 *   - 헤더 x = 너비, y = 높이[, rule = 규칙]
 *   - 본문 b: 죽은 칸, o: 산 칸, $: 줄 끝, !: 패턴 끝, 앞의 숫자: 반복 횟수
 *   메서드
 *   - read(in)          : 패턴을 읽어 보드 가운데에 놓은 좌표 집합을 돌려줌. 잘못된 입력은 IOException
 *   - write(cells, out) : 셀들을 감싸는 사각형 기준으로 RLE를 씀. 한 줄 70자 이하
 *
 * [테스트 시나리오]
 *   1. 위치: 패턴은 보드 가운데에 놓이고, 폭이 홀수면 남는 한 칸은 오른쪽(아래쪽) 여백으로 감
 *   2. 본문 해석: 반복 횟수, 빈 줄(n$), 여러 줄에 나뉜 본문, 주석, CRLF, rule과 '!' 생략
 *   3. 규칙: B3/S23의 다른 표기도 읽음
 *   4. 잘못된 입력: 헤더 없음, 보드보다 큰 패턴, 다른 규칙, 모르는 글자, x를 넘는 줄은 IOException
 *   5. 쓰기: 정해진 문자열, 빈 줄 합치기, 빈 보드
 *   6. 왕복: Gosper glider gun을 쓰고 다시 읽어도 같음, 본문은 한 줄 70자 이하
 *   7. Board와 함께: Load한 gun이 glider를 쏨, 잘못된 RLE를 Load해도 보드가 남음
 *
 * [커버리지]
 *   - RleFormatTest.java 테스트 대상: read, write, Board.load(in, format)과의 연결
 *   - 테스트 제외 대상
 *     - 메뉴에서 .rle 파일 고르기
 *     - 형식 오류(보드보다 높음, y를 넘는 줄 수, 숫자와 기호 사이 공백, 반복 횟수 0, 빈 파일):
 *       남긴 경우와 대칭이거나 드물어서 생략. 코드에서는 모두 IOException
 */
class RleFormatTest {
	private static final int SIZE = LifeTestDriver.SIZE;
	private static final String GLIDER_FILE = "test/resources/glider.rle";
	private static final String GUN_FILE = "test/resources/gosper-glider-gun.rle";

	private final RleFormat rle = new RleFormat(SIZE);

	//  1. 위치

	@Test
	@DisplayName("read(): 폭이 짝수인 4x4 패턴은 정확히 가운데(30~33행, 30~33열)에 놓인다")
	void evenPatternIsCentered() throws IOException {
		// Given, When
		Set<Point> cells = read("x = 4, y = 4\n4o$4o$4o$4o!");

		// Then: 보드 중심선(31과 32 사이)의 양쪽으로 두 칸씩
		Set<Point> expected = new HashSet<>();
		for (int row = 30; row <= 33; row++) {
			for (int column = 30; column <= 33; column++) {
				expected.add(new Point(column, row));
			}
		}
		assertEquals(expected, cells);
	}

	@Test
	@DisplayName("read(): 폭이 홀수인 glider(glider.rle)는 30행, 30열부터 놓인다 (남는 한 칸은 오른쪽, 아래쪽 여백)")
	void oddPatternLeavesExtraSpaceOnRightAndBottom() throws IOException {
		// Given, When
		Set<Point> cells = readFile(GLIDER_FILE);

		// Then: 3칸 → (64 - 3) / 2 = 30부터
		assertEquals(Cells.fromRowColumn("30,31", "31,32", "32,30", "32,31", "32,32"), cells);
	}

	//  2. 본문 해석

	@Test
	@DisplayName("read(): 반복 횟수와 빈 줄(n$)을 해석한다")
	void readsRunCountsAndBlankRows() throws IOException {
		// Given: 0행에 산 칸 2개, 빈 줄 하나, 2행에 죽은 칸 1개 뒤 산 칸 1개
		String text = "x = 2, y = 3\n2o2$bo!";

		// When
		Set<Point> cells = read(text);

		// Then: 2x3 패턴이라 (31, 30)부터 놓임
		assertEquals(Cells.fromRowColumn("30,31", "30,32", "32,32"), cells);
	}

	@Test
	@DisplayName("read(): 주석, 여러 줄에 나뉜 본문, 기호 사이 공백, CRLF, rule과 '!' 생략을 모두 받아들인다")
	void acceptsOptionalSyntax() throws IOException {
		// Given: glider와 같은 패턴을 너그러운 형태로 씀 (rule 없음, '!' 없음)
		String text = "#N Glider\r\n#C 주석의 한글도 괜찮음\r\nx=3,y=3\r\nbo$ 2b\r\no$3o\r\n";

		// When, Then
		assertEquals(read(GLIDER_FILE_TEXT), read(text));
	}

	//  3. 규칙

	@ParameterizedTest(name = "rule = {0}")
	@ValueSource(strings = { "b3/s23", "S23/B3", "23/3" })
	@DisplayName("read(): B3/S23의 다른 표기도 받아들인다")
	void acceptsConwayRuleNotations(String rule) throws IOException {
		assertEquals(3, read("x = 3, y = 1, rule = " + rule + "\n3o!").size());
	}

	//  4. 잘못된 입력

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"헤더 없음                 | bo$2bo$3o!",
			"보드보다 큼               | x = 65, y = 1\\no!",
			"지원하지 않는 규칙        | x = 3, y = 1, rule = B36/S23\\n3o!",
			"모르는 글자               | x = 3, y = 1\\n3x!",
			"줄이 x보다 김             | x = 2, y = 1\\n3o!",
	})
	@DisplayName("read(): 잘못된 입력은 IOException으로 실패한다")
	void invalidInputThrowsIOException(String name, String text) {
		String withNewlines = text.replace("\\n", "\n");
		assertThrows(IOException.class, () -> read(withNewlines), name);
	}

	//  5. 쓰기

	@Test
	@DisplayName("write(): glider를 표준 RLE 문자열로 쓴다 (보드 위치와 상관없이 패턴 기준)")
	void writesGlider() throws IOException {
		// Given: 보드 아무 곳에 놓인 glider
		Set<Point> glider = Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22");

		// When, Then
		assertEquals("x = 3, y = 3, rule = B3/S23\nbo$2bo$3o!\n", write(glider));
	}

	@Test
	@DisplayName("write(): 줄 끝의 죽은 칸은 생략하고, 빈 줄은 n$ 하나로 합친다")
	void writesBlankRowsAsOneToken() throws IOException {
		// Given: 0행 0열과 3행 2열. 사이의 1, 2행은 빈 줄
		Set<Point> cells = Cells.fromRowColumn("0,0", "3,2");

		// When, Then: 0행은 o 뒤의 죽은 칸 2개를 생략
		assertEquals("x = 3, y = 4, rule = B3/S23\no3$2bo!\n", write(cells));
	}

	@Test
	@DisplayName("write(), read(): 빈 보드는 x = 0, y = 0으로 쓰고, 다시 읽으면 오류 없이 빈 집합이 된다")
	void emptyBoardRoundTrip() throws IOException {
		// When
		String text = write(new HashSet<>());

		// Then
		assertEquals("x = 0, y = 0, rule = B3/S23\n!\n", text);
		assertEquals(Set.of(), read(text));
	}

	//  6. 왕복

	@Test
	@DisplayName("read(), write(): Gosper glider gun(36x9, 셀 36개)을 쓰고 다시 읽어도 같고, 본문은 한 줄 70자 이하로 나뉜다")
	void gunRoundTrip() throws IOException {
		// Given
		Set<Point> gun = readFile(GUN_FILE);
		assertEquals(36, gun.size());

		// When: gun의 본문은 100자 정도라 70자에서 줄이 바뀜
		String text = write(gun);

		// Then: 모든 줄이 70자 이하
		for (String line : text.split("\n")) {
			assertTrue(line.length() <= 70, "70자를 넘는 줄: " + line);
		}

		// Then: 다시 읽으면 같음. 둘 다 가운데에 놓으므로 위치까지 같아야 함
		assertEquals(gun, read(text));
	}

	//  7. Board와 함께

	@Test
	@DisplayName("Board.load(in, RleFormat): Load한 Gosper glider gun은 30세대 뒤 glider 하나를 쏜다")
	void loadedGunEmitsGlider() throws IOException {
		// Given
		Board board = new Board();
		try (InputStream in = new FileInputStream(GUN_FILE)) {
			board.load(in, rle);
		}

		// When: gun의 주기는 30세대
		for (int generation = 0; generation < 30; generation++) {
			board.step();
		}

		// Then: gun은 처음 모양(36개)으로 돌아오고, 새로 나온 glider 5개가 더해짐
		Set<Point> gun = readFile(GUN_FILE);
		assertTrue(board.liveCells().containsAll(gun), "gun이 처음 모양으로 돌아와야 함");
		assertEquals(36 + 5, board.liveCells().size());
	}

	@Test
	@DisplayName("Board.load(in, RleFormat): 잘못된 RLE를 Load하면 IOException이 나고 기존 보드는 그대로 남는다")
	void invalidRleKeepsBoard() {
		// Given: 셀이 있는 보드와 보드보다 큰 패턴
		Board board = new Board();
		board.toggle(10, 10);
		board.toggle(10, 11);
		Set<Point> before = board.liveCells();
		byte[] tooLarge = "x = 100, y = 1\no!".getBytes(StandardCharsets.UTF_8);

		// When, Then
		assertThrows(IOException.class, () -> board.load(new ByteArrayInputStream(tooLarge), rle));
		assertEquals(before, board.liveCells());
	}

	//  헬퍼

	private static final String GLIDER_FILE_TEXT = "x = 3, y = 3, rule = B3/S23\nbo$2bo$3o!\n";

	private Set<Point> read(String text) throws IOException {
		return rle.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private Set<Point> readFile(String path) throws IOException {
		try (InputStream in = new FileInputStream(path)) {
			return rle.read(in);
		}
	}

	private String write(Set<Point> cells) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		rle.write(cells, out);
		return out.toString(StandardCharsets.UTF_8);
	}
}
