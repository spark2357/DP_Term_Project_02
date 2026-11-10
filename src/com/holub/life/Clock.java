package com.holub.life;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;		// not java.util.Timer: ticks must run on the Swing event thread
import com.holub.tools.Publisher;

/***
 * The <code>Clock</code> class handles the timing of gameboard
 * updates. It sends notifications off to any observers every
 * time the clock "ticks." (The Go menu that sets the clock speed
 * is created by GoMenu.)
 * <p>
 * Ticks are delivered by a <code>javax.swing.Timer</code>, so every
 * listener runs on the Swing event thread (EDT), one event at a time,
 * together with painting, mouse clicks and menu actions. If the EDT
 * is busy, pending ticks are coalesced into one instead of being run
 * back to back.
 *
 * <h2>Revisions</h2>
 * <p>
 * 12-8-2004	AIH	Added a kludge to the clock-tick handler that
 * 				checks whether any menu item is active before it
 * 				allows the clock to tick. This mod fixes a bug
 * 				that caused the running game to overwrite any
 * 				displayed menus. See {@link #menuIsActive} for
 * 				details.
 *
 * @include /etc/license.txt
 */

public class Clock implements TickSource
{
	// [수정] 2026-11-07: java.util.Timer는 별도 스레드에서 tick을 실행해 그리기, 마우스 클릭(EDT)과 같은 셀을 동시에 건드렸음.
	// javax.swing.Timer로 바꿔 tick도 EDT의 이벤트 큐에서 하나씩 실행되게 함 (그리기, 클릭, 메뉴와 한 줄로 처리)
	private Timer clock = new Timer
	(	0,
		new ActionListener()
		{	public void actionPerformed(ActionEvent e){ tick(); }
		}
	);

	// [수정] 2026-11-10: Go 메뉴 만들기(createMenus)를 GoMenu로 옮김. 메뉴 이름의 첫 글자로
	// 동작을 고르던 분기를 항목마다 Command 객체로 바꿈. Clock은 이제 메뉴(MenuSite)를 모름
	private Clock()
	{
	}

	private static Clock instance;

	/** The clock is a singleton. Get a reference to it by calling
	 *  <code>Clock.instance()</code>. It's illegal to call
	 *  <code>new Clock()</code>.
	 */
	public synchronized static Clock instance()
	{	if( instance == null )
			instance = new Clock();
		return instance;
	}

	/** Start up the clock.
	 *  @param millisecondsBetweenTicks The number of milliseconds between
	 *  					 ticks. A value of 0 indicates that
	 *  					 the clock should be stopped.
	 */

	public void startTicking( int millisecondsBetweenTicks )
	{	clock.stop();

		if( millisecondsBetweenTicks > 0 )
		{	clock.setDelay( millisecondsBetweenTicks );
			clock.setInitialDelay( 0 );		// first tick right away, as before
			clock.start();
		}
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
				{	if( !menuIsActive() )
						((TickSource.Listener)subscriber).tick();
				}
			}
		);
	}

	/** Check if any item on the menu bar has been selected.
	 *  This is an incredible kluge. The problem is that Swing draws the
	 *  menus on the same "canvas" as the main frame. As a consequence,
	 *  displayed menus are overwritten by a running game. Moreover, Swing
	 *  provides no notification on the order of "some menu item has been
	 *  selected," so the only way to detect menu-bar activity is to poll
	 *  it. This method does just that, and returns true if some menu
	 *  is being displayed. The {@link #tick} method effectively suspends
	 *  clock ticks as long as a menu is displayed, (which slows down
	 *  the clock, unfortunately).
	 *  <p>
	 *  From a design-patterns perspective, this method demonstrates that
	 *  a facade (the MenuSite) does not provide strict isolation from
	 *  the underlying subsystem. I can't immagine another application
	 *  that would need to know whether or not the menu bar is active,
	 *  so adding this menu to the MenuSite would be "noise." Since
	 *  the method does something that's of no relevance to other
	 *  MenuSite users, it makes an "end run" around the facade.
	 */

	private boolean menuIsActive()
	{	MenuElement[] path =
					MenuSelectionManager.defaultManager().getSelectedPath();
		return ( path != null && path.length > 0 );
	}
}
