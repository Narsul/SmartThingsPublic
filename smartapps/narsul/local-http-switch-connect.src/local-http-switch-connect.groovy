/**
 *  HAM Bridge (Connect)
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
definition(
    name: "Local HTTP switch (Connect)",
    namespace: "narsul",
    author: "Anthony Hell",
    description: "Use this SmartApp to connect to local HTTP switch device",
    category: "SmartThings Labs",
    iconUrl: "http://i.imgur.com/NbKQ83q.png",
    iconX2Url: "http://i.imgur.com/TO44Kej.png",
    iconX3Url: "http://i.imgur.com/1HdLsHr.png")


preferences {
    section("Get the IP address for your HTTP switch server:") {
        input "serverAddress", "string", title: "IP address", multiple: false, required: true
    }
    section("HTTP switch port") {
        input "serverPort", "string", title: "port", multiple: false, required: true
    }
    section("Switch name") {
        input "switchName", "string", title: "internal name", multiple: false, required: true
    }
    section("Switch label") {
        input "switchLabel", "string", title: "label in the list", multiple: false, required: true
    }

    section("on this hub...") {
        input "theHub", "hub", multiple: false, required: true
    }
}

def installed() {
    //log.debug "Installed ${app.label} for switch ${settings.switchName} with address '${settings.serverAddress}:${settings.serverPort}' on hub '${settings.theHub.name}'"

    initialize()
}

def updated() {
}

def initialize() {
    def iphex = convertIPtoHex(serverAddress)
    def porthex = convertPortToHex(serverPort)
    def dni = "$iphex:$porthex:$switchName"
    def d = addChildDevice("narsul", "Local HTTP Switch", dni, theHub.id, [label:switchLabel, completedSetup: true])
    log.trace "created HTTP Switch '${d.displayName}' with id $dni"

    d.setSwitchName(switchName)
    log.trace "setSwitchName to ${switchName}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize('.').collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
