#!/usr/bin/env python3

import json
import sys
from pathlib import Path
import requests
from urllib.parse import urlparse

try:
    import jsonschema
except ImportError:
    print("Error: jsonschema library not found. Please install with pip:")
    print("pip install jsonschema")
    sys.exit(1)

try:
    import requests
except ImportError:
    print("Error: requests library not found. Please install with pip:")
    print("pip install requests")
    sys.exit(1)


def check_endpoint(url, timeout=10):
    """Check if an endpoint is reachable and returns 200 status."""
    try:
        response = requests.head(url, timeout=timeout, allow_redirects=True)
        if response.status_code == 200:
            return True, f"✅ {url} - OK (200)"
        else:
            return False, f"❌ {url} - HTTP {response.status_code}"
    except requests.exceptions.RequestException as e:
        return False, f"❌ {url} - Connection error: {str(e)}"


def validate_endpoints(config):
    """Validate that all endpoints in the configuration are reachable."""
    print("\nValidating endpoints...")
    
    if "endpoints" not in config:
        print("No endpoints found in configuration")
        return True
    
    endpoints = config["endpoints"]
    all_valid = True
    
    for name, url in endpoints.items():
        # Parse URL to validate format
        try:
            parsed = urlparse(url)
            if not all([parsed.scheme, parsed.netloc]):
                print(f"❌ {name}: Invalid URL format - {url}")
                all_valid = False
                continue
        except Exception as e:
            print(f"❌ {name}: URL parsing error - {url} ({e})")
            all_valid = False
            continue
        
        # Check endpoint
        is_valid, message = check_endpoint(url)
        print(f"   {message}")
        
        if not is_valid:
            all_valid = False
    
    return all_valid


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

        # Validate against schema
        jsonschema.validate(config, schema)
        print("✅ Configuration is valid against schema!")

        # Validate endpoints
        endpoints_valid = validate_endpoints(config)
        
        if endpoints_valid:
            print("✅ All endpoints are reachable!")
            return True
        else:
            print("❌ Some endpoints are not reachable!")
            return False

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
