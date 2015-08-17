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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class Show2Session {

	public Show2Session() {
		_keepYourselfAlive = true;
	}

	public Show2Session(Show2Commands commands) {
		_keepYourselfAlive = false;
		enqueueCommands(commands);
	}

	public boolean preprocess() {
		Show2Commands firstCommands = peekCommands();
		if (firstCommands != null) {
			return firstCommands.preprocess(this);
		} else {
			return true;
		}
	}

	public void start(boolean joinSessionThread) throws InterruptedException {
		CommandWriter writerThread = new CommandWriter();
		writerThread.start();
		if (joinSessionThread) {
			writerThread.join();
		}
	}

	public void stop() {
		_keepYourselfAlive = false;
	}

	// session state
	//
	private boolean _keepYourselfAlive;
	int _textWidth = 12;
	int _textHeight = 16;
	int _textSize = 2;
	int _screenRotation = 1;
	boolean _echo = false;
	long _postCommandDelay = 100;
	String _devicePath = "/dev/ttyUSB0";
	String _portOpenPath = System.getProperty("user.home")
			+ "/ODROID-SHOW/example/linux/port_open";

	private class CommandWriter extends Thread {
		public void run() {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(
					_devicePath, true))) {
				if (_portOpenPath != null) {
					File portOpenFile = new File(_portOpenPath);
					if (portOpenFile.exists() && portOpenFile.canExecute()) {
						Show2Util.execPortOpen(_portOpenPath, _devicePath);
					}
				}
				Thread.sleep(2000); // wait for Show2 reset

				do {
					Show2Commands commands = dequeueCommands();
					if (commands != null) {
						commands.read();
						commands.eval(writer, Show2Session.this);
					} else {
						Thread.sleep(100);
					}
				} while (_keepYourselfAlive);

			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	//
	//

	private LinkedList<Show2Commands> _commandQueue = new LinkedList<Show2Commands>();

	public synchronized void enqueueCommands(Show2Commands commands) {
		_commandQueue.add(commands);
	}

	private synchronized Show2Commands dequeueCommands() {
		if (_commandQueue.isEmpty())
			return null;
		else
			return _commandQueue.removeFirst();
	}

	private synchronized Show2Commands peekCommands() {
		if (_commandQueue.isEmpty())
			return null;
		else
			return _commandQueue.getFirst();
	}
}
