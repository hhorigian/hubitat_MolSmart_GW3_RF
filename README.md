# hubitat_MolSmart_GW3_RF


Esse é o driver para utilizar o Gateway da MolSmart GW3 com RF.
No caso, esse é o driver + app, para utilizar a função do RF do Gateway.

Instalar o código do Driver Hub.
Instalar o código do APP no Hub.
Antes de seguir para o passo 3, recomendamos já ter o Gateway instalado na rede, cadastrado no APP iDoor, e criado/aprendido o "Controle Remoto" como informa o manual

Manual de instruções do GW: https://bit.ly/manualgw3
Manual/API de integração do GW: https://bit.ly/apigw3
Video de instalação do GW: https://youtu.be/EIgOz2DrALA?si=8kOFyFPpvq7FjPaY
Dicas de integração do GW: https://youtu.be/Ex9b1RNuMUs?si=Kt0DbUi9_nxtS-E5

Instalar um novo "APP de usuário" no Hub, e seleccionar o MolSmart GW3 (RF). a) Ingressar os dados do Gateway:Endereço IP - sempre recomendado fixar o IP, ou reservar no DHCP); Numero de Serie; Código de Verificação (esses dados estão na etiqueta na parte de baixo do gateway);CiD;RCid. Esses dados já tem que ser do app iDoor.

Por default, o driver vai criar um Device no Hub, com 3 Pushable Buttons.
O Botão = 1, é para Subir
O Botão = 2, é para Stop
O Botão = 3, é para Baixar

Agora é só testar desde o Device, ou botar no Dashboard, usando o Device, e accionando o numero de botão para accionar.
