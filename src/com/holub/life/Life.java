package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.command.GoMenu;

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{
	// [수정] 2026-11-09: 게임 조립(Board, LifeGame, Universe)을 Universe 싱글톤에서 이곳으로 옮김.
	// 무엇이 어떤 순서로 만들어지는지 한곳에서 보이고, Universe는 받은 것만 씀.
	// Swing 객체는 이벤트 스레드(EDT)에서 만들어야 하므로 invokeLater 안에서 조립함

	public static void main( String[] arguments )
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Life frame = new Life();	// establishes the MenuSite

					// Reserve the Grid menu first, so that it stays to the
					// left of the Go menu.
					MenuSite.addMenu( frame, "Grid" );

					Board		board	 = new Board();
					LifeGame	game	 = new LifeGame( board, Clock.instance() );
					Universe	universe = new Universe( board, game );

					// [수정] 2026-11-10: Go 메뉴를 Clock 생성자 대신 이곳에서 등록함 (항목마다 Command)
					new GoMenu( game ).register();

					frame.install( universe );
				}
			}
		);
	}

	private Life()
	{	super( "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

		setDefaultCloseOperation	( EXIT_ON_CLOSE 		);
		getContentPane().setLayout	( new BorderLayout()	);
	}

	/** Put the view into the frame and show the frame.
	 */
	private void install( JComponent universe )
	{	getContentPane().add( universe, BorderLayout.CENTER); //{=life.java.install}
		pack();
		setVisible( true );
	}
}
