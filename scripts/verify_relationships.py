import re
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REL = ROOT / 'docs' / 'uml' / 'relationships.txt'
SRC = ROOT / 'core' / 'src' / 'main' / 'java'

rel_lines = []
with REL.open(encoding='utf-8') as f:
    started = False
    for ln in f:
        if ln.strip().startswith('Relationships (project-internal):'):
            started = True
            continue
        if not started:
            continue
        s = ln.strip()
        if not s or s.startswith('#'):
            continue
        rel_lines.append(s)

java_files = [p for p in SRC.rglob('*.java')]
java_text = {str(p): p.read_text(encoding='utf-8', errors='ignore') for p in java_files}

results = []

for r in rel_lines:
    # pattern: Left <arrow> Right : optional desc
    m = re.match(r"([\w\.]+)\s+([-.|o*>]+)\s+([\w\.]+)\s*(?::\s*(.*))?", r)
    if not m:
        results.append((r, 'PARSE_FAIL', 'Could not parse line'))
        continue
    left, arrow, right, desc = m.group(1), m.group(2), m.group(3), m.group(4)
    evidence = []
    # find files containing left class
    left_simple = left.split('.')[-1]
    right_simple = right.split('.')[-1]
    left_files = [p for p,t in java_text.items() if re.search(rf"\b(class|interface|enum)\s+{re.escape(left_simple)}\b", t)]
    # if none, try whole project search
    if not left_files:
        left_files = [p for p,t in java_text.items() if re.search(rf"\b{re.escape(left_simple)}\b", t)]
    # checks per arrow
    ok = False
    notes = []
    if '--|>' in arrow:  # inheritance
        for p in left_files:
            t = java_text[p]
            if re.search(rf"\b(class|enum)\s+{re.escape(left_simple)}\b[^\n]*\bextends\s+{re.escape(right_simple)}\b", t):
                ok = True; evidence.append(f'extends found in {p}')
            if re.search(rf"\binterface\s+{re.escape(left_simple)}\b[^\n]*\bextends\s+{re.escape(right_simple)}\b", t):
                ok = True; evidence.append(f'interface extends found in {p}')
    if '..|>' in arrow:  # implements
        for p in left_files:
            t = java_text[p]
            if re.search(rf"\bclass\s+{re.escape(left_simple)}\b[^\n]*\bimplements\s+[^\n]*\b{re.escape(right_simple)}\b", t):
                ok = True; evidence.append(f'implements found in {p}')
            if re.search(rf"\b{re.escape(left_simple)}\b[^\n]*\bimplements\b", t):
                # further verify right present
                if re.search(rf"\bimplements\b[^\n]*\b{re.escape(right_simple)}\b", t):
                    ok = True; evidence.append(f'implements found in {p}')
    if 'o--' in arrow or '*--' in arrow or '-->' in arrow or '..>' in arrow:
        # search left files for field declarations or constructor new or method params or usage
        for p in left_files:
            t = java_text[p]
            # field of type Right
            if re.search(rf"\b{re.escape(right_simple)}\b\s+\w+\s*[=;)]", t) or re.search(rf"\b{re.escape(right_simple)}\b\s*\[", t):
                ok = True; evidence.append(f'field or local var of {right_simple} in {p}')
            # new Right(
            if re.search(rf"new\s+{re.escape(right_simple)}\s*\(", t):
                ok = True; evidence.append(f'instantiation of {right_simple} in {p}')
            # method param
            if re.search(rf"\b{re.escape(right_simple)}\b\s+\w+\s*[),]", t):
                ok = True; evidence.append(f'method parameter of {right_simple} in {p}')
            # instanceof usage
            if re.search(rf"instanceof\s+{re.escape(right_simple)}\b", t):
                ok = True; evidence.append(f'instanceof {right_simple} in {p}')
    results.append((r, 'OK' if ok else 'MISSING', evidence))

out = ROOT / 'scripts' / 'verification_report.txt'
with out.open('w', encoding='utf-8') as f:
    for r,status,e in results:
        f.write(f'{status}: {r}\n')
        if e:
            if isinstance(e, list):
                for ev in e:
                    f.write(f'    - {ev}\n')
            else:
                f.write(f'    - {e}\n')

print('Verification complete. Report at:', out)
