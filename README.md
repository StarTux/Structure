# Structure

World Structure Cache.  This plugin will look for the structures.txt
file in each world folder.  This file must be generated via NBTDump,
for example:

- `java -jar NBTDump.jar -s -g structures.starts region/r.*.mca > structures.txt`

Thus, the file will contain one line per chunk.  Each line is a JSON
object, mapping structure namespaced keys to structure objects.  This
plugin will figure out the types and bounding boxes and provide them
via its Core API.

- See `com.cavetale.core.structure.Structures`