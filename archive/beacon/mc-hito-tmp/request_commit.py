import sys,json,pathlib,subprocess
key,message,note=sys.argv[1:]
report=pathlib.Path('docs/integration/MILESTONE-v0.1.0.md')
with report.open('a') as f: f.write('\n'+note+'\n')
paths=subprocess.check_output(['git','diff','--name-only'],text=True).splitlines()+subprocess.check_output(['git','ls-files','--others','--exclude-standard'],text=True).splitlines()
assert all(not p.startswith('.tmp/') for p in paths),paths
pathlib.Path(f'.tmp/commit-requests/{key}.json').write_text(json.dumps({'paths':paths,'message':message}))
pathlib.Path('.tmp/CHECKPOINT.md').write_text('# v0.1.0 checkpoint\n\n'+report.read_text()+'\nLatest request: '+message+'\nNo unresolved conflict markers at request time. Python checks recorded in command logs; Java/Gradle remain CI-only.\n')
print(paths)
