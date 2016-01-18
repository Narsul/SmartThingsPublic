/**
 *  Local HTTP Hub
 *
 *  Author: Anthony Hell
 */
// for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Local HTTP Hub", namespace: "narsul", author: "Anthony Hell") {
		attribute "udn", "string"
		attribute "networkAddress", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
     	multiAttributeTile(name:"rich-control"){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
        attributeState "default", label: "Local HTTP Hub", action: "", icon: "st.Home.home2", backgroundColor: "#F3C200"
			}
    }
		standardTile("icon", "icon", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "default", label: "Local HTTP Hub", action: "", icon: "st.Home.home2", backgroundColor: "#FFFFFF"
		}
		valueTile("networkAddress", "device.networkAddress", decoration: "flat", height: 2, width: 4, inactiveLabel: false) {
			state "default", label:'${currentValue}', height: 1, width: 2, inactiveLabel: false
		}

		main (["icon"])
		details(["rich-control", "networkAddress"])
	}
}

// parse events into attributes
def parse(description) {
	log.debug "Parsing '${description}'"
	def results = []
	def result = parent.parse(this, description)
	if (result instanceof physicalgraph.device.HubAction){
		log.trace "LOCAL HTTP HUB SmatThings HubAction received -- DOES THIS EVER HAPPEN?"
		results << result
	} else if (description == "updated") {
		//do nothing
		log.trace "LOCAL HTTP HUB was updated"
	} else {
		def map = description
		if (description instanceof String)  {
			map = stringToMap(description)
		}
		if (map?.name && map?.value) {
			log.trace "LOCAL HTTP HUB, GENERATING EVENT: $map.name: $map.value"
			results << createEvent(name: "${map.name}", value: "${map.value}")
		} else {
    	log.trace "Parsing description"
			def msg = parseLanMessage(description)
			if (msg.body) {
				def contentType = msg.headers["Content-Type"]
				if (contentType?.contains("json")) {
					def bulbs = new groovy.json.JsonSlurper().parseText(msg.body)
					if (bulbs.state) {
						log.info "Bridge response: $msg.body"
					} else {
						// Sending Bulbs List to parent"
            if (parent.state.inDeviceDiscovery) {
              log.debug device
            }
          	log.info parent.devicesListHandler(device.hub.id, msg.body)
					}
				}
			}
		}
	}
	results
}
