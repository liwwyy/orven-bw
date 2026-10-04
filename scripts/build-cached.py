#!/usr/bin/env python3
"""Offline fallback for environments where Gradle's local lock sockets are unavailable.
Requires an existing Gradle dependency cache and this project's generated Loom cache.
The usual supported build remains ./gradlew build.
"""
from pathlib import Path
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent.parent
os.chdir(ROOT)
JDK = Path(os.environ.get("ORVEN_JAVA_HOME", "/usr/lib/jvm/java-27-temurin"))
CACHE = Path.home() / ".gradle/caches"
TOOLS = ROOT / "build/cached-tools"
TOOLS.mkdir(parents=True, exist_ok=True)


def run(*args):
    try:
        subprocess.run(list(map(str, args)), check=True)
    except subprocess.CalledProcessError as error:
        raise SystemExit(f"{args[0]} failed (exit {error.returncode})") from None


def version_key(path):
    return tuple((0, int(x)) if x.isdigit() else (1, x) for x in re.split(r"[.\-+]", path.name))


jars = []  # The published SDK must already be in the Gradle cache.
for artifact in (CACHE / "modules-2/files-2.1").glob("*/*"):
    versions = [p for p in artifact.iterdir() if p.is_dir()]
    if versions:
        jars += list(max(versions, key=version_key).glob("*/*.jar"))
mc = [p for p in (ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*.jar") if not p.name.endswith("-sources.jar")]
if not mc:
    raise SystemExit("Run the normal Gradle build once to populate the named Minecraft cache.")
jars += mc
classpath = os.pathsep.join(map(str, jars))
main_out = ROOT / "build/classes/java/main"
test_out = ROOT / "build/classes/java/test"
for folder in (main_out, test_out):
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir(parents=True)


def compile_sources(sources, output, cp):
    arguments = ["--release", "25", "-proc:none", "-cp", cp, "-d", str(output), *map(str, sources)]
    argfile = TOOLS / "javac.args"
    argfile.write_text("\n".join('"' + a.replace("\\", "\\\\").replace('"', '\\"') + '"' for a in arguments))
    run(JDK / "bin/javac", "@" + str(argfile))


compile_sources((ROOT / "src/main/java").rglob("*.java"), main_out, classpath)
runner = TOOLS / "TestRunner.java"
runner.write_text('''
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class TestRunner {
 public static void main(String[] args) {
  var request = LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("io.github.liwwyy.orvenbw")).build();
  var listener = new SummaryGeneratingListener();
  LauncherFactory.create().execute(request, listener);
  var summary = listener.getSummary();
  summary.printTo(new java.io.PrintWriter(System.out));
  summary.printFailuresTo(new java.io.PrintWriter(System.out));
  if (summary.getTestsFoundCount() == 0 || summary.getTotalFailureCount() > 0) System.exit(1);
 }
}
''')
remap = TOOLS / "Remap.java"
remap.write_text('''
import java.nio.file.*;
import net.fabricmc.tinyremapper.*;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;
public class Remap {
 public static void main(String[] args) throws Exception {
  var remapper = TinyRemapper.newRemapper().withMappings(TinyUtils.createTinyMappingProvider(Path.of(args[2]), "named", "intermediary"))
    .extension(new MixinExtension()).threads(2).build();
  try (var out = new OutputConsumerPath(Path.of(args[1]))) {
   out.addNonClassFiles(Path.of(args[0]));
   remapper.readClassPath(java.util.Arrays.stream(args).skip(3).map(Path::of).toArray(Path[]::new));
   remapper.readInputs(Path.of(args[0]));
   remapper.apply(out);
  } finally { remapper.finish(); }
 }
}
''')
compile_sources([*(ROOT / "src/test/java").rglob("*.java"), runner, remap], test_out, classpath + os.pathsep + str(main_out))
run(JDK / "bin/java", "-cp", os.pathsep.join([str(test_out), str(main_out), classpath]), "TestRunner")
properties = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines() if "=" in line and not line.startswith("#"))
version = properties["mod_version"] + "+mc" + properties["minecraft_version"]
name = "orven-bw-Ornithe-" + version
dev = ROOT / "build/devlibs" / (name + "-dev.jar")
jar = ROOT / "build/libs" / (name + ".jar")
dev.parent.mkdir(parents=True, exist_ok=True)
jar.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(dev, "w", zipfile.ZIP_DEFLATED) as z:
    for p in main_out.rglob("*.class"):
        z.write(p, p.relative_to(main_out))
    for p in (ROOT / "src/main/resources").rglob("*"):
        if p.is_file():
            z.writestr(str(p.relative_to(ROOT / "src/main/resources")), p.read_text().replace("${version}", version))
    z.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nCalamus-Generation: 2\nFabric-Jar-Type: classes\nFabric-Loom-Mixin-Remap-Type: static\nFabric-Minecraft-Version: 1.8.9\nFabric-Mapping-Namespace: intermediary\n\n")
mappings = CACHE / "fabric-loom/1.8.9/loom.mappings.1_8_9.layered+hash.1480139456-v2/mappings.tiny"
if not mappings.exists():
    raise SystemExit("The cached Feather Gen 2 build 2 mappings are missing; use the normal Gradle build.")
if jar.exists():
    jar.unlink()
run(JDK / "bin/java", "-Xmx2G", "-cp", str(test_out) + os.pathsep + classpath, "Remap", dev, jar, mappings, *mc)
with zipfile.ZipFile(jar.parent / (name + "-sources.jar"), "w", zipfile.ZIP_DEFLATED) as z:
    for folder in (ROOT / "src/main/java", ROOT / "src/main/resources"):
        for p in folder.rglob("*"):
            if p.is_file():
                z.write(p, p.relative_to(folder))
print("Built", jar)
