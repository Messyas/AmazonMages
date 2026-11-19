package com.example.amazonMages.progression;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.amazonMages.gamepanel.ForestHud;
import com.example.amazonMages.ui.PixelUiKit;
import java.util.List;
import java.util.Locale;

/**
 * Tabbed GBA pixel-art modal hosting the 3-branch Skill Tree (ui_skill_medallion + ui_skill_icons)
 * and the 7-attribute / derived combat summary screen (ui_bar_frame + ui_stepper).
 */
public final class AttributesPanel {
    private static final String[] KEYS = {"vigor", "mente", "resistencia", "inteligencia", "destreza", "arcano", "forca"};
    private static final String[] LABELS = {"Vigor", "Mente", "Resistencia", "Inteligencia", "Destreza", "Arcano", "Forca"};
    private static final SkillTree.Branch[] BRANCHES = SkillTree.Branch.values();

    private final Paint shapePaint = new Paint();
    private final RectF panelRect = new RectF();
    private final RectF closeRect = new RectF();
    private final RectF tabTreeRect = new RectF();
    private final RectF tabAttrRect = new RectF();
    private final RectF inspectorRect = new RectF();
    private final RectF learnRect = new RectF();
    private final RectF respecRect = new RectF();
    private final RectF bottomCloseRect = new RectF();
    private final RectF barRect = new RectF();
    private final RectF[][] nodeRects = new RectF[3][3];
    private final RectF[] attrPlusRects = new RectF[KEYS.length];

    private float treeSplitX;

    private PixelUiKit fallbackUiKit;
    private SkillTree skillTree;
    private String selectedNodeId = "golpe_triplo";
    private int activeTab = 0; // 0 = Habilidades (Skill Tree), 1 = Atributos
    private boolean open;

    public AttributesPanel() {
        shapePaint.setAntiAlias(false);
        shapePaint.setDither(false);
        for (int b = 0; b < 3; b++) {
            for (int t = 0; t < 3; t++) nodeRects[b][t] = new RectF();
        }
        for (int i = 0; i < KEYS.length; i++) attrPlusRects[i] = new RectF();
    }

    public void attachSkillTree(Context context, SkillTree skillTree) {
        this.skillTree = skillTree;
        if (context != null && fallbackUiKit == null) {
            fallbackUiKit = new PixelUiKit(context);
        }
    }

    public boolean isOpen() { return open; }
    public void toggle() {
        open = !open;
        if (open && skillTree != null) activeTab = 0;
    }
    public void close() {
        open = false;
    }
    public void selectTab(int tab) {
        if (tab == 0 || tab == 1) activeTab = tab;
    }

    public boolean touch(float x, float y, int width, int height, PlayerProgression stats) {
        if (!open) return false;
        computeLayout(width, height);

        if (!panelRect.contains(x, y) || closeRect.contains(x, y)) {
            close();
            return true;
        }
        if (skillTree == null) return handleLegacyTouch(x, y, stats);

        if (tabTreeRect.contains(x, y)) {
            activeTab = 0;
            return false;
        }
        if (tabAttrRect.contains(x, y)) {
            activeTab = 1;
            return false;
        }

        if (activeTab == 0) {
            for (int b = 0; b < BRANCHES.length; b++) {
                List<SkillTree.Node> branchNodes = skillTree.nodesForBranch(BRANCHES[b]);
                for (int t = 0; t < branchNodes.size() && t < 3; t++) {
                    RectF r = nodeRects[b][t];
                    if (r.contains(x, y)) {
                        selectedNodeId = branchNodes.get(t).id;
                        return false;
                    }
                }
            }
            if (learnRect.contains(x, y)) return skillTree.unlock(selectedNodeId, stats);
            if (respecRect.contains(x, y)) return skillTree.respec(stats) > 0;
            return false;
        }

        if (bottomCloseRect.contains(x, y)) {
            close();
            return true;
        }
        for (int i = 0; i < attrPlusRects.length; i++) {
            RectF r = attrPlusRects[i];
            if (r.contains(x, y)) {
                return stats.allocate(KEYS[i]);
            }
        }
        return false;
    }

    private boolean handleLegacyTouch(float x, float y, PlayerProgression stats) {
        if (bottomCloseRect.contains(x, y)) {
            close();
            return true;
        }
        for (int i = 0; i < attrPlusRects.length; i++) {
            if (attrPlusRects[i].contains(x, y)) return stats.allocate(KEYS[i]);
        }
        return false;
    }

    public void draw(Canvas canvas, int width, int height, PlayerProgression stats, ForestHud hud) {
        if (!open || width <= 0 || height <= 0) return;
        PixelUiKit ui = hud != null ? hud.uiKit() : fallbackUiKit;
        if (ui == null) return;

        computeLayout(width, height);
        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(0xA406100A);
        canvas.drawRect(0, 0, width, height, shapePaint);

        ui.drawPanel(canvas, panelRect);
        float ribbonW = Math.min(panelRect.width() * .34f, 260f * Math.max(2, ui.scale()));
        float ribbonH = Math.max(46f, Math.min(58f, panelRect.height() * .095f));
        String ribbonTitle = skillTree == null || activeTab == 1 ? "ATRIBUTOS" : "HABILIDADES";
        ui.drawTitleRibbon(canvas, panelRect.centerX(), panelRect.top - ribbonH * 0.42f, ribbonW, ribbonH, ribbonTitle);
        ui.drawCloseButton(canvas, closeRect, false);

        if (skillTree == null) {
            drawAttributesView(canvas, ui, stats, false);
            return;
        }

        ui.drawTab(canvas, tabTreeRect, PixelUiKit.ICON_ATTRIBUTES, -1,
            "ARVORE (" + stats.skillPoints + ")", activeTab == 0);
        ui.drawTab(canvas, tabAttrRect, PixelUiKit.ICON_SWORD, -1,
            "ATRIBUTOS (" + stats.attributePoints + ")", activeTab == 1);

        if (activeTab == 0) {
            drawSkillTreeView(canvas, ui, stats);
        } else {
            drawAttributesView(canvas, ui, stats, true);
        }
    }

    private void drawSkillTreeView(Canvas canvas, PixelUiKit ui, PlayerProgression stats) {
        ui.drawDividerV(canvas, treeSplitX, tabTreeRect.bottom + 8f, panelRect.bottom - 12f);

        for (int b = 0; b < BRANCHES.length; b++) {
            SkillTree.Branch branch = BRANCHES[b];
            List<SkillTree.Node> branchNodes = skillTree.nodesForBranch(branch);
            RectF first = nodeRects[b][0];
            int branchColor = b == 0 ? 0xFFF4AE8A : b == 1 ? 0xFF9EE6A8 : 0xFF7CC4F0;
            ui.drawTextShadow(canvas, branch.label.toUpperCase(Locale.ROOT),
                first.left + 2f, first.top - 4f, Math.max(10f, panelRect.height() * .029f),
                branchColor, true, Paint.Align.LEFT);

            // Pixel-bordered horizontal wooden/gold branch connectors between Tier 1 -> 2 -> 3
            for (int t = 0; t < branchNodes.size() - 1; t++) {
                RectF cur = nodeRects[b][t];
                RectF nxt = nodeRects[b][t + 1];
                boolean activeLink = skillTree.rank(branchNodes.get(t).id) > 0;
                float medSize = nodeMedallionSize(cur);
                float cy = cur.top + medSize * .5f;
                float linkLeft = cur.centerX() + medSize * .36f;
                float linkRight = nxt.centerX() - medSize * .36f;
                shapePaint.setColor(0xFF0B1A12);
                canvas.drawRect(linkLeft, cy - 5f, linkRight, cy + 5f, shapePaint);
                shapePaint.setColor(activeLink ? 0xFFC99A2E : 0xFF2E3B2A);
                canvas.drawRect(linkLeft, cy - 3f, linkRight, cy + 3f, shapePaint);
                if (activeLink) {
                    shapePaint.setColor(0xFFF2D36B);
                    canvas.drawRect(linkLeft, cy - 1.5f, linkRight, cy + 1.5f, shapePaint);
                }
            }

            for (int t = 0; t < branchNodes.size() && t < 3; t++) {
                SkillTree.Node node = branchNodes.get(t);
                int iconIndex = b * 3 + t;
                drawMedallionNode(canvas, ui, nodeRects[b][t], node, iconIndex, stats);
            }
        }

        // Right Inspector Card
        ui.drawSubPanel(canvas, inspectorRect);
        SkillTree.Node sel = skillTree.node(selectedNodeId);
        if (sel == null) return;
        int rank = skillTree.rank(sel.id);
        int selIdx = Math.max(0, skillTree.nodes().indexOf(sel));
        int medallionState = medallionStateFor(sel, rank, stats);

        float cx = inspectorRect.centerX();
        float medSize = Math.min(inspectorRect.width() * .30f, inspectorRect.height() * .18f);
        RectF medRect = new RectF(cx - medSize * .5f, inspectorRect.top + 12f,
            cx + medSize * .5f, inspectorRect.top + 12f + medSize);
        ui.drawSkillMedallion(canvas, medRect, medallionState, selIdx, false);

        float titleSize = Math.max(13f, inspectorRect.height() * .050f);
        float titleY = medRect.bottom + titleSize * 1.15f;
        ui.drawTextShadow(canvas, sel.title, cx, titleY, titleSize, 0xFFFFE082, true, Paint.Align.CENTER);

        // Rank progress bar
        float barW = inspectorRect.width() * .56f;
        float barH = Math.max(14f, Math.min(26f, inspectorRect.height() * .050f));
        barRect.set(cx - barW * .5f, titleY + 10f, cx + barW * .5f, titleY + 10f + barH);
        ui.drawBar(canvas, barRect, rank / (float) Math.max(1, sel.maxRank), rank >= sel.maxRank ? 1 : 0);
        float rankSize = Math.max(10.5f, inspectorRect.height() * .033f);
        ui.drawTextShadow(canvas, "NIVEL " + rank + "/" + sel.maxRank, cx, barRect.bottom + rankSize * 1.25f,
            rankSize, 0xFF9BF06A, true, Paint.Align.CENTER);

        float infoY = barRect.bottom + rankSize * 2.55f;
        if (sel.prerequisiteId != null) {
            SkillTree.Node pre = skillTree.node(sel.prerequisiteId);
            boolean met = skillTree.rank(sel.prerequisiteId) > 0;
            String reqLabel = (met ? "Desbloqueado por: " : "Requer: ") + (pre != null ? pre.title : sel.prerequisiteId);
            if (!met) {
                ui.drawUiIcon(canvas, PixelUiKit.ICON_LOCK, inspectorRect.left + 20f, infoY - 4f, 16f, 255);
            }
            ui.drawTextShadow(canvas, reqLabel, cx, infoY, Math.max(9.8f, inspectorRect.height() * .031f),
                met ? 0xFF9EE6A8 : 0xFFF4AE8A, false, Paint.Align.CENTER);
            infoY += 18f;
        }

        ui.drawWrappedText(canvas, sel.summary, cx, infoY + 2f, inspectorRect.width() * .88f,
            Math.max(10.5f, inspectorRect.height() * .036f), 0xFFF4EBC8, false, Paint.Align.CENTER);

        boolean canLearn = skillTree.canUnlock(sel.id, stats);
        boolean maxed = rank >= sel.maxRank;
        String learnText = maxed ? "NIVEL MAXIMO" : canLearn ? "APRENDER (1 PT)"
            : (stats.skillPoints <= 0 ? "SEM PONTOS" : "BLOQUEADA");
        int learnState = maxed ? 1 : (canLearn ? 0 : 2);
        ui.drawButton(canvas, learnRect, learnText, learnState, maxed ? 0xFFFFF1B0 : 0);

        boolean canRespec = skillTree.totalSpentPoints() > 0;
        ui.drawButton(canvas, respecRect, "RESETAR (" + skillTree.totalSpentPoints() + ")",
            canRespec ? 0 : 2, canRespec ? 0xFFFFC4B0 : 0);
    }

    private int medallionStateFor(SkillTree.Node node, int rank, PlayerProgression stats) {
        if (rank >= node.maxRank) return 3;
        if (rank > 0) return 2;
        return skillTree.canUnlock(node.id, stats) ? 1 : 0;
    }

    private void drawMedallionNode(Canvas canvas, PixelUiKit ui, RectF slotRect, SkillTree.Node node,
                                   int iconIndex, PlayerProgression stats) {
        int rank = skillTree.rank(node.id);
        boolean selected = node.id.equals(selectedNodeId);
        int state = medallionStateFor(node, rank, stats);

        float medSize = nodeMedallionSize(slotRect);
        RectF medRect = new RectF(slotRect.centerX() - medSize * .5f, slotRect.top,
            slotRect.centerX() + medSize * .5f, slotRect.top + medSize);
        ui.drawSkillMedallion(canvas, medRect, state, iconIndex, selected);

        float labelSize = Math.max(9f, Math.min(panelRect.height() * .023f, slotRect.width() * .135f));
        String[] parts = node.title.split("\\s+");
        String shortLabel = parts.length >= 2 && node.title.length() > 13
            ? parts[0] + " " + parts[parts.length - 1].substring(0, Math.min(4, parts[parts.length - 1].length())) + "."
            : node.title;
        int titleColor = selected ? 0xFFFFF1B0 : (state >= 1 ? 0xFFF4EBC8 : 0xFF8C9F8B);
        ui.drawTextShadow(canvas, shortLabel, slotRect.centerX(), medRect.bottom + labelSize * 1.02f,
            labelSize, titleColor, selected || rank > 0, Paint.Align.CENTER);

        String rankLabel = rank >= node.maxRank ? "MAX" : rank + "/" + node.maxRank;
        int rankColor = state == 3 ? 0xFFF2D36B : state == 2 ? 0xFF9BF06A : state == 1 ? 0xFF8BE065 : 0xFF7D8C78;
        ui.drawTextShadow(canvas, rankLabel, medRect.right - 2f, medRect.bottom - 2f,
            Math.max(9.5f, labelSize * 1.05f), rankColor, true, Paint.Align.RIGHT);
    }

    private float nodeMedallionSize(RectF slot) {
        return Math.max(1f, Math.min(slot.width() * .90f, slot.height() * .81f));
    }

    private void drawAttributesView(Canvas canvas, PixelUiKit ui, PlayerProgression stats, boolean hasTabs) {
        float topBase = hasTabs ? tabTreeRect.bottom + 8f : panelRect.top + 38f;
        ui.drawTextShadow(canvas, "NIVEL " + stats.level + "  ·  XP " + stats.experience
                + "  ·  PONTOS: " + stats.attributePoints,
            panelRect.left + 20f, topBase + 16f, Math.max(11.5f, panelRect.height() * .034f),
            0xFFFFE082, true, Paint.Align.LEFT);

        float splitX = panelRect.left + panelRect.width() * .53f;
        ui.drawDividerV(canvas, splitX, topBase + 24f, bottomCloseRect.top - 10f);

        int[] values = {stats.vigor, stats.mente, stats.resistencia, stats.inteligencia,
            stats.destreza, stats.arcano, stats.forca};
        for (int i = 0; i < KEYS.length; i++) {
            RectF plus = attrPlusRects[i];
            float labelSize = Math.max(11f, Math.min(panelRect.height() * .034f, plus.height() * .60f));
            ui.drawTextShadow(canvas, LABELS[i], panelRect.left + 22f, plus.centerY() + labelSize * .32f,
                labelSize, 0xFFF4EBC8, true, Paint.Align.LEFT);

            float meterLeft = panelRect.left + panelRect.width() * .19f;
            float meterRight = plus.left - 36f;
            if (meterRight > meterLeft + 32f) {
                float meterH = Math.max(22f, Math.min(30f, plus.height() * .74f));
                barRect.set(meterLeft, plus.centerY() - meterH * .5f, meterRight, plus.centerY() + meterH * .5f);
                ui.drawBar(canvas, barRect, Math.min(1f, values[i] / 25f), i <= 1 ? 0 : 1);
            }
            ui.drawTextShadow(canvas, Integer.toString(values[i]), plus.left - 10f, plus.centerY() + labelSize * .32f,
                labelSize, 0xFFFFF1B0, true, Paint.Align.RIGHT);

            ui.drawStepper(canvas, plus, stats.attributePoints > 0);
        }

        // Right derived combat summary box
        inspectorRect.set(splitX + 10f, topBase + 26f, panelRect.right - 18f, bottomCloseRect.top - 10f);
        ui.drawSubPanel(canvas, inspectorRect);
        float sumTitleSize = Math.max(13f, inspectorRect.height() * .055f);
        float sumTitleY = inspectorRect.top + sumTitleSize * 1.25f;
        ui.drawTextShadow(canvas, "RESUMO DE COMBATE", inspectorRect.centerX(), sumTitleY,
            sumTitleSize, 0xFFFFE082, true, Paint.Align.CENTER);
        float divY = sumTitleY + 14f;
        ui.drawDividerH(canvas, inspectorRect.left + 12f, inspectorRect.right - 12f, divY);

        int bonusPosture = skillTree != null ? (int) Math.round(skillTree.bonusPosture()) : 0;
        int magnetPx = (int) Math.round(145.0 + Math.max(0, stats.arcano - 1) * 24.0);
        int[] miniIcons = {
            PixelUiKit.ICON_HEART, PixelUiKit.ICON_MANA, PixelUiKit.ICON_SHIELD,
            PixelUiKit.ICON_SWORD, PixelUiKit.ICON_ATTRIBUTES, PixelUiKit.ICON_MAP, PixelUiKit.ICON_BAG
        };
        String[] summaryLines = {
            "Vida Maxima: " + stats.maximumHealth() + " coracoes",
            "Mana Maxima: " + (int) stats.manaCapacity(),
            "Postura Maxima: " + (100 + bonusPosture),
            "Dano Cocos: " + Math.round(stats.physicalMultiplier() * 100) + "%",
            "Dano Magico: " + Math.round(stats.magicMultiplier() * 100) + "%",
            "Cadencia: " + Math.round(stats.castSpeed() * 100) + "%",
            "Iman de XP: " + magnetPx + " px"
        };
        float startY = divY + 26f;
        float lineStep = Math.max(18f, (inspectorRect.bottom - startY - 12f) / summaryLines.length);
        float textSize = Math.max(10.5f, Math.min(inspectorRect.height() * .048f, lineStep * .58f));
        for (int i = 0; i < summaryLines.length; i++) {
            float cy = startY + i * lineStep;
            ui.drawUiIcon(canvas, miniIcons[i], inspectorRect.left + 22f, cy - 3f, Math.max(14f, textSize * 1.1f), 255);
            ui.drawTextShadow(canvas, summaryLines[i], inspectorRect.left + 36f, cy + textSize * .28f,
                textSize, 0xFFD8ECC8, false, Paint.Align.LEFT);
        }

        ui.drawButton(canvas, bottomCloseRect, "FECHAR", 0);
    }

    private void computeLayout(int width, int height) {
        panelRect.set(width * .026f, height * .065f, width * .974f, height * .967f);
        float pw = panelRect.width(), ph = panelRect.height();
        float closeSize = Math.max(38f, Math.min(52f, ph * .09f));
        closeRect.set(panelRect.right - closeSize - 10f, panelRect.top + 8f,
            panelRect.right - 10f, panelRect.top + 8f + closeSize);

        float ribbonH = Math.max(46f, Math.min(58f, ph * .095f));
        float tabTop = panelRect.top + ribbonH * .58f + 4f;
        float tabH = Math.max(40f, Math.min(48f, ph * .095f));
        float tabW = Math.min(pw * .28f, 235f);
        tabTreeRect.set(panelRect.left + 16f, tabTop, panelRect.left + 16f + tabW, tabTop + tabH);
        tabAttrRect.set(tabTreeRect.right + 10f, tabTop, tabTreeRect.right + 10f + tabW, tabTop + tabH);

        treeSplitX = panelRect.left + pw * .65f;
        float bodyTop = tabTreeRect.bottom + 9f;
        float bodyBottom = panelRect.bottom - 14f;
        float treeLeft = panelRect.left + 12f;
        float treeRight = treeSplitX - 12f;
        float laneH = (bodyBottom - bodyTop) / 3f;
        float gapX = Math.min(14f, (treeRight - treeLeft) * .026f);
        float nodeW = (treeRight - treeLeft - gapX * 2f) / 3f;
        for (int b = 0; b < 3; b++) {
            float laneTop = bodyTop + b * laneH + Math.max(12f, ph * .031f);
            float laneBottom = bodyTop + (b + 1) * laneH - 4f;
            for (int t = 0; t < 3; t++) {
                float nl = treeLeft + t * (nodeW + gapX);
                nodeRects[b][t].set(nl, laneTop, nl + nodeW, laneBottom);
            }
        }

        inspectorRect.set(treeSplitX + 10f, tabTreeRect.bottom + 10f, panelRect.right - 14f, panelRect.bottom - 14f);
        float btnH = Math.max(30f, Math.min(54f, inspectorRect.height() * .13f));
        float insBtnW = Math.min(280f, inspectorRect.width() - 28f);
        float insCx = inspectorRect.centerX();
        respecRect.set(insCx - insBtnW * .5f, inspectorRect.bottom - btnH - 10f,
            insCx + insBtnW * .5f, inspectorRect.bottom - 10f);
        learnRect.set(insCx - insBtnW * .5f, respecRect.top - btnH - 8f,
            insCx + insBtnW * .5f, respecRect.top - 8f);

        float closeBtnW = Math.min(260f, pw * .32f);
        bottomCloseRect.set(panelRect.centerX() - closeBtnW * .5f, panelRect.bottom - btnH - 12f,
            panelRect.centerX() + closeBtnW * .5f, panelRect.bottom - 12f);

        float attrLeft = panelRect.left + 20f;
        float attrRight = panelRect.left + pw * .53f - 12f;
        float rowTop = (skillTree == null ? panelRect.top + 64f : tabTreeRect.bottom + 36f);
        float availH = bottomCloseRect.top - 12f - rowTop;
        float rowHeight = Math.max(1f, availH / KEYS.length);
        float stepSize = Math.max(1f, Math.min(38f, rowHeight - 2f));
        for (int i = 0; i < KEYS.length; i++) {
            float cy = rowTop + i * rowHeight + rowHeight * .5f;
            attrPlusRects[i].set(attrRight - stepSize, cy - stepSize * .5f, attrRight, cy + stepSize * .5f);
        }
    }
}
