/***********************************************************************************************************************
*
*  A Hubitat Smart App for creating Envisalink Connection Device and Child Virtual Contact Zones
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
*  Name: Envisalink Integration
*  https://github.com/omayhemo/hubitat_envisalink
*
**************************************************************
**********	See Release Notes at the bottom ******************
***********************************************************************************************************************/

public static String version()      {  return "v0.14.0"  }
def boolean isDebug

definition(
    name: "Envisalink Integration",
    namespace: "dwb",
    singleInstance: true,
    author: "Doug Beard",
    description: "Integrate your DSC Alarm system, using Envisalink 3 or 4",
    category: "My Apps",
    iconUrl: "https://dougbeardrdiag.file.core.windows.net/icons/HomeSecIcon.PNG",
    iconX2Url: "https://dougbeardrdiag.file.core.windows.net/icons/HomeSecIcon.PNG",
    iconX3Url: "https://dougbeardrdiag.file.core.windows.net/icons/HomeSecIcon.PNG",
)

preferences {
	page(name: "mainPage", nextPage: "zoneMapsPage")
    page(name: "zoneMapsPage", nextPage: "mainPage")
    page(name: "defineZoneMap", nextPage: "zoneMapsPage")
    page(name: "editZoneMapPage", nextPage: "zoneMapsPage")
    page(name: "aboutPage", nextPage: "mainPage")
}



//App Pages/Views
def mainPage() {
    ifDebug("Showing mainPage")
	return dynamicPage(name: "mainPage", title: "", install: false, uninstall: true) {
        if(!state.envisalinkIntegrationInstalled && getChildDevices().size() == 0) {
            section("Define your Envisalink device") {
                clearStateVariables()
            	input "envisalinkName", "text", title: "Envisalink Name", required: true, multiple: false, defaultValue: "Envisalink", submitOnChange: false
                input "envisalinkIP", "text", title: "Envisalink IP Address", required: true, multiple: false, defaultValue: "", submitOnChange: false
                input "envisalinkPassword", "text", title: "Envisalink Password", required: true, multiple: false, defaultValue: "", submitOnChange: false
                input "envisalinkCode", "text", title: "Envisalink Disarm Code", required: true, multiple: false, defaultValue: "", submitOnChange: false
            }
        }
        else {
             state.enableHSM = enableHSM
                section("<h3>Safety Monitor</h3>") {
                    paragraph "Enabling Hubitat Safety Monitor Integration will tie your Envisalink state to the state of HSM.  Your Envisalink will receive the Arm Away, Arm Home and Disarm commands based on the HSM state. " 
                        input "enableHSM", "bool", title: "Enable HSM Integration", required: false, multiple: false, defaultValue: true, submitOnChange: true
               }
            
      
        
            
             section("") {
                href (name: "zoneMapsPage", title: "Zones", 
                description: "Create Virtual Contacts and Map them to Existing Zones in your Envisalink setup",
                page: "zoneMapsPage")	
            }
        }
        section("") {
            href (name: "aboutPage", title: "About", 
                  description: "Find out more about Envisalink Integration",
                  page: "aboutPage")	
        }
        section("") {
            input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: true, submitOnChange: true
        }

    }
}

def aboutPage() {
    ifDebug("Showing aboutPage")
    
	dynamicPage(name: "aboutPage", title: none){
        section("<h1>Introducing Envisalink Integration</h1>"){
            paragraph "An EyezOn EnvisaLink module allows you to upgrade your existing security system with IP control ... " +
                "Envisalink Integration connects to your Envisalink module via Telnet, using eyezon's public TPI."
            paragraph "Evisalink Integration automates installation and configuration of the Envisalink Connection Driver" +
                " as well as Virtual Contacts representing the dry contact zones configured in your DSC Alarm system."
            paragraph "You must have the Hubitat Envisalink Connection driver already installed before making use of Envisalink Integration application " +
                "https://github.com/omayhemo/hubitat_envisalink/blob/master/hubitat_envisalink_connection_driver.groovy"
            paragraph "Special Thanks to the Hubitat staff and cuboy29."
        }
	}
}

def zoneMapsPage() {
    ifDebug("Showing zoneMapsPage")
    if (getChildDevices().size() == 0 && !state.envisalinkIntegrationInstalled)
    {
        createEnvisalinkParentDevice()
    }

    if (state.creatingZone)
    {
        createZone()
    }
    /* Maybe coming soon?
    if (state.editingZone)
    {
     	editZone()   
    }
	
    ifDebug("DeleteZone? ${state.deleteZone}")
    if (state.deleteZone == true){
        section("Deleted Zone"){
            paragraph "Deleted your Zone"
            state.deleteZone == false
        }
        
    }
	*/
    
	dynamicPage(name: "zoneMapsPage", title: "", install: true, uninstall: false){
       
        section("<h1>Zone Maps</h1>"){
            paragraph "The partition of your Envisalink Installation should be divided into Zones.  Should the Zone be fitted with a dry contact, you can map the Zone to a Virtual Contact component device in Envisalink Integration "
            paragraph "You'll want to determine the Zone number as it is defined in " +
                "your Envisalink setup.  Define a new Zone in Envisalink Itegration and the application will then create a Virtual Contact sensor component device, which will report the state of the Envisalink Zone to which it is mapped. " +  
                " The Virtual Contact sensor components can be used in Rule Machine or any other application that is capable of leveraging the contact capability.  Envisalink is capable of 64 zones, your zone map should correspond to the numeric representation of that zone."
        }
        section("Create a Zone Map") {
            href (name: "createZoneMapPage", title: "Create a Zone Map", 
            description: "Create a Virtual Contact Zone",
            page: "defineZoneMap")	
        }
        
       section("<h2>Existing Zones</h2>"){
       		def deviceList = ""
            getChildDevice(state.EnvisalinkDNI).getChildDevices().each{
                href (name: "editZoneMapPage", title: "${it.label}", 
                description: "Zone Details",
                params: [deviceNetworkId: it.deviceNetworkId],
                page: "editZoneMapPage")	
            }	
		}
	}
}

def defineZoneMap() {
    ifDebug("Showing defineZoneMap")
    state.creatingZone = true;
	dynamicPage(name: "defineZoneMap", title: ""){
        section("<h1>Create a Zone Map</h1>"){
            paragraph "Create a Map for a zone in Envisalink"
           	input "zoneName", "text", title: "Zone Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
            input "zoneNumber", "number", title: "Which Zone 1-64", required: true, multiple: false, defaultValue: 001, range: "1..64", submitOnChange: false
            input "zoneType", "enum", title: "Motion or Contact Sensor?", required: true, multiple: false, 
                options: [[0:"Contact"],[1:"Motion"]]
        }
	}
}

def editZoneMapPage(message) {
    ifDebug("Showing editZoneMapPage")
    ifDebug("editing ${message.deviceNetworkId}")
    state.allZones = getChildDevice(state.EnvisalinkDNI).getChildDevices()
    def zoneDevice = getChildDevice(state.EnvisalinkDNI).getChildDevice(message.deviceNetworkId)
    def paragraphText = ""
    //Maybe Editing and Deleting coming soon
    //state.editingZone = true;
    state.editedZoneDNI = message.deviceNetworkId;
    if (zoneDevice.capabilities.find { item -> item.name.startsWith('Motion')}){
        paragraphText = paragraphText + "Motion Sensor\n"
    } 
    if (zoneDevice.capabilities.find { item -> item.name.startsWith('Contact')}){
        paragraphText = paragraphText + "Contact Sensor\n"
    } 
    dynamicPage(name: "editZoneMapPage", title: ""){
        section("<h1>${zoneDevice.label}</h1>"){
            paragraph paragraphText
        }
        /*
        section("<h2>Delete this Zone", title: ""){
         	input "deleteZoneBool", "bool", title: "Are you sure?", required: false, multiple: false, submitOnChange: true
        }
		*/
    }
    
    
}

def clearStateVariables(){
	ifDebug("Clearing State Variables just in case.")
    state.EnvisalinkDeviceName = null
    state.EnvisalinkIP = null
    state.EnvisalinkPassword = null
    state.EnvisalinkCode = null
}

def createEnvisalinkParentDevice(){
 	ifDebug("Creating Parent Envisalink Device")
    if (getChildDevice(state.EnvisalinkDNI) == null){
        state.EnvisalinkDNI = UUID.randomUUID().toString()
    	ifDebug("Setting state.EnvisalinkDNI ${state.EnvisalinkDNI}")
	    addChildDevice("dwb", "Envisalink Connection", state.EnvisalinkDNI, null, [name: envisalinkName, isComponent: true, label: envisalinkName])
        getChildDevice(state.EnvisalinkDNI).updateSetting("ip",[type:"text", value:envisalinkIP])
    	getChildDevice(state.EnvisalinkDNI).updateSetting("passwd",[type:"text", value:envisalinkPassword])
    	getChildDevice(state.EnvisalinkDNI).updateSetting("code",[type:"text", value:envisalinkCode])   
		castEnvisalinkDeviceStates()
    }
}

def castEnvisalinkDeviceStates(){
  	ifDebug("Casting to State Variables")
    state.EnvisalinkDeviceName = envisalinkName
    ifDebug("Setting state.EnvisalinkDeviceName ${state.EnvisalinkDeviceName}")
    state.EnvisalinkIP = envisalinkIP
    ifDebug("Setting state.EnvisalinkIP ${state.EnvisalinkIP}")
    state.EnvisalinkPassword = envisalinkPassword
    ifDebug("Setting state.EnvisalinkPassword ${state.EnvisalinkPassword}")
    state.EnvisalinkCode = envisalinkCode
    ifDebug("Setting state.EnvisalinkCode ${state.EnvisalinkCode}")
    if (getChildDevice(state.EnvisalinkDNI)){
        ifDebug("Found a Child Envisalink ${getChildDevice(state.EnvisalinkDNI).label}")
    }
    else{
     	ifDebug("Did not find a Parent Envisalink")
    }
}

def createZone(){
    ifDebug("Starting validation of ${zoneName} ZoneType: ${zoneType}")
    String formatted = String.format("%03d", zoneNumber)
    String deviceNetworkId
    
    if (zoneType == "0"){
	    deviceNetworkId = state.EnvisalinkDNI + "_" + formatted
    }else{
        deviceNetworkId = state.EnvisalinkDNI + "_M_" + formatted
    }
    ifDebug("Entered zoneNumber: ${zoneNumber} formatted as: ${formatted}")
    getChildDevice(state.EnvisalinkDNI).createZone([zoneName: zoneName, deviceNetworkId: deviceNetworkId, zoneType: zoneType])
    state.creatingZone = false;
}

def editZone(){
    def childZone = getChildDevice(state.EnvisalinkDNI).getChildDevice(state.editedZoneDNI);
	ifDebug("Starting validation of ${childZone.label}")
    ifDebug("Attempting rename of zone to ${newZoneName}")
    childZone.updateSetting("label",[type:"text", value:newZoneName])
   	newZoneName = null;
    state.editingZone = false
    state.editedZoneDNI = null;
    
}

private ifDebug(msg)     
{  
    if (msg && isDebug)  log.debug 'Envisalink Integration: ' + msg  
}

//General App Events
def installed() {
    state.envisalinkIntegrationInstalled = true
	initialize()
}

def updated() {
    
    
	unsubscribe()
	initialize()
}

def initialize() {
    state.creatingZone = false;
    subscribe(location, "hsmStatus", statusHandler)
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

def statusHandler(evt) {
    log.info "HSM Alert: $evt.value"
    
    if (evt.value && state.enableHSM)
    {
		ifDebug("HSM is enabled")
        switch(evt.value){
         	case "armedAway":
            ifDebug("Sending Arm Away")
            	getChildDevice(state.EnvisalinkDNI).ArmAway()
            	break
            case "armedHome":
            ifDebug("Sending Arm Home")
            	getChildDevice(state.EnvisalinkDNI).ArmHome()
            	break
            case "armedNight":
            ifDebug("Sending Arm Home")
            	getChildDevice(state.EnvisalinkDNI).ArmHome()
            	break
            case "disarmed":
            ifDebug("Sending Disarm")
            	getChildDevice(state.EnvisalinkDNI).Disarm()
            	break
        }
    }
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}


/***********************************************************************************************************************
* Version: 0.14.0
*	Added armedNight Handler
*
* Version: 0.13.0
*	Provided Debug switch for less logging if desired.
*	Moved this list to bottom of file
*
* Version: 0.12.1
*	HSM Integration Changes
*
* Version: 0.11.0
*	Added Motion Zone Capability
*
* Version: 0.10.0
* 
* 	Just the basics. 
*		Creates the Envisalink Connection device and allows definition of Zone Maps, creating virtual contact sensors as child components.
*		Allows subscription to HSM to mirror the state of HSM to Envisalink (ArmAway, ArmHome, Disarm)
*/


