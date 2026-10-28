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
            g.fillRect(left, top, Math.max(2, px(building.width)), Math.max(2, px(building.depth)));
        }
        for (WorldDefinition.Site site : world.discoveries) {
            g.setColor(new Color(0xc8d7a4));
            g.fillOval(px(site.center.col) - 2, px(site.center.row) - 2, 4, 4);
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
            int x = px(site.center.col), y = px(site.center.row), radius = px(36);
            g.setColor(new Color(0xf4d66a));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
        g.setColor(Color.WHITE);
        g.fillOval(px(world.spawn.col) - 4, px(world.spawn.row) - 4, 8, 8);
        g.setColor(new Color(0xf4f1d2));
        g.drawString("TOPOLOGY VALIDATION / 1920 x 1920 tiles / 5-minute reference diameter", 16, SIZE + 28);
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
            case SOLIMOES: case MEETING_SILT: return 0xa8784f;
            case BRIDGE: return 0xe7c087;
            case BORDER: return 0x15251e;
            case TREE: return 0x284e39;
            case GRASS: return 0x638249;
            case GROUND: case PATH: return 0xc6ac6e;
            case ACID_SWAMP: return 0xa9b956;
            case BURNED: return 0x514d49;
            case PAVING: return 0xacb29d;
            default: return 0x3c6a6b;
        }
    }
}
