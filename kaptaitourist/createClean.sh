#!/bin/bash

# ============================
# Dynamic Module Generator
# ============================

# --- INPUTS ---
NAME=$1
TARGET_DIR=$2

if [ -z "$NAME" ] || [ -z "$TARGET_DIR" ]; then
  echo "Usage: ./create-module.sh <ModuleName> <TargetFolderPath>"
  exit 1
fi

UPPER=$(echo "$NAME" | sed -E 's/(.)/\U\1/')
LOWER=$(echo "$NAME" | sed -E 's/(.)/\L\1/')

# Create base folder
mkdir -p "$TARGET_DIR"

echo "Creating module '$UPPER' at: $TARGET_DIR"
cd "$TARGET_DIR" || exit

# --- FOLDER STRUCTURE ---
mkdir -p adapter/in/web/dto
mkdir -p adapter/in/web/handler
mkdir -p adapter/in/web/router

mkdir -p adapter/out/persistence/entity
mkdir -p adapter/out/persistence/repository

mkdir -p application/port/in
mkdir -p application/port/out
mkdir -p application/service

mkdir -p domain

# --- FILE CREATION FUNCTIONS ---
create_file() {
  FILE_PATH=$1
  CLASS_NAME=$2

  cat > "$FILE_PATH" <<EOF
public class $CLASS_NAME {
}
EOF
}

create_interface() {
  FILE_PATH=$1
  CLASS_NAME=$2

  cat > "$FILE_PATH" <<EOF
public interface $CLASS_NAME {
}
EOF
}

# --- FILE CREATION ---

# DTO Files
create_file "adapter/in/web/dto/${UPPER}ListRequestDto.java" "${UPPER}ListRequestDto"
create_file "adapter/in/web/dto/${UPPER}ListResponseDto.java" "${UPPER}ListResponseDto"

# Handler
create_file "adapter/in/web/handler/${UPPER}Handler.java" "${UPPER}Handler"

# Router
create_file "adapter/in/web/router/${UPPER}RouterConfig.java" "${UPPER}RouterConfig"

# Outbound adapter
create_file "adapter/out/persistence/${UPPER}Adapter.java" "${UPPER}Adapter"

# Entity
create_file "adapter/out/persistence/entity/${UPPER}Entity.java" "${UPPER}Entity"

# Repository
create_interface "adapter/out/persistence/repository/${UPPER}Repository.java" "${UPPER}Repository"

# Ports
create_interface "application/port/in/${UPPER}UseCase.java" "${UPPER}UseCase"
create_interface "application/port/out/${UPPER}Port.java" "${UPPER}Port"

# Service
create_file "application/service/${UPPER}Service.java" "${UPPER}Service"

# Domain
create_file "domain/${UPPER}.java" "${UPPER}"

echo "✔ Module '$UPPER' created successfully in: $TARGET_DIR"

