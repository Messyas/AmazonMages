# AmazonMages — documentação de desenvolvimento

Esta pasta reúne o contexto de design e o estado real do código na branch `fix/android-performance`. Leia este índice antes de implementar sistemas novos ou produzir arte. **Planejado não significa implementado.**

| Documento | Conteúdo |
|---|---|
| [Visão do jogo](game-design.md) | Fantasia, combate, equipamentos, atributos e progressão |
| [Estado de implementação](implementation-status.md) | O que existe, protótipos, lacunas e prioridades |
| [Arte e áudio pendentes](asset-backlog.md) | Sprites, animações, ícones, VFX, SFX e critérios de entrega |
| [Arquitetura de combate](combat-refactor.md) | Classes Java atuais e migração gradual |
| [Contrato dos fantasmas](ghost-animation-atlases.md) | Layout e nomenclatura dos atlases de animação |
| [Validação](testing-checklist.md) | Compilação e testes manuais antes do merge |
| [Diálogos e missões](dialogue-quests.md) | Eventos, estados, recompensas, cards e retratos pendentes |

## Diretrizes

- Projeto Android nativo em Java, renderização Canvas 2D, foco em aparelhos modestos.
- Manter mudanças na branch de trabalho; não alterar `main` sem validação.
- Preservar saves Room existentes, compatibilidade de ciclo de vida e desempenho.
- Recursos de imagem são arquivos reais do projeto: desenhar círculos no Canvas **não equivale** a ter sprites finalizados.
- Não anunciar build, testes ou recursos gráficos como concluídos sem verificação.
