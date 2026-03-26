import re
from pathlib import Path

root = Path(r"d:\Year 1\Sem 2\OOP\Libgdk Projects\Project Work\OOP")
search_dir = root / "core" / "src" / "main" / "java" / "io" / "github" / "raesleg" / "engine"

out_lines = []

java_files = list(search_dir.rglob('*.java'))
java_files.sort()

class_pattern = re.compile(r"\b(public|protected|private)?\s*(?:final|abstract|static)?\s*(class|interface|enum)\s+(\w+)(?:\s+extends\s+(\w+))?(?:\s+implements\s+([\w,\s]+))?")
field_pattern = re.compile(r"\b(public|protected|private)\s+(static\s+)?([\w<>\[\]?\.]+)\s+([A-Za-z0-9_]+)\s*(=|;)" )
method_pattern = re.compile(r"\b(public|protected|private)\s+(static\s+)?([\w<>\[\]?\.]+)\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*(?:throws[^{]+)?\{?")
ctor_pattern = re.compile(r"\b(public|protected|private)?\s*([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*\{")

def clean_type(t):
    return t.strip().replace('\n',' ').replace('  ',' ')

for f in java_files:
    with f.open('r', encoding='utf-8') as fh:
        src = fh.read()
    m_pkg = re.search(r"^\s*package\s+([\w\.]+)\s*;", src, re.MULTILINE)
    pkg = m_pkg.group(1) if m_pkg else 'engine'
    mcls = class_pattern.search(src)
    if not mcls:
        clsname = f.stem
        cls_kind = 'class'
    else:
        cls_kind = mcls.group(2)
        clsname = mcls.group(3)
    out_lines.append(f"// Package: {pkg}")
    out_lines.append(f"class {clsname} {{")
    # fields
    src_no_strings = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    src_no_bodies = re.sub(r"\{[^{}]*\}", "{ }", src_no_strings, flags=re.DOTALL)
    fields = field_pattern.findall(src_no_bodies)
    seen_fields = set()
    for vis, st, typ, name, _ in fields:
        if name in seen_fields:
            continue
        seen_fields.add(name)
        out_lines.append(f"- {name}:{clean_type(typ)}")
    # methods
    methods = method_pattern.findall(src)
    seen_methods = set()
    for vis, st, rettype, name, params in methods:
        params = params.strip()
        param_list = []
        if params:
            parts = [p.strip() for p in params.split(',') if p.strip()]
            for p in parts:
                toks = p.split()
                if len(toks) >=2:
                    ptype = ' '.join(toks[:-1])
                    pname = toks[-1]
                    param_list.append(f"{pname}:{ptype}")
                else:
                    param_list.append(f"arg:{toks[0]}")
        param_str = ', '.join(param_list)
        sig = f"{name}({param_str}): {clean_type(rettype)}"
        if sig in seen_methods:
            continue
        seen_methods.add(sig)
        out_lines.append(f"+ {sig}")
    # constructors
    ctor_pattern_local = re.compile(r"\b(public|protected|private)?\s*" + re.escape(clsname) + r"\s*\(([^)]*)\)\s*\{")
    ctors = ctor_pattern_local.findall(src)
    for vis, params in ctors:
        params = params.strip()
        param_list = []
        if params:
            parts = [p.strip() for p in params.split(',') if p.strip()]
            for p in parts:
                toks = p.split()
                if len(toks) >=2:
                    ptype = ' '.join(toks[:-1])
                    pname = toks[-1]
                    param_list.append(f"{pname}:{ptype}")
                else:
                    param_list.append(f"arg:{toks[0]}")
        param_str = ', '.join(param_list)
        sig = f"{clsname}({param_str}): void"
        if sig not in seen_methods:
            out_lines.append(f"+ {sig}")
            seen_methods.add(sig)
    out_lines.append('}')
    out_lines.append('')

out_path = root / 'docs' / 'uml' / 'engine_layer_full.txt'
out_path.parent.mkdir(parents=True, exist_ok=True)
with out_path.open('w', encoding='utf-8') as outfh:
    outfh.write('\n'.join(out_lines))

print('WROTE', out_path)
