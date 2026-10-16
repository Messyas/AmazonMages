# Sistema de áudio do AmazonMages

## Arquitetura

- `audio/GameAudioManager.java`: gerenciador único pertencente à Activity, com `MediaPlayer` para música em loop e `SoundPool` para efeitos curtos.
- `MainActivity`: instancia uma vez, chama `resume()` e `pause()` conforme o ciclo de vida e `release()` em `onDestroy()`. O menu principal e o menu de pausa oferecem sliders independentes para música e efeitos.
- Configurações persistidas com `SharedPreferences` sob `audio_settings`, chaves `music` e `effects`.
- Requisição de foco de áudio `AudioManager` (API legada, compatível com o projeto); interrupções suspendem a música.
- `Game`: emite eventos para ataque, Tempestade de Cocos, derrota de fantasma e diálogo. Não inicia mais o `MusicService`; declaração do serviço removida do Manifest.
- Música existente: `R.raw.videoplayback`. O arquivo e seu conteúdo não foram substituídos.

## Arquivos de efeitos opcionais

Coloque os seguintes arquivos em `app/src/main/res/raw/` (nomes exatos, extensão suportada pelo Android, como `.ogg`):

| Evento | Nome do recurso |
| --- | --- |
| Ataque básico | `sfx_attack` |
| Dano recebido | `sfx_hit` |
| Fantasma derrotado | `sfx_ghost_defeat` |
| Tempestade de Cocos | `sfx_storm` |
| Diálogo | `sfx_dialogue` |
| Subida de nível | `sfx_level_up` |
| Interface | `sfx_menu` |

O gerenciador consulta os recursos por nome; se um arquivo não existir, **não reproduz o efeito e não quebra o jogo**. Os eventos `hit`, `level_up` e `menu` ainda não estão todos conectados a eventos reais; os nomes estão reservados. Nenhum arquivo de efeito foi adicionado nesta etapa.

## Validação pendente

1. Compilar Android, validar versões de SDK e permissões de foco de áudio.
2. Confirmar que a música toca, pausa, retoma e libera recursos ao alternar entre apps, menus e partidas.
3. Testar volumes em 0%, 50% e 100%, inclusive após reiniciar o app.
4. Adicionar e testar arquivos OGG/WAV licenciados para uso no projeto.
5. Testar concorrência de efeitos, latência, fones Bluetooth e chamadas telefônicas.
6. Integrar `hit` e `level_up` e avaliar atenuação da música durante diálogos.
