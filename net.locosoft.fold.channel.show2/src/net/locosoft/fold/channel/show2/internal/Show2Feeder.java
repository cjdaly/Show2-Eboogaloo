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

package net.locosoft.fold.channel.show2.internal;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.eclipsesource.json.JsonValue;

import net.locosoft.Show2Eboogaloo.Show2Commands;
import net.locosoft.Show2Eboogaloo.Show2Session;
import net.locosoft.fold.channel.chatter.ChatterItemDetails;
import net.locosoft.fold.channel.chatter.IChatterChannel;
import net.locosoft.fold.channel.fold.IFoldChannel;
import net.locosoft.fold.channel.thing.IThingChannel;
import net.locosoft.fold.channel.vitals.IVitalsChannel;
import net.locosoft.fold.channel.vitals.Vital;
import net.locosoft.fold.channel.vitals.VitalsItemDetails;
import net.locosoft.fold.util.FoldUtil;
import net.locosoft.fold.util.MonitorThread;

public class Show2Feeder extends MonitorThread {

	private Show2Channel _channel;
	private Show2Session _session;

	private IChatterChannel _chatterChannel;
	private IVitalsChannel _vitalsChannel;
	private IThingChannel _thingChannel;

	public Show2Feeder(Show2Channel channel, Show2Session session) {
		_channel = channel;
		_session = session;

		_chatterChannel = _channel.getChannelService().getChannel(
				IChatterChannel.class);
		_vitalsChannel = _channel.getChannelService().getChannel(
				IVitalsChannel.class);
		_thingChannel = _channel.getChannelService().getChannel(
				IThingChannel.class);
	}

	protected long getSleepTimePreCycle() {
		return 1000;
	}

	protected long getSleepTimePostCycle() {
		return 1000;
	}

	private String _thingName;
	private String _foldUrlFragment;
	private long _startCount = -1;
	private int _cycleCount = 0;
	private int _epicycleCount = 0;
	private FoldBanner _foldBanner = new FoldBanner();

	private boolean _restart = true;

	public boolean cycle() throws Exception {
		while (_thingName == null) { // wait for ThingChannel init
			Thread.sleep(500);
			_thingName = _channel.getChannelService().getChannelData("thing",
					"name");
		}
		if (_foldUrlFragment == null) {
			String[] foldUrls = FoldUtil.getFoldUrls();
			if (foldUrls.length > 0) {
				_foldUrlFragment = foldUrls[0].substring(7);
			}
		}
		if (_startCount == -1) {
			IFoldChannel foldChannel = _channel.getChannelService().getChannel(
					IFoldChannel.class);
			_startCount = foldChannel.getStartCount();
		}

		boolean restarted = false;
		if (_restart) {
			Show2Commands commands = new Show2Commands();
			commands.addCommand("cls");
			commands.addCommand("blt64");

			String hasWeatherBoard = _thingChannel.getThingConfigProperties()
					.getProperty("Show2.WeatherBoard");
			if ("true".equals(hasWeatherBoard)) {
				commands.addCommand("-WB");
			}

			_session.enqueueCommands(commands);
			_restart = false;
			restarted = true;
			_cycleCount++;
			_epicycleCount = 0;
		} else {
			_epicycleCount++;
		}

		Show2Commands foldBannerCommands = _foldBanner.step();
		if (foldBannerCommands == null) {
			_foldBanner.reset();
			_restart = true;
		} else {
			_session.enqueueCommands(foldBannerCommands);
			if (restarted) {
				updateMiniBanner1();
				updateStats();
			}

			if (_epicycleCount % 6 == 0) {
				updateEvents();
			}

			if (restarted) {
				updateMiniBanner2();
				updateUrl();
			}
		}

		return true;
	}

	private void updateMiniBanner1() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz2");
		commands.addCommand("xy0,2");
		commands.addCommand("bg0");
		commands.addCommand("fg4");
		commands.addCommand("+--------------------------");

		_session.enqueueCommands(commands);
	}

	private void updateMiniBanner2() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz2");
		commands.addCommand("xy0,12");
		commands.addCommand("bg0");
		commands.addCommand("fg4");
		commands.addCommand("+--------------------------");

		_session.enqueueCommands(commands);
	}

	private void updateStats() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz3");
		commands.addCommand("xy0,2");
		commands.addCommand("bg0");
		commands.addCommand("fg6");
		commands.addCommand("+Thing: ");
		commands.addCommand("fg3");
		commands.addCommand("+" + _thingName);

		commands.addCommand("xy0,3");
		commands.addCommand("fg6");
		commands.addCommand("+start: ");
		commands.addCommand("fg3");
		commands.addCommand("+" + _startCount);

		commands.addCommand("xy0,4");
		commands.addCommand("fg6");
		commands.addCommand("+cycle: ");
		commands.addCommand("fg3");
		commands.addCommand("+" + _cycleCount);

		_session.enqueueCommands(commands);
	}

	private void updateEvents() {

		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz3");
		commands.addCommand("xy0,5");
		commands.addCommand("bg0");
		commands.addCommand("fg4");
		commands.addCommand("+--");
		commands.addCommand("fg5");
		commands.addCommand("+/");

		switch (_epicycleCount / 6) {
		case 1: // vitals
		case 3: // vitals
			commands.addCommand("fg6");
			commands.addCommand("/3,5/Vitals");
			commands.addCommand("fg5");

			VitalsItemDetails vitalsItemDetails = _vitalsChannel
					.getVitalsItemDetails(-1);
			if (vitalsItemDetails == null) {
				commands.addCommand("/5r/???");
				commands.addCommand("fg3");
				commands.addCommand("/6/???");
				commands.addCommand("/7/???");
			} else {
				commands.addCommand("/5r/" + vitalsItemDetails.getOrdinal());
				commands.addCommand("fg3");
				Vital[] vitals = vitalsItemDetails.getVitals();
				if (vitals.length > 0) {
					Vital vital = vitals[0];
					String name = vital.Name == null ? vital.Id : vital.Name;
					commands.addCommand("/6/" + name);
					JsonValue jsonValue = vitalsItemDetails.getValue(vital.Id);
					commands.addCommand("/7/" + jsonValue.toString());
					commands.addCommand("fg4");
					commands.addCommand("/7r/" + vital.Units);
				} else {
					commands.addCommand("/6/???");
					commands.addCommand("/7/???");
				}
			}
			break;
		case 2: // chatter
			commands.addCommand("fg6");
			commands.addCommand("/3,5/Chatter");
			commands.addCommand("fg5");

			ChatterItemDetails chatterItemDetails = _chatterChannel
					.getChatterItemDetails(-1);
			if (chatterItemDetails == null) {
				commands.addCommand("/5r/???");
				commands.addCommand("fg3");
				commands.addCommand("/6/???");
				commands.addCommand("/7/???");
			} else {
				commands.addCommand("/5r/" + chatterItemDetails.getOrdinal());
				commands.addCommand("fg3");
				commands.addCommand("/6/" + chatterItemDetails.getCategory());
				commands.addCommand("/7/" + chatterItemDetails.getMessage());
			}
			break;
		default: // time
			commands.addCommand("fg6");
			commands.addCommand("/3,5/Time");

			Date now = new Date();
			SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a, EEE");
			String time = timeFormat.format(now);
			commands.addCommand("fg3");
			commands.addCommand("/6/ " + time);

			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String date = dateFormat.format(now);
			commands.addCommand("/7/ " + date);

			break;
		}

		_session.enqueueCommands(commands);
	}

	private void updateUrl() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz2");
		commands.addCommand("xy0,13");
		commands.addCommand("bg0");
		commands.addCommand("fg7");
		commands.addCommand("+http://");
		commands.addCommand("xy0,14");
		commands.addCommand("+" + _foldUrlFragment);

		_session.enqueueCommands(commands);
	}

	private class FoldBanner {
		private Show2Commands[] _commandSequence = { //
		//
				s2c("fg2", "+fold         "), //
				s2c("fg7", "+{", "fg2", "+fold        "), //
				s2c("fg7", "+-{", "fg2", "+fold       "), //
				s2c("fg7", "+--{", "fg2", "+fold      "), //
				s2c("fg7", "+--{", "fg2", "+ fold     "), //
				s2c("fg7", "+-{ ", "fg2", "+  fold    "), //
				s2c("fg7", "+{  ", "fg2", "+   fold   "), //
				s2c("fg2", "+       fold", "fg7", "+ }"), //
				s2c("fg2", "+        fold", "fg7", "+}"), //
				s2c("fg2", "+         fold"), //
				s2c("fg0", "+         ", "bg2", "+fold"), //
				s2c("fg2", "+         fold"), //
				s2c("fg2", "+        fold", "fg7", "+}"), //
				s2c("fg2", "+       fold", "fg7", "+}-"), //
				s2c("fg2", "+      fold", "fg7", "+}--"), //
				s2c("fg2", "+     fold ", "fg7", "+}--"), //
				s2c("fg2", "+    fold  ", "fg7", "+ }-"), //
				s2c("fg2", "+   fold   ", "fg7", "+  }"), //
				s2c("fg7", "+{", "fg2", "+ fold       "), //
				s2c("fg7", "+{", "fg2", "+fold        "), //
				s2c("fg2", "+fold         "), //
				s2c("bg2", "fg0", "+fold", "bg0", "+         "), //
				s2c("bg2", "fg4", "+fold", "bg0", "+         "), //
				s2c("fg2", "+fold         "), //
		};

		private int _step = 0;

		private Show2Commands s2c(String... commands) {
			Show2Commands s2c = new Show2Commands("siz4", "xy0,0", "bg0");
			s2c.addCommands(commands);
			return s2c;
		}

		void reset() {
			_step = 0;
		}

		Show2Commands step() {
			if (_step == _commandSequence.length)
				return null;
			else
				return _commandSequence[_step++];
		}
	}

}
