# Hijacker

Hijacker is a Graphical User Interface for the wireless auditing tools airodump-ng, aireplay-ng and mdk3. It offers a simple and easy UI to use these tools without typing commands in a console and copy&pasting MAC addresses.

This application requires an android device with a wireless adapter that supports **Monitor Mode**. A few android devices do, but none of them natively. This means that you will need a custom firmware. Nexus 5 and any other device that uses the BCM4339 (and BCM4358 (although injection is not yet supported so no aireplay or mdk)) chipset will work with [Nexmon](https://github.com/seemoo-lab/nexmon). Also, devices that use BCM4330 can use [bcmon](http://bcmon.blogspot.gr/).

The required tools are included in the app. To install them go to Settings and click "Install Tools". This will install everything in the directory you select. If you have already installed them, you don't have to do anything. You can also have them at any directory you want and set the directories in Settings, though this might cause the wireless tools not being found by the aircrack-ng suite. The Nexmon driver and management utility is also included.

Root is also necessary, as these tools need root to work. If you don't grant root permissions to it, it hangs... for some reason... don't know why...

##Features:
* View a list of access points and stations (clients) around you (even hidden ones)
* View the activity of a network (by measuring beacons and data packets) and its clients
* Deauthenticate all the clients of a network
* Deauthenticate a specific client from the network it's connected
* MDK3 Beacon Flooding with custom SSID list
* MDK3 Authentication DoS for a specific network or to everyone
* Try to get a WPA handshake or gather IVs to crack a WEP network
* Statistics about access points (only encryption for now)
* See the manufacturer of a device (AP or station) from a OUI database (pulled from IEEE)
* See the signal power of devices and filter the ones that are closer to you
* Leave the app running in the background, optionally with a notification
* Copy commands or MAC addresses to clipboard, so you can run them in a terminal if something goes wrong
* Include the tools
* Reaver WPS cracking (pixie-dust attack using NetHunter chroot and external adapter)
* .cap files cracking with custom wordlist

##Future features:
* Let the user create custom commands to be ran on an access point or a client with one click.

##Installation:
Make sure:
* you are on Android 5+
* you are rooted. SuperSU is required. If you are on CM, install SuperSU
* have installed busybox (opened and installed the tools)
* have a firmware to support Monitor Mode on your wireless interface

####Download the latest version [here](https://github.com/chrisk44/Hijacker/releases).

When you run Hijacker for the first time, you will be asked whether you want to set up the tools or go to home screen. If you have installed your firmware and all the tools, you can just go to the home screen. Otherwise, click set up to install the tools. You can change the directories in which they will be installed, but I recommend that you leave them unchanged. The app will check what directories are available and select the best for you. Keep in mind that on some devices, installing files in /system might trigger an Android security feature and your system partition will be restored when you reboot. After installing the tools and the firmware (only Nexmon) you will land on the home screen and airodump will start. If you don't see any networks, make sure you have enabled your WiFi and it's in monitor mode. If you have a problem, go to settings and click "Test Tools". If they all pass, you probably don't have monitor mode enabled. If something fails, click "Copy test command" and select the tool that fails. A sample command will be copied to your clipboard so you can open a terminal, run it, and see what's wrong. 

Keep in mind that Hijacker is just a GUI for these tools. The way it runs the tools is fairly simple, and if all the tests pass and you are in monitor mode, then you should be getting the results you want. But also keep in mind that these are AUDITING tools. This means that they are used to TEST the integrity of your network, so there is a chance (and you should hope for it) that the attacks don't work on a network. It's not the app's fault, it's actually something to be happy about (given that this means that your network is safe). However, if an attack works when you type a command in a terminal, but not with the app, feel free to post here to resolve the issue. This app is still under development so bugs are to be expected.

##Troubleshooting:
First of all, if the app happens to crash at a random time, run it again and close it properly. This is to make sure that there are not any tools still running in the background, as this can cause battery drain. If it crashes during startup or exiting, open a terminal, run `ps | busybox grep -e air -e mdk` and kill the processes you see.

Most of the problems arise from the binaries not being installed (correctly or at all). If that's the case, go to settings, click "install tools", choose directories for binaries and the lib (libfakeioctl.so) and click install. If the directory for your binaries is included in PATH, then you don't have to do anything else. If it's not, the you need to adjust the absolute paths of the binaries, right below the "install tools" option. This might also cause problems (especially with mdk) since these programs require the wireless tools to be installed, and they won't find them if you install them anywhere other than the paths included in your PATH variable. If you don't know what the PATH variable is, then you probably shouldn't be using any of these programs.

If you are certain that there is problem with the app itself and not the tools installation, open an issue here so I can fix it. Make sure to include precise steps to reproduce the problem and a logcat (having the logcat messages options enabled in settings). If the app happens to crash, a new activity should start which will generate a report in /sdcard and give you the option to email it to me directly. I suggest you do that, and if you are worried about what will be sent you can check it out yourself, it's just a txt file and it will be sent as an email attachment to me.
