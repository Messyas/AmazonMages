# Arquitetura modular e roteiro de validação

Referência: 2026-10-16, branch `fix/android-performance`, base `3f4ea25`.
Não houve alteração, push ou merge na `main`. Java, Android nativo e Canvas 2D foram mantidos.
Nenhuma dependência de runtime foi adicionada.

**Integrado** significa que o fluxo normal do jogo usa o sistema. A validação física continua
pendente: não há dispositivo Android conectado neste ambiente. Não confundir compilação do APK
instrumentado com execução dos testes Android.

## Funcionalidades e commits

| Fase / módulo | Estado real | Testes / evidência | Commits principais |
| --- | --- | --- | --- |
| 1. Auditoria e estabilização | Compilado; unitários aprovados; Android pendente | Build debug, 26 unitários, lint sem erros; testes de banco/combate compilados | `2d273ba`, `80edb9b`, `53e17db`, `3d762e6` |
| 2. Modularização de Game | Extrações integradas, incrementais | `GameRenderer` desenha mundo/pickups; `InputController` processa ponteiros; `SaveCoordinator` captura snapshots | `30daacc`, `71fdcd8` |
| 3. ECS | Jogador/projéteis hostis/pickups integrados; inimigos com adaptador ECS | `WorldTest`, `MovementSystemTest`, `LifetimeSystemTest`, colisões, scheduler; `CombatIntegrationTest` compilado | `4e11642`, `15eb228`, `55a869d` |
| 4. Inventário e consumíveis | Painel acessível pelo HUD, quantidades/descrições/seleção/uso; pilhas limitadas; aviso de capacidade | `InventoryTest`, `ConsumableServiceTest`, `GameplayEffectTest`, `SaveCoordinatorTest` | `586d1b5`, `b715d42`, `71fdcd8`, `cf1eaef` |
| 4. Equipamentos | Quatro slots, troca, armas únicas por slot, coco inicial e cajado utilizável | `WeaponLoadoutTest`; testes Android de transação e isolamento compilados | `586d1b5`, `71fdcd8`, `869f1e9`, `cf1eaef` |
| 4. Loot e chão | Tabelas por espécie, morte única, ECS, persistência, capacidade 64 e expiração | `DropTableTest`, `PickupSystemTest`, restauração repetida; combate Android compilado | `586d1b5`, `55a869d`, `869f1e9`, `cf1eaef` |
| 5. Recursos | Cache central de bitmaps, 16 MiB; áudio existente preservado e concorrência corrigida | `ResourceManagerTest` compilado; lint; sem medição em dispositivo | `9bd41ac` |
| 6. Estados/cenas | Menu, loading, jogo, pausa, diálogo, inventário, atributos e morte integrados | `GameStateManagerTest`; ciclo de vida físico pendente | `b715d42`, `71fdcd8`, `cf1eaef` |
| 7. Efeitos | Dano existente preservado; cura/mana/buff reutilizáveis; lentidão temporária integrada | `DamageResolverTest`, `GameplayEffectTest` | `b715d42` |
| 8. Spawn e mundo | Região/configuração, população, distância, despawn, escalonamento; Sentinela acionada pela missão | `SpawnDirectorTest`, combate Android compilado; testar encontro manualmente | `15eb228`, `71fdcd8` |
| 9. Persistência coordenada | Seis tabelas no snapshot; I/O global ordenado; callbacks encerrados descartados | `SaveCoordinatorTest`; migrações/round trip/rollback/exclusão Android compilados | `80edb9b`, `586d1b5`, `e24b572`, `869f1e9`, `cf1eaef` |
| 10. Interfaces | Primitivas Canvas para painéis, botões, barras e texto; lista/seleção, mana/cooldown, joystick proporcional | `InputControllerTest` compilado; lint; testes físicos de legibilidade pendentes | `586d1b5`, `71fdcd8` |
| 11. Revisão/documentação | Relatório, instruções e pendências registrados | [Relatório](validation/2026-10-16.md), [checklist Android](testing-checklist.md) | Commit de documentação desta entrega |

As fases 6–8 foram desenvolvidas com ajustes por dependência: o spawn foi isolado junto ao registro
ECS; a transação de save foi antecipada para estabilizar o consumo; o schema de equipamentos/chão
foi criado junto à interface que os utiliza. Cada incremento foi compilado e testado antes da revisão final.

## Responsabilidades e estado autoritativo

- `Game`: integração Android, composição dos módulos, coordenação de atualização e HUD legado.
  A extração foi gradual; não houve reescrita completa e ainda existem métodos de HUD em `Game`.
- `GameLoop`: timestep fixo, catch-up limitado, medições FPS/UPS/update/draw, uma thread por Surface ativa.
- `GameRenderer`: mapa, jogador, inimigos, projéteis e pickups; Paint e buffers reutilizados.
- `InputController`: ponteiros independentes, cancelamento, reset modal e fila de ataques limitada a 8.
- `GameStateManager`: preserva a cena modal sobre pausa/retomada; loading e morte bloqueiam ações.
- `SaveCoordinator`: snapshots de jogador, progressão, missões, inventário, armas e chão.
- `EnemyRegistry`: entidades ECS com `EnemyComponent` que referencia o modelo legado. Posição e vida
  do inimigo **não** são copiadas para Transform/Health; IA, animação e dano leem o mesmo `Enemy`.
- Projéteis: Transform/Velocity são autoritativos; objetos Canvas recebem apenas espelhamento para desenho.
  Cada projétil é removido antes do dano. A colisão antiga de magias permanece como API legada,
  mas não é executada pelo loop junto ao sistema ECS.
- `EnemyManager.reportDefeat` reivindica a derrota uma vez no registro ECS. Fusão/despawn/limpeza
  removem entidades sem passar por recompensas. Eventos de derrota carregam ID ECS real.
- `SpawnDirector`: conteúdo/timing/capacidade/região e política de atualização por distância.
- `ResourceManager`: cache de bitmaps por recurso/escala; nunca retém Activity. Remover do cache não
  recicla bitmaps ainda usados pelos renderizadores. O orçamento limita o cache, não todos os assets ativos.
  O `GameAudioManager` continua centralizando SoundPool/MediaPlayer e seu ciclo de vida.

## Regras de itens e encontros

- Catálogo: erva (cura 2), essência (mana 25), bênção (velocidade/defesa por 300 ticks), coco e cajado.
- Uso ocorre pelo botão INVENTARIO no HUD. Cura fracionária respeita a vida real e não revive mortos.
  Vida/mana cheias e buffs equivalentes ainda ativos não gastam itens.
- Uma arma possuída pode ocupar um slot por vez. Seleção ignora slots vazios. Coco permanece no
  inventário e pode ser reequipado; é concedido ao restaurar saves antigos que ainda não o continham.
- Drops são rolados somente em derrotas reais, antes do snapshot de recompensa/XP/missão.
  Chances e intervalos de quantidade ficam em `DropTable`. Limpeza debug não gera drops nem XP.
- Cada mapa/slot retém no máximo 64 pilhas; drops novos são descartados quando esse limite é atingido.
  Pilhas não aceitas pelo inventário permanecem no chão. Cada pilha expira após 36.000 ticks
  de simulação ativa (10 minutos); pausa, diálogo e inventário interrompem essa contagem.
- O save substitui todo o conjunto de pickups. O load limpa o World antes de restaurar, ignora IDs,
  quantidades, duração ou coordenadas inválidas e limita pilhas/capacidade. Não soma dados ao estado anterior.
- A missão do NPC preserva XP/recompensas existentes e agora solicita a Sentinela ao reivindicar a recompensa.
  `sentinel_encounter` registra o encontro pendente/concluído na tabela de missões existente.
  Ao carregar um encontro pendente, o chefe recomeça com vida cheia próximo ao jogador;
  após a derrota registrada ele não é recriado. Outros chefes continuam disponíveis pelo contrato de evento/encounter.
- Inimigos ambientais, projéteis, tempestade, cooldowns e buffs/debuffs não são persistidos;
  são estado transitório da sessão. Não houve remoção de funcionalidade anterior.

## Room e preservação de saves

Versão atual: **7**. Cadeia explícita: **1→2→3→4→5→6→7**.
Não há `fallbackToDestructiveMigration`.

| Migração | Alteração |
| --- | --- |
| 1→2 | Adiciona `health INTEGER NOT NULL DEFAULT 5` |
| 2→3 | Cria `quest_progress`, chave slot/missão |
| 3→4 | Cria `player_stats`, uma linha por slot |
| 4→5 | Cria `inventory_items`, chave slot/item |
| 5→6 | Reconstrói apenas `player_positions`, copiando registros e normalizando `health DEFAULT 5` |
| 6→7 | Cria `equipment` (slot/posição da arma) e `world_pickups` (slot/ID da pilha) |

A versão 6 é necessária porque bancos novos v2–v5 não tinham o default da migração 1→2.
Alterar só a anotação em v5 mudaria a identidade Room sem migrar saves já existentes.
A reconstrução roda na transação de upgrade do Room, sem alterar coordenadas, slots, vida ou outras tabelas.
A versão 7 mantém as quatro tabelas antigas e acrescenta dados novos, inicialmente vazios.

`GameDatabaseTest` monta cada versão 1–6 e abre o resultado pelo Room atual, incluindo validação
real de schema. Há fixture adicional de v5 sem default, testes de fresh install, rollback e exclusão isolada.
Esses testes **compilaram, mas não foram executados** sem dispositivo Android.

## Threads e consistência

- Entrada, update, draw e captura de save compartilham o monitor de `Game`.
- Pausa/destruição da Surface fazem `join` sem segurar esse monitor, evitando deadlock.
- Carga lê as seis tabelas numa transação antes de liberar o loop. Falha mantém gameplay bloqueado.
- Repositórios do menu e das partidas compartilham uma fila I/O: abrir o slot depois de fechar a partida
  observa os saves anteriores na ordem de submissão, sem competir entre executores independentes.
- `close` é idempotente, oferece conclusão opcional de descarte, rejeita novos trabalhos e suprime callbacks antigos, mas deixa os saves já
  enfileirados terminarem antes de fechar a instância Room.
- Snapshot completo substitui missões/inventário/equipamentos/pickups e grava jogador/atributos atomicamente.
- As APIs públicas anteriores foram preservadas. A UI diz “salvamento solicitado”, sem afirmar sucesso do I/O.

## Problemas encontrados e corrigidos

1. Sombreamento ilegal de parâmetro em callbacks aninhados de inventário: removido pelo load transacional.
2. Incompatibilidade do default de vida entre bancos novos e migrados: migração 5→6 explícita.
3. Falha de load tratada como partida vazia, com risco de sobrescrita: agora bloqueia o início.
4. Callbacks tardios e fechamento repetido causando submissão a executor encerrado: descarte/idempotência.
5. UI/loop sem sincronização sobre inventário, input e atributos: monitor compartilhado.
6. Movimento/ataques pendentes após pausa, diálogo ou cancelamento: reset centralizado.
7. Vida e item consumido gravados separadamente: snapshot de todas as tabelas na mesma transação.
8. Fila independente de menu/partida permitindo leitura antes de flush: ordenação global.
9. Eventos de coleta sempre identificados como erva: agora usam o ID real do item.
10. Possível duplicação de arma em vários slots: equipamento move a instância única.
11. Instanciação de Paint por coco/quadro e por overlay: reutilização; consultas ECS/scheduler/bus usam buffers/snapshots estáveis.
12. Caches estáticos de assets sem limite/ciclo de vida: cache central e referências de sessão.
13. Joystick fixo e câmera desconectada do tamanho real da Surface: layout/resize proporcionais.
14. Game Over impedia revival pelo comando debug de cura após introduzir estados: revival explícito preserva o console.
15. API de InstrumentationRegistry antiga e tentativa de try-with-resources com Room 2.6:
    atualizadas conforme os erros reais encontrados na compilação dos testes.

## Pendências de validação e limites

- Executar testes instrumentados em Android API 28 e API 34; validar migrações sobre cópias de saves reais.
- Validar multitoque, rotação, troca de app, tela bloqueada, retorno ao menu e exclusão durante I/O.
- Testar legibilidade em telas pequenas e densidades diferentes. HUD antigo ainda pode receber melhorias de layout.
- Medir FPS, UPS, tempos, alocações, memória e custo de colisão em aparelho modesto. Não há números de desempenho físico nesta entrega.
- Vida continua inteira no save, como no formato anterior; frações de dano são mantidas durante combate.
- `exportSchema` permanece desabilitado, como na base; fixtures históricas dos testes documentam os contratos.
- Persistência de HP/fase exatos de chefes, efeitos e cooldowns entre sessões é uma expansão futura, distinta da regra de reinício documentada.
- Assets específicos, animações finais de armas, VFX/SFX de tempestade e artes de fantasmas continuam no backlog artístico anterior.
- Lint: 31 avisos, zero erros. Não foram atualizadas dependências/ícones apenas para silenciar avisos.

Esses pontos não foram marcados como validados nem como conclusão integral em dispositivo.
