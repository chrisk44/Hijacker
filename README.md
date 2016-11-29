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
* MDK3 Beacon Flooding
* MDK3 Authentication DoS for a specific network or to everyone
* Try to get a WPA handshake or gather IVs to crack a WEP network
* Statistics about access points (only encryption for now)
* See the manufacturer of a device (AP or station) from a OUI database (pulled from IEEE)
* See the signal power of devices and filter the ones that are closer to you
* Leave the app running in the background, optionally with a notification
* Copy commands or MAC addresses to clipboard, so you can run them in a terminal if something goes wrong
* Include the tools

##Future features:
* Let the user create custom commands to be ran on an access point or a client with one click.

##Installation:
Make sure:
* you are on Android 5+
* you are rooted. SuperSU is required. If you are on CM, install SuperSU
* have installed busybox (opened and installed the tools)
* have a firmware to support Monitor Mode on your wireless interface

Run ifconfig to find out the name of your monitor mode interface.

####Download the latest version [here](https://github.com/chrisk44/Hijacker/releases).

Run Hijacker and go to Settings. Here you can configure your interface name (the one you found using ifconfig), commands to run to enable or disable monitor mode (optional, leave them blank if you don't want to use them), directories for the tools (in case you installed them manually and they are not accessible without an absolute path) and many more. You can also test the configuration to make sure everything works correctly.
After configuring everything, click the "Test tools" option to run a test. If all the tools pass, you are good to go.

That's it, you are done. Make sure wifi is enabled and in monitor mode, and press back to go to the Home Screen. Airodump will start (give it a second) and you should see Access Points and Stations on your screen. You can click them to see the available actions for each one.

Keep in mind that Hijacker is just a GUI for these tools. The way it runs the tools is fairly simple, and if all the tests pass, then you should be getting the results you want. But also keep in mind that these are AUDITING tools. This means that they are used to TEST the integrity of your network, so there is a chance (and you should hope for it) that the attacks don't work on a network. It's not the app's fault, it's actually something to be happy about (given that this means that your network is safe). Also, deauthenticating clients from a network without specifying one is a bit unreliable. However, if an attack works when you type a command in a terminal, but not with the app, feel free to post here to resolve the issue. This app is still under development so bugs are to be expected.

##Troubleshooting:
First of all, if the app happens to crash at a random time, run it again and close it properly. This is to make sure that there are not any tools still running in the background, as this can cause battery drain. If it crashes during startup or exiting, open a terminal, run `ps | grep -e air -e mdk` and kill the processes you see.

Generally, when you first install the app you will need to setup the tools. You can do it through the app or you can do it manually (eg using the Nexmon app). Either way, if something doesn't seem to be working, go to Settings and click "Test tools". This options will try to run all the tools and will tell you what works and what doesn't. If something fails, you can click the "Copy test command" option and select the tool that fails. This will copy to your clipboard an exact copy of the command that the app uses to run the tool. You can then run that command in a terminal emulator to see what goes wrong.

When you are certain that there is problem with the app itself and not the tools installation, then open an issue here so I can try to fix it. Make sure to include exact steps to reproduce the problem and a logcat (having the logcat messages options enabled in settings).
