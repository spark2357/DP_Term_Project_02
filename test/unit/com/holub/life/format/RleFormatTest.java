package com.holub.life.format;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.holub.life.model.CellColor;
import com.holub.life.model.CellPattern;
import com.holub.life.model.LiveCells;

/**
 * RleFormat 단위 테스트: RLE 패턴 형식 (PatternFormat 구현)
 *
 *   형식
 *   - # 으로 시작하는 줄은 주석. #R x y는 패턴 왼쪽 위의 보드 좌표(열, 행)
 *   - 헤더 x = 너비, y = 높이[, rule = 규칙]
 *   - 본문 b: 죽은 칸, o: 산 칸, $: 줄 끝, !: 패턴 끝, 앞의 숫자: 반복 횟수
 *   메서드
 *   - read(in)          : 패턴을 읽어 #R 위치(없으면 보드 가운데)에 놓은 좌표 집합을 돌려줌. 잘못된 입력은 IOException
 *   - write(cells, out) : 셀들을 감싸는 사각형 기준으로 RLE를 쓰고, 사각형의 왼쪽 위를 #R로 씀. 한 줄 70자 이하
 *   - readPattern(in)   : #R은 버리고 패턴 모양(크기와 패턴 안의 좌표)만 돌려줌 (Stamp에서 씀)
 *
 * [테스트 시나리오]
 *   1. 위치: #R이 없으면 패턴은 보드 가운데에 놓이고, 폭이 홀수면 남는 한 칸은 오른쪽(아래쪽) 여백으로 감
 *   2. 본문 해석: 반복 횟수, 빈 줄(n$), 여러 줄에 나뉜 본문, 주석, CRLF, rule과 '!' 생략
 *   3. 규칙: B3/S23의 다른 표기도 읽음
 *   4. 잘못된 입력: 헤더 없음, 보드보다 큰 패턴, 다른 규칙, 모르는 글자, x를 넘는 줄, 잘못된 #R은 IOException
 *   5. 쓰기: 정해진 문자열(#R 포함), 빈 줄 합치기, 빈 보드
 *   6. 왕복: Gosper glider gun을 쓰고 다시 읽어도 같음, 본문은 한 줄 70자 이하. 구석의 패턴도 같은 자리로 돌아옴
 *   7. 색: RLE는 색을 저장하지 않음. 쓸 때 색을 버리고, 읽은 셀은 모두 기본색(빨강)
 *   8. 패턴 모양만 읽기: readPattern()은 크기와 모양을 그대로 돌려주고, #R은 무시함(보드 위치 검사를 하지 않음)
 *
 * [커버리지]
 *   - RleFormatTest.java 테스트 대상: read, write, readPattern
 *   - 테스트 제외 대상
 *     - Board.load(in, RleFormat)와의 연결, 실제 파일(test/resources) 읽기: 통합 테스트 RleBoardLoadTest에서 확인
 *       (이 테스트는 샘플 파일 내용을 문자열로 옮겨 써서 파일을 읽지 않음)
 *     - 메뉴에서 .rle 파일 고르기
 *     - 형식 오류(보드보다 높음, y를 넘는 줄 수, 숫자와 기호 사이 공백, 반복 횟수 0, 빈 파일):
 *       남긴 경우와 대칭이거나 드물어서 생략. 코드에서는 모두 IOException
 */
@Tag("extension")
class RleFormatTest {
	private static final int SIZE = LifeTestDriver.SIZE;

	// test/resources/glider.rle과 같은 내용
	private static final String GLIDER_TEXT =
			"#N Glider\n"
			+ "#C The smallest spaceship. Moves one cell diagonally every 4 generations.\n"
			+ "x = 3, y = 3, rule = B3/S23\n"
			+ "bo$2bo$3o!\n";

	// test/resources/gosper-glider-gun.rle과 같은 내용
	private static final String GUN_TEXT =
			"#N Gosper glider gun\n"
			+ "#O Bill Gosper\n"
			+ "#C The first known gun. Emits a new glider every 30 generations.\n"
			+ "x = 36, y = 9, rule = B3/S23\n"
			+ "24bo$22bobo$12b2o6b2o12b2o$11bo3bo4b2o12b2o$2o8bo5bo3b2o$2o8bo3bob2o4b\n"
			+ "obo$10bo5bo7bo$11bo3bo$12b2o!\n";

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
	@DisplayName("read(): 폭이 홀수인 glider는 30행, 30열부터 놓인다 (남는 한 칸은 오른쪽, 아래쪽 여백)")
	void oddPatternLeavesExtraSpaceOnRightAndBottom() throws IOException {
		// Given, When
		Set<Point> cells = read(GLIDER_TEXT);

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
		assertEquals(read(GLIDER_TEXT), read(text));
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
			"#R 위치에서 보드를 벗어남 | #R 62 0\\nx = 3, y = 1\\n3o!",
			"#R 음수 좌표              | #R -1 0\\nx = 1, y = 1\\no!",
			"#R 좌표가 하나뿐          | #R 5\\nx = 1, y = 1\\no!",
	})
	@DisplayName("read(): 잘못된 입력은 IOException으로 실패한다")
	void invalidInputThrowsIOException(String name, String text) {
		String withNewlines = text.replace("\\n", "\n");
		assertThrows(IOException.class, () -> read(withNewlines), name);
	}

	//  5. 쓰기

	@Test
	@DisplayName("write(): glider를 #R(왼쪽 위 20열, 10행)과 표준 RLE 문자열로 쓴다")
	void writesGlider() throws IOException {
		// Given: 보드 아무 곳에 놓인 glider
		Set<Point> glider = Cells.fromRowColumn("10,21", "11,22", "12,20", "12,21", "12,22");

		// When, Then
		assertEquals("#R 20 10\nx = 3, y = 3, rule = B3/S23\nbo$2bo$3o!\n", write(glider));
	}

	@Test
	@DisplayName("write(): 줄 끝의 죽은 칸은 생략하고, 빈 줄은 n$ 하나로 합친다")
	void writesBlankRowsAsOneToken() throws IOException {
		// Given: 0행 0열과 3행 2열. 사이의 1, 2행은 빈 줄
		Set<Point> cells = Cells.fromRowColumn("0,0", "3,2");

		// When, Then: 0행은 o 뒤의 죽은 칸 2개를 생략
		assertEquals("#R 0 0\nx = 3, y = 4, rule = B3/S23\no3$2bo!\n", write(cells));
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
		Set<Point> gun = read(GUN_TEXT);
		assertEquals(36, gun.size());

		// When: gun의 본문은 100자 정도라 70자에서 줄이 바뀜
		String text = write(gun);

		// Then: 모든 줄이 70자 이하
		for (String line : text.split("\n")) {
			assertTrue(line.length() <= 70, "70자를 넘는 줄: " + line);
		}

		// Then: 다시 읽으면 같음
		assertEquals(gun, read(text));
	}

	@Test
	@DisplayName("write(), read(): 오른쪽 아래 구석의 glider를 쓰고 다시 읽으면 가운데가 아니라 같은 자리에 놓인다")
	void positionSurvivesRoundTrip() throws IOException {
		// Given: 61~63행, 61~63열
		Set<Point> glider = Cells.fromRowColumn("61,62", "62,63", "63,61", "63,62", "63,63");

		// When, Then
		assertEquals(glider, read(write(glider)));
	}

	//  색: RLE는 색을 저장하지 않음

	@Test
	@DisplayName("write(): 색은 버리고 위치만 쓴다 (색이 달라도 기본색으로 쓴 것과 같은 내용)")
	void writeDropsColors() throws IOException{
		// Given: 같은 위치의 파란 glider와 기본색 glider
		Set<Point> glider = Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2");
		Map<Point, CellColor> blue = new HashMap<>();
		for (Point cell : glider) {
			blue.put(cell, new CellColor(0, 0, 255));
		}

		// When
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		rle.write(new LiveCells(blue), out);

		// Then
		assertEquals(write(glider), out.toString(StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("read(): 읽은 셀은 모두 기본색(빨강)이다")
	void readGivesDefaultColor() throws IOException{
		// When
		LiveCells cells = rle.read(new ByteArrayInputStream(GLIDER_TEXT.getBytes(StandardCharsets.UTF_8)));

		// Then
		assertEquals(5, cells.size());
		for (Point cell : cells.positions()) {
			assertEquals(CellColor.DEFAULT, cells.colorAt(cell), cell.toString());
		}
	}

	//  8. 패턴 모양만 읽기

	@Test
	@DisplayName("readPattern(): glider의 크기(3x3)와 패턴 안의 좌표를 보드에 놓지 않고 그대로 돌려준다")
	void readPatternKeepsSizeAndShape() throws IOException{
		// When
		CellPattern pattern = readPattern(GLIDER_TEXT);

		// Then: 왼쪽 위가 (0, 0). read()처럼 보드 가운데로 옮기지 않음
		assertEquals(new CellPattern(3, 3, Cells.fromRowColumn("0,1", "1,2", "2,0", "2,1", "2,2")), pattern);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "#R 20 10", "#R 62 0", "#R -1 0" })
	@DisplayName("readPattern(): #R은 무시하고, read()라면 보드를 벗어나는 위치여도 IOException 없이 같은 모양을 돌려준다")
	void readPatternIgnoresPosition(String position) throws IOException{
		// Given: #R 62 0과 #R -1 0은 read()에서는 보드를 벗어나 IOException
		String body = "x = 3, y = 1\n3o!";

		// When
		CellPattern withPosition = readPattern(position + "\n" + body);

		// Then
		assertEquals(readPattern(body), withPosition);
		assertEquals(new CellPattern(3, 1, Cells.fromRowColumn("0,0", "0,1", "0,2")), withPosition);
	}

	//  헬퍼

	private Set<Point> read(String text) throws IOException {
		return rle.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))).positions();
	}

	private CellPattern readPattern(String text) throws IOException{
		return rle.readPattern(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private String write(Set<Point> cells) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		rle.write(LiveCells.of(cells), out);
		return out.toString(StandardCharsets.UTF_8);
	}
}
