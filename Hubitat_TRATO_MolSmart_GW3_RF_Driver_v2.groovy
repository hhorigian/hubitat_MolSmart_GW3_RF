/**
 *  MolSmart GW3 Driver - RF - Cortinas, Luzes RF, Controle tudo via RF. 
 *
 *  Copyright 2024 VH 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable lawkkk or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *
 *   +++  Versão para enviar Códigos SENDIR Diretamente no botão ++++
 */

metadata {
  definition (name: "MolSmart - GW3 - RF", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Switch"  
    capability "Contact Sensor"
    capability "Sensor"
    capability "PushableButton"      

	attribute "currentstatus", "string"
      

  }
      
  }

  preferences {
        input name: "molIPAddress", type: "text", title: "MolSmart IP",   required: true, defaultValue: "192.168.1.100" 
    	input name: "serialNum", title:"N Serie (Etiqueta GW3)", type: "string", required: true
	    input name: "verifyCode", title:"Verify code (Etiqueta GW3)", type: "string", required: true
    	input name: "cId", title:"Control ID (pego no idoor)", type: "string", required: true  
    	//input name: "rcId", title:"RCID (51=RF)", type: "string", required: false, defaultValue: "51"  
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false      
  }   

def AtualizaDadosGW3() {
    state.currentip = settings.molIPAddress
    state.serialNum = settings.serialNum
    state.verifyCode = settings.verifyCode
    state.cId = settings.cId
    state.rcId = 51
    log.info "Dados do GW3 atualizados: " + state.currentip + " -- " + state.serialNum + " -- " +  state.verifyCode + " -- " + state.cId + " -- " +  state.rcId
}

def initialized()
{

    log.debug "initialized()"
    
}

def installed()
{
   

    sendEvent(name:"numberOfButtons", value:4)   
    sendEvent(name: "status", value: "stop")   
    log.debug "installed()"
    
}

def updated()
{
   
    sendEvent(name:"numberOfButtons", value:4)    
    log.debug "updated()"
    AtualizaDadosGW3()       

    
}



def on() {
     //sendEvent(name: "switch", value: "on", isStateChange: true)
     
}

def off() {
     //sendEvent(name: "switch", value: "off", isStateChange: true)
     
}

def push(number) {
    sendEvent(name:"pushed", value:number, isStateChange: true)
    log.info "Enviado o botão " + number  
    EnviaComando(number)
    
}


def EnviaComando(buttonnumber) {
    
    def URI = "http://" + state.currentip + "/api/device/deviceDetails/smartHomeAutoHttpControl?serialNum=" + state.serialNum + "&verifyCode="  + state.verifyCode + "&cId=" + state.cId + "&state=" + buttonnumber + "&rcId=" + state.rcId        
    httpPOSTExec(URI)
    log.info "HTTP" +  URI 
    
    switch(buttonnumber)
    {
        case "1":
            tempStatus = "up"
            break
        case "2":
            tempStatus = "stop"
            break
        case "3":
            tempStatus = "down"
            break
        case "4":
            tempStatus = "paused"
            break
    }        
    sendEvent(name: "status", value: tempStatus)
    sendEvent(name: "curentstatus", value: tempStatus)    
    
    
}



def httpPOSTExec(URI)
{
    
    try
    {
        getString = URI
        segundo = ""
        httpPost(getString.replaceAll(' ', '%20'),segundo,  )
        { resp ->
            if (resp.data)
            {
                        log.info "Response " + resp.data 
            }
        }
    }
                            

    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
    }
    
}


//DEBUG
private logDebug(msg) {
  if (settings?.debugOutput || settings?.debugOutput == null) {
    log.debug "$msg"
  }
}



