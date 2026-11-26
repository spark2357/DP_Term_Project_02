package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.io.*;

import com.holub.io.Files;
import com.holub.life.Cell;

import com.holub.life.Cell;
import com.holub.life.Clock;
import com.holub.life.Direction;
import com.holub.life.Storable;

/***
 * A group of {@link Cell} objects. Cells are grouped into neighborhoods
 * to make board updates more efficient. When a neighborhood is
 * quiescent (none of the Cells it contains are active), then it
 * ignores any requests to update itself.
 *
 * <h3>History</h3>
 * <p>11-29-04
 * Alexy Marinichev fixed the disapearing-glider problem by clearing
 * the active edges in transistion() rather then figureNextState().
 * The original call is commented out and the new line is marked 
 * with "(1)"
 *
 * @include /etc/license.txt
 */

public final class Neighborhood implements Cell
{
	// [수정] 2026-11-17: transition() 중에 그리기를 막던 잠금(readingPermitted, nestingLevel)을 제거함 (이슈 #5 2단계).
	// Swing Timer로 바꾼 뒤 tick(계산)과 그리기, 클릭이 모두 EDT에서 하나씩 실행되므로 둘이 겹칠 수 없음.
	// 잠금 때문에 그리기를 포기하면 다시 그리기 요청(oneLastRefreshRequired)이 사라지던 문제도 함께 없어짐

	/** Returns true only if none of the cells in the Neighborhood
	 *  changed state during the last transition.
	 */

	private boolean amActive = false;

	/** The actual grid of Cells contained within this neighborhood. */
	private final Cell[][] grid;

	/** The neighborhood is square, so gridSize is both the horizontal
	 *  and vertical size.
	 */
	private final int 	   gridSize;

	/** Create a new Neigborhood containing gridSize-by-gridSize
	 *  clones of the prototype. The Protype is deliberately
	 *  not put into the grid, so you can reuse it if you like.
	 */

	public Neighborhood(int gridSize, Cell prototype)
	{
		this.gridSize = gridSize;
 		this.grid = new Cell[gridSize][gridSize];

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column] = prototype.create();
	}

	/** The "clone" method used to create copies of the current
	 *  neighborhood. This method is called from the containing
	 *  neighborhood's constructor. (The current neighborhood
	 *  is passed into the containing-neighborhood constructor
	 *  as the "prototype" argument.
	 */

	public Cell create()
	{	return new Neighborhood(gridSize, grid[0][0]);
	}

	/** Became stable on the last clock tick. One more refresh is
	 *  required.
	 */

	private boolean oneLastRefreshRequired = false;

	/** Shows the direction of the cells along the edge of the block 
	 *  that will change  state in the next transition. For example,
	 *  if the upper-left corner has changed, then the current
	 *  Cell is disruptive in the NORTH, WEST, and NORTHWEST directions.
	 *  If this is the case, the neigboring
	 *  cells may need to be updated, even if they were previously
	 *  stable.
	 */
	public  Direction isDisruptiveTo(){ return activeEdges; }
	private	Direction activeEdges = new Direction( Direction.NONE );

	/** Figures the next state of the current neighborhood and the
	 *  contained neigborhoods (or cells). Does not transition to the
	 *  next state, however. Note that the neighboring cells are passed
	 *  in as arguments rather than being stored internally---an
	 *  example of the Flyweight pattern.
	 *
	 *  @see #transition
	 *  @param north		The neighbor to our north
	 *  @param south		The neighbor to our south
	 *  @param east			The neighbor to our east
	 *  @param west			The neighbor to our west
	 *  @param northeast	The neighbor to our northeast
	 *  @param northwest	The neighbor to our northwest
	 *  @param southeast	The neighbor to our southeast
	 *  @param southwest	The neighbor to our southwest
	 * 
	 *  @return true if this neighborhood (i.e. any of it's cells)
	 *  			 will change state in the next transition.
	 */

	public boolean figureNextState(	Cell north, 	Cell south,
									Cell east,		Cell west,
									Cell northeast, Cell northwest,
									Cell southeast, Cell southwest )
	{
		boolean	nothingHappened = true;

		// Is some ajacent neigborhood active on the edge
		// that ajoins me?

		if(		amActive
			||	north	 .isDisruptiveTo().the( Direction.SOUTH 	  )
			||	south	 .isDisruptiveTo().the( Direction.NORTH 	  )
			||	east	 .isDisruptiveTo().the( Direction.WEST  	  )
			||	west	 .isDisruptiveTo().the( Direction.EAST  	  )
			||	northeast.isDisruptiveTo().the( Direction.SOUTHWEST )
			||	northwest.isDisruptiveTo().the( Direction.SOUTHEAST )
			||	southeast.isDisruptiveTo().the( Direction.NORTHWEST )
			||	southwest.isDisruptiveTo().the( Direction.NORTHEAST )
		)
		{
			Cell	northCell,		southCell,
					eastCell,		westCell,
					northeastCell, northwestCell,
					southeastCell, southwestCell;

			// activeEdges.clear();
					
			for( int row = 0; row < gridSize; ++row )
			{	for( int column = 0; column < gridSize; ++column )
				{
					// Get the current cell's eight neighbors

					if(row == 0 )		//{=Neighborhood.get.neighbors}
					{	northwestCell = (column==0)
							? northwest.edge(gridSize-1,gridSize-1)
							: north.edge	(gridSize-1,column-1)
							;

						northCell=	north.edge(gridSize-1,column);

						northeastCell = (column == gridSize-1 )
							? northeast.edge (gridSize-1, 0)
							: north.edge	 (gridSize-1, column+1)
							;
					}
					else
					{	northwestCell  = (column == 0)
							? west.edge(row-1, gridSize-1)
							: grid[row-1][column-1]
							;

						northCell = grid[row-1][column];

						northeastCell = (column == gridSize-1)
							? east.edge(row-1, 0)
							: grid[row-1][column+1]
							;
					}

					westCell = (column == 0)
							? west.edge( row, gridSize-1)
							: grid[row][column-1]
							;

					eastCell = (column == gridSize-1)
							? east.edge(row, 0)
							: grid[row][column+1]
							;

					if(row == gridSize-1)
					{	southwestCell = ( column==0 )
							? southwest.edge(0,gridSize-1)
							: south.edge(0,column-1)
							;

						southCell = south.edge(0,column);

						southeastCell = (column == gridSize-1 )
							? southeast.edge(0,0)
							: south.edge(0, column+1)
							;
					}
					else
					{	southwestCell  = (column == 0)
							? west.edge(row+1, gridSize-1)
							: grid[row+1][column-1]
							;

						southCell = grid[row+1][column];

						southeastCell = (column == gridSize-1)
							? east.edge(row+1, 0)
							: grid[row+1][column+1]
							;
					}

					// Tell the cell to change its state. If
					// the cell changed (the figureNextState request
					// returned false), then mark the current block as
					// unstable. Also, if the unstable cell is on the
					// edge of the block modify activeEdges to
					//  indicate which edge or edges changed. 

					if( grid[row][column].figureNextState
						( northCell, 	 southCell,
						  eastCell,	  	 westCell,
						  northeastCell, northwestCell,
						  southeastCell, southwestCell
						)
					)
					{   nothingHappened = false;
					}
				}
			}
		}

		if( amActive && nothingHappened )
			oneLastRefreshRequired = true;

		amActive = !nothingHappened;
		return amActive;
	}


	/** Transition the neighborhood to the previously-computed
	 *  state.
	 *  @return true if the transition actually changed anything.
	 *  @see #figureNextState
	 */
	public boolean transition()
	{
		boolean someSubcellChangedState = false;

		activeEdges.clear();							/*(1)*/

		for( int row = 0; row < gridSize; ++row ) //{=transition.start}
			for( int column = 0; column < gridSize; ++column )
			{	if( grid[row][column].transition() )
				{	rememberThatCellAtEdgeChangedState(row, column);
					someSubcellChangedState = true;
				}								 //{=transition.end}
			}

		return someSubcellChangedState;
	}


	/** Modifies activeEdges to indicate whether the addition
	 *  of the cell at (row,column) makes an edge active.
	 */
	private void rememberThatCellAtEdgeChangedState(int row,int column)
	{
		if( row == 0 )
		{	activeEdges.add( Direction.NORTH );

			if(column==0)
				activeEdges.add( Direction.NORTHWEST );
			else if(column==gridSize-1)
				activeEdges.add( Direction.NORTHEAST );
		}
		else if( row == gridSize-1 )
		{	activeEdges.add( Direction.SOUTH );

			if(column==0)
				activeEdges.add( Direction.SOUTHWEST );
			else if(column==gridSize-1)
				activeEdges.add( Direction.SOUTHEAST );
		}

		if( column == 0 )
		{	activeEdges.add( Direction.WEST );
		}
		else if( column == gridSize-1 )
		{	activeEdges.add( Direction.EAST );
		}
		// else it's an internal cell. Do nothing.
	}

	/** Redraw the current neighborhood only if necessary (something
	 *  changed in the last transition).
	 *
	 *  @param painter Draw using this painter.
	 *  @param here Bounding rectangle for current Neighborhood.
	 *  @param drawAll force a redraw, even if nothing has changed.
	 *  @see #transition
	 */

	public void redraw(CellPainter painter, Rectangle here, boolean drawAll)
	{	
		// If the current neighborhood is stable (nothing changed
		// in the last transition stage), then there's nothing
		// to do. Just return. Otherwise, update the current block
		// and all sub-blocks. Since this algorithm is applied
		// recursively to sublocks, only those blocks that actually
		// need to update will actually do so.

			
		if( !amActive && !oneLastRefreshRequired && !drawAll )
			return;

		oneLastRefreshRequired = false;
		int compoundWidth = here.width;
		Rectangle subcell = new Rectangle(	here.x, here.y,
										here.width  / gridSize,
										here.height / gridSize );

		for( int row = 0; row < gridSize; ++row )
		{   for( int column = 0; column < gridSize; ++column )
			{   grid[row][column].redraw( painter, subcell, drawAll );	// {=Neighborhood.redraw3}
			    subcell.translate( subcell.width, 0);
			}
			subcell.translate(-compoundWidth, subcell.height);
		}

		painter.paintBorder( here, amActive );
	}

	/** Return the edge cell in the indicated row and column.
	 */
	public Cell	edge(int row, int column)
	{	assert 	  (row	  == 0 || row	 == gridSize-1)
			   || (column == 0 || column == gridSize-1)
			   :  "central cell requested from edge()";

		return grid[row][column];
	}

	/** Notification of a mouse click. The point is relative to the
	 *  upper-left corner of the surface.
	 */
	public void userClicked(Point here, Rectangle surface)
	{	deliver( here, surface, (cell, position, subcell) -> cell.userClicked(position, subcell) );
	}

	// [수정] 2026-11-22: 셀 색 기능. 칠하기도 클릭과 같은 길(deliver)로 하위 셀을 찾아 전달하고 블록을 깨움.
	// 그래서 색만 바뀐 셀(생사는 그대로)도 화면에 다시 그려짐
	public void userBrushed(Point here, Rectangle surface, CellColor color){
		deliver( here, surface, (cell, position, subcell) -> cell.userBrushed(position, subcell, color) );
	}

	private interface Delivery{
		void to( Cell cell, Point position, Rectangle subcell );
	}

	private void deliver(Point here, Rectangle surface, Delivery delivery)
	{
		int pixelsPerCell = surface.width / gridSize ;
		int row				= here.y     	/ pixelsPerCell ;
		int column			= here.x     	/ pixelsPerCell ;
		int rowOffset		= here.y     	% pixelsPerCell ;
		int columnOffset	= here.x     	% pixelsPerCell ;

		Point position = new Point( columnOffset, rowOffset );
		Rectangle subcell = new Rectangle(	0, 0, pixelsPerCell,
												  pixelsPerCell );

		delivery.to( grid[row][column], position, subcell ); //{=Neighborhood.userClicked.call}
		amActive = true;
		rememberThatCellAtEdgeChangedState(row, column);
	}

	public boolean isAlive()
	{	return true;
	}

	public int widthInCells()
	{	return gridSize * grid[0][0].widthInCells();
	}

	// [수정] 2026-11-16: Visitor가 블록 안의 모든 Cell을 방문하도록 넘겨 줌 (Composite이 트리를 돎).
	// 자식이 블록이든 셀이든 똑같이 accept()를 부르므로, 바깥 블록 → 안쪽 블록 → 셀 순서로 재귀 방문됨

	/** Visit this block: tell the visitor we are entering it, pass
	 *  the visitor on to every cell inside it, then tell the visitor
	 *  we are leaving it.
	 */
	public void accept( CellVisitor visitor )
	{	visitor.enterNeighborhood( this );
		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column].accept( visitor );
		visitor.leaveNeighborhood( this );
	}

	// [수정] 2026-11-16: 통계(활성 블록 수)용 조회 메서드. amActive는 블록 건너뛰기가 쓰는 값으로,
	// 마지막 세대 진행에서 블록 안의 셀이 바뀌었으면 true (셀 클릭이나 Load로 셀을 놓은 블록도 true)

	/** Return true if a cell in this block changed state in the last
	 *  generation (so the block was not skipped).
	 */
	public boolean isActive()
	{	return amActive;
	}

	public void clear()
	{	activeEdges.clear();

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column].clear();

		amActive = false;
	}

	/** Cause subcells to add an annotation to the indicated
	 *  memento if they happen to be alive.
	 */

	public boolean transfer(Storable memento, Point corner,
														boolean load)
	{ 	int   subcellWidth	= grid[0][0].widthInCells();
		int   myWidth		= widthInCells();
		Point upperLeft	= new Point( corner );

		for( int row = 0; row < gridSize; ++row )
		{   for( int column = 0; column < gridSize; ++column )
			{	if(grid[row][column].transfer(memento,upperLeft,load))
				{	amActive = true;
					rememberThatCellAtEdgeChangedState(row, column);
				}

				Direction d =
						grid[row][column].isDisruptiveTo();

				if( !d.equals( Direction.NONE ) )
					activeEdges.add(d);

				upperLeft.translate( subcellWidth, 0);
			}
			upperLeft.translate(-myWidth, subcellWidth );
		}
		return amActive;
	}

	public Storable createMemento()
	{	Memento m = new NeighborhoodState();
		transfer(m, new Point(0,0), Cell.STORE);
		return m;
	}

	// [수정] 2026-11-10: 기존 .life 형식을 LifeFormatAdapter가 쓸 수 있도록 빈 Memento를 만드는 factory 추가.
	// NeighborhoodState는 private으로 두고 Cell.Memento 인터페이스로만 내줌
	static Cell.Memento createEmptyMemento()
	{	return new NeighborhoodState();
	}

	/**
	 * The NeighborhoodState stores the state of this neighborhood
	 * and all its sub-neighborhoods. For the moment, I'm storing
	 * state with serialization, but a future modification might
	 * rewrite load() and flush() to use XML.
	 */

	private static class NeighborhoodState implements Cell.Memento
	{	Collection liveCells = new LinkedList();

		// [수정] 2026-11-23: 산 셀의 색. 파일에는 liveCells 다음에 RGB 숫자로 씀(CellColor 클래스 이름이 파일에 들어가지 않게)
		Map<Point, CellColor> colors = new HashMap<>();

		public NeighborhoodState( InputStream in ) throws IOException
												{ load(in); }
		public NeighborhoodState( 			   ){			}

		public void load( InputStream in ) throws IOException
		{	try
			{	CountingInputStream counted = new CountingInputStream( in );
				ObjectInputStream source = new ObjectInputStream( counted );
				Collection read = (Collection)( source.readObject() );

				// [수정] 2026-11-10: Point가 아닌 원소가 든 파일은 오류 없이 읽혀 보드가 빈 채로 Load되었음.
				// 원소를 모두 확인하고 통과해야 liveCells를 바꿈
				for( Object cell : read )
					if( !(cell instanceof Point) )
						throw new IOException( "Not a Life file: expected Points" );

				Map<Point, CellColor> readColors = readColors( source, counted );
				liveCells = read;
				colors = readColors;
			}
			// [수정] 2026-11-10: Collection이 아닌 객체가 든 파일이면 캐스팅에서 ClassCastException이 나서
			// Universe가 IOException만 잡는 탓에 오류 창 없이 실패했음. ClassCastException도 IOException으로 바꿔 던짐
			catch( ClassNotFoundException | ClassCastException e )
			{	// This exception shouldn't be rethrown as
				// a ClassNotFoundException because the
				// outside world shouldn't know (or care) that we're
				// using serialization to load the object. Nothing
				// wrong with treating it as an I/O error, however.

				throw new IOException( "Not a Life file", e );
			}
		}

		public void flush( OutputStream out ) throws IOException
		{	ObjectOutputStream sink = new ObjectOutputStream(out);
			sink.writeObject( liveCells );
			sink.writeObject( toRgb( colors ) );
		}

		// [수정] 2026-11-23: 예전 파일은 liveCells 뒤에 색이 없어서 EOF가 남. 그러면 색 없이(기본색) 읽음
		private static Map<Point, CellColor> readColors( ObjectInputStream source, CountingInputStream counted ) throws IOException, ClassNotFoundException{
			long start = counted.getCount();
			Object read;
			try{
				read = source.readObject();
			}
			catch( EOFException e ){
				// [수정] 2026-11-24: 색 표가 아예 없으면(한 바이트도 못 읽음) 예전 파일이라 기본색으로 읽고,
				// 색 표를 읽다가 끊기면 깨진 파일이라 IOException을 던짐. 그대로 두면 색을 모두 잃은 채 Load되었음
				if( counted.getCount() == start )
					return new HashMap<>();
				throw new IOException( "Not a Life file: cell colors are cut off", e );
			}

			if( !(read instanceof Map) )
				throw new IOException( "Not a Life file: expected cell colors" );

			Map<Point, CellColor> colors = new HashMap<>();
			for( Map.Entry<?, ?> entry : ((Map<?, ?>) read).entrySet() ){
				if( !(entry.getKey() instanceof Point) || !(entry.getValue() instanceof Integer) )
					throw new IOException( "Not a Life file: expected cell colors" );
				int rgb = (Integer) entry.getValue();
				colors.put( (Point) entry.getKey(), new CellColor( (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF ) );
			}
			return colors;
		}

		private static class CountingInputStream extends FilterInputStream{
			private long count = 0;

			CountingInputStream( InputStream in ){
				super( in );
			}

			public long getCount(){
				return count;
			}

			public int read() throws IOException{
				int b = super.read();
				if( b >= 0 )
					++count;
				return b;
			}

			public int read( byte[] b, int off, int len ) throws IOException{
				int n = super.read( b, off, len );
				if( n > 0 )
					count += n;
				return n;
			}

			public long skip( long n ) throws IOException{
				long skipped = super.skip( n );
				count += skipped;
				return skipped;
			}
		}

		private static Map<Point, Integer> toRgb( Map<Point, CellColor> colors ){
			Map<Point, Integer> rgb = new HashMap<>();
			for( Map.Entry<Point, CellColor> entry : colors.entrySet() ){
				CellColor color = entry.getValue();
				rgb.put( entry.getKey(), (color.red() << 16) | (color.green() << 8) | color.blue() );
			}
			return rgb;
		}

		// [수정] 2026-11-24: 원래의 1인자 markAsAlive는 이제 Cell.Memento의 default(기본색으로 적음)가 맡음
		public void markAsAlive( Point location, CellColor color ){
			liveCells.add( new Point( location ) );
			colors.put( new Point( location ), color );
		}

		public CellColor colorAt( Point location ){
			return colors.getOrDefault( location, CellColor.DEFAULT );
		}

		public boolean isAlive(Point location)
		{	return liveCells.contains(location);
		}

		public String toString()
		{	StringBuffer b = new StringBuffer();

			b.append("NeighborhoodState:\n");
			for( Iterator i = liveCells.iterator(); i.hasNext() ;)
				b.append( ((Point) i.next()).toString() + "\n" );
			return b.toString();
		}
	}
}
