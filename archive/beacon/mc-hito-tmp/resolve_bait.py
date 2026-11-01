from resolve import resolve
from pathlib import Path
for name in ['gradle.properties','README.md','build.gradle','.github/workflows/build.yml','docs/plan/TOOLCHAIN.md','src/clientSmoke/resources/fabric.mod.json']:resolve(name)
resolve('src/main/resources/fabric.mod.json',[lambda m:m[1].replace('"mixins": [','"mixins": [\n    "unallocated.bait.mixins.json",\n    {"config": "unallocated.bait.client.mixins.json", "environment": "client"},')])
resolve('scripts/smoke_test.py',['o','o',lambda m:'''                failed = SELFTEST_FAILED.search(game_log)
                if failed:
                    raise RuntimeError(f'In-game self-test failed: {failed.group(0)}')
'''+m[1],'o','o','t','t','o'])
p=Path('scripts/smoke_test.py');s=p.read_text().replace("startup + [entered, status],", "startup + [entered, status, r'UNALLOCATED bait selftest passed'],",1).replace("('unallocated act LOBBY', [entered]), ('unallocated status', [status])],", "('unallocated act LOBBY', [entered]), ('unallocated status', [status]),\n            ('ubait selftest', [r'UNALLOCATED bait selftest passed'])],",1)
s=s.replace("r'UNALLOCATED interface smoke: effects rendered; options untouched byte-for-byte after world exit\\.',", "r'UNALLOCATED interface smoke: effects rendered; options untouched byte-for-byte after world exit\\.',\n                r'UNALLOCATED bait step=2/2',\n                r'UNALLOCATED bait screamer shown',\n                r'UNALLOCATED spyglass smoke: three looks, closer each time, screamer, figure gone\\.',")
s=s.replace("('splash', 'loading', 'f3', 'advancement', 'sign', 'death')]],", "('splash', 'loading', 'f3', 'advancement', 'sign', 'death')],\n                            *BAIT_SCREENSHOTS],")
s=s.replace("'timeout': 240,", "'timeout': 420,",1);p.write_text(s)
p=Path('src/clientSmoke/java/org/unallocated/smoke/TitleScreenSmokeTest.java');p.write_text(p.read_text().replace('InterfaceEffectsSmokeTest.run(context);', 'InterfaceEffectsSmokeTest.run(context);\n            new SpyglassBaitSmokeTest().runTest(context);'))
p=Path('src/main/java/org/unallocated/bait/SpyglassBaitLogic.java');s=p.read_text().replace('            lastMove = age;\n            if (phase == Phase.WAITING)', '''            lastMove = age;
            // Placement succeeded: a director step may use the very next hidden moment.
            // Failed MOVE attempts still retain their own retry deadline in move().
            lastMoveAttempt = age - RETRY_TICKS;
            if (phase == Phase.WAITING)''');p.write_text(s)
p=Path('src/test/java/org/unallocated/bait/SpyglassBaitLogicTest.java');s=p.read_text().replace('    @Test\n    void neverPlacedOrMovedWhileVisible()', '''    @Test
    void failedForcedMoveWaitsBeforeRetryingAndDoesNotAdvance() {
        SpyglassBaitLogic logic = placed(THREE);
        assertTrue(logic.forceStep());
        assertEquals(Action.MOVE, logic.tick(hidden(60)).action());
        logic.moved(false);
        assertEquals(0, logic.step());
        quiet(logic, hidden(60), SpyglassBaitLogic.RETRY_TICKS / 2 - 1);
        assertEquals(Action.MOVE, logic.tick(hidden(60)).action());
        logic.moved(true);
        assertEquals(1, logic.step());
    }

    @Test
    void neverPlacedOrMovedWhileVisible()''');p.write_text(s)
