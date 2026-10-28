HolubLife.jar
Version 1.02
Released 16 Jul., 2005


version		description

1.01		Alexey Marinichev: Fixed the "stuck glider" problem in
			Neighborhood.java.

1.02		Added a workaround to a Swing bug that caused menus
			to be overwritten by the game. Swing just draws menus
			directly on the content-pane's main canvas, so these
			menus were being overwritten by the game itself if you
			selected a menu while the game was runing. Modified
			the Clock class to suspend any board updating when
			menu items are displayed.
