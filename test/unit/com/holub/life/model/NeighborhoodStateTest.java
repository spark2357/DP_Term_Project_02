package com.holub.life.model;

import com.holub.life.support.LifeFileBytes;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * NeighborhoodState 단위 테스트: 보드 저장용 Memento (Neighborhood 안의 private static 클래스, Cell.Memento 구현)
 *
 *   필드
 *   - liveCells : 산 셀 좌표(java.awt.Point)의 Collection (LinkedList)
 *   - colors    : 산 셀 좌표마다의 색 (Map<Point, CellColor>). 파일에는 liveCells 다음에 RGB 숫자 표로 씀
 *   메서드
 *   - markAsAlive(Point, 색) : 좌표를 liveCells에, 색을 colors에 추가
 *   - markAsAlive(Point)     : 기본색(빨강)으로 기록 (Cell.Memento의 default)
 *   - isAlive(Point)         : liveCells.contains()로 조회
 *   - colorAt(Point)         : 기록한 색. 기록하지 않은 좌표는 기본색(빨강)
 *   - flush(OutputStream)    : liveCells와 색 표를 ObjectOutputStream으로 직렬화 (.life 파일 내용)
 *   - load(InputStream)      : 역직렬화한 객체가 Point만 든 Collection인지 확인하고, 이어서 색 표를 읽은 뒤 내용을 바꿈.
 *                              잘못된 입력이면 IOException이고 기존 내용은 그대로. 색 표가 없는 예전 파일은 기본색으로 읽음
 *   - toString()             : 디버깅용 문자열
 *
 * [테스트 시나리오]
 *   1. 정상 파일: 기록한 좌표가 flush → load 뒤에도 그대로 남음
 *   2. 직렬화 파일이 아닌 입력(텍스트): IOException
 *   3. Collection이 아닌 객체를 직렬화한 파일: IOException (원본은 ClassCastException)
 *   4. Point가 아닌 원소가 든 Collection: IOException, 기존 내용 유지 (원본은 오류 없이 읽히고 아무 셀도 살지 않음)
 *   5. 색 저장: 기록한 색은 flush → load 뒤에도 그대로 남음 (여러 색)
 *   6. 색 없이 기록: 1인자 markAsAlive는 기본색(빨강)으로 기록
 *   7. 기록하지 않은 좌표의 색: colorAt()은 기본색(빨강)
 *   8. 예전 파일: 색 표가 없으면 산 셀을 모두 기본색(빨강)으로 읽음
 *   9. 끊긴 색 표: 색 표를 읽다가 파일이 끊기면 IOException (예전 파일로 착각해 색을 잃지 않음)
 *  10. 잘못된 색 표: Map<Point, Integer>가 아니면 IOException
 *  11. 색 표 실패 뒤: 기존 내용(좌표와 색)은 바뀌지 않음
 *
 * [커버리지]
 *   - NeighborhoodStateTest.java 테스트 대상: markAsAlive(1인자, 2인자), isAlive, colorAt, flush,
 *     load(정상 입력 1가지, 잘못된 입력 3가지, 예전 파일, 끊긴 색 표, 잘못된 색 표 3가지)
 *   - 테스트 제외 대상
 *     - toString(): 디버깅용이라 생략
 *     - InputStream을 받는 생성자: 원본에서 쓰이지 않아 생략
 */
class NeighborhoodStateTest {

	@Test
	@Tag("original")
	@DisplayName("flush(), load(): 기록한 좌표는 저장했다 읽어도 그대로 남는다")
	void flushAndLoadKeepLiveCells() throws IOException {
		// Given
		Cell.Memento saved = emptyMemento();
		saved.markAsAlive(new Point(1, 2));
		saved.markAsAlive(new Point(63, 63));

		// When: 저장한 바이트를 새 memento로 읽음
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		saved.flush(file);

		Cell.Memento loaded = emptyMemento();
		loaded.load(new ByteArrayInputStream(file.toByteArray()));

		// Then
		assertTrue(loaded.isAlive(new Point(1, 2)));
		assertTrue(loaded.isAlive(new Point(63, 63)));
		assertFalse(loaded.isAlive(new Point(0, 0)));
	}

	@Test
	@Tag("original")
	@DisplayName("load(): 직렬화 파일이 아닌 입력은 IOException으로 실패한다")
	void nonSerializedInputThrowsIOException() {
		// Given
		byte[] textFile = "this is not a life file".getBytes();
		Cell.Memento memento = emptyMemento();

		// When, Then
		assertThrows(IOException.class,
				() -> memento.load(new ByteArrayInputStream(textFile)));
	}

	@Test
	@Tag("improvement")
	@DisplayName("load(): 다른 타입의 직렬화 객체는 IOException으로 실패한다 (결함)")
	void otherSerializedObjectThrowsIOException() throws IOException {
		// Given: Collection이 아닌 객체(String)를 직렬화한 파일
		// 원본은 ClassCastException이 나서 Universe가 오류 창 없이 실패함
		byte[] wrongFile = serialize("not a collection");
		Cell.Memento memento = emptyMemento();

		// When, Then: Universe.doLoad()는 IOException만 잡으므로 IOException이어야 오류 창이 뜸
		assertThrows(IOException.class,
				() -> memento.load(new ByteArrayInputStream(wrongFile)));
	}

	@Test
	@Tag("improvement")
	@DisplayName("load(): Point가 아닌 원소가 든 파일은 IOException으로 실패하고, 기존 내용은 바뀌지 않는다 (결함)")
	void collectionOfNonPointsThrowsIOException() throws IOException {
		// Given: 셀 하나가 기록된 memento와, Point 하나와 String 하나가 섞인 리스트를 직렬화한 파일
		// 원본은 오류 없이 읽혀 빈 보드가 됨
		Cell.Memento memento = emptyMemento();
		memento.markAsAlive(new Point(5, 5));

		ArrayList<Object> mixed = new ArrayList<>();
		mixed.add(new Point(0, 0));
		mixed.add("(1,1)");
		byte[] wrongFile = serialize(mixed);

		// When, Then
		assertThrows(IOException.class,
				() -> memento.load(new ByteArrayInputStream(wrongFile)));

		// Then: 검사에 실패하면 읽은 내용으로 바꾸지 않음
		assertTrue(memento.isAlive(new Point(5, 5)));
		assertFalse(memento.isAlive(new Point(0, 0)));
	}

	// NeighborhoodState는 private 클래스라 Neighborhood.createMemento()로 생성
	// [수정] 2026-11-27: 셀(Resident)과 출생 색 규칙 없이 빈 NeighborhoodState를 만드는 createEmptyMemento()를 씀
	private static Cell.Memento emptyMemento() {
		return Neighborhood.createEmptyMemento();
	}

	// .life 파일과 같은 방식(ObjectOutputStream)으로 직렬화
	private static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(object);
		out.close();
		return bytes.toByteArray();
	}

	//  셀 색 저장

	private static final CellColor BLUE = new CellColor(0, 0, 255);
	private static final Point[] TWO_CELLS = { new Point(1, 2), new Point(63, 63) };

	@ParameterizedTest(name = "({0}, {1}, {2})")
	@CsvSource({
		"0, 0, 255",
		"0, 0, 0",
		"255, 255, 255",
		"1, 2, 3",
		"128, 64, 200"
	})
	@Tag("extension")
	@DisplayName("flush(), load(): 기록한 색은 저장했다 읽어도 그대로 남는다")
	void colorsSurviveRoundTrip(int red, int green, int blue) throws IOException{
		// Given
		CellColor color = new CellColor(red, green, blue);
		Cell.Memento saved = Neighborhood.createEmptyMemento();
		saved.markAsAlive(new Point(4, 5), color);

		// When
		Cell.Memento loaded = Neighborhood.createEmptyMemento();
		loaded.load(new ByteArrayInputStream(flushed(saved)));

		// Then
		assertTrue(loaded.isAlive(new Point(4, 5)));
		assertEquals(color, loaded.colorAt(new Point(4, 5)));
	}

	@Test
	@Tag("extension")
	@DisplayName("markAsAlive(좌표): 색 없이 기록하면 기본색(빨강)으로 기록된다")
	void oneArgMarkUsesDefault(){
		// Given
		Cell.Memento memento = Neighborhood.createEmptyMemento();

		// When
		memento.markAsAlive(new Point(4, 5));

		// Then
		assertTrue(memento.isAlive(new Point(4, 5)));
		assertEquals(CellColor.DEFAULT, memento.colorAt(new Point(4, 5)));
	}

	@Test
	@Tag("extension")
	@DisplayName("colorAt(): 기록하지 않은 좌표는 기본색(빨강)으로 답한다")
	void unrecordedPointIsDefault(){
		// Given
		Cell.Memento memento = Neighborhood.createEmptyMemento();
		memento.markAsAlive(new Point(4, 5), BLUE);

		// When, Then
		assertEquals(CellColor.DEFAULT, memento.colorAt(new Point(6, 7)));
	}

	@Test
	@Tag("extension")
	@DisplayName("load(): 색 표가 없는 예전 파일은 산 셀을 모두 기본색(빨강)으로 읽는다")
	void oldFileLoadsWithDefaultColors() throws IOException{
		// Given: 산 셀 목록만 있는 원본 형식 파일
		byte[] oldFile = LifeFileBytes.oldFormat(TWO_CELLS);
		Cell.Memento memento = Neighborhood.createEmptyMemento();

		// When
		memento.load(new ByteArrayInputStream(oldFile));

		// Then
		for (Point cell : TWO_CELLS) {
			assertTrue(memento.isAlive(cell), cell.toString());
			assertEquals(CellColor.DEFAULT, memento.colorAt(cell), cell.toString());
		}
	}

	@ParameterizedTest(name = "{0}: {1}바이트까지만 남김")
	@MethodSource("cutInsideColorPart")
	@Tag("extension")
	@DisplayName("load(): 색 표를 읽다가 파일이 끊기면 IOException으로 실패한다 (예전 파일로 착각해 색을 잃지 않음)")
	void truncatedColorPartIsRejected(String where, int length) throws IOException{
		// Given: 색 표의 첫 바이트부터 마지막 바이트 앞까지 중 한 곳에서 잘린 파일
		byte[] cut = LifeFileBytes.truncate(twoColoredCells(), length);
		Cell.Memento memento = Neighborhood.createEmptyMemento();

		// When, Then
		assertThrows(IOException.class, () -> memento.load(new ByteArrayInputStream(cut)));
	}

	// 색 표 안에서 자르는 모든 길이: 색 표 시작 + 1 ~ 파일 길이 - 1
	// (색 표 시작에서 정확히 자르면 예전 파일과 같으므로 제외. oldFileLoadsWithDefaultColors에서 확인)
	// [수정] 2026-11-27: 모든 길이(약 190번) 대신 대표 위치 4곳만 자름.
	// 색 표의 마지막 항목은 클래스 정보를 앞에서 이미 썼으므로 항상 키(Point) 14바이트 + 값(Integer) 10바이트이고, 그 뒤에 끝 표시 1바이트가 옴
	private static Stream<Arguments> cutInsideColorPart() throws IOException{
		int start = LifeFileBytes.colorPartStart(TWO_CELLS);
		byte[] file = twoColoredCells();
		assertArrayEquals(LifeFileBytes.oldFormat(TWO_CELLS), LifeFileBytes.truncate(file, start),
				"색 표 앞부분은 예전 파일과 같아야 함");
		int lastEntry = file.length - 1 - 10 - 14;
		assertEquals(0x73, file[lastEntry], "마지막 항목은 새 객체 표시(TC_OBJECT)로 시작해야 함");
		return Stream.of(
				Arguments.of("색 표 첫 바이트 뒤", start + 1),
				Arguments.of("첫 항목과 마지막 항목 사이", lastEntry),
				Arguments.of("마지막 항목의 키 안", lastEntry + 7),
				Arguments.of("끝 표시 바로 앞", file.length - 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("malformedColorParts")
	@Tag("extension")
	@DisplayName("load(): 색 표 자리에 Map<Point, Integer>가 아닌 것이 있으면 IOException으로 실패한다")
	void malformedColorPartIsRejected(String name, Object colorPart) throws IOException{
		// Given
		byte[] file = LifeFileBytes.withColorPart(TWO_CELLS, colorPart);
		Cell.Memento memento = Neighborhood.createEmptyMemento();

		// When, Then
		assertThrows(IOException.class, () -> memento.load(new ByteArrayInputStream(file)));
	}

	private static Stream<Arguments> malformedColorParts(){
		HashMap<Object, Object> stringKey = new HashMap<>();
		stringKey.put("(1,2)", 0x0000FF);
		HashMap<Object, Object> stringValue = new HashMap<>();
		stringValue.put(new Point(1, 2), "blue");
		return Stream.of(
				Arguments.of("Map이 아닌 객체(String)", "colors"),
				Arguments.of("키가 Point가 아님", stringKey),
				Arguments.of("값이 Integer가 아님", stringValue));
	}

	@Test
	@Tag("extension")
	@DisplayName("load(): 색 표가 잘못되어 실패하면 기존 내용(좌표와 색)은 바뀌지 않는다")
	void failedColorLoadKeepsContent() throws IOException{
		// Given: 파랑 셀 하나가 기록된 memento와, 산 셀 목록은 맞지만 색 표가 잘못된 파일
		Cell.Memento memento = Neighborhood.createEmptyMemento();
		memento.markAsAlive(new Point(5, 5), BLUE);
		byte[] file = LifeFileBytes.withColorPart(TWO_CELLS, "colors");

		// When
		assertThrows(IOException.class, () -> memento.load(new ByteArrayInputStream(file)));

		// Then
		assertTrue(memento.isAlive(new Point(5, 5)));
		assertEquals(BLUE, memento.colorAt(new Point(5, 5)));
		assertFalse(memento.isAlive(TWO_CELLS[0]));
	}

	private static byte[] twoColoredCells() throws IOException{
		Cell.Memento memento = Neighborhood.createEmptyMemento();
		memento.markAsAlive(TWO_CELLS[0], BLUE);
		memento.markAsAlive(TWO_CELLS[1], new CellColor(0, 128, 0));
		return flushed(memento);
	}

	private static byte[] flushed(Cell.Memento memento) throws IOException{
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		memento.flush(file);
		return file.toByteArray();
	}
}
