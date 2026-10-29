Locki 26.2 build recovery

1. Unzip this folder somewhere, then copy src/main/java/ladysnake/requiem/common/locki/LockiSpiritSystem.java over the same path in your project. It fixes the Optional<Double> -> float compile error using .floatValue().

OR, preferably for an existing Windows working tree, run the supplied script from PowerShell:
  .\apply_locki_build_recovery.ps1 -ProjectRoot 'C:\Mods developer\possuidor-template-26.2'

The script backs up LockiSpiritSystem.java before editing. If an extra LockiDebugCommand.java exists and no other Java source references it, the script renames it to LockiDebugCommand.java.disabled so javac does not compile that stale, unregistered diagnostic class. The original is preserved under that filename and can be restored later. If it has references elsewhere, the script leaves it untouched.

Why: the supplied refined project does not contain or register LockiDebugCommand.java, while the build log shows a local copy using incompatible/mixed Minecraft APIs. Its missing CommandContext, invalid permission predicate return type, outdated text/style packages, bad CustomData references, and argument mismatch are source incompatibilities, not 92 independent astral-system errors. To retain that debug command, it needs a separate migration against the actual target APIs rather than a blind replacement.

After applying, run .\gradlew.bat compileJava. This package has not been verified by a full Gradle build in this environment; the next compile may expose additional errors.
