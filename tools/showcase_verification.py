"""Drive the 3.0 showcase exhibit: RCON builds the stage on the flat verification server while the client
(`runShowcaseClient`, -Dtribalpower.showcaseVerification=true -Dtribalpower.showcaseScreens=true) takes labelled
screenshots through the showcase-command.txt hand-off in client/ShowcaseVerification.java.

Usage: python3 tools/showcase_verification.py [--only blocks,entities,structures,items,screens,codex]
Screenshots land in build/showcase-client/screenshots/showcase-<n>-<label>.png.
"""
import re, sys, time
from pathlib import Path
from verification_rcon import command as rcon

ROOT = Path(__file__).resolve().parents[1]
CLIENT = ROOT / 'build/showcase-client'
PLAYER = 'LatticeQA'
FLOOR = -61          # flat world grass layer; first air block is FLOOR+1
X0, Z0 = 1000, 1000  # stage origin, far from spawn
TRIBES = ['soil', 'stone', 'sprout', 'claw', 'spark', 'clock', 'swarm', 'sigil', 'spindle']
ROLES = ['ELDER', 'DRUMMER', 'HUNTER', 'WEAVER']
ITEMS = ['tribe_hearth', 'tribe_banner', 'kinship_totem', 'tribe_mark', 'tribal_kin_elder_spawn_egg', 'tribal_kin_drummer_spawn_egg',
         'tribal_kin_hunter_spawn_egg', 'tribal_kin_weaver_spawn_egg', 'bonding_charm', 'mossback_scale', 'the_unsung_spawn_egg',
         'ley_lens', 'camp_charter', 'lore_tablet', 'silent_drum', 'pulse_gauge', 'pulse_threshold', 'echo_unweave']
CODEX = ['tribes_standing', 'tribes_kin', 'tribes_kinship_totem', 'tribe_soil', 'tribe_spindle', 'march_ancestor_hall', 'march_drum_circle',
         'march_the_unsung', 'march_crystal_spire', 'familiars_bonding', 'familiars_fox', 'familiars_mossback_stag', 'camps_identity',
         'rite_world_rites', 'rite_land_rites', 'ley_lens', 'pulse_logic', 'codex_diagnostics', 'loom_sixth_voice', 'tablet_0',
         'walk_first_camp', 'walk_sixth_voice', 'walk_wake_the_unsung', 'walk_first_rite', 'walk_bond_a_familiar', 'walk_found_a_camp',
         'chapter_1', 'chapter_12', 'camp_grove_tender', 'item:tribalpower:kinship_totem', 'item:tribalpower:tribe_hearth']


def say(text):
    print(time.strftime('%H:%M:%S'), text, flush=True)


def run(cmd):
    out = rcon(cmd)
    if out and ('Unknown' in out or 'Incorrect' in out or 'Expected' in out or 'No entity' in out or "Can't" in out or 'cannot' in out.lower() or 'not loaded' in out):
        say(f'  !! {cmd} -> {out.strip()}')
    return out


def client(cmd, timeout=30):
    """Send one command to the client's ShowcaseVerification poller and wait for its result."""
    done = CLIENT / 'showcase-done.txt'
    done.unlink(missing_ok=True)
    (CLIENT / 'showcase-command.txt').write_text(cmd + '\n')
    end = time.time() + timeout
    while time.time() < end:
        if done.is_file():
            result = done.read_text().splitlines()
            reply = result[1] if len(result) > 1 else '?'
            if not reply.startswith('ok'):
                say(f'  !! client {cmd} -> {reply}')
            return reply
        time.sleep(0.25)
    say(f'  !! client {cmd} timed out')
    return 'timeout'


def player_uuid():
    """UUID of the joined client player, from its int-array entity data (offline-mode servers do not log it)."""
    out = rcon(f'data get entity {PLAYER} UUID')
    m = re.search(r'\[I;\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]', out)
    if not m:
        raise SystemExit(f'player UUID unavailable ({out.strip()}); has the client joined?')
    # LatticeAnimal reads Owner with hasUUID(), which only accepts the int-array form.
    return '[I;' + ','.join(m.groups()) + ']'


def camera(x, y, z, fx, fy, fz, label, settle=6):
    """Stand the player on an invisible barrier at (x,y,z) facing a point, wait for chunks, take a labelled shot."""
    run(f'setblock {int(x // 1)} {int(y) - 1} {int(z // 1)} minecraft:barrier')
    run(f'teleport {PLAYER} {x} {y} {z} facing {fx} {fy} {fz}')
    time.sleep(settle)
    run(f'teleport {PLAYER} {x} {y} {z} facing {fx} {fy} {fz}')
    time.sleep(1)
    client(f'shot {label}')


def load(x0, z0, x1, z1):
    """Force-load a stage region so /setblock, /summon and /place work before the player arrives."""
    run(f'forceload add {x0} {z0} {x1} {z1}')
    time.sleep(3)


def prepare():
    say('preparing world')
    for cmd in [f'op {PLAYER}', f'gamemode creative {PLAYER}', 'gamerule doDaylightCycle false', 'gamerule doWeatherCycle false',
                'gamerule doMobSpawning false', 'difficulty normal', 'gamerule sendCommandFeedback false', 'time set 6000', 'weather clear',
                'kill @e[type=!player]', f'effect give {PLAYER} minecraft:resistance infinite 4 true',
                f'effect give {PLAYER} minecraft:slow_falling infinite 0 true']:
        run(cmd)


def blocks():
    say('block row')
    y = FLOOR + 1
    z = Z0
    load(X0 - 8, Z0 - 8, X0 + 70, Z0 + 8)
    row = []
    for i, t in enumerate(TRIBES):
        row.append((f'tribalpower:tribe_hearth', {'Tribe': i}))
    for i, t in enumerate(TRIBES):
        row.append((f'tribalpower:tribe_banner[tribe={i},facing=south,wall=false]', None))
    for i, t in enumerate(TRIBES):
        row.append((f'tribalpower:kinship_totem[tribe={i}]', None))
    row += [('tribalpower:resonance_totem_loom', None), ('tribalpower:echo_unweave', None), ('tribalpower:silent_drum', None),
            ('tribalpower:spirit_light', None)]
    x = X0
    for block, nbt in row:
        tag = '{' + ','.join(f'{k}:{v}' for k, v in nbt.items()) + '}' if nbt else ''
        run(f'setblock {x} {y} {z} {block}{tag}')
        x += 2
    # wall banners + lore tablets on a stone wall behind the row
    for i in range(12):
        wx = X0 + i * 2
        run(f'setblock {wx} {y} {z + 4} minecraft:stone_bricks')
        run(f'setblock {wx} {y + 1} {z + 4} minecraft:stone_bricks')
        run(f'setblock {wx} {y} {z + 3} tribalpower:lore_tablet[facing=north]{{Tablet:{i}}}')
    for i in range(9):
        wx = X0 + 24 + i * 2
        run(f'setblock {wx} {y} {z + 4} minecraft:stone_bricks')
        run(f'setblock {wx} {y + 1} {z + 4} minecraft:stone_bricks')
        run(f'setblock {wx} {y + 1} {z + 3} tribalpower:tribe_banner[tribe={i},facing=north,wall=true]')
    # pulse logic: charged drumheart with a gauge and a threshold pointing into it, plus a lamp
    lx = X0 + 44
    run(f'setblock {lx} {y} {z + 3} tribalpower:drumheart{{Pulse:6000}}')
    run(f'setblock {lx - 1} {y} {z + 3} tribalpower:pulse_gauge[facing=east]')
    run(f'setblock {lx + 1} {y} {z + 3} tribalpower:pulse_threshold[facing=west]')
    run(f'setblock {lx + 2} {y} {z + 3} minecraft:redstone_lamp')
    run(f'setblock {lx} {y} {z + 4} minecraft:stone_bricks')
    time.sleep(2)
    camera(X0 + 8.5, y + 1, z - 5.5, X0 + 8.5, y, z, 'hearths')
    camera(X0 + 26.5, y + 1, z - 5.5, X0 + 26.5, y, z, 'banners')
    camera(X0 + 44.5, y + 1, z - 5.5, X0 + 44.5, y, z, 'kinship_totems')
    camera(X0 + 59.5, y + 1, z - 4.5, X0 + 59.5, y, z, 'loom_unweave_drum_light')
    camera(X0 + 5.5, y + 1, z - 1.5, X0 + 5.5, y + 0.5, z + 3, 'lore_tablets')
    camera(X0 + 31.5, y + 1.5, z - 1.5, X0 + 31.5, y + 1.5, z + 3, 'wall_banners')
    camera(lx + 0.5, y + 1, z - 1.5, lx + 0.5, y + 0.5, z + 3, 'pulse_logic')
    camera(X0 + 1.5, y + 0.5, z - 1.5, X0 + 0.5, y + 0.5, z, 'hearth_close')
    camera(X0 + 20.5, y + 1, z - 1.5, X0 + 19.5, y + 0.5, z, 'banner_close')


def entities(uuid):
    say('entities')
    y = FLOOR + 1
    z = Z0 + 30
    load(X0 - 8, z - 8, X0 + 70, z + 8)
    for i, t in enumerate(TRIBES):
        for r, role in enumerate(ROLES):
            run(f'summon tribalpower:tribal_kin {X0 + i * 5 + r + 0.5} {y} {z + 0.5} {{Tribe:{i},Role:"{role}",PersistenceRequired:1b,NoAI:1b,Rotation:[180f,0f]}}')
    run(f'summon tribalpower:the_unsung {X0 + 50.5} {y} {z + 0.5} {{NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}}')
    for i, animal in enumerate(['dawn_stag', 'lantern_fox', 'mossback']):
        run(f'summon tribalpower:{animal} {X0 + 56 + i * 3 + 0.5} {y} {z + 0.5} {{Owner:{uuid},Age:0,NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}}')
    time.sleep(3)
    for i in range(0, 9, 3):
        camera(X0 + i * 5 + 6.5, y + 1, z - 5.5, X0 + i * 5 + 6.5, y + 1, z, f'kin_{TRIBES[i]}_{TRIBES[i+2]}')
    camera(X0 + 1.5, y + 0.6, z - 2.5, X0 + 1.5, y + 1, z, 'kin_soil_close')
    camera(X0 + 41.5, y + 0.6, z - 2.5, X0 + 41.5, y + 1, z, 'kin_spindle_close')
    camera(X0 + 50.5, y + 1, z - 6.5, X0 + 50.5, y + 1.5, z, 'the_unsung')
    camera(X0 + 47.5, y + 3, z - 3.5, X0 + 50.5, y + 1.5, z, 'the_unsung_side')
    camera(X0 + 50.5, y + 0.5, z - 3, X0 + 50.5, y + 2.5, z, 'the_unsung_close')
    run('kill @e[type=tribalpower:the_unsung]')  # its boss bar would otherwise sit on every later capture
    time.sleep(2)
    camera(X0 + 60.5, y + 1, z - 5.5, X0 + 60.5, y + 0.5, z, 'familiars_bonded')
    camera(X0 + 57.5, y + 0.5, z - 2.5, X0 + 57.5, y + 0.5, z, 'familiars_close')


def structures():
    say('structures')
    base = FLOOR - 1  # camp layer 1 is its surface: lands on the grass layer
    load(X0 + 100, Z0, X0 + 100 + 9 * 40, Z0 + 30)
    load(X0 + 100, Z0 + 100, X0 + 240, Z0 + 125)
    for i, t in enumerate(TRIBES):
        x, z = X0 + 100 + i * 40, Z0
        run(f'place template tribalpower:tribe_camp_{t} {x} {base} {z}')
    run(f'place template tribalpower:ancestor_hall {X0 + 100} {FLOOR - 2} {Z0 + 100}')
    run(f'place template tribalpower:drum_circle {X0 + 160} {FLOOR - 1} {Z0 + 100}')
    run(f'place template tribalpower:crystal_spire {X0 + 210} {FLOOR - 1} {Z0 + 100}')
    time.sleep(4)
    for i, t in enumerate(TRIBES):
        x, z = X0 + 100 + i * 40, Z0
        camera(x + 14.5, FLOOR + 12, z - 12.5, x + 14.5, FLOOR + 1, z + 14.5, f'camp_{t}', settle=8)
        if t in ('soil', 'stone', 'spark', 'sigil'):
            camera(x + 14.5, FLOOR + 2, z + 2.5, x + 14.5, FLOOR + 1, z + 14.5, f'camp_{t}_inside', settle=5)
    hx, hz = X0 + 100, Z0 + 100
    camera(hx + 22.5, FLOOR + 14, hz - 10.5, hx + 22.5, FLOOR + 4, hz + 10.5, 'ancestor_hall_outside', settle=8)
    # the hall's walkable layer 1 sits at FLOOR-1 when the template origin is FLOOR-2
    camera(hx + 3.5, FLOOR - 1, hz + 10.5, hx + 22.5, FLOOR - 1, hz + 10.5, 'ancestor_hall_inside', settle=5)
    camera(hx + 22.5, FLOOR - 1, hz + 10.5, hx + 22.5, FLOOR + 2, hz + 1.5, 'ancestor_hall_inside_b', settle=5)
    camera(hx + 40.5, FLOOR - 1, hz + 10.5, hx + 22.5, FLOOR - 1, hz + 10.5, 'ancestor_hall_inside_c', settle=5)
    dx, dz = X0 + 160, Z0 + 100
    camera(dx + 16.5, FLOOR + 14, dz - 8.5, dx + 16.5, FLOOR + 2, dz + 16.5, 'drum_circle', settle=8)
    camera(dx + 16.5, FLOOR + 3, dz + 3.5, dx + 16.5, FLOOR + 2, dz + 16.5, 'drum_circle_inside', settle=5)
    sx, sz = X0 + 210, Z0 + 100
    camera(sx + 10.5, FLOOR + 30, sz - 30.5, sx + 10.5, FLOOR + 20, sz + 10.5, 'crystal_spire', settle=8)
    camera(sx + 10.5, FLOOR + 3, sz - 6.5, sx + 10.5, FLOOR + 6, sz + 10.5, 'crystal_spire_base', settle=5)


def items():
    say('items')
    run(f'clear {PLAYER}')
    for i, item in enumerate(ITEMS[:9]):
        run(f'item replace entity {PLAYER} hotbar.{i} with tribalpower:{item}')
    run(f'item replace entity {PLAYER} hotbar.0 with tribalpower:tribe_hearth')
    time.sleep(2)
    x, y, z = X0 + 8.5, FLOOR + 2, Z0 - 5.5
    run(f'teleport {PLAYER} {x} {y} {z} facing {x} {y} {Z0}')
    time.sleep(2)
    client('shot hotbar_a')
    for i, item in enumerate(ITEMS[9:18]):
        run(f'item replace entity {PLAYER} hotbar.{i} with tribalpower:{item}')
    time.sleep(2)
    client('shot hotbar_b')
    run(f'clear {PLAYER}')


def screens(uuid):
    say('screens')
    y = FLOOR + 1
    z = Z0 + 60
    x = X0
    load(x - 8, z - 8, x + 16, z + 8)
    run(f'setblock {x} {y - 1} {z} minecraft:barrier')
    run(f'summon tribalpower:tribal_kin {x + 0.5} {y} {z + 2.5} {{Tribe:2,Role:"ELDER",PersistenceRequired:1b,NoAI:1b,Rotation:[180f,0f]}}')
    run(f'summon tribalpower:mossback {x + 4.5} {y} {z + 2.5} {{Owner:{uuid},Age:0,NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}}')
    # Elders only trade with Guests or better: offer the Rootbinders' reagent at their hearth (+8 each, creative keeps it)
    run(f'setblock {x + 2} {y} {z - 2} tribalpower:tribe_hearth{{Tribe:2}}')
    run(f'clear {PLAYER}')
    run(f'item replace entity {PLAYER} hotbar.0 with tribalpower:mossback_scale')
    run(f'teleport {PLAYER} {x + 0.5} {y} {z + 0.5} facing {x + 2.5} {y} {z - 1.5}')
    time.sleep(4)
    client(f'use {x + 2} {y} {z - 2} 20')
    time.sleep(2)
    client('shot hearth_offering')
    run(f'clear {PLAYER}')
    run(f'teleport {PLAYER} {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1} {z + 2.5}')
    time.sleep(3)
    client('interact tribalpower:tribal_kin')
    time.sleep(3)
    client('shot kin_trading')
    client('close')
    time.sleep(1)
    run(f'teleport {PLAYER} {x + 4.5} {y} {z + 0.5} facing {x + 4.5} {y + 1} {z + 2.5}')
    time.sleep(3)
    client('interact tribalpower:mossback sneak')
    time.sleep(3)
    client('shot mossback_saddlebag')
    client('close')
    time.sleep(1)
    for t in (0, 5, 11):
        client(f'tablet {t}')
        time.sleep(2)
        client(f'shot lore_tablet_{t}')
    client('close')


def codex():
    say('codex tour')
    for entry in CODEX:
        client(f'codex {entry}')
        time.sleep(2.5)
        client('shot codex_' + entry.replace(':', '_'))
    client('close')


if __name__ == '__main__':
    only = sys.argv[sys.argv.index('--only') + 1].split(',') if '--only' in sys.argv else None
    uuid = player_uuid()
    say(f'player {PLAYER} {uuid}')
    prepare()
    for name, fn in [('blocks', blocks), ('entities', lambda: entities(uuid)), ('structures', structures), ('items', items),
                     ('screens', lambda: screens(uuid)), ('codex', codex)]:
        if only is None or name in only:
            fn()
    say('done')
