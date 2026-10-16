# Compilação e validação Android

Estado de 2026-10-16: build debug, APK instrumentado e 26 testes JVM aprovados;
Android Lint sem erros (31 avisos). Testes instrumentados e checklist físico abaixo pendentes.

## Comandos

Com JDK e SDK Android 34 configurados (`ANDROID_HOME` ou `local.properties`):

```sh
bash gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
adb devices
bash gradlew connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Não desinstalar o aplicativo nem limpar seus dados para testar migrações. Use cópias de saves
ou emuladores de teste. A atualização do APK deve preservar os dados anteriores.
Os testes de banco usam databases de teste, sem apagar o banco da partida.

## Roteiro de gameplay (API 28 e 34, telas/densidades diferentes)

- [ ] Iniciar nos três slots, pausar, salvar, retornar ao menu e reabrir cada um.
- [ ] Abrir INVENTARIO no HUD: conferir nome, quantidade, descrição, seleção e fechamento pelo X.
- [ ] Perder vida, coletar erva e usar pelo inventário; conferir cura, limite máximo e quantidade.
- [ ] Tentar usar erva com vida cheia e essência com mana cheia: não gastar item.
- [ ] Gastar mana com Tempestade e usar essência; usar bênção, tentar repetir imediatamente e aguardar expiração.
- [ ] Derrotar fantasmas normalmente: verificar loot, XP e progresso da missão uma única vez.
- [ ] Coletar cajado, equipar em cada um dos quatro slots e alternar com TROCA; slots vazios são ignorados.
- [ ] Mover a mesma arma para outro slot e confirmar que ela não aparece equipada duas vezes.
- [ ] Salvar/reabrir: conferir inventário, arma selecionada, missões e pickups ainda no chão, sem duplicação.
- [ ] Completar a missão de três fantasmas e reclamar recompensa no NPC: a Sentinela deve surgir.
- [ ] Reabrir encontro pendente: chefe reinicia com vida cheia; derrotá-lo e confirmar que não reaparece após salvar.
- [ ] Arremesso de Coco, direção de mira, Tempestade, fantasmas sociais/fusão e fases dos chefes funcionam.
- [ ] Projétil do jogador não causa dano a vários inimigos; projétil hostil não acerta mais de uma vez.
- [ ] No debug: `give erva`, `spawn erva`, `inventory`, `use erva` continuam disponíveis.
- [ ] No debug: `clear` remove inimigos/projéteis sem drops, XP ou progresso de missão.
- [ ] Encher a pilha de ervas (99): excedente fica no chão e há aviso de capacidade.
- [ ] Atingir 64 pilhas no chão: drops adicionais são descartados, sem crescimento ilimitado.
- [ ] Pickups expiram após 10 minutos de simulação ativa; interfaces modais/pausa congelam esse tempo.

## Input, Surface e concorrência

- [ ] Mover e mirar/atacar com dois dedos; soltar apenas um dedo e manter o outro funcionando.
- [ ] Abrir diálogo, atributos ou inventário durante movimento: jogador e ataques ficam parados.
- [ ] Cancelar gesto, bloquear a tela, trocar de app, girar tela e voltar: sem movimento preso.
- [ ] Pausar/retomar e destruir/recriar Surface repetidamente: sem loops duplicados, travamento ou crash.
- [ ] Entrar/sair rapidamente enquanto o save carrega: callbacks antigos não reiniciam a partida anterior.
- [ ] Retornar ao menu e abrir imediatamente o mesmo slot: dados recém-salvos devem aparecer.
- [ ] Excluir um slot e confirmar que seus itens/equipamentos/chão/missões sumiram; outros slots intactos.
- [ ] Jogador morto não pode mover, atacar, usar inventário ou disparar tempestade.

## Desempenho e recursos

- [ ] Monitorar FPS/UPS, tempo de update/draw, entidades e memória pelo overlay de debug.
- [ ] Usar profiler Android para comparar alocações e colisões com população próxima ao limite.
- [ ] Testar em aparelho modesto por sessão prolongada, incluindo muitas derrotas e pickups.
- [ ] Testar trim de memória, retorno ao menu e novas partidas: sem bitmaps reciclados em uso ou retenção de Activity.
- [ ] Verificar fallback de sprites e áudio com/sem assets opcionais.

[Relatório executado](validation/2026-10-16.md) · [Arquitetura e pendências](architecture-roadmap.md)
