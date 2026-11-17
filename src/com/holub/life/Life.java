package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.command.GoMenu;
import com.holub.life.command.Command;
import com.holub.life.command.HaltCommand;
import com.holub.life.command.MacroCommand;
import com.holub.life.command.ShowRunReportCommand;
import java.util.function.Consumer;

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

					// [수정] 2026-11-10: Load/Store가 지원할 파일 형식을 이곳에서 정함 (순서대로 파일 창에 나옴).
					// 형식을 추가할 때는 이 목록에 한 줄만 더하면 됨
					java.util.List<PatternFormat> formats = java.util.List.of
					(	new LifeFormatAdapter( board.widthInCells() ),
						new RleFormat( board.widthInCells() )
					);
					Universe	universe = new Universe( board, game, formats );

					// [수정] 2026-11-16: 실행 통계. RunRecorder가 세대마다 통계(Visitor)를 모으고, Halt를 누르거나 모든 셀이
					// 죽으면 "멈추기 + 실행 리포트"(MacroCommand)를 실행해 창 아래 상태 표시줄에 결과를 보여 줌.
					// 리포트 Command는 어디에 출력하는지 모르고 출력 함수(statusBar::setText)만 받음. Swing은 이곳에만 있음
					JLabel				statusBar	= createStatusBar();
					Consumer<String>	showStatus	= statusBar::setText;
					RunRecorder			recorder	= new RunRecorder( game );

					Command haltAndReport = new MacroCommand
					(	new HaltCommand( game ),
						new ShowRunReportCommand( recorder, "사용자 정지", showStatus )
					);
					Command stopOnExtinction = new MacroCommand
					(	new HaltCommand( game ),
						new ShowRunReportCommand( recorder, "멸종으로 정지", showStatus )
					);
					recorder.whenExtinct( stopOnExtinction::execute );

					// [수정] 2026-11-10: Go 메뉴를 Clock 생성자 대신 이곳에서 등록함 (항목마다 Command)
					// [수정] 2026-11-16: Halt 메뉴에 "멈추기 + 실행 리포트"를 넘김
					new GoMenu( game, haltAndReport ).register();

					frame.install( universe, statusBar );
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

	// [수정] 2026-11-16: 창 아래 상태 표시줄 (글자 한 줄을 보여 주는 JLabel). 실행 리포트를 보여 줌

	/** Create the one-line status bar shown under the board.
	 */
	private static JLabel createStatusBar()
	{	JLabel statusBar = new JLabel( "실행 후 Halt를 누르면 이번 실행의 통계가 여기에 표시됩니다" );
		statusBar.setFont( statusBar.getFont().deriveFont( 11f ) );					// 글씨를 조금 작게
		statusBar.setBorder( BorderFactory.createEmptyBorder( 3, 6, 3, 6 ) );		// 위·왼쪽·아래·오른쪽 여백
		return statusBar;
	}

	/** Put the view into the middle of the frame and the status bar
	 *  under it, then show the frame.
	 */
	private void install( JComponent universe, JComponent statusBar )
	{	getContentPane().add( universe, BorderLayout.CENTER); //{=life.java.install}
		getContentPane().add( statusBar, BorderLayout.SOUTH );	// 창의 아래 칸
		pack();
		setVisible( true );
	}
}
