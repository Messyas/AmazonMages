# Atributos, progressão e persistência

## Implementado na branch de desenvolvimento

- `progression/PlayerProgression.java`: sete atributos (Vigor, Mente, Resistência, Inteligência, Destreza, Arcano, Força), nível, XP, pontos de atributo, pontos de habilidade, mana atual/máxima.
- Valores iniciais provisórios: nível 1, atributos 1, mana 100/100, sem pontos livres.
- `gainExperience(amount)`: XP acumulado até o próximo nível; limiar provisório de `100 * nível`, limite de nível 100; cada subida concede um ponto de atributo e um de habilidade.
- `allocate(attribute)`: API para investir pontos, ainda sem tela de atributos.
- `Game` concede 10 XP por fantasma ácido derrotado pelo jogador, mostra nível/XP/pontos no HUD provisório e salva o snapshot após o evento.
- `PlayerStatsEntity` / `PlayerStatsDao`: tabela `player_stats` por `slotId`.
- `GameDatabase` v4; migração `MIGRATION_3_4` cria a nova tabela, sem apagar posição e missões.
- `GameSaveRepository` carrega/salva stats de forma assíncrona e remove stats ao excluir slot.
- `Game` restaura posição, missões e stats antes de iniciar o loop; autosave e pausa incluem stats.

## Limitações intencionais

- **Apenas efeitos defensivos de Resistência/Arcano e mana máxima de Mente foram ligados**; demais efeitos e balanceamento permanecem pendentes.
- Mana é um dado persistente, mas ainda não há consumo/regeneração em feitiços.
- Pontos de atributo podem ser distribuídos no painel Canvas; pontos de habilidade não possuem árvore funcional.
- Atributos não incluem inventário, equipamentos, skills e desbloqueios persistidos; são etapas separadas.
- A experiência vem somente de mortes de fantasmas pelo jogador no protótipo; outros eventos e recompensas ainda não dão XP.
- Persistência não foi compilada nem testada em Android nesta sessão.

## Validação pendente

1. Compilar e validar schema Room v4 e migrações 1→2→3→4, 2→3→4 e 3→4.
2. Confirmar que XP e atributos restauram após encerrar e reabrir cada um dos três slots.
3. Verificar que excluir slot apaga progresso sem afetar os outros.
4. Testar subida de nível, XP excedente, máximo de nível e fechamento do app durante autosave.
5. Construir tela de atributos com confirmação de distribuição e salvar imediatamente após alocação.

## Painel de atributos e efeitos provisórios

- `AttributesPanel` desenha um modal Canvas com nível, XP, pontos e sete atributos, com botão `+` para distribuir e `X` para fechar.
- `Game` exibe botão `ATRIBUTOS`, intercepta toques durante o modal, pausa o mundo e salva imediatamente após a alocação.
- `applyProgressionEffects()`: Resistência reduz dano físico em 2,5% por ponto acima de 1 (máximo 40%); Arcano reduz ácido em 1,5% por ponto acima de 1 (máximo 25%); Mente concede +10 de mana máxima por ponto acima de 1.
- Os efeitos são **provisórios** e não substituem o balanceamento final. Mana não tem consumo/regeneração real; Vigor, Inteligência, Destreza e Força ainda não aplicam efeitos. Não há botão para gastar pontos de habilidade.
- **Pendente:** testar toque/modal em diferentes resoluções, compilar Android, verificar migração Room v4 e balancear efeitos. Nenhum build foi executado nesta sessão.

## Integração de combate e mana (etapa seguinte)

- Vigor: +1 de HP máximo por ponto acima de 1; barra de vida usa o limite dinâmico. HP salvo é restaurado após calcular o máximo do personagem.
- Mente: +10 de mana máxima por ponto acima de 1; regeneração de mana de 1,5 por segundo +0,15 por ponto extra, em 60 atualizações/s.
- Inteligência: +6% de dano mágico por ponto extra nos projéteis mágicos.
- Força: +6% de dano físico por ponto extra nos cocos básicos e na Tempestade de Cocos.
- Destreza: +3,5% de velocidade de ataque básico por ponto extra, até 2×; cooldown calculado em ticks.
- Tempestade de Cocos: consome 35 de mana apenas se puder ser ativada; mantém cooldown de 1080 ticks (~18 s a 60 UPS).
- O HUD mostra mana atual/máxima. Regeneração ocorre somente quando a atualização do mundo está ativa.
- Resistência e Arcano mantêm os efeitos defensivos provisórios anteriores.

**Limitações:** não existe recuperação automática de vida por Vigor, nem efeitos de status ou equipamentos alterando atributos; os números são provisórios. Não houve build nem teste em dispositivo nesta sessão. Validar salvamento/restauração com Vigor alto, consumo e regeneração de mana, dano fracionário e cooldowns.
