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
    name: "HAM Bridge (Connect)",
    namespace: "narsul",
    author: "Anthony Hell",
    description: "Use this SmartApp to connect to HAM Bridge device",
    category: "SmartThings Labs",
    iconUrl: "http://solutionsetcetera.com/stuff/STIcons/HB.png",
    iconX2Url: "http://solutionsetcetera.com/stuff/STIcons/HB@2x.png")


preferences {
    section("Get the IP address for your HAM Bridge server:") {
        input "bridgeAddress", "string", title: "IP address", multiple: false, required: true
    }
    section("HAM Bridge port") {
        input "bridgePort", "string", title: "port", multiple: false, required: true
    }

    section("on this hub...") {
        input "theHub", "hub", multiple: false, required: true
    }

}

def installed() {
    log.debug "Installed ${app.label} with address '${settings.bridgeAddress}:${settings.bridgePort}' on hub '${settings.theHub.name}'"

    initialize()
}

def updated() {
}

def initialize() {
    def iphex = convertIPtoHex(bridgeAddress)
    def porthex = convertPortToHex(bridgePort)
    def dni = "$iphex:$porthex"
    def hubNames = location.hubs*.name.findAll { it }
    def d = addChildDevice("narsul", "HAM Bridge", dni, theHub.id, [label:"HAM Bridge", name:"HAM Bridge", completedSetup: true])
    log.trace "created HAM Bridge '${d.displayName}' with id $dni"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize('.').collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
