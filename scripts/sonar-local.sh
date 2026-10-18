#!/usr/bin/env bash
# Análisis local de EcoAula en SonarCloud con cobertura (JaCoCo) y Quality Gate.
#
# Uso:
#   SONAR_TOKEN=<token> ./scripts/sonar-local.sh
#   ./scripts/sonar-local.sh            (pide el token sin mostrarlo)
#
# El token nunca se guarda en archivos del proyecto. Ver docs/CALIDAD_CI.md.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -z "${SONAR_TOKEN:-}" ]; then
  read -r -s -p "Token de SonarCloud: " SONAR_TOKEN
  echo
fi
if [ -z "${SONAR_TOKEN}" ]; then
  echo "No se recibió el token de SonarCloud." >&2
  exit 1
fi
export SONAR_TOKEN

./mvnw -B clean verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.branch.name="$(git branch --show-current)"

echo "Resultado: https://sonarcloud.io/dashboard?id=Software-III_ecoaula"
