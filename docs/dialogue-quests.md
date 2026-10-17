# Diálogos, missões e desbloqueios

## Estado atual da interface

O atalho global `FALAR (NPC)` e a conversa demonstrativa do Ancião foram removidos: não existe uma entidade NPC posicionada para justificar essa interação. O renderer e os dados de diálogo ficam sem gatilho até haver NPC real com interação por proximidade. O diário continua mostrando as missões por slot.

## Implementação inicial

As classes Java de dados e controle foram adicionadas em `dialogue/` e `quest/`. **Não estão ligadas a Game.java, não desenham cards e não persistem em Room.**

- `DialogueLine`: speakerId, texto, expressão e evento opcional.
- `DialogueSequence`: sequência imutável de falas.
- `DialogueManager`: máquina de estado com surgimento de 15 ticks (~250 ms a 60 UPS), texto progressivo e toque para completar/avançar. Emite eventos ao concluir uma fala.
- `QuestDefinition`: id estável, título, tipo de evento objetivo, quantidade e chave de desbloqueio.
- `QuestJournal`: LOCKED -> AVAILABLE -> ACTIVE -> COMPLETED -> REWARDED. Só permite reivindicar recompensa uma vez por sessão; **a garantia após reinício requer transação persistente**.

## Fluxo pretendido

1. NPC dispara diálogo via identificador.
2. Jogador lê e avança; diálogo emite evento `quest.offer.<id>` ou `quest.accept.<id>` para um roteador de eventos (ainda não implementado).
3. Mundo emite eventos de gameplay, como `enemy.acid_ghost.defeated` (ainda não conectados).
4. Journal atualiza objetivo e disponibiliza conclusão.
5. Recompensa é reivindicada e registrada **na mesma transação Room** que atualiza inventário/desbloqueios; nunca conceder duas vezes.
6. Flags de diálogo e missões por slot determinam falas e interações futuras.

## Renderer e diário

`DialogueRenderer` pode desenhar um card inferior responsivo quando integrado a um NPC real. Atualmente não há conversa ativa no gameplay. O diário e o mapa compartilham o painel modal do HUD.

Continuam pendentes os retratos em PNG, escolhas/ramificações e interação com NPCs posicionados no mapa.

## Assets pendentes

- `portrait_player_neutral.png`, `portrait_player_surprised.png`
- `portrait_npc_elder_neutral.png`, `portrait_npc_elder_angry.png`
- Moldura/ornamento do card (opcional; pode ser desenhado via Canvas), ícone de continuar e efeitos sonoros de entrada, letra e confirmação.

## Critérios de aceite futuros

- Falas de Player e NPC com retratos e nomes alternados.
- Toque primeiro revela texto completo, segundo avança; texto longo quebra linhas sem ultrapassar a tela.
- Evento de missão não dispara duas vezes por toque repetido.
- Estado de missão persiste separadamente em cada um dos três saves.
- Recompensa e flag de missão são persistidas atomicamente.
- Gameplay não recebe toques durante card modal; loop continua desenhando o diálogo.
- Testes com 30/60 UPS, acessibilidade, fontes grandes e diferentes proporções de tela.

## Integração demonstrativa atual (outubro de 2026)

- `Game.java` mantem `QuestJournal`; `DialogueManager` e `DialogueRenderer` ficam isolados ate existir interacao real com NPC.
- Atualização da interface: o atalho **FALAR (NPC)** e a conversa demonstrativa foram removidos até haver uma entidade NPC no mapa e interação por proximidade.
- `DialogueRenderer` nao desenha nada no gameplay atual.
- A missão de demonstração `first_ghosts` exige 3 mortes de fantasmas ácidos. `EnemyManager` emite evento na transição de vivo para morto por projétil ou Tempestade de Cocos.
- O evento final marca `REWARDED` e mostra aviso textual **História arcana descoberta**; **não concede habilidade, item ou magia real**.
- **Tudo permanece em memória:** missões, recompensa e diálogo são reiniciados ao reabrir a jogatina; saves Room ainda armazenam somente posição e vida. Não há migração de banco nesta etapa.
- Próximos passos: NPC no mapa com detecção de proximidade; transições de fechamento; escolhas ramificadas; persistência Room por slot com transação atômica de recompensa; assets de retrato; testes em Android.

**Atenção:** a integração foi commitada, mas não compilada/testada em aparelho nesta sessão.

## Persistência Room v3 — implementação

- `QuestProgressEntity`: tabela `quest_progress`, chave composta `(slotId, questId)`, colunas `state` e `progress`.
- `QuestProgressDao`: leitura por slot, upsert e exclusão por slot.
- `GameDatabase` passa da versão 2 para 3; `GameSaveRepository.MIGRATION_2_3` cria a tabela sem destruir saves existentes.
- `QuestJournal.snapshot(slotId)` e `restore(rows)` fazem conversão com validação de estados e limites de progresso; IDs desconhecidos são ignorados.
- `Game` carrega posição e depois missões antes de iniciar o loop. Salva em eventos de objetivo/diálogo, no autosave e na pausa.
- `deleteSlot` apaga posição e missões em uma transação Room.
- **Limitação:** a recompensa demonstrativa ainda é somente uma flag de missão `REWARDED` e aviso de texto, sem item/habilidade real; transação de entrega de recompensa real fica para o inventário. Falhas de I/O são registradas em log, não notificadas no HUD.
- **Validar:** upgrade de base v1/v2 para v3, independência dos três slots, progresso parcial, estado REWARDED, exclusão, reinício e fechamento da Activity. Build Android não executado nesta sessão.
