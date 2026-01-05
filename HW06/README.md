# Backend for frontends. Apigateway

##  Цель:
В этом ДЗ вы научитесь добавлять в приложение аутентификацию и регистрацию пользователей.


## Описание/Пошаговая инструкция выполнения домашнего задания:
Добавить в приложение аутентификацию и регистрацию пользователей.

Реализовать сценарий "Изменение и просмотр данных в профиле клиента".

Пользователь регистрируется. Заходит под собой и по определенному урлу получает данные о своем профиле. 
Может поменять данные в профиле. Данные профиля для чтения и редактирования не должны быть доступны другим клиентам (аутентифицированным или нет).

## На выходе должны быть предоставлена
1. описание архитектурного решения и схема взаимодействия сервисов (в виде картинки)
2. команда установки приложения (из helm-а или из манифестов). 
Обязательно указать в каком namespace нужно устанавливать.

В тестах обязательно
- наличие {{baseUrl}} для урла
- использование домена arch.homework в качестве initial значения {{baseUrl}}
- использование сгенерированных случайно данных в сценарии
- отображение данных запроса и данных ответа при запуске из командной строки с помощью newman.



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
   7. При старте миграция и приложение запускаются с Init контейнером, которые ждет готовности предыдущего шага.  
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