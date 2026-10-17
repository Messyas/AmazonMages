# AmazonMages — Menu inicial v3 (6.2)

Menu integrado ao aplicativo e instalado no Moto G52. Fundo de floresta ao entardecer, logo original, fonte Pixelify Sans e três botões de pedra musgosa: **Jogar**, **Opções**, **Sair**.

## Entregas

- `assets/bg_menu.png`: fundo pixelado opaco, 400×180 pixels, reduzido 3× e ampliado por fator inteiro no jogo.
- `assets/ui_logo.png`: logo HD com transparência, 840×180.
- `assets/portrait_player.png`: retrato HD com transparência, 1024×1024, preparado para as próximas telas.
- `preview-1200x540.png`: captura real do novo menu no aparelho, reduzida para a resolução de referência.
- `preview-pressed-1200x540.png`: captura real com **Opções** pressionado.
- `capture-font-130.png`: captura da verificação anterior com a fonte do Android em 130%, ainda com o fundo HD daquela versão.
- `measurements.json`: dimensões das artes, margens de 9-slice e áreas clicáveis medidas no aparelho.
- `main-menu-v3.zip`: artes, fonte/licença, medidas, prévias e documentação.
- `sources.json`: prompts exatos em inglês e arquivos de origem do gerador integrado `image_gen`.

As gerações brutas estão em `../source/main-menu-v3/`. Conforme a preferência atualizada do usuário, **somente o fundo é pixelizado**: fonte enquadrada em 1200×540, redução **3× para 400×180** e paleta fixa de 48 cores extraídas da cena original (`../palettes/menu_dusk.json`). O logo e o retrato mantêm seus arquivos HD anteriores. O atlas dos botões vem do kit 6.1; novos componentes passam a usar redução 4×, conforme `../README.md`.

`PixelSceneDrawable` amplia o fundo pelo menor fator inteiro que cobre a área disponível, sem interpolação, e recorta as sobras nas bordas. No Moto G52, cada pixel da cena ocupa 6×6 pixels físicos (antes, 16×16). Logo, retrato e fonte usam seus renderizadores próprios.

## Implementação

`MainMenuView` mede a área disponível da janela e acomoda o logo, a frase e as ações com margens. O recorte da câmera e a barra de navegação ficam fora do conteúdo útil. O fundo preenche a superfície inteira; o espaçamento interno é aplicado aos controles.

`AtlasMenuButtonDrawable` usa os três quadros de `ui_button.png`, células de 32×20 e margens de 9-slice **8/6/8/6** (esquerda/cima/direita/baixo). Os cantos usam escala inteira. Os estados pressionado e desabilitado acompanham os estados reais da View; foco por teclado recebe contorno dourado. O texto acompanha o deslocamento do quadro pressionado.

## Verificação

- `assembleDebug`: compilação concluída; cache de configuração reutilizado.
- Instalado e aberto em **Moto G52**, tela física **2400×1080 em paisagem**, densidade 400 dpi.
- Área clicável de cada botão: **602×120 px**, equivalente a 240,8×48 dp.
- **Jogar** abre a seleção de jogatinas; **Opções** abre as configurações; **Sair** fecha a Activity. As duas navegações foram verificadas pela hierarquia de interface; a saída foi confirmada pelo launcher em primeiro plano. O jogo foi reaberto no menu ao terminar.
- Estado pressionado observado na captura de **Opções**.
- Atualização do fundo: recompilado, instalado e inspecionado novamente no Moto G52, incluindo o estado pressionado. Capturas normal e pressionada atualizadas para o fundo pixelado.
- Fonte Android a **100% e 130%**, sem títulos ou ações cortados. A configuração foi restaurada a **100%**.
- Capturas observadas visualmente no aparelho; recorte esquerdo de 105 px e navegação inferior de 60 px respeitados.
- A CLI não conseguiu instalar seu servidor de inspeção de layout; a hierarquia foi obtida com `adb shell uiautomator dump`.

## Checklist do prompt

| Item | Situação nesta etapa |
|---|---|
| HD magenta com dimensões definidas | Originais preservados; logo/retrato finais em HD com transparência. Fundo opaco reduzido 3× conforme a correção do usuário. |
| Sem texto nos componentes | Botões sem texto embutido; rótulos renderizados pelo jogo. O logo é a exceção solicitada. |
| Pixel art limpa no kit | Atlas nativo do kit 6.1 e fundo reduzido com paleta fixa, sem interpolação bilinear no jogo. Logo/retrato permanecem HD. |
| 9-slice | 8/6/8/6 nos botões, cantos em fator inteiro. |
| Paleta e tema | Pedra musgosa, folhas, dourado, texto creme e floresta ao entardecer. |
| Zoom redondo | Etapa de HUD futura; não faz parte do menu inicial. |
| Mochila e duas abas | Etapa 6.6 futura; retrato já preparado. |
| Esqueleto de painéis | Etapas de painéis futuras; o menu inicial segue a composição própria 6.2. |
| Monitor de desempenho | Etapa 6.8 futura. |
| Mira e troca | Etapa de HUD futura. |
| Área de toque e área segura | Conferidas no aparelho; botões com 48 dp de altura. |
| 1200×540, acentos e fonte única | Prévias reais e texto em Pixelify Sans; rótulo “Opções” com acento. |
| Arte original | Gerações feitas para o projeto, sem extração de assets de outros jogos. |

Reprodução das artes: `python tools/pixelize/prepare_main_menu_v3.py`. Para atualizar somente o fundo: `python tools/pixelize/prepare_main_menu_v3.py --background-only`. A paleta da cena é `menu_dusk`; ao usar a CLI genérica: `python tools/pixelize/pixelize.py build bg_menu --palette menu_dusk`. Para compilar e instalar: `./gradlew.bat --configuration-cache installDebug`.

A próxima tela do fluxo é **6.3 — seleção de jogatinas**, com três cartões lado a lado.
