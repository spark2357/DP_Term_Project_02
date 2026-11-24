package com.holub.life;

import java.io.*;
import java.awt.*;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	private BirthColorRule birthColorRule = new ClashRule();

	public Board()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident( () -> birthColorRule )
							)
						);
	}

	public void setBirthColorRule( BirthColorRule rule ){
		birthColorRule = Objects.requireNonNull( rule );
		replaceWith( liveCellColors() );
	}

	public BirthColorRule birthColorRule(){
		return birthColorRule;
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
  
	public void brush( int row, int column, CellColor color ){
		int width = widthInCells();
		outermostCell.userBrushed( new Point(column, row),
								   new Rectangle(0, 0, width, width),
								   Objects.requireNonNull( color ) );
	}

  // [수정] 2026-11-23: 4096칸마다 NeighborhoodState(LinkedList)에 isAlive()로 확인하던 것을, transfer(STORE)로 살아 있는 셀을
	// HashSet에 바로 모으도록 바꿈. Undo가 세대마다 스냅샷을 생성하므로 속도 개선을 위한 변경 (보드가 가득 차면 12.7ms → 0.05ms)
	// CellSetMemento 가 생겨서 변경 가능해짐
	/** Return the positions of all live cells on the board. Each
	 *  position is a Point whose x is the column and whose y is the
	 *  row (in cells, not pixels), the same coordinates used by
	 *  {@link #toggle}.
	 *
	 *  The returned set is a new copy. Changing it does not change
	 *  the board, and no internal Cell is exposed. The live cells are
	 *  collected with transfer(STORE), the same traversal that the
	 *  original Store used, so no cell changes state.
	 */
	public Set<Point> liveCells()
	{	return new HashSet<Point>( liveCellColors().keySet() );
	}

	// [수정] 2026-11-24: 셀 색 브랜치와 Undo 브랜치 병합. CellSetMemento가 색까지 담는 Map으로 바뀌어서
	// liveCells()도 이 메서드의 transfer(STORE) 결과를 쓰도록 하고, 칸마다 isAlive()로 확인하던 반복문은 없앰
	private Map<Point, CellColor> liveCellColors()
	{	Map<Point, CellColor> live = new HashMap<>();
		outermostCell.transfer( new CellSetMemento(live), new Point(0,0), Cell.STORE );
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
	{	replaceWith( format.readColored( in ) );
	}

	// [수정] 2026-11-17: Grid → Stamp. 패턴의 왼쪽 위를 (row, column)에 맞추고, 패턴 사각형 안의 칸을 덮어씀.
	// 보드 밖으로 나가는 부분은 잘라냄

	/** 패턴 사각형 안은 패턴대로(죽은 칸 포함) 덮어쓰고, 밖의 셀은 그대로 둠.
	 */
	public void stamp( CellPattern pattern, int row, int column, CellColor color ){
		Map<Point, CellColor> cells = liveCellColors();
		Set<Point> alive  = pattern.cells();
		int		   width  = widthInCells();

		for( int y = 0; y < pattern.height(); ++y )
			for( int x = 0; x < pattern.width(); ++x )
			{	Point here = new Point( column + x, row + y );
				if( here.x < 0 || here.x >= width || here.y < 0 || here.y >= width )
					continue;
				if( alive.contains( new Point(x, y) ) )
					cells.put( here, Objects.requireNonNull( color ) );
				else
					cells.remove( here );
			}
		replaceWith( cells );
	}

	// 원래 Load와 같은 transfer(LOAD) 경로로 셀을 놓음. 블록 가장자리의 옆 블록도 깨움
	private void replaceWith( Map<Point, CellColor> cells )
	{	outermostCell.clear();
		outermostCell.transfer( new CellSetMemento(cells), new Point(0,0), Cell.LOAD );
	}

	// [수정] 2026-11-23: Undo/Redo용 snapshot(), restore() 추가 (Memento 패턴의 Originator)

	/** 현재 보드의 살아 있는 셀을 담은 스냅샷 생성
	 */
	public BoardSnapshot snapshot()
	{	return new BoardSnapshot( liveCellColors() );	// [수정] 2026-11-24: Undo 후에도 셀 색이 남도록 색까지 저장
	}

	/** 스냅샷 상태의 보드로 복구. Load와 같은 transfer(LOAD) 경로이므로
	 * 복구 후 세대 계산도 정상적으로 진행
	 */
	public void restore( BoardSnapshot snapshot )
	{	replaceWith( snapshot.cells() );
	}

	/** 지금 패턴을 주어진 형식으로 out에 씀.
	 *  스트림은 닫지 않음 (연 쪽이 닫음)
	 */
	public void store( OutputStream out, PatternFormat format ) throws IOException
	{	format.writeColored( liveCellColors(), out );
	}

	/** 살아 있는 셀 집합을 transfer(LOAD)에 넘기거나 transfer(STORE)로 모으기만 하는 Memento.
	 *  스트림 읽기/쓰기는 PatternFormat이 맡으므로 쓰지 않음
	 */
	private static class CellSetMemento implements Cell.Memento
	{	private final Map<Point, CellColor> cells;

		CellSetMemento( Map<Point, CellColor> cells )	{ this.cells = cells;				}
		public boolean isAlive( Point p )	{ return cells.containsKey( p );	}
		public CellColor colorAt( Point p )	{ return cells.get( p );			}

		public void markAsAlive( Point p, CellColor color )	{ cells.put( new Point(p), color );	}
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
	public void redraw( CellPainter painter, Rectangle here, boolean drawAll )
	{	outermostCell.redraw( painter, here, drawAll );
	}
}
