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
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
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

	@Usage(order = 10, title = "Primary display commands", //
	text = "+hello - display text following the +")
	public static class Text extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("[+](.+)");

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
			int chunkLength = 8;
			for (int i = 0; i < _text.length(); i += chunkLength) {
				if (i > 0) {
					writer.flush();
					Thread.sleep(session._postCommandDelay);
				}
				int chunkEnd = Math.min(i + chunkLength, _text.length());
				writer.write(_text.substring(i, chunkEnd));
			}
			writer.flush();
		}

	}

	@Usage(order = 11, text = {
			"/N/text - display text on line N; pad with spaces to end of line",
			"/Nr/text - display text on line N, right justified",
			"/N,N/text - display text at char X,Y coordinates; pad with spaces to EOL" })
	public static class Textra extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("/(\\d+)(,(\\d+))?(r)?/(.+)");

		public Textra(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			int first = getValue(matcher.group(1), 0);
			int second = getValue(matcher.group(3), -1);
			if (second == -1) {
				_x = 0;
				_y = first;
			} else {
				_x = first;
				_y = second;
			}
			_rightJustify = "r".equals(matcher.group(4)) && (second == -1);
			_text = matcher.group(5);
			return true;
		}

		private int _x;
		private int _y;
		private boolean _rightJustify;
		private String _text;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			int lineLength = getLineLength(session);
			if (_x >= lineLength)
				return;

			Show2Commands commands = new Show2Commands();

			if (_rightJustify) {
				int x = lineLength - _text.length();
				commands.addCommand("xy" + x + "," + _y);
				commands.addCommand("+" + _text);
			} else {
				int textEnd = _x + _text.length();
				String text = _text;
				String pad = "";
				if (textEnd >= lineLength) {
					text = _text.substring(0, lineLength - _x);
				} else {
					char[] spaces = new char[lineLength - textEnd];
					Arrays.fill(spaces, ' ');
					pad = new String(spaces);
				}

				commands.addCommand("xy" + _x + "," + _y);
				commands.addCommand("+" + text);
				if (!pad.isEmpty()) {
					commands.addCommand("+" + pad);
				}
			}

			commands.read();
			commands.eval(writer, session);
		}

		private int getLineLength(Show2Session session) {
			switch (session._screenRotation) {
			case 0:
			case 2:
				return 240 / session._textWidth;
			case 1:
			case 3:
			default:
				return 320 / session._textWidth;
			}
		}

	}

	@Usage(order = 12, text = "cls - clear screen; reposition cursor to 0,0")
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
			Thread.sleep(200); // extra pause for thorough window cleaning
		}
	}

	public static abstract class HasXY extends Show2Command {
		public HasXY(Pattern pattern, String command) {
			super(pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_x = getValue(matcher.group(2), 1);
			_y = getValue(matcher.group(3), 1);
			_altEval = Character.isUpperCase(getCommand().charAt(0));
			return true;
		}

		protected boolean _altEval;
		protected int _x;
		protected int _y;

		protected void writeHelper(BufferedWriter writer, Show2Session session,
				int x, int y, char key) throws IOException,
				InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			writer.write(Integer.toString(x));
			writer.write(";");
			writer.write(Integer.toString(y));
			writer.write(key);
		}
	}

	@Usage(order = 13, text = {//
	"dotN,N - draw block with background color at character X,Y coordinates" //
	/* , "DOTN,N - draw dot with foreground color at PIXEL X,Y coordinates" *///
	})
	public static class DOT extends HasXY {
		private static final Pattern _Pattern = Pattern
				.compile("(dot|DOT)(\\d+),(\\d+)");

		public DOT(String command) {
			super(_Pattern, command);
		}

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (_altEval) {
				// TODO: why doesn't this work?
				writeHelper(writer, session, _x, _y, 'X');
				writer.flush();
			} else {
				writeHelper(writer, session, _x * session._textWidth, _y
						* session._textHeight, 'H');
				writer.write(' ');
				writer.flush();
			}
		}
	}

	//
	// cursor movement

	@Usage(order = 20, title = "Cursor positioning", text = {
			"xyN,N - move cursor to character X,Y coordinates",
			"XYN,N - move cursor to PIXEL X,Y coordinates" })
	public static class XY extends HasXY {
		private static final Pattern _Pattern = Pattern
				.compile("(xy|XY)(\\d+),(\\d+)");

		public XY(String command) {
			super(_Pattern, command);
		}

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (_altEval) {
				writeHelper(writer, session, _x, _y, 'H');
				writer.flush();
			} else {
				writeHelper(writer, session, _x * session._textWidth, _y
						* session._textHeight, 'H');
				writer.flush();
			}
		}
	}

	public static abstract class CursorMove extends Show2Command {
		public CursorMove(Pattern pattern, String command, char key) {
			super(pattern, command);
			_key = key;
		}

		private char _key;

		protected boolean readParams(Matcher matcher) {
			_count = getValue(matcher.group(2), 1);
			_altEval = Character.isUpperCase(getCommand().charAt(0));
			return true;
		}

		protected boolean _altEval;
		protected int _count;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			if (_altEval) {
				writer.write(Integer.toString(_count));
			} else {
				int offset = _key > 'B' ? session._textWidth
						: session._textHeight;
				writer.write(Integer.toString(_count * offset));
			}
			writer.write(_key);
			writer.flush();
		}
	}

	@Usage(order = 21, text = "up[N] or UP[N] - cursor up by N chars/PIXELs (default N=1)")
	public static class UP extends CursorMove {
		private static final Pattern _Pattern = Pattern
				.compile("(up|UP)(\\d*)");

		public UP(String command) {
			super(_Pattern, command, 'A');
		}
	}

	@Usage(order = 22, text = "dn[N] or DN[N] - cursor down by N chars/PIXELs")
	public static class DN extends CursorMove {
		private static final Pattern _Pattern = Pattern
				.compile("(dn|DN)(\\d*)");

		public DN(String command) {
			super(_Pattern, command, 'B');
		}
	}

	@Usage(order = 23, text = "rt[N] or RT[N] - cursor right by N chars/PIXELs")
	public static class RT extends CursorMove {
		private static final Pattern _Pattern = Pattern
				.compile("(rt|RT)(\\d*)");

		public RT(String command) {
			super(_Pattern, command, 'C');
		}
	}

	@Usage(order = 24, text = "lt[N] or LT[N] - cursor left by N chars/PIXELs")
	public static class LT extends CursorMove {
		private static final Pattern _Pattern = Pattern
				.compile("(lt|LT)(\\d*)");

		public LT(String command) {
			super(_Pattern, command, 'D');
		}
	}

	//
	// colors

	@Usage(order = 30, title = "Color control", text = "fgN - set foreground color (N=0-7)")
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

	@Usage(order = 31, text = { "bgN - set background color (N=0-7)",
			"(0=black, 1=red, 2=green, 3=yellow, 4=blue, 5=magenta, 6=cyan, 7=white)" })
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

	@Usage(order = 40, title = "Screen control", text = "rotN - set screen orientation (N=0-3)")
	public static class ROT extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("rot([0-3])");

		public ROT(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_rot = matcher.group(1).charAt(0);
			return true;
		}

		private char _rot;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			writer.write(_rot);
			writer.write("r");
			writer.flush();
			Thread.sleep(200);
			session._screenRotation = _rot - '0';
		}
	}

	@Usage(order = 41, text = "bltN - set backlight intensity (N=0-255)")
	public static class BLT extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("blt(\\d+)");

		public BLT(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_blt = getValue(matcher.group(1), 1);
			return true;
		}

		private int _blt;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			writer.write(Integer.toString(_blt));
			writer.write("q");
			writer.flush();
			Thread.sleep(200);
		}
	}

	@Usage(order = 42, text = "sizN - set text size (N>=2)")
	public static class SIZ extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("siz(\\d+)");

		public SIZ(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_siz = getValue(matcher.group(1), 2);
			return true;
		}

		private int _siz;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("[");
			writer.write(Integer.toString(_siz));
			writer.write("s");
			writer.flush();
			Thread.sleep(100);

			session._textWidth = 6 * _siz;
			session._textHeight = 8 * _siz;
			session._textSize = _siz;
		}
	}

	@Usage(order = 45, text = {
			"ledR or ledG or ledB - turn ON red, green or blue LED",
			"ledr or ledg or ledb - turn off red, green or blue LED" })
	public static class LED extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("led([rRgGbB])");

		public LED(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_led = matcher.group(1).charAt(0);
			return true;
		}

		private char _led;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("~");
			writer.write(_led);
			writer.flush();
			Thread.sleep(100);
		}
	}

	//
	// Meta Commands
	//

	@Usage(order = 50, title = "Delay commands", text = "-dsN - delay N seconds")
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

	}

	@Usage(order = 51, text = "-dmsN - delay N milliseconds")
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

	@Usage(order = 52, text = "-dDmsN - set the inter-command delay (default N=100 millis)")
	public static class DefaultDelayMilliseconds extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-dDms(\\d+)");

		public DefaultDelayMilliseconds(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_delay = getValue(matcher.group(1), 100);
			return true;
		}

		private long _delay;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			session._postCommandDelay = _delay;
		}
	}

	@Usage(order = 60, title = "Load and run commands from file", text = {
			"-F/full/path/to/command/file", "-F./relativePath/to/commandFile" })
	public static class CommandFile extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("-F([/\\.])(.*)");

		public CommandFile(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_path = matcher.group(1) + matcher.group(2);
			return true;
		}

		private String _path;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			File commandFile = new File(_path);
			if (commandFile.exists() && commandFile.canRead()) {
				Show2Commands commands = new Show2Commands();
				commands.addCommandsFromFile(_path);
				commands.read();
				commands.eval(writer, session);
			}
		}
	}

	@Usage(order = 70, title = "Configuration options", text = "-T/dev/ttyUSBN - full path to Show2 device (default: /dev/ttyUSB0)")
	public static class DevicePath extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-T(/dev/.*)");

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

	@Usage(order = 71, text = {
			"-P/.../port_open - full path to alternate port_open command",
			"-P./.../port_open - relative path to alternate port_open command",
			"-Px - skip invocation of port_open" })
	public static class PortOpenPath extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("-P(x|[/\\.].*)");

		public PortOpenPath(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_path = matcher.group(1);
			_unDefault = _path.equals("x");
			return true;
		}

		private boolean _unDefault;
		private String _path;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (writer == null) {
				if (_unDefault) {
					session._portOpenPath = null;
				} else {
					session._portOpenPath = _path;
				}
			}
		}
	}

	@Usage(order = 72, text = "-WB or -wb - WeatherBoard sensor dump ON or off")
	public static class WeatherBoard extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("-(WB|wb)");

		public WeatherBoard(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_enabled = "WB".equals(matcher.group(1));
			return true;
		}

		private boolean _enabled;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			writer.write(_CHAR_ESCAPE);
			writer.write("~");
			if (_enabled) {
				writer.write('W');
			} else {
				writer.write('w');
			}
			writer.flush();
			Thread.sleep(100);
		}
	}

	@Usage(order = 73, text = "-WDN[Title] - WeatherBoard demo (N=0-3 screen orientation)")
	public static class WeatherDemo extends Show2Command {
		private static final Pattern _Pattern = Pattern
				.compile("-WD([0-3])(.*)");

		public WeatherDemo(String command) {
			super(_Pattern, command);
		}

		protected boolean readParams(Matcher matcher) {
			_rot = matcher.group(1).charAt(0);
			_title = matcher.group(2);
			return true;
		}

		private char _rot;
		private String _title;

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (writer == null) {
				session._weatherBoardDemoMode = true;
			} else {
				session._screenRotation = _rot - '0';
				session._weatherBoardDemoTitle = _title;
			}
		}
	}

	@Usage(order = 80, text = "--v - print version information to stdout")
	public static class Version extends Show2Command {
		private static final Pattern _Pattern = Pattern.compile("--v");

		public Version(String command) {
			super(_Pattern, command);
		}

		public void eval(BufferedWriter writer, Show2Session session)
				throws IOException, InterruptedException {
			if (writer == null) {
				line("Show2-EBoogaloo version 0.1.0.17 <weatherThing.ino, ver: 1.6.x>");
			}
		}

		private static void line(String text) {
			System.out.println(text);
		}
	}

	//
	//
	//

	public static Show2Command loadCommand(String command) {
		if ((command == null) || (command.length() < 2))
			return null;

		char c1 = command.charAt(0);
		if (c1 == '+') {
			return new Show2Command.Text(command);
		} else if (c1 == '/') {
			return new Show2Command.Textra(command);
		} else if (c1 == '-') {
			if (command.length() < 3)
				return null;
			switch (command.substring(0, 3)) {
			case "-ds":
				return new Show2Command.DelaySeconds(command);
			case "-dm":
				return new Show2Command.DelayMilliseconds(command);
			case "-dD":
				return new Show2Command.DefaultDelayMilliseconds(command);
			case "-F/":
			case "-F.":
				return new Show2Command.CommandFile(command);
			case "-T/":
				return new Show2Command.DevicePath(command);
			case "-P/":
			case "-P.":
			case "-Px":
				return new Show2Command.PortOpenPath(command);
			case "-wb":
			case "-WB":
				return new Show2Command.WeatherBoard(command);
			case "-WD":
				return new Show2Command.WeatherDemo(command);
			case "--v":
				return new Show2Command.Version(command);
			default:
				return null;
			}
		} else {
			switch (command.substring(0, 2)) {
			case "cl":
				return new Show2Command.CLS(command);
			case "do":
			case "DO":
				return new Show2Command.DOT(command);
			case "up":
			case "UP":
				return new Show2Command.UP(command);
			case "dn":
			case "DN":
				return new Show2Command.DN(command);
			case "rt":
			case "RT":
				return new Show2Command.RT(command);
			case "lt":
			case "LT":
				return new Show2Command.LT(command);
			case "xy":
			case "XY":
				return new Show2Command.XY(command);
			case "fg":
				return new Show2Command.FG(command);
			case "bg":
				return new Show2Command.BG(command);
			case "ro":
				return new Show2Command.ROT(command);
			case "bl":
				return new Show2Command.BLT(command);
			case "si":
				return new Show2Command.SIZ(command);
			case "le":
				return new Show2Command.LED(command);
			default:
				return null;
			}
		}
	}

	//
	//

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface Usage {
		int order();

		String title() default "";

		String[] text();
	}

}
