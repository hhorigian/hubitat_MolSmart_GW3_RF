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
 *  No primeiro uso, rode open até 100% e cronometre; preencha openTimeMs.
 *  Rode close até 0% e cronometre; preencha closeTimeMs.
 *  Ajuste tickMs (400 ms é bom) e tolerance (3–5%).
 *  Use o slider — o driver envia subir/descer, espera o tempo calculado e manda Parar sozinho.
 *
 *
 *   +++  Versão para enviar Códigos SENDIR Diretamente no botão ++++
 *        1.0
 *        1.1 - 13/06/2024 - Added User Manual Link 
 *        1.2 - 29/09/2025 - Changed to ASYNC Http method  
 *        1.3 - 05/10/2025 - Added Sliding possibility
 *  
 */


import groovy.transform.Field

metadata {
  definition (name: "MolSmart - GW3 - RF (Shades c/ Slider)", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Sensor"
    capability "Actuator"

    // Mantidos do driver original (não retirar para evitar quebra de dashboards/automations existentes)
    capability "Contact Sensor"
    capability "PushableButton"
    capability "WindowBlind"               // legado / compat

    // Adicionados para o slider de cortina (Hubitat Dashboard Shade)
    capability "Window Shade"              // open, close, pause/stop, setPosition, start/stopPositionChange

    // Comandos legados (mantidos)
    command "Up"
    command "Down"
    command "Stop"

    // Atributos auxiliares
    attribute "currentstatus", "string"    // legado
    attribute "status", "string"           // legado
    attribute "position", "NUMBER"         // 0..100 (para o tile Shade)
    attribute "moving", "ENUM", ["up","down","stopped"]
  }

  preferences {
    // === Gateway (mantidos exatamente como no seu driver) ===
    input name: "molIPAddress", type: "text",   title: "MolSmart IP",                  required: true, defaultValue: "192.168.1.100"
    input name: "serialNum",    type: "string", title: "N Serie (Etiqueta GW3)",       required: true
    input name: "verifyCode",   type: "string", title: "Verify code (Etiqueta GW3)",   required: true
    input name: "cId",          type: "string", title: "Control ID (pego no iDoor)",   required: true
    //input name: "rcId",       type: "string", title: "RCID (51=RF)",                  required: false, defaultValue: "51"
    input name: "logEnable",    type: "bool",   title: "Enable debug logging",         defaultValue: false

    // === Temporização/estimativa para slider ===
    input name: "openTimeMs",   type: "number", title: "Tempo para ABRIR 0→100 (ms)",  defaultValue: 12000, required: true
    input name: "closeTimeMs",  type: "number", title: "Tempo para FECHAR 100→0 (ms)", defaultValue: 12000, required: true
    input name: "settleMs",     type: "number", title: "Tempo extra após PARAR (ms)",  defaultValue: 150,   required: true
    input name: "tickMs",       type: "number", title: "Intervalo de atualização (ms)",defaultValue: 400,   required: true
    input name: "tolerance",    type: "number", title: "Tolerância de posição (%)",    defaultValue: 3,     required: true
    input name: "invertOpenClose", type: "bool", title: "Inverter sentido (Open/Close)", defaultValue: false
  }
}

/* ======================= Setup / Estado ======================= */
def installed() {
  sendEvent(name:"numberOfButtons", value:4)
  sendEvent(name:"status", value:"stop")
  state.rcId = 51
  initialize()
}

def updated() {
  sendEvent(name:"numberOfButtons", value:4)
  state.rcId = 51
  initialize()
  if (logEnable) runIn(1800, logsOff)
}

private initialize() {
  unschedule()
  state.currentip   = settings.molIPAddress
  state.serialNum   = settings.serialNum
  state.verifyCode  = settings.verifyCode
  state.cId         = settings.cId
  state.rcId        = 51
  state.lastKnownPos = (state.lastKnownPos == null) ? 0 : clamp(state.lastKnownPos as int, 0, 100)
  state.targetPos    = state.targetPos ?: state.lastKnownPos
  state.moving       = "stopped"
  sendEvent(name:"position", value: state.lastKnownPos as int, isStateChange:true)
  sendShadeEventForPos(state.lastKnownPos as int)
  sendEvent(name:"moving", value:"stopped", isStateChange:true)
  if (logEnable) log.debug "Init -> ip=${state.currentip} sn=${state.serialNum} cId=${state.cId} pos=${state.lastKnownPos}"
}

/* ======================= Comandos Legados (mantidos) ======================= */
def Up()   { EnviaComando(1); trackStart("up") }
def Down() { EnviaComando(3); trackStart("down") }
def Stop() { EnviaComando(2); finalizeStop(estimateNow()) }

/* ======================= Capabilities de Shade ======================= */
def open()                { moveTo(100) }
def close()               { moveTo(0) }
def pause()               { stopPositionChange() }   // alias para compat
def stopPositionChange()  {
  if (state.moving in ["up","down"]) {
    EnviaComando(2)
    runIn(calcSec((settleMs ?: 150) as int), "onManualStopSettle")
  } else {
    sendEvent(name:"moving", value:"stopped")
    sendShadeEventForPos(state.lastKnownPos as int ?: 0)
  }
}
def startPositionChange(direction) {
  // direction: "opening"/"open"/"up"  ou  "closing"/"close"/"down"
  def dir = (direction in ["open","opening","up"]) ? "up" : "down"
  EnviaComando(dir == "up" ? 1 : 3)
  trackStart(dir)
}
def setPosition(Number pos) { moveTo(clamp((pos as int), 0, 100)) }

/* ======================= Lógica de Tempo/Slider ======================= */
private moveTo(Integer target) {
  Integer current = estimateNow()
  Integer tol = (tolerance ?: 3) as int
  if (Math.abs(target - current) <= tol) {
    if (logEnable) log.debug "Dentro da tolerância (current=${current}, target=${target})"
    finalizeStop(target)
    return
  }

  String dir = (target > current) ? "up" : "down"
  Integer totalMs = (dir == "up" ? (openTimeMs ?: 12000) : (closeTimeMs ?: 12000)) as int
  BigDecimal fraction = (Math.abs(target - current) / 100.0)
  Integer runMs = Math.max(50, (int)Math.round(totalMs * fraction))

  if (logEnable) log.debug "moveTo: current=${current}, target=${target}, dir=${dir}, runMs=${runMs}"

  // Envia mover e agenda parar
  // Primeiro, assegura que o tile está na posição atual (evita pular para ~100%)
  sendPositionNow(current)
  EnviaComando(dir == "up" ? 1 : 3)
  trackStart(dir)
  state.targetPos = target

  state.pendingStop = true
  runIn(calcSec(runMs), "onMoveTimeout")
}

private trackStart(String dir) {
  state.moving = dir
  state.moveStartEpoch = now()
  state.moveStartPos   = estimateNow()
  sendEvent(name:"moving", value: dir, isStateChange:true)
  sendEvent(name:"windowShade", value: (dir == "up" ? "opening" : "closing"), isStateChange:true)
  // Envia posição inicial para evitar "salto" visual no dashboard (forçado)
  sendPositionNow(state.moveStartPos as int)
  scheduleTick()
}

private tick() {
  def est = estimateNow()
  sendPosition(est)
  scheduleTick()
}

private scheduleTick() {
  unschedule("tick")
  Integer t = Math.max(200, (tickMs ?: 400) as int)
  runIn(calcSec(t), "tick")
}

private Integer estimateNow() {
  if (!(state.moving in ["up","down"])) return (state.lastKnownPos ?: 0) as int
  if (!state.moveStartEpoch || state.moveStartPos == null) return (state.lastKnownPos ?: 0) as int

  Long elapsed = now() - (state.moveStartEpoch as Long)
  Integer totalMs = (state.moving == "up" ? (openTimeMs ?: 12000) : (closeTimeMs ?: 12000)) as int
  if (totalMs <= 0) return (state.lastKnownPos ?: 0) as int

  BigDecimal deltaPct = (elapsed / (totalMs as BigDecimal)) * 100.0
  Integer est = (state.moving == "up")
      ? Math.min(100, Math.round((state.moveStartPos as int) + deltaPct) as int)
      : Math.max(0,   Math.round((state.moveStartPos as int) - deltaPct) as int)

  if (state.targetPos != null) {
    Integer tgt = state.targetPos as int
    if (state.moving == "up")   est = Math.min(est, tgt)
    if (state.moving == "down") est = Math.max(est, tgt)
  }
  return est as int
}

private sendPosition(Integer pos) {
  pos = clamp(pos as int, 0, 100)
  if (pos != (state.lastKnownPos ?: -1)) {
    state.lastKnownPos = pos
    sendEvent(name:"position", value: pos)
    sendShadeEventForPos(pos)
    if (logEnable) log.debug "tick -> pos=${pos}"
  }
}

private sendPositionNow(Integer pos) {
  pos = clamp(pos as int, 0, 100)
  state.lastKnownPos = pos
  sendEvent(name:"position", value: pos, isStateChange:true)
  sendShadeEventForPos(pos)
  if (logEnable) log.debug "sendPositionNow -> pos=${pos}"
}


private finalizeStop(Integer finalPos) {
  unschedule("tick")
  Integer pos = clamp(finalPos as int, 0, 100)
  state.lastKnownPos = pos
  state.moving = "stopped"
  sendEvent(name:"position", value: pos, isStateChange:true)
  sendEvent(name:"moving", value:"stopped", isStateChange:true)
  sendShadeEventForPos(pos)
  if (logEnable) log.debug "finalizeStop -> pos=${pos}"
}

private sendShadeEventForPos(Integer pos) {
  String shade = (pos <= 0) ? "closed" : (pos >= 100 ? "open" : "partially open")
  sendEvent(name:"windowShade", value: shade, isStateChange:true)
}

/* ======================= Envio HTTP — MANTIDO ======================= */
private String buildFullUrl(button) {
  def ip   = settings.molIPAddress
  def sn   = settings.serialNum
  def vc   = settings.verifyCode
  def cid  = settings.cId
  def rcid = (settings.rcId ?: "51")
  return "http://${ip}/api/device/deviceDetails/smartHomeAutoHttpControl" +
         "?serialNum=${sn}&verifyCode=${vc}&cId=${cid}&state=${button}&rcId=${rcid}"
}

def EnviaComando(button) {
  settings.timeoutSec = 7
  String fullUrl = buildFullUrl(button)
  if (logEnable) log.info "FullURL = ${fullUrl}"
  Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]

  try {
    asynchttpPost('gw3PostCallback', params, [cmd: button])
    // Atualiza atributos legados rapidamente (sem esperar resposta)
    String tempStatus = (button == 1) ? "up" : (button == 2 ? "stop" : (button == 3 ? "down" : "paused"))
    sendEvent(name: "status", value: tempStatus)
    sendEvent(name: "currentstatus", value: tempStatus) // legado
  } catch (e) {
    log.warn "${device.displayName} Async POST scheduling failed: ${e.message}"
  }
}

void gw3PostCallback(resp, data) {
  String cmd = "${data?.cmd}"
  try {
    if (resp?.status in 200..299) {
      if (logEnable) log.debug "POST OK (async) cmd=${cmd} status=${resp?.status}"
      state.ultimamensagem = "Resposta OK"
    } else {
      log.warn "POST error (async) status=${resp?.status} cmd=${cmd}"
      state.ultimamensagem = "Erro no envio do comando"
    }
  } catch (e) {
    log.warn "Async callback exception: ${e.message} (cmd=${cmd})"
    state.errormessage = e.message
  }
}


/* ======= Callbacks para agendamento em segundos (compat sem runInMillis) ======= */
def onMoveTimeout() {
  // Dispara comando de PARAR após o tempo calculado e agenda o settle
  EnviaComando(2)
  runIn(calcSec((settleMs ?: 150) as int), "onSettleTimeout")
}

def onSettleTimeout() {
  // Finaliza no target desejado (state.targetPos)
  Integer tgt = (state.targetPos != null) ? (state.targetPos as Integer) : estimateNow()
  finalizeStop(tgt)
}

def onManualStopSettle() {
  // Stop manual via stopPositionChange()
  finalizeStop(estimateNow())
}

private Integer calcSec(Integer ms) {
  Integer s = Math.round((ms ?: 0) / 1000.0) as Integer
  return Math.max(1, s)
}


/* ======================= Util ======================= */
private Integer clamp(int v, int lo, int hi) { Math.max(lo, Math.min(hi, v)) }

def logsOff() {
  log.warn 'logging disabled...'
  device.updateSetting('logEnable', [value:'false', type:'bool'])
}
