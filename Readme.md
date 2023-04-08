# Blockcha

Main:
[![Tests](https://github.com/prdumbledore/blockcha/actions/workflows/gradle-tests.yml/badge.svg?branch=main)](https://github.com/prdumbledore/blockcha/actions/workflows/gradle-tests.yml)
Develop:
[![Tests](https://github.com/prdumbledore/blockcha/actions/workflows/gradle-tests.yml/badge.svg?branch=dev)](https://github.com/prdumbledore/blockcha/actions/workflows/gradle-tests.yml)

Запуск (перед запуском собрать jarСобрать gradlew.bat JarBuild)
Лежит в build/libs):
```
java -jar blockChain-0.0.1.jar 8080 8081 8082 1
java -jar blockChain-0.0.1.jar 8081 8080 8082 0
java -jar blockChain-0.0.1.jar 8082 8080 8081 0
```
Аргументы:
[0] - порт текущего узла
[1] - порт второго узла
[2] - порт третьего узла
[3] - флаг главного узла ("1" - главный, "0" - не главный)

## Docker
```
docker build -t blockСhain.
```

## Test

Были реализованы тесты:

- Модульные тесты
- Интеграционные