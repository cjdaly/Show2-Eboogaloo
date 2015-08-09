/*****************************************************************************
 * Copyright (c) 2015 Chris J Daly (github user cjdaly)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   cjdaly - initial API and implementation
 ****************************************************************************/

package net.locosoft.Show2Eboogaloo;

public class Show2Driver {

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 0) {
			usage("Show2-Eboogaloo: command line arguments");
			usage(" Display commands");
			usage("  +text - print text following the +");
			usage("  xyN,N - set cursor X and Y positions");
			usage("  cls - clear screen; reposition cursor to 0,0");
			usage("  fgN - set foreground color (N=0-7)");
			usage("  bgN - set background color (N=0-7)");
			usage("  xN - set cursor X position");
			usage("  yN - set cursor Y position");
			usage(" Meta commands");
			usage("  -T/dev/ttyUSBN - full path to Show2 device");
			usage("  -P/.../port_open - full path to port_open command");
			usage("  -dsN - delay N seconds");
			usage("  -dmsN - delay N milliseconds");
			usage("  -dDmsN - set the default write delay in millis");
			usage("  -echoN - echo on (N=1) or off (N=0)");
			usage(" Examples");
			usage("  show2 -/dev/ttyUSB0 fg2 +hello -ds1 fg3 '+ world'");
		} else {
			Show2Commands commands = new Show2Commands(args);
			commands.read();
			Show2Session session = new Show2Session(commands);
			session.init();
			session.eval();
		}
	}

	private static void usage(String line) {
		System.out.println(line);
	}

}
