package com.holub.life.unit;

import com.holub.life.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NeighborhoodState 단위 테스트: 보드 저장용 Memento (Neighborhood 안의 private static 클래스, Cell.Memento 구현)
 *
 *   필드
 *   - liveCells : 산 셀 좌표(java.awt.Point)의 Collection (LinkedList)
 *   메서드
 *   - markAsAlive(Point)  : 좌표를 liveCells에 추가
 *   - isAlive(Point)      : liveCells.contains()로 조회
 *   - flush(OutputStream) : liveCells를 ObjectOutputStream으로 직렬화 (.life 파일 내용)
 *   - load(InputStream)   : 역직렬화한 객체가 Point만 든 Collection인지 확인한 뒤 liveCells에 넣음. 아니면 IOException
 *   - toString()          : 디버깅용 문자열
 *
 * [테스트 시나리오]
 *   1. 정상 파일: 기록한 좌표가 flush → load 뒤에도 그대로 남음
 *   2. 직렬화 파일이 아닌 입력(텍스트): IOException
 *   3. Collection이 아닌 객체를 직렬화한 파일: IOException (원본은 ClassCastException)
 *   4. Point가 아닌 원소가 든 Collection: IOException, 기존 내용 유지 (원본은 오류 없이 읽히고 아무 셀도 살지 않음)
 *
 * [커버리지]
 *   - NeighborhoodStateTest.java 테스트 대상: markAsAlive, isAlive, flush, load(정상 입력 1가지, 잘못된 입력 3가지)
 *   - 테스트 제외 대상
 *     - toString(): 디버깅용이라 생략
 *     - InputStream을 받는 생성자: 원본에서 쓰이지 않아 생략
 */
class NeighborhoodStateTest {

	@Test
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
	@DisplayName("load(): 다른 타입의 직렬화 객체는 IOException으로 실패한다 (원본: ClassCastException이 나서 Universe가 오류 창 없이 실패함)")
	void otherSerializedObjectThrowsIOException() throws IOException {
		// Given: Collection이 아닌 객체(String)를 직렬화한 파일
		byte[] wrongFile = serialize("not a collection");
		Cell.Memento memento = emptyMemento();

		// When, Then: Universe.doLoad()는 IOException만 잡으므로 IOException이어야 오류 창이 뜸
		assertThrows(IOException.class,
				() -> memento.load(new ByteArrayInputStream(wrongFile)));
	}

	@Test
	@DisplayName("load(): Point가 아닌 원소가 든 파일은 IOException으로 실패하고, 기존 내용은 바뀌지 않는다 (원본: 오류 없이 읽혀 빈 보드가 됨)")
	void collectionOfNonPointsThrowsIOException() throws IOException {
		// Given: 셀 하나가 기록된 memento와, Point 하나와 String 하나가 섞인 리스트를 직렬화한 파일
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
	private static Cell.Memento emptyMemento() {
		return (Cell.Memento) new Neighborhood(8, new Resident()).createMemento();
	}

	// .life 파일과 같은 방식(ObjectOutputStream)으로 직렬화
	private static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(object);
		out.close();
		return bytes.toByteArray();
	}
}
