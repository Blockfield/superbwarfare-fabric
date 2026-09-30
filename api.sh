#!/usr/bin/env bash
# Проверка существования API без запуска gradle: javap по classpath уже скачанных зависимостей.
#   ./api.sh net.minecraft.world.item.ItemStack          -- все сигнатуры класса
#   ./api.sh net.minecraft.world.item.ItemStack burn     -- только строки с 'burn'
#   ./api.sh -f RecipeRemainder                          -- найти класс(ы), где встречается имя
#
# Классы NeoForge из classpath выброшены намеренно: они лежат в кэше от апстрим-сборки,
# и без этого net.neoforged.* «существует» при проверке, хотя в сборке его нет.
set -euo pipefail
cd "$(dirname "$0")"
JAVAP=${JAVAP:-${JAVA_HOME:+$JAVA_HOME/bin/javap}}
JAVAP=${JAVAP:-$(command -v javap)}
CACHE=.api-classpath
if [ ! -s "$CACHE" ]; then
    {
        # Сначала jar проекта: в нём есть инъекции интерфейсов (Accessories вкручивает
        # AbstractButtonExtension прямо в AbstractButton). В общем кэше loom их нет.
        ls .gradle/loom-cache/minecraftMaven/net/minecraft/*/*/*.jar 2>/dev/null | grep -v sources
        ls "$HOME"/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/*/*.jar | grep -v sources
        find "$HOME/.gradle/caches/modules-2" -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar'
        find "$HOME/.gradle/caches/fabric-loom" -name '*.jar' ! -name '*-sources.jar' -path '*remapped*'
        ls .gradle/pinned-libs/*.jar 2>/dev/null || true
    } | sort -u | grep -v -e neoforge -e fancymodloader -e '/fmlloader/' >"$CACHE"
fi
CP=$(tr '\n' ':' <"$CACHE")
if [ "${1:-}" = "-f" ]; then
    grep -rl "$2" /dev/null 2>/dev/null || true
    while read -r j; do
        unzip -l "$j" 2>/dev/null | grep -i "$2" | sed "s|^|$(basename "$j"): |"
    done <"$CACHE"
    exit 0
fi
CLASS=$1
shift
if [ $# -gt 0 ]; then
    "$JAVAP" -cp "$CP" "$CLASS" | grep -i "$1"
else
    "$JAVAP" -cp "$CP" "$CLASS"
fi
