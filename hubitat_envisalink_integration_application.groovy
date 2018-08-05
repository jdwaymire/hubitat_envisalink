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
***********************************************************************************************************************/

public static String version()      {  return "v0.10.0"  }
private static boolean isDebug()    {  return true  }

/***********************************************************************************************************************
* Version: 0.10.0
* 
* 	Just the basics. 
*		Creates the Envisalink Connection device and allows definition of Zone Maps, creating virtual contact sensors as child components.
*		Allows subscription to HSM to mirror the state of HSM to Envisalink (ArmAway, ArmHome, Disarm)
*/
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
    log.debug "Showing mainPage"
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
    }
}

def aboutPage() {
    log.debug "Showing aboutPage"
    
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
    log.debug "Showing zoneMapsPage"
    if (getChildDevices().size() == 0 && !state.envisalinkIntegrationInstalled)
    {
        createEnvisalinkParentDevice()
    }

    if (state.creatingZone)
    {
        createZone()
    }
        
    if (state.editingZone)
    {
     	editZone()   
    }
    
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
        
/* 
			Maybe allow editing of zone names later
			getChildDevice(state.EnvisalinkDNI).getChildDevices().each{
                
                
                href (name: "editZoneMapPage", title: "${it.label}", 
                description: "Edit the name of the Zone",
                params: [deviceNetworkId: it.deviceNetworkId],
                page: "editZoneMapPage")	
			
            }	
*/
        
       section("<h2>Existing Zones</h2>"){
       		def deviceList = ""
            

           	getChildDevice(state.EnvisalinkDNI).getChildDevices().each{
                deviceList = deviceList + "${it.label}\n"
       		}
            
            paragraph deviceList        
		}
                
       
	}
}

def defineZoneMap() {
    log.debug "Showing defineZoneMap"
    state.creatingZone = true;
	dynamicPage(name: "defineZoneMap", title: ""){
        section("<h1>Create a Zone Map</h1>"){
            paragraph "Create a Map for a zone in Envisalink"
           	input "zoneName", "text", title: "Zone Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
            input "zoneNumber", "number", title: "Which Zone 1-64", required: true, multiple: false, defaultValue: 001, range: "1..64", submitOnChange: false
        }
	}
}

def editZoneMapPage(message) {
    log.debug "Showing editZoneMapPage"
    log.debug "editing ${message.deviceNetworkId}"
    def zoneDevice = getChildDevice(state.EnvisalinkDNI).getChildDevice(message.deviceNetworkId)
    state.editingZone = true;
    state.editedZoneDNI = message.deviceNetworkId;
	dynamicPage(name: "editZoneMapPage", title: ""){
        section("<h1>Create a Zone Map</h1>"){
            paragraph "Edit the name of this existing Zone"
           	input "newZoneName", "text", title: "Zone Name", required: true, multiple: false, defaultValue: "${zoneDevice.label}", submitOnChange: false
        }
	}
}

def clearStateVariables(){
	log.debug "Clearing State Variables just in case."
    state.EnvisalinkDeviceName = null
    state.EnvisalinkIP = null
    state.EnvisalinkPassword = null
    state.EnvisalinkCode = null
}

def createEnvisalinkParentDevice(){
 	log.debug "Creating Parent Envisalink Device"
    if (getChildDevice(state.EnvisalinkDNI) == null){
        state.EnvisalinkDNI = UUID.randomUUID().toString()
    	log.debug "Setting state.EnvisalinkDNI ${state.EnvisalinkDNI}"
	    addChildDevice("dwb", "Envisalink Connection", state.EnvisalinkDNI, null, [name: envisalinkName, isComponent: true, label: envisalinkName])
        getChildDevice(state.EnvisalinkDNI).updateSetting("ip",[type:"text", value:envisalinkIP])
    	getChildDevice(state.EnvisalinkDNI).updateSetting("passwd",[type:"text", value:envisalinkPassword])
    	getChildDevice(state.EnvisalinkDNI).updateSetting("code",[type:"text", value:envisalinkCode])   
		castEnvisalinkDeviceStates()
    }
}

def castEnvisalinkDeviceStates(){
  	log.debug "Casting to State Variables"
    state.EnvisalinkDeviceName = envisalinkName
    log.debug "Setting state.EnvisalinkDeviceName ${state.EnvisalinkDeviceName}"
    state.EnvisalinkIP = envisalinkIP
    log.debug "Setting state.EnvisalinkIP ${state.EnvisalinkIP}"
    state.EnvisalinkPassword = envisalinkPassword
    log.debug "Setting state.EnvisalinkPassword ${state.EnvisalinkPassword}"
    state.EnvisalinkCode = envisalinkCode
    log.debug "Setting state.EnvisalinkCode ${state.EnvisalinkCode}"
    if (getChildDevice(state.EnvisalinkDNI)){
        log.debug "Found a Child Envisalink ${getChildDevice(state.EnvisalinkDNI).label}"   
    }
    else{
     	log.debug "Did not find a Parent Envisalink"   
    }
}

def createZone(){
	log.debug "Starting validation of ${zoneName}"
    String formatted = String.format("%03d", zoneNumber)
    String deviceNetworkId = state.EnvisalinkDNI + "_" + formatted
    log.debug "Entered zoneNumber: ${zoneNumber} formatted as: ${formatted}"
    //state.EnvisalinkDevice.createZone([name: zoneName, deviceNetworkId: deviceNetworkId])
    getChildDevice(state.EnvisalinkDNI).createZone([zoneName: zoneName, deviceNetworkId: deviceNetworkId])
    state.creatingZone = false;
}

def editZone(){
    def childZone = getChildDevice(state.EnvisalinkDNI).getChildDevice(state.editedZoneDNI);
	log.debug "Starting validation of ${childZone.label}"
    log.debug "Attempting rename of zone to ${newZoneName}"
    childZone.updateSetting("label",[type:"text", value:newZoneName])
   	newZoneName = null;
    state.editingZone = false
    state.editedZoneDNI = null;
    
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
    log.info "HSM Alert: $evt.value" + (evt.value == "rule" ? ",  $evt.descriptionText" : "")
    
    if (evt.value && state.enableHSM)
    {

        switch(evt.value){
         	case "armedAway":
            	getChildDevice(state.EnvisalinkDNI).ArmAway()
            	break
            case "armedHome":
            	getChildDevice(state.EnvisalinkDNI).ArmHome()
            	break
            case "disarmed":
            	getChildDevice(state.EnvisalinkDNI).Disarm()
            	break
        }
    }
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}



