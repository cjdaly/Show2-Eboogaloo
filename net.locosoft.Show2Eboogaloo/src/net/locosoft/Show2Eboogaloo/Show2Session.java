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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

public class Show2Session {

	public Show2Session() {
		_keepYourselfAlive = true;
		_dumpToStdout = false;
	}

	public Show2Session(Show2Commands commands) {
		_keepYourselfAlive = false;
		_dumpToStdout = true;
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
		runPortOpen();

		if (_weatherBoardDemoMode) {
			_keepYourselfAlive = true;
			_dumpToStdout = false;
		}

		Show2Reader readerThread = new Show2Reader();
		readerThread.start();

		CommandWriter writerThread = new CommandWriter();
		writerThread.start();

		if (_weatherBoardDemoMode) {
			WeatherBoardDemo weatherBoardDemo = new WeatherBoardDemo(this);
			weatherBoardDemo.start();
		}

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
	private boolean _dumpToStdout;
	private boolean _writerDone = false;
	boolean _weatherBoardDemoMode = false;
	String _weatherBoardDemoTitle = null;
	int _textWidth = 12;
	int _textHeight = 16;
	int _textSize = 2;
	int _screenRotation = 1;
	long _postCommandDelay = 100;
	String _devicePath = "/dev/ttyUSB0";
	String _portOpenPath = System
			.getProperty("net.locosoft.Show2Eboogaloo.homeDir")
			+ "/Show2-Eboogaloo-SETUP/port_open/port_open";

	//
	//

	private void runPortOpen() {
		if (_portOpenPath != null) {
			File portOpenFile = new File(_portOpenPath);
			if (portOpenFile.exists() && portOpenFile.canExecute()) {
				Show2Util.execPortOpen(_portOpenPath, _devicePath);
			}
		}
	}

	//
	//

	private class CommandWriter extends Thread {
		public void run() {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(
					_devicePath, true))) {
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
			_writerDone = true;
		}
	}

	//
	//

	private LinkedList<Show2Commands> _commandQueue = new LinkedList<Show2Commands>();

	public synchronized int getCommandQueueSize() {
		return _commandQueue.size();
	}

	public synchronized void clearCommands() {
		_commandQueue.clear();
	}

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

	//
	//
	//

	private class Show2Reader extends Thread {
		public void run() {
			try (BufferedReader reader = new BufferedReader(new FileReader(
					_devicePath))) {
				Thread.sleep(2000); // wait for Show2 reset

				do {
					String line = reader.readLine();
					if ((line != null) && (!"..".equals(line))) {
						pushOutputLine(line);
					}
					Thread.sleep(100);
				} while (!_writerDone);
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private LinkedList<String> _show2OutputLines = new LinkedList<String>();

	public synchronized String pullOutputLine() {
		if (_show2OutputLines.isEmpty())
			return null;
		else
			return _show2OutputLines.removeFirst();
	}

	private synchronized void pushOutputLine(String line) {
		if (line == null)
			return;

		if (_dumpToStdout) {
			System.out.println(line);
		}

		_show2OutputLines.add(line);

		while (_show2OutputLines.size() > 64) {
			_show2OutputLines.removeFirst();
		}
	}
}
