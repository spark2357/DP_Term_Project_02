package com.holub.life;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import com.holub.io.Files;
import com.holub.life.Cell;
import com.holub.ui.MenuSite;
import com.holub.ui.Colors;
import com.holub.asynch.ConditionVariable;

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
	/** Block if reading is not permitted because the grid is
	 *  transitioning to the next state. Only one lock is
	 *  used (for the outermost neighborhood) since all updates
	 *  must be requested through the outermost neighborhood.
	 */
	private static final ConditionVariable readingPermitted =
											new ConditionVariable(true);

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
		// The condition variable is set and reset only by the
		// outermost neighborhood. It's actually incorrect
		// for an inner block to touch it because the whole
		// board has to be locked for edge cells in a subblock
		// to compute their next state correctly. There's no
		// race condition since the only place that transition()
		// is called is from the clock tick, and recursively
		// from here. As long as the recompute time is less
		// than the tick interval, everything's copacetic.

		boolean someSubcellChangedState = false;

		if( ++nestingLevel == 0 )
			readingPermitted.set(false);

		activeEdges.clear();							/*(1)*/

		for( int row = 0; row < gridSize; ++row ) //{=transition.start}
			for( int column = 0; column < gridSize; ++column )
			{	if( grid[row][column].transition() )
				{	rememberThatCellAtEdgeChangedState(row, column);
					someSubcellChangedState = true;
				}								 //{=transition.end}
			}
		
		if( nestingLevel-- == 0 )
			readingPermitted.set(true);

		return someSubcellChangedState;
	}
	// The following variable is used only by the transition()
	// method. Since Java doesn't support static local variables,
	// I am forced to declare it in class scope, but I deliberately
	// don't put it up at the top of the class defintion because
	// it's not really an attribute of the class---it's just
	// an implemenation detail of the immediately preceding
	// method.
	//
	private static int nestingLevel = -1;


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
	 *  @param g Draw onto this graphics.
	 *  @param here Bounding rectangle for current Neighborhood.
	 *  @param drawAll force a redraw, even if nothing has changed.
	 *  @see #transition
	 */

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
	{	
		// If the current neighborhood is stable (nothing changed
		// in the last transition stage), then there's nothing
		// to do. Just return. Otherwise, update the current block
		// and all sub-blocks. Since this algorithm is applied
		// recursively to sublocks, only those blocks that actually
		// need to update will actually do so.

			
		if( !amActive && !oneLastRefreshRequired && !drawAll )
			return;
		try
		{
			oneLastRefreshRequired = false;
			int compoundWidth = here.width;
			Rectangle subcell = new Rectangle(	here.x, here.y,
											here.width  / gridSize,
											here.height / gridSize );

			// Check to see if we can paint. If not, just return. If
			// so, actually wait for permission (in case there's
			// a race condition, then paint.

			if( !readingPermitted.isTrue() )	//{=Neighborhood.reading.not.permitted}
				return;

			readingPermitted.waitForTrue();

			for( int row = 0; row < gridSize; ++row )
			{   for( int column = 0; column < gridSize; ++column )
				{   grid[row][column].redraw( g, subcell, drawAll );	// {=Neighborhood.redraw3}
				    subcell.translate( subcell.width, 0);
				}
				subcell.translate(-compoundWidth, subcell.height);
			}

			g = g.create();
			g.setColor( Colors.LIGHT_ORANGE );
			g.drawRect( here.x, here.y, here.width, here.height );

			if( amActive )
			{	g.setColor( Color.BLUE );
				g.drawRect(	here.x+1,	  here.y+1,
							here.width-2, here.height-2 );
			}

			g.dispose();
		}
		catch( InterruptedException e ) 
		{	// thrown from waitForTrue. Just
			// ignore it, since not printing is a
			// reasonable reaction to an interrupt.
		}
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
	{
		int pixelsPerCell = surface.width / gridSize ;
		int row				= here.y     	/ pixelsPerCell ;
		int column			= here.x     	/ pixelsPerCell ;
		int rowOffset		= here.y     	% pixelsPerCell ;
		int columnOffset	= here.x     	% pixelsPerCell ;

		Point position = new Point( columnOffset, rowOffset );
		Rectangle subcell = new Rectangle(	0, 0, pixelsPerCell,
												  pixelsPerCell );

		grid[row][column].userClicked(position, subcell); //{=Neighborhood.userClicked.call}
		amActive = true;
		rememberThatCellAtEdgeChangedState(row, column);
	}

	public boolean isAlive()
	{	return true;
	}

	public int widthInCells()
	{	return gridSize * grid[0][0].widthInCells();
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
					amActive = true;

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

	/**
	 * The NeighborhoodState stores the state of this neighborhood
	 * and all its sub-neighborhoods. For the moment, I'm storing
	 * state with serialization, but a future modification might
	 * rewrite load() and flush() to use XML.
	 */

	private static class NeighborhoodState implements Cell.Memento
	{	Collection liveCells = new LinkedList();

		public NeighborhoodState( InputStream in ) throws IOException
												{ load(in); }
		public NeighborhoodState( 			   ){			}

		public void load( InputStream in ) throws IOException
		{	try
			{	ObjectInputStream source = new ObjectInputStream( in );
				liveCells = (Collection)( source.readObject() );
			}
			catch(ClassNotFoundException e)
			{	// This exception shouldn't be rethrown as
				// a ClassNotFoundException because the
				// outside world shouldn't know (or care) that we're
				// using serialization to load the object. Nothring
				// wrong with treating it as an I/O error, however.

				throw new IOException(
							"Internal Error: Class not found on load");
			}
		}

		public void flush( OutputStream out ) throws IOException
		{	ObjectOutputStream sink = new ObjectOutputStream(out);
			sink.writeObject( liveCells );
		}
	
		public void markAsAlive(Point location)
		{	liveCells.add( new Point( location ) );
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
