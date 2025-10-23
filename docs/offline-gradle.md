# Running Gradle tasks without downloading the distribution

Some environments (such as CI sandboxes or restricted corporate networks) block
outbound traffic to `services.gradle.org`, which prevents the Gradle Wrapper
from downloading the distribution archive that it normally relies on. This
repository already vendors Gradle 8.9 under [`gradle-8.9/`](../gradle-8.9), so
you can point the wrapper at that local copy instead of attempting a download.

## Linux and macOS

```bash
STATUSWINDOW_USE_BUNDLED_GRADLE=1 ./gradlew -Dorg.gradle.java.home="$JAVA_HOME" :app:test
```

The `STATUSWINDOW_USE_BUNDLED_GRADLE` flag tells the wrapper script to execute
the bundled `gradle-8.9/bin/gradle` binary directly. If the distribution folder
is missing or not executable you will see an explanatory error message.

## Windows PowerShell or Command Prompt

```powershell
set STATUSWINDOW_USE_BUNDLED_GRADLE=1
set ORG_GRADLE_JAVA_HOME=%JAVA_HOME%
./gradlew.bat :app:test
```

The batch file has the same fallback behaviour, invoking
`gradle-8.9\bin\gradle.bat` when the flag is set. Setting
`ORG_GRADLE_JAVA_HOME` overrides the project-level `org.gradle.java.home`
property to point at the JDK that is available on your machine.

## Running without the wrapper

You can also skip the wrapper entirely by invoking the bundled binary:

```bash
./gradle-8.9/bin/gradle -Dorg.gradle.java.home="$JAVA_HOME" :app:test
```

This mirrors what the wrapper does under the hood once the fallback flag is
enabled.

> **Tip:** The repository's `gradle.properties` points `org.gradle.java.home`
> to the embedded Android Studio runtime on the original developer's machine.
> Override it with the `-Dorg.gradle.java.home=...` flag (as shown above) or by
> exporting `ORG_GRADLE_JAVA_HOME` before invoking Gradle so that it can find a
> JDK on your system.
