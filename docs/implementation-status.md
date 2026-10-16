# Estado real da implementação

Referência: branch `fix/android-performance`, outubro de 2026. Não houve build Android verificado nesta sessão.

## Implementado ou integrado ao protótipo

- Loop de jogo Canvas 2D, mapa, jogador, movimento, inimigos fantasmas ácidos e projéteis.
- AI de fantasmas com personalidades, ataques coordenados e fusão; renderização de atlas com fallback.
- Menu principal, três slots de save Room, pausa/salvamento e retorno ao menu.
- Combate: `DamageType`, `DamageEvent`, `DamageResolver`, `CombatStats`, `StatusEffects`, `Posture`; recebimento de dano com precisão fracionária.
- `WeaponType`, `WeaponDefinition`, `WeaponLoadout`: quatro posições, somente Domínio dos Cocos equipado inicialmente.
- `Spell`: projétil circular de coco desenhado via Canvas, dano físico; direção de ataque separada da direção de movimento.
- `EnemyManager.coconutImpact`: dano de área. `Game`: tempestade de 150 ticks e recarga de 1080 ticks, impactos aleatórios; HUD e áreas de toque provisórios.

## Parcial / provisório

- HUD: botões são círculos/textos Canvas com posições relativas, joystick ainda possui posição fixa em pixels; necessita layout adaptativo.
- Ataque: tipo de arma é representado nos dados, mas **não existe executor específico** de ataques para catalisadores e armas físicas. O ataque básico atual instancia `Spell`.
- Tempestade: dano por pulsos e círculo indicador, **sem cocos caindo visivelmente, sombras individuais, animação de invocação ou SFX**.
- Postura: acúmulo e regeneração existem; quebra, atordoamento e reação de inimigos ainda não.
- Fusão e animações: lógica existe, mas os atlases específicos de fantasma ainda não foram fornecidos; fallback pode usar `ghost.png`.
- Mira: ponteiros independentes para movimento/ataque, mas precisa de testes de multitoque e ergonomia.

## Não implementado

- Inventário real, coleta, quatro equipamentos efetivamente utilizáveis, poções e seletor de consumíveis.
- Sistema de mana, XP, níveis, atributos distribuíveis, 20 habilidades, pré-requisitos e telas de progressão.
- Escudo frontal, colisão direcional, parry perfeito, quebra de postura/stun, bônus acionado por parry.
- Animações e sons próprios de coco, cajados, catalisadores e especiais; áudio de impacto cômico.
- Persistência dos novos equipamentos, progressão e cooldowns nos saves.

## Prioridade sugerida

1. Rodar build e corrigir falhas; validar regressões de save, ciclo de vida e controles.
2. Implementar recursos visuais essenciais do coco e contrato de sprites; manter fallback.
3. Separar `AttackExecutor` / estratégias de arma de `Game.java`, com ataques específicos para cada categoria.
4. Introduzir mana, consumíveis e inventário persistente.
5. Implementar escudo/parry/postura e progressão em incrementos testáveis.

Nenhum item pendente deve ser marcado como pronto apenas porque aparece no documento de design.

## Atualização: missões persistentes (Room v3)

A base de diálogo demonstrativa e a missão `first_ghosts` estão conectadas a `Game`. O progresso/estado agora são gravados em `quest_progress` por slot, restaurados antes de iniciar o loop e removidos junto com o slot. A recompensa continua simbólica (sem inventário/desbloqueio real). Falta compilar e testar a migração e o ciclo de vida.
