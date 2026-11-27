package com.holub.life;

import java.awt.*;
import javax.swing.*;
import com.holub.ui.MenuSite;
import com.holub.life.command.EditMenu;
import com.holub.life.command.GoMenu;
import com.holub.life.command.GridMenu;
import com.holub.life.view.SwingDialogs;
import com.holub.life.mouse.MouseController;
import com.holub.life.mouse.BrushMode;
import com.holub.life.command.Command;
import com.holub.life.command.HaltCommand;
import com.holub.life.command.MacroCommand;
import com.holub.life.command.ShowRunReportCommand;
import java.util.function.Consumer;
import com.holub.life.mouse.Pen;
import com.holub.life.view.BrushToolBar;
import com.holub.life.clock.Clock;
import com.holub.life.clock.SwingTickTimer;
import com.holub.life.format.LifeFormatAdapter;
import com.holub.life.format.PatternFormats;
import com.holub.life.format.RleFormat;
import com.holub.life.game.LifeGame;
import com.holub.life.game.RunRecorder;
import com.holub.life.model.Board;
import com.holub.life.model.CellColor;
import com.holub.life.view.Universe;

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

					// [수정] 2026-11-16: Clock Singleton을 없애고 이곳에서 만듦. 어떤 타이머로 tick을 낼지(SwingTickTimer)도 이곳에서 정함
					Board		board	 = new Board();
					Clock		clock	 = new Clock( new SwingTickTimer() );
					LifeGame	game	 = new LifeGame( board, clock );

					// [수정] 2026-11-10: Load/Store가 지원할 파일 형식을 이곳에서 정함 (순서대로 파일 창에 나옴).
					// 형식을 추가할 때는 이 목록에 한 줄만 더하면 됨
					RleFormat		rle		= new RleFormat( board.widthInCells() );
					PatternFormats	formats	= new PatternFormats( java.util.List.of
					(	new LifeFormatAdapter( board.widthInCells() ),
						rle
					) );

					// [수정] 2026-11-17: 마우스 모드(State)와 Grid 메뉴(Command)를 이곳에서 조립함
					Pen				pen		 = new Pen( CellColor.DEFAULT );
					// [수정] 2026-11-27: 기본 마우스 모드를 칠하기(BrushMode)로 함. ToggleMode는 원본 클릭 그대로 남겨 둠
					MouseController mouse	 = new MouseController( new BrushMode( game, pen ) );
					Universe		universe = new Universe( board, game, mouse );

					new GridMenu( game, formats, rle, mouse, pen, new SwingDialogs() ).register();

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

					// [수정] 2026-11-23: Edit 메뉴(Undo, Redo) 추가
					new EditMenu( game ).register();

					BrushToolBar	toolBar	= new BrushToolBar( game, pen );

					frame.install( universe, toolBar, statusBar );
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

	/** Put the tool bar above the view, the view into the middle of
	 *  the frame and the status bar under it, then show the frame.
	 */
	// [수정] 2026-11-26: 11-24 merge에서 빠진 상태 표시줄(SOUTH)을 다시 붙임
	private void install( JComponent universe, JComponent toolBar, JComponent statusBar )
	{	getContentPane().add( toolBar,  BorderLayout.NORTH );
		getContentPane().add( universe, BorderLayout.CENTER); //{=life.java.install}
		getContentPane().add( statusBar, BorderLayout.SOUTH );	// 창의 아래 칸
		pack();
		setVisible( true );
	}
}
