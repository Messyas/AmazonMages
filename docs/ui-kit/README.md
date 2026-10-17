# AmazonMages UI kit — etapa 5.1

> Versão atual: [kit v3 — etapa 6.1](../../art/ui-kit-v3/README.md), com 15 atlas, estados, medidas, PNGs HD/nativos e mockup sobre a captura do aparelho. O conteúdo abaixo registra o conceito anterior.

`ui_kit_atlas_concept.png` é a prancha de direção visual para aprovação. Ela reúne moldura de painel, faixa de título, botões, slots, abas, divisores, barras, stepper, ícones e cursor de seleção, usando madeira, pedra musgosa, folhas e dourado.

## Especificação para os PNGs finais

| Arquivo | Grade/célula | Escala | 9-slice |
|---|---|---:|---:|
| `ui_panel.png` | 32×32 | 1× | 10×10 px |
| `ui_panel_title.png` | 48×20 | 1× | 8×7 px |
| `ui_button.png` | 3×1, células 32×20 | 1× | 8×6 px |
| `ui_button_round.png` | 3×1, células 32×32 | 1× | 15×15 px |
| `ui_close.png` | 2×1, células 20×20 | 1× | — |
| `ui_slot.png` | 4×1, células 24×24 | 1× | — |
| `ui_tab.png` | 2×1, células 40×16 | 1× | 8×6 px |
| `ui_divider.png` | 2×1, células 64×6 | 1× | — |
| `ui_bar.png` | peças 9-slice + amostras vazia/verde/dourada/âmbar | 1× | 3×2 px |
| `ui_stepper.png` | 3×1, células 20×20 | 1× | — |
| `ui_icons.png` | 18×1, células 16×16 | 1× | — |
| `ui_font.png` | 16×8, células 8×12 | 1× | — |
| `ui_cursor_select.png` | 2×1, células 28×28 | 1× | — |

## Ajuste dimensional

A folha de fonte descrita no prompt como 16×6 teria 96 células, insuficientes para ASCII 32–126 mais as letras acentuadas do português. A especificação foi ajustada para 16×8 (128 células), mantendo células de 8×12 px.

## Estados funcionais

- Botões de menu e diálogo usam `PixelMenuButtonDrawable`: toque desenha o rosto um pixel de arte abaixo; foco/seleção acende o contorno dourado; desabilitado fica dessaturado.
- Botão redondo de pausa usa os três quadros já presentes em `hud_button.png`: normal, pressionado e desabilitado.
- Os estados vêm do estado real da `View` (`pressed`, `focused`, `selected` e `enabled`), não de uma imagem estática escolhida manualmente.

## Estado da etapa

A prancha é um conceito de arte, não um atlas de produção. O menu principal (etapa 5.2) já usa os botões stateful e o painel de madeira/folhas. O mockup de referência existente está em `../hud_mockup.png`. Antes de distribuir os demais PNGs 1×, eles ainda precisam ser separados e conferidos com alpha binário e ampliação nearest-neighbor.
