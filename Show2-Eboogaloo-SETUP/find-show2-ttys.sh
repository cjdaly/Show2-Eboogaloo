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

SHOW2_SERIAL_ID_PREFIX="usb-Silicon_Labs_CP2104_USB_to_UART_Bridge_Controller_"

if [ ! -d "/dev/serial/by-id" ]; then
  if [ "$1" == "-verbose" ]; then
    echo "No Show2 devices found."
  fi
  exit 1
fi

show2_tty_found_count=0
show2_tty_lines="`ls -l /dev/serial/by-id/$SHOW2_SERIAL_ID_PREFIX* 2>/dev/null | cut -d ' ' -f12 | sort`"
for line in $show2_tty_lines
do
  show2_tty_path=`readlink -f /dev/serial/by-id/$line`
  if [ -c "$show2_tty_path" ]; then
    (( show2_tty_found_count += 1 ))
    if [ "$1" == "-verbose" ]; then
      echo "Found Show2 device at: $show2_tty_path"
    elif [ "$1" == "-first" ]; then
      echo $show2_tty_path
      break
    else
      echo $show2_tty_path
    fi
  fi
done

if [ "$1" == "-verbose" ]; then
  if [ $show2_tty_found_count -eq 0 ]; then
    echo "No Show2 devices found."
  fi
fi
