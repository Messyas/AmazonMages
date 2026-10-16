# Checklist de compilação e validação

Nenhum teste desta lista foi confirmado nesta sessão. Marcar somente após executar.

## Build

- [ ] Sincronizar Gradle e compilar APK debug no Android Studio ou CI.
- [ ] Verificar imports, referências a recursos `R.drawable`, compatibilidade Java e warnings relevantes.
- [ ] Iniciar uma jogatina nova em cada um dos três slots.

## Combate

- [ ] Arremesso de Coco aparece como fruta, dispara na direção de mira e atinge fantasma.
- [ ] Mira funciona sem movimentação e com dois dedos simultâneos.
- [ ] Tempestade de Cocos aplica impactos na área, respeita recarga e não trava com muitos inimigos.
- [ ] Troca de arma ignora slots vazios; não tenta executar arma sem implementação.
- [ ] Dano e vida fracionária funcionam com resistências e vida próxima de zero.
- [ ] Fusão, morte e projéteis inimigos continuam funcionando.
- [ ] Postura recupera sem provocar efeitos de stun não implementados.

## Interface e ciclo de vida

- [ ] Joystick e botões não se sobrepõem em resoluções e densidades diferentes.
- [ ] Pausar/retomar várias vezes não cria múltiplos loops nem congela o jogo.
- [ ] Salvar, retornar ao menu, reabrir e excluir slots não causa crash.
- [ ] Desligar a tela e voltar mantém controles funcionais.
- [ ] Game over e performance overlay permanecem legíveis.

## Assets e desempenho

- [ ] Sem sprites novos: fallback Canvas/ghost original funciona.
- [ ] Com sprites novos: transparência, escala, atlas e direção corretos.
- [ ] Nenhum bitmap é decodificado a cada frame.
- [ ] Testar estabilidade e consumo de memória em Android de hardware modesto.

## Atenções técnicas conhecidas

- `Game.java` ainda concentra input, renderização do HUD, tempestade e lógica de loop: extrair gradualmente.
- `WeaponLoadout` suporta slots de dados, não a execução completa de todas as categorias.
- Posições de botões são proporcionais à tela; joystick permanece em coordenadas fixas.
- Ainda não há teste automatizado comprovado para novos sistemas.
