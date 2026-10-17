# Prompt — Geração de sprites para AmazonMages

> Prompt para entregar ao modelo gerador de imagens/pixel art.
> Mande primeiro a seção **0 a 4** (contexto + regras globais) junto com **um lote por vez** da seção 5.
> Não peça tudo numa única requisição: um lote por pedido, com o checklist da seção 6 no final.

---

## 0. Papel e objetivo

Você é um pixel artist sênior de jogos de RPG de 16/32 bits. Vai produzir sprites **originais** para **AmazonMages**, um RPG de ação 2D para Android (paisagem, celular), com câmera **top-down 3/4** no espírito dos RPGs de mão da era GBA (estilo "Pokémon Emerald"), mas com combate em tempo real: magia elemental, parry e árvore de habilidades.

Ambientação: **Amazônia fantástica**. Protagonista: um jovem mago cujo elemento é **Folha / Natureza**: as magias dele são folhas cortantes, vinhas, raízes e esporos luminosos.

**Inspiração, não cópia:** use a *linguagem visual* daquela era (perspectiva, proporções, contornos coloridos, leitura clara em tela pequena). Não reproduza tiles, personagens, prédios, Pokébolas, logos ou qualquer asset existente de Pokémon, Minecraft ou outro jogo. Tudo precisa ser desenho novo.

## 1. Problema que você está resolvendo

A arte atual é básica demais:
- personagem com 3 quadros apenas, só de frente, sem resposta às 8 direções que o código já suporta;
- ataque é uma bola azul-ciano lisa (gradiente circular), sem forma nem identidade;
- grama é um verde escuro chapado com ruído; arbustos são retângulos 3×3 de ovais iguais; tudo é **reto, repetido e geométrico**;
- HUD é feito de retângulos e texto solto.

O que se quer é o oposto: **silhuetas orgânicas, bordas irregulares, variações, movimento secundário e textura um pouco mais detalhada que a referência GBA, mas sem virar ruído.** Pixelado, sempre.

## 2. Regras técnicas globais (obrigatórias)

### 2.1 Escala de pixel
- O jogo desenha o mundo numa grade de **64×64 px reais por tile**.
- Desenhe com **resolução lógica de 32×32 por tile** e entregue **ampliado 2× por vizinho mais próximo** (cada pixel de arte = bloco 2×2 de pixels reais idênticos). Sem pixel "meio-bloco", sem pixel isolado de 1×1 real.
- **HUD** é exceção: entregue em **1× nativo** (cada pixel da arte = 1 pixel do PNG), o código amplia por fator inteiro.
- Nada de antialiasing, blur, gradiente suave, dithering de imagem fotográfica ou redimensionamento bilinear. Degradês só por **rampas de cor discretas** (e dithering em padrão xadrez ordenado, se usar, com no máximo 2 tons).

### 2.2 Transparência
- PNG RGBA. Fundo **100% transparente** (alpha 0) em sprites, VFX e HUD; tiles de chão-base são opacos.
- Personagem, tiles e HUD: só alpha 0 ou 255.
- VFX (brilho, esporos, rastro): permitido no máximo **3 níveis de alpha** (ex.: 255 / 160 / 80), em blocos de pixel 2×2.

### 2.3 Contorno e luz
- Contorno **colorido escuro** (tom mais escuro da própria rampa), não preto puro (#000). Preto só em pupilas, se precisar.
- Luz vindo de **cima-esquerda** em todos os assets. Sombra de contato no chão (elipse achatada, 1 tom escuro, semitransparente permitido só nela) sob personagem, árvores, pedras e arbustos.
- Perspectiva 3/4: topo dos objetos visível, face frontal levemente visível; paredes/penhascos mostram face vertical; árvores mostram copa vista de cima e tronco embaixo.

### 2.4 Paleta-mestra
Use rampas com **hue shift** (sombras puxam para azul/azulado, luzes puxam para amarelo). Valores-base sugeridos (pode ajustar ±, mas mantenha a lógica):

| Rampa | Sombra → luz |
|---|---|
| **Grama (verde mais verde, vivo, saturado)** | `#1D5A2C` `#2C8A3A` `#46B144` `#74D25A` `#B4EC78` |
| Folhagem de árvore | `#123F2A` `#1E6436` `#2F8B3E` `#55B24A` `#94D768` |
| Terra / trilha | `#5A3A24` `#86593A` `#B38455` `#D6AF78` `#EED7A4` |
| Penhasco / barranco | `#4A2E2A` `#7A4A3C` `#A8705A` `#C99A7C` |
| Água de rio | `#1A3E7A` `#2861B0` `#3E8FD6` `#7CC4F0` `#D2F2FF` |
| Magia de Folha (jogador) | `#0F4A2E` `#1E8A4A` `#3DC45E` `#9BF06A` núcleo `#F4F7A0` |
| Ácido (inimigo, já existe) | lima/amarelado `#8FD12A` `#C6F032` + roxo `#8A3FD0` |

**Importante:** o inimigo "Fantasma Ácido" já é verde-lima com olhos roxos. A magia de Folha do jogador tem de ser **visivelmente diferente**: verde **esmeralda/floresta**, com núcleo **amarelo-dourado** e formato de **folha** (pontas, nervura central), nunca blob redondo. O ácido é amorfo e borbulhante; a folha é afiada e direcional.

A grama precisa ser **mais verde e mais clara** que o atual `#3F7F2F` chapado, mais próxima do verde vivo de jogos GBA, porém **menos turquesa** que o verde-água do Emerald.

### 2.5 Direções (contrato do código)
Atlas direcionais têm **8 linhas, nesta ordem exata, de cima para baixo**:

```
linha 0 SOUTH        (de frente para a câmera)
linha 1 SOUTH_EAST
linha 2 EAST         (perfil olhando para a direita)
linha 3 NORTH_EAST
linha 4 NORTH        (de costas)
linha 5 NORTH_WEST
linha 6 WEST
linha 7 SOUTH_WEST
```
- Colunas = quadros da animação, da esquerda para a direita.
- Todas as células do arquivo têm **o mesmo tamanho**, sem espaçamento, sem borda, sem texto, sem número.
- Desenhe S, SE, E, NE, N de verdade (5 vistas únicas). W, NW, SW podem ser espelho horizontal de E, NE, SE, **desde que** o personagem não tenha item assimétrico; se tiver, redesenhe. As 8 linhas precisam existir no arquivo.
- **Pivô:** pés do personagem centralizados horizontalmente, apoiados na mesma linha de base em todos os quadros e direções (linha real y = 58 numa célula de 64). O personagem não pode "pular" entre quadros por erro de alinhamento.

### 2.6 Nomes e destino
- Pasta de destino: `app/src/main/res/drawable-nodpi/`.
- Nomes em minúsculas, `snake_case`, só `[a-z0-9_]` (regra de recursos Android). Ex.: `player_move.png`.

## 3. Direção de arte — "mais elaborado, menos reto"

Aplique em **todos** os lotes:

1. **Silhuetas orgânicas.** Nenhuma borda de objeto natural em linha reta por mais de 3 pixels de arte. Copas de árvore com lóbulos irregulares, arbustos com folhas saindo da silhueta, trilhas com bordas onduladas e tufos invadindo.
2. **Variações.** Todo tile repetível vem com no mínimo 3–4 variantes que se encaixam entre si, para quebrar o padrão xadrez. Objetos (árvore, arbusto, pedra, flor) com 2–3 variantes de forma.
3. **Detalhe na medida.** Um pouco mais rico que GBA: tufos de grama em 2 tons, pequenas flores, trevos, pedrinhas, nervura nas folhas. Mas cada tile de chão deve ter **áreas de respiro** (pelo menos ~50% de cor base lisa), senão o personagem some no fundo.
4. **Animação viva (12 princípios em miniatura):**
   - antecipação antes do ataque (recolhe a mão, folhas se juntam);
   - follow-through e overlapping: ponta do chapéu e barra do casaco atrasam 1 quadro em relação ao corpo;
   - squash & stretch leve (1 pixel de arte) no passo e no impacto;
   - respiração no idle (subida de 1 px de arte no peito/ombros), piscar de olhos ocasional;
   - partículas secundárias: 1–3 folhinhas/esporos orbitando a mão em idle e cast.
5. **Legibilidade em celular.** O jogo roda em tela de ~6" em paisagem. Teste mental: o sprite reduzido a 50% ainda mostra para onde o personagem olha e qual magia está ativa?

## 4. Personagem: identidade

Mantenha o reconhecimento do protagonista atual e adicione a identidade de Folha:
- jovem mago, cabelo castanho bagunçado, olhos verde-água;
- **chapéu de mago grande, de aba larga e ponta caída**, vermelho com estrelinhas douradas (como hoje) — agora com **uma folha/broto preso na fita do chapéu**;
- casaco/manto vermelho longo e aberto, camisa azul-petróleo por dentro, botas escuras;
- novo: **bracelete de vinha** no antebraço de conjuração e mãos que brilham verde quando carrega magia;
- proporção de RPG GBA: cabeça+chapéu ≈ 40–45% da altura, corpo compacto. Ocupa ≈ 20 px de largura × 28–30 px de altura em arte lógica dentro da célula de 32×32.

Vermelho no corpo + verde na magia dá contraste forte sobre a grama verde; não deixe o personagem todo verde.

## 5. Lotes de produção

> Peça um lote por vez. Para cada arquivo, entregue o PNG e uma linha descrevendo grade, tamanho e quadros.

### Lote 1 — Folha de modelo do personagem (aprovar antes de animar)
- `player_turnaround.png`: **1 linha × 8 colunas**, célula 64×64, as 8 direções na ordem da seção 2.5, pose neutra em pé.
- `player_concept_large.png` (opcional, só referência, não vai no APK): o mesmo personagem 4× maior para fixar detalhes.

### Lote 2 — Animações do jogador (atlas 8 linhas × N colunas, célula 64×64)

| Arquivo | Quadros/dir | Ticks/quadro (60 UPS) | Loop | Conteúdo |
|---|---:|---:|---|---|
| `player_idle.png` | 4 | 14 | sim | respiração, ponta do chapéu balançando, barra do casaco mexendo, piscar no quadro 3, folhinha flutuando perto da mão |
| `player_move.png` | 6 | 6 | sim | ciclo de caminhada: contato–passagem–contato com bob de cabeça de 1 px, braços opostos às pernas, casaco e chapéu com atraso |
| `player_attack.png` | 6 | 5 | não | 1 antecipação (recolhe a mão, folhas convergem) → 2 carga → 3 **disparo** (braço estendido na direção, flash verde; projétil nasce aqui) → 4 extensão → 5–6 recuperação |
| `player_hurt.png` | 3 | 6 | não | tranco para trás, olhos fechados, chapéu desalinhado |
| `player_death.png` | 8 | 8 | não | ajoelha, cai; chapéu escorrega; folhas se espalham e murcham no último quadro |
| `player_special.png` | 8 | 6 | não | (futuro) mãos para cima, redemoinho de folhas, brilho no chão |
| `player_parry.png` | 4 | 4 | não | (futuro) giro rápido do braço, escudo de folhas abre na frente |
| `player_dodge.png` | 5 | 4 | não | (futuro) rolamento/passo lateral com rastro de folhas |

Tamanhos finais: idle 256×512, move 384×512, attack 384×512, hurt 192×512, death 512×512, special 512×512, parry 256×512, dodge 320×512.

### Lote 3 — VFX da magia de Folha (substitui a bola azul)

| Arquivo | Grade | Célula | Descrição |
|---|---|---|---|
| `leaf_projectile.png` | 8 linhas (direções) × 4 colunas | 48×48 | lâmina de folha esmeralda com nervura clara, núcleo dourado, girando em torno do próprio eixo; 2–3 folhinhas menores de rastro atrás, orientadas pela direção de voo |
| `leaf_cast_flash.png` | 1 × 5 | 48×48 | brilho na mão no quadro de disparo: anel de folhas que se abre e some |
| `leaf_impact.png` | 1 × 6 | 64×64 | explosão de fragmentos de folha + faíscas verde-douradas + anel achatado em perspectiva 3/4 no chão |
| `leaf_storm.png` | 1 × 10 | 128×128 | especial "Tempestade de Folhas": vórtice ascendente em espiral, visto em 3/4, com sombra elíptica no chão; quadros 1–2 formação, 3–8 ciclo, 9–10 dissipação |
| `leaf_storm_warning.png` | 1 × 4 | 64×64 | marcador no chão (círculo de brotos/raízes surgindo) indicando a área antes do dano; pulsa |
| `leaf_shield.png` | 8 linhas × 4 colunas | 64×64 | escudo **frontal** (arco ≈ 100°) de folhas sobrepostas, nunca uma bolha 360°; orientado pela direção |
| `parry_flash.png` | 1 × 5 | 64×64 | estalo seco de luz verde-branca em forma de estrela de 4 pontas + pétalas voando |
| `posture_break.png` | 1 × 6 | 64×64 | rachaduras/anel quebrando sobre o alvo |
| `status_rooted.png` | 1 × 4 | 32×32 | ícone sobre a cabeça do inimigo: raízes prendendo (status elemental "Enraizado") |

### Lote 4 — Tiles do mundo (64×64, opacos salvo indicado)

Cada grupo num arquivo próprio, em **grade horizontal de células 64×64**, sem espaço. Descreva no fim a ordem das células.

- `tile_grass.png` — 4 variantes de grama base (tileáveis entre si em qualquer combinação) + 4 overlays transparentes de detalhe (trevos, florzinhas amarelas e brancas, pedrinhas, folhas caídas).
- `tile_tall_grass.png` — 2 variantes de mato alto + 3 quadros de balanço (vento/passagem do jogador). Borda superior recortada, transparente atrás.
- `tile_path.png` — trilha de terra em **autotile de 13 peças**: centro, 4 bordas, 4 cantos externos, 4 cantos internos. Bordas **onduladas**, com tufos de grama invadindo e pedrinhas.
- `tile_water.png` — água de rio 4 quadros animados (brilhos que se movem) + margem em autotile 13 peças com faixa de areia/lama e vitórias-régias opcionais.
- `tile_ledge.png` — barranco de mão única estilo RPG de mão: tampa esquerda, meio (2 variantes), tampa direita, com contorno irregular de pedra avermelhada e grama pendurada na borda de cima.
- `tile_cliff.png` — face de penhasco em autotile 13 peças, com estrias horizontais irregulares e musgo.
- `tile_ground.png` — chão de pedra/terra batida, 3 variantes.
- `tile_lava.png` e `tile_acid_swamp.png` — 4 quadros cada (tipos já existentes no código). Lava com crosta escura e veios; pântano ácido lima com bolhas e lodo, coerente com o fantasma.

### Lote 5 — Objetos do cenário (transparentes)

- `obj_tree_round.png` — 3 variantes, cada uma **128×128** (2×2 tiles): copa lobulada irregular vista em 3/4, tronco visível no tile de baixo, raízes; sombra no chão.
- `obj_tree_palm.png` — 2 variantes 128×192: palmeira de açaí/buriti com cachos.
- `obj_tree_giant.png` — 1 sumaúma 192×192 (marco de cenário) com raízes tabulares.
- `obj_bush.png` — 4 variantes 64×64, silhuetas diferentes, folhas escapando da borda; nada de grade de ovais.
- `obj_flowers.png` — 6 variantes 64×64 (helicônia, bromélia, flores pequenas em tufos).
- `obj_rock.png` — 4 variantes 64×64 com musgo.
- `obj_sign.png`, `obj_fence.png` (3 peças: esquerda, meio, direita) — madeira rústica.

### Lote 6 — HUD (1× nativo, nearest-neighbor)

Objetivo de layout (para o qual a arte é feita):
- **Esquerda, vertical:** barra de 4 slots de arma no estilo hotbar de sandbox, **porém vertical**, encostada na borda esquerda, centralizada na altura; slot selecionado com moldura destacada.
- **Canto superior esquerdo:** corações de vida em linha, embaixo gotas-folha de mana, embaixo barra fina de XP com selo de nível.
- **Direita:** botões redondos ATK, ESP, TROCA/parry, com ícones e sem texto.
- **Canto superior direito:** barra pequena de ícones (atributos, inventário, mapa, missões, menu).
- Tema visual: **madeira escura + vinhas + pedra musgosa**, coerente com a Amazônia; nada de retângulo cinza liso.

Arquivos:
- `hud_hotbar_frame.png` — moldura vertical para 4 slots (cerca de 28×112), com vinha enrolada nas laterais.
- `hud_slot.png` — 1 × 4, célula 24×24: normal, selecionado, em recarga (escurecido com overlay), bloqueado (cadeado de cipó).
- `hud_heart.png` — 1 × 4, célula 9×9: cheio, meio, vazio, piscando (dano).
- `hud_mana.png` — 1 × 3, célula 9×9: gota-folha cheia, meia, vazia (verde-esmeralda com brilho dourado).
- `hud_xp_bar.png` — peças 9-slice: tampa esquerda 4×5, meio 1×5, tampa direita 4×5, preenchimento 1×3; + selo de nível 13×13.
- `hud_posture_bar.png` — mesma lógica 9-slice, cor âmbar.
- `hud_button.png` — 1 × 3, célula 32×32: normal, pressionado, desabilitado (pedra musgosa redonda).
- `hud_icons.png` — 1 × N, célula 16×16: ataque (lâmina de folha), especial (redemoinho), troca (setas em ciclo de vinha), escudo/parry, poção, falar (balão), atributos, inventário (bolsa), mapa, missões (pergaminho), pausa/menu, fechar.
- `hud_weapon_icons.png` — 1 × 6, célula 16×16: Domínio das Folhas, Domínio dos Cocos (coco **fruta**), cajado de madeira, catalisador de cristal, arma física, vazio.
- `hud_element_icons.png` — 1 × 5, célula 12×12: folha, água, fogo, ácido, físico.
- `hud_joystick.png` — base 48×48 e manche 24×24 (lado a lado em 72×48).
- `hud_panel.png` — painel 9-slice 24×24 (cantos 8×8) para diálogos e menus, borda de madeira com folhas.
- `hud_mockup.png` (referência, não vai no APK) — tela 1200×540 em paisagem mostrando o HUD inteiro montado sobre o cenário novo, com o personagem no centro, para validar escala e respiro.

## 6. Checklist de entrega (responda item a item em cada lote)

- [ ] Grade exata: largura = colunas × célula, altura = linhas × célula; nenhum pixel fora da célula.
- [ ] Ordem das 8 linhas = S, SE, E, NE, N, NW, W, SW.
- [ ] Escala 2× por vizinho mais próximo (mundo) ou 1× (HUD); nenhum pixel de arte "quebrado".
- [ ] Sem antialiasing; alpha só 0/255 (exceto VFX, máx. 3 níveis).
- [ ] Pés alinhados na mesma linha de base em todos os quadros.
- [ ] Contorno colorido escuro; luz de cima-esquerda; sombra de contato.
- [ ] Magia de Folha distinguível do ácido do inimigo (esmeralda + dourado, forma de folha).
- [ ] Grama verde vivo, com variantes e áreas de respiro.
- [ ] Nenhuma borda natural reta; cada objeto/tile repetível com variantes.
- [ ] Nenhum texto, número ou assinatura dentro das imagens.
- [ ] Nada copiado de jogo existente.
- [ ] Nome de arquivo em `snake_case` válido para Android.

---

### Notas para quem integra (não enviar ao gerador)

- O código atual (`SpriteSheet`) lê `sprites.png` em grade 64×64 com 3 quadros do jogador na linha 0. Os atlas novos `player_<acao>.png` devem ser carregados num esquema igual ao do `GhostAtlasRenderer` (8 linhas × N colunas), preenchendo `AnimationSet.put(action, direction, clip)`.
- `GhostAtlasRenderer` usa `Paint.FILTER_BITMAP_FLAG`, que borra pixel art ao escalar; para os assets novos, desenhar com filtro desligado e escala inteira.
- O repositório local está na branch `fix/android-performance`; não existe uma branch `developer`/`develop` nem local nem no `origin`.
