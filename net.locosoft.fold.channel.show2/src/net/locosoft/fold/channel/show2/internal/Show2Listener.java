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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.locosoft.Show2Eboogaloo.Show2Session;
import net.locosoft.fold.channel.vitals.IVitalsChannel;
import net.locosoft.fold.util.MonitorThread;

public class Show2Listener extends MonitorThread {

	private Show2Channel _channel;
	private Show2Session _session;

	private IVitalsChannel _vitalsChannel;

	private boolean _show2Reset = false;

	public Show2Listener(Show2Channel channel, Show2Session session) {
		_channel = channel;
		_session = session;

		_vitalsChannel = _channel.getChannelService().getChannel(
				IVitalsChannel.class);
	}

	protected long getSleepTimePreCycle() {
		return 100;
	}

	protected long getSleepTimePostCycle() {
		return 100;
	}

	private static final Pattern _SensorDataPattern = Pattern
			.compile("!! (\\w+):(.*) !!");

	public boolean cycle() throws Exception {
		String line = _session.pullOutputLine();
		if ((line != null) && (line.startsWith("!! "))) {
			Matcher matcher = _SensorDataPattern.matcher(line);
			if (matcher.matches()) {
				String vitalsId = "weatherThing." + matcher.group(1);

				HashMap<String, Object> vitalsData = new HashMap<String, Object>();

				String vitalsList = matcher.group(2);
				String[] vitalsArray = vitalsList.split(",");
				for (String vitalKeyValue : vitalsArray) {
					int eqPos = vitalKeyValue.indexOf('=');
					if (eqPos != -1) {
						String key = vitalKeyValue.substring(0, eqPos).trim();
						String value = vitalKeyValue.substring(eqPos + 1,
								vitalKeyValue.length()).trim();
						vitalsData.put(key, value);
					}
				}

				if ("weatherThing.Show2".equals(vitalsId)) {
					if ("now".equals(vitalsData.get("reset"))) {
						_show2Reset = true;
					}
				} else if (_show2Reset) {
					_vitalsChannel.recordStaticVitals(vitalsId, vitalsData);
				}
			}
		}
		return true;
	}
}
