# Backlog de arte, sprites e áudio

**Objetivo:** registrar o que ainda precisa ser produzido e conectado ao Android. Não confundir placeholders Canvas, sprites de fallback e assets definitivos. Preferir PNG transparente em `app/src/main/res/drawable-nodpi/` para sprites pixel art sem redimensionamento automático; sons curtos em `app/src/main/res/raw/` quando integrados. Evitar criar arquivos com nomes conflitantes com recursos existentes.

| Prioridade | Asset / nome sugerido | Estado | Requisito |
|---|---|---|---|
| Alta | `coconut_projectile.png` | Integrado | Coco **fruta**, atlas 4×32×32 em 3/4; substitui o círculo marrom Canvas |
| Alta | `coconut_summon.png` | Integrado | Atlas de cinco quadros ligado à conjuração do ataque básico |
| Alta | `coconut_fall.png` | Integrado | Atlas 4×32×40 para a queda da tempestade |
| Alta | `coconut_shadow.png` | Integrado | Marcador pulsante no chão sob cada coco agendado |
| Alta | `coconut_impact.png` | Integrado | Atlas de seis quadros para impactos e colisões |
| Alta | `sfx_coconut_hit.wav` | Integrado | Pancada procedural curta, sem amostra de terceiros |
| Média | `sfx_coconut_storm_cast.wav`, `sfx_coconut_storm_hit.wav` | Integrados | Conjuração ascendente e pancadas curtas sincronizadas com cada queda |
| Alta | `acid_ghost_<acao>.png` | Integrado | 14 atlases normais com oito direções e quadros conforme [contrato](ghost-animation-atlases.md) |
| Alta | `acid_ghost_fused_<acao>.png` | Integrado | 14 atlases fundidos carregados pelo renderer existente |
| Média | `leaf_shield.png` | Integrado | Defesa frontal direcional durante a janela de parry; remove projéteis inimigos interceptados |
| Média | `parry_flash.png`, `posture_break.png` | Integrados | Feedback visual para parry bem-sucedido e postura esgotada |
| Média | `hud_icons.png`, `hud_weapon_icons.png`, `hud_button.png` | Integrados | Atlas de controles, armas e estados normal/pressionado/bloqueado |
| Média | Telas de mapa e missões | Integradas em Canvas | Atalhos do HUD abrem mapa com posição do jogador e diário com estado/progresso das missões |
| Média | `staff_attack.png` / VFX | Planejado | Golpe físico com cajado, orientação e alcance coerentes |
| Baixa | Ícones de árvore/atributos | Planejados | Sete atributos e 20 habilidades, ícones distinguíveis |

## Contrato visual geral

- Estilo consistente com personagem e cenário existentes; câmera top-down 3/4, paleta apropriada ao bioma amazônico.
- Fundo transparente e margens consistentes; sem textos embutidos nos sprites.
- Sprites direcionais: oito direções na ordem SOUTH, SOUTH_EAST, EAST, NORTH_EAST, NORTH, NORTH_WEST, WEST, SOUTH_WEST.
- Para atlas de fantasmas, **seguir exatamente** o contrato existente de quadros e dimensões em [ghost-animation-atlases.md](ghost-animation-atlases.md). Não reutilizar automaticamente essa grade para cocos; especificar atlas de coco quando houver animação pronta.
- Tamanhos de cocos ainda **não foram fixados**. Definir após comparar com o personagem no jogo; evitar criar uma grade rígida sem teste.
- Recursos não devem quebrar o jogo quando ausentes: fallback Canvas/bitmap existente, sem exceções de carregamento.
- Testar consumo de memória, decodificação única/caching e legibilidade em aparelho modesto.

## Integração pendente

1. Produzir os assets restantes com licenciamento adequado e versões fonte editáveis quando necessário.
2. Importar PNGs em `drawable-nodpi` e áudio em `raw`.
3. Criar e integrar o VFX de ataque do cajado.
4. Criar retratos do protagonista e do ancião para o diálogo.
5. Validar dimensões, direções, FPS/UPS, transparência, ordem de desenho e efeitos sonoros em dispositivo.

## Notas de gameplay para artistas

O coco é **fruta**, não fezes. O especial é uma tempestade de cocos reais que caem em vários pontos de uma área. O humor deve vir do contraste entre conjuração dramática e pancadas sonoras cômicas. O escudo é frontal e direcionado à mira, não uma esfera envolvendo o personagem.

## Retratos e diálogos (pendentes)

- `portrait_player_neutral.png`, `portrait_player_surprised.png`: expressões do protagonista.
- `portrait_npc_elder_neutral.png`, `portrait_npc_elder_angry.png`: retratos iniciais de NPC.
- Card de diálogo responsivo (pode começar com Canvas), indicador de continuação e SFX de abertura/texto/confirmação.
- Detalhes técnicos e requisitos: [dialogue-quests.md](dialogue-quests.md).
