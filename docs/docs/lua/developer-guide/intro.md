Lua was chosen because it is fast, relatively expressive, and it has a very low embedding cost (it doesn't add much to the apps runtime requirements or download size).






You may notice that only the configuration parameters of the script are immediately visible.

TODO insert image

This is because by convention the only part of the script that is shown is the part between the following two lines:

```lua
--- PREVIEW_START
```

and 

```lua
--- PREVIEW_END
```
