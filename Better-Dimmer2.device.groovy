/* Better-Dimmer2.device.groovy
 *
 * Variation of the stock SmartThings "Dimmer-Switch"
 *
 * Device type orients the slider to horizontal versus vertical and
 * adds increment up and down buttons. Hardcoded to increment by 10.
 *
 * I also added the ability to use photo/picture as background.
 *
 * To use you must have IDE access on your acount to install this custom device.
 *
 * Replace the starter code with this code and save the file. Go into
 * "My devices" and select the dimmer you want to change. Select "Edit"
 * and then change the "Type" to use this device type.
 *
 * Happy Hacking!
 *
 * twack@wackware.net
 * 20140208
 *
*/


metadata {
	// Automatically generated. Make future change here.
	definition (name: "Better Dimmer", author: "todd@wackford.net") {
		capability "Refresh"
        capability "Switch"
		capability "Switch Level"
		capability "Polling"
        
        attribute "stepsize", "string"

		command "levelDown"
		command "levelUp"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}
    
    
    preferences {
		input "stepsize", "number", title: "Step Size", description: "Dimmer Step Size", defaultValue: 10, required: false, displayDuringSetup: true
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#79b821"
			state "turningOff", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
		standardTile("indicator", "device.indicatorStatus", inactiveLabel: false, decoration: "flat") {
			state "when off", action:"indicator.indicatorWhenOn", icon:"st.indicators.lit-when-off"
			state "when on", action:"indicator.indicatorNever", icon:"st.indicators.lit-when-on"
			state "never", action:"indicator.indicatorWhenOff", icon:"st.indicators.never-lit"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        valueTile("lValue", "device.level", inactiveLabel: true, height:1, width:1, decoration: "flat") {
            state "levelValue", label:'${currentValue}%', unit:"", backgroundColor: "#53a7c0"
        }

        standardTile("lUp", "device.switchLevel", inactiveLabel: false,decoration: "flat", canChangeIcon: false) {
                        state "default", action:"levelUp", icon:"st.illuminance.illuminance.bright"
        }
        standardTile("lDown", "device.switchLevel", inactiveLabel: false,decoration: "flat", canChangeIcon: false) {
                        state "default", action:"levelDown", icon:"st.illuminance.illuminance.light"
        }

		main(["switch"])
		details(["switch", "lUp", "lDown", "levelSliderControl", "lValue" , "refresh", "indicator","preferences"])
	}
}

def initialize() {   
	if ( !settings.stepsize )
    	state.stepsize = 10
    else
		state.stepsize = settings.stepsize
    
    if (!device.currentValue("level"))
    	setLevel(100)
}

def levelUp(){
	if ( !state.stepsize ) {
    	initialize()
        log.info "initialized on first up"
    } else {
    	state.stepsize = settings.stepsize
    }
    
        
    def thisStep = state.stepsize as float
    int nextLevel = device.currentValue("level") + thisStep
    
    if( nextLevel > 100){
    	nextLevel = 100
    }
    
    log.debug "Setting dimmer level up to: ${nextLevel}"
    delayBetween ([zwave.basicV1.basicSet(value: nextLevel).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 1500)

	//send event since most zwave devices don't publish the level event until polled
    sendEvent(name:"level",value:nextLevel)
    sendEvent(name:"switch.setLevel",value:nextLevel)
}

def levelDown(){
	if ( !state.stepsize ) {
    	initialize()
        log.info "initialized on first up"
    } else {
    	state.stepsize = settings.stepsize
    }
    
    def thisStep = state.stepsize as float
    int nextLevel = device.currentValue("level") - thisStep
    
    if (nextLevel < 1){
    	nextLevel = 0
    }
    
    if (nextLevel == 0){
    	off()
    }
    else
    {
    	log.debug "Setting dimmer level down to: ${nextLevel}"
        delayBetween ([zwave.basicV1.basicSet(value: nextLevel).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 1500)
    
    	//send event since most zwave devices don't publish the level event until polled
    	sendEvent(name:"level",value:nextLevel)
        sendEvent(name:"switch.setLevel",value:nextLevel)
    }
}

def parse(String description) {
	def item1 = [
		canBeCurrentState: false,
		linkText: getLinkText(device),
		isStateChange: false,
		displayed: false,
		descriptionText: description,
		value:  description
	]
	def result
	def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
	if (cmd) {
		result = createEvent(cmd, item1)
	}
	else {
		item1.displayed = displayed(description, item1.isStateChange)
		result = [item1]
	}
	log.debug "Parse returned ${result?.descriptionText}"
	result
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	result[0].descriptionText = "${item1.linkText} is ${item1.value}"
	result[0].handlerName = cmd.value ? "statusOn" : "statusOff"
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "digital"
	}
	result
}

def doCreateEvent(physicalgraph.zwave.Command cmd, Map item1) {
	def result = [item1]
	item1.name = "switch"
	item1.value = cmd.value ? "on" : "off"
	item1.handlerName = item1.value
	item1.descriptionText = "${item1.linkText} was turned ${item1.value}"
	item1.canBeCurrentState = true
	item1.isStateChange = isStateChange(device, item1.name, item1.value)
	item1.displayed = item1.isStateChange

	if (cmd.value >= 5) {
		def item2 = new LinkedHashMap(item1)
		item2.name = "level"
		item2.value = cmd.value as String
		item2.unit = "%"
		item2.descriptionText = "${item1.linkText} dimmed ${item2.value} %"
		item2.canBeCurrentState = true
		item2.isStateChange = isStateChange(device, item2.name, item2.value)
		item2.displayed = false
		result << item2
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	def value = "when off"
	if (cmd.configurationValue[0] == 1) {value = "when on"}
	if (cmd.configurationValue[0] == 2) {value = "never"}
	[name: "indicatorStatus", value: value, display: false]
}

def createEvent(physicalgraph.zwave.Command cmd,  Map map) {
	// Handles any Z-Wave commands we aren't interested in
	log.debug "UNHANDLED COMMAND $cmd"
}

def on() {
	log.info "on"
	delayBetween([zwave.basicV1.basicSet(value: 0xFF).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def off() {
	delayBetween ([zwave.basicV1.basicSet(value: 0x00).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def setLevel(value) {
	log.debug "Setting the level to: ${value}"
    
    //send event since most zwave devices don't publish the level event until polled
    sendEvent(name:"level",value:value)
    sendEvent(name:"switch.setLevel",value:value)
    
	delayBetween ([zwave.basicV1.basicSet(value: value).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def setLevel(value, duration) {
	log.debug "Firing the multi arg version"
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	zwave.switchMultilevelV2.switchMultilevelSet(value: value, dimmingDuration: dimmingDuration).format()
}

def poll() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
	poll()
}

def indicatorWhenOn() {
	sendEvent(name: "indicatorStatus", value: "when on", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 3, size: 1).format()
}

def indicatorWhenOff() {
	sendEvent(name: "indicatorStatus", value: "when off", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 3, size: 1).format()
}

def indicatorNever() {
	sendEvent(name: "indicatorStatus", value: "never", display: false)
	zwave.configurationV1.configurationSet(configurationValue: [2], parameterNumber: 3, size: 1).format()
}

def invertSwitch(invert=true) {
	if (invert) {
		zwave.configurationV1.configurationSet(configurationValue: [1], parameterNumber: 4, size: 1).format()
	}
	else {
		zwave.configurationV1.configurationSet(configurationValue: [0], parameterNumber: 4, size: 1).format()
	}
}
