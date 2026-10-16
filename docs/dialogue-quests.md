# Diálogos, missões e desbloqueios

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

## Renderer pendente

Criar `DialogueRenderer` Canvas: card inferior responsivo, retrato e nome do falante, quebra de linha por largura, texto progressivo, indicador de continuação, abertura/desaparecimento suaves. Separar assets de retrato dos sprites de movimento. O card deve capturar toques antes dos controles de combate e pausar o mundo de forma configurável; não confundir com pausa de Activity. Oferecer opções de resposta e ramificações em fase posterior.

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

- `Game.java` instancia `DialogueManager`, `DialogueRenderer` e `QuestJournal`.
- Botão **FALAR (NPC)** desenhado no topo abre conversa de demonstração com Ancião da Floresta; **não há entidade NPC no mapa nem interação por proximidade ainda**.
- `DialogueRenderer` desenha card animado, nome, retrato-placeholder, quebra de linha e texto progressivo; não usa PNG de retrato.
- Durante o diálogo, `Game.update()` atualiza somente a conversa; toque no card completa ou avança falas.
- A missão de demonstração `first_ghosts` exige 3 mortes de fantasmas ácidos. `EnemyManager` emite evento na transição de vivo para morto por projétil ou Tempestade de Cocos.
- O evento final marca `REWARDED` e mostra aviso textual **História arcana descoberta**; **não concede habilidade, item ou magia real**.
- **Tudo permanece em memória:** missões, recompensa e diálogo são reiniciados ao reabrir a jogatina; saves Room ainda armazenam somente posição e vida. Não há migração de banco nesta etapa.
- Próximos passos: NPC no mapa com detecção de proximidade; transições de fechamento; escolhas ramificadas; persistência Room por slot com transação atômica de recompensa; assets de retrato; testes em Android.

**Atenção:** a integração foi commitada, mas não compilada/testada em aparelho nesta sessão.
