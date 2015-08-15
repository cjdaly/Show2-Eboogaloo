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

import net.locosoft.Show2Eboogaloo.Show2Session;
import net.locosoft.fold.channel.AbstractChannel;
import net.locosoft.fold.channel.IChannel;
import net.locosoft.fold.channel.show2.IShow2Channel;

public class Show2Channel extends AbstractChannel implements IShow2Channel {

	public Class<? extends IChannel> getChannelInterface() {
		return IShow2Channel.class;
	}

	private Show2Session _session;
	private Show2Feeder _feeder;

	public void init() {
		try {
			_session = new Show2Session();
			_session.start(false);
			_feeder = new Show2Feeder(this, _session);
			_feeder.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void fini() {
		if (_feeder != null) {
			_feeder.stop();
		}
		if (_session != null) {
			_session.stop();
		}
	}
}
