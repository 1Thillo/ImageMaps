package net.craftcitizen.imagemaps;

import net.craftcitizen.imagemaps.util.MessageLevel;
import net.craftcitizen.imagemaps.util.MessageUtil;
import net.craftcitizen.imagemaps.util.Tuple;
import net.craftcitizen.imagemaps.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.MapInitializeEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

// TODO permissions per image or folder
// TODO per-user maps
public class ImageMaps extends JavaPlugin implements Listener {
    private static final String MAPS_YML = "maps.yml";
    private static final String CONFIG_VERSION_KEY = "storageVersion";
    private static final int CONFIG_VERSION = 1;
    private static final long AUTOSAVE_PERIOD = 18000L; // 15 minutes

    public static final int MAP_WIDTH = 128;
    public static final int MAP_HEIGHT = 128;
    static final String IMAGES_DIR = "images";

    /**
     * The known image maps, both directions. Placing the same tile of the same image twice reuses the existing map,
     * looking a map up by its id is needed to attach a renderer once the server loads it.
     */
    private final Map<ImageMap, Integer> maps = new HashMap<>();
    private final Map<Integer, ImageMap> mapsById = new HashMap<>();

    /**
     * Resolution and modification stamp per image. A few dozen bytes per file, as opposed to the fully decoded
     * images previous versions kept here.
     */
    private final ConcurrentMap<String, ImageInfo> imageInfo = new ConcurrentHashMap<>();

    /** Players that ran /imagemap place and have not clicked a block yet. */
    private final Map<UUID, PlacementData> pendingPlacements = new HashMap<>();

    private MapTileCache tileCache;
    private NamespacedKey rotatableKey;

    static {
        ConfigurationSerialization.registerClass(ImageMap.class);
    }

    @Override
    public void onEnable() {
        if (!getImagesDir().exists() && !getImagesDir().mkdirs())
            getLogger().warning("Failed to create the images directory!");

        // Must happen on the main thread before any conversion runs, see ImageTiles#warmColorCache.
        ImageTiles.warmColorCache();

        tileCache = new MapTileCache(this);
        rotatableKey = new NamespacedKey(this, "rotatable");

        ImageMapCommandHandler commandHandler = new ImageMapCommandHandler(this);
        getCommand("imagemap").setExecutor(commandHandler);
        getCommand("imagemap").setTabCompleter(commandHandler);
        getServer().getPluginManager().registerEvents(this, this);

        loadMaps();

        // Maps normally get their renderer from MapInitializeEvent, which fires when the server loads them. When
        // the plugin is enabled while players are already online we may have missed that event, so catch up once.
        if (!getServer().getOnlinePlayers().isEmpty())
            attachLoadedMaps();

        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> saveMaps(), AUTOSAVE_PERIOD,
                                                              AUTOSAVE_PERIOD);
    }

    @Override
    public void onDisable() {
        saveMaps();
    }

    MapTileCache getTileCache() {
        return tileCache;
    }

    File getImagesDir() {
        return new File(getDataFolder(), IMAGES_DIR);
    }

    /**
     * Stops a right click from rotating a map that is part of an image.
     * <p>
     * Rotating one tile of a multi map image tears a hole into it and there is no way to put it back other than
     * placing the image again. Vanilla lets anyone do this with an empty hand.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFrameRotate(PlayerItemFrameChangeEvent event) {
        if (event.getAction() != PlayerItemFrameChangeEvent.ItemFrameChangeAction.ROTATE
            || getImageMap(event.getItemFrame()) == null || isRotatable(event.getItemFrame()))
            return;

        event.setCancelled(true);

        // The client predicts the rotation and is sent no correction when the interaction is refused, so the
        // image looks torn until the frame happens to be sent again. Re-setting the item marks the frame's data
        // dirty, which resyncs it to everyone watching.
        ItemFrame frame = event.getItemFrame();
        getServer().getGlobalRegionScheduler().run(this, task -> frame.setItem(frame.getItem(), false));
    }

    /**
     * Whether this frame may be rotated by players.
     * <p>
     * Rotating one tile of an image tears a hole into it, so image frames are protected by default. The flag lives
     * on the frame itself, which means it persists with the entity and needs no bookkeeping of its own.
     */
    boolean isRotatable(ItemFrame frame) {
        Byte value = frame.getPersistentDataContainer().get(rotatableKey, PersistentDataType.BYTE);

        return value != null && value != 0;
    }

    void setRotatable(ItemFrame frame, boolean rotatable) {
        if (rotatable)
            frame.getPersistentDataContainer().set(rotatableKey, PersistentDataType.BYTE, (byte) 1);
        else
            frame.getPersistentDataContainer().remove(rotatableKey);
    }

    /**
     * The image map shown by an item frame, or null if the frame does not hold one of ours.
     */
    ImageMap getImageMap(ItemFrame frame) {
        ItemStack item = frame.getItem();

        if (item.getType() != Material.FILLED_MAP || !(item.getItemMeta() instanceof MapMeta meta))
            return null;

        MapView view = meta.getMapView();

        return view == null ? null : mapsById.get(view.getId());
    }

    /**
     * Every item frame belonging to the same image as the given one, or just that frame when it does not hold an
     * image map. The search is bounded by how large the image is, so it never looks further than it has to.
     */
    Collection<ItemFrame> getImageFrames(ItemFrame origin) {
        ImageMap definition = getImageMap(origin);

        if (definition == null)
            return List.of(origin);

        ImageInfo info = getImageInfo(definition.getFilename());
        int reach = 2;

        if (info != null) {
            Tuple<Integer, Integer> size = ImageTiles.getTileCount(info.width(), info.height(),
                                                                   definition.getScale());
            reach += Math.max(size.getKey(), size.getValue());
        }

        List<ItemFrame> frames = new ArrayList<>();

        for (Entity entity : origin.getWorld().getNearbyEntities(origin.getLocation(), reach, reach, reach,
                                                                 ItemFrame.class::isInstance)) {
            ItemFrame frame = (ItemFrame) entity;
            ImageMap other = getImageMap(frame);

            if (other != null && other.getFilename().equalsIgnoreCase(definition.getFilename())
                && other.getScale() == definition.getScale())
                frames.add(frame);
        }

        return frames.isEmpty() ? List.of(origin) : frames;
    }

    /**
     * Remembers that a player wants to place an image with their next right click.
     */
    void startPlacement(Player player, PlacementData data) {
        pendingPlacements.put(player.getUniqueId(), data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingPlacements.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Attaches a renderer the moment the server loads one of our maps, which happens when a player first comes
     * close enough to see it. Previous versions instead loaded every single map during startup, which forced the
     * server to read all of their data files up front.
     */
    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        ImageMap definition = mapsById.get(event.getMap().getId());

        if (definition != null)
            attachRenderer(event.getMap(), definition);
    }

    private void attachLoadedMaps() {
        for (Entry<Integer, ImageMap> entry : new ArrayList<>(mapsById.entrySet())) {
            MapView map = getMap(entry.getKey());

            if (map != null)
                attachRenderer(map, entry.getValue());
        }
    }

    private void attachRenderer(MapView map, ImageMap definition) {
        for (MapRenderer renderer : map.getRenderers()) {
            if (renderer instanceof ImageMapRenderer)
                return;

            map.removeRenderer(renderer);
        }

        map.setTrackingPosition(false);
        map.addRenderer(new ImageMapRenderer(this, definition.getFilename(), definition.getX(), definition.getY(),
                                             definition.getScale()));
    }

    @SuppressWarnings("deprecation") // Bukkit#getMap takes the map's item id, there is no other way to address it
    private MapView getMap(int id) {
        return Bukkit.getMap(id);
    }

    private void saveMaps() {
        FileConfiguration config = new YamlConfiguration();
        config.set(CONFIG_VERSION_KEY, CONFIG_VERSION);
        config.set("maps", new HashMap<>(mapsById));

        Runnable save = () -> {
            try {
                config.save(new File(getDataFolder(), MAPS_YML));
            }
            catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to save " + MAPS_YML, e);
            }
        };

        if (isEnabled())
            getServer().getAsyncScheduler().runNow(this, task -> save.run());
        else
            save.run();
    }

    private void loadMaps() {
        File configFile = new File(getDataFolder(), MAPS_YML);

        if (!configFile.exists())
            return;

        Configuration config = YamlConfiguration.loadConfiguration(configFile);
        int version = config.getInt(CONFIG_VERSION_KEY, -1);

        if (version == -1)
            config = convertLegacyMaps(config);

        ConfigurationSection section = config.getConfigurationSection("maps");

        if (section == null)
            return;

        section.getValues(false).forEach((key, value) -> {
            if (!(value instanceof ImageMap imageMap))
                return;

            int id = Utils.parseIntegerOrDefault(key, -1);

            if (id == -1) {
                getLogger().warning(() -> "Skipping map entry with invalid id " + key + ".");
                return;
            }

            maps.put(imageMap, id);
            mapsById.put(id, imageMap);
        });

        getLogger().info(() -> "Loaded " + mapsById.size() + " image maps.");
    }

    /**
     * Removes maps whose image file or map data is gone.
     * <p>
     * This has to ask the server for every single map, which loads all of them. It is an explicit admin action, not
     * something that happens on its own.
     */
    public int cleanupMaps() {
        int start = maps.size();

        maps.entrySet().removeIf(entry -> {
            boolean invalid = getMap(entry.getValue()) == null || getImageFile(entry.getKey().getFilename()) == null;

            if (invalid)
                mapsById.remove(entry.getValue());

            return invalid;
        });

        saveMaps();
        return start - maps.size();
    }

    private Configuration convertLegacyMaps(Configuration config) {
        getLogger().info("Converting maps from Version <1.0");

        try {
            Files.copy(new File(getDataFolder(), MAPS_YML).toPath(),
                       new File(getDataFolder(), MAPS_YML + ".backup").toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to backup " + MAPS_YML + "!", e);
        }

        Map<Integer, ImageMap> map = new HashMap<>();

        for (String key : config.getKeys(false)) {
            int id = Integer.parseInt(key);
            String image = config.getString(key + ".image");
            int x = config.getInt(key + ".x") / MAP_WIDTH;
            int y = config.getInt(key + ".y") / MAP_HEIGHT;
            double scale = config.getDouble(key + ".scale", 1.0);
            map.put(id, new ImageMap(image, x, y, scale));
        }

        config = new YamlConfiguration();
        config.set(CONFIG_VERSION_KEY, CONFIG_VERSION);
        config.createSection("maps", map);
        return config;
    }

    public boolean hasImage(String filename) {
        return getImageInfo(filename) != null;
    }

    /**
     * The names of every file in the images folder.
     */
    public String[] listImages() {
        String[] files = getImagesDir().list();

        return files == null ? new String[0] : files;
    }

    /**
     * Resolves an image name to a file inside the images folder, ignoring case.
     *
     * @return the file, or null if the name is not valid or no such file exists
     */
    File getImageFile(String filename) {
        if (filename == null || filename.isEmpty() || filename.contains("/") || filename.contains("\\")
            || filename.contains(":")) {
            getLogger().warning("Someone tried to get an image with illegal characters in its file name.");
            return null;
        }

        File exact = new File(getImagesDir(), filename);

        if (exact.isFile())
            return exact;

        File[] matches = getImagesDir().listFiles((dir, name) -> name.equalsIgnoreCase(filename));

        return matches != null && matches.length > 0 ? matches[0] : null;
    }

    /**
     * The resolution of an image, read from the file header without decoding the pixel data.
     *
     * @return the image's info, or null if it does not exist or is not a readable image
     */
    ImageInfo getImageInfo(String filename) {
        File file = getImageFile(filename);

        if (file == null)
            return null;

        String key = filename.toLowerCase(Locale.ROOT);
        ImageInfo cached = imageInfo.get(key);

        if (cached != null && cached.matches(file))
            return cached;

        ImageInfo read = readImageInfo(file);

        if (read == null) {
            imageInfo.remove(key);
            getLogger().log(Level.WARNING, () -> String.format("Failed to read file as image %s.", file.getName()));
            return null;
        }

        imageInfo.put(key, read);
        return read;
    }

    private ImageInfo readImageInfo(File file) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            if (stream == null)
                return null;

            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (!readers.hasNext())
                return null;

            ImageReader reader = readers.next();

            try {
                reader.setInput(stream);
                return new ImageInfo(reader.getWidth(0), reader.getHeight(0), file.lastModified(), file.length());
            }
            finally {
                reader.dispose();
            }
        }
        catch (IOException e) {
            getLogger().log(Level.SEVERE, String.format("Error while trying to read image %s.", file.getName()), e);
            return null;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!pendingPlacements.containsKey(player.getUniqueId()))
            return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            pendingPlacements.remove(player.getUniqueId());
            MessageUtil.sendMessage(player, MessageLevel.NORMAL, "Image placement cancelled.");
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        PlacementData data = pendingPlacements.get(player.getUniqueId());
        PlacementResult result = placeImage(player, event.getClickedBlock(), event.getBlockFace(), data);

        switch (result) {
            case INVALID_FACING:
                MessageUtil.sendMessage(player, MessageLevel.WARNING,
                                        "You can't place an image on this block face.");
                break;
            case INVALID_DIRECTION:
                MessageUtil.sendMessage(player, MessageLevel.WARNING, "Couldn't calculate how to place the map.");
                break;
            case EVENT_CANCELLED:
                MessageUtil.sendMessage(player, MessageLevel.NORMAL,
                                        "Image placement cancelled by another plugin.");
                break;
            case INSUFFICIENT_SPACE:
                MessageUtil.sendMessage(player, MessageLevel.NORMAL,
                                        "Map couldn't be placed, the space is blocked.");
                break;
            case INSUFFICIENT_WALL:
                MessageUtil.sendMessage(player, MessageLevel.NORMAL,
                                        "Map couldn't be placed, the supporting wall is too small.");
                break;
            case OVERLAPPING_ENTITY:
                MessageUtil.sendMessage(player, MessageLevel.NORMAL,
                                        "Map couldn't be placed, there is another entity in the way.");
                break;
            case MISSING_IMAGE:
                MessageUtil.sendMessage(player, MessageLevel.WARNING, "The image could no longer be read.");
                break;
            case SUCCESS:
                break;
        }

        pendingPlacements.remove(player.getUniqueId());
        event.setCancelled(true);
    }

    private PlacementResult placeImage(Player player, Block block, BlockFace face, PlacementData data) {
        if (!isAxisAligned(face)) {
            getLogger().severe("Someone tried to create an image with an invalid block facing");
            return PlacementResult.INVALID_FACING;
        }

        ImageInfo info = getImageInfo(data.getFilename());

        if (info == null)
            return PlacementResult.MISSING_IMAGE;

        Block b = block.getRelative(face);
        double scale = info.getScale(data.getSize());
        Tuple<Integer, Integer> size = ImageTiles.getTileCount(info.width(), info.height(), scale);
        BlockFace widthDirection = calculateWidthDirection(player, face);
        BlockFace heightDirection = calculateHeightDirection(player, face);

        if (widthDirection == null || heightDirection == null)
            return PlacementResult.INVALID_DIRECTION;

        // check for space
        for (int x = 0; x < size.getKey(); x++)
            for (int y = 0; y < size.getValue(); y++) {
                Block frameBlock = b.getRelative(widthDirection, x).getRelative(heightDirection, y);

                if (!block.getRelative(widthDirection, x).getRelative(heightDirection, y).getType().isSolid())
                    return PlacementResult.INSUFFICIENT_WALL;
                if (frameBlock.getType().isSolid())
                    return PlacementResult.INSUFFICIENT_SPACE;
                if (!b.getWorld().getNearbyEntities(frameBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5,
                                                    Hanging.class::isInstance)
                      .isEmpty())
                    return PlacementResult.OVERLAPPING_ENTITY;
            }

        ImagePlaceEvent event = new ImagePlaceEvent(player, block, widthDirection, heightDirection, size.getKey(),
                                                    size.getValue(), data);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return PlacementResult.EVENT_CANCELLED;

        // spawn item frame
        for (int x = 0; x < size.getKey(); x++)
            for (int y = 0; y < size.getValue(); y++) {
                Class<? extends ItemFrame> itemFrameClass = data.isGlowing() ? GlowItemFrame.class : ItemFrame.class;
                ItemFrame frame = block.getWorld().spawn(b.getRelative(widthDirection, x)
                                                          .getRelative(heightDirection, y).getLocation(),
                                                         itemFrameClass);
                frame.setFacingDirection(face);
                frame.setItem(getMapItem(x, y, data, scale));
                frame.setRotation(facingToRotation(heightDirection, widthDirection));
                frame.setFixed(data.isFixed());
                frame.setVisible(!data.isInvisible());
            }

        return PlacementResult.SUCCESS;
    }

    public boolean deleteImage(String filename) {
        File file = getImageFile(filename);
        boolean fileDeleted = file != null && file.delete();

        imageInfo.remove(filename.toLowerCase(Locale.ROOT));
        tileCache.invalidate(filename);

        Iterator<Entry<ImageMap, Integer>> it = maps.entrySet().iterator();

        while (it.hasNext()) {
            Entry<ImageMap, Integer> entry = it.next();

            if (!entry.getKey().getFilename().equalsIgnoreCase(filename))
                continue;

            MapView map = getMap(entry.getValue());

            if (map != null)
                map.getRenderers().forEach(map::removeRenderer);

            mapsById.remove(entry.getValue());
            it.remove();
        }

        saveMaps();
        return fileDeleted;
    }

    /**
     * Re-reads an image from disk and redraws every map showing it.
     */
    public boolean reloadImage(String filename) {
        imageInfo.remove(filename.toLowerCase(Locale.ROOT));
        tileCache.invalidate(filename);

        if (getImageInfo(filename) == null) {
            getLogger().warning(() -> "Failed to reload image: " + filename);
            return false;
        }

        // Only touches the maps of this one image, the rest is left untouched and unloaded.
        maps.entrySet().stream().filter(a -> a.getKey().getFilename().equalsIgnoreCase(filename))
            .map(a -> getMap(a.getValue())).filter(Objects::nonNull)
            .flatMap(a -> a.getRenderers().stream()).filter(ImageMapRenderer.class::isInstance)
            .forEach(a -> ((ImageMapRenderer) a).invalidate());

        return true;
    }

    /**
     * Every scale an image has actually been placed with, plus the unscaled default.
     */
    Set<Double> getUsedScales(String filename) {
        Set<Double> scales = new HashSet<>();
        scales.add(1.0D);

        for (ImageMap map : maps.keySet())
            if (map.getFilename().equalsIgnoreCase(filename))
                scales.add(map.getScale());

        return scales;
    }

    Set<String> getPlacedImages() {
        Set<String> images = new HashSet<>();

        for (ImageMap map : maps.keySet())
            images.add(map.getFilename());

        return images;
    }

    private ItemStack getMapItem(int x, int y, PlacementData data, double scale) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ImageMap imageMap = new ImageMap(data.getFilename(), x, y, scale);
        MapMeta meta = (MapMeta) item.getItemMeta();

        Integer existing = maps.get(imageMap);

        if (existing != null) {
            MapView map = getMap(existing);

            if (map != null) {
                meta.setMapView(map);
                item.setItemMeta(meta);
                return item;
            }

            // The map data is gone, drop the stale entry and create a replacement below.
            maps.remove(imageMap);
            mapsById.remove(existing);
        }

        MapView map = getServer().createMap(getServer().getWorlds().get(0));
        attachRenderer(map, imageMap);

        meta.setMapView(map);
        item.setItemMeta(meta);

        maps.put(imageMap, map.getId());
        mapsById.put(map.getId(), imageMap);

        return item;
    }

    /**
     * The size of an image in maps, for a requested target size.
     */
    public Tuple<Integer, Integer> getImageSize(String filename, Tuple<Integer, Integer> size) {
        ImageInfo info = getImageInfo(filename);

        if (info == null)
            return new Tuple<>(0, 0);

        return ImageTiles.getTileCount(info.width(), info.height(), info.getScale(size));
    }

    public double getScale(String filename, Tuple<Integer, Integer> size) {
        ImageInfo info = getImageInfo(filename);

        return info == null ? 1.0D : info.getScale(size);
    }

    private static Rotation facingToRotation(BlockFace heightDirection, BlockFace widthDirection) {
        switch (heightDirection) {
            case WEST:
                return Rotation.CLOCKWISE_45;
            case NORTH:
                return widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
            case EAST:
                return Rotation.CLOCKWISE_135;
            case SOUTH:
                return widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
            default:
                return Rotation.NONE;
        }
    }

    private static BlockFace calculateWidthDirection(Player player, BlockFace face) {
        float yaw = (360.0f + player.getLocation().getYaw()) % 360.0f;
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            case WEST:
                return BlockFace.SOUTH;
            case UP:
            case DOWN:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.NORTH;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.EAST;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.SOUTH;
                else
                    return BlockFace.WEST;
            default:
                return null;
        }
    }

    private static BlockFace calculateHeightDirection(Player player, BlockFace face) {
        float yaw = (360.0f + player.getLocation().getYaw()) % 360.0f;
        switch (face) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return BlockFace.DOWN;
            case UP:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.EAST;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.SOUTH;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.WEST;
                else
                    return BlockFace.NORTH;
            case DOWN:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.WEST;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.NORTH;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.EAST;
                else
                    return BlockFace.SOUTH;
            default:
                return null;
        }
    }

    private static boolean isAxisAligned(BlockFace face) {
        switch (face) {
            case DOWN:
            case UP:
            case WEST:
            case EAST:
            case SOUTH:
            case NORTH:
                return true;
            default:
                return false;
        }
    }
}
