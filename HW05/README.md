
``` bash
# открываем терминал в папке с проектом /HW04  

# Ставим helm через bash скрипт: deploy.sh
./deploy.sh {action}
    где action:
        install     - Install the chart
        upgrade     - Upgrade the chart (default)
        uninstall   - Uninstall the release
        status      - Show release status
        list        - List releases in namespace
        history     - Show release history
./deploy.sh install
./deploy.sh upgrade
./deploy.sh list
./deploy.sh status
./deploy.sh uninstall

#дожидаемся всех подов. Должно быть 3 пода сервиса, 1 под БД, 1 завершенный JOB 
$ kubectl get po -n hw04
    #$ kubectl get po -n hw04
    #NAME                                 READY   STATUS      RESTARTS   AGE
    #hw04-service-user-56d45f86fb-6cksp   0/1     Running     0          108s
    #hw04-service-user-56d45f86fb-lq9n2   0/1     Running     0          108s
    #hw04-service-user-56d45f86fb-t8cdf   0/1     Running     0          108s
    #hw04-service-user-migration-s4c4s    0/1     Completed   0          108s
    #postgresql-0                         1/1     Running     0          108s

#Убеждаемся что все работает
curl http://arch.homework/user-service/actuator/health

#Запускаем тесты
$ newman run ./postman-tests/otus-hw4.postman_collection.json

#Чистим за собой Chart
$ ./deploy.sh uninstall

#Удаляем неймспейс
$ kubectl delete ns hw04

```


``` bash
#установка Nginx с включенным мониторингом
helm install nginx ingress-nginx/ingress-nginx  `
--namespace ng                                  `         
-f nginx-ingress.yaml                           `
--set controller.metrics.enabled=true           `
--set controller.metrics.serviceMonitor.enabled=true `
--set controller.metrics.serviceMonitor.additionalLabels.release="prometheus"

    #Обновление Nginx с включенным мониторингом
    helm upgrade nginx ingress-nginx/ingress-nginx `
    --namespace ng `
    -f nginx-ingress.yaml                           `
    --set controller.metrics.enabled=true `
    --set controller.metrics.serviceMonitor.enabled=true `
    --set controller.metrics.serviceMonitor.additionalLabels.release="prometheus"

#Установка Prometheus и Grafana
helm install stack prometheus-community/kube-prometheus-stack `
-f prometheus.yaml `
--namespace prometheus `
--create-namespace

    #Обновление Prometheus и Grafana
    helm upgrade stack prometheus-community/kube-prometheus-stack `
    -f prometheus.yaml `
    --namespace prometheus 

#PortForward для Grafana и Prometheus
port-forward services/prometheus-operated 9090:9090 -n prometheus
port-forward services/stack-grafana 9000:80 -n prometheus

#Включим туннель для сервиса
minikube tunnel

#Проверим что сервис доступен 
curl http://arch.homework/user-service/actuator/health

#Запускаем тесты Newman
newman run ./postman-tests/otus-hw5.postman_collection.json


```