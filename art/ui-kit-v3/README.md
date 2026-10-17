# AmazonMages — Kit de componentes v3 (6.1)

Lote de arte do prompt v3, usando as capturas do aparelho como referência. Os botões já foram integrados ao [menu inicial 6.2](../main-menu-v3/README.md); os demais componentes preparam as próximas telas. O mockup deste kit é uma referência de composição, não uma captura do aplicativo atualizado.

## Entregas

- `contact-sheet.png`: prancha dos PNGs nativos reais, ampliados por fator inteiro.
- `mockup-1200x540.png`: referência sobre a captura de gameplay fornecida pelo usuário.
- `native/`: 15 atlas RGBA em escala 1×, com alpha somente 0/255.
- `hd/`: os mesmos atlas limpos, ampliados exatamente 8×, sobre magenta puro `#FF00FF`. Cada detalhe ocupa blocos de 8×8; sem gradientes ou cores fora da paleta.
- `component-kit-v3.zip`: pacote com HD, PNGs nativos, fonte/licença, medidas e prévias.
- `measurements.json`: dimensões originais, dimensões HD, células e margens de 9-slice.
- `sources.json` e `mockup-source.json`: prompts exatos em inglês e origem das gerações feitas com a ferramenta integrada `image_gen`.

As gerações brutas ficam em `../source/ui-kit-v3/`, preservadas. O gerador não respeitou todas as medidas pedidas. `prepare_ui_kit_v3.py` remove apenas o espaço magenta, separa os 18 ícones e normaliza as células. `pixelize.py` reduz e fixa a paleta. O HD final entregue foi reconstruído a partir dessa saída limpa por ampliação 8× nearest-neighbor; não é apresentado como geração bruta.

## Medidas

Margens abaixo em pixels lógicos, na ordem esquerda/cima/direita/baixo. Multiplicar por 8 no HD. Círculos, slots, ícones, cursores e botões de incremento usam escala inteira, sem 9-slice.

| Asset | Quadros | Célula nativa | Atlas HD | 9-slice |
|---|---:|---|---|---|
| ui_panel | 1 | 32×32 | 256×256 | 10/10/10/10 |
| ui_panel_title | 1 | 48×20 | 384×160 | 8/7/8/7 |
| ui_button | 3 | 32×20 | 768×160 | 8/6/8/6 |
| ui_button_round | 3 | 32×32 | 768×256 | — |
| ui_close | 2 | 20×20 | 320×160 | — |
| ui_slot | 4 | 24×24 | 768×192 | — |
| ui_tab | 2 | 40×16 | 640×128 | 8/6/8/6 |
| ui_divider_h | 1 | 64×6 | 512×48 | — |
| ui_divider_v | 1 | 6×64 | 48×512 | — |
| ui_bar_frame | 1 | 64×12 | 512×96 | 4/3/4/3 |
| ui_bar_fill | 3 | 16×6 | 384×48 | — |
| ui_stepper | 3 | 20×20 | 480×160 | — |
| ui_icons | 18 | 16×16 | 2304×128 | — |
| ui_cursor_select | 2 | 28×28 | 448×224 | — |
| ui_perf_overlay | 1 | 16×16 | 128×128 | 4/4/4/4 |

## Contrato de estados

- `ui_button`, `ui_button_round`, `ui_stepper`: normal, pressionado, desabilitado. O pressionado é o mesmo desenho deslocado exatamente 1 pixel lógico para baixo, confirmado por comparação dos pixels após a redução.
- `ui_close`: normal, pressionado, com o mesmo deslocamento.
- `ui_slot`: normal, seleção dourada, seleção dourada clara, bloqueado.
- `ui_tab`: ativo, inativo.
- `ui_bar_fill`: verde, dourado, âmbar.
- `ui_cursor_select`: dois quadros para pulsação dourada.
- `ui_perf_overlay`: arte opaca; aplicar opacidade e texto no renderizador, sem interceptar toques.

Ordem dos ícones: lupa, mochila, atributos, mapa, diário, chat, menu, pausa, som, gráficos, salvar, voltar, coração, mana, escudo, espada-folha, mão, cadeado.

## Checklist do prompt, item por item

| Item | Resultado deste lote |
|---|---|
| HD magenta, medidas, quadros iguais | Conferido no HD final; fontes brutas preservadas separadamente. |
| Componentes sem texto e sem magenta interno | Conferido visualmente; o magenta é removido dos PNGs nativos. X e + são símbolos de controle. |
| Contornos e detalhes na grade, sem gradiente/ruído/blur | HD final em blocos 8×8 e paleta fixa. As formas dos contornos foram inspecionadas na prancha; não há certificação automática de espessura uniforme em toda diagonal. |
| 9-slice e bordas | Margens iguais nos quatro lados do painel; demais margens documentadas acima. O teste de encaixe nas telas pertence à integração. |
| Paleta do prompt | Validação automática com `art/palettes/ui_v3.json`, excluindo rampas do mundo/personagem. |
| Zoom redondo com lupa e fator | Botão e ícone entregues; representado no mockup. Fator será texto do jogo na integração. |
| Mochila com duas abas e retrato | Slots e abas preparados; a tela 6.6 e o retrato pertencem aos próximos pedidos. |
| Esqueleto comum nos painéis | Moldura, faixa de título e X preparados; aplicação nas telas pendente. |
| Monitor translúcido, azul, cinco linhas | Fundo e ícone preparados; opacidade, conteúdo e posicionamento pertencem à etapa 6.8. |
| Mira e troca | Layout funcional pertence à integração da HUD; este lote não altera os controles. |
| Toques ≥48 px e área segura | Restrição registrada para a integração; o mockup mantém a área útil. Assets nativos não definem a área de toque. |
| Mockup 1200×540, português com acentos | Entregue como referência de composição. A prancha determinística usa Pixelify Sans. |
| Arte original | Prompts exigem desenhos originais; sem assets extraídos de outros jogos. |

## Reprodução

```powershell
python tools/pixelize/prepare_ui_kit_v3.py
python tools/pixelize/pixelize.py build --prefix ui_ --palette ui_v3
python tools/pixelize/pixelize.py check --prefix ui_ --palette ui_v3
python tools/pixelize/preview_ui_kit_v3.py
```

Validação: 15 atlas, zero falhas em tamanho, paleta, alpha e quadros vazios. Botões pressionados: zero diferenças em relação ao normal deslocado 1 pixel. Não foi necessário compilar o APK neste lote de arte.

Fonte: [Pixelify Sans, distribuição oficial no Google Fonts](https://github.com/google/fonts/tree/main/ofl/pixelifysans), com licença SIL OFL incluída em `../fonts/PixelifySans-OFL.txt`.
