# Hubitat Envisalink Integration

Hubitat Envisalink Connection driver and Integration application, provides Hubitat integration with Envisalink.
Visit http://www.eyez-on.com/ for more information on Envisalink.

**Prerequisites**

* [Envisalink v3 or v4](https://github.com/omayhemo/hubitat_envisalink/blob/master/www.eyez-on.com)
* [Hubitat](https://github.com/omayhemo/hubitat_envisalink/blob/master/www.hubitat.com)
* The Envisalink system password that you setup when installing Envisalink. 
* Master Disarming Code
* The static IP address of the Envisalink device.
* Knowledge of how the Zones are laid out in your DSC Alarm system configuration. 

_At this time, I only support a single partition configuration._

## Installation

1. Install Envisalink Connection driver
2. Add Application Code and Load Envisalink Integration application
3. Using the Envisalink Integration application, configure your IP, Password and Code to Envisalink and your Zone layout


> KNOWN ISSUES

* If the HSM system is armed and disarmed very quickly in response to events, sometimes Envisalink continues to execute the Arm command and seemingly loses the disarm command.
