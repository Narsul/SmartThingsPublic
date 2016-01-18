/**
 *  Local HTTP Hub (Connect)
 *
 *  Author: Anthony Hell
 *  Date: 2016-01-16
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
  name: "Local HTTP Hub (Connect)",
  namespace: "narsul",
  author: "Anthony Hell",
  description: "Use this SmartApp to connect to local HTTP Hub",
  category: "SmartThings Labs",
  iconUrl: "http://i.imgur.com/NbKQ83q.png",
  iconX2Url: "http://i.imgur.com/TO44Kej.png",
  iconX3Url: "http://i.imgur.com/1HdLsHr.png")


preferences {
  page(name:"mainPage", title:"Local HTTP Hub setup", content:"mainPage", refreshTimeout:5)
  page(name:"hubDiscoveryPage", title:"Local HTTP Hub discovery", content:"hubDiscoveryPage", refreshTimeout:5)
  page(name:"devicesDiscoveryPage", title:"Local HTTP Hub devices discovery", content:"devicesDiscoveryPage", refreshTimeout:5)
}

def mainPage() {
  if (canInstallLabs()) {
    def hubs = hubsDiscovered()
    return hubs ? devicesDiscoveryPage() : hubDiscoveryPage()
  }

  def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

  return dynamicPage(name:"hubDiscoveryPage", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
      section("Upgrade") {
          paragraph "$upgradeNeeded"
      }
  }
}

def hubDiscoveryPage(params=[:]) {
  def hubs = hubsDiscovered()
  int hubRefreshCount = !state.hubRefreshCount ? 0 : state.hubRefreshCount as int
  state.hubRefreshCount = hubRefreshCount + 1
  def refreshInterval = 3

  def options = hubs ?: []
  def numFound = options.size() ?: 0

  if (numFound == 0 && state.hubRefreshCount > 25) {
    log.trace "Cleaning old hubs memory"
    state.hubs = [:]
    state.hubRefreshCount = 0
    app.updateSetting("selectedHub", "")
  }

  subscribe(location, null, locationHandler, [filterEvents:false])

  //bridge discovery request every 15 //25 seconds
  if ((hubRefreshCount % 5) == 0) {
    discoverHubs()
  }

  //setup.xml request every 3 seconds except on discoveries
  if (((hubRefreshCount % 1) == 0) && ((hubRefreshCount % 5) != 0)) {
    verifyHubs()
  }

  return dynamicPage(name:"hubDiscoveryPage", title:"Discovery Started!", nextPage:"devicesDiscoveryPage", refreshInterval:refreshInterval, uninstall: true) {
    section("Please wait while we discover your Local HTTP Hub. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
      input "selectedHub", "enum", required:false, title:"Select Local HTTP Hub (${numFound} found)", multiple:false, options:options
    }
  }
}

def devicesDiscoveryPage() {
  int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
  state.deviceRefreshCount = deviceRefreshCount + 1
  def refreshInterval = 3
  state.inDeviceDiscovery = true
  def hub = null
  if (selectedHub) {
    hub = getChildDevice(selectedHub)
    subscribe(hub, "devicesList", devicesListData)
  }
  state.hubRefreshCount = 0
  def deviceOptions = devicesDiscovered() ?: [:]
  def numFound = deviceOptions.size() ?: 0
  if (numFound == 0) {
    app.updateSetting("selectedDevices", "")
  }

  if ((deviceRefreshCount % 5) == 0) {
    discoverDevices()
  }

  return dynamicPage(name:"devicesDiscoveryPage", title:"Devices Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
    section("Please wait while we discover your devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
      input "selectedDevices", "enum", required:false, title:"Select devices (${numFound} found)", multiple:true, options:deviceOptions
    }
    section {
      def title = getLocalHttpHubIP() ? "Local HTTP Hub (${getLocalHttpHubIP()})" : "Find hubs"
      href "hubDiscoveryPage", title: title, description: "", state: selectedHub ? "complete" : "incomplete", params: [override: true]

    }
  }
}

Map hubsDiscovered() {
  def vhubs = getVerifiedHubs()
  def map = [:]
  vhubs.each {
    def value = "${it.value.name}"
    def key = "${it.value.mac}"
    map["${key}"] = value
  }
  map
}

Map devicesDiscovered() {
  def devices = getDevices()
  def devicesMap = [:]
  if (devices instanceof java.util.Map) {
    devices.each {
      def value = "${it.value.name}"
      def key = app.id +"/"+ it.value.id
      devicesMap["${key}"] = value
    }
  } else { //backwards compatable
    devices.each {
      def value = "${it.name}"
      def key = app.id +"/"+ it.id
            logg += "$value - $key, "
      devicesMap["${key}"] = value
    }
  }
  devicesMap
}

def devicesListData(evt) {
  state.devices = evt.jsonData
}


Map getDevices() {
  state.devices = state.devices ?: [:]
}

def getHubs() {
  state.hubs = state.hubs ?: [:]
}

def getVerifiedHubs() {
  getHubs().findAll{ it?.value?.verified == true }
}

def installed() {
  log.trace "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.trace "Updated with settings: ${settings}"
  unschedule()
  unsubscribe()
  initialize()
}

def initialize() {
  log.debug "Initializing"
  unsubscribe(hub)
  state.inDeviceDiscovery = false
  state.hubRefreshCount = 0
  state.deviceRefreshCount = 0
  if (selectedHub) {
    addHub()
    addDevices()
    doDeviceSync()
    runEvery5Minutes("doDeviceSync")
  }
}

def uninstalled(){
  state.hubs = [:]
}

// Handles events to add new bulbs
def devicesListHandler(stHub, data = "") {
  def msg = "Devices list not processed. Only while in settings menu."
  def devices = [:]
  if (state.inDeviceDiscovery) {
    def logg = ""
    log.trace "Adding devices to state..."
    state.hubProcessedLightList = true
    def object = new groovy.json.JsonSlurper().parseText(data)
    object.each { k,v ->
      if (v instanceof Map) {
        devices[k] = [id: k, name: v.name, type: v.type, stHub:stHub]
      }
    }
  }
  def hub = null
  if (selectedHub) {
    hub = getChildDevice(selectedHub)
  }
  hub.sendEvent(name: "devicesList", value: stHub, data: devices, isStateChange: true, displayed: false)
  msg = "${devices.size()} bulbs found. ${devices}"
  return msg
}

def addDevices() {
  def devices = getDevices()
  selectedDevices?.each { dni ->
    def d = getChildDevice(dni)
    if (!d) {
      def newDevice
      if (devices instanceof java.util.Map) {
        newDevice = devices.find { (app.id + "/" + it.value.id) == dni }
        if (newDevice != null) {
          if (newDevice?.value?.type?.equalsIgnoreCase("switch") ) {
            d = addChildDevice("narsul", "Local HTTP Switch", dni, newDevice?.value.stHub, ["label":newDevice?.value.name])
          } else {
            d = addChildDevice("narsul", "Local HTTP Socket", dni, newDevice?.value.stHub, ["label":newDevice?.value.name])
          }
        } else {
          log.debug "$dni in not longer paired to the Local HTTP Hub or ID changed"
        }
      }

      log.debug "created ${d.displayName} with id $dni"
      d.refresh()
    } else {
      log.debug "found ${d.displayName} with id $dni already exists, type: '$d.typeName'"
      if (devices instanceof java.util.Map) {
        def newDevice = devices.find { (app.id + "/" + it.value.id) == dni }
        if (newDevice?.value?.type?.equalsIgnoreCase("socket") && d.typeName == "Local HTTP Switch") {
          d.setDeviceType("Local HTTP Socket")
        }
      }
    }
  }
}

def addHub() {
  def vhubs = getVerifiedHubs()
  def vhub = vhubs.find {"${it.value.mac}" == selectedHub}

  if (vhub) {
    def d = getChildDevice(selectedHub)
    if(!d) {
      // compatibility with old devices
      def newHub = true
      childDevices.each {
        if (it.getDeviceDataByName("mac")) {
          def newDNI = "${it.getDeviceDataByName("mac")}"
          if (newDNI != it.deviceNetworkId) {
            def oldDNI = it.deviceNetworkId
            log.debug "updating dni for device ${it} with $newDNI - previous DNI = ${it.deviceNetworkId}"
            it.setDeviceNetworkId("${newDNI}")
            if (oldDNI == selectedHub) {
              app.updateSetting("selectedHub", newDNI)
            }
            newHub = false
          }
        }
      }
      if (newHub) {
        log.debug "ADDING NEW HUB CHILD DEVICE ${vhub.value}"
        d = addChildDevice("narsul", "Local HTTP Hub", selectedHub, vhub.value.stHub)
        log.debug "created ${d.displayName} with id ${d.deviceNetworkId}"
        def childDevice = getChildDevice(d.deviceNetworkId)
        childDevice.sendEvent(name: "udn", value: vhub.value.udn)
        if (vhub.value.ip && vhub.value.port) {
          if (vhub.value.ip.contains(".")) {
            childDevice.sendEvent(name: "networkAddress", value: vhub.value.ip + ":" +  vhub.value.port)
            childDevice.updateDataValue("networkAddress", vhub.value.ip + ":" +  vhub.value.port)
          } else {
            childDevice.sendEvent(name: "networkAddress", value: convertHexToIP(vhub.value.ip) + ":" +  convertHexToInt(vhub.value.port))
            childDevice.updateDataValue("networkAddress", convertHexToIP(vhub.value.ip) + ":" +  convertHexToInt(vhub.value.port))
          }
        } else {
          childDevice.sendEvent(name: "networkAddress", value: convertHexToIP(vhub.value.networkAddress) + ":" +  convertHexToInt(vhub.value.deviceAddress))
          childDevice.updateDataValue("networkAddress", convertHexToIP(vhub.value.networkAddress) + ":" +  convertHexToInt(vhub.value.deviceAddress))
        }
      }
    } else {
      log.debug "found ${d.displayName} with id $selectedHue already exists"
    }
  }
}

def locationHandler(evt) {
  def description = evt.description

  def stHub = evt?.hubId
  def parsedEvent = parseLanMessage(description)
  parsedEvent << ["hub":stHub]

  if (parsedEvent?.ssdpTerm?.contains("urn:local-http-hub:1")) {
    //SSDP DISCOVERY EVENTS
    log.trace "SSDP DISCOVERY EVENTS"
    def hubs = getHubs()
    if (!(hubs."${parsedEvent.ssdpUSN.toString()}")) {
      // hub does not exist
      log.trace "Adding hub ${parsedEvent.ssdpUSN}"
      hubs << ["${parsedEvent.ssdpUSN.toString()}":parsedEvent]
    } else {
      // update the values
      def ip = convertHexToIP(parsedEvent.networkAddress)
      def port = convertHexToInt(parsedEvent.deviceAddress)
      def host = ip + ":" + port
      log.debug "Device ($parsedEvent.mac) was already found in state with ip = $host."
      def dstate = hubs."${parsedEvent.ssdpUSN.toString()}"
      def dni = "${parsedEvent.mac}"
      def d = getChildDevice(dni)
      def networkAddress = null
      if (!d) {
        childDevices.each {
          if (it.getDeviceDataByName("mac")) {
            def newDNI = "${it.getDeviceDataByName("mac")}"
            if (newDNI != it.deviceNetworkId) {
              def oldDNI = it.deviceNetworkId
              log.debug "updating dni for device ${it} with $newDNI - previous DNI = ${it.deviceNetworkId}"
              it.setDeviceNetworkId("${newDNI}")
              if (oldDNI == selectedHub) {
                app.updateSetting("selectedHub", newDNI)
              }
              doDeviceSync()
            }
          }
        }
      } else {
        if (d.getDeviceDataByName("networkAddress")) {
          networkAddress = d.getDeviceDataByName("networkAddress")
        } else {
          networkAddress = d.latestState('networkAddress').stringValue
        }
        log.trace "Host: $host - $networkAddress"
        if (host != networkAddress) {
          log.debug "Device's port or ip changed for device $d..."
          dstate.ip = ip
          dstate.port = port
          dstate.name = "Philips hue ($ip)"
          d.sendEvent(name:"networkAddress", value: host)
          d.updateDataValue("networkAddress", host)
        }
      }
    }
  } else if (parsedEvent.headers && parsedEvent.body) {
    log.trace "LOCAL HTTP HUB RESPONSES"

    def headerString = parsedEvent.headers.toString()
    if (headerString?.contains("json")) {
      def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)

      if (body?.method?.contains("description")) {
        // GET /description
        log.trace "description response (application/json)"
        def hubs = getHubs()
        def hub = hubs.find {it?.key?.contains(body?.udn)}
        if (hub) {
          hub.value << [name:body?.name, udn:body?.udn, verified: true]
        } else {
          log.error "/description returned a bridge that didn't exist"
        }
      } else if (body?.method?.contains("devices")) {
        // GET /devices
        log.trace "devices response (application/json)"
        def devices = getDevices()
        log.debug "Adding devices to state!"
        def dni
        def d
        // switches
        body.devices.switches.each { k,v ->
          devices[v.id] = [id: v.id, name: v.name, type: "switch", stHub:parsedEvent.hub]
        }
        // sockets
        body.devices.sockets.each { k,v ->
          devices[v.id] = [id: v.id, name: v.name, type: "socket", stHub:parsedEvent.hub]
        }
      } else if (body?.method?.contains("switch")) {
        // GET|PUT /switch

        def state = (body.switch.state == true) ? "on" : "off"
        log.debug "Setting state for device ${body.switch.id}: ${state}"
        def dni = app.id + "/" + body.switch.id
        def d = getChildDevice(dni)
        if (d) {
          d.sendEvent(name: "switch", value: state)
        }
      } else if (body?.method?.contains("socket")) {
        // GET|PUT /switch

        def state = (body.socket.state == true) ? "on" : "off"
        log.debug "Setting state for device ${body.socket.id}: ${state}"
        def dni = app.id + "/" + body.socket.id
        def d = getChildDevice(dni)
        if (d) {
          d.sendEvent(name: "switch", value: state)
        }
      }
    }
  } else {
    log.trace "NON-LOCAL HTTP HUB EVENT $evt.description"
  }
}

def doDeviceSync(){
  log.trace "Doing Local HTTP Hub Device Sync!"
  convertDevicesListToMap()
  poll()
  try {
    subscribe(location, null, locationHandler, [filterEvents:false])
  } catch (all) {
    log.trace "Subscription already exist"
  }
  discoverHubs()
}

private discoverHubs() {
  log.trace "discoverHubs()"
  sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:local-http-hub:1", physicalgraph.device.Protocol.LAN))
}

private discoverDevices() {
  log.trace "discoverDevices()"
  def host = getLocalHttpHubIP()
  sendHubCommand(new physicalgraph.device.HubAction([
    method: "GET",
    path: "/devices",
    headers: [
      HOST: host
    ]], "${selectedHub}"))
}

private verifyHub(String deviceNetworkId, String host) {
  log.trace "verifyHub(${deviceNetworkId}, ${host})"
  sendHubCommand(new physicalgraph.device.HubAction([
    method: "GET",
    path: "/description",
    headers: [
      HOST: host
    ]], deviceNetworkId))
}

private verifyHubs() {
  log.trace "verifyHubs()"
  def devices = getHubs().findAll { it?.value?.verified != true }
  devices.each {
    def ip = convertHexToIP(it.value.networkAddress)
    def port = convertHexToInt(it.value.deviceAddress)
    verifyHub("${it.value.mac}", (ip + ":" + port))
  }
}

private Boolean canInstallLabs() {
  return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware) {
  return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions() {
  return location.hubs*.firmwareVersionString.findAll { it }
}

private getLocalHttpHubIP() {
  def host = null
  if (selectedHub) {
    def d = getChildDevice(selectedHub)
    if (d) {
      if (d.getDeviceDataByName("networkAddress")) {
        host = d.getDeviceDataByName("networkAddress")
      } else {
        host = d.latestState('networkAddress').stringValue
      }
    }
    if (host == null || host == "") {
      def hubUdn = selectedHub
      def hub = getHubs().find { it?.value?.udn?.equalsIgnoreCase(hubUdn) }?.value
      if (!hub) {
        hub = getHubs().find { it?.value?.mac?.equalsIgnoreCase(hubUdn) }?.value
      }
      if (hub?.ip && hub?.port) {
        if (hub?.ip.contains(".")) {
          host = "${hub?.ip}:${hub?.port}"
        } else {
          host = "${convertHexToIP(hub?.ip)}:${convertHexToInt(hub?.port)}"
        }
      } else if (hub?.networkAddress && hub?.deviceAddress) {
        host = "${convertHexToIP(hub?.networkAddress)}:${convertHexToInt(hub?.deviceAddress)}"
      }
    }
    log.trace "Hub: $selectedHub - Host: $host"
  }
  return host
}

private Integer convertHexToInt(hex) {
  Integer.parseInt(hex,16)
}

def convertDevicesListToMap() {
  try {
    if (state.devices instanceof java.util.List) {
      def map = [:]
      state.devices.unique {it.id}.each { device ->
        map << ["${device.id}":["id":device.id, "name":device.name, "stHub":device.stHub]]
      }
      state.devices = map
    }
  }
  catch(Exception e) {
    log.error "Caught error attempting to convert devices list to map: $e"
  }
}

private String convertHexToIP(hex) {
  [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}


/////////////////////////////////////
//CHILD DEVICE METHODS
/////////////////////////////////////

// switch methods

def switchOn(childDevice) {
  log.debug "Executing 'on'"
  putRequest("/switch/${getId(childDevice)}/on", [:])
  return "Switch is On"
}

def switchOff(childDevice) {
  log.debug "Executing 'off'"
  putRequest("/switch/${getId(childDevice)}/off", [:])
  return "Switch is Off"
}

def switchPoll(childDevice) {
  log.debug "Executing 'poll'"
  getRequest("/switch/${getId(childDevice)}")
  return "Polling switch state"

}

// switch methods

def socketOn(childDevice) {
  log.debug "Executing 'on'"
  putRequest("/socket/${getId(childDevice)}/on", [:])
  return "Socket is On"
}

def socketOff(childDevice) {
  log.debug "Executing 'off'"
  putRequest("/socket/${getId(childDevice)}/off", [:])
  return "Socket is Off"
}

def socketPoll(childDevice) {
  log.debug "Executing 'poll'"
  getRequest("/socket/${getId(childDevice)}")
  return "Polling socket state"

}

// general methods

private getId(childDevice) {
    return childDevice.device?.deviceNetworkId.split("/")[-1]
}

private poll() {
  def host = getLocalHttpHubIP()
  def uri = "/devices"
  try {
    sendHubCommand(new physicalgraph.device.HubAction("""GET ${uri} HTTP/1.1
HOST: ${host}

""", physicalgraph.device.Protocol.LAN, selectedHub))
  } catch (all) {
    log.warn "Parsing Body failed - trying again..."
    doDeviceSync()
  }
}

private getRequest(path) {
  def host = getLocalHttpHubIP()
  def uri = path

  sendHubCommand(new physicalgraph.device.HubAction("""GET ${uri} HTTP/1.1
HOST: ${host}

""", physicalgraph.device.Protocol.LAN, selectedHub))
}

private putRequest(path, body) {
  def host = getLocalHttpHubIP()
  def uri = path
  def bodyJSON = new groovy.json.JsonBuilder(body).toString()
  def length = bodyJSON.getBytes().size().toString()

  log.debug "PUT:  $host$uri"
  log.debug "BODY: ${bodyJSON}"

  sendHubCommand(new physicalgraph.device.HubAction("""PUT $uri HTTP/1.1
HOST: ${host}
Content-Length: ${length}

${bodyJSON}
""", physicalgraph.device.Protocol.LAN, selectedHub))
}
