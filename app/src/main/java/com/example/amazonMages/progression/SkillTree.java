package com.example.amazonMages.progression;

import com.example.amazonMages.save.QuestProgressEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java 3-branch GBA Skill Tree covering Combat Combos, Parry/Dash Defense,
 * and Amazonian Coconut/Herb Foraging & Alchemy. Persists inside quest_progress via "skill." IDs.
 */
public final class SkillTree {
    public static final String PERSIST_PREFIX = "skill.";

    public enum Branch {
        FURIA_AMAZONICA("Furia Amazonica"),
        GUARDIAO_DO_CIPO("Guardiao do Cipo"),
        SEIVA_DA_FLORESTA("Seiva da Floresta");

        public final String label;
        Branch(String label) { this.label = label; }
    }

    public static final class Node {
        public final String id;
        public final Branch branch;
        public final int tier;
        public final int maxRank;
        public final String prerequisiteId;
        public final String title;
        public final String summary;
        public final int pickupIconIndex;

        public Node(String id, Branch branch, int tier, int maxRank, String prerequisiteId,
                    String title, String summary, int pickupIconIndex) {
            this.id = id;
            this.branch = branch;
            this.tier = tier;
            this.maxRank = maxRank;
            this.prerequisiteId = prerequisiteId;
            this.title = title;
            this.summary = summary;
            this.pickupIconIndex = pickupIconIndex;
        }
    }

    private final Map<String, Node> nodesById = new LinkedHashMap<>();
    private final List<Node> orderedNodes;
    private final Map<String, Integer> ranks = new LinkedHashMap<>();

    public SkillTree() {
        register(new Node("golpe_triplo", Branch.FURIA_AMAZONICA, 0, 2, null,
            "Combo Amazonico",
            "+15%/+30% dano no 3o golpe do combo e -20%/-35% tempo de Disparo Carregado.", 7));
        register(new Node("cocos_explosivos", Branch.FURIA_AMAZONICA, 1, 2, "golpe_triplo",
            "Cocos Explosivos",
            "Cocos carregados, finalizadores e cada 3o coco automatico explodem em area (84px/112px).", 5));
        register(new Node("furia_castanheira", Branch.FURIA_AMAZONICA, 2, 1, "cocos_explosivos",
            "Mestre Ancestral",
            "Tempestade de Cocos e Vortice das Aguas duram +40% e Disparo Carregado lanca +2 projeteis.", 6));

        register(new Node("guarda_viva", Branch.GUARDIAO_DO_CIPO, 0, 2, null,
            "Guarda de Cipo",
            "+3/+6 ticks de Parry Perfeito, Guarda absorve 80%/88% de dano e +25/+50 Postura maxima.", 1));
        register(new Node("passo_curupira", Branch.GUARDIAO_DO_CIPO, 1, 2, "guarda_viva",
            "Rastro do Curupira",
            "-20%/-35% recarga do Dash e deixa brasas que causam dano e enraizam inimigos.", 2));
        register(new Node("manto_castanheira", Branch.GUARDIAO_DO_CIPO, 2, 1, "passo_curupira",
            "Manto da Castanheira",
            "A cada 8s sem dano na vida, cria um Escudo Vivo que anula 1 golpe, cura +1 Vida e +15 Mana.", 3));

        register(new Node("raizes_fartas", Branch.SEIVA_DA_FLORESTA, 0, 2, null,
            "Colheita Amazonica",
            "Arvores geram Cocos/Ervas +35%/+70% mais rapido e reserva automatica sobe para 12/16 Cocos.", 7));
        register(new Node("alquimia_viva", Branch.SEIVA_DA_FLORESTA, 1, 2, "raizes_fartas",
            "Alquimia do Pantano",
            "Ervas, Essencias e Cocos curam/recuperam +40%/+80% e concedem +20% velocidade por 3s.", 4));
        register(new Node("sinfonia_flora", Branch.SEIVA_DA_FLORESTA, 2, 1, "alquimia_viva",
            "Sinfonia da Floresta",
            "Coletar itens/cristais reduz recargas em 0.4s e 30% de chance de nao gastar consumiveis.", 0));

        orderedNodes = Collections.unmodifiableList(new ArrayList<>(nodesById.values()));
    }

    private void register(Node node) {
        nodesById.put(node.id, node);
    }

    public List<Node> nodes() { return orderedNodes; }
    public Node node(String id) { return id == null ? null : nodesById.get(id); }

    public List<Node> nodesForBranch(Branch branch) {
        List<Node> list = new ArrayList<>(3);
        for (Node n : orderedNodes) {
            if (n.branch == branch) list.add(n);
        }
        return list;
    }

    public int rank(String id) {
        Integer value = id == null ? null : ranks.get(id);
        return value == null ? 0 : value;
    }

    public int totalSpentPoints() {
        int sum = 0;
        for (Integer value : ranks.values()) sum += value;
        return sum;
    }

    public boolean canUnlock(String id, PlayerProgression progression) {
        if (progression == null || progression.skillPoints <= 0) return false;
        Node node = node(id);
        if (node == null || rank(id) >= node.maxRank) return false;
        return node.prerequisiteId == null || rank(node.prerequisiteId) > 0;
    }

    public boolean unlock(String id, PlayerProgression progression) {
        if (!canUnlock(id, progression)) return false;
        progression.skillPoints--;
        ranks.put(id, rank(id) + 1);
        return true;
    }

    public int respec(PlayerProgression progression) {
        int spent = totalSpentPoints();
        if (spent > 0 && progression != null) {
            progression.skillPoints += spent;
        }
        ranks.clear();
        return spent;
    }

    public int chargeTicksRequired() {
        int r = rank("golpe_triplo");
        return r >= 2 ? 18 : r == 1 ? 22 : 28;
    }

    public double comboFinisherBonus() {
        return 1.25 + rank("golpe_triplo") * 0.15;
    }

    public double explosiveCoconutRadius() {
        int r = rank("cocos_explosivos");
        return r >= 2 ? 112.0 : r == 1 ? 84.0 : 0.0;
    }

    public int chargedBonusProjectiles() {
        return rank("furia_castanheira") > 0 ? 2 : 0;
    }

    public double specialDurationMultiplier() {
        return rank("furia_castanheira") > 0 ? 1.40 : 1.0;
    }

    public int bonusParryTicks() {
        return rank("guarda_viva") * 3;
    }

    public double guardReduction() {
        int r = rank("guarda_viva");
        return r >= 2 ? 0.88 : r == 1 ? 0.80 : 0.70;
    }

    public double bonusPosture() {
        return rank("guarda_viva") * 25.0;
    }

    public int dashCooldownTicks() {
        int r = rank("passo_curupira");
        return r >= 2 ? 62 : r == 1 ? 76 : 96;
    }

    public boolean hasCurupiraTrail() {
        return rank("passo_curupira") > 0;
    }

    public int curupiraTrailRank() {
        return rank("passo_curupira");
    }

    public boolean hasLivingAegis() {
        return rank("manto_castanheira") > 0;
    }

    public int foragingIntervalTicks() {
        int r = rank("raizes_fartas");
        return r >= 2 ? 120 : r == 1 ? 155 : 210;
    }

    public int coconutReserveCap() {
        int r = rank("raizes_fartas");
        return r >= 2 ? 16 : r == 1 ? 12 : 8;
    }

    public int coconutRegrowTicks() {
        int r = rank("raizes_fartas");
        return r >= 2 ? 165 : r == 1 ? 220 : 300;
    }

    public double alchemyMultiplier() {
        int r = rank("alquimia_viva");
        return r >= 2 ? 1.8 : r == 1 ? 1.4 : 1.0;
    }

    public boolean hasFloraSymphony() {
        return rank("sinfonia_flora") > 0;
    }

    public void appendSnapshot(int slotId, List<QuestProgressEntity> target) {
        if (target == null) return;
        for (Node node : orderedNodes) {
            int r = rank(node.id);
            if (r > 0) {
                target.add(new QuestProgressEntity(slotId, PERSIST_PREFIX + node.id, "UNLOCKED", r));
            }
        }
    }

    public void restore(List<QuestProgressEntity> rows) {
        ranks.clear();
        if (rows == null) return;
        for (QuestProgressEntity row : rows) {
            if (row == null || row.questId == null || !row.questId.startsWith(PERSIST_PREFIX)) continue;
            String id = row.questId.substring(PERSIST_PREFIX.length());
            Node node = node(id);
            if (node == null) continue;
            int clamped = Math.max(0, Math.min(node.maxRank, row.progress));
            if (clamped > 0) ranks.put(node.id, clamped);
        }
    }
}

