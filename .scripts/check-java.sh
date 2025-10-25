#!/bin/bash

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║         Проверка Java версии для проекта                ║${NC}"
echo -e "${YELLOW}╚══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Получить текущую версию Java
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)

echo -e "Текущая версия Java: ${YELLOW}$JAVA_VERSION${NC}"
echo ""

if [ "$JAVA_VERSION" == "17" ] || [ "$JAVA_VERSION" == "21" ]; then
    echo -e "${GREEN}✓ Java версия совместима с проектом!${NC}"
    echo -e "${GREEN}  Можно приступать к сборке: make install${NC}"
    exit 0
elif [ "$JAVA_VERSION" == "25" ]; then
    echo -e "${RED}✗ Java 25 несовместима с текущей версией Maven compiler plugin${NC}"
    echo ""
    echo -e "${YELLOW}Решение:${NC}"
    echo ""
    echo -e "${GREEN}1. Установить Java 21 (рекомендуется):${NC}"
    echo -e "   sudo apt update"
    echo -e "   sudo apt install openjdk-21-jdk"
    echo ""
    echo -e "${GREEN}2. Переключиться на Java 21:${NC}"
    echo -e "   sudo update-alternatives --config java"
    echo -e "   sudo update-alternatives --config javac"
    echo ""
    echo -e "${GREEN}3. Проверить версию:${NC}"
    echo -e "   java --version"
    echo ""
    echo -e "${YELLOW}После установки Java 21 запустите:${NC}"
    echo -e "   ./check-java.sh"
    echo -e "   make install"
    exit 1
else
    echo -e "${YELLOW}⚠ Неизвестная версия Java: $JAVA_VERSION${NC}"
    echo -e "${YELLOW}  Рекомендуется Java 17 или 21${NC}"
    exit 1
fi
