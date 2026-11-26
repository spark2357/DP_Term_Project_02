package com.holub.life;

import java.awt.Rectangle;

/**
 * A read-only view of the Board: just what is needed to draw it.
 * The Universe (the Swing view) holds the Board through this
 * interface, so it can draw the board but cannot change it.
 * Every change (stepping, toggling a cell, clearing, loading) must
 * go through the LifeGame facade, and the compiler enforces that.
 *
 * @include /etc/license.txt
 */

public interface BoardView
{
	/** Width (and height) of the whole board, in cells.
	 */
	int widthInCells();

	/** Draw the board into the rectangle <code>here</code>. If
	 *  <code>drawAll</code> is false, only the blocks that changed
	 *  in the last transition are redrawn.
	 */
	void redraw( CellPainter painter, Rectangle here, boolean drawAll );
}
