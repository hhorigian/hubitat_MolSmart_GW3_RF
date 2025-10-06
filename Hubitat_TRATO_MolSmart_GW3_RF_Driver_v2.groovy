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
 *        1.0
 *        1.1 - 13/06/2024 - Added User Manual Link 
 *        1.2 - 29/09/2025 - Changed to ASYNC Http method  

 *
*/
metadata {
  definition (name: "MolSmart - GW3 - RF", namespace: "TRATO", author: "VH", vid: "generic-contact") {
//    capability "Switch"  
    capability "Contact Sensor"
    capability "Sensor"
    capability "PushableButton"    
	capability "WindowBlind"


	command "Up"
    command "Down"
    command "Stop"
	attribute "currentstatus", "string"
      

  }
      
  }


    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_GW3_RF"


    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }



  preferences {
        input name: "molIPAddress", type: "text", title: "MolSmart IP",   required: true, defaultValue: "192.168.1.100" 
    	input name: "serialNum", title:"N Serie (Etiqueta GW3)", type: "string", required: true
	    input name: "verifyCode", title:"Verify code (Etiqueta GW3)", type: "string", required: true
    	input name: "cId", title:"Control ID (pego no idoor)", type: "string", required: true  
    	//input name: "rcId", title:"RCID (51=RF)", type: "string", required: false, defaultValue: "51"  
        input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver")       
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
	if (logEnable) runIn(1800,logsOff)

    
}

def open(){
    EnviaComando(1)   
}

def Up(){
    EnviaComando(1)

}

def stop(){
    EnviaComando(2)

}


def close(){
    EnviaComando(3)
}

def Down(){
    EnviaComando(3)
}

def Stop(){
    EnviaComando(2)
}


def on() {
    EnviaComando(1)     
}

def off() {
    EnviaComando(3)     
}

def push(number) {
    sendEvent(name:"pushed", value:number, isStateChange: true)
    log.info "Enviado o botão " + number  
    EnviaComando(number)
    
}


private String buildFullUrl(button) {
    def ip   = settings.molIPAddress
    def sn   = settings.serialNum
    def vc   = settings.verifyCode
    def cid  = settings.cId
    def rcid = (settings.rcId ?: "51")
    // IMPORTANT: we deliberately do NOT URL-encode 'pw' here
    return "http://${ip}/api/device/deviceDetails/smartHomeAutoHttpControl" +
           "?serialNum=${sn}&verifyCode=${vc}&cId=${cid}&state=${button}&rcId=${rcid}"
    
}



def EnviaComando(button) {
    //if (!pw) return
	
    settings.timeoutSec  = 7    
    String fullUrl = buildFullUrl(button)
    log.info "FullURL = " + fullUrl

    // params: give only a 'uri' so Hubitat won't rebuild/encode the query
    Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]
    log.info "Params = " + params
	
        try {
            asynchttpPost('gw3PostCallback', params, [cmd: button])
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
        } catch (e) {
            log.warn "${device.displayName} Async POST scheduling failed: ${e.message}"
    }
      
 
}

void gw3PostCallback(resp, data) {
    String cmd = data?.cmd
    try {
        if (resp?.status in 200..299) {
            logDebug "POST OK (async) cmd=${cmd} status=${resp?.status}"
             state.ultimamensagem =  "Resposta OK"
        } else {
            logWarn "POST error (async) status=${resp?.status} cmd=${cmd}"
             state.ultimamensagem =  "Erro no envio do comando"
        }
    } catch (e) {
        logWarn "Async callback exception: ${e.message} (cmd=${cmd})"
        state.errormessage = e.message
    }
}



    
 


private logInfo(msg)  { if (settings?.txtEnable   != false) log.info  "${device.displayName} ${msg}" }
private logDebug(msg) { if (settings?.debugOutput == true)  log.debug "${device.displayName} ${msg}" }
private logWarn(msg)  { log.warn "${device.displayName} ${msg}" }


def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}



