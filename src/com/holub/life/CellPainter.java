package com.holub.life;

import java.awt.Rectangle;

/***
 * Cells describe <em>what</em> to draw (where they are and what state
 * they are in), and a CellPainter decides <em>how</em> it looks (colors
 * and shapes). This is the Strategy in a Strategy pattern: the model
 * only knows this interface, never a particular way of drawing.
 */

public interface CellPainter
{
	/** Draw a single cell.
	 *  @param here  the bounds of the cell.
	 *  @param alive true if the cell is alive.
	 */
	void paintCell( Rectangle here, boolean alive );

	// [수정] 2026-11-22: 셀 색 기능. 셀은 이 메서드로 자기 상태(살았나와 색)를 넘김.
	// 기본 구현은 색을 무시하고 위의 paintCell을 부르므로, 색을 모르는 Painter는 고치지 않아도 됨
	default void paintCell( Rectangle here, CellState state ){
		paintCell( here, state.isAlive() );
	}

	/** Draw the border of a block of cells.
	 *  @param here   the bounds of the block.
	 *  @param active true if the block changed in the last transition.
	 */
	void paintBorder( Rectangle here, boolean active );
}
