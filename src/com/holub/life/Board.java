package com.holub.life;

import java.io.*;
import java.awt.*;
import java.util.Set;
import java.util.HashSet;

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

public class Board implements BoardView
{
	// [수정] 2026-11-09: 그리기용 읽기 창구(BoardView)를 구현. Universe는 이 인터페이스로만 Board를 봄

	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int DEFAULT_GRID_SIZE = 8;

	private final Cell outermostCell;

	/** load(InputStream), store(OutputStream)이 쓰는 기존 .life 형식.
	 *  원래 Load/Store가 그대로 동작하게 함
	 */
	private final PatternFormat lifeFormat;

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
		lifeFormat = new LifeFormatAdapter( outermostCell.widthInCells() );
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

	/** Return the positions of all live cells on the board. Each
	 *  position is a Point whose x is the column and whose y is the
	 *  row (in cells, not pixels), the same coordinates used by
	 *  {@link #toggle}.
	 *
	 *  The returned set is a new copy. Changing it does not change
	 *  the board, and no internal Cell is exposed. The live cells are
	 *  collected through the same memento that {@link #store} uses,
	 *  so no cell changes state.
	 */
	public Set<Point> liveCells()
	{	Cell.Memento memento = (Cell.Memento) outermostCell.createMemento();
		Set<Point>	 live	 = new HashSet<Point>();
		int			 width	 = widthInCells();

		for( int row = 0; row < width; ++row )
			for( int column = 0; column < width; ++column )
			{	Point here = new Point( column, row );
				if( memento.isAlive( here ) )
					live.add( here );
			}
		return live;
	}

	/** Kill every cell on the board.
	 */
	public void clear()
	{	outermostCell.clear();
	}

	/** <code>in</code>에서 주어진 형식으로 패턴을 읽어 보드를 바꿈.
	 *  다 읽은 뒤에 보드를 지우므로, 읽다 실패하면 보드는 그대로 남음.
	 *  스트림은 닫지 않음 (연 쪽이 닫음)
	 */
	public void load( InputStream in, PatternFormat format ) throws IOException
	{	Set<Point> cells = format.read( in );
		outermostCell.clear();

		// 원래 Load와 같은 transfer(LOAD) 경로로 셀을 놓음. 블록 가장자리의 옆 블록도 깨움
		outermostCell.transfer( new CellSetMemento(cells), new Point(0,0), Cell.LOAD );
	}

	/** 지금 패턴을 주어진 형식으로 <code>out</code>에 씀.
	 *  스트림은 닫지 않음 (연 쪽이 닫음)
	 */
	public void store( OutputStream out, PatternFormat format ) throws IOException
	{	format.write( liveCells(), out );
	}

	/** 기존 .life 형식으로 Load
	 *  @see #load(InputStream, PatternFormat)
	 */
	public void load( InputStream in ) throws IOException
	{	load( in, lifeFormat );
	}

	/** 기존 .life 형식으로 Store
	 *  @see #store(OutputStream, PatternFormat)
	 */
	public void store( OutputStream out ) throws IOException
	{	store( out, lifeFormat );
	}

	/** 살아 있는 셀 집합을 transfer(LOAD)에 전달하기만 하는 Memento.
	 *  스트림 읽기/쓰기는 PatternFormat이 맡으므로 쓰지 않음
	 */
	private static class CellSetMemento implements Cell.Memento
	{	private final Set<Point> cells;

		CellSetMemento( Set<Point> cells )	{ this.cells = cells;				}
		public boolean isAlive( Point p )	{ return cells.contains( p );		}
		public void markAsAlive( Point p )	{ cells.add( new Point(p) );		}

		public void load( InputStream in )
		{	throw new UnsupportedOperationException();
		}
		public void flush( OutputStream out )
		{	throw new UnsupportedOperationException();
		}
	}

	/** Draw the board into the rectangle <code>here</code>. If
	 *  <code>drawAll</code> is false, only the blocks that changed
	 *  in the last transition are redrawn.
	 */
	public void redraw( Graphics g, Rectangle here, boolean drawAll )
	{	outermostCell.redraw( g, here, drawAll );
	}
}
