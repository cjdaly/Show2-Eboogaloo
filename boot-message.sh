#!/bin/bash
####
# Copyright (c) 2015 Chris J Daly (github user cjdaly)
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#   cjdaly - initial API and implementation
####

# !!!!!
# Note: Run this script from /etc/rc.local to show info after reboot.
# !!!!!

eth_ipAddr=`ip -o -4 addr | grep -o -m 1 'eth\w\+\s\+inet\s\+[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+'`

ipAddr=`grep -o '[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+' <<< $eth_ipAddr`
echo "found IP addr: $ipAddr"

S2EB_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

`$S2EB_HOME/show2.sh fg7 siz4 '+IP Addr:' siz3 fg3 xy0,2 "+$ipAddr" xy0,3 fg4 +----------------- xy0,4 fg2 "$@"`

