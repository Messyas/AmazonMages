# Backlog de arte, sprites e áudio

**Objetivo:** registrar o que ainda precisa ser produzido e conectado ao Android. Não confundir placeholders Canvas, sprites de fallback e assets definitivos. Preferir PNG transparente em `app/src/main/res/drawable-nodpi/` para sprites pixel art sem redimensionamento automático; sons curtos em `app/src/main/res/raw/` quando integrados. Evitar criar arquivos com nomes conflitantes com recursos existentes.

| Prioridade | Asset / nome sugerido | Estado | Requisito |
|---|---|---|---|
| Alta | `coconut_projectile.png` | Ausente | Coco **fruta**, visto em 3/4, transparente, leitura clara em escala pequena; substituir círculo marrom Canvas |
| Alta | `coconut_summon.png` | Ausente | Frames do coco materializando na mão do mago; sincronizar com ataque |
| Alta | `coconut_fall.png` | Ausente | Coco caindo de cima para baixo, com sequência de quadros ou movimento procedural |
| Alta | `coconut_shadow.png` | Ausente | Marca/sombra no chão para indicar onde cada coco vai cair |
| Alta | `coconut_impact.png` | Ausente | Poeira, rachadura/fragmentos leves no impacto, animação curta |
| Alta | `coconut_hit.ogg` | Ausente | Som cômico curto de pancada de desenho animado, sem material protegido copiado |
| Média | `coconut_storm_cast.ogg`, `coconut_storm_hit.ogg` | Ausentes | Invocação e impactos múltiplos, mixagem sem saturação |
| Alta | `acid_ghost_<acao>.png` | Ausente conforme contrato | Atlases normais por ação/direção; ver [ghost-animation-atlases.md](ghost-animation-atlases.md) |
| Alta | `acid_ghost_fused_<acao>.png` | Ausente conforme contrato | Atlases fundidos por ação/direção; mesmos estados de animação |
| Média | `arcane_shield.png` / atlas | Planejado | Silhueta de escudo de energia frontal em 8 direções, não bolha 360° |
| Média | `parry_flash.png`, `posture_break.png` | Planejados | Feedback legível de parry e quebra de postura |
| Média | `hud_attack.png`, `hud_special.png`, `hud_weapon.png`, `hud_potion.png` | Ausentes | Ícones pequenos legíveis com estados pressionado, bloqueado e recarga |
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

1. Produzir sprites e sons com licenciamento adequado e versões fonte editáveis armazenadas fora do APK quando necessário.
2. Importar PNGs em `drawable-nodpi` e áudio em `raw`.
3. Implementar carregamento/cache e seleção de frames; remover placeholder apenas quando houver fallback robusto.
4. Associar quadros a eventos reais: invocação, arremesso, sombra, queda, colisão e desaparecimento.
5. Validar dimensões, direções, FPS/UPS, transparência, ordem de desenho e efeitos sonoros em dispositivo.

## Notas de gameplay para artistas

O coco é **fruta**, não fezes. O especial é uma tempestade de cocos reais que caem em vários pontos de uma área. O humor deve vir do contraste entre conjuração dramática e pancadas sonoras cômicas. O escudo é frontal e direcionado à mira, não uma esfera envolvendo o personagem.
