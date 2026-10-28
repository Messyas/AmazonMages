# Novo mundo amazônico — plano de implementação

Estado: mundo integrado ao fluxo normal da partida, com arte HD reduzida, colisão, streaming, overview e migração de saves Room v8. Os cinco marcos, quatro arenas e 32 descobertas estão implementados. Ver [entrega visual](../art/world-v1/README.md) e [validação no Android](validation/2026-10-27-world.md).

Este documento preserva a direção e a sequência do planejamento. Os resultados efetivamente medidos e as diferenças da implementação estão no relatório de validação. O teste de dez minutos usa o renderer de produção em Canvas no emulador; desempenho físico, FPS/UPS completos e comparação com a versão antiga não foram certificados.

Referência de composição: [esboço do autor](design/amazon-world-sketch.png).

## 1. Direção e composição

Substituir o cenário atual por uma floresta amazônica circular, grande, viva e misteriosa. O jogador começa entre as raízes de uma castanheira colossal e pode alcançar livremente quatro regiões externas por terra, trilhas e pontes. A geografia e as construções são fixas; a distribuição de detalhes é procedural e reproduzível. Esta etapa entrega exteriores e quatro arenas ambientais, sem interiores, novos NPCs ou implementação dos quatro novos bosses.

| Posição | Região garantida | Elementos visuais e exploração |
|---|---|---|
| Centro | Castanheira monumental | Tronco, raízes e copa ocupando várias telas, clareira inicial, caminhos entre raízes e quatro saídas |
| Noroeste | Núcleo de malocas | Malocas coletivas, madeira, palha, cerâmica, praça de terra e passarelas entre a mata e a água |
| Nordeste | Cidade antiga | Cidade perdida fantástica com pirâmides escalonadas, escadarias, pátios, relevos e vegetação recuperando os edifícios; influência maia solicitada pelo autor |
| Sudoeste | Mata morta | Árvore monumental queimada, solo escuro, cinzas, troncos carbonizados e grandes vazios; transição visível da floresta viva para a morta |
| Sudeste | Cidade moderna amazônica | Arquitetura inspirada no Teatro Amazonas, edifício cultural monumental, fachadas claras e coloridas, praça, calçamento, jardins e porto |

As regiões permanecem imersas em uma floresta contínua: evitar quatro quadrantes com cortes retos. Cada região recebe um conjunto construído reconhecível, oito descobertas secundárias e uma arena: 32 descobertas secundárias no total. Usar vestígios de ruínas, portos pequenos, mirantes, clareiras, caminhos antigos e árvores marcantes; variar os conjuntos conforme a região. Distribuir sinais de interesse ao longo das rotas aproximadamente a cada 40–80 tiles, para que a escala não vire caminhada por terreno repetido.

O círculo é o território explorável, cercado por mata fechada e margens naturais; o limite atual de colisão é circular e recebe o terreno de mata fechada na borda. Preservar a leitura dos dois corredores fluviais diagonais do esboço e dos vazios entre massas de floresta. As cores chapadas do esboço orientam a composição, sem determinar a paleta final.

### Escala e posições

- Grade de referência: 1.920 × 1.920 tiles de 64 px; centro em `(960, 960)`, raio explorável de referência de 936 tiles, limite circular de colisão.
- Diâmetro de referência: 119.808 px. A 400 px/s, são 299,52 s de deslocamento retilíneo básico; trilhas, obstáculos e combate aumentam o tempo real. Não prometer cinco minutos para qualquer rota.
- Centros dos conjuntos regionais, em tiles: malocas `(440, 440)`, cidade antiga `(1480, 440)`, mata morta `(440, 1480)`, cidade moderna `(1480, 1480)`.
- Tronco principal ancorado em `(960, 940)`; spawn em terreno livre em `(960, 1010)`. Proteger um raio de 48 tiles em torno do spawn contra inimigos e obstáculos procedurais. O encontro inicial existente será acessível fora desse raio.
- Quatro caminhos principais saem do núcleo; uma rota de ligação circunda a clareira central e outra conecta as quatro regiões. Cada conjunto regional possui dois acessos terrestres independentes. Caminhos principais têm ao menos quatro tiles livres; pontes têm ao menos seis.

### Rios e travessias

- Rio Negro com água escura e reflexos azulados; Solimões com água barrenta e quente. O Encontro das Águas deve manter duas massas distinguíveis correndo lado a lado, com animação discreta e margem contínua.
- Usar o corredor noroeste do desenho para o Rio Negro e o corredor sudeste para o Solimões. Conectar um braço do Negro pela periferia oeste/sul à faixa sul do segundo corredor, formando o encontro ao sul do núcleo. Essa conexão é uma interpretação de composição para tornar a hidrografia coerente; não pretende reproduzir um mapa geográfico real.
- As faixas fluviais não atravessam o spawn nem cortam o tronco central. Prever meandros, ilhas, praias e igarapés secundários, com margens transitáveis junto aos pontos de interesse.
- Construir pontes onde os dois circuitos e os quatro caminhos principais cruzarem a água; essas travessias fazem parte da geografia fixa. A geração de vegetação não pode bloqueá-las. Água funda bloqueia deslocamento; navegação e natação ficam fora desta etapa.

### Quatro arenas reservadas

Uma arena por região, com área de combate livre de pelo menos 48 × 48 tiles, entrada e saída largas e terreno válido para futuros inimigos. As arenas são acessíveis e ficam abertas nesta etapa; não criar bloqueios de progressão, gatilhos de boss nem novos inimigos.

| ID estável | Local | Reserva temática |
|---|---|---|
| `arena_storm` | Clareira externa às malocas, `(380, 330)` | Tempestades e manifestação ancestral; identidade da entidade será definida no trabalho de bosses |
| `arena_ancient` | Pátio da cidade antiga, `(1550, 370)` | Guardião ou lenda menos comum, a escolher na etapa de bosses |
| `arena_forest` | Borda entre mata viva e morta, `(370, 1510)` | Referência de espaço para Mapinguari |
| `arena_river` | Margem e praça portuária externa à cidade moderna, `(1550, 1510)` | Referência de espaço para anaconda; solo firme para o jogador e área aquática adjacente |

Os nomes temáticos não são uma afirmação sobre tradições indígenas nem contratos definitivos de personagens. O terreno e os IDs devem permitir trocar o boss futuro sem reconstruir o mapa.

## 2. Produção de arte e castanheira

Reaproveitar o pipeline de `art/README.md`, com fontes HD preservadas, prompts e identificadores em inglês e textos visíveis ao jogador em português. A referência de Emerald orienta perspectiva 3/4, proporções, contornos e legibilidade; a referência de Elden Ring orienta a escala monumental da árvore.

1. Produzir primeiro uma composição HD do mundo para referência visual e uma cena de câmera contendo protagonista, vegetação, margem de rio e arquitetura. Usar geração de imagem para desenho limpo, cel shading e grandes massas de cor; não pedir pixel art ao gerador.
2. Aplicar o perfil inicial dos novos assets: fonte com quatro vezes a resolução final, redução 4×, `scale: 1`, tile final de 64 × 64 px. Isso mantém o tamanho no mundo e dá mais detalhe que os atuais tiles lógicos de 32 px ampliados em 2×. Gerar comparação com o perfil antigo, mas adotar o perfil novo como padrão desta reformulação.
3. Quantizar deterministicamente para uma paleta do cenário derivada da arte nova, mantendo as cores do protagonista como referência. Não alterar globalmente a paleta dos sprites e da UI existentes. Usar contornos limpos, sombras suaves em massas, sem dithering ruidoso ou textura fotográfica.
4. Produzir assets modulares: chão e transições; Negro/Solimões/encontro; mata e palmeiras; madeira/palha/cerâmica; ruínas escalonadas; cidade/teatro/porto; mata morta; castanheira. Objetos têm transparência real, âncora nos pés e máscara de colisão independente da silhueta visível.
5. Conferir tiles repetidos 3 × 3 e cenas no tamanho real do jogo. Corrigir emendas, halos, escalas e margens antes de integrar cada família. Gerar frames e variantes separadamente e montar os atlases por ferramenta determinística, sem confiar em grades desenhadas pelo gerador.

A composição HD é referência, não um bitmap gigante usado como cenário inteiro. O pipeline existente deve receber suporte a peças de edifícios e à montagem das famílias novas sem modificar contratos de personagem, fantasmas ou HUD. Desativar o uso do gerador antigo de tiles como fonte dos assets substituídos e documentar a nova fonte de verdade para impedir sobrescrita acidental.

### Castanheira monumental

- Silhueta de castanheira: tronco muito alto, copa ampla no alto e raízes gigantes. Tons de casca e verde vivo, com magia discreta; não presumir uma árvore inteiramente dourada.
- Dimensões iniciais no mundo: tronco de 48 × 80 tiles, raízes abrangendo cerca de 160 × 140 tiles e copa de 256 × 192 tiles projetada ao norte da base. Dividir a arte em peças de no máximo 512 × 512 px finais, carregadas conforme a câmera.
- Separar solo/sombras, raízes/tronco e copa. Colisão apenas em tronco e raízes maciças; corredores entre raízes são caminháveis. Não desenhar uma sombra escura uniforme em toda a clareira.
- Ordenar objetos e atores pela base no mundo. Reduzir a opacidade das partes de copa que encobrirem o jogador a 35%, com transição curta, sem alterar sua colisão. Manter sua posição legível também durante combate.
- A árvore inteira aparece na visão geral. Na câmera normal, a monumentalidade vem dos fragmentos em várias telas, das raízes e da copa; não reduzir toda a árvore para caber em uma tela.

## 3. Implementação e interfaces

O projeto anterior usava Java e Canvas, mapa de 200 × 140 tiles, início em `(1000, 500)`, árvores globais e chunks visuais de 8 × 8 tiles. A simulação não bloqueia deslocamento sobre o terreno. Não basta ampliar as constantes do mapa: o overview atual cresceria para um bitmap de 5.760 × 5.760 px, e a busca de árvores e de tiles de spawn percorre listas ou grades globais.

- Introduzir `WorldDefinition` como fonte única de dimensões, centro/spawn, seed fixa `20261027L`, revisão `1`, rios, caminhos, construções, arenas e localizações nomeadas. `MapLayout` passa a adaptar essa definição, removendo a matriz manual no canto. Os IDs de terreno deixam de depender de ordinais para novos tipos.
- Separar terreno, decoração, obstáculos e regiões de gameplay. Acrescentar terrenos Negro, Solimões, confluência, chão queimado, praça/calçamento e travessias. Os pântanos mágicos existentes continuam sendo regiões de gameplay, fora da área inicial e sem sobrescrever rios, caminhos, construções ou arenas. Remover lagos de lava aleatórios do novo cenário.
- Expor consultas `sampleTile(row, col)`, `isWalkable(x, y, radius)`, `resolveMovement(...)`, `landmark(id)`, `arena(id)` e `regionAt(x, y)` a partir do mesmo modelo. Usar coordenadas do mundo nas interfaces de movimento e limites; os descritores podem armazenar suas posições em tiles.
- Gerar dados de terreno e decoração por blocos lógicos de 32 × 32 tiles e cache limitado, com hash da seed e das coordenadas. Cada bloco é independente da ordem de carregamento. Consultas incluem vizinhos necessários para transições, copas e objetos que cruzam suas bordas.
- Manter chunks visuais de 8 × 8 tiles, renderização antecipada no worker e descarte por distância; indexar somente chunks residentes/pendentes. Usar cache LRU com orçamento de 64 MiB para bitmaps de cenário, incluindo terreno e grandes peças. Cancelar trabalho antigo ao teleportar ou sair da partida e evitar criar a mesma peça simultaneamente no worker e no render.
- Indexar decoração por bloco, buscando apenas o retângulo visível expandido pela extensão máxima dos sprites. Substituir o loop global de árvores e ordenar objetos/atores visíveis pela base, com uma camada separada para copas. Não embutir copas enormes nos chunks de chão.
- Gerar overview de no máximo 1.024 × 1.024 px por amostragem direta do modelo, com castanheira, regiões, rios e pontes reconhecíveis. A imagem é cacheada; posição do jogador e marcos são overlays. Não gerar uma imagem de três pixels por tile.
- Aplicar colisão a movimento do jogador e aos inimigos terrestres, com deslizamento por eixo e limite circular; normalizar entrada diagonal acima de magnitude 1, preservando os modificadores de velocidade. Fantasmas podem atravessar objetos internos, mas não o limite do mundo. Projetéis mantêm o comportamento atual; colisão ambiental de projéteis é trabalho separado.
- Fazer spawns por amostragem limitada em blocos próximos ao jogador, respeitando região e terreno válido, sem percorrer o mundo inteiro. Usar distância radial ao centro para distribuir perigo fora da clareira; preservar templates, recompensas e missões atuais. As quatro arenas novas não iniciam encontros.
- Derivar limites, teleporte, pickups, localizações de debug e posições dos encontros existentes do modelo do mundo. Eliminar limites antigos como `6400 × 4480` e `6300/4380`. Reposicionar o encontro de Sentinela existente em uma clareira de pântano acessível fora da proteção inicial, distinta das quatro reservas novas.

### Saves e compatibilidade

- Acrescentar `worldRevision` à posição salva, atualizar Room de v7 para v8 com migração explícita e passar essa informação no snapshot. Registros antigos recebem revisão `0`; partidas gravadas no mundo novo recebem revisão `1`.
- Na primeira carga de um save antigo, reposicionar o jogador no spawn central e redistribuir os pickups preservados em células livres próximas, sem alterar seus itens, quantidades ou tempo restante. Preservar vida, inventário, equipamento, atributos e progresso das missões. Persistir a nova revisão junto ao snapshot somente após a restauração completa.
- Saves da revisão atual mantêm a posição se válida; posições fora do território ou dentro de obstáculos vão para a célula caminhável mais próxima e, se não houver candidata local, para o spawn. Aplicar a mesma validação aos comandos de teleporte e aos drops.
- Não limpar dados nem reinstalar o app com exclusão de saves para testar a migração. Usar databases de teste e instalação de atualização.

## 4. Sequência de entrega

1. **Referência visual:** composição HD e cena no tamanho da câmera; registrar prompts, paleta e comparação da redução. Conservar o esboço junto a este plano.
2. **Mundo navegável:** definição, rios, circuitos, quatro conjuntos e quatro arenas; validar acessibilidade e escala com recursos temporários identificados como tais.
3. **Arte final por família:** integrar floresta/rios, castanheira e as quatro regiões, com transições e colisões; conferir em jogo após cada família.
4. **Integração:** mapa de exploração, distribuição ambiental de inimigos, encontros existentes, debug, carregamento por blocos e migração de saves.
5. **Validação final:** percorrer as regiões no Android, verificar memória/ciclo de vida e registrar capturas e resultados. Só então considerar a substituição do cenário concluída.

## 5. Critérios de aceite e testes

- **Composição:** capturas comprovam os cinco marcos, a mata viva, a região morta, as duas cores de água e a confluência. Cidade antiga e cidade moderna têm linguagens claramente distintas. A castanheira ocupa múltiplas telas sem tornar o personagem ilegível.
- **Garantias:** teste estrutural confirma um spawn, quatro conjuntos regionais, quatro arenas e 32 descobertas secundárias; todos dentro da área válida. A rede de rotas conecta o spawn aos marcos, arenas e descobertas; cada segmento é amostrado a cada 16 px com o raio de 32 px do jogador. Travessias adicionais dão acesso às margens norte e oeste do Negro.
- **Determinismo:** gerar os mesmos blocos em ordens diferentes produz terreno, decoração e colisões idênticos. Uma seed alternativa altera apenas detalhes autorizados e mantém construções, rios, caminhos e arenas.
- **Movimento:** verificar margem de rio, pontes, raízes, paredes, cantos, borda do círculo e movimento diagonal; spawn, teleporte e carregamento nunca deixam o jogador preso. Medir aproximadamente cinco minutos no diâmetro teórico com velocidade básica, sem depender do trajeto sinuoso.
- **Arte:** verificar dimensões, alpha, paleta, repetição e alinhamento entre frames; conferir costuras entre blocos e a ordenação de atores junto a copas e edifícios em todos os níveis de zoom.
- **Saves:** testar v7 → v8 com inventário/equipamentos/missões/pickups; nova partida, carga repetida e save perto de rio/obstáculo. Confirmar que a realocação do legado acontece uma única vez e que itens não se duplicam.
- **Desempenho:** caminho de 10 minutos com entrada nas quatro regiões e teleportes; caches permanecem limitados, sem crescimento proporcional ao mundo explorado ou varredura global por frame. Medir FPS, UPS, p95 de draw/update, memória Java/nativa e cache de bitmaps antes/depois no mesmo aparelho; buscar 60 UPS e o alvo selecionado de 30/60 FPS, registrando qualquer limite encontrado.
- **Verificação automatizada:** executar os testes relevantes do pipeline de imagem, testes de geração/colisão, migração instrumentada e `.\gradlew.bat assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug`. Executar os testes instrumentados de migração no dispositivo de teste e o roteiro manual de mapa, combate existente, salvar/carregar, pausa e retorno ao menu.

Suposições fechadas: orientação do esboço corresponde ao norte no topo; cinco minutos se referem ao diâmetro sem combate; exteriores apenas; regiões livres desde o início; seed fixa para todas as partidas; sem fast travel novo, navegação, interiores ou novos NPCs nesta entrega. A identidade e os assets dos quatro bosses serão decididos em trabalho posterior, sem bloquear os ambientes.

## Implementação entregue

- 1.920 × 1.920 tiles, raio de 936, seed `20261027L`; 299,52 s no diâmetro teórico a 400 px/s.
- 57 estruturas garantidas, incluindo 32 pequenas descobertas, quarteirões da cidade e ruínas adicionais.
- Rotas radiais, dois circuitos, acessos às quatro arenas, porto e travessias norte/oeste do Negro.
- Cache de chão limitado a 36 MiB; blocos lógicos de terreno limitados a 128 × 32 × 32 bytes. Assets e partes de cenário somam aproximadamente 19,6 MiB em RGBA antes de cópias auxiliares.
- Tronco e raízes em nove partes raster de até 512 px; copa com transparência de aproximadamente 35% quando cobre o jogador. A transparência é imediata nesta versão.
- Saves de revisão antiga reposicionam jogador e pickups preservados no núcleo; a migração Room 7→8 não apaga as demais tabelas.
- Não foram implementados novos bosses, interiores, navegação, NPCs ou fast travel. A identidade dos quatro bosses continua aberta.
