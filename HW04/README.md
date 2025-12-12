# Инфраструктурные паттерны

##  Цель:
В этом ДЗ вы создадите простейший RESTful CRUD.


## Описание/Пошаговая инструкция выполнения домашнего задания:

Сделать простейший RESTful CRUD по созданию, удалению, просмотру и обновлению пользователей.

Пример API - https://app.swaggerhub.com/apis/otus55/users/1.0.0
Добавить базу данных для приложения.  
Конфигурация приложения должна хранится в Configmaps.  
Доступы к БД должны храниться в Secrets.  
Первоначальные миграции должны быть оформлены в качестве Job-ы, если это требуется.  
Ingress-ы должны также вести на url arch.homework/ (как и в прошлом задании). 

## На выходе должны быть предоставлена

1. ссылка на директорию в github, где находится директория с манифестами кубернетеса (в виде pull request).
2. инструкция по запуску приложения.
   - команда установки БД из helm, вместе с файлом values.yaml.
   - команда применения первоначальных миграций
   - команда kubectl apply -f, которая запускает в правильном порядке манифесты кубернетеса
3. Postman коллекция, в которой будут представлены примеры запросов к сервису на создание, получение, изменение и удаление пользователя. Важно: в postman коллекции использовать базовый url - arch.homework.
4. Проверить корректность работы приложения используя созданную коллекцию newman run коллекция_постман и приложить скриншот/вывод исполнения корректной работы


# Описание решения
Все приложение собирается из единого Helm чарта - name: hw04-service-user
1. В качестве БД используется postgresql, которое ставится как зависимость.
2. Неймспейс - hw04
3. Сам user-service, который является простейшим RESTful CRUD сервисом JAVA Spring Boot.
   1. Приложение работает в двух режимах на базе профилей:
      - migration - для миграций, в приложении встроен Liquibase, отключен веб сервер
      - app - для работы приложения  
   2. migration - запускается как JOB  
   3. app - запускается как Deployment  
   4. В секрете лежат пароли от БД 
   5. В конфигмапе лежат настройки приложения  
   6. В чарте так же есть папка secrets, в которой лежат секреты для БД. Предполагается, что она исключена из GIT.
4. Коллекция Postman для тестирования лежит в папке postman-tests и скриншот результатов тестов.  


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