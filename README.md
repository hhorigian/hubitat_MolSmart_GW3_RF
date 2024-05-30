# hubitat_MolSmart_GW3_RF


Esse é o driver para utilizar o Gateway da MolSmart GW3 com RF.
No caso, agora só precisa dele para utilizar a função do RF do Gateway.
<br>
1. Instalar o código do Driver Hub.<br>
2. Pode também achar ele no HPM (Hubitat Package Manager) e instalar ele desse jeito (recomendado para receber updates). 

Antes de seguir para o passo 3, recomendamos já ter o Gateway instalado na rede, cadastrado no APP iDoor, e criado/aprendido o "Controle Remoto" como informa o manual<br>
<br>
Manual de instruções do GW: https://bit.ly/manualgw3 <br>
Manual/API de integração do GW: https://bit.ly/apigw3 <br>
Video de instalação do GW: https://youtu.be/EIgOz2DrALA?si=8kOFyFPpvq7FjPaY <br>
Dicas de integração do GW: https://youtu.be/Ex9b1RNuMUs?si=Kt0DbUi9_nxtS-E5 <br>
<br><br>
3. Instalar o um novo DEVICE no Hub, e seleccionar o MolSmart GW3 (RF). a) Ingressar os dados do Gateway:Endereço IP - sempre recomendado fixar o IP, ou reservar no DHCP); Numero de Serie; Código de Verificação (esses dados estão na etiqueta na parte de baixo do gateway);CiD = Esses dados já tem que ser do app iDoor.
<br>
Por default, o driver vai criar um Device no Hub, com 3 Pushable Buttons. <br>
O Botão = 1, é para Subir <br>
O Botão = 2, é para Stop <br>
O Botão = 3, é para Baixar <br>
<br>
Agora é só testar desde o Device, na parte superior usando o "PUSH" e mandar 1, 2 ou 3 no PUSH,  ou adicionar o novo DEVICE no Dashboard. No caso do dashboard, precisa adicionar ele como tipo "Button" e adicionar ele 3 vezes, cada uma com o numero de botão para accionar com PUSH. No final vão aparecer 3 botões no dashboard. 
<br>
<img src="https://images2.imgbox.com/f3/d1/ucoRPeMO_o.png">
<br>
<img src="https://images2.imgbox.com/a0/3f/joOpkMgW_o.png">
