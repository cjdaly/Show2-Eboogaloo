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

import java.util.Map.Entry;
import java.util.TreeMap;

import net.locosoft.Show2Eboogaloo.Show2Command.Usage;

public class Show2Driver {

	public static void main(String[] args) throws InterruptedException {
		if (args.length == 0) {
			showUsage();
		} else {
			Show2Commands commands = new Show2Commands(args);
			commands.read();
			Show2Session session = new Show2Session(commands);
			if (session.preprocess())
				session.eval();
		}
	}

	private static void showUsage() {
		TreeMap<Integer, String> usageFragments = new TreeMap<Integer, String>();

		for (Class<?> commandClass : Show2Command.class.getDeclaredClasses()) {
			Usage usage = commandClass.getAnnotation(Show2Command.Usage.class);
			if (usage != null) {
				String fragment = usage.title();
				for (String line : usage.text()) {
					if (!fragment.isEmpty())
						fragment += "\n";
					fragment += "  ";
					fragment += line;
				}
				usageFragments.put(usage.order(), fragment);
			}
		}

		line("Show2 EBoogaloo - https://github.com/cjdaly/Show2-Eboogaloo");
		for (Entry<Integer, String> entry : usageFragments.entrySet()) {
			line(entry.getValue());
		}
		line("Examples");
		line("  ./show2.sh +hello fg3 '+ world'");
		line("  ./show2.sh -T/dev/ttyUSB0 +hello xy4,2 +world'");
	}

	private static void line(String text) {
		System.out.println(text);
	}
}
