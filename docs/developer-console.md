# Console de desenvolvimento

Console interno para testes de QA e desenvolvimento. O botão **Console de desenvolvimento** aparece no menu de pausa apenas quando `ApplicationInfo.FLAG_DEBUGGABLE` está ativo. `Game.runDevCommand()` também rejeita chamadas em builds não depuráveis.

## Comandos
| Comando | Efeito |
| --- | --- |
| `help` | Lista a sintaxe |
| `pos` | Exibe coordenadas atuais |
| `locais` | Lista pontos nomeados |
| `tp inicio` | Teleporte por nome |
| `tp 1000 500` | Teleporte por coordenadas do mundo |
| `god on` / `god off` | Invulnerabilidade |
| `heal` | Vida e mana completas |
| `give coco` | Equipa arma inicial no slot 1 |
| `give cajado` | Equipa arma mágica de teste no slot 2 |
| `spawn fantasma 5` | Invoca cinco fantasmas normais |
| `spawn sentinela` | Invoca miniboss |
| `spawn espectro` | Invoca boss |
| `clear` | Limpa inimigos e projéteis sem recompensas |
| `level 10` | Avança até dez níveis, respeitando limite 100 |

## Organização
- `dev/DevConsole`: interpretador desacoplado do Android, com interface `Commands`.
- `dev/DevLocations`: catálogo de coordenadas nomeadas; IDs iniciais: `inicio`, `pantano`, `clareira`. Coordenadas são provisórias e devem ser ajustadas ao mapa definitivo.
- `Game.runDevCommand`: adaptador para jogador, progressão, equipamentos e `EnemyManager`.
- `MainActivity.showDevConsole`: diálogo de comando no menu de pausa, sem bloquear o loop do jogo.
- `Player.setInvulnerable`: intercepta dano no caminho central `receiveHit`; é estado transitório e não é salvo.
- `EnemyManager.spawnNormalForDebug` e `clearForDebug`: ações de teste independentes de spawn ambiente e eventos de recompensa.

## Limitações
- `give` equipa diretamente em slots; **não há inventário real** nem persistência de equipamentos.
- Bosses de teste não persistem entre sessões e ainda usam sprites de fantasma.
- Teleporte por nome consulta uma tabela estática, não um sistema de regiões ou pontos de viagem descobertos.
- Os comandos não são persistidos em histórico; apenas a última resposta aparece no diálogo.
- `level` altera a progressão normal e pode ser salva; use um slot separado de QA. `god` não é persistido.
- O sistema ainda não inclui `quest`, `item` genérico, `weather`, `noclip`, `time`, telemetria ou logs exportáveis.

## Testes pendentes
Validar compilação Android, visibilidade somente em debug, pausa segura, coordenadas válidas, colisões após teleporte, limites de spawn, comportamento dos bosses, salvamento de progressão e ausência de comandos no APK release. Nenhum build foi executado nesta sessão.

## Monitor de desempenho Android

Comandos disponíveis em builds debug:
- `debug on`: ativa overlay com FPS, UPS, duração média de update/draw, contagem de inimigos, projéteis e magias, posição do jogador, heap Java, heap nativo, RAM disponível, núcleos de CPU, ticks de CPU do processo por segundo (quando legíveis), bateria, temperatura da **bateria** e estado térmico do sistema.
- `debug off`: desativa o overlay e a amostragem periódica.
- `debug info`: mostra uma fotografia das métricas no console. Para FPS/UPS úteis, executar após jogar alguns segundos, pois o jogo fica pausado durante o console.

`dev/DevicePerformanceMonitor` amostra métricas de sistema no máximo uma vez por segundo. O loop já calcula FPS, UPS e tempos médios. Estado térmico é fornecido por `PowerManager` em Android 10+; em versões anteriores aparece como indisponível. Temperatura é a **bateria**, não CPU/GPU. Ticks de CPU não são porcentagem e só aparecem quando `/proc/self/stat` é legível. A métrica de memória nativa é estimativa da heap nativa, não toda a memória do processo. O overlay fica desativado por padrão e não existe em builds não depuráveis.

**Pendente:** build Android, teste em aparelho real, teste de telas pequenas e conferência da coleta em diferentes fabricantes.
