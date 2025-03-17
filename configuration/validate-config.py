#!/usr/bin/env python3

import json
import sys
from pathlib import Path
try:
    import jsonschema
except ImportError:
    print("Error: jsonschema library not found. Please install with pip:")
    print("pip install jsonschema")
    sys.exit(1)


def validate_config():
    print("Starting validation of remote configuration...")

    # Define paths to schema and config files
    schema_path = Path("./remote-configuration.schema.json")
    config_path = Path("./remote-configuration.json")

    # Check if files exist
    if not schema_path.exists():
        print(f"Error: Schema file not found at {schema_path}")
        return False

    if not config_path.exists():
        print(f"Error: Configuration file not found at {config_path}")
        return False

    try:
        # Load schema and config
        with open(schema_path, 'r') as schema_file:
            schema = json.load(schema_file)
            print(f"✅ Schema loaded successfully from {schema_path}")

        with open(config_path, 'r') as config_file:
            config = json.load(config_file)
            print(f"✅ Configuration loaded successfully from {config_path}")

        # Validate
        jsonschema.validate(config, schema)
        print("✅ Configuration is valid!")
        return True

    except json.JSONDecodeError as e:
        print(f"❌ JSON parse error: {e}")
        return False
    except jsonschema.exceptions.ValidationError as e:
        print(f"❌ Validation error: {e.message}")
        print(f"   at path: {' > '.join(str(path) for path in e.path)}")
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return False


if __name__ == "__main__":
    success = validate_config()
    sys.exit(0 if success else 1)
