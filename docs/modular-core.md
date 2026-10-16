# Núcleo modular incremental: ECS e eventos

## Escopo da primeira etapa
A implementação introduz um ECS **leve e opcional**, sem reescrever os objetos Canvas existentes. O `ecs/World` administra entidades por IDs locais, `ecs/Entity` permite composição de componentes e consultas por tipo. Os primeiros dados são `TransformComponent` e `HealthComponent`. Não há ainda sincronização entre `World` e `Player`/`Enemy`; o ECS é uma fundação, **não** um substituto já integrado ao gameplay.

O `events/GameEventBus` é síncrono e simples. No jogo, derrotas dos inimigos agora publicam `GameEvent` e três assinantes processam progresso de missões, áudio e recompensas/salvamento. A ordem atual de assinantes preserva a sequência anterior de efeitos. Os IDs de eventos existentes permanecem iguais.

## Limites
- Event bus deve ser utilizado na thread de jogo; ainda não tem fila, política de exceções, filtros ou assinatura por tipo.
- `GameEvent.entityId` fica em zero para eventos legados de derrota, porque os inimigos atuais não têm ID ECS.
- A migração de objetos deve ser gradual e orientada por testes. Evitar duplicar estado de vida/posição em `Player` e componentes até existir um adaptador bem definido.
- Não houve build Android nem execução dos testes nesta sessão.

## Próximas etapas
1. Validar build e testes unitários (`WorldTest`, `GameEventBusTest`) e corrigir regressões pré-existentes.
2. Introduzir ciclo de vida e `System`/scheduler com ordem determinística e orçamento de atualização.
3. Migrar primeiro entidades simples e efêmeras (projéteis ou itens coletáveis), com renderização Canvas mantida.
4. Criar catálogo de itens, inventário e habilidades orientados a dados.
5. Adicionar transições de cenas, recursos compartilhados e persistência de entidades relevantes.

## Segunda etapa: scheduler e projéteis do jogador

- `ecs/GameSystem` define `update(World, ticks)`.
- `ecs/SystemScheduler` executa sistemas em ordem de registro, sem criar threads.
- `ecs/VelocityComponent` prepara o movimento orientado a dados.
- `ecs/ProjectileComponent` faz a ponte com o `Spell` atual.
- `ecs/ProjectileMovementSystem` atualiza projéteis e remove os que saem do mundo.
- `ecs/ProjectileWorld` encapsula criação, atualização, renderização, snapshot para colisão e reconciliação dos projéteis consumidos.
- `Game` deixa de manter a lista de `Spell` diretamente e delega seu ciclo de vida ao `ProjectileWorld`.
- `SystemSchedulerTest` cobre ordem dos sistemas e remoção durante consulta snapshot.

**Limitação importante:** `Spell` ainda contém sua própria velocidade e movimento. O `VelocityComponent` não dirige projéteis nesta etapa, evitando estado duplicado. O sistema de colisões dos inimigos continua legado e usa uma lista temporária de projéteis. Não houve compilação nem execução de testes Android.

## Terceira etapa: dados autoritativos de movimento

- Projéteis do jogador agora recebem `TransformComponent`, `VelocityComponent`, `ColliderComponent` e `ProjectileComponent` no momento da criação.
- `ProjectileMovementSystem` atualiza posição com os componentes e sincroniza o objeto `Spell` apenas para desenho e colisões legadas. O método `Spell.update()` continua disponível para código legado, mas não é chamado pelo `ProjectileWorld`.
- `ProjectileCollisionSystem` introduz consulta geométrica reutilizável sem dependências Android; o combate atual ainda chama `EnemyManager.collideWithPlayerSpells` e **não foi migrado** para esse sistema.
- Testes unitários novos verificam consultas circulares e atualização de posição por componentes. Não foram executados.
- Próximo passo: integrar o sistema de colisões ao combate com entidades de inimigos, garantindo eventos de dano únicos, e introduzir itens coletáveis usando os mesmos componentes.

## Quarta etapa: itens coletáveis (protótipo)

- `items/ItemDefinition`, `ItemRegistry` e `Inventory` implementam catálogo, quantidades e limite de pilha por tipo.
- `ecs/PickupComponent` representa uma pilha coletável; `PickupSystem` usa consultas geométricas ECS e mantém o restante no mundo quando o inventário está cheio.
- `Game` cria um `World` de itens e coleta por proximidade do jogador; os itens são círculos provisórios no Canvas.
- Comandos debug: `spawn erva [1-10]` cria itens próximos; `give erva` adiciona uma unidade ao inventário.
- A coleta publica `item.erva.collected` no Event Bus. Ainda não há interface de inventário, persistência de itens nem tabela de drops.
- `PickupSystemTest` cobre coleta por proximidade e capacidade parcial.
- **Não foi executado build nem teste Android.** A migração de colisões de projéteis contra inimigos continua pendente: `ProjectileCollisionSystem` é reutilizado para pickups, mas o combate ainda usa `EnemyManager.collideWithPlayerSpells`.
