// (c) 2003 Allen I Holub. All rights reserved.

package com.holub.tools;

import java.util.logging.*;
import java.io.*;

/** This class provides a single method that sets up logging for
 *  a particular package to go to the console window with the
 *  normal timestamp header stripped of.
 *  You can accomplish a similar thing by
 *  modifying the <i>$JAVA_HOME/jre/lib/logging.properties</i>
 *  file, setting the <code>.level</code> and
 *  <code>java.util.logging.ConsoleHandler.level</code>
 *  properties to <code>ALL</code>. This change causes all messages
 *  to go to the console, but the timestamp header will appear, too.
 *	<p>Here's an example:
 *  <pre>
 *  import com.holub.tools.Log;
 *
 *  Log.toScreen("com.holub.tools");
 *  //...
 *	private static final Logger log = Logger.getLogger("com.holub.tools");
 *	log.warning( "The sky is falling!" );
 *	</pre>
 *
 * @include /etc/license.txt
 */
public class Log
{	/** Send all log messages for the indicated package to
	 *	the console (System.err). The normal header (which holds the
	 *	timestamp and package name) is not printed.
	 */

	public static void toScreen( String packageName )
	{
		// Arrange for log output to be visible on the screen.

		Logger  log = Logger.getLogger(packageName);
		Handler h   = new ConsoleHandler();
		h.setLevel    ( Level.ALL );
		h.setFormatter( new Formatter()
						{	public String format(LogRecord r)
							{	return r.getMessage() + "\n";
							}
						}
					  );
		log.setUseParentHandlers(false);
		log.setLevel(Level.ALL);
		log.addHandler(h);
	}

	/** Turn off all logging for a particular package.
	 */
	public static void off( String packageName )
	{	Logger.getLogger(packageName).setLevel(Level.OFF);
	}

	/** Convenience for error messages, return a stack trace
	 *  for the indicated exception as a string. Let's you
	 *  put a stack trace into a "logged" message.
	 */

	public static String stackTraceAsString( Exception e )
	{	StringWriter out = new StringWriter();
		e.printStackTrace( new PrintWriter(out) );
		return out.toString();
	}
}
