package com.holub.life;

/**
 * Something that can be started and stopped to drive the game
 * forward at a fixed rate, and that tells its listeners about every
 * tick. The Clock implements this interface in
 * the application. Tests can supply a fake implementation instead,
 * so that code which needs to stop or start the clock (such as
 * LifeGame) can be used without a real timer.
 * <p>
 * Ticks must be delivered on the Swing event thread (EDT). The Board
 * is not thread-safe: drawing, mouse clicks and menu actions all use
 * it on the EDT, so a tick from another thread would race with them.
 *
 * @include /etc/license.txt
 */

public interface TickSource
{
	/** Start ticking.
	 *  @param millisecondsBetweenTicks The number of milliseconds between
	 *  					 ticks. A value of 0 indicates that
	 *  					 the ticking should stop.
	 */
	void startTicking( int millisecondsBetweenTicks );

	/** Stop ticking.
	 */
	void stop();

	// [수정] 2026-11-09: tick 구독을 인터페이스로 올림. LifeGame이 구체 클래스 Clock을 몰라도
	// tick을 받을 수 있게 하기 위함

	/** Add a listener that is told about every tick. Ticks must be
	 *  delivered on the Swing event thread (see above).
	 */
	void addTickListener( Listener listener );

	/** Implement this interface to be notified about ticks.
	 */
	interface Listener
	{	void tick();
	}
}
