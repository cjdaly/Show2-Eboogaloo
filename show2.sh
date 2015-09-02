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

# http://www.ostricher.com/2014/10/the-right-way-to-get-the-directory-of-a-bash-script/
S2EB_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

java \
 -Dnet.locosoft.Show2Eboogaloo.homeDir=$S2EB_HOME \
 -jar $S2EB_HOME/Show2-Eboogaloo-SETUP/plugins/net.locosoft.Show2Eboogaloo_0.1.0.jar "$@"

