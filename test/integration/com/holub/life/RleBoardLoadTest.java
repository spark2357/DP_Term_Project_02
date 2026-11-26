package com.holub.life;

import com.holub.life.support.*;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * RLE 형식으로 Board에 Load하는 통합 테스트: RleFormat + Board
 * (RleFormatTest의 "Board와 함께" 시나리오를 통합 테스트로 옮김)
 *
 * [테스트 시나리오]
 *   1. 조합: Load한 Gosper glider gun이 30세대 뒤 glider를 쏨
 *   2. 트랜잭션: 잘못된 RLE를 Load하면 IOException이 나고 기존 보드는 그대로 남음
 *
 * [커버리지]
 *   - RleBoardLoadTest.java 테스트 대상: Board.load(in, RleFormat), Board.step()
 *   - 테스트 제외 대상
 *     - RLE 문법 자체(read, write): 단위 테스트 RleFormatTest에서 확인
 */
@Tag("extension")
class RleBoardLoadTest {
	private static final String GUN_FILE = "test/resources/gosper-glider-gun.rle";

	private final RleFormat rle = new RleFormat(LifeTestDriver.SIZE);

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
		Set<Point> gun;
		try (InputStream in = new FileInputStream(GUN_FILE)) {
			gun = rle.read(in).positions();
		}
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
}
