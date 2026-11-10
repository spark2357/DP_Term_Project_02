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

	/** Draw the border of a block of cells.
	 *  @param here   the bounds of the block.
	 *  @param active true if the block changed in the last transition.
	 */
	void paintBorder( Rectangle here, boolean active );
}
