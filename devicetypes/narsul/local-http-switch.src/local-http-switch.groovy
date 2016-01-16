/**
 *  External Switch
 *
 *  Copyright 2015 Anthony Hell
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

metadata {
  definition (name: "Local HTTP Switch", namespace: "narsul", author: "Anthony Hell") {
    capability "Polling"
    capability "Switch"

    // setSwitchName(String switchName)
    command "setSwitchName", ["string"]
  }

  // simulator metadata
  simulator {
  }

  // UI tile definitions
  tiles {
    standardTile("button", "device.switch", width: 2, height: 2, decoration: "flat") {
      state "off", label: 'Off', action: "switch.on", icon:"st.Lighting.light13", backgroundColor: "#ffffff", nextState: "on"
      state "on", label: 'On', action: "switch.off", icon: "st.Lighting.light13", backgroundColor: "#79b821", nextState: "off"
    }
    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
        state "default", label:'', action:"polling.poll", icon:"st.secondary.refresh"
    }
    main(["button"])
    details(["button", "refresh"])
  }
}

def parse(description) {
	log.trace "parse()"

    def msg = parseLanMessage(description)
    def json = msg.json
	log.trace "parse(${msg})"
	log.debug "Received json: ${json}"
    
    if (json && json.containsKey(state.switchName)) {
	    def newState = json[state.switchName] == true ? 'on' : 'off'
        log.debug("State of ${state.switchName}: ${newState}")
        sendEvent(name: "switch", value: newState)
    }
}

def on() {
  log.trace('on')
  doRequest("/switch", [name: state.switchName, state: 'on'])
}

def off() {
  log.trace('off')
  doRequest("/switch", [name: state.switchName, state: 'off'])
}

def poll() {
  log.trace "poll()"
  doRequest("/switches", [:])
}

def setSwitchName(switchName) {
  state.switchName = switchName;
}

// handle commands
def doRequest(path, query=[:], success={}) {
  log.trace "doRequest(${path}, ${query})"

//  def contentType = path == '/switches' ? 'application/json' : 'text/html'
  def request = new physicalgraph.device.HubAction(
    method: "GET",
    path: path,
    headers: ["HOST": getHostAddress()],
    query: query
  )
  
  //log.debug request

  request
}

// Private functions used internally
private Integer convertHexToInt(hex) {
  Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
  [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String urlEncode(text) {
  URLEncoder.encode(text, "UTF-8").replaceAll("\\+", "%20")
}

private getHostAddress() {
  def parts = device.deviceNetworkId.split(":")
  def ip = convertHexToIP(parts[0])
  def port = convertHexToInt(parts[1])
  return ip + ":" + port
}
