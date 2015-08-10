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

public class Show2Session {

	private Show2Commands _commands;

	// TODO: hold session open for fold channel traffic
	boolean _keepAlive = false;

	boolean _echo = false;
	long _postCommandDelay = 500;

	String _devicePath;
	String _portOpenPath;

	public Show2Session(Show2Commands commands) {
		_commands = commands;

		_devicePath = "/dev/ttyUSB0";
		_portOpenPath = "/home/"
				+ System.getProperty("user.name")
				+ "/Show2-Eboogaloo/Show2-Eboogaloo-SETUP/ODROID-SHOW-master/example/linux/port_open";
	}

	public void init() {
		_commands.init(this);
	}

	public void eval() throws InterruptedException {
		CommandWriter writerThread = new CommandWriter();
		writerThread.start();
		writerThread.join();
	}

	private class CommandWriter extends Thread {
		public void run() {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(
					_devicePath, true))) {
				File portOpenFile = new File(_portOpenPath);
				if (portOpenFile.exists() && portOpenFile.canExecute()) {
					Show2Util.execPortOpen(_portOpenPath, _devicePath);
				}
				Thread.sleep(2000); // wait for Show2 reset
				_commands.eval(writer, Show2Session.this);
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
