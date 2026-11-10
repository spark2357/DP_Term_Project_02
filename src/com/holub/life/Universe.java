package com.holub.life;

import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

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
	 */
	public Universe( BoardView board, LifeGame game )
	{	this.board = board;
		this.game  = game;

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

	private void doLoad()
	{	try
		{
			FileInputStream in = new FileInputStream(
			   Files.userSelected(".",".life","Life File","Load"));

			game.load( in );				// stops the game, then loads

			in.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
		repaint();
	}

	private void doStore()
	{	try
		{
			FileOutputStream out = new FileOutputStream(
				  Files.userSelected(".",".life","Life File","Write"));

			game.store( out );				// stops the game, then stores

			out.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Write Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
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
