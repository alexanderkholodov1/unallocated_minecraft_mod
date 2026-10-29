"""Synchronize only contract language keys from the owner's editable Appendix C."""

import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'contract.unallocated.'


def contract_text():
    source = (ROOT / 'docs/plan/CONTRACT-TEXT.md').read_text(encoding='utf-8')
    match = re.search(r'<!-- contract-text:start -->\s*```json\n(.*?)\n```\s*<!-- contract-text:end -->',
                      source, re.S)
    if not match:
        raise ValueError('Missing contract source block')
    data = json.loads(match.group(1))
    if not data or not all(key.startswith(PREFIX) and isinstance(value, str) for key, value in data.items()):
        raise ValueError('Contract source must contain only contract string keys')
    return data


def synchronize(write=False):
    expected = contract_text()
    for locale in ('es_es', 'en_us'):
        path = ROOT / f'src/main/resources/assets/unallocated/lang/{locale}.json'
        data = json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}
        actual = {key: value for key, value in data.items() if key.startswith(PREFIX)}
        if write:
            data = {key: value for key, value in data.items() if not key.startswith(PREFIX)}
            data.update(expected)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        elif actual != expected:
            raise ValueError(f'{path.relative_to(ROOT)} differs; run python3 scripts/contract_text.py --write')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--write', action='store_true')
    mode.add_argument('--check', action='store_true')
    args = parser.parse_args()
    synchronize(args.write)
