# Boitatá na Mata Morta

A luta acontece na `arena_forest`, em (42, 148) no mapa atual, marcada como **Arena do Boitatá**. O Boitatá começa dormindo no centro da clareira. O anel de basalto tem uma entrada ao norte e deixa o chão livre para esquivas. Cruzar a entrada e chegar a cinco tiles do centro inicia o despertar. Sair da clareira ou renascer encerra a tentativa e devolve o chefe ao centro com vida cheia. As ondas ficam suspensas dentro do anel. A Sentinela do Pântano usa a clareira próxima em (58, 124).

## Ritmo da luta

1. **Despertar (~2 s).** O corpo se desenrola e a cabeça sobe. A primeira chama se abre em leque, revelando os olhos flutuantes. Os olhos das escamas abrem em sequência, da cabeça para a cauda. A animação termina antes de o chefe atacar.
2. **Órbita (1,6–3,2 s).** O Boitatá desliza numa curva em torno do jogador. Ao iniciar cada volta, escolhe um trecho entre 110° e 240°, um raio entre 2,5 e 4 tiles e um sentido de giro. Mantém o sentido durante a curva, corrige o centro devagar quando o jogador se move e ajusta a curva para ficar dentro da arena. A cabeça acompanha o alvo enquanto o corpo segue a trajetória.
3. **Compromisso.** Escolhe um ataque pela distância, tempo desde o último uso e espaço disponível. O mesmo ataque não sai duas vezes seguidas. Mordida e rajada travam a direção nos últimos 22 ticks do aviso, dando tempo para esquivar. O cerco marca a posição do jogador desde o começo.
4. **Recuperação.** Depois de cada ataque, a cabeça baixa, parte dos olhos se fecha e o fogo encolhe. A cada dois ataques, o chefe para por 1,8 s. Essas pausas abrem tempo para se aproximar, atacar ou reorganizar a posição.
5. **Fogo azul (40% de vida).** Na próxima recuperação, o chefe faz uma pausa de 1,6 s. O fogo laranja se apaga por um instante, depois retorna azul e percorre o corpo, junto com a mudança de cor das escamas e a reabertura dos olhos. A velocidade de órbita fica igual. O desafio aumenta pelos padrões de ataque e pela área ocupada pelas chamas.

## Ataques

| Ataque | Aviso e execução | Recuperação | Fogo azul |
|---|---|---|---|
| Mordida | A cabeça recua por 0,85 s. O golpe avança em linha curta e a mandíbula fecha uma vez. | 1,2 s com a cabeça baixa. | Pode prolongar o alcance quando o jogador está no limite da mordida. A direção continua fixa. |
| Rajada | As chamas se juntam na boca por 1 s. Três bolas saem em leque, com a do meio apontada para a posição marcada. | 1,25 s. | Cinco bolas mais espaçadas no leque. O jogador ainda consegue passar entre elas. |
| Cerco de brasas | O corpo começa a fechar um anel em torno da posição marcada no chão. A abertura fica visível durante 1,35 s, antes de as chamas subirem em um único pulso. | 1,7 s, com o corpo se abrindo novamente. | Um rastro azul aparece no arco externo. A passagem permanece livre. Só é escolhido quando há espaço para a passagem e pelo menos dois ataques desde o último cerco. |

Durante a órbita, o corpo dá espaço para o jogador se reposicionar. A mordida, as bolas e o pulso têm janelas de dano próprias. A duração dos avisos fica igual nas duas fases para preservar o ritmo da luta.

## Recompensas

Após a queda e a extinção das chamas, o encontro deixa três materiais para craft futuro: **Pele de Boitatá**, **Dente Venenoso** e **Olho de Boitatá**. O olho é um globo esférico com íris âmbar e pupila vertical. A derrota, a experiência e os materiais são gravados no mesmo snapshot do slot. Os materiais ficam no chão até serem recolhidos, inclusive depois de sair do jogo. O Boitatá pode ser derrotado uma vez por slot.

## Atlases

Cada PNG é uma fila de três quadros, lidos da esquerda para a direita. A perspectiva é elevada em 3/4 e o pivô fica no centro de cada quadro. Órbita, mordida, rajada e recuperação têm uma vista lateral voltada à direita, que pode ser espelhada para a esquerda. O cerco tem uma vista mais elevada, com um espaço vazio no centro para desenhar o jogador. A trajetória no mundo e a direção da mordida e dos projéteis são controladas pelo encontro.

| Recurso em `drawable-nodpi` | Quadro | Sequência |
| --- | ---: | --- |
| `boitata_awaken` | 192 × 192 | Dorme → ergue a cabeça → se desenrola aceso. |
| `boitata_eyes` | 192 × 192 | Fechados → entreabertos → abertos. |
| `boitata_orbit` | 192 × 192 | Três poses de ondulação. Repetir durante a volta. |
| `boitata_bite` | 192 × 192 | Recua → dá o bote → recolhe a cabeça. O dano ocorre no segundo quadro. |
| `boitata_cast` | 192 × 192 | Carrega → dispara → abaixa a cabeça. Os projéteis saem no segundo quadro. |
| `boitata_encircle` | 192 × 192 | Forma um U → fecha em C → acende o arco. Ancorar no alvo marcado e manter a passagem livre. |
| `boitata_recover` | 192 × 192 | Cabeça baixa → repouso → volta a erguer. Sustentar o segundo quadro durante o descanso maior. |
| `boitata_phase_shift` | 192 × 192 | Fogo laranja diminui → chamas azuis surgem → corpo azul em combustão. |
| `boitata_defeat` | 192 × 192 | Cambaleia → desaba → fogo apaga. |
| `boitata_fireball` | 48 × 48 | Três quadros do projétil azul, apontado para a direita. Girar o próprio projétil na direção do disparo. |
| `boitata_loot` | 48 × 48 | Pele → dente → globo ocular. |
| `boitata_arena_props` | 128 × 128 | Trecho de muro → pilar → toco queimado. |

Os atlas de combate e projétil com sufixo `_ember` usam a paleta laranja nas mesmas poses. As fontes HD ficam em `art/hd`. Foram geradas por ImageGen com os [prompts de produção](prompts/boitata-sprites.md). O gerador usa a paleta `boitata` e o conversor do projeto, com redução nominal de 8× para pixels lógicos e ampliação de 2× por vizinho mais próximo para o Android.

```powershell
python tools/pixelize/build_boitata.py --install
python tools/pixelize/pixelize.py preview --prefix boitata_
```
