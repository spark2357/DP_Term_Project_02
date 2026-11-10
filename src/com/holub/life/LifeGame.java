package com.holub.life;

import java.io.*;
import java.awt.Point;
import java.util.Set;
import com.holub.tools.Publisher;

/**
 * LifeGame is a Facade over the game subsystem: the Board (the
 * model) and the TickSource (the clock that drives it). Some
 * operations must use both parts together. For example, loading or
 * storing a pattern also stops the clock, so the game is halted
 * afterwards. (Ticks run on the Swing event thread, so a tick can
 * never overlap a load or store; stopping is a rule of the game,
 * not a thread-safety measure.) LifeGame captures that rule in a
 * single call, so callers (the
 * Universe, and later Commands) don't need to know it.
 *
 * LifeGame adds no game features of its own. New features go into
 * the Board first, and LifeGame only delegates to them. Using the
 * Board directly (for drawing or for tests) is not forbidden, but
 * anything that involves the clock should go through LifeGame.
 * <p>
 * LifeGame also drives the game: it listens to the TickSource and
 * advances the board one generation on every tick. Whenever a step
 * changes the board, it tells its own listeners (the Universe), so
 * the view never talks to the clock.
 *
 * @include /etc/license.txt
 */

public class LifeGame
{	private final Board 		board;
	private final TickSource 	ticker;

	private final Publisher		publisher = new Publisher();

	// [수정] 2026-11-09: "tick이 오면 한 세대 진행"은 게임 규칙이므로 Universe(화면)에서 이곳으로 옮김.
	// LifeGame이 TickSource를 구독하고, 보드가 바뀌면 자기 구독자(Universe)에게 알림

	public LifeGame( Board board, TickSource ticker )
	{	this.board  = board;
		this.ticker = ticker;

		// Every tick advances the game by one generation. (Registering
		// here is safe: everything runs on the Swing event thread, and
		// the clock is not started until after construction.)
		ticker.addTickListener
		(	new TickSource.Listener()
			{	public void tick()
				{	step();
				}
			}
		);
	}

	/** Implement this interface to be told when a step changed the
	 *  board (so the screen needs to be refreshed).
	 */
	public interface Listener
	{	void boardChanged();
	}

	/** Add a listener that is told every time a step changes the board.
	 */
	public void addGameListener( Listener listener )
	{	publisher.subscribe( listener );
	}

	/** Advance the board by one generation. If anything changed,
	 *  tell the listeners. This is called on every tick, and can also
	 *  be called directly (for single stepping, or from tests).
	 *  @return true if anything on the board changed state.
	 */
	public boolean step()
	{	boolean changed = board.step();
		if( changed )
			publisher.publish
			(	new Publisher.Distributor()
				{	public void deliverTo( Object subscriber )
					{	((Listener)subscriber).boardChanged();
					}
				}
			);
		return changed;
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

	// [수정] 2026-11-10: Go → Tick (Single Step)을 Facade에 추가. 원본처럼 시계를 한 번 tick하므로
	// 메뉴가 열려 있으면 건너뛰는 Clock의 규칙도 그대로 적용됨

	/** Advance the game by one generation (single step) by
	 *  ticking the TickSource once. The game is not started.
	 */
	public void tick()
	{	ticker.tick();
	}

	/** Toggle the cell at the indicated row and column (in cells).
	 */
	public void toggle( int row, int column )
	{	board.toggle( row, column );
	}

	/** Return the positions of all live cells (x = column, y = row).
	 */
	public Set<Point> liveCells()
	{	return board.liveCells();
	}

	/** Kill every cell on the board.
	 */
	public void clear()
	{	board.clear();
	}

	// [수정] 2026-11-10: 파일 형식(.life, .rle)을 인자로 받음. 어떤 형식인지는 부르는 쪽(파일 이름)이 정함

	/** Stop the game, then replace the board with the pattern read
	 *  from <code>in</code> in the given format. If reading fails,
	 *  the board is left unchanged. The stream is not closed.
	 */
	public void load( InputStream in, PatternFormat format ) throws IOException
	{	ticker.stop();
		board.load( in, format );
	}

	/** Stop the game, then write the board to <code>out</code> in
	 *  the given format. The stream is not closed.
	 */
	public void store( OutputStream out, PatternFormat format ) throws IOException
	{	ticker.stop();
		board.store( out, format );
	}
}
