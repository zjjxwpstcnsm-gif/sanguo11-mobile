#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v dotnet >/dev/null || { echo 'BLOCKED: .NET SDK unavailable; this is not a Unity Player test'; exit 2; }
dotnet run --project tools/architecture-client/ArchitectureClient.csproj --configuration Release -- "$PWD"
