/**
 *  HAM Bridge
 *
 *  Author: Anthony Hell
 *  Date: 2015-11-07
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
  definition (name: "HAM Bridge", namespace: "narsul", author: "Anthony Hell") {
    capability "Speech Synthesis"

    // sendCommand(String commandName, String param1, String param2)
    command "sendCommand", ["string", "string", "string"]
  }

  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    valueTile("state", "device.state", decoration: "flat") {
      state "state", label:"${currentValue}"
    }

    main "state"
    details(["state"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
}

// handle commands
def sendCommand(command, param1=null, param2=null) {
  log.trace "sendCommand(${command}, ${param1}, ${param2})"

  // encode command parts for sending it via HTTP GET request
  def encodedCommand = [urlEncode(command)]
  if (param1 != null) encodedCommand.add(urlEncode(param1))
  if (param2 != null) encodedCommand.add(urlEncode(param2))
  encodedCommand = encodedCommand.join('&')
  def path = "/?${encodedCommand}"

  def headers = [:]
  headers.put("HOST", getHostAddress())

  def result = new physicalgraph.device.HubAction(
    method: "GET",
    path: path,
    headers: headers,
    body: ""
  )

  result
}

def speak(text) {
  log.trace "speak(${text})"

  sendCommand('say_text', text)
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
