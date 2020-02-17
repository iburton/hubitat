/**
 *  IMPORT URL: https://raw.githubusercontent.com/iburton/hubitat/master/drivers/GE-Jasco%20Z-Wave%20Plus%20Fan%20Control%20(MegaPak).groovy
 *
 *  GE Z-Wave Plus Fan Control
 *
 *  History
 *  -------------------------------------------------------------------------------------------------------------------
 *  1.0.1 (2020-02-17) - Forked from Jason Bottjen (Blotched1). Cleaned code in preparation for standardization.
 *  1.0.2 (2020-02-17) - Added version GetVersionReport functionality
 */

metadata {
  definition (name: "GE Z-Wave Plus Fan Control (MegaPak)", namespace: "iburton", author: "Ira Burton") {
		capability "Actuator"
		capability "PushableButton"
		capability "DoubleTapableButton"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "SwitchLevel"
		capability "FanControl"

    command "getVersionReport"    
		
		attribute "speed", "enum", ["low","medium-low","medium","medium-high","high","on","off","auto"]
	}

  preferences {
    input (
      type: "enum",
      element: "paramInverted", 
      title: "Fan Buttons Direction", 
      multiple: false, 
      options: ["0" : "Normal (default)", "1" : "Inverted"], 
      required: false, 
      displayDuringSetup: true
    )

		input (
      type: "number",
      element: "paramLOW", 
      title: "Low Speed Fan %", 
      multiple: false, 
      defaultValue: "20",  
      range: "1..99", 
      required: false, 
      displayDuringSetup: true
    )

		input (
      type: "number",
      element: "paramMEDLOW",  
      title: "Medium-Low Speed Fan %", 
      multiple: false, 
      defaultValue: "40", 
      range: "1..99", 
      required: false, 
      displayDuringSetup: true
    )

		input (
      type: "number",
      element: "paramMED",
      title: "Medium Speed Fan %",
      multiple: false,
      defaultValue: "60",
      range: "1..99",
      required: false,
      displayDuringSetup: true
    )

		input (
      type: "number",
      element: "paramMEDHIGH",
      title: "Medium-High Speed Fan %",
      multiple: false,
      defaultValue: "80",
      range: "1..99",
      required: false,
      displayDuringSetup: true
    )

		input (
      type: "number",
      element: "paramHIGH",
      title: "High Speed Fan %",
      multiple: false,
      defaultValue: "99",
      range: "1..99",
      required: false,
      displayDuringSetup: true
    )

    input (
      type: "text",
      name: "requestedGroup2",
      title: "Association Group 2 Members (Max of 5):",
      description: "<br/><br/>Devices in group 2 will turn on/off when the switch is turned on or off.<br/><br/>" +
                   "Devices are entered as a comma delimited list of IDs in hexadecimal format.",
      required: false
    )

    input (
      type: "text",
      name: "requestedGroup3",
      title: "Association Group 3 Members (Max of 4):",
      description: "<br/><br/>Devices in group 3 will turn on/off when the switch is double tapped up or down.<br/><br/>" +
                   "Devices are entered as a comma delimited list of IDs in hexadecimal format.",
      required: false
    )

    input (
      type: "bool",       
      name: "logEnable", 
      title: "Enable debug logging", 
      defaultValue: false	
    )
  }
}

/** -------------------------------------------------------------------------------------------------------------------
  * MegaPak Helper Functions
  * ---------------------------------------------------------------------------------------------------------------- */

/** -------------------------------------------------------------------------------------------------------------------
  * Log to the Hubitat log file
  *   ERROR
  *   WARN
  *   INFO
  *   DEBUG
  *   TRACE
  * ---------------------------------------------------------------------------------------------------------------- */
def log(String level, String message) {
  String header = "MegaPak: ${device.displayName}: "

  if (level == "TRACE" && logEnable) {
    log.trace "$header $message"
  } else if (level == "DEBUG" && logEnable) {
    log.debug "$header $message"
  } else if (level == "INFO") {
    log.info "$header $message"
  } else if (level == "WARN") {
    log.warn "$header $message"
  } else {
    log.error "$header $message"
  }
}

/** -------------------------------------------------------------------------------------------------------------------
  * Called when the device is first installed
  * ---------------------------------------------------------------------------------------------------------------- */
def installed() {
  configure()
}

/** -------------------------------------------------------------------------------------------------------------------
  * Parse a ZWAVE packet
  * ---------------------------------------------------------------------------------------------------------------- */
def parse(String description) {
  
  def result = null

  log "DEBUG", "Parsing($description)"
    
  if (description != "updated") {
    def cmd = zwave.parse(description, [0x70: 1])

    log "DEBUG", "Command: $cmd"
    
    if (cmd) {
      result = zwaveEvent(cmd)
    }
  }

  log "DEBUG", "Parse returned ${result}"
  
  result
}

/** -------------------------------------------------------------------------------------------------------------------
  * Send Configuration data to the device
  * ---------------------------------------------------------------------------------------------------------------- */
def configure() {
  log "INFO", "Sending configuration to device"

  def commands = []

  commands << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format()
  commands << zwave.associationV1.associationRemove(groupingIdentifier:2, nodeId:zwaveHubNodeId).format()
  commands << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId).format()

  delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * Send ON command to the device
  * ---------------------------------------------------------------------------------------------------------------- */
def on() {
  log "INFO", "Turning device ON"

	def commands = []
  def currlevel = device.currentValue("level")
	
  sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
	
	if (currlevel == null) currlevel=20

	if (currlevel==0) sendEvent([name: "speed", value: "off", descriptionText: "fan speed set to off"])
	if (currlevel>0 && currlevel<=paramLOW) sendEvent([name: "speed", value: "low", displayed: true, descriptionText: "fan speed set to low"])
	if (currlevel>paramLOW && currlevel<=paramMEDLOW) sendEvent([name: "speed", value: "medium-low", displayed: true, descriptionText: "fan speed set to medium-low"])
	if (currlevel>paramMEDLOW && currlevel<=paramMED) sendEvent([name: "speed", value: "medium", displayed: true, descriptionText: "fan speed set to medium"])
	if (currlevel>paramMED && currlevel<=paramMEDHIGH) sendEvent([name: "speed", value: "medium-high", displayed: true, descriptionText: "fan speed set to medium-high"])
	if (currlevel>paramMEDHIGH && currlevel<=99) sendEvent([name: "speed", value: "high", displayed: true, descriptionText: "fan speed set to high"])
	
	commands << zwave.basicV1.basicSet(value: 0xFF).format()
  commands << zwave.switchMultilevelV2.switchMultilevelGet().format()
	
  delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * Send OFF command to the device
  * ---------------------------------------------------------------------------------------------------------------- */
def off() {
  log "INFO", "Turning device OFF"

	def commands = []

	sendEvent([name: "switch", value: "off", isStateChange: true, descriptionText: "$device.displayName is off"])
	
  commands << zwave.basicV1.basicSet(value: 0x00).format()
 	commands << zwave.switchMultilevelV2.switchMultilevelGet().format()

	delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * Set the power level of the fan
  * ---------------------------------------------------------------------------------------------------------------- */
// Ignore duration if provided
def setLevel(value, duration) {
	setLevel(value)
}

def setLevel(value) {
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def currval = device.currentValue("switch")

	state.level = level
	
  log "INFO", "Set level to $level"

	if (level > 0 && currval == "off") {
		sendEvent(name: "switch", value: "on", descriptionText: "$device.displayName is on")
	} else if (level == 0 && currval == "on") {
		sendEvent(name: "switch", value: "off", descriptionText: "$device.displayName is off")
	}

	sendEvent(name: "level", value: level, unit: "%", descriptionText: "$device.displayName is " + level + "%")

  def commands = []

  commands << zwave.basicV1.basicSet(value: level).format()
  commands << zwave.switchMultilevelV1.switchMultilevelGet().format()

	delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * Set the speed of the fan
  * ---------------------------------------------------------------------------------------------------------------- */
def setSpeed(fanspeed) {
  log "INFO", "Set fan speed to $fanspeed"

  def validSpeeds = ["low", "medium-low", "medium", "medium-high", "high"]
  def speedMap = ["low": paramLOW, "medium-low": paramMEDLOW, "medium": paramMED, "medium-high": paramMEDHIGH, "high": paramHIGH]

  if (validSpeeds.contains(fanspeed)) {
    def value = speedMap[fanspeed]
    sendEvent([name: "speed", value: fanspeed, displayed: true, descriptionText: "fan speed set to $fanspeed"])
    setLevel(value)
  } else if (fanspeed == "on") {
    on()
  } else if (fanspeed == "off") {
    off()    
  } else {
    log.warn "Unable to set fan to unknown value: $fanspeed"
  }
}

/** -------------------------------------------------------------------------------------------------------------------
  * Refresh data from the device
  * ---------------------------------------------------------------------------------------------------------------- */
def refresh() {
    log "INFO", "refresh() is called"
  
    def commands = []


    commands << zwave.switchBinaryV1.switchBinaryGet().format()
	  commands << zwave.switchMultilevelV1.switchMultilevelGet().format()
    commands << zwave.configurationV1.configurationGet(parameterNumber: 4).format()
	  commands << zwave.associationV2.associationGet(groupingIdentifier: 3).format()

    if (getDataValue("MSR") == null) {
      commands << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
    }

    delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * The configuration data has been updated
  * ---------------------------------------------------------------------------------------------------------------- */
def updated() {
  log "INFO", "Configuration Updated"
  log "WARN", "Debug logging is: ${logEnable == true}"

  // Turn logging off again after 30 minutes
  if (logEnable) runIn 1800, turnDebugLogsOff

  // Limit updates to no more frequent than every three seconds
  if (state.lastUpdated && now() <= state.lastUpdated + 3000) return
  state.lastUpdated = now()

	def commands = []
  commands << zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format()
	commands << zwave.associationV1.associationRemove(groupingIdentifier:2, nodeId:zwaveHubNodeId).format()
	commands << zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:zwaveHubNodeId).format()

	//associations
	def nodes = null
	
  if (settings.requestedGroup2 != state.currentGroup2) {
    nodes = parseAssocGroupList(settings.requestedGroup2, 2)
    commands << zwave.associationV2.associationRemove(groupingIdentifier: 2, nodeId: []).format()
    commands << zwave.associationV2.associationSet(groupingIdentifier: 2, nodeId: nodes).format()
    commands << zwave.associationV2.associationGet(groupingIdentifier: 2).format()
    state.currentGroup2 = settings.requestedGroup2
  }

  if (settings.requestedGroup3 != state.currentGroup3) {
    nodes = parseAssocGroupList(settings.requestedGroup3, 3)
    commands << zwave.associationV2.associationRemove(groupingIdentifier: 3, nodeId: []).format()
    commands << zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: nodes).format()
    commands << zwave.associationV2.associationGet(groupingIdentifier: 3).format()
    state.currentGroup3 = settings.requestedGroup3
  }

	commands << zwave.configurationV1.configurationSet(scaledConfigurationValue: paramInverted.toInteger(), parameterNumber: 4, size: 1).format()
	commands << zwave.configurationV1.configurationGet(parameterNumber: 4).format()  
	
	// Set Inverted param
	if (paramInverted==null) paramInverted = 0

  // Set LED param
  if (paramLED==null) paramLED = 0

  delayBetween commands, 500
}

/** -------------------------------------------------------------------------------------------------------------------
  * Turn debug logging back off
  * ---------------------------------------------------------------------------------------------------------------- */
def turnDebugLogsOff() {
    log "WARN", "Debug logging disabled"
    device.updateSetting "logEnable", [value:"false", type:"bool"]
}

/** -------------------------------------------------------------------------------------------------------------------
  * Ensure the group list provided as a string to ensure it is in a valid format
  * ---------------------------------------------------------------------------------------------------------------- */
private parseAssocGroupList(list, group) {
  def nodes = group == 2 ? [] : [zwaveHubNodeId]

  if (list) {
    def nodeList = list.split(',')
    def max = group == 2 ? 5 : 4
    def count = 0

    nodeList.each { node ->
      node = node.trim()
      if ( count >= max) {
        log "WARN", "Association Group ${group}: Too many members. Greater than ${max}! This one was discarded: ${node}"
      } else if (node.matches("\\p{XDigit}+")) {
        def nodeId = Integer.parseInt(node,16)
        if (nodeId == zwaveHubNodeId) {
          log "WARN", "Association Group ${group}: Adding the hub ID as an association is not allowed."
        } else if ( (nodeId > 0) & (nodeId < 256) ) {
          nodes << nodeId
          count++
        } else {
          log "WARN", "Association Group ${group}: Invalid member: ${node}"
        }
      } else {
        log "WARN", "Association Group ${group}: Invalid member: ${node}"
      }
    }
  }
  
  log "DEBUG", "Nodes is $nodes"
  
  nodes
}

/** -------------------------------------------------------------------------------------------------------------------
  * Request a version report from the device
  * ---------------------------------------------------------------------------------------------------------------- */
def getVersionReport(){
	return zwave.versionV1.versionGet().format()
}

/** -------------------------------------------------------------------------------------------------------------------
  * Z-Wave Event Definitions
  * ---------------------------------------------------------------------------------------------------------------- */
def zwaveEvent(hubitat.zwave.commands.crc16encapv1.Crc16Encap cmd) {
  log "DEBUG", "Z-Wave Event: CRC-16 Encapsulation Command received: ${cmd}"
  
  def newVersion = 1
  
  // Manually set the version on commands, as needed based on the device capabilities
  if (cmd.commandClass == 114) { newVersion = 2 }    // Manufacturer
  if (cmd.commandClass == 133) { newVersion = 2 }    // Association
  
  def encapsulatedCommand = zwave.getCommand(cmd.commandClass, cmd.command, cmd.data, newVersion)

  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  } else {
    log "WARN", "Unable to extract CRC16 command from ${cmd}"
  }
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
  log "DEBUG", "Basic Report V1: ${cmd}"
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
  log "DEBUG", "Basic Set V1: ${cmd}"

  def result = []
  
  if (cmd.value == 255) {
    log "DEBUG", "Double Up Triggered"
    result << createEvent([name: "doubleTapped", value: 1, descriptionText: "$device.displayName had Doubletap up (button 1)", type: "physical", isStateChange: true])
  } else if (cmd.value == 0) {
    log "DEBUG", "Double Down Triggered"
    result << createEvent([name: "doubleTapped", value: 2, descriptionText: "$device.displayName had Doubletap down (button 2)", type: "physical", isStateChange: true])
  }

  result
}

def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
  log "DEBUG", "Association Report V2: groupingIdentifier: ${cmd.groupingIdentifier} maxNodesSupported: ${cmd.maxNodesSupported} nodeId: ${cmd.nodeId} reportsToFollow: ${cmd.reportsToFollow}"

  if (cmd.groupingIdentifier == 3) {
    if (cmd.nodeId.contains(zwaveHubNodeId)) {
      sendEvent(name: "numberOfButtons", value: 2, displayed: false)
    } else {
      sendEvent(name: "numberOfButtons", value: 0, displayed: false)
      zwave.associationV2.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
      zwave.associationV2.associationGet(groupingIdentifier: 3).format()
    }
  }
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log "DEBUG", "Configuration Report V1: ${cmd}"

  def result = []
	def name = ""
  def value = ""
  def reportValue = cmd.configurationValue[0]

  switch (cmd.parameterNumber) {
    case 4:
      name = "inverted"
      value = reportValue == 1 ? "true" : "false"
      break
    default:
      break
  }
	
  result << createEvent([name: name, value: value, displayed: false])
	
  result
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { 
	def desc = "$device.displayName was turned "
  desc += cmd.value ? "on" : "off"
	
	createEvent([name: "switch", value: cmd.value ? "on" : "off", descriptionText: "$desc", isStateChange: true])
}

def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  log "DEBUG", "Manufacturer Specific Report V2: ${cmd}"
  log "DEBUG", "manufacturerId:   ${cmd.manufacturerId}"
  log "DEBUG", "manufacturerName: ${cmd.manufacturerName}"

  state.manufacturer=cmd.manufacturerName

  log "DEBUG", "productId:        ${cmd.productId}"
  log "DEBUG", "productTypeId:    ${cmd.productTypeId}"

  def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)

  updateDataValue("MSR", msr)	
  
  sendEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
  log "DEBUG", "Version Report V1: ${cmd}"
  def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
  updateDataValue("fw", fw)
  log "DEBUG", "Version Report V1: Is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.versionv3.VersionReport cmd) {
  log "DEBUG", "Version Report V3: ${cmd}"
  def fw = "${cmd.firmware0Version}.${cmd.firmware0SubVersion}"
  updateDataValue("fw", fw)
  log "DEBUG", "Version Report V3: Is running firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	log "DEBUG", "Switch Multilevel Report V3: ${cmd}"
	
	def currSpeed = device.currentValue("speed")
	
	if (cmd.value) {
		sendEvent(name: "level", value: cmd.value, unit: "%", descriptionText: "$device.displayName is " + cmd.value + "%")
		if (device.currentValue("switch") == "off") sendEvent(name: "switch", value: "on", isStateChange: true, descriptionText: "$device.displayName is on")
	} else {
		if (device.currentValue("switch") == "on") sendEvent(name: "switch", value: "off", isStateChange: true, , descriptionText: "$device.displayName is off")
	}

	if (cmd.value == 0) sendEvent([name: "speed", value: "off", descriptionText: "fan speed set to off"])
	if (cmd.value > 0 && cmd.value <= paramLOW) sendEvent([name: "speed", value: "low", displayed: true, descriptionText: "fan speed set to low"])
	if (cmd.value > paramLOW && cmd.value <= paramMEDLOW) sendEvent([name: "speed", value: "medium-low", displayed: true, descriptionText: "fan speed set to medium-low"])
	if (cmd.value > paramMEDLOW && cmd.value <= paramMED) sendEvent([name: "speed", value: "medium", displayed: true, descriptionText: "fan speed set to medium"])
	if (cmd.value > paramMED && cmd.value <= paramMEDHIGH) sendEvent([name: "speed", value: "medium-high", displayed: true, descriptionText: "fan speed set to medium-high"])
	if (cmd.value > paramMEDHIGH && cmd.value <= 99) sendEvent([name: "speed", value: "high", displayed: true, descriptionText: "fan speed set to high"])
}

def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
  log "DEBUG", "Hail V1: ${cmd}"
  [name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
}

def zwaveEvent(hubitat.zwave.Command cmd) {
  log "WARN", "Received unhandled command: ${cmd}"
}
