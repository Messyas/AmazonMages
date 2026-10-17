# Chat de comandos e diagnostico

Em builds `debug`, o icone de conversa no HUD abre o chat de comandos. O chat pausa o mundo enquanto esta aberto, aceita comandos com ou sem `/` e mantem o historico visivel durante a sessao. Ele nao envia mensagens a outros jogadores. Em builds release, o icone nao aparece e `Game.runDevCommand()` rejeita comandos.

O botao `FPS` no canto superior esquerdo do chat alterna o painel de desempenho. O painel aparece sobre o jogo quando o chat fecha e atualiza enquanto a partida esta ativa. Reabra o chat e toque em `FPS` para desligar.

## Comandos

| Comando | Efeito |
| --- | --- |
| `/help` | Lista a sintaxe |
| `/pos` | Exibe coordenadas atuais |
| `/locais` | Lista pontos nomeados |
| `/tp inicio` | Teleporte por nome |
| `/tp 1000 500` | Teleporte por coordenadas do mundo |
| `/god on` / `/god off` | Invulnerabilidade temporaria |
| `/heal` | Vida e mana completas |
| `/give coco` | Equipa arma inicial no slot 1 |
| `/give cajado` | Equipa arma magica de teste no slot 2 |
| `/spawn fantasma 5` | Invoca cinco fantasmas normais |
| `/spawn sentinela` | Invoca miniboss |
| `/spawn espectro` | Invoca boss |
| `/clear` | Limpa inimigos e projeteis sem recompensas |
| `/level 10` | Avanca ate dez niveis, respeitando limite 100 |
| `/sair` | Fecha o chat |

## Monitor de desempenho

O painel inclui FPS, UPS, duracao media de update/draw, contagem de inimigos, projeteis e magias, posicao do jogador, heap Java, heap nativo, RAM disponivel, nucleos de CPU, ticks de CPU do processo por segundo (quando legiveis), bateria, temperatura da bateria e estado termico do sistema.

`dev/DevicePerformanceMonitor` amostra metricas de sistema no maximo uma vez por segundo. Estado termico vem de `PowerManager` no Android 10+; em versoes anteriores aparece como indisponivel. Temperatura e da bateria, nao CPU/GPU. Ticks de CPU nao sao porcentagem. A heap nativa e uma estimativa, nao toda a memoria do processo.

O zoom da camera e opcional e percorre 1.0x, 1.25x, 1.5x, 1.7x e volta a 1.0x pelo botao no canto superior direito. A orientacao do app fica travada em paisagem.
