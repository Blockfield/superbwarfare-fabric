#!/usr/bin/env bash
# Ищет в собранном jar сигнатуры с ванильными именами, которые ремаппер не тронул.
#
# Ссылка на свойство в Kotlin (`Data::field`, `by DELEGATE`) хранит JVM-сигнатуру геттера
# обычной строкой в constant pool. tiny-remapper переписывает дескрипторы, но не строки,
# поэтому в собранном jar сигнатура остаётся named, а метод уже intermediary -- и
# kotlin-reflect падает с "Property 'x' not resolved". В dev-запуске этого не видно:
# там имена и так named. Ловится только на production-jar, то есть только здесь.
#
# Лечение: убрать ванильный тип из свойства, которое читается рефлексией
# (см. DefaultGunData.shootShake и .icon), а конверсию делать в complexProp.
set -euo pipefail
cd "$(dirname "$0")"
JAR=${1:-$(ls build/libs/*.jar | grep -v sources | head -1)}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
unzip -q -o "$JAR" 'com/*' -d "$WORK"

# Делегированные свойства (`var x by ACCESSOR`) тоже создают ссылку, но делегат в
# EntityUtil.kt игнорирует KProperty, поэтому ссылка никогда не разрешается. Такие -- мимо.
BENIGN='ArtilleryEntity.class  getOriginPos|ArtilleryEntity.class  getTargetPos'

hits=$(cd "$WORK" && for f in $(grep -ral "net/minecraft/" . 2>/dev/null | grep -v '/mixins/'); do
    strings "$f" | grep -oE '(get|set|is)[A-Za-z0-9_]+\([^)]*\)\[*L?net/minecraft/[a-zA-Z0-9/_$]+;' |
        sed "s|^|$(basename "$f")  |"
done 2>/dev/null | sort -u | grep -vE "$BENIGN" || true)

if [ -n "$hits" ]; then
    echo "$hits"
    echo
    echo "^ ссылки на свойства с ванильным типом: в собранном jar они не разрешатся."
    exit 1
fi
echo "ремап чистый: ссылок с ванильными типами нет"
