package com.holub.life;

import java.io.*;

/**
 * LifeGame is a Facade over the game subsystem: the Board (the
 * model) and the TickSource (the clock that drives it). Some
 * operations must use both parts in a fixed order. For example,
 * the clock must be stopped before the board is loaded or stored.
 * LifeGame captures that order in a single call, so callers (the
 * Universe, and later Commands) don't need to know it.
 *
 * LifeGame adds no game features of its own. New features go into
 * the Board first, and LifeGame only delegates to them. Using the
 * Board directly (for drawing or for tests) is not forbidden, but
 * anything that involves the clock should go through LifeGame.
 *
 * @include /etc/license.txt
 */

public class LifeGame
{	private final Board 		board;
	private final TickSource 	ticker;

	public LifeGame( Board board, TickSource ticker )
	{	this.board  = board;
		this.ticker = ticker;
	}

	/** Advance the board by one generation.
	 *  @return true if anything on the board changed state.
	 */
	public boolean step()
	{	return board.step();
	}

	/** Start running the game, one generation every
	 *  <code>millisecondsBetweenTicks</code> milliseconds.
	 */
	public void start( int millisecondsBetweenTicks )
	{	ticker.startTicking( millisecondsBetweenTicks );
	}

	/** Stop running the game.
	 */
	public void stop()
	{	ticker.stop();
	}

	/** Toggle the cell at the indicated row and column (in cells).
	 */
	public void toggle( int row, int column )
	{	board.toggle( row, column );
	}

	/** Kill every cell on the board.
	 */
	public void clear()
	{	board.clear();
	}

	/** Stop the game, then replace the board with the pattern read
	 *  from <code>in</code>. The stream is not closed.
	 */
	public void load( InputStream in ) throws IOException
	{	ticker.stop();
		board.load( in );
	}

	/** Stop the game, then write the board to <code>out</code>.
	 *  The stream is not closed.
	 */
	public void store( OutputStream out ) throws IOException
	{	ticker.stop();
		board.store( out );
	}
}
