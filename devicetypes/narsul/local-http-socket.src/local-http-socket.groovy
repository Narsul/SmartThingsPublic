/**
 *  Local HTTP Socket
 *
 *  Author: Anthony Hell
 */
// for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Local HTTP Socket", namespace: "narsul", author: "Anthony Hell") {
		capability "Switch"
		capability "Polling"
		capability "Refresh"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.Appliances.appliances17", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.Appliances.appliances17", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.Appliances.appliances17", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.Appliances.appliances17", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}

		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
	}

	main(["switch"])
	details(["switch", "refresh"])
}

// parse events into attributes
def parse(description) {
	log.debug "parse() - $description"
}

// handle commands
def on() {
	log.trace parent.socketOn(this)
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.trace parent.socketOff(this)
	sendEvent(name: "switch", value: "off")
}

def poll() {
	parent.socketPoll(this)
}

def refresh() {
	parent.socketPoll(this)
}
