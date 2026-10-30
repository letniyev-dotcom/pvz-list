# ПВЗ лист — Android (Kotlin + Jetpack Compose)

Только release-сборка. Debug-вариант отключён.

## Как получить APK
1. Залей содержимое этого архива в репозиторий GitHub (ветка `main`).
2. Открой вкладку **Actions** → workflow **Build release APK** запустится сам
   (или нажми **Run workflow**).
3. По завершении скачай APK из **Artifacts** → `pvz-list-release-apk`.

Чтобы APK ещё и появился в **Releases**, поставь тег:
`git tag v1.0 && git push origin v1.0`

## Подпись
Релизный ключ: `app/release.jks` (alias `pvz`), пароли прописаны в `app/build.gradle.kts`.
Держи репозиторий приватным и сохрани копию ключа: им нужно подписывать все обновления.

## Шрифт
Onest скачивается в CI в `app/src/main/assets/fonts/onest.ttf`.
Если положишь этот файл в репозиторий сам — будет использован он.

## Вход через Telegram (TDLib)
Вход настоящий: номер → код из Telegram → облачный пароль (если включён).
Сессия хранится в приватной папке приложения, после перезапуска вход не нужен.
`api_id` и `api_hash` лежат в `app/src/main/java/ru/pvzlist/app/Tg.kt` — репозиторий должен быть приватным.
Библиотека `com.github.tdlibx:td` приходит с JitPack. Если первая сборка в Actions упала
с таймаутом на jitpack.io — просто нажмите **Re-run jobs**: JitPack при первом обращении собирает пакет.
