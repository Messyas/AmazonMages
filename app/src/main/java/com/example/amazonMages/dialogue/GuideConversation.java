package com.example.amazonMages.dialogue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Local replies about the game, with a short history per open save. */
public final class GuideConversation {
    public static final String NAME = "Iara";
    public static final String PLAYER = "Você";
    private static final int HISTORY_LIMIT = 41;
    private static final DialogueSequence INTRO = new DialogueSequence("iara_welcome", Arrays.asList(
        iara("greet", "Ei, viajante! Chegue mais. Sou a Iara. Você está no Santuário das Folhas."),
        player("É impressão minha ou a floresta ficou mais quieta quando cruzei o círculo?"),
        iara("explain", "Foi a Castanheira. Dentro da marca você pode respirar e recuperar as forças. Quando sair, siga a trilha até se acostumar com a floresta."),
        player("E aquela fumaça ao sul? Vi de longe quando cheguei."),
        iara("think", "É a Mata Morta. O Boitatá dorme numa clareira lá. Dá para chegar pela entrada norte, mas eu esperaria até você pegar o jeito da esquiva."),
        player("E se ele acordar antes de eu conseguir sair?"),
        iara("warn", "Olhe o chão. Ele avisa antes de atacar. Se começar a se enrolar à sua volta, corra para a abertura. Quando baixar a cabeça para descansar, é sua vez."),
        player("Vou começar pelos arredores. Se eu me perder, volto aqui."),
        iara("greet", "Combinado! Estarei pelo círculo. Quando quiser, posso ajudar com o mapa, o combate ou a criação de itens.")
    ));
    private final List<DialogueLine> lines = new ArrayList<>();
    private int introIndex;
    private DialogueLine pendingReply;

    public List<DialogueLine> lines() { return Collections.unmodifiableList(lines); }
    public boolean hasIntroLine() { return introIndex < INTRO.lines.size(); }

    public DialogueLine nextIntroLine() {
        if (!hasIntroLine()) return null;
        return add(INTRO.lines.get(introIndex++));
    }

    public DialogueLine replayIntro() {
        introIndex = 0;
        return nextIntroLine();
    }

    public DialogueLine ask(String question) {
        String text = question.trim();
        if (text.isEmpty()) return null;
        introIndex = INTRO.lines.size();
        pendingReply = reply(text);
        return add(player(text));
    }

    public boolean hasReply() { return pendingReply != null; }

    public DialogueLine nextReply() {
        if (pendingReply == null) return null;
        DialogueLine line = pendingReply;
        pendingReply = null;
        return add(line);
    }

    private DialogueLine add(DialogueLine line) {
        lines.add(line);
        while (lines.size() > HISTORY_LIMIT) lines.remove(0);
        return line;
    }

    private DialogueLine reply(String text) {
        String message = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        if (has(message, "boitata", "mata morta", "chefe", "boss", "dente venenoso", "pele"))
            return iara("warn", "Procure a Arena do Boitatá no sudoeste do mapa. Entre pelo norte. Observe o aviso no chão e aproveite as pausas entre os ataques. Se ele se enrolar em volta de você, procure a abertura.");
        if (has(message, "craft", "cria", "receita", "alquimia", "material"))
            return iara("explain", "Abra a mochila e escolha a aba de criação. Ali você vê as receitas e os materiais necessários. A pele, o dente e o olho do Boitatá ficam guardados para receitas futuras.");
        if (has(message, "mapa", "caminho", "explor", "onde"))
            return iara("explain", "Abra o mapa pelo atalho do HUD. Arraste com um dedo e use dois para aproximar. O botão 1× mostra a floresta inteira. O santuário fica marcado em verde.");
        if (has(message, "parry", "combate", "luta", "ataq", "defen", "esquiv"))
            return iara("warn", "Use a mira para lançar cocos à distância. Quando um inimigo for acertar você, tente aparar no último instante. Contra um grupo, continue se movendo e use os caminhos abertos.");
        if (has(message, "habilidade", "arvore", "nivel", "evolu", "ponto"))
            return iara("explain", "Recolha os cristais dos inimigos para ganhar experiência. Na árvore de habilidades, toque numa habilidade para ver o custo e os requisitos. A aba Atributos mostra os pontos que você pode distribuir.");
        if (has(message, "comec", "inicio", "santuario", "cura", "vida", "ajuda"))
            return iara("greet", "Comece perto do círculo e pratique a mira com os cocos. Recolha a experiência e volte ao santuário se precisar recuperar vida ou mana antes de explorar mais longe.");
        if (has(message, "missao", "missoes", "diario"))
            return iara("explain", "Abra o diário pelo HUD para consultar suas missões e o progresso de cada uma.");
        if (has(message, "obrigad", "valeu"))
            return iara("greet", "De nada! Estarei andando pelo círculo quando você voltar.");
        return iara("think", "Posso conversar sobre como começar, combate, habilidades, criação, o mapa ou o Boitatá. Qual desses você quer conhecer?");
    }

    private static DialogueLine iara(String expression, String text) {
        return new DialogueLine(NAME, text, expression, null);
    }

    private static DialogueLine player(String text) {
        return new DialogueLine(PLAYER, text, "neutral", null);
    }

    private static boolean has(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
