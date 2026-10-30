package com.holub.life;

import java.io.*;
import java.awt.*;

/**
 * The Board is the model of the Game of Life. It creates the
 * Composite Neighborhood and knows the order in which the
 * outermost Cell must be called to advance a generation, toggle
 * a cell, clear the grid, and load or store a pattern.
 *
 * The Board knows nothing about Swing, menus, the Clock, or file
 * dialogs, so it can be used on its own (for example, in tests)
 * simply by creating it with <code>new Board()</code>.
 *
 * @include /etc/license.txt
 */

public class Board
{
	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int DEFAULT_GRID_SIZE = 8;

	private final Cell outermostCell;

	public Board()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident()
							)
						);
	}

	/** Width (and height) of the whole board, in cells.
	 */
	public int widthInCells()
	{	return outermostCell.widthInCells();
	}

	/** Advance the board by one generation. The outermost cell has
	 *  no neighbors, so DUMMY cells are passed in on all eight sides.
	 *
	 *  @return true if anything on the board changed state (so the
	 *  		screen needs to be refreshed).
	 */
	public boolean step()
	{	if( outermostCell.figureNextState
			   ( Cell.DUMMY,Cell.DUMMY,Cell.DUMMY,Cell.DUMMY,
				 Cell.DUMMY,Cell.DUMMY,Cell.DUMMY,Cell.DUMMY
			   )
		  )
		{	return outermostCell.transition();
		}
		return false;
	}

	/** Toggle the cell at the indicated row and column (in cells,
	 *  not pixels). The Neighborhood only accepts pixel coordinates,
	 *  so the request is made against a virtual surface on which
	 *  each cell is exactly one pixel wide.
	 */
	public void toggle( int row, int column )
	{	int width = widthInCells();
		outermostCell.userClicked( new Point(column, row),
								   new Rectangle(0, 0, width, width) );
	}

	/** Kill every cell on the board.
	 */
	public void clear()
	{	outermostCell.clear();
	}

	/** Clear the board, then replace it with the pattern read
	 *  from <code>in</code>. The stream is not closed.
	 */
	public void load( InputStream in ) throws IOException
	{	outermostCell.clear();

		Storable memento = outermostCell.createMemento();
		memento.load( in );
		outermostCell.transfer( memento, new Point(0,0), Cell.LOAD );
	}

	/** Write the current pattern to <code>out</code>.
	 *  The stream is not closed.
	 */
	public void store( OutputStream out ) throws IOException
	{	Storable memento = outermostCell.createMemento();
		outermostCell.transfer( memento, new Point(0,0), Cell.STORE );
		memento.flush( out );
	}

	/** Draw the board into the rectangle <code>here</code>. If
	 *  <code>drawAll</code> is false, only the blocks that changed
	 *  in the last transition are redrawn.
	 */
	public void redraw( Graphics g, Rectangle here, boolean drawAll )
	{	outermostCell.redraw( g, here, drawAll );
	}
}
