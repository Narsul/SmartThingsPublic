/**
 *  External Switch
 *
 *  Copyright 2014 Anthony Peklo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
preferences {
	input("switchUri", "text", title: "Switch URL", description: "URL of a switch", required: true)
	input("switchName", "text", title: "Switch name", description: "Internal name of a switch", required: true)
}

metadata {
	definition (name: "HTTP Switch", namespace: "narsul", author: "Anthony Hell") {
		capability "Polling"
		capability "Switch"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: false) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Kids.kid10", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Kids.kid10", backgroundColor: "#79b821", nextState: "off"
		}
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"polling.poll", icon:"st.secondary.refresh"
        }
		main "button"
		details(["button", "refresh"])
	}
}

def parse(String description) {
	//log.debug "Parsing '${description}'"
}

def on() {
	log.debug('on')
	doRequest("/switch", [name: settings.switchName, state: 'on'])
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug('off')
	doRequest("/switch", [name: settings.switchName, state: 'off'])
	sendEvent(name: "switch", value: "off")
}

def poll() {
	log.debug('refreshing...')
    def pollClosure = { response ->
    	def state = response.data[settings.switchName] == true ? 'on' : 'off'
        log.debug("State of ${settings.switchName}: ${state}")
        sendEvent(name: "switch", value: state)
    }
    doRequest("/switches", [:], pollClosure)
}

def doRequest(path, query=[:], success={}) {
	def contentType = path == '/switches' ? 'application/json' : 'text/html'
	def requestParams = [
		uri:settings.switchUri, 
		path:path, 
		query:query,
		contentType: contentType,
        success: success
	]
	httpGet(requestParams)
}

