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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeatherBoardDemo extends Thread {

	private Show2Session _session;

	Show2Session getSession() {
		return _session;
	}

	int getRot() {
		return getSession()._screenRotation;
	}

	String getTitle() {
		String title = getSession()._weatherBoardDemoTitle;
		if ((title == null) || title.isEmpty())
			return "Thing1";
		else
			return title;
	}

	boolean isVertical() {
		int screenRotation = getRot();
		return (screenRotation == 0) || (screenRotation == 2);
	}

	private long _cycleCount = 0;
	private long _epicycleCount = 0;

	String _currentTimeText = null;
	String _currentDateText = null;
	String _currentDayText = null;

	boolean _clearSection0;

	int _currentSensorSection = 1;

	private LinkedList<DemoElement> _demoElements = new LinkedList<DemoElement>();;

	private static final Pattern _SensorDataPattern = Pattern
			.compile("!! (\\w+):(.*) !!");

	public WeatherBoardDemo(Show2Session session) {
		_session = session;
	}

	public void run() {
		try {
			while (true) {
				if ((_epicycleCount == 0) && (_cycleCount == 0)) {
					Thread.sleep(5000);

					Show2Commands commands = new Show2Commands();
					commands.addCommand("-WB");
					_session.enqueueCommands(commands);
				}

				if (_epicycleCount == 0) {
					_demoElements.add(new ResetDemoElement());
				}

				Thread.sleep(500);

				// process new sensor data
				String line = _session.pullOutputLine();
				if ((line != null) && (line.startsWith("!! "))) {
					Matcher matcher = _SensorDataPattern.matcher(line);
					if (matcher.matches()) {
						String type = matcher.group(1);
						String[] kvs = matcher.group(2).split(",");
						processSensorData(type, kvs);
					}
				}

				// update time / IP address
				if (_epicycleCount % 20 == 0) {
					if (_epicycleCount % 120 == 0) {
						_demoElements.add(new IPAddressDemoElement());
						_clearSection0 = true;
					} else {
						_demoElements.add(new TimeDemoElement(_clearSection0));
						_clearSection0 = false;
					}
				}

				// issue commands to Show2 for current demoElement
				if (!_demoElements.isEmpty()) {
					Show2Commands commands = new Show2Commands();
					DemoElement demoElement = _demoElements.getFirst();
					boolean done = demoElement.emitCommands(commands);
					_session.enqueueCommands(commands);
					if (done)
						_demoElements.removeFirst();
				}

				_epicycleCount++;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void processSensorData(String type, String[] kvs) {
		SensorDataDemoElement demoElement = null;

		if ("BMP180".equals(type)) {
			demoElement = new BMP180DemoElement();
		} else if ("Si1132".equals(type)) {
			demoElement = new Si1132DemoElement();
		} else if ("Si7020".equals(type)) {
			demoElement = new Si7020DemoElement();
		} else {
			// ???
		}

		if (demoElement != null) {
			demoElement.setKVs(kvs);
			demoElement.setSection(_currentSensorSection);
			if (_currentSensorSection == 1) {
				_currentSensorSection = 2;
			} else {
				_currentSensorSection = 1;
			}
			_demoElements.add(demoElement);
		}
	}

	abstract class DemoElement {
		protected int _step = 0;

		protected int _section = 0;

		void setSection(int section) {
			_section = section;
		}

		abstract boolean emitCommands(Show2Commands commands);

		int getStartRow() {
			int startRow = 0;
			switch (_section) {
			case 0:
				startRow = isVertical() ? 2 : 2;
				break;
			case 1:
				startRow = isVertical() ? 6 : 5;
				break;
			case 2:
				startRow = isVertical() ? 10 : 8;
				break;
			}
			return startRow;
		}

		void fgSelect(Show2Commands commands, boolean condition, int fgTrue,
				int fgFalse) {
			if (condition)
				commands.addCommand("fg" + fgTrue);
			else
				commands.addCommand("fg" + fgFalse);
		}

		void miniBanner(Show2Commands commands, int position) {
			miniBanner(commands, null, position);
		}

		void miniBanner(Show2Commands commands, String title, int position) {
			int row = 0;
			switch (position) {
			case 0:
				row = isVertical() ? 2 : 2;
				break;
			case 1:
				row = isVertical() ? 8 : 6;
				break;
			case 2:
				row = isVertical() ? 14 : 11;
				break;
			}

			commands.addCommand("siz2");
			commands.addCommand("xy0," + row);
			commands.addCommand("bg0");
			commands.addCommand("fg4");
			if (isVertical()) {
				commands.addCommand("+--------------------");
			} else {
				commands.addCommand("+--------------------------");
			}

			if (title != null) {
				commands.addCommand("xy2," + row);
				commands.addCommand("fg7");
				commands.addCommand("+" + title);
			}
		}

		void clearSection(Show2Commands commands, int position) {
			int startRow = 0;
			int rowCount = isVertical() ? 3 : 2;
			switch (position) {
			case 0:
				startRow = isVertical() ? 2 : 2;
				break;
			case 1:
				startRow = isVertical() ? 6 : 5;
				break;
			case 2:
				startRow = isVertical() ? 10 : 8;
				break;
			}

			commands.addCommand("siz3");
			commands.addCommand("bg0");
			for (int i = 0; i < rowCount; i++) {
				commands.addCommand("xy0," + (startRow + i));
				if (isVertical()) {
					commands.addCommand("+             ");
				} else {
					commands.addCommand("+                 ");
				}
			}
		}
	}

	class ResetDemoElement extends DemoElement {
		boolean emitCommands(Show2Commands commands) {
			switch (_step++) {
			case 0:
				commands.addCommand("rot" + getRot());
				commands.addCommand("cls");
				break;
			case 1:
				commands.addCommand("siz4");
				commands.addCommand("bg0");
				commands.addCommand("fg4");
				commands.addCommand("/0r/" + getTitle());
				break;
			case 2:
				commands.addCommand("siz4");
				commands.addCommand("bg0");
				commands.addCommand("fg6");
				commands.addCommand("/0r/" + getTitle());
				break;
			case 3:
				commands.addCommand("siz4");
				commands.addCommand("bg0");
				commands.addCommand("fg7");
				commands.addCommand("/0r/" + getTitle());
				break;
			}
			return _step == 4;
		}
	}

	private static final SimpleDateFormat _TimeFormat = new SimpleDateFormat(
			"hh:mm a");
	private static final SimpleDateFormat _DayFormat = new SimpleDateFormat(
			"EEE");
	private static final SimpleDateFormat _DateFormat = new SimpleDateFormat(
			"dd MMM yyyy");

	class TimeDemoElement extends DemoElement {

		TimeDemoElement(boolean clearSection) {
			_clearSection = clearSection;
		}

		boolean _clearSection;

		boolean _timeChanged;
		String _time;

		boolean _dateChanged;
		String _date;
		String _day;

		boolean emitCommands(Show2Commands commands) {

			if (_step == 0) {
				Date now = new Date();
				_time = _TimeFormat.format(now);
				_date = _DateFormat.format(now);
				_day = _DayFormat.format(now);

				if (!_time.equals(_currentTimeText)) {
					_currentTimeText = _time;
					_timeChanged = true;
				}
				if (!_date.equals(_currentDateText)) {
					_currentDateText = _date;
					_dateChanged = true;
				}
			}

			switch (_step++) {
			case 0:
				if (_clearSection) {
					clearSection(commands, 0);
					miniBanner(commands, "Time", 0);
				}
				commands.addCommand("siz3");
				commands.addCommand("bg0");
				if (isVertical()) {
					fgSelect(commands, _timeChanged, 3, 6);
					commands.addCommand("/2/" + _time);

					fgSelect(commands, _dateChanged, 3, 6);
					commands.addCommand("/3r/" + _day);
					commands.addCommand("/4r/" + _date);
				} else {
					fgSelect(commands, _timeChanged, 3, 6);
					commands.addCommand("xy0,2");
					commands.addCommand("+" + _time);

					fgSelect(commands, _dateChanged, 3, 6);
					commands.addCommand("/2r/" + _day);
					commands.addCommand("/3r/" + _date);
				}
				break;
			case 1:
				commands.addCommand("siz3");
				commands.addCommand("bg0");
				commands.addCommand("fg6");
				if (isVertical()) {
					commands.addCommand("/2/" + _time);
					commands.addCommand("/3r/" + _day);
					commands.addCommand("/4r/" + _date);
				} else {
					commands.addCommand("xy0,2");
					commands.addCommand("+" + _time);
					commands.addCommand("/2r/" + _day);
					commands.addCommand("/3r/" + _date);
				}
				break;
			}

			return _step == 2;
		}
	}

	private static final Pattern _ipPattern = Pattern
			.compile("\\d+:\\s+((eth|wlan)\\d+)\\s+inet\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)/");

	class IPAddressDemoElement extends DemoElement {
		boolean emitCommands(Show2Commands commands) {
			StringBuilder processOut = new StringBuilder();
			String ipCommand = "ip -o -4 addr";
			Show2Util.execCommand(ipCommand, processOut);

			miniBanner(commands, "IP Address", 0);
			clearSection(commands, 0);

			int startRow;
			if (isVertical()) {
				commands.addCommand("siz2");
				startRow = 4;
			} else {
				commands.addCommand("siz3");
				startRow = 2;
			}
			commands.addCommand("bg0");
			commands.addCommand("fg6");

			Matcher matcher = _ipPattern.matcher(processOut);

			for (int i = 0; i < 2; i++) {
				if (matcher.find()) {
					// String iface = matcher.group(1);
					String ipAddr = matcher.group(3);
					commands.addCommand("/" + (startRow + i) + "/" + ipAddr);
				} else {
					commands.addCommand("/" + (startRow + i) + "/ ");
				}
			}

			return true;
		}
	}

	abstract class SensorDataDemoElement extends DemoElement {
		HashMap<String, String> _kvMap = new HashMap<String, String>();

		String getValue(String key) {
			return _kvMap.get(key);
		}

		String getValue(String key, int truncateLength) {
			String value = _kvMap.get(key);
			if (value == null)
				return null;
			if (value.length() <= truncateLength)
				return value;
			return value.substring(0, truncateLength);
		}

		void setKVs(String[] kvs) {
			for (String kv : kvs) {
				int eqPos = kv.indexOf('=');
				if (eqPos != -1) {
					String key = kv.substring(0, eqPos).trim();
					String value = kv.substring(eqPos + 1, kv.length()).trim();
					_kvMap.put(key, value);
				}
			}
		}
	}

	class BMP180DemoElement extends SensorDataDemoElement {
		boolean emitCommands(Show2Commands commands) {

			int startRow = getStartRow();

			switch (_step++) {
			case 0:
				miniBanner(commands, "BMP180", _section);
				clearSection(commands, _section);

				commands.addCommand("siz3");
				commands.addCommand("bg0");
				if (isVertical()) {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+temp");
					commands.addCommand("/" + startRow + "r/baro");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+(C)");
					commands.addCommand("/" + (startRow + 1) + "r/(hPa)");

					commands.addCommand("fg3");
					commands.addCommand("xy0," + (startRow + 2));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 2) + "r/"
							+ getValue("pres", 6));
				} else {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+temp(C)");
					commands.addCommand("/" + startRow + "r/baro(hPa)");

					commands.addCommand("fg3");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 1) + "r/"
							+ getValue("pres", 6));
				}
				break;
			case 1:
				if (isVertical()) {
					commands.addCommand("fg6");
					commands.addCommand("xy0," + (startRow + 2));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 2) + "r/"
							+ getValue("pres", 6));
				} else {
					commands.addCommand("fg6");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 1) + "r/"
							+ getValue("pres", 6));
				}
				break;
			}
			return _step == 2;
		}
	}

	class Si1132DemoElement extends SensorDataDemoElement {
		boolean emitCommands(Show2Commands commands) {

			int startRow = getStartRow();

			switch (_step++) {
			case 0:
				miniBanner(commands, "Si1132", _section);
				clearSection(commands, _section);

				commands.addCommand("siz3");
				commands.addCommand("bg0");
				if (isVertical()) {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+    vis");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+(lx) IR");
					commands.addCommand("xy0," + (startRow + 2));
					commands.addCommand("+     UV");

					commands.addCommand("fg3");
					commands.addCommand("/8," + startRow + "/"
							+ getValue("vis", 5));
					commands.addCommand("/8," + (startRow + 1) + "/"
							+ getValue("IR", 5));
					commands.addCommand("/8," + (startRow + 2) + "/"
							+ getValue("UV", 5));
				} else {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+vis   IR (lx)  UV");

					commands.addCommand("fg3");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("vis", 5));
					commands.addCommand("xy6," + (startRow + 1));
					commands.addCommand("+" + getValue("IR", 5));
					commands.addCommand("xy12," + (startRow + 1));
					commands.addCommand("+" + getValue("UV", 5));
				}
				break;
			case 1:
				if (isVertical()) {
					commands.addCommand("fg6");
					commands.addCommand("/8," + startRow + "/"
							+ getValue("vis", 5));
					commands.addCommand("/8," + (startRow + 1) + "/"
							+ getValue("IR", 5));
					commands.addCommand("/8," + (startRow + 2) + "/"
							+ getValue("UV", 5));
				} else {
					commands.addCommand("fg6");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("vis", 5));
					commands.addCommand("xy6," + (startRow + 1));
					commands.addCommand("+" + getValue("IR", 5));
					commands.addCommand("xy12," + (startRow + 1));
					commands.addCommand("+" + getValue("UV", 5));
				}
				break;
			}
			return _step == 2;
		}
	}

	class Si7020DemoElement extends SensorDataDemoElement {
		boolean emitCommands(Show2Commands commands) {

			int startRow = getStartRow();

			switch (_step++) {
			case 0:
				miniBanner(commands, "Si7020", _section);
				clearSection(commands, _section);

				commands.addCommand("siz3");
				commands.addCommand("bg0");
				if (isVertical()) {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+temp");
					commands.addCommand("/" + startRow + "r/humi");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+(C)");
					commands.addCommand("/" + (startRow + 1) + "r/(%)");

					commands.addCommand("fg3");
					commands.addCommand("xy0," + (startRow + 2));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 2) + "r/"
							+ getValue("humi", 6));
				} else {
					commands.addCommand("fg5");
					commands.addCommand("xy0," + startRow);
					commands.addCommand("+temp(C)");
					commands.addCommand("/" + startRow + "r/humi(%)");

					commands.addCommand("fg3");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 1) + "r/"
							+ getValue("humi", 6));
				}
				break;
			case 1:
				if (isVertical()) {
					commands.addCommand("fg6");
					commands.addCommand("xy0," + (startRow + 2));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 2) + "r/"
							+ getValue("humi", 6));
				} else {
					commands.addCommand("fg6");
					commands.addCommand("xy0," + (startRow + 1));
					commands.addCommand("+" + getValue("temp", 5));
					commands.addCommand("/" + (startRow + 1) + "r/"
							+ getValue("humi", 6));
				}
				break;
			}
			return _step == 2;
		}
	}

}
