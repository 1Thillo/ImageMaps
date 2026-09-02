# ImageMaps

ImageMaps is a plugin for Paper Minecraft servers that enables you to place custom images in your world
by rendering them onto maps and placing them in item frames.

This can be used to create info graphics, custom paintings and even the appearance of custom textures.

![Example Screenshot](./screenshot.png)

## Installation
You will need a server running Paper 26.2 or newer and Java 25.

Put the plugin into your servers `plugins` folder and start it up. A folder for the plugin will be created
automatically. You can place your images in the `images` folder inside it.

### Upgrading from 1.0.x
Existing images keep working, no migration steps are required. `maps.yml` is read as before and the item frames
already placed in your world stay exactly as they are.

The first time a player looks at an image after the upgrade, it is converted to map colours and the result is
stored in the plugin's `cache` folder. If you would rather get that out of the way up front, run
`/imagemap bake` once after starting the server. Deleting the `cache` folder is always safe, it is rebuilt from
the files in `images`.

Two things changed that you may notice:
* The plugin no longer loads every map during startup. Maps are prepared when the server first loads them,
  which is when a player comes close enough to see them.
* The `reload` command now checks the `imagemaps.reload` permission. Previous versions checked `imagemap.reload`,
  which did not match the permission the plugin actually registers.

### Upgrading from versions &lt; 1.0.0
The plugin should convert your maps.yml automatically. It will attempt to create a backup before doing so,
but it is recommended to do one manually as well.

## Usage
In order for you use place images they must be present in the `images` folder. You can either put them
there via FTP or just copying it, or by using the plugin's download command, allowing you to access
images from the Internet.

To place images just run the `/imagemap place` command and right click the block that should be the
upper left corner of the image (based on your perspective).

Images will by default split into 128x128 pixel sub-images to fit on a map. The image may be scaled via
command parameters, however **it is recommended to prepare correctly scaled images**.

The color palette of Minecraft maps is very limited, requiring the plugin to convert the input image.
This will result in your images looking different ingame than the source. The available color table can be
found here: https://minecraft.wiki/w/Map_item_format#Color_table

![Example of color conversion](./colorConversion.png)

The invisible and fixed (-> can't be modified) item frame properties can be changed for any item frame
by using a configurable (default a wooden hoe) item. Right click to toggle visibility and Shift+Right click to
toggle the fixed state.

### Memory use
The plugin keeps no image data in memory. Converting an image is expensive but its result never changes, so
each map's 16 KiB of colour indices is written to the `cache` folder once and read back on demand. A map that
nobody is looking at costs nothing beyond its entry in `maps.yml`.

If you have many large images, the disk cache grows by 16 KiB per map. An image covering a 5x3 block wall
therefore takes about 240 KiB of cache.

### Commands
* /imagemap place &lt;filename> [frameInvisible] [frameFixed] [frameGlowing] [size]
  * Starts the placement process
* /imagemap download &lt;filename> &lt;sourceURL>
  * downloads an image from the given URL into the `images` folder
* /imagemap delete &lt;filename>
  * deletes an image from the `images` folder
* /imagemap info &lt;filename>
  * prints info, such as resolution and expected size
* /imagemap reload &lt;filename>
  * reloads an image, to be used when the image file changed
* /imagemap bake [filename]
  * converts images to map colours ahead of time, so nothing has to be converted while players are looking
* /imagemap list [page]
  * lists all images in the `images` folder
* /imagemap help [command]
  * prints help for commands

File names containing spaces have to be quoted, for example `/imagemap info "my image.png"`.

### Permissions
`imagemaps.*` - grants access to all permissions
`imagemaps.place` - grants access to /imagemap place command
`imagemaps.download` - grants access to /imagemap download command
`imagemaps.delete` - grants access to /imagemap delete command
`imagemaps.info` - grants access to /imagemap info command
`imagemaps.list` - grants access to /imagemap list command
`imagemaps.reload` - grants access to /imagemap reload command
`imagemaps.help` - grants access to /imagemap help command
`imagemaps.admin` - grants access to /imagemap bake, cleanup and debuginfo
`imagemaps.toggleFixed` - allows to toggle the "fixed" state of frames with a wooden hoe
`imagemaps.toggleVisible` - allows to toggle the "visible" state of frames with a wooden hoe

All permissions are default true for OPs.

## Building
The plugin has no dependencies beyond the Paper API and produces a plain jar.

```
mvn clean package
```

You will need JDK 25. The result is `target/ImageMaps.jar`.

## Contact
* originally developed by SydMontague: https://github.com/SydMontague/ImageMaps
* directly on GitHub

This plugin has been developed for the [CraftCitizen](https://craftcitizen.net) survival Minecraft server.
