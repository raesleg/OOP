import re
from pathlib import Path

root = Path(r"d:\Year 1\Sem 2\OOP\Libgdk Projects\Project Work\OOP")
search_dir = root / "core" / "src" / "main" / "java" / "io" / "github" / "raesleg"

java_files = list(search_dir.rglob('*.java'))
java_files.sort()

# collect class definitions
class_info = {}  # classname -> {pkg, extends, implements, fields:set(types), methods:param types set}

class_pattern = re.compile(r"\b(class|interface|enum)\s+(\w+)(?:\s+extends\s+([\w<>.]+))?(?:\s+implements\s+([\w<>,.\s]+))?")
field_pattern = re.compile(r"\b(public|protected|private)\s+(static\s+)?([\w<>\[\]?\.]+)\s+([A-Za-z0-9_]+)\s*(=|;)" )
method_pattern = re.compile(r"\b(public|protected|private)\s+(static\s+)?([\w<>\[\]?\.]+)\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)")

for f in java_files:
    src = f.read_text(encoding='utf-8')
    m_pkg = re.search(r"^\s*package\s+([\w\.]+)\s*;", src, re.MULTILINE)
    pkg = m_pkg.group(1) if m_pkg else ''
    for m in class_pattern.finditer(src):
        kind, name, extends, impls = m.groups()
        extends = extends if extends else None
        impls = [s.strip() for s in impls.split(',')] if impls else []
        class_info[name] = {'pkg':pkg, 'extends':extends, 'implements':impls, 'fields':set(), 'methods':set()}
    # fields
    for m in field_pattern.finditer(src):
        typ = m.group(3)
        name = m.group(4)
        # normalize type to main token
        main_type = re.sub(r"<.*>", "", typ).strip().split('.')[-1]
        # find class owning this by naive: look for nearest class declaration earlier
        # simplified: add to all classes in file
        for cls in [k for k in class_info.keys() if class_info[k]['pkg']==pkg or True]:
            pass
        # We'll add fields by file's primary class
    # determine primary class from filename
    primary = f.stem
    for m in field_pattern.finditer(src):
        typ = m.group(3)
        main_type = re.sub(r"<.*>", "", typ).strip().split('.')[-1]
        if primary in class_info:
            class_info[primary]['fields'].add(main_type)
    # methods param types
    for m in method_pattern.finditer(src):
        params = m.group(5)
        if not params.strip():
            continue
        parts = [p.strip() for p in params.split(',') if p.strip()]
        for p in parts:
            toks = p.split()
            if len(toks)>=2:
                ptype = re.sub(r"<.*>", "", ' '.join(toks[:-1])).split('.')[-1]
                if primary in class_info:
                    class_info[primary]['methods'].add(ptype)

# build relationships list
rels = []
legend = {
    'inheritance':'--|>',
    'implementation':'..|>',
    'composition':'o--',
    'aggregation':'--o',
    'association':'-->',
    'dependency':'..>'
}

for cls, info in class_info.items():
    if info['extends']:
        et = info['extends'].split('.')[-1]
        rels.append((cls, legend['inheritance'], et, 'extends'))
    for impl in info['implements']:
        imp = impl.split('.')[-1]
        rels.append((cls, legend['implementation'], imp, 'implements'))
    for ftype in sorted(info['fields']):
        if ftype and ftype not in ['int','float','boolean','double','long','String','List','Map','Set','SpriteBatch','Vector2','Texture']:
            rels.append((cls, legend['association'], ftype, 'field'))
    for mtype in sorted(info['methods']):
        if mtype and mtype not in ['int','float','boolean','double','long','String','List','Map','Set','SpriteBatch','Vector2','Texture']:
            rels.append((cls, legend['dependency'], mtype, 'method param'))

out = []
out.append('Legend:')
for k,v in legend.items():
    out.append(f"- {k}: {v}")
out.append('')
out.append('Relationships (inferred):')
for a,arrow,b,why in rels:
    out.append(f"{a} {arrow} {b} : {why}")

out_path = root / 'docs' / 'uml' / 'relationships.txt'
out_path.parent.mkdir(parents=True, exist_ok=True)
with out_path.open('w', encoding='utf-8') as fh:
    fh.write('\n'.join(out))

print('WROTE', out_path)
