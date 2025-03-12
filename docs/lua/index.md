
```bash
/ (project root)
├── lua/
│   ├── src/
│   │   └── tng/                          # Lua API code
│   └── community/                # Community Lua scripts
│       ├── bar-charts/                   # Category folder (e.g. bar charts)
│       │   ├── example-script/           # Each script in its own directory
│       │   │   ├── example-script.lua    # The Lua code
│       │   │   ├── example-script.md     # Documentation for the script (usage, details, images)
│       │   │   └── images/               # Images/assets used in the docs
│       │   │       └── screenshot.png
│       │   └── another-script/           # Additional scripts in this category
│       └── line-graphs/                  # Another category, etc.
│           └── sample-script/
│               ├── sample-script.lua
│               ├── sample-script.md
│               └── images/
├── docs/
│   ├── faq/                              # Pre-existing FAQ docs (cannot change this path)
│   └── lua/                              # General Lua documentation
│       ├── introduction.md               # What is Lua and how it integrates with Track & Graph
│       └── developer-guide.md            # Developer guide for Lua script integration
└── ...
```
