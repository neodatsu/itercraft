#!/bin/bash
# Script de développement local pour Itercraft
# Build et lance les containers essentiels : front, api, keycloak, bdd

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$SCRIPT_DIR/docker/docker-compose.dev.yml"
ENV_FILE="$SCRIPT_DIR/terraform/env.sh"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start       Build et démarre tous les containers (défaut)"
    echo "  stop        Arrête tous les containers"
    echo "  restart     Redémarre tous les containers"
    echo "  logs        Affiche les logs de tous les containers"
    echo "  logs-api    Affiche les logs de l'API uniquement"
    echo "  clean       Arrête et supprime les containers et volumes"
    echo "  status      Affiche le statut des containers"
    echo "  init-games  Initialise la ludothèque de Laurent (après première connexion)"
    echo ""
    echo "Variables d'environnement optionnelles:"
    echo "  METEOFRANCE_API_TOKEN  Token API Météo France"
    echo "  ANTHROPIC_API_KEY      Clé API Anthropic (Claude)"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Erreur: Docker n'est pas installé${NC}"
        exit 1
    fi
    if ! docker info &> /dev/null; then
        echo -e "${RED}Erreur: Le daemon Docker n'est pas démarré${NC}"
        exit 1
    fi
}

load_env() {
    if [[ -f "$ENV_FILE" ]]; then
        echo -e "${YELLOW}Chargement des variables d'environnement depuis terraform/env.sh...${NC}"
        # shellcheck source=/dev/null
        source "$ENV_FILE"
        # Map TF_VAR_ variables to docker-compose expected names
        export METEOFRANCE_API_TOKEN="${TF_VAR_meteo_api_key:-changeme}"
        export ANTHROPIC_API_KEY="${TF_VAR_anthropic_api_key:-changeme}"
        export KEYCLOAK_LAURENT_PASSWORD="${TF_VAR_keycloak_laurent_password:-changeme}"
        export MQTT_USER="${TF_VAR_mqtt_user:-laurent@itercraft.com}"
        export MQTT_PASSWORD="${TF_VAR_mqtt_password:-changeme}"
        export MQTT_BACKEND_PASSWORD="${TF_VAR_mqtt_backend_password:-changeme}"
    else
        echo -e "${YELLOW}Fichier env.sh non trouvé, utilisation des valeurs par défaut${NC}"
    fi
}

start() {
    echo -e "${GREEN}=== Démarrage de l'environnement de développement Itercraft ===${NC}"
    echo ""

    load_env
    cd "$PROJECT_ROOT"

    echo -e "${YELLOW}Build des images Docker...${NC}"
    docker compose -f "$COMPOSE_FILE" build

    echo ""
    echo -e "${YELLOW}Démarrage des containers...${NC}"
    docker compose -f "$COMPOSE_FILE" up -d

    echo ""
    echo -e "${GREEN}=== Containers démarrés ===${NC}"
    echo ""
    echo "Services disponibles:"
    echo "  - Frontend:  http://localhost:3000"
    echo "  - API:       http://localhost:8080"
    echo "  - Keycloak:  http://localhost:8180 (admin/admin)"
    echo "  - PostgreSQL: localhost:5432 (itercraft/itercraft)"
    echo "  - MQTT:       localhost:8883 (TLS)"
    echo ""
    echo "Commandes utiles:"
    echo "  $0 logs     - Voir les logs"
    echo "  $0 stop     - Arrêter les containers"
    echo "  $0 clean    - Tout supprimer"
}

stop() {
    echo -e "${YELLOW}Arrêt des containers...${NC}"
    cd "$PROJECT_ROOT"
    docker compose -f "$COMPOSE_FILE" stop
    echo -e "${GREEN}Containers arrêtés${NC}"
}

restart() {
    stop
    start
}

logs() {
    cd "$PROJECT_ROOT"
    docker compose -f "$COMPOSE_FILE" logs -f
}

logs_api() {
    cd "$PROJECT_ROOT"
    docker compose -f "$COMPOSE_FILE" logs -f api
}

clean() {
    echo -e "${YELLOW}Arrêt et suppression des containers et volumes...${NC}"
    cd "$PROJECT_ROOT"
    docker compose -f "$COMPOSE_FILE" down -v
    echo -e "${GREEN}Nettoyage terminé${NC}"
}

status() {
    cd "$PROJECT_ROOT"
    docker compose -f "$COMPOSE_FILE" ps
}

init_games() {
    echo -e "${YELLOW}Initialisation de la ludothèque de Laurent...${NC}"
    echo -e "${YELLOW}Note: Laurent doit s'être connecté au moins une fois.${NC}"
    cd "$PROJECT_ROOT"
    docker exec -i itercraft-postgres psql -U itercraft -d itercraft < "$SCRIPT_DIR/liquibase/scripts/init-laurent-games.sql"
    echo -e "${GREEN}Ludothèque initialisée${NC}"
}

check_docker

case "${1:-start}" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    logs)
        logs
        ;;
    logs-api)
        logs_api
        ;;
    clean)
        clean
        ;;
    status)
        status
        ;;
    init-games)
        init_games
        ;;
    -h|--help|help)
        usage
        ;;
    *)
        echo -e "${RED}Commande inconnue: $1${NC}"
        usage
        exit 1
        ;;
esac
