import com.example.amazonMages.map.WorldDefinition;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

/** Offline overview from runtime geometry and installed raster assets; no startup world scan. */
public final class WorldOverviewExporter {
    public static void main(String[] args) throws Exception {
        WorldDefinition world = new WorldDefinition();
        int size = 768;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Map<String, BufferedImage> images = new HashMap<>();
        for (String name : new String[]{"grass", "earth", "negro", "solimoes", "burned", "paving", "bridge", "bark"})
            images.put(name, image("world_tile_" + name + "_v1"));
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int col = x * WorldDefinition.COLUMNS / size, row = y * WorldDefinition.ROWS / size;
            WorldDefinition.Terrain terrain = world.sampleTile(row, col);
            if (terrain == WorldDefinition.Terrain.BORDER) { result.setRGB(x, y, 0xff142a20); continue; }
            String material;
            switch (terrain) {
                case NEGRO: case MEETING_DARK: material = "negro"; break;
                case SOLIMOES: case MEETING_SILT: material = "solimoes"; break;
                case BURNED: case DEAD_TREE: material = "burned"; break;
                case PAVING: material = "paving"; break;
                case GROUND: case PATH: material = "earth"; break;
                case BRIDGE: material = "bridge"; break;
                default: material = "grass";
            }
            BufferedImage texture = images.get(material);
            int color = texture.getRGB((x * 7) % texture.getWidth(), (y * 11) % texture.getHeight());
            result.setRGB(x, y, color);
        }
        Graphics2D g = result.createGraphics();
        double scale = size / (double) WorldDefinition.COLUMNS;
        BufferedImage forest = image("world_tree_broadleaf_v1");
        for (int row = 0; row < WorldDefinition.ROWS / 8; row++)
            for (int col = 0; col < WorldDefinition.COLUMNS / 8; col++) {
                WorldDefinition.Point anchor = world.treeAtCell(row, col);
                if (anchor == null) continue;
                int x = (int) (anchor.col * scale), y = (int) (anchor.row * scale);
                g.drawImage(forest, x - 2, y - 5, 4, 6, null);
            }
        // Route overlays stay readable when sub-tile widths are lost by overview sampling.
        g.setColor(new Color(0xc8b27b));
        for (WorldDefinition.Route route : world.routes) for (int i = 1; i < route.points.size(); i++) {
            WorldDefinition.Point a = route.points.get(i - 1), b = route.points.get(i);
            g.drawLine((int) (a.col * scale), (int) (a.row * scale), (int) (b.col * scale), (int) (b.row * scale));
        }
        BufferedImage canopy = image("world_canopy_v1");
        g.setColor(new Color(0x6f4c30));
        g.fillRect((int) (936 * scale), (int) (850 * scale), (int) (48 * scale), (int) (110 * scale));
        g.drawImage(canopy, (int) (832 * scale), (int) (674 * scale), (int) (256 * scale), (int) (192 * scale), null);
        String[] icons = {"world_maloca_v1", "world_temple_v1", "world_burned_tree_v1", "world_theater_v1"};
        for (int i = 1; i < world.landmarks.size(); i++) {
            WorldDefinition.Site site = world.landmarks.get(i);
            BufferedImage icon = image(icons[i - 1]);
            int w = 70, h = w * icon.getHeight() / icon.getWidth();
            int x = (int) (site.center.col * scale), y = (int) (site.center.row * scale);
            g.drawImage(icon, x - w / 2, y - h, w, h, null);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        for (WorldDefinition.Site site : world.landmarks) {
            int x = (int) (site.center.col * scale), y = (int) (site.center.row * scale) + 14;
            int width = g.getFontMetrics().stringWidth(site.displayName);
            g.setColor(new Color(0xd9142a20, true)); g.fillRect(x - width / 2 - 4, y - 12, width + 8, 18);
            g.setColor(new Color(0xffede6c3, true)); g.drawString(site.displayName, x - width / 2, y + 1);
        }
        for (WorldDefinition.Site arena : world.arenas) {
            int x = (int) (arena.center.col * scale), y = (int) (arena.center.row * scale);
            g.setColor(new Color(0xffe1c265, true)); g.drawOval(x - 7, y - 7, 14, 14);
        }
        g.dispose();
        File destination = new File("app/src/main/res/drawable-nodpi/world_overview_v1.png");
        ImageIO.write(result, "png", destination);
        ImageIO.write(result, "png", new File("art/world-v1/world_runtime_overview.png"));
        System.out.println(destination.getPath());
    }
    private static BufferedImage image(String name) throws Exception {
        return ImageIO.read(new File("app/src/main/res/drawable-nodpi/" + name + ".png"));
    }
}
