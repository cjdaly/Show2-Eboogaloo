
## Show2-Eboogaloo

This is a love letter to a great part: the ODroid [Show2](http://odroid.com/dokuwiki/doku.php?id=en:odroidshow).  It's an Arduino with a screen! Does that sound familiar? Maybe you remember the MicroView [Kickstarter](https://www.kickstarter.com/projects/1516846343/microview-chip-sized-arduino-with-built-in-oled-di).  The MicroView is another Arduino with built-in display.  In fact the Show2 and the MicroView use the same (ATMEL ATmega328P) Arduino SOC. Here is a look at both of them in action (attached to an ODroid-XU4):

![Show2 and MicroView](https://github.com/cjdaly/Show2-Eboogaloo/blob/master/images/Show2-and-MicroView.jpg?raw=true)

So does the MicroView suck? Not at all! The MicroView is a very well designed and constructed part ... but I'm not in love with MicroView (any more) ... it's an emotional thing - I can't explain it; but I'll try anyway:

<table>
<tr><th>Show2</th><th></th><th></th><th>MicroView</th><th></th></tr>
<tr><td>bigger is better - more pixels (320x200 vs. 64x48) and larger screen make for easier reading</td><td>+2</td><td></td><td>small is beautiful - it really depends on the application</td><td>+1</td></tr>
<tr><td>color</td><td>+64</td><td></td><td>monochrome</td><td>+1</td></tr>
<tr><td>Arduino driver sketch (apparently) written by Lady Ada</td><td>+7</td><td></td><td>Geek Ammo kickstarter team performed admirably</td><td>+6</td></tr>
<tr><td>price: about <a href='http://ameridroid.com/products/odroid-show-2'>$27</a></td><td>+3</td><td></td><td>Sparkfun has it for about <a href='https://www.sparkfun.com/products/12923'>$40</a>, but you probably also want <a href='https://www.sparkfun.com/products/12924'>this</a> this for $15 to go with.</td><td>-25</td></tr>
<tr><td>bare/unfinished part</td><td>0</td><td></td><td>superb style and finish</td><td>+12</td></tr>
<tr><td>breakout pins</td><td>+12</td><td></td><td>breakout pins</td><td>+16</td></tr>
<tr><td>buttons and LEDs</td><td>+6</td><td></td><td>(crickets)</td><td>0</td></tr>
<tr><td></td><td><b>96</b></td><td></td><td></td><td><b>11</b></td></tr>
</table>

(_Scoring system approved by Her Majesty's royal auditors at Top Gear BBC._)

How do I express my love for the Show2? Read on ... (safe for work!) ...

### prereqs

- Linux machine connected to Show2 via USB
- Java 7 or 8 runtime

### initial setup

First make sure Linux tool dependencies are installed:

    sudo apt-get update
    sudo apt-get install ant arduino git python-pip
    sudo pip install ino

Then get Show2-Eboogaloo bits and run setup script:

    cd
    git clone https://github.com/cjdaly/Show2-Eboogaloo.git
    cd Show2-Eboogaloo/Show2-Eboogaloo-SETUP
    ant -f setup.xml

Press the Show2 `RESET` button and note the message that is briefly displayed as the Show2 restarts. If you see the message `Hello ODROID-SHOW!` with version **`v1.6`**, this should be compatible with `Show2-Eboogaloo`.  Otherwise, you may need to skip to the sections below on Arduino sketches and upload either `show_main.ino` or `weatherThing.ino`.

And now you should be ready for...

![Hello World](https://github.com/cjdaly/Show2-Eboogaloo/blob/master/images/Show2-HelloWorld.jpg?raw=true)

### normal usage

The top level `Show2-Eboogaloo` directory contains several bash scripts.  The `find-show2-ttys.sh` script will search for connected Show2 devices and print their Linux device file path (e.g. `/dev/ttyUSB0`). The `show2.sh` script controls the Show2 by interpreting a series of command line arguments.  To produce an image like in the picture above:

    ./show2.sh siz4 fg3 +Hello fg6 '+ world!'

Note in this example how quotes are needed for the final argument, because it contains whitespace.  If the Show2 is associated with a non-default device file, use the `-T` argument to specify the correct one:

    ./show2.sh -T/dev/ttyUSB1 +hello fg3 '+ world'

Run `./show2.sh` with no arguments to see a usage message detailing the commands and syntax:

![show2.sh usage](https://github.com/cjdaly/Show2-Eboogaloo/blob/master/images/show2-usage.png?raw=true)

### Arduino action

The `setup.xml` script clones the hardkernel [ODROID-SHOW](https://github.com/hardkernel/ODROID-SHOW) repo to the home directory and does a `git checkout` to the version mentioned above.  Inside the `ODROID-SHOW` repo is the Arduino sketch (`~/ODROID-SHOW/show_main/show_main.ino`) that serves as the Show2 firmware.

The `setup.xml` script can copy the Arduino sketch into an [Ino](http://inotool.org/) project structure in the `Show2-Eboogaloo` project and build it using the Arduino tool chain. To do this:

    cd ~/Show2-Eboogaloo/Show2-Eboogaloo-SETUP
    ant -f setup.xml Show2-sketch.build

If you need to make changes to the Arduino sketch, the quick-and-dirty command line way to rebuild and deploy goes like this:

    cd ~/Show2-Eboogaloo/Show2-Eboogaloo-SETUP/show_main
    ino clean
    ino build
    ino upload -p /dev/ttyUSB0

Depending on the orientation of your Show2, it may make sense to switch the default text rotation setting in the firmware sketch.  To do this, edit the sketch file (`.../show_main/src/show_main.ino`) and look for this line:

    uint8_t rotation = 1;

Valid values are `0-3`.  Change to a different value and then repeat the Ino `clean`, `build`, `upload` sequence described above.

### weatherThing Arduino sketch

I have made some enhancements to the `show_main.ino` sketch, forking off a new sketch called `weatherThing.ino`.  This supports new commands to enable (`-WB`) and disable (`-wb`) dumping of sensor data from the [ODroid WeatherBoard](http://odroid.com/dokuwiki/doku.php?id=en:weather-board), if one is attached to the I2C headers on the Show2.

Also new are the `led` commands.  Use `ledB` to turn the blue LED on and `ledb` to turn it off.  Similar commands control the green and red LEDs.

![weatherThing](https://github.com/cjdaly/Show2-Eboogaloo/blob/master/images/Cali-weatherThing.jpg?raw=true)

To build and deploy the weatherThing sketch:

    cd ~/Show2-Eboogaloo/Show2-Eboogaloo-SETUP
    ant -f setup.xml weatherThing.build
    cd weatherThing/inoProject
    ino upload -p /dev/ttyUSB0

### fold extension channel

To use the Show2 with [fold](https://github.com/cjdaly/fold), copy the 2 jars in `~/Show2-Eboogaloo/Show2-Eboogaloo-SETUP/plugins` to the `~/fold-runtime/extend/plugins` directory and then run `./fold.sh extend` from the `fold-runtime` directory.

![fold channel](https://github.com/cjdaly/Show2-Eboogaloo/blob/master/images/Show2-fold-channel.jpg?raw=true)

