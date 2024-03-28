/**
 *  MolSmart GW3 Driver - RF
 *
 *  Copyright 2024 VH 
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
 *
 *
 *   +++  Versão para enviar Códigos SENDIR Diretamente no botão ++++
 */

metadata {
  definition (name: "MolSmart GW3 - RF", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Contact Sensor"
    capability "Sensor"
    capability "Switch"  
    capability "PushableButton"  

  }
      
  }

  preferences {
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        //input name: "ipdomodulo", type: "string", title: "Mol IP", defaultValue: ""     
      
  }   
  

def initialized()
{
    state.currentip = ""  
    log.debug "initialized()"
    
}

def installed()
{
   

    sendEvent(name:"numberOfButtons", value:3)   
    sendEvent(name: "status", value: "stop")   
    log.debug "installed()"
    
}

def updated()
{
   
    sendEvent(name:"numberOfButtons", value:3)    
    log.debug "updated()"
    
}



def on() {
     //sendEvent(name: "switch", value: "on", isStateChange: true)
     
}

def off() {
     //sendEvent(name: "switch", value: "off", isStateChange: true)
     
}

def push(number) {
    sendEvent(name:"pushed", value:number, isStateChange: true)
    log.info "Pushed cortina" 
    EnviaComando(number)

}

def AtualizaDadosGW3(ipADD,TempserialNum,TempverifyCode,TempcId,TemprcId) {
    state.currentip = ipADD
    state.serialNum = TempserialNum
    state.verifyCode = TempverifyCode
    state.cId = TempcId
    state.rcId = TemprcId
    log.info "Dados do GW3 atualizados: " + state.currentip + " -- " + state.serialNum + " -- " +  state.verifyCode + " -- " + state.cId + " -- " +  state.rcId
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
                       
                        //def resp_json
                        //def coverfile
                        //resp_json = resp.data
                        //coverfile = resp_json.track.album.image[1]."#text"
                        log.info "Response " + resp.data 
                       
                                  
            }
        }
    }
                            

    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
    }
    
}



