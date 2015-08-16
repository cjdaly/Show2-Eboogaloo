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

import net.locosoft.Show2Eboogaloo.Show2Commands;
import net.locosoft.Show2Eboogaloo.Show2Session;
import net.locosoft.fold.channel.fold.IFoldChannel;
import net.locosoft.fold.util.FoldUtil;
import net.locosoft.fold.util.MonitorThread;

public class Show2Feeder extends MonitorThread {

	private Show2Channel _channel;
	private Show2Session _session;

	public Show2Feeder(Show2Channel channel, Show2Session session) {
		_channel = channel;
		_session = session;
	}

	protected long getSleepTimePreCycle() {
		return 500;
	}

	protected long getSleepTimePostCycle() {
		return 1500;
	}

	private String _thingName;
	private String _foldUrlFragment;
	private long _startCount = -1;
	private int _cycleCount = 0;
	private FoldBanner _foldBanner = new FoldBanner();

	private boolean _restart = true;

	public boolean cycle() throws Exception {

		if (_thingName == null) {
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
			commands.addCommand("blt120");
			_session.enqueueCommands(commands);
			_restart = false;
			restarted = true;
			_cycleCount++;
		}

		Show2Commands foldBannerCommands = _foldBanner.step();
		if (foldBannerCommands == null) {
			_foldBanner.reset();
			_restart = true;
		} else {
			_session.enqueueCommands(foldBannerCommands);
			if (restarted) {
				updateStats();
				updateUrl();
			}
		}

		return true;
	}

	private void updateStats() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz3");
		commands.addCommand("xy0,2");
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

	private void updateUrl() {
		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz2");
		commands.addCommand("xy0,13");
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
