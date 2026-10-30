# TDLib вызывает классы и поля из нативного кода по именам — их нельзя переименовывать и вырезать
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**
