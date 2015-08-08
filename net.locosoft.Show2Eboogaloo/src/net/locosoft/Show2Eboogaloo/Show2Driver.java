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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Show2Driver {

	public static final char CHAR_ESCAPE = '\u001b';

	public static void main(String[] args) {
		System.out.println("Hello!!");
		String portOpenCommand = null;
		if (args.length > 0) {
			portOpenCommand = args[0];
		}

		write("Hello world!", portOpenCommand);
	}

	private static void write(String message, String portOpenCommand) {
		String path = "/dev/ttyUSB0";
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path,
				true))) {
			if (portOpenCommand != null) {
				execCommand(portOpenCommand + " " + path, null);
			}
			writer.write(message);
			Thread.sleep(12000);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static int execCommand(String command, StringBuilder processOut) {
		int status = -1;
		if (processOut == null)
			processOut = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec(command);
			ProcessStreamReader reader = new ProcessStreamReader(
					process.getInputStream(), processOut);
			reader.start();
			status = process.waitFor();
			reader.join();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		return status;
	}

	private static class ProcessStreamReader extends Thread {
		private InputStream _inputStream;
		private StringBuilder _outputBuffer;

		ProcessStreamReader(InputStream inputStream, StringBuilder outputBuffer) {
			_inputStream = inputStream;
			_outputBuffer = outputBuffer;
		}

		public void run() {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(_inputStream))) {

				char[] buffer = new char[1024];

				int bytesRead;
				while ((bytesRead = reader.read(buffer)) != -1) {
					_outputBuffer.append(buffer, 0, bytesRead);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}
