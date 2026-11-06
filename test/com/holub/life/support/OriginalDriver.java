package com.holub.life.support;

import com.holub.life.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * 원본 코드 LifeTestDriver
 * 게임 진행 순서가 Universe(JPanel) 안에 있어 화면(JPanel) 없이는 부를 수 없음 -> 우리가 수정할 부분
 * Universe와 같은 보드를 만들고 Universe의 코드와 같은 순서로 Neighborhood를 호출함
 *
 *   tick()   : Universe의 clock 리스너와 같음. figureNextState(DUMMY x 8)가 true면 transition()
 *   click()  : Universe의 마우스 클릭과 같음. 셀 가운데 픽셀로 userClicked()
 *   clear()  : Grid → Clear와 같음. clear()
 *   store()  : Universe.doStore()와 같음. createMemento() → transfer(STORE) → flush()
 *   load()   : Universe.doLoad()와 같음. clear() → createMemento() → load() → transfer(LOAD)
 *   liveCells() : createMemento()로 보드를 읽음 (보드를 읽기만 하는 메서드가 없어서)
 */
public class OriginalDriver implements LifeTestDriver {
	private static final int CELL_PIXELS = 8;
	private static final Rectangle SCREEN = new Rectangle(0, 0, SIZE * CELL_PIXELS, SIZE * CELL_PIXELS);

	// Universe와 같은 구조: 8x8 블록 안에 8x8 셀
	private final Neighborhood board = new Neighborhood(8, new Neighborhood(8, new Resident()));

	public void tick() {
		boolean willChange = board.figureNextState(
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
		if (willChange) {
			board.transition();
		}
	}

	public void click(int row, int column) {
		Point center = new Point(column * CELL_PIXELS + CELL_PIXELS / 2, row * CELL_PIXELS + CELL_PIXELS / 2);
		board.userClicked(center, SCREEN);
	}

	public void clear() {
		board.clear();
	}

	public void store(OutputStream out) throws IOException {
		Storable memento = board.createMemento();
		board.transfer(memento, new Point(0, 0), Cell.STORE);
		memento.flush(out);
	}

	public void load(InputStream in) throws IOException {
		board.clear();
		Storable memento = board.createMemento();
		memento.load(in);
		board.transfer(memento, new Point(0, 0), Cell.LOAD);
	}

	public Set<Point> liveCells() {
		Cell.Memento memento = (Cell.Memento) board.createMemento();
		Set<Point> live = new HashSet<>();
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				Point cell = new Point(column, row);
				if (memento.isAlive(cell)) {
					live.add(cell);
				}
			}
		}
		return live;
	}
}
