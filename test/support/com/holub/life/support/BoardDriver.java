package com.holub.life.support;

import com.holub.life.model.Board;
import com.holub.life.format.LifeFormatAdapter;
import com.holub.life.model.PatternFormat;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * 현재 코드 LifeTestDriver: 리팩터링한 Board에 그대로 위임
 * OriginalDriver(원본 호출 순서)와 같은 계약 테스트를 돌려, 리팩터링 뒤에도 동작이 같은지 확인함
 *
 *   tick()      : Board.step()
 *   click()     : Board.toggle()
 *   clear()     : Board.clear()
 *   store()     : Board.store(out, .life 형식)
 *   load()      : Board.load(in, .life 형식)
 *   liveCells() : Board.liveCells()
 */
public class BoardDriver implements LifeTestDriver {
	private final Board board = new Board();
	private final PatternFormat format = new LifeFormatAdapter(SIZE);

	public void tick() {
		board.step();
	}

	public void click(int row, int column) {
		board.toggle(row, column);
	}

	public void clear() {
		board.clear();
	}

	public void store(OutputStream out) throws IOException {
		board.store(out, format);
	}

	public void load(InputStream in) throws IOException {
		board.load(in, format);
	}

	public Set<Point> liveCells() {
		return board.liveCells();
	}
}
