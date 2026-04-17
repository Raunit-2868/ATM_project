#!/bin/bash

set -euo pipefail

# Resolve the script directory to support spaces and arbitrary mount points.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
SQLITE_JAR="/Users/raunitkumarsharaff/.m2/repository/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
SLF4J_JAR="/Users/raunitkumarsharaff/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
BIN_DIR="$PROJECT_ROOT/target/classes"
SRC_DIR="$PROJECT_ROOT/src/main/java"
RES_DIR="$PROJECT_ROOT/src/main/resources"
SOURCES_FILE="$PROJECT_ROOT/sources.txt"

echo "=== SecureATM Build & Run System ==="

# 1. Clean and Create build directory
echo "Cleaning old build..."
rm -rf "$BIN_DIR"
mkdir -p "$BIN_DIR"

# 2. Find all source files
echo "Detecting source files..."
# Wrap each path in double quotes for javac @sources.txt
find "$SRC_DIR" -name "*.java" | sed 's/^/"/;s/$/"/' > "$PROJECT_ROOT/sources.txt"

# 3. Compiling
echo "Compiling project..."
javac -d "$BIN_DIR" -cp "$SQLITE_JAR:$SLF4J_JAR" @"$PROJECT_ROOT/sources.txt"

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    rm "$PROJECT_ROOT/sources.txt"
    exit 1
fi

# 4. Copy resources (Localization files)
echo "Copying resources..."
if [ -d "$RES_DIR" ]; then
    cp -r "$RES_DIR/"* "$BIN_DIR/"
fi

# 5. Launching
echo "Launching application..."
java -cp "$BIN_DIR:$SQLITE_JAR:$SLF4J_JAR" com.atm.Main &

# Cleanup
rm "$PROJECT_ROOT/sources.txt"
echo "Application started in background."
