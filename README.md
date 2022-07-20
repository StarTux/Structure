# Structure

World Structure Cache.  This plugin will look for the structures.db
database file in each world folder.  This file is be generated via
NBTDump, for example:

- `java -jar NBTDump.jar --structures world`

Client plugins can add their own structures to the database.

```java
import static com.cavetale.structure.StructurePlugin.structureCache;

Structure structure = structureCache().at(block);
List<Structure> structures = structureCache().within(worldName, cuboid);
```