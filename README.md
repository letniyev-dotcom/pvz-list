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
