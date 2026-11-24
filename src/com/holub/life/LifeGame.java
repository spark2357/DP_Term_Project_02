package com.holub.life;

import java.io.*;
import java.awt.Point;
import java.util.Set;
import java.util.Optional;
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
 * <p>
 * LifeGame also keeps the Undo/Redo history. Every method that changes
 * the board (toggle, stamp, step, clear, load) records a snapshot of the
 * board from just before the change, so changes can be undone in the
 * order they happened. A call that leaves the board as it was is not
 * recorded.
 *
 * @include /etc/license.txt
 */

public class LifeGame
{	private final Board 		board;
	private final TickSource 	ticker;

	// [수정] 2026-11-23: Undo/Redo 기록.
	// 보드를 바꾸는 동작이 모두 이 LifeGame을 통해 이루어져서 여기서 스냅샷을 남김.
	// 마우스 편집과 세대 진행을 한 History에 둬서 스냅샷 기록을 한 곳에서 관리

	// Undo 최대 한도. BoardHistory에 저장 가능한 스냅샷 개수 제한
	public static final int UNDO_LIMIT = 100;

	private final BoardHistory	history = new BoardHistory( UNDO_LIMIT );

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

	// [수정] 2026-11-17: boardEdited() 추가. Clear, Load, 클릭, Stamp 뒤에도 알려서 Command가 화면을 몰라도
	// 다시 그려지게 함. clear()는 블록의 active 표시를 지우므로 바뀐 블록만 그리는 boardChanged()로는 안 지워짐

	/** Implement this interface to be told when the board changed
	 *  (so the screen needs to be refreshed).
	 */
	public interface Listener
	{	/** A step changed the board. Only the blocks that changed
		 *  need to be redrawn. */
		void boardChanged();

		/** The board was cleared, loaded, clicked or stamped.
		 *  The whole board must be redrawn. */
		void boardEdited();
	}

	/** Add a listener that is told every time a step or an edit
	 *  (clear, load, click, stamp) changes the board.
	 */
	public void addGameListener( Listener listener )
	{	publisher.subscribe( listener );
	}

	// [수정] 2026-11-23: 바뀐 세대만 Undo 기록에 저장

	/** Advance the board by one generation. If anything changed,
	 *  tell the listeners. This is called on every tick, and can also
	 *  be called directly (for single stepping, or from tests).
	 *  @return true if anything on the board changed state.
	 */
	public boolean step()
	{	BoardSnapshot before  = board.snapshot();
		boolean		  changed = board.step();
		if( changed )
		{	history.record( before );
			publisher.publish
			(	new Publisher.Distributor()
				{	public void deliverTo( Object subscriber )
					{	((Listener)subscriber).boardChanged();
					}
				}
			);
		}
		return changed;
	}

	private void publishBoardEdited()
	{	publisher.publish
		(	new Publisher.Distributor()
			{	public void deliverTo( Object subscriber )
				{	((Listener)subscriber).boardEdited();
				}
			}
		);
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

	// [수정] 2026-11-10: Go → Tick (Single Step)을 Facade에 추가. 원본처럼 시계를 한 번 tick함
	// [수정] 2026-11-16: 메뉴가 열려 있으면 건너뛰는 규칙은 SwingTickTimer(자동 실행)로 옮겨져 여기에는 적용되지 않음.
	// 메뉴 항목을 클릭하면 Swing이 메뉴를 먼저 닫고 동작을 실행하므로, 원본과 마찬가지로 한 단계 진행됨

	/** Advance the game by one generation (single step) by
	 *  ticking the TickSource once. The game is not started.
	 */
	public void tick()
	{	ticker.tick();
	}

	/** Toggle the cell at the indicated row and column (in cells).
	 */
	public void toggle( int row, int column )
	{	// toggle은 이전 스냅샷과 같을 일이 없어서 recordIfChanged 생략
		history.record( board.snapshot() );
		board.toggle( row, column );
		publishBoardEdited();
	}

	public void brush( int row, int column, CellColor color ){
		board.brush( row, column, color );
		publishBoardEdited();
	}

	public void setBirthColorRule( BirthColorRule rule ){
		board.setBirthColorRule( rule );
		publishBoardEdited();
	}

	public BirthColorRule birthColorRule(){
		return board.birthColorRule();
	}

	/** Return the positions of all live cells (x = column, y = row).
	 */
	public Set<Point> liveCells()
	{	return board.liveCells();
	}

	/** Kill every cell on the board.
	 */
	public void clear()
	{	BoardSnapshot before = board.snapshot();
		board.clear();
		recordIfChanged( before );
		publishBoardEdited();
	}

	// [수정] 2026-11-10: 파일 형식(.life, .rle)을 인자로 받음. 어떤 형식인지는 부르는 쪽(파일 이름)이 정함

	// [수정] 2026-11-23: Undo 기록 추가. 읽다 실패하면 board.load()가 예외를 던져 기록 줄까지 가지 않음 (보드도 그대로)

	/** Stop the game, then replace the board with the pattern read
	 *  from <code>in</code> in the given format. If reading fails,
	 *  the board is left unchanged. The stream is not closed.
	 */
	public void load( InputStream in, PatternFormat format ) throws IOException
	{	ticker.stop();
		BoardSnapshot before = board.snapshot();
		board.load( in, format );
		recordIfChanged( before );
		publishBoardEdited();
	}

	/** Stop the game, then write the board to <code>out</code> in
	 *  the given format. The stream is not closed.
	 */
	public void store( OutputStream out, PatternFormat format ) throws IOException
	{	ticker.stop();
		board.store( out, format );
	}

	/** Stamp the pattern with its upper-left corner at the given
	 *  row and column. The game keeps running, like a click.
	 */
	public void stamp( CellPattern pattern, int row, int column, CellColor color )
	{	BoardSnapshot before = board.snapshot();
		board.stamp( pattern, row, column, color );
		recordIfChanged( before );
		publishBoardEdited();
	}

	// [수정] 2026-11-23: 편집 뒤 보드가 그대로면 저장하지 않음
	// toggle은 항상 바뀌므로 쓰지 않음
	private void recordIfChanged( BoardSnapshot before )
	{	if( !board.snapshot().equals( before ) )
			history.record( before );
	}

	// [수정] 2026-11-23: Undo/Redo. 실행 중이면 먼저 멈춤. 멈추지 않으면 다음 tick이 바로 진행되고 redo가 바로 불가능해짐.
	// 되돌릴 것이 없어도 멈춤

	/** Stop the game and put the board back to how it was before the
	 *  last change (an edit or a generation).
	 *  @return false if there was nothing to undo (the game is
	 *  		stopped anyway).
	 */
	public boolean undo()
	{	ticker.stop();
		return apply( history.undo( board.snapshot() ) );
	}

	/** Stop the game and redo the change that the last undo took
	 *  back. A new change after an undo clears what can be redone.
	 *  @return false if there was nothing to redo.
	 */
	public boolean redo()
	{	ticker.stop();
		return apply( history.redo( board.snapshot() ) );
	}

	private boolean apply( Optional<BoardSnapshot> snapshot )
	{	if( snapshot.isEmpty() )
			return false;
		board.restore( snapshot.get() );
		publishBoardEdited();	// [수정] 2026-11-24: 셀 색 브랜치 병합 때 빠진 화면 갱신 알림 복구
		return true;
	}

}
