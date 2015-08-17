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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Show2Commands {

	public Show2Commands(String... commands) {
		addCommands(commands);
	}

	public void addCommandsFromFile(String commandFilePath)
			throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(
				commandFilePath))) {
			String command;
			while ((command = reader.readLine()) != null) {
				if (command.trim().isEmpty()) // skip blank lines
					continue;
				if (command.startsWith("#")) // skip comments
					continue;

				addCommand(command);
			}
		}
	}

	public void addCommands(String... commands) {
		for (String command : commands) {
			addCommand(command);
		}
	}

	public void addCommand(String command) {
		if (_readDone) {
			throw new IllegalStateException();
		}

		Show2Command show2Command = Show2Command.loadCommand(command);
		if (show2Command == null) {
			System.out.println("Invalid command: " + command);
			_invalidCommandCount++;
		} else {
			_commands.add(show2Command);
		}
	}

	boolean preprocess(Show2Session session) {
		boolean foundSessionCommand = false;
		for (Show2Command command : _commands) {
			if ((command instanceof Show2Command.DevicePath)
					|| (command instanceof Show2Command.PortOpenPath)
					|| (command instanceof Show2Command.Echo)
					|| (command instanceof Show2Command.Version)) {
				try {
					command.eval(null, session);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				foundSessionCommand = true;
			}
		}
		return foundSessionCommand;
	}

	private boolean _readDone = false;

	void read() {
		if (!_readDone) {
			for (Show2Command command : _commands) {
				if (!command.read())
					_malformedCommandCount++;
			}
			_readDone = true;
		}
	}

	void eval(BufferedWriter writer, Show2Session session) throws IOException,
			InterruptedException {
		for (Show2Command command : _commands) {
			if (session._echo) {
				System.out.println(command.getCommand() + "  "
						+ command.getEchoMessage());
			}
			command.eval(writer, session);
			Thread.sleep(session._postCommandDelay);
		}
	}

	private ArrayList<Show2Command> _commands = new ArrayList<Show2Command>();

	private int _invalidCommandCount = 0;

	public int getInvalidCommandCount() {
		return _invalidCommandCount;
	}

	private int _malformedCommandCount = 0;

	public int getMalformedCommandCount() {
		return _malformedCommandCount;
	}

}
