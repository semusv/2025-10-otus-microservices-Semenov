# microservices_book

### Команды сборки
```bash
./gradlew licensing-service:jibDockerBuild
./gradlew licensing-service:jib
```


```bash
# 1. Собрать все образы и запустить полный стек
./gradlew composeUp

# 2. Только с лицензионным сервисом
./gradlew composeUpLicensing

# 3. С профилем разработки
./gradlew composeUpDev

# 4. Просмотр статуса
./gradlew composePs

# 5. Логи
./gradlew composeLogs

# 6. Остановка
./gradlew composeDown

# 7. Пересборка и перезапуск
./gradlew composeRestart
```