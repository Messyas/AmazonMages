# Instituto Federal do Amazonas  
## Projeto Aranouá – Curso de Prototipação de Jogos Mobile 2D  

# Amazon Mages  

**Autor:** Messyas Gois França   

---

## Introdução

Este documento apresenta uma descrição detalhada do jogo mobile para dispositivos Android **Amazon Mages**, um RPG de aventura em 2D. O objetivo é fornecer uma visão clara e abrangente do jogo, cobrindo temática, narrativa, jogabilidade, design visual, personagens, fluxo de navegação e demais tópicos fundamentais.

---

## Temática

- **Gênero:** RPG de aventura  
- **Atmosfera:** Fofa e agradável, imersiva e relaxante  
- **Foco:** Aventuras do protagonista e sua companhia enfrentando desafios em diferentes cenários  

---

## Narrativa

### Personagens Principais

#### James
- Jovem, baixo, magro e de constituição levemente atlética  
- Cabelo castanho ondulado, olhos azul-claro  
- Vestimenta: casaco longo de mago vermelho, calça azul, botas de exploração  
- Chapéu de mago clássico com joia azul adornada em ouro  
- **Poder:** Manifestação de energia explosiva azul   

### Enredo

1. **Início:** James e Lavínia exploram uma floresta em busca de um monstro que rouba alimentos de uma vila.  
2. **Conflito:** Cercados por muitos monstros, abortam a missão, mas surgem hordas e o lendário mago Alexandre de Moraes, com cabeça resplandecente.  
3. **Reviravolta:** Alexandre, furioso ao supor emboscada pela academia, ordena a eliminação dos dois. Lavínia é raptada ao revelar-se uma elfa com potencial para seus planos.  
4. **Desfecho Momentâneo:** James derrota capangas, mas esgota sua mana e é salvo por um nobre, que o ajuda a se recuperar e fornece suprimentos.  
5. **Novo Objetivo:** Reunir aliados e informações para enfrentar as forças de Moraes e resgatar Lavínia, iniciando a jornada na região da casa do benfeitor, sob a maldição que faz espíritos atacarem plantações.

---

## Jogabilidade

### Mecânicas Principais
- **Exploração do mapa:** Cenários com desafios e histórias próprias.  
- **Combate:** Derrote monstros e sub-chefes para ganhar XP, aumentando dano e vida.  

### Iterações do Jogador
- **Movimentação:** Andar, correr, pular, agachar-se.  
- **Interação Ambiental:** Resolver puzzles (mover pedras, explorar troncos).  
- **Ataques & Defesa:** Variedade de magias ofensivas e escudos mágicos.  
- **Sistema de Batalha:** Enfrentamento contra diferentes classes de inimigos e bosses.  

---

## Design Visual

- **Estilo:** Pixel art 2D com gráficos retrô e animações suaves.  
- **Paleta de Cores:** Saturada e vibrante, com fases de tonalidade mais sombria conforme a narrativa.  
- **Ambientes:** Florestas exuberantes, masmorras sombrias, cavernas ameaçadoras.  
- **Detalhes:** Movimentos dos personagens, interação com objetos e efeitos visuais de magias que enriquecem a imersão.

---

## Primeira Fase

1. **Contexto:** James recupera-se na casa do nobre senhor.  
2. **Objetivo:** Coletar informações iniciais e seguir o trajeto informado por um NPC.  
3. **Zona 1:** Infestada por fantasmas radioativos de mana.  
4. **Boss Final:** Golem mutante contaminado por gás produzido pelos fantasmas.  

**Objetivos de Aprendizagem nesta Fase:**  
- Exploração Segura  
- Uso de sistema de defesa e ataque  
- Coleta e crafting de itens  

---

## Personagem Principal

- **Inspiração Visual:** Protagonista de Pokémon Emerald  
- **Traços:** Olhos azul-claro, manto vermelho-claro, botas e calças azul-marinho  
- **Item Icônico:** Chapéu de mago com adornos em ouro e pedra de mana azul  

### Poderes e Magias
- Magias derivadas da mana azul (energia principal)  
- Magias especiais com cores variadas (menos comuns)  

---

## Sprites e Elementos do Mundo

- Personagem jogável e animações de magias  
- Elementos de água, lava, pedregulhos e grama  
- Arbustos especiais que recuperam mana ao serem consumidos  

---

As aulas base para esta aplicacao podem ser encontradas em: https://www.youtube.com/watch?v=GIc2GRCUI8s&list=PL2EfDMM6n_LYJdzaOQ5jZZ3Dj5L4tbAuM

---

## Como Compilar e Rodar no Dispositivo Android (Sem Android Studio)

Este projeto está preparado para desenvolvimento leve via linha de comando ou assistido por IA, sem a necessidade de abrir o Android Studio.

### Pré-requisitos

- **JDK 17 LTS** (configurado no `gradle.properties` em `org.gradle.java.home`).
- **Android SDK / Command-line Tools & Platform-Tools (`adb`)** configurados no sistema.

---

### Passo a Passo

#### 1. Conectar o Dispositivo Android
1. No seu celular Android, acesse **Configurações > Opções do desenvolvedor** e ative a **Depuração USB** (USB Debugging).
2. Conecte o aparelho ao computador via cabo USB (permita a solicitação de depuração na tela do celular se aparecer).
3. Abra o terminal na raiz do projeto e certifique-se de que o dispositivo foi detectado:
   ```powershell
   adb devices
   ```
   *(O aparelho deve aparecer listado com o status `device`)*.

---

#### 2. Gerar o APK de Teste (Debug)
Para apenas compilar e gerar o arquivo APK:
```powershell
.\gradlew.bat assembleDebug
```
O arquivo APK compilado estará disponível em:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

#### 3. Instalar e Executar no Dispositivo Conectado

Você pode escolher qualquer uma das opções abaixo:

- **Opção A: Compilar e Instalar automaticamente (Recomendado)**
  ```powershell
  .\gradlew.bat installDebug
  ```

- **Opção B: Instalar o APK gerado via ADB**
  ```powershell
  adb install -r app\build\outputs\apk\debug\app-debug.apk
  ```

- **Opção C: Usar o Android CLI**
  ```powershell
  android run
  ```

---

### Comandos Úteis

- **Iniciar/Abrir o jogo no aparelho via terminal:**
  ```powershell
  adb shell am start -n com.example.androidstudio2dgamedevelopment/com.example.amazonMages.MainActivity
  ```
- **Acompanhar logs de execução do jogo no aparelho:**
  ```powershell
  adb logcat | Select-String "com.example.androidstudio2dgamedevelopment"
  ```
- **Desinstalar a versão de desenvolvimento do aparelho:**
  ```powershell
  adb uninstall com.example.androidstudio2dgamedevelopment
  ```
