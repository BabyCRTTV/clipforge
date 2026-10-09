#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
if [ ! -x .venv/bin/python ]; then
  printf 'Run bash setup.sh first.\n'
  exit 1
fi
exec .venv/bin/python engine/server.py
