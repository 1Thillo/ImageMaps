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

### Item frames
The item frames of a placed image are changed with one command, which always applies to every frame of the image
you are looking at rather than the single one you aim at:

```
/imagemap frame invisible true     hides the frames, leaving only the image
/imagemap frame fixed true         locks the frames so they cannot be broken or modified
/imagemap frame rotatable true     allows players to rotate the individual maps again
```

Each takes `true` or `false`.

Rotating a single tile of a multi map image tears a hole into it that can only be repaired by placing the image
again, so image frames refuse rotation by default. `rotatable true` gives it back for that image. A frame that is
`fixed` cannot be rotated either way, because Minecraft refuses every interaction with a fixed frame.

The `rotatable` flag is stored on the frame itself and survives restarts.

Previous versions changed the invisible and fixed properties by right clicking a frame with a configurable item,
a wooden hoe by default. That never worked reliably: a plain right click on a frame is answered by the client
with an interaction on the block behind it, so the server never learns about it, and a frame that had been set to
fixed refused every further interaction, which made the change impossible to undo. The item and the `toggleItem`
config option are gone, the command replaces them.

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
* /imagemap frame &lt;invisible|fixed|rotatable> &lt;true|false>
  * changes the item frames of the image you are looking at
* /imagemap bake [filename]
  * converts images to map colours ahead of time, so nothing has to be converted while players are looking
* /imagemap list [page]
  * lists all images in the `images` folder
* /imagemap version
  * shows which version of the plugin is running
* /imagemap help [command]
  * prints help for commands

File names containing spaces have to be quoted, for example `/imagemap info "my image.png"`.

### Permissions
Every sub command has its own permission, so they can be handed out individually.

Available to everyone:

`imagemaps.version` - /imagemap version
`imagemaps.help` - /imagemap help
`imagemaps.frame` - /imagemap frame, with one child per property:
* `imagemaps.frame.invisible` - showing and hiding frames
* `imagemaps.frame.fixed` - locking and unlocking frames
* `imagemaps.frame.rotatable` - releasing frames for rotation

Operators only:

`imagemaps.place` - /imagemap place
`imagemaps.download` - /imagemap download
`imagemaps.delete` - /imagemap delete
`imagemaps.info` - /imagemap info
`imagemaps.list` - /imagemap list
`imagemaps.reload` - /imagemap reload
`imagemaps.bake` - /imagemap bake
`imagemaps.cleanup` - /imagemap cleanup
`imagemaps.debuginfo` - /imagemap debuginfo
`imagemaps.*` - all of the above

On a public server consider revoking `imagemaps.frame.fixed` and `imagemaps.frame.rotatable`, or
`imagemaps.frame` as a whole: they let any player unlock a placed image and then break or rotate it.

### Configuration
The plugin has no configuration options. The `toggleItem` option of previous versions is gone together with
the item based frame toggle, an existing `config.yml` is ignored.

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
