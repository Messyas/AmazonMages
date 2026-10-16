# Estado real da implementação

Atualizado em 2026-10-16 na branch `fix/android-performance`.
O documento de referência para funcionalidades, testes e commits é
[architecture-roadmap.md](architecture-roadmap.md).

## Integrado ao jogo

- Loop fixo Canvas 2D, mapa, jogador, fantasmas, chefes, combate e progressão existentes preservados.
- World renderer, controlador de input multitoque, estados modais e coordenação de snapshots extraídos de Game.
- ECS para projéteis do jogador, projéteis hostis e pickups; registro dos inimigos por adaptador sem duplicação de posição/vida.
- Inventário no HUD, descrições/quantidades, seleção, cura, recuperação de mana e buff temporário.
- Quatro slots equipáveis; Domínio dos Cocos inicial e cajado mágico obtido por loot.
- Drops por tipo de inimigo, limites de pilha/chão, expiração e persistência por slot.
- Room 7 com migrações explícitas 1→7, save transacional de seis tabelas e callbacks descartados ao encerrar sessão.
- Spawn configurável por região; Sentinela solicitada pela missão, com registro persistente de encontro pendente/concluído.
- Cache central de bitmaps limitado, ciclo de vida de áudio preservado, controles proporcionais à Surface.

## Verificado neste ambiente

- APK debug e APK instrumentado compilados.
- 26 testes unitários aprovados, zero falhas/erros.
- Android Lint: zero erros, 31 avisos.
- Testes instrumentados **não executados**: `No connected devices!`.

## Limites e backlog

- Validação de gameplay, migrações em saves reais, multitoque, densidades e desempenho físico pendente.
- HUD legado ainda parcialmente em Game; extração incremental, sem reescrita completa.
- Chefes pendentes reiniciam com vida cheia ao carregar; efeitos, cooldowns e projéteis são transitórios.
- Arte final, VFX/SFX e animações próprias de armas permanecem no backlog artístico.
- Parry/escudo direcional, stun por postura e árvore completa de 20 habilidades do design anterior
  não fizeram parte da lista de sistemas iniciais desta implementação.

Veja os detalhes e regras de compatibilidade no roadmap e no
[relatório de validação](validation/2026-10-16.md).
