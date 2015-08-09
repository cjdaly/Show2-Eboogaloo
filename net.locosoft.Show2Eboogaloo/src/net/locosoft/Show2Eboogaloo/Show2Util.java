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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Show2Util {

	public static void execPortOpen(String portOpenPath, String devicePath) {
		execCommand(portOpenPath + " " + devicePath, null);
	}

	public static int execCommand(String command, StringBuilder processOut) {
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

	public static class ProcessStreamReader extends Thread {
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
