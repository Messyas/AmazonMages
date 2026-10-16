# Templates de inimigos, minibosses e bosses

## Objetivo
Reutilizar IA, combate, sprites e colisões dos inimigos comuns sem copiar classes inteiras para cada chefe.

## Estrutura
- `ai/EnemyRank`: NORMAL, ELITE, MINI_BOSS, BOSS.
- `ai/EnemyTemplate`: definição imutável com ID, nome, rank, comportamento, vida máxima, dano do projétil, raio, XP, evento de derrota e limiares de fases.
- `ai/EnemyTemplates`: catálogo de exemplos: ACID_GHOST (normal), SWAMP_SENTINEL (miniboss) e ANCIENT_WRAITH (boss).
- `gameobject/Enemy`: instancia templates, mantém fase atual e preserva IA, renderização e colisões existentes.
- `ai/EnemyManager.spawnEncounter(template, x, y)`: cria boss/miniboss sob demanda, impede instância duplicada do mesmo template enquanto viva e mantém chefes fora do despawn por distância. `activeBoss()` permite HUD.
- `Game.startBossEncounter(template, x, y)`: ponto de entrada para quests, gatilhos de arena ou cenas futuras. `drawBossHud` mostra nome, fase e vida.
- Derrotas geram IDs próprios e XP específico do template. Fantasmas comuns continuam alimentando a missão introdutória; bosses não a completam por engano.

## Exemplos
| Template | Rank | Vida | Dano | XP | Fases |
| --- | --- | ---: | ---: | ---: | --- |
| Fantasma Ácido | NORMAL | 4 | 1 | 10 | 1 |
| Sentinela do Pântano | MINI_BOSS | 28 | 2 | 80 | 2 (50%) |
| Espectro Ancestral | BOSS | 90 | 3 | 300 | 3 (70%, 35%) |

Limiar de fase: a fase é monotônica e avança quando a vida restante cai abaixo do percentual definido. Cada fase adicional aumenta a velocidade de movimento em 12% e o dano de projétil em 1 (valores provisórios). A IA base também possui modo enfurecido por vida baixa.

## Uso
Exemplo em um gatilho futuro de missão/arena:

```java
startBossEncounter(EnemyTemplates.SWAMP_SENTINEL, arenaX, arenaY);
```

O código **não dispara bosses automaticamente**: o encontro deve ser acionado por missão, região ou evento de história. Os exemplos ainda usam o renderizador dos fantasmas, sem sprites de chefe dedicados.

## Limitações e evolução
- Ainda não há arena, bloqueio de saída, cutscene, ataques telegráficos exclusivos, padrões de projéteis por fase, música específica, loot nem salvamento de boss derrotado.
- `ELITE` está disponível como classificação, mas o gerenciador só aceita criação explícita de miniboss e boss. É possível expandir a fábrica para elites posteriormente.
- O limite de um boss ativo é por ID de template; templates diferentes podem coexistir.
- Validar Android build, combate, fases, desempenho, UI em múltiplas telas e integridade das recompensas. **Não foi executado build/teste nesta sessão.**
