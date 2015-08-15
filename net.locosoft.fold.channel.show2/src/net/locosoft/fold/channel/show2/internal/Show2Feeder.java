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
		return 2500;
	}

	private String _thingName;
	private String _foldUrlFragment;
	private int _count = 0;
	private FoldBanner _foldBanner = new FoldBanner();

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

		_count++;

		_session.enqueueCommands(_foldBanner.step());

		Show2Commands commands = new Show2Commands();

		commands.addCommand("siz3");
		commands.addCommand("xy0,2");
		commands.addCommand("fg6");
		commands.addCommand("+Thing: ");
		commands.addCommand("fg3");
		commands.addCommand("+" + _thingName);

		commands.addCommand("xy0,3");
		commands.addCommand("fg6");
		commands.addCommand("+cycle: ");
		commands.addCommand("fg3");
		commands.addCommand("+" + _count);

		commands.addCommand("siz2");
		commands.addCommand("xy0,13");
		if (_count % 8 == 0) {
			commands.addCommand("fg3");
		} else {
			commands.addCommand("fg7");
		}
		commands.addCommand("+http://");
		commands.addCommand("xy0,14");
		commands.addCommand("+" + _foldUrlFragment);

		_session.enqueueCommands(commands);

		return true;
	}

	private class FoldBanner {
		private Show2Commands[] _commandSequence = { //
		//
				s2c("cls", "blt64", "fg2", "+fold         "), //
				s2c("fg7", "+{", "fg2", "+fold        "), //
				s2c("fg7", "+-{", "fg2", "+fold       "), //
				s2c("fg7", "+--{", "fg2", "+fold      "), //
				s2c("fg7", "+--{", "fg2", "+ fold     "), //
				s2c("fg7", "+-{ ", "fg2", "+  fold    "), //
				s2c("fg7", "+{  ", "fg2", "+   fold   "), //
				s2c("fg2", "+       fold", "fg7", "+ }"), //
				s2c("fg2", "+        fold", "fg7", "+}"), //
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
				s2c("fg2", "+fold         "), //
		};

		private int _step = 0;

		private Show2Commands s2c(String... commands) {
			Show2Commands s2c = new Show2Commands("siz4", "xy0,0", "bg0");
			s2c.addCommands(commands);
			return s2c;
		}

		Show2Commands step() {
			Show2Commands commands = _commandSequence[_step++];
			if (_step == _commandSequence.length)
				_step = 0;
			return commands;
		}
	}

}
