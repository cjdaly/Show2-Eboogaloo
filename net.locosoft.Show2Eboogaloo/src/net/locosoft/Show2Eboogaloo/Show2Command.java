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
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Show2Command {

	private Pattern _pattern;
	private String _command;

	public Show2Command(Pattern pattern, String command) {
		_pattern = pattern;
		_command = command;
	}

	protected Pattern getPattern() {
		return _pattern;
	}

	protected String getCommand() {
		return _command;
	}

	protected final boolean read() {
		Matcher matcher = _pattern.matcher(_command);
		if (matcher.find()) {
			return readParams(matcher);
		}
		return false;
	}

	protected boolean readParams(Matcher matcher) {
		return true;
	}

	public abstract void eval(BufferedWriter writer, Show2Session session)
			throws IOException, InterruptedException;

	protected String getEchoMessage() {
		return "";
	}

	//
	//

	private static final char _CHAR_ESCAPE = '\u001b';

	private static int getValue(String text, int defaultValue) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	//
	// Display Commands
	//

	public static class Text extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("[+](.*)");

		public Text(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_text = matcher.group(1);
			return true;
		}

		private String _text;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_text);
			writer.flush();
		}

		protected String getEchoMessage() {
			return "'" + _text + "'";
		}
	}

	public static class CLS extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("cls");

		public CLS(String command) {
			super(_Pattern, command);
		}

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[2J");
			writer.flush();
		}

	}

	public static class FG extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("fg(\\d)");

		public FG(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_fg = matcher.group(1).charAt(0);
			return true;
		}

		private char _fg;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[3");
			writer.write(_fg);
			writer.write("m");
			writer.flush();
		}
	}

	public static class BG extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("bg(\\d)");

		public BG(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_bg = matcher.group(1).charAt(0);
			return true;
		}

		private char _bg;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[4");
			writer.write(_bg);
			writer.write("m");
			writer.flush();
		}
	}

	public static class XY extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("xy(\\d+),(\\d+)");

		public XY(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_x = getValue(matcher.group(1), 1);
			_y = getValue(matcher.group(2), 1);
			return true;
		}

		private int _x;
		private int _y;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			writer.write(Integer.toString(_x));
			writer.write(";");
			writer.write(Integer.toString(_y));
			writer.write("H");
			writer.flush();
		}
	}

	//
	// Meta Commands
	//
	public static class DevicePath extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-(/dev/.*)");

		public DevicePath(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_path = matcher.group(1);
			return true;
		}

		private String _path;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (writer == null) {
				session._devicePath = _path;
			}
		}
	}

	public static class PortOpenPath extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("--(/.*)");

		public PortOpenPath(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_path = matcher.group(1);
			return true;
		}

		private String _path;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (writer == null) {
				session._portOpenPath = _path;
			}
		}
	}

	public static class DelaySeconds extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-ds(\\d+)");

		public DelaySeconds(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_delay = getValue(matcher.group(1), 0) * 1000;
			return true;
		}

		private long _delay;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			Thread.sleep(_delay);
		}

		protected String getEchoMessage() {
			return "delay " + _delay + " milliseconds";
		}
	}

	public static class DelayMilliseconds extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-dms(\\d+)");

		public DelayMilliseconds(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_delay = getValue(matcher.group(1), 0);
			return true;
		}

		private long _delay;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			Thread.sleep(_delay);
		}
	}

	public static class DefaultDelayMilliseconds extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-dDms(\\d+)");

		public DefaultDelayMilliseconds(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_delay = getValue(matcher.group(1), 500);
			return true;
		}

		private long _delay;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			session._postCommandDelay = _delay;
		}
	}

	public static class Echo extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-echo(\\d)");

		public Echo(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_echo = "1".equals(matcher.group(1));
			return true;
		}

		private boolean _echo;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			session._echo = _echo;
		}
	}

	//
	//
	//

	public static Show2Command readCommand(String command) {
		if ((command == null) || (command.length() < 2))
			return null;

		char c1 = command.charAt(0);
		if (c1 == '+') {
			return new Show2Command.Text(command);
		} else if (c1 == '-') {
			if (command.length() < 3)
				return null;
			switch (command.substring(0, 3)) {
			case "-/d":
				return new Show2Command.DevicePath(command);
			case "--/":
				return new Show2Command.DevicePath(command);
			case "-ds":
				return new Show2Command.DelaySeconds(command);
			case "-dm":
				return new Show2Command.DelayMilliseconds(command);
			case "-dD":
				return new Show2Command.DefaultDelayMilliseconds(command);
			case "-ec":
				return new Show2Command.Echo(command);
			default:
				return null;
			}
		} else {
			switch (command.substring(0, 2)) {
			case "cl":
				return new Show2Command.CLS(command);
			case "fg":
				return new Show2Command.FG(command);
			case "bg":
				return new Show2Command.BG(command);
			case "xy":
				return new Show2Command.XY(command);
			default:
				return null;
			}
		}
	}

}
