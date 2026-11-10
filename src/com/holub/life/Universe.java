package com.holub.life;

import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;			// not java.awt.List
import java.util.Locale;
import java.util.Optional;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.holub.io.Files;
import com.holub.ui.MenuSite;

import com.holub.life.BoardView;
import com.holub.life.LifeGame;

/**
 * The Universe is the Swing view of the game. It does not create
 * the game: the Board and the LifeGame are built in Life.main and
 * passed in. It draws the
 * Board, handles all Swing events (mouse clicks, Grid menu,
 * file dialogs), and translates them into requests to the
 * LifeGame facade. The game logic itself (building the Composite
 * Neighborhood, advancing a generation, loading and storing)
 * lives in the Board, and the order in which the Board and the
 * Clock must be used lives in LifeGame. The Universe does not
 * listen to the clock: LifeGame advances the game on every tick and
 * tells the Universe when the board changed, so it can redraw.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{	// [수정] 2026-11-09: Board 대신 읽기 전용 BoardView로 받음. 보드를 바꾸는 요청은
	// LifeGame을 거칠 수밖에 없도록 컴파일 단계에서 강제함
	private 		final BoardView	board;	// read only: used just for drawing
	private 		final LifeGame	game;	// every request that changes the game

	// [수정] 2026-11-10: Load/Store가 쓰는 파일 형식 목록을 생성자로 받음. 어떤 형식을 지원할지는
	// 조립(Life.main)이 정하고, 화면은 받은 목록으로 파일 창 필터를 만들고 확장자로 형식을 고르기만 함
	private final List<PatternFormat> formats;

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 8;

	// [수정] 2026-11-09: 싱글톤을 없애고 Board와 LifeGame을 생성자로 받음.
	// 화면(View)이 모델을 직접 만들지 않고, 조립은 Life.main에서 함

	/** Create the view of a game that is already assembled.
	 *  @param board drawn directly by {@link #paint}. It is a
	 *  			 read-only view, so the Universe cannot change it.
	 *  @param game  receives every request that changes the game.
	 *  @param formats the file formats offered by Load and Store,
	 *  			 in the order they appear in the file chooser.
	 */
	public Universe( BoardView board, LifeGame game, List<PatternFormat> formats )
	{	this.board   = board;
		this.game    = game;
		this.formats = List.copyOf( formats );

		final Dimension PREFERRED_SIZE =
						new Dimension
						(  board.widthInCells() * DEFAULT_CELL_SIZE,
						   board.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be an even multiple of 63.

					Rectangle bounds = getBounds();
					bounds.height /= board.widthInCells();
					bounds.height *= board.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		addMouseListener					//{=Universe.mouse}
		(	new MouseAdapter()
			{	public void mousePressed(MouseEvent e)
				{	// Translate the pixel position into a cell position.
					// Knowing the size of the panel is the view's job.

					int pixelsPerCell = getBounds().width / board.widthInCells();
					int row		= e.getY() / pixelsPerCell;
					int column	= e.getX() / pixelsPerCell;
					game.toggle( row, column );
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	game.clear();
					repaint();
				}
			}
		);

		MenuSite.addLine			// {=Universe.load.setup}
		(	this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doLoad();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
				{	doStore();
				}
			}
		);

		MenuSite.addLine
		(	this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed(ActionEvent e)
		        {	System.exit(0);
		        }
			}
		);

		// [수정] 2026-11-09: Clock을 직접 구독해 game.step()을 부르던 것을, LifeGame의 알림을 받도록 변경.
		// 세대 진행은 LifeGame이 맡고, 화면은 "보드가 바뀌었다"는 알림에 다시 그리기만 함

		game.addGameListener
		(	new LifeGame.Listener()
			{	public void boardChanged()
				{	refreshNow();
				}
			}
		);
	}

	// [수정] 2026-11-10: Load/Store 흐름 정리
	// - 파일 창을 열기 전에 게임을 멈춤. 창이 떠 있는 동안에도 tick이 계속 와서, Store는 OK를 누른
	//   순간의 보드를 저장 (Swing Timer로 바꾼 뒤에도 모달 창은 이벤트를 계속 처리함)
	// - 취소하면 아무 일도 하지 않음.
	// - try-with-resources로 실패해도 파일을 닫음. 원본은 예외가 나면 스트림이 열린 채 남았음
	// - 확장자로 형식(.life, .rle)을 고르고, 오류 창에 원인 출력

	private void doLoad()
	{	game.stop();

		Optional<File> chosen = Files.userSelected( ".", "Load", fileFilters(true) );
		if( chosen.isEmpty() )
			return;								// cancelled: not an error

		File file = chosen.get();
		Optional<PatternFormat> format = formatFor( file );
		if( format.isEmpty() )
		{	showError( "Can't load \"" + file.getName() + "\".\n"
						+ "Supported files: " + extensionList() + "." );
			return;
		}

		try( InputStream in = new FileInputStream( file ) )
		{	game.load( in, format.get() );		// keeps the board if reading fails
		}
		catch( IOException theException )
		{	showError( "Can't load \"" + file.getName() + "\".\n"
						+ "The file may be missing or damaged, may not be a Life pattern,\n"
						+ "or may be larger than the board ("
						+ board.widthInCells() + "x" + board.widthInCells() + ")." );
		}
		repaint();
	}

	private void doStore()
	{	game.stop();

		Optional<File> chosen = Files.userSelected( ".", "Store", fileFilters(false) );
		if( chosen.isEmpty() )
			return;								// cancelled: not an error

		File file = chosen.get();
		Optional<PatternFormat> format = formatFor( file );
		if( format.isEmpty() )
		{	showError( "Can't save \"" + file.getName() + "\".\n"
						+ "Use a file name that ends in " + extensionList() + "." );
			return;
		}

		try( OutputStream out = new FileOutputStream( file ) )
		{	game.store( out, format.get() );
		}
		catch( IOException theException )
		{	showError( "Can't save \"" + file.getName() + "\".\n"
						+ "Check that the folder exists and that you can write to it." );
		}
	}

	private void showError( String message )
	{	JOptionPane.showMessageDialog( null, message,
				"The Game of Life", JOptionPane.ERROR_MESSAGE);
	}

	/** Return the format whose extension the file name ends in
	 *  (ignoring case), or an empty Optional if there is none.
	 */
	private Optional<PatternFormat> formatFor( File file )
	{	String name = file.getName().toLowerCase( Locale.ROOT );
		for( PatternFormat format : formats )
			if( name.endsWith( "." + format.extension() ) )
				return Optional.of( format );
		return Optional.empty();
	}

	/** One file chooser filter for each format. If
	 *  <code>allFormatsFirst</code> is true, a filter that shows
	 *  every format at once comes first (used for Load).
	 */
	private FileNameExtensionFilter[] fileFilters( boolean allFormatsFirst )
	{	List<FileNameExtensionFilter> filters = new ArrayList<>();
		if( allFormatsFirst )
		{	String[]	 extensions = new String[ formats.size() ];
			List<String> patterns	= new ArrayList<>();
			for( int i = 0; i < extensions.length; ++i )
			{	extensions[i] = formats.get(i).extension();
				patterns.add( "*." + extensions[i] );
			}
			filters.add( new FileNameExtensionFilter(
						"Pattern Files (" + String.join( ", ", patterns ) + ")", extensions ) );
		}
		for( PatternFormat format : formats )
			filters.add( new FileNameExtensionFilter(
						format.description() + " (*." + format.extension() + ")",
						format.extension() ) );
		return filters.toArray( new FileNameExtensionFilter[0] );
	}

	/** The supported extensions, for messages: ".life, .rle"
	 */
	private String extensionList()
	{	List<String> extensions = new ArrayList<>();
		for( PatternFormat format : formats )
			extensions.add( "." + format.extension() );
		return String.join( ", ", extensions );
	}

	/** Override paint to ask the Board to draw itself.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the Board.)
	 */

	public void paint(Graphics g)
	{
		Rectangle panelBounds = getBounds();
		Rectangle clipBounds  = g.getClipBounds();

		// The panel bounds is relative to the upper-left
		// corner of the screen. Pretend that it's at (0,0)
		panelBounds.x = 0;
		panelBounds.y = 0;
		board.redraw(g, panelBounds, true);		//{=Universe.redraw1}
	}

	/** Force a screen refresh by queuing a request on
	 *  the Swing event queue. This method is called on every
	 *  clock tick, which already runs on the Swing event thread,
	 *  so the request simply runs after the current tick has
	 *  finished. (In the original code ticks came from another
	 *  thread, and this queue was used as an Active Object.) Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )		// Universe not displayable
						return;
					try
					{
						Rectangle panelBounds = getBounds();
						panelBounds.x = 0;
						panelBounds.y = 0;
						board.redraw(g, panelBounds, false); //{=Universe.redraw2}
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}
}
