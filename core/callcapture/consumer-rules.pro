# Shizuku crea el servicio por su nombre en otro proceso: R8 no debe renombrarlo
-keep class com.traduya.core.callcapture.shell.CallAudioUserService { <init>(...); }
-keep class com.traduya.core.callcapture.ICallAudioService { *; }
-keep class com.traduya.core.callcapture.ICallAudioService$* { *; }
