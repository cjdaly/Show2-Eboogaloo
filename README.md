
## Show2-Eboogaloo

This is a love letter to a great part: the ODroid [Show2](http://odroid.com/dokuwiki/doku.php?id=en:odroidshow).  It's an Arduino with a screen! Does that sound familiar? Maybe you remember the MicroView [Kickstarter](https://www.kickstarter.com/projects/1516846343/microview-chip-sized-arduino-with-built-in-oled-di).  The MicroView is another Arduino with built-in display.  In fact the Show2 and the MicroView use the same (ATMEL ATmega328P) Arduino SOC. Here is a look at both of them in action:

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

(_Scoring system approved by her Majesty's royal auditors at Top Gear BBC._)

How do I express my love for the Show2? Read on...

### prereqs

- Linux machine connected to Show2 via USB
- Java 7 or 8 runtime

### initial setup

First make sure Linux tool dependencies are installed:

    sudo apt-get update
    sudo apt-get install ant git python-pip
    sudo pip install ino

Then get Show2-Eboogaloo bits and run setup script:

    cd
    git clone https://github.com/cjdaly/Show2-Eboogaloo.git
    cd Show2-Eboogaloo/Show2-Eboogaloo-SETUP
    ant -f setup.xml

### use with fold

...
