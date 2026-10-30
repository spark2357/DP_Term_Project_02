package com.holub.life;

/**
 * Something that can be started and stopped to drive the game
 * forward at a fixed rate. The Clock implements this interface in
 * the application. Tests can supply a fake implementation instead,
 * so that code which needs to stop or start the clock (such as
 * LifeGame) can be used without a real timer.
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
}
