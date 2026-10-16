# Fantasma de ácido — contrato de sprites

O código já suporta as animações abaixo; os PNGs individuais ainda precisam ser produzidos.
Os recursos devem ficar em `app/src/main/res/drawable-nodpi/` para evitar escalonamento automático do Android.

## Convenção

- `acid_ghost_<acao>.png`: fantasma normal
- `acid_ghost_fused_<acao>.png`: fantasma fundido
- **Linhas (8):** SOUTH, SOUTH_EAST, EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST.
- **Colunas:** número de quadros conforme a tabela.
- Todos os quadros devem ter **o mesmo tamanho quadrado**, fundo transparente, sem bordas, textos ou espaçamento.
- Tamanho recomendado por quadro: **64 × 64 px** (normal), **96 × 96 px** (fundido). Outros tamanhos quadrados também funcionam.
- Exemplo: idle normal = 256 × 512; fusion normal = 512 × 512.
- A orientação deve representar visão elevada 3/4, como um RPG top-down. Não basta espelhar a vista frontal para as oito linhas.

| Sufixo | Quadros por direção | Ticks por quadro | Loop |
|---|---:|---:|---|
| idle | 4 | 12 | sim |
| move | 6 | 7 | sim |
| alert | 4 | 9 | não |
| spotted_player | 4 | 9 | não |
| startled | 4 | 8 | não |
| spotted_ally | 4 | 10 | não |
| social | 4 | 10 | sim |
| flee | 6 | 6 | sim |
| chase | 6 | 7 | sim |
| attack | 6 | 7 | não |
| hurt | 4 | 7 | não |
| enraged | 4 | 8 | sim |
| fusion | 8 | 8 | não |
| death | 8 | 8 | não |

## Integração

`GhostAnimation` descreve o contrato; `GhostAnimationController` mantém reações temporárias;
`GhostAtlasRenderer` carrega cada atlas uma única vez e recorta o quadro adequado.
`Enemy` emite eventos de percepção, ataque, dano, encontro social, fusão e morte.

Caso uma ação não tenha PNG, usa-se `acid_ghost_atlas.png` / `acid_ghost_fused_atlas.png`
(grade antiga 4 × 8) se estiverem presentes, ou `ghost.png` original.
**As artes faltantes não são sintetizadas automaticamente pelo código.**

A morte mantém a entidade desenhada até a conclusão da animação, mas já bloqueia
novos ataques, fusões e colisões com feitiços.
