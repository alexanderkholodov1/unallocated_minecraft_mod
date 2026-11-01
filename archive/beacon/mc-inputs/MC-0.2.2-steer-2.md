Captain (2026-10-28 07:10Z): your last session ended on OpenAI's "Selected model is at capacity" error, after you
committed `2867c3e` (isolate story mixins). Codex's weekly quota reset tonight, so quota is no longer a constraint.
Two rehearsals of `2867c3e` since then, each 3 of 4 jobs passing:
- run 40643489145: the 0.161.0 job printed nothing after "Starting a Gradle Daemon" for 35 minutes and hit the job
  timeout (infrastructure);
- run 40647196328: the 0.153.1 job (the profile Loader must reject) failed in "Smoke test dedicated server" with
  `java.lang.UnsupportedClassVersionError: net/minecraft/bundler/Main has been compiled by a more recent version of the
  Java Runtime (class file version 69.0)`. `scripts/smoke_test.py:265` starts the server with a bare `java` from PATH,
  so a runner whose PATH java is older than `JAVA_HOME` (25) breaks the smoke. That is a real robustness defect.

Do now, in this package:
1. Make every Java process the smoke scripts start use the JDK of `JAVA_HOME` (fail with a clear message if it is unset
   or not Java 25), with a Python test that proves a PATH java is never used. You own `scripts/smoke_test.py` for this.
2. Commit through the relay (`0.2.0.2: start the smoke server with JAVA_HOME's Java, never PATH's`), request a
   rehearsal and wait. All four jobs must pass; a failure that is only infrastructure (a download timeout, a silent
   hang until the job timeout) gets one more rehearsal, said in your report.
3. When it passes, report the real-game evidence of the `story` phase (marker lines and screenshot names from the
   diagnostics artifact of that run) and stop.
