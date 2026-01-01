#!/bin/bash

# Validate remote configuration with Python virtual environment setup

set -e

echo "==> Setting up Python environment for configuration validation"

cd ./configuration

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    python3 -m venv venv
    echo "Installing dependencies..."
    venv/bin/pip install --upgrade pip
    venv/bin/pip install -r requirements.txt
fi

echo "==> Running configuration validation"
. venv/bin/activate && python3 ./validate-config.py

echo "==> Remote configuration validation completed successfully"
