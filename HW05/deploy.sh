#!/bin/bash

CHART_PATH="./hw04-service-user"
RELEASE_NAME="hw04-service-user"
NAMESPACE="hw04"
SECRETS_FILE="./hw04-service-user/secrets/secret.yaml"
ACTION="${1:-upgrade}"  # По умолчанию upgrade

echo "Performing Helm $ACTION for $RELEASE_NAME..."

case $ACTION in
    "install")
        helm install $RELEASE_NAME \
            $CHART_PATH \
            --namespace $NAMESPACE \
            --dependency-update \
            --create-namespace \
            --values $SECRETS_FILE \
            --render-subchart-notes
        ;;

    "upgrade")
        helm upgrade $RELEASE_NAME \
            $CHART_PATH \
            --namespace $NAMESPACE \
            --dependency-update \
            --create-namespace \
            --values $SECRETS_FILE \
            --render-subchart-notes \
            --install
        ;;

    "uninstall")
        echo "WARNING: This will uninstall release '$RELEASE_NAME' from namespace '$NAMESPACE'"
        read -p "Are you sure? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            helm uninstall $RELEASE_NAME \
                --namespace $NAMESPACE
        else
            echo "Uninstall cancelled."
            exit 0
        fi
        ;;

    "status")
        helm status $RELEASE_NAME \
            --namespace $NAMESPACE
        ;;

    "list")
        helm list \
            --namespace $NAMESPACE
        ;;

    "history")
        helm history $RELEASE_NAME \
            --namespace $NAMESPACE
        ;;

    "rollback")
        REVISION="${2:-1}"
        echo "Rolling back to revision $REVISION..."
        helm rollback $RELEASE_NAME $REVISION \
            --namespace $NAMESPACE
        ;;

    "template")
        echo "Rendering Helm templates..."
        helm template $RELEASE_NAME \
            $CHART_PATH \
            --namespace $NAMESPACE \
            --values $SECRETS_FILE \
            --render-subchart-notes
        ;;

    *)
        echo "Unknown action: $ACTION"
        echo "Available actions:"
        echo "  install     - Install the chart"
        echo "  upgrade     - Upgrade the chart (default)"
        echo "  uninstall   - Uninstall the release"
        echo "  status      - Show release status"
        echo "  list        - List releases in namespace"
        echo "  history     - Show release history"
        echo "  rollback <revision> - Rollback to specific revision"
        echo "  template    - Render templates without installation"
        exit 1
        ;;
esac

if [ $? -eq 0 ] && [ "$ACTION" != "template" ]; then
    echo "Helm $ACTION completed successfully!"
else
    echo "Helm $ACTION finished with status: $?"
fi