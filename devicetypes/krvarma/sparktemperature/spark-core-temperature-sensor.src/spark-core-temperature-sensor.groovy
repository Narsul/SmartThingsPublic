/*
*
*  	Spark Core Temperature & Humidity Sensor
*
*  	Copyright 2014 Krishnaraj Varma
*
*	Reference and Inspiration
*		https://gist.github.com/Dianoga/6055918
*	
*	INSTALLATION
*	------------
* 	1) 	Create a new device type (https://graph.api.smartthings.com/ide/devices)
*     	Name: Spark Core Temperature Sensor
*     	Author: Krishnaraj Varma
*     	Capabilities:
*         	Polling, 
*			Relative Humidity Measurement, 
*			Sensor, 
*			Temperature Measurement
*
* 	2) 	Create a new device (https://graph.api.smartthings.com/device/list)
*     	Name: Your Choice
*     	Device Network Id: Your Choice
*     	Type: Spark Core Temperature Sensor (should be the last option)
*     	Location: Choose the correct location
*     	Hub/Group: Leave blank
*
* 	3) 	Update device preferences
*     	Click on the new device to see the details.
*     	Click the edit button next to Preferences
*     	Enter the Device ID and Access Token
*
*	4) 	Open the Mobile Application and add the newly created device, 
*		click refresh to see the Temperature and Humidity values
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*		http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
 
preferences {
    input("deviceId", "text", title: "Device ID")
    input("token", "text", title: "Access Token")
}

metadata {
	definition (name: "Spark Core Temperature Sensor", namespace: "krvarma/sparktemperature", author: "Krishnaraj Varma") {
		capability "Polling"
		capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Sensor"
	}

	simulator {
		
	}

	tiles {
		valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: false){
            state "temperature", label: '${currentValue}°', unit:"",
            	backgroundColors: [
                    [value: 17, color: "#288CD7"],
                    [value: 25, color: "#1F9500"],
                    [value: 30, color: "#DF5322"]
                ]
		}
        
        valueTile("humidity", "device.humidity", width: 2, height: 2){
            state "humidity", label: '${currentValue}%', unit:"",
            	backgroundColors: [
                    [value: 20, color: "#202040"],
                    [value: 50, color: "#202040"],
                    [value: 80, color: "#202080"]
                ]
		}
        
        standardTile("refresh", "device.temperature", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
        
        main(["temperature", "humidity"])
		details(["temperature", "humidity", "refresh"])
	}
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    
    getTemperature()
}

// Get the temperature & humidity
private getTemperature() {
    //Spark Core API Call
    def temperatureClosure = { response ->
	  	log.debug "Temeprature Request was successful, $response.data.result"
      
      	sendEvent(name: "temperature", value: Math.round(response.data.result as float), unit: "C")
	}
    
    def temperatureParams = [
  		uri: "https://api.spark.io/v1/devices/${deviceId}/temperature",
        query: [access_token: token],  
        success: temperatureClosure
	]

	httpGet(temperatureParams)
    
    def humidityClosure = { response ->
	  	log.debug "Humidity Request was successful, $response.data.result"
      
      	sendEvent(name: "humidity", value: Math.round(response.data.result as float), unit: '%')
	}
    
    def humidityParams = [
  		uri: "https://api.spark.io/v1/devices/${deviceId}/humidity",
        query: [access_token: token],  
        success: humidityClosure
	]

	httpGet(humidityParams)
}