package com.holub.life.clock;	// [수정] 2026-11-27: 패키지 정리. com.holub.life에서 com.holub.life.clock로 옮김

import com.holub.tools.Publisher;

/***
 * The <code>Clock</code> class handles the timing of gameboard
 * updates. It sends notifications off to any observers every
 * time the clock "ticks." (The Go menu that sets the clock speed
 * is created by GoMenu.)
 * <p>
 * The actual timing is done by the {@link TickTimer} passed to the
 * constructor. In the application this is a {@link SwingTickTimer},
 * so every listener runs on the Swing event thread (EDT), one event
 * at a time, together with painting, mouse clicks and menu actions.
 *
 * <h2>Revisions</h2>
 * <p>
 * 12-8-2004	AIH	Added a kludge to the clock-tick handler that
 * 				checks whether any menu item is active before it
 * 				allows the clock to tick. This mod fixes a bug
 * 				that caused the running game to overwrite any
 * 				displayed menus. (This check now lives in
 * 				{@link SwingTickTimer}.)
 *
 * @include /etc/license.txt
 */

public class Clock implements TickSource
{
	// [수정] 2026-11-16: javax.swing.Timer를 직접 쓰지 않고 TickTimer 인터페이스로 받음 (Adapter 패턴의 Client).
	// Swing Timer와 열린 메뉴 검사는 SwingTickTimer(Adapter)로 옮겨서 Clock은 javax.swing을 import하지 않음
	private final TickTimer timer;

	// [수정] 2026-11-10: Go 메뉴 만들기(createMenus)를 GoMenu로 옮김. 메뉴 이름의 첫 글자로
	// 동작을 고르던 분기를 항목마다 Command 객체로 바꿈. Clock은 이제 메뉴(MenuSite)를 모름
	// [수정] 2026-11-16: Singleton(instance()) 제거. Clock을 만드는 곳은 Life.main 하나뿐이고, 쓰는 쪽(LifeGame, Go 메뉴)은
	// 모두 TickSource를 생성자로 받으므로 전역 접근이 필요 없음. 어떤 타이머를 쓸지는 만드는 쪽(Life.main)이 정함

	/** Create a clock that is driven by the given timer.
	 *  @param timer Repeats the clock's ticks. Use a {@link SwingTickTimer}
	 *  			 in the application.
	 */
	public Clock( TickTimer timer )
	{	this.timer = timer;
	}

	/** Start up the clock.
	 *  @param millisecondsBetweenTicks The number of milliseconds between
	 *  					 ticks. A value of 0 indicates that
	 *  					 the clock should be stopped.
	 */

	public void startTicking( int millisecondsBetweenTicks )
	{	if( millisecondsBetweenTicks > 0 )
			timer.start( millisecondsBetweenTicks, this::tick );
		else
			timer.stop();
	}

	/** Stop the clock
	 */

	public void stop()
	{	startTicking( 0 );
	}

	private Publisher publisher = new Publisher();

	/** Add a listener that's notified every time the clock ticks:
	 *  <PRE>
	 *  clock.addTickListener
	 *  (	new TickSource.Listener()
	 *  	{	public void tick()
	 *  		{	System.out.println("tick!");
	 *  		}
	 *		}
	 *  );
	 *  </PRE>
	 */
	public void addTickListener( TickSource.Listener observer )
	{	publisher.subscribe(observer);
	}

	/** Same as {@link #addTickListener}. Kept for existing callers.
	 */
	public void addClockListener( Listener observer )
	{	addTickListener( observer );
	}

	/** Implement this interface to be notified about clock ticks.
	 *  It is the same as {@link TickSource.Listener}, and is kept
	 *  for existing callers.
	 *  @see Clock
	 */
	public interface Listener extends TickSource.Listener
	{
	}

	/** Force the clock to "tick," even if it's not time for
	 *  a tick. Useful for forcing a tick when the clock is
	 *  stopped. (Life uses this for single stepping.)
	 */
	public void tick()
	{	publisher.publish
		(	new Publisher.Distributor()
			{	public void deliverTo( Object subscriber )
				{	((TickSource.Listener)subscriber).tick();
				}
			}
		);
	}
}
