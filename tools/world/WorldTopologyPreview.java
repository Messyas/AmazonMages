import com.example.amazonMages.map.WorldDefinition;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Diagnostic geometry preview only. It does not substitute for generated environment art. */
public final class WorldTopologyPreview {
    private static final int SIZE = 768;
    public static void main(String[] args) throws Exception {
        WorldDefinition world = new WorldDefinition();
        BufferedImage image = new BufferedImage(SIZE, SIZE + 70, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x15251e));
        g.fillRect(0, 0, SIZE, SIZE + 70);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int row = y * WorldDefinition.ROWS / SIZE, col = x * WorldDefinition.COLUMNS / SIZE;
            image.setRGB(x, y, color(world.sampleTile(row, col)));
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        for (WorldDefinition.Building building : world.buildings) {
            int left = px(building.base.col - building.width / 2);
            int top = px(building.base.row - building.depth);
            g.setColor(new Color(0xefe4bc));
            g.fillRect(left, top, Math.max(4, px(building.width)), Math.max(4, px(building.depth)));
        }
        for (WorldDefinition.Site site : world.discoveries) {
            g.setColor(new Color(0xc8d7a4));
            g.fillOval(px(site.center.col) - 3, px(site.center.row) - 3, 6, 6);
        }
        for (WorldDefinition.Site site : world.landmarks) {
            int x = px(site.center.col), y = px(site.center.row);
            g.setColor(new Color(0x15251e));
            g.fillRect(x - 75, y + 12, 150, 22);
            g.setColor(new Color(0xf4f1d2));
            String name = site.displayName;
            g.drawString(name, x - g.getFontMetrics().stringWidth(name) / 2, y + 28);
        }
        for (WorldDefinition.Site site : world.arenas) {
            int x = px(site.center.col), y = px(site.center.row), radius = px(7.5);
            g.setColor(new Color(0xf4d66a));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
        g.setColor(Color.WHITE);
        g.fillOval(px(world.spawn.col) - 5, px(world.spawn.row) - 5, 10, 10);
        g.setColor(new Color(0xf4f1d2));
        g.drawString("TOPOLOGY VALIDATION / 192 x 192 tiles / compact Vampire-Emerald world", 16, SIZE + 28);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.drawString("Diagnostic colors, paths and footprints; environment art is produced separately.", 16, SIZE + 52);
        g.dispose();
        File output = new File(args.length > 0 ? args[0] : "art/world-v1/world_topology.png");
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
        System.out.println(output.getPath());
    }
    private static int px(double tile) { return (int) Math.round(tile * SIZE / WorldDefinition.COLUMNS); }
    private static int color(WorldDefinition.Terrain terrain) {
        switch (terrain) {
            case NEGRO: case MEETING_DARK: return 0x202c42;
            case SOLIMOES: case MEETING_SILT: return 0x3e6e64;
            case BRIDGE: return 0xe7c087;
            case BORDER: return 0x15251e;
            case TREE: return 0x1e6436;
            case GRASS: return 0x3ca342;
            case GROUND: case PATH: return 0xb38455;
            case ACID_SWAMP: return 0x8fd12a;
            case BURNED: return 0x2f352e;
            case PAVING: return 0x837d6e;
            default: return 0x3c6a6b;
        }
    }
}
