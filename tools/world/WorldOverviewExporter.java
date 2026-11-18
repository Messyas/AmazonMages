import com.example.amazonMages.map.WorldDefinition;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

/** Offline overview from runtime geometry and cohesive Emerald raster assets; no startup world scan. */
public final class WorldOverviewExporter {
    public static void main(String[] args) throws Exception {
        WorldDefinition world = new WorldDefinition();
        int size = 768;
        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Map<String, BufferedImage> images = new HashMap<>();
        images.put("grass", image("emerald_tiles_grass"));
        images.put("path", image("emerald_tiles_path"));
        images.put("negro", image("emerald_tiles_water_negro"));
        images.put("solimoes", image("emerald_tiles_water_solimoes"));
        images.put("swamp", image("emerald_tiles_swamp"));
        images.put("paving", image("emerald_tiles_paving"));
        images.put("bridge", image("emerald_tiles_bridge"));
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int col = x * WorldDefinition.COLUMNS / size;
                int row = y * WorldDefinition.ROWS / size;
                WorldDefinition.Terrain terrain = world.sampleTile(row, col);
                if (terrain == WorldDefinition.Terrain.BORDER) {
                    result.setRGB(x, y, 0xff142a20);
                    continue;
                }
                String material;
                switch (terrain) {
                    case NEGRO: case MEETING_DARK: material = "negro"; break;
                    case SOLIMOES: case MEETING_SILT: material = "solimoes"; break;
                    case BURNED: case DEAD_TREE: case ACID_SWAMP: material = "swamp"; break;
                    case PAVING: material = "paving"; break;
                    case GROUND: case PATH: material = "path"; break;
                    case BRIDGE: material = "bridge"; break;
                    default: material = "grass";
                }
                BufferedImage texture = images.get(material);
                int color = texture.getRGB(16 + (x * 7) % 32, 16 + (y * 11) % 32);
                if (terrain == WorldDefinition.Terrain.ACID_SWAMP && (x + y) % 5 == 0) {
                    color = 0xff8fd12a;
                }
                result.setRGB(x, y, color);
            }
        }
        Graphics2D g = result.createGraphics();
        double scale = size / (double) WorldDefinition.COLUMNS;
        BufferedImage forestSheet = image("emerald_scenery_trees");
        BufferedImage treeIcon = forestSheet.getSubimage(0, 0, 128, 160);
        for (int row = 0; row < WorldDefinition.ROWS / 4; row++) {
            for (int col = 0; col < WorldDefinition.COLUMNS / 4; col++) {
                WorldDefinition.Point anchor = world.treeAtCell(row, col);
                if (anchor == null) continue;
                int x = (int) (anchor.col * scale);
                int y = (int) (anchor.row * scale);
                g.drawImage(treeIcon, x - 6, y - 10, 12, 14, null);
            }
        }
        g.setColor(new Color(0xd6af78));
        for (WorldDefinition.Route route : world.routes) {
            for (int i = 1; i < route.points.size(); i++) {
                WorldDefinition.Point a = route.points.get(i - 1);
                WorldDefinition.Point b = route.points.get(i);
                g.drawLine((int) (a.col * scale), (int) (a.row * scale),
                    (int) (b.col * scale), (int) (b.row * scale));
            }
        }
        BufferedImage castanheira = image("emerald_castanheira");
        int cw = 54, ch = cw * castanheira.getHeight() / castanheira.getWidth();
        g.drawImage(castanheira, (int) (96 * scale) - cw / 2, (int) (92 * scale) - ch + 6, cw, ch, null);

        String[] icons = {
            "emerald_struct_maloca",
            "emerald_struct_temple",
            "emerald_struct_burned_tree",
            "emerald_struct_theater"
        };
        for (int i = 1; i < world.landmarks.size(); i++) {
            WorldDefinition.Site site = world.landmarks.get(i);
            BufferedImage icon = image(icons[i - 1]);
            int w = 44, h = w * icon.getHeight() / icon.getWidth();
            int x = (int) (site.center.col * scale);
            int y = (int) (site.center.row * scale);
            g.drawImage(icon, x - w / 2, y - h + 6, w, h, null);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        for (WorldDefinition.Site site : world.landmarks) {
            int x = (int) (site.center.col * scale);
            int y = (int) (site.center.row * scale) + 16;
            int width = g.getFontMetrics().stringWidth(site.displayName);
            g.setColor(new Color(0xd9142a20, true));
            g.fillRect(x - width / 2 - 4, y - 12, width + 8, 18);
            g.setColor(new Color(0xffede6c3, true));
            g.drawString(site.displayName, x - width / 2, y + 1);
        }
        for (WorldDefinition.Site arena : world.arenas) {
            int x = (int) (arena.center.col * scale);
            int y = (int) (arena.center.row * scale);
            g.setColor(new Color(0xffe1c265, true));
            g.drawOval(x - 10, y - 10, 20, 20);
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
