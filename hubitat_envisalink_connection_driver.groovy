/***********************************************************************************************************************
*
*  A Hubitat Driver using Telnet to connect to an Envisalink 3 or 4.
*  http://www.eyezon.com/
*
*  Copyright (C) 2018 Doug Beard
*
*  License:
*  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
*  General Public License as published by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
*  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
*  for more details.
*
*  You should have received a copy of the GNU General Public License along with this program.
*  If not, see <http://www.gnu.org/licenses/>.
*
*  Name: Envisalink Connection
*  https://github.com/omayhemo/hubitat_envisalink
*
*	Special Thanks to Chuck Swchwer, Mike Maxwell and cuboy29 
*	and to the entire Hubitat staff, you guys are killing it!
*
***********************************************************************************************************************/

public static String version()      {  return "v0.10.0"  }
private static boolean isDebug()    {  return true  }

/***********************************************************************************************************************
* Version: 0.11.0
*	Added Motion Zone Capability
*
* Version: 0.10.0
* 
* 	Just the basics. 
*		Establish Telnet with Envisalink
* 		Interpret Incoming Messages from Envisalink
*		Arm Away
*		Arm Home
*	 	Disarm
* 		Switch to Arm Away and Disarm
*	 	Status Report
*		Toggle Chime
*		Toggle Timestamp
*	 	Send Raw Message
*		Create Child Virtual Contacts (Zones)
*		Zone Open and Restored (Closed) Implementation
*		Error Codes
*		
*/

import groovy.transform.Field
import java.util.regex.Matcher
import java.util.regex.Pattern;

    
metadata {
	definition (name: "Envisalink Connection", namespace: "dwb", author: "Doug Beard") {
		capability "Initialize"
		capability "Telnet"
		capability "Alarm"
        capability "Switch"
        command "sendMsg", ["String"]
        command "StatusReport"
        command "ArmAway"
        command "ArmHome"
        //command "SoundAlarm"
        command "Disarm"
        command "ChimeToggle"
        command "ToggleTimeStamp"
        
        attribute   "Status", "string"
	}

	preferences {
		input("ip", "text", title: "IP Address", description: "ip", required: true)
        input("passwd", "text", title: "Password", description: "password", required: true)
        input("code", "text", title: "Code", description: "code", required: true)
	}
}

//general handlers
def installed() {
	log.warn "installed..."
    initialize()
   }

def updated() {
	log.info "updated..."
    log.debug "Configuring IP: ${ip}, Code: ${code}, Password: ${passwd}"
	initialize()
}

def initialize() {
    telnetClose() 
	try {
		//open telnet connection
		telnetConnect([termChars:[13,10]], ip, 4025, null, null)
		//give it a chance to start
		pauseExecution(1000)
		log.info "Telnet connection to Envisalink established"
        //poll()
	} catch(e) {
		log.debug "initialize error: ${e.message}"
	}
}

def uninstalled() {
    telnetClose() 
	removeChildDevices(getChildDevices())
}

//envisalink calls
def on(){
 	log.debug "On"
    ArmAway()
}

def off(){
 	log.debug "Off"
    Disarm()
}

def ArmAway(){
	log.debug "Arm Away"
    def message = "0301"
    sendMsg(message)
}

def ArmHome(){
 	log.debug "Arm Home"   
    def message = "0311"
    sendMsg(message)
}

def both(){
    log.debug "Both"   
 	siren()
    strobe()
}

def ChimeToggle(){
	log.debug "Chime Toggle Command"
    def message = "0711*4"
    sendMsg(message)
}

def Disarm(){
 	log.debug "Disarm"   
    def message = "0401" + code
    sendMsg(message)
}

def SoundAlarm(){
 	log.debug "Sound Alarm : NOT IMPLEMENTED"   
}

def siren(){
	log.debug "Siren : NOT IMPLEMENTED"
}

def StatusReport(){
	sendMsg("001")
}

def strobe(){
 	log.debug "Stobe : NOT IMPLEMENTED"   
}

def ToggleTimeStamp(){
    log.debug "Toggle Time Stamp"   
    def message
    if (state.timeStampOn)
    {
    	message = "0550"
    }
    else{
     	message = "0551" 
    }
    sendMsg(message)
}

//actions
def createZone(zoneInfo){
    log.debug "Creating ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
    if (zoneInfo.zoneType == 0)
    {
    	def newContact = addChildDevice("hubitat", "Virtual Contact Sensor", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: true, label: zoneInfo.zoneName])
    } else {
     	def newContact = addChildDevice("hubitat", "Virtual Motion Sensor", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: true, label: zoneInfo.zoneName])   
    }
}

def removeZone(zoneInfo){
    log.debug "Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
    deleteChildDevice(zoneInfo.deviceNetworkId)
}

private parse(String message) {
    log.debug "Parsing Incoming message: " + message
    message = preProcessMessage(message)
	log.debug "${tpiResponses[message.take(3) as int]}"
    
    if(message.startsWith("5053")) {
        sendLogin()
    }
    if(message.startsWith("502")) {
        systemError(message)
    }
     if(message.startsWith("609")) {
        zoneOpen(message)
    }
     if(message.startsWith("610")) {
         zoneClosed(message)
    }
     if(message.startsWith("650")) {
        sendEvent(name:"Status", value: "Partion Ready", displayed:false, isStateChange: true)
    }
    if(message.startsWith("651")) {
        sendEvent(name:"Status", value: "Partion NOT Ready", displayed:false, isStateChange: true)
    }
    if(message.startsWith("652")) {
        sendEvent(name:"Status", value: "Partion Armed", displayed:false, isStateChange: true)
    }
    if(message.startsWith("653")) {
        sendEvent(name:"Status", value: "Partition Ready - Force Arming Enabled", displayed:false, isStateChange: true)
    }
    if(message.startsWith("654")) {
        sendEvent(name:"Status", value: "Partion In Alarm", displayed:false, isStateChange: true)
    }
    if(message.startsWith("655")) {
        sendEvent(name:"Status", value: "Partion Disarmed", displayed:false, isStateChange: true)
    }   
    if(message.startsWith("656")) {
        sendEvent(name:"Status", value: "Exit Delay in Progress", displayed:false, isStateChange: true)
    }
    if(message.startsWith("657")) {
        sendEvent(name:"Status", value: "Entry Delay in Progress", displayed:false, isStateChange: true)
    }
    if(message.startsWith("658")) {
        sendEvent(name:"Status", value: "Keypad Lock-out", displayed:false, isStateChange: true)
    }
    if(message.startsWith("5050")) {
        
        log.debug "Password provided was incorrect"
    }
    if(message.startsWith("5051")) {
        
        log.debug "Loging Succesfull"
    }
    if(message.startsWith("5052")) {
        
        log.debug "Time out.  You did not send password within 10 seconds"
    }
    if(message.startsWith("502020")) {
        log.debug "API Command Syntax Error"
    }
}

def zoneOpen(message){
    def zoneDevice
    def substringCount = message.size() - 3
    zoneDevice = getChildDevice("${device.deviceNetworkId}_${message.substring(substringCount).take(3)}")
    log.debug zoneDevice
    if (zoneDevice){
        if (zoneDevice.capabilities.find { it.name > "Contact Sensor"}){
            log.debug "Contact Open"
            zoneDevice.open()
         }else {
            log.debug "Motion Active"
            zoneDevice.inactive()
        }
    }
    
}

def zoneClosed(message){
    def zoneDevice
    def substringCount = message.size() - 3
    zoneDevice = getChildDevice("${device.deviceNetworkId}_${message.substring(substringCount).take(3)}")
    if (zoneDevice){
    	log.debug zoneDevice
        if (zoneDevice.capabilities.find { it.name > "Contact Sensor"}){
	     	log.debug "Contact Closed"
    		zoneDevice.close()
        }else {
            log.debug "Motion Inactive"
            zoneDevice.active()
        }
    }
}

def systemError(message){
    def substringCount = message.size() - 3
    message = message.substring(substringCount).take(3).replaceAll('0', '')
    log.debug "System Error: ${message} - ${errorCodes.getAt(message)}"
}


//helpers
private checkTimeStamp(message){
    if (timeStampPattern.matcher(message)){
        log.debug "Time Stamp Found"
        	state.timeStampOn = true;
        	message = message.replaceAll(timeStampPattern, "")
        	log.debug "Time Stamp Remove ${message}"
        }
        else{
            state.timeStampOn = false;
        	log.debug "Time Stamp Not Found"
        }
    return message
}



private generateChksum(String cmdToSend){
		def cmdArray = cmdToSend.toCharArray()
        def cmdSum = 0
        cmdArray.each { cmdSum += (int)it }
        def chkSumStr = DataType.pack(cmdSum, 0x08)
        if(chkSumStr.length() > 2) chkSumStr = chkSumStr[-2..-1]
        cmdToSend += chkSumStr
    	cmdToSend
}

private preProcessMessage(message){
    log.debug "Preprocessing Message"
 	message = checkTimeStamp(message)
    //strip checksum
    message = message.take(message.size() - 2)
    log.debug "Stripping Checksum: ${message}"
    return message
}

def poll() {
    return new hubitat.device.HubAction("000", hubitat.device.Protocol.TELNET)
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

private sendLogin(){
    def cmdToSend = "005${passwd}"
    def cmdArray = cmdToSend.toCharArray()
    def cmdSum = 0
    cmdArray.each { cmdSum += (int)it }
    def chkSumStr = DataType.pack(cmdSum, 0x08)
    if(chkSumStr.length() > 2) chkSumStr = chkSumStr[-2..-1]
    cmdToSend += chkSumStr
    cmdToSend = cmdToSend + "\r\n"
    sendHubCommand(new hubitat.device.HubAction(cmdToSend, hubitat.device.Protocol.TELNET))   
}

def sendMsg(String s) {
    s = generateChksum(s)
    log.debug s
	return new hubitat.device.HubAction(s, hubitat.device.Protocol.TELNET)
}

//Telnet
def getReTry(Boolean inc){
	def reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status){
	log.info "telnetStatus- error: ${status}"
	if (status != "receive error: Stream is closed"){
		getReTry(true)
		log.error "Telnet connection dropped..."
		initialize()
	} else {
		log.warn "Telnet is restarting..."
	}
}



@Field Pattern timeStampPattern = ~/^\d{2}:\d{2}:\d{2} /   

@Field final Map 	errorCodes = [
    0: 	'No Error', 
    1: 	'Receive Buffer Overrun',
    2: 	'Receive Buffer Overflow',
    3: 	'Transmit Buffer Overflow',
    10: 'Keybus Transmit Buffer Overrun',
    11: 'Keybus Transmit Time Timeout',
    12: 'Keybus Transmit Mode Timeout', 
    13: 'Keybus Transmit Keystring Timeout',
    14: 'Keybus Interface Not Functioning (the TPI cannot communicate with the security system)',
    15: 'Keybus Busy (Attempting to Disarm or Arm with user code)',
    16: 'Keybus Busy – Lockout (The panel is currently in Keypad Lockout – too many disarm attempts)',
    17: 'Keybus Busy – Installers Mode (Panel is in installers mode, most functions are unavailable)',
    18: 'Keybus Busy – General Busy (The requested partition is busy)',
    20: 'API Command Syntax Error',
    21: 'API Command Partition Error (Requested Partition is out of bounds)',
    22: 'API Command Not Supported',
    23: 'API System Not Armed (sent in response to a disarm command)',
    24: 'API System Not Ready to Arm (system is either not-secure, in exit-delay, or already armed',
    25: 'API Command Invalid Length 26 API User Code not Required',
    27: 'API Invalid Characters in Command (no alpha characters are allowed except for checksum'
]

@Field final Map 	tpiResponses = [
    500: "Command accepted",
    501: "Command Error",
    502: "System Error",
    505: "Login Interaction",
    510: "Keypad LED State - partition 1",
    511: "Keypad LED FLASH state - partition 1",
    550: "Time - Date Broadcast",
    560: "Ring Detected",
    561: "Indoor Temp Broadcast",
    562: "Outdoor Temperature Broadcast",
    601: "Zone Alarm",
    602: "Zone Alarm Restore",
    603: "Zone Tamper",
	604: "Zone Tamper Restore",
    605: "Zone Fault",
    606: "Zone Fault Restore",
    609: "Zone Open",
    610: "Zone Restored",
    615: "Envisalink Zone Timer Dump",
    616: "Byp[assed Zones Bitfield Dump",
    620: "Duress Alarm",
    621: "F Key Alarm",
    622: "F Key Restored",
    623: "A Key Alarm",
    624: "A Key Restore",
    625: "P Key Alarm",
    626: "P Key Restore",
    631: "2-Wire Smoke Aux Alarm",
    632: "2-Wire Smoke Aux Restore",
    650: "Partition Ready",
    651: "Partition NOT Ready",
    652: "Partition Armed",
    653: "Partition Ready - Force Arming Enabled",
    654: "Partition In Alarm",
    655: "Partition Disarmed",
    656: "Exit Delay in Progress",
   	657: "Entry Delay in Progress",
    658: "Keypad Lock-out",
    659: "Partition Failed to Arm",
    660: "PFM Output is in Progress",
   	663: "Chime Enabled",
    664: "Chime Disabled",
    670: "Invalid Access Code",
    671: "Function Not Available",
    672: "Failure to Arm",
    673: "Partition is busy",
    674: "Systemn Arming Progress",
    680: "System in installers Mode",
    700: "User Closing",
    701: "Special Closing",
    702: "Partial Closing",
   	750: "User Opening",
    751: "Special Opening",
    800: "Panel Battery Trouble",
    801: "Panel Battery Trouble Restore",
    802: "Panel AC Trouble",
   	803: "Panel AC Restore",
    806: "System Bell Trouble",
    807: "System Bell Trouble Restore",
    814: "FTC Trouble",
    815: "FTC Trouble Restore",
    816: "Buffer Near Full",
    829: "General System Tamper",
    830: "General System Tamper Restore",
    840: "Trouble LED ON",
    841: "Trouble LED OFF",
    842: "Fire Trouble Alarm",
    843: "Fire Trouble Alarm Restore",
    849: "Verbose Trouble Status",
    900: "Code Required",
    912: "Command Output Pressed",
    921: "Master Code Required",
    922: "Installers Code Required"
]
