#!/usr/bin/env python3
"""Give every web asset URL a new ?v= number. Run before each web deploy.

Cloudflare stretches the browser cache for static files to 4 hours, so browsers keep old JS/CSS/icons
unless their URLs change. index.html is never cached, so new ?v= URLs reach everyone immediately.
Usage: python web/bump-version.py
"""
import glob, os, re

os.chdir(os.path.dirname(os.path.abspath(__file__)))
html = open('index.html', encoding='utf-8').read()
cur = int((re.search(r'js/main\.js\?v=(\d+)', html) or [None, '0'])[1])
V = str(cur + 1)

def rewrite(path, fn):
    s = open(path, encoding='utf-8').read()
    n = fn(s)
    if n != s:
        open(path, 'w', encoding='utf-8', newline='').write(n)

for f in glob.glob('js/*.js'):
    rewrite(f, lambda s: re.sub(r"""(from\s+'|import\(')\./([a-z]+)\.js(?:\?v=\d+)?'""", lambda m: f"{m.group(1)}./{m.group(2)}.js?v={V}'", s))
rewrite('index.html', lambda s: re.sub(r'(href|src)="((?:js/)?[a-z0-9-]+\.(?:css|js|svg|png|webmanifest))(?:\?v=\d+)?"', lambda m: f'{m.group(1)}="{m.group(2)}?v={V}"', s))
rewrite('styles.css', lambda s: re.sub(r"url\('([a-z0-9-]+\.(?:svg|png))(?:\?v=\d+)?'\)", lambda m: f"url('{m.group(1)}?v={V}')", s))
rewrite('manifest.webmanifest', lambda s: re.sub(r'"src": "([a-z0-9-]+\.(?:svg|png))(?:\?v=\d+)?"', lambda m: f'"src": "{m.group(1)}?v={V}"', s))
rewrite('sw.js', lambda s: re.sub(r"ff-shell-v\d+", f"ff-shell-v{V}", s))
print(f'web assets now at ?v={V}')
