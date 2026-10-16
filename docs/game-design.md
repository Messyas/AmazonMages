# AmazonMages — visão de gameplay

## Identidade

RPG de ação 2D para Android, com fantasia amazônica, magias, cajados e humor pontual. Os fantasmas ácidos possuem personalidades (cautelosa, agressiva, social), ataques à distância e fusão. O jogador deve atacar, se posicionar, defender e aproveitar aberturas.

## Armas e magias

O inventário poderá armazenar múltiplos itens, mas o combate terá **quatro espaços rápidos de armas**. Categorias: magia intrínseca (sem catalisador), catalisadores/cajados mágicos e armas físicas secundárias. A arma selecionada determina o ataque básico; cada magia intrínseca possui também um especial próprio. Um seletor separado servirá para curas e poções. Troca de arma e consumíveis devem ser acessíveis sem abrir menu.

### Magia inicial: Domínio dos Cocos

- **Arremesso de Coco:** conjura a fruta coco na mão e arremessa, com impacto físico e efeito sonoro cômico de pancada de desenho animado. **Não é cocô/fezes.**
- **Tempestade de Cocos:** invoca cocos (frutas) que caem numa área à frente do jogador; dano em área e pressão de postura. Os frutos precisam de sprites, queda visível, sombras/avisos no chão e som.
- Implementação atual é um protótipo; custos de mana, animações e balanceamento definitivo não foram fechados.

## Combate defensivo

Escudo Arcano desbloqueável e equipável: silhueta convencional de escudo feita de energia, protegendo **somente o arco frontal da mira** (~100° sugeridos), nunca 360°. Parry perfeito exige janela curta (~150 ms sugeridos); bloqueio normal gasta postura e pode quebrá-la. Parry perfeito pode conceder por ~2 s +20% velocidade e +20% resistência. Esses valores são propostas de balanceamento, não sistemas completos. Ataques laterais e traseiros devem continuar perigosos.

## Atributos (planejados)

Sete: Vigor (vida), Mente (mana), Resistência (defesa/postura), Inteligência (dano mágico), Destreza (agilidade/conjuração), Arcano (afinidades/efeitos) e Força (dano físico de cajados, ferramentas e armas físicas). Força **não** aumenta dano de feitiços. Considerar escalonamento por arma e limites suaves.

## Árvore de habilidades (planejada)

20 habilidades, quatro ramos de cinco: Proteção Arcana (escudo, parry, estabilidade, recuperação, bônus pós-parry), Domínio Mágico (conjuração, mana, intrínsecas, catalisadores, concentração), Mobilidade Mística (esquiva, passo, reposicionamento) e Combate com Cajado (golpes físicos, carregados, postura, contra-ataque). Desbloqueado não é necessariamente equipado. Evitar botões demais no celular; passivas não ocupam botões.

## Progressão e UX

Planejar XP/nível, pontos de atributo, pontos de habilidade, pré-requisitos e melhoria de equipamento. Menus de personagem, árvore e inventário precisam persistir por slot Room. HUD: movimento à esquerda, ataque/mira à direita, especial contextual, troca rápida, consumível, pausa e escudo quando equipado. Priorizar legibilidade, dimensionamento por tela e multitoque.
