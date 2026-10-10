#!/usr/bin/env python3
"""Compile the repository's actual A/C/E check classes against minimal offline packet stubs.
No Minecraft/Bukkit server runs. This repository has no F check and is not deployed AGC.
Only System.currentTimeMillis is replaced with a controllable replay clock.
"""
import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STUBS = {
    'club/mineman/antigamingchair/AntiGamingChair.java': 'package club.mineman.antigamingchair; public class AntiGamingChair {}',
    'org/bukkit/entity/Player.java': 'package org.bukkit.entity; public interface Player {}',
    'net/minecraft/server/v1_8_R3/Packet.java': 'package net.minecraft.server.v1_8_R3; public class Packet {}',
    'net/minecraft/server/v1_8_R3/PacketPlayInArmAnimation.java': 'package net.minecraft.server.v1_8_R3; public class PacketPlayInArmAnimation extends Packet {}',
    'net/minecraft/server/v1_8_R3/PacketPlayInFlying.java': 'package net.minecraft.server.v1_8_R3; public class PacketPlayInFlying extends Packet {}',
    'club/mineman/antigamingchair/event/player/PlayerAlertEvent.java': 'package club.mineman.antigamingchair.event.player; public class PlayerAlertEvent { public enum AlertType { RELEASE, EXPERIMENTAL } }',
    'replay/Clock.java': 'package replay; public class Clock { public static long now; }',
    'club/mineman/antigamingchair/data/PlayerData.java': '''package club.mineman.antigamingchair.data;
import java.util.*;
public class PlayerData {
 public long lastMove;
 public boolean digging,placing;
 private final Map<Object,Double> vl=new HashMap<>();
 public boolean isDigging(){return digging;} public boolean isPlacing(){return placing;}
 public long getLastDelayedMovePacket(){return -1000000;}
 public Move getLastMovePacket(){return new Move(lastMove);} public record Move(long timestamp){public long getTimestamp(){return timestamp;}}
 public double getCheckVl(Object c){return vl.getOrDefault(c,0.0);} public void setCheckVl(double n,Object c){vl.put(c,n);}
 public int getViolations(Object c,long window){return 0;} public boolean isBanning(){return false;} public void setCps(int cps){}
}''',
    'club/mineman/antigamingchair/check/checks/PacketCheck.java': '''package club.mineman.antigamingchair.check.checks;
import club.mineman.antigamingchair.*;import club.mineman.antigamingchair.data.*;import club.mineman.antigamingchair.event.player.*;
import org.bukkit.entity.Player;import net.minecraft.server.v1_8_R3.Packet;
public abstract class PacketCheck {
 protected final PlayerData playerData; public int alerts;
 public PacketCheck(AntiGamingChair p,PlayerData d){playerData=d;}
 public abstract void handleCheck(Player p,Packet packet);
 protected boolean alert(PlayerAlertEvent.AlertType t,Player p,String text){alerts++;return true;}
 protected void ban(Player p,String reason){}
}''',
    'club/mineman/antigamingchair/check/Placeholder.java': 'package club.mineman.antigamingchair.check; public class Placeholder {}',
}
SIMULATOR = '''
import io.github.liwwyy.orvenbw.feature.clickassist.*;
import club.mineman.antigamingchair.check.impl.autoclicker.*;
import club.mineman.antigamingchair.check.checks.*;
import club.mineman.antigamingchair.data.*;
import club.mineman.antigamingchair.*;
import net.minecraft.server.v1_8_R3.*;
import java.util.*;
public class Simulation {
 public static void main(String[] args) throws Exception {
  for(String mode:List.of("long_use","partial_restarts","physical_mix")) {
   for(int profile=0;profile<2;profile++) {
    Random random=new Random(73+profile);var engine=new ClickAssistEngine();
    var session=new ClickProfileSession(random::nextDouble,0);
    var data=new PlayerData();var plugin=new AntiGamingChair();
    PacketCheck[] checks={new AutoClickerA(plugin,data),new AutoClickerC(plugin,data),new AutoClickerE(plugin,data)};
    java.lang.reflect.Method begin=null;try{begin=engine.getClass().getMethod("beginTick");}catch(NoSuchMethodException ignored){}
    int swings=0,duplicateTicks=0,peak=0;long nextPhysical=0;
    for(long ms=0;ms<120000;ms+=50) {
     replay.Clock.now=ms+3000;long ns=ms*1000000;int count=0;
     if(begin!=null)begin.invoke(engine);
     if(mode.equals("physical_mix")&&ms>=nextPhysical) {
      engine.physicalClick(0,ns);for(var check:checks)check.handleCheck(null,new PacketPlayInArmAnimation());count++;
      nextPhysical=ms+(long)(80+random.nextDouble()*150);
     }
     double target=session.target(ns,true,new ClickProfileSession.Options(profile,true,22));
     double rate=Math.max(0,target-engine.manualRate(0,ns));
     if(mode.equals("partial_restarts")&&ms%2400>=1500&&ms%2400<1850)rate=0;
     int generated=engine.pollDue(0,ns,rate,true,0,22,session::intervalWeight,n->{for(var check:checks)check.handleCheck(null,new PacketPlayInArmAnimation());});
     count+=generated;swings+=count;if(count>1)duplicateTicks++;peak=Math.max(peak,count);
     data.lastMove=replay.Clock.now;for(var check:checks)check.handleCheck(null,new PacketPlayInFlying());
    }
    System.out.printf(java.util.Locale.ROOT,"{\\"mode\\":\\"%s\\",\\"profile\\":\\"%s\\",\\"seconds\\":120,\\"swings\\":%d,\\"duplicate_ticks\\":%d,\\"max_per_tick\\":%d,\\"A\\":%d,\\"C\\":%d,\\"E\\":%d}%n",mode,profile==0?"Humble":"Performative",swings,duplicateTicks,peak,checks[0].alerts,checks[1].alerts,checks[2].alerts);
   }
  }
 }
}
'''


def run(repo, engine, output):
    sources = repo / 'AGC/club/mineman/antigamingchair/check/impl/autoclicker'
    if not sources.is_dir():
        raise SystemExit(f'Missing source directory: {sources}')
    gson = sorted((Path.home() / '.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson').glob('*/*/gson-*.jar'))
    gson = [p for p in gson if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
    if not gson:
        raise SystemExit('Run ./gradlew build first to cache Gson.')
    java = Path('/usr/lib/jvm/java-27-temurin/bin')
    with tempfile.TemporaryDirectory(prefix='orven-agc-replay-') as directory:
        temp = Path(directory)
        for path, text in STUBS.items():
            target = temp / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)
        for source in sources.glob('*.java'):
            text = source.read_text()
            name = re.search(r'public class (\w+)', text).group(1)
            # Preserve every check expression, threshold and state update.
            target = temp / f'club/mineman/antigamingchair/check/impl/autoclicker/{name}.java'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text.replace('System.currentTimeMillis()', 'replay.Clock.now'))
        for name, source in [('ClickAssistEngine', engine), ('ClickProfileSession', ROOT / 'src/main/java/io/github/liwwyy/orvenbw/feature/clickassist/ClickProfileSession.java')]:
            target = temp / f'io/github/liwwyy/orvenbw/feature/clickassist/{name}.java'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read_bytes())
        (temp / 'Simulation.java').write_text(SIMULATOR)
        classes = temp / 'classes'
        classes.mkdir()
        subprocess.run([str(java / 'javac'), '--release', '25', '-cp', str(gson[-1]), '-d', str(classes), *map(str, temp.rglob('*.java'))], check=True, capture_output=True)
        result = subprocess.run([str(java / 'java'), '-cp', f'{classes}:{ROOT / "src/main/resources"}:{gson[-1]}', 'Simulation'], check=True, capture_output=True, text=True)
        report = {'source_commit': subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip(), 'engine': str(engine), 'missing_checks': ['F'], 'limits': 'Deterministic offline replay; assumes one movement every 50ms, no lag, no digging/placing. Not deployed AGC and not a guarantee.', 'results': [json.loads(line) for line in result.stdout.splitlines()]}
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, indent=2) + '\n')
        print(json.dumps(report, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=ROOT / '.reference/AntiGameChair')
    parser.add_argument('--engine', type=Path, default=ROOT / 'src/main/java/io/github/liwwyy/orvenbw/feature/clickassist/ClickAssistEngine.java')
    parser.add_argument('--output', type=Path, default=ROOT / '.reference/agc-replay-0.7.3.json')
    args = parser.parse_args()
    try:
        run(args.repo, args.engine, args.output)
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.stderr or str(error)) from None
