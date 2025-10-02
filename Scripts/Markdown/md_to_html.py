#!/usr/bin/env python3
"""
Converts markdown to HTML for printing to PDF.
"""

import sys
import re

def markdown_to_html(md_content):
    """Convert markdown to HTML."""
    html = []
    html.append('''<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Yuno Popup Flow System - Developer Guide</title>
    <style>
        body {
            font-family: 'Arial', sans-serif;
            line-height: 1.6;
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            color: #333;
        }
        h1 {
            color: #1a1a1a;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
            margin-top: 30px;
        }
        h2 {
            color: #2a2a2a;
            border-bottom: 2px solid #ddd;
            padding-bottom: 8px;
            margin-top: 25px;
        }
        h3 {
            color: #3a3a3a;
            margin-top: 20px;
        }
        h4 {
            color: #4a4a4a;
            margin-top: 15px;
        }
        code {
            background-color: #f4f4f4;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
        }
        pre {
            background-color: #f4f4f4;
            border: 1px solid #ddd;
            border-left: 3px solid #4CAF50;
            padding: 10px;
            overflow-x: auto;
            border-radius: 3px;
        }
        pre code {
            background-color: transparent;
            padding: 0;
        }
        table {
            border-collapse: collapse;
            width: 100%;
            margin: 15px 0;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #4CAF50;
            color: white;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        ul, ol {
            margin: 10px 0;
        }
        li {
            margin: 5px 0;
        }
        blockquote {
            border-left: 4px solid #4CAF50;
            padding-left: 15px;
            margin: 15px 0;
            color: #666;
        }
        .page-break {
            page-break-after: always;
        }
        @media print {
            body {
                max-width: 100%;
            }
            h1, h2, h3 {
                page-break-after: avoid;
            }
            pre {
                page-break-inside: avoid;
            }
        }
    </style>
</head>
<body>
''')

    lines = md_content.split('\n')
    in_code_block = False
    code_lines = []
    in_table = False
    table_lines = []

    for i, line in enumerate(lines):
        # Handle code blocks
        if line.startswith('```'):
            if in_code_block:
                # End code block
                html.append('<pre><code>')
                html.append('\n'.join(code_lines))
                html.append('</code></pre>')
                code_lines = []
                in_code_block = False
            else:
                in_code_block = True
            continue

        if in_code_block:
            # Escape HTML in code
            escaped = line.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            code_lines.append(escaped)
            continue

        # Handle tables
        if line.startswith('| '):
            if not in_table:
                in_table = True
                table_lines = []
            table_lines.append(line)
            # Check if next line is not a table line
            if i + 1 >= len(lines) or not lines[i + 1].startswith('| '):
                # End of table
                html.append(process_table(table_lines))
                in_table = False
                table_lines = []
            continue

        # Handle horizontal rules
        if line.strip() in ['---', '___', '***']:
            html.append('<hr>')
            continue

        # Handle headings
        if line.startswith('# '):
            html.append(f'<h1>{escape_html(line[2:])}</h1>')
        elif line.startswith('## '):
            html.append(f'<h2>{escape_html(line[3:])}</h2>')
        elif line.startswith('### '):
            html.append(f'<h3>{escape_html(line[4:])}</h3>')
        elif line.startswith('#### '):
            html.append(f'<h4>{escape_html(line[5:])}</h4>')
        # Handle lists
        elif line.startswith('- ') or line.startswith('* '):
            html.append(f'<ul><li>{process_inline(line[2:])}</li></ul>')
        elif re.match(r'^\d+\. ', line):
            match = re.match(r'^(\d+)\. (.+)$', line)
            if match:
                html.append(f'<ol><li>{process_inline(match.group(2))}</li></ol>')
        # Handle empty lines
        elif line.strip() == '':
            html.append('<p></p>')
        # Regular paragraphs
        else:
            if line.strip():
                html.append(f'<p>{process_inline(line)}</p>')

    html.append('</body></html>')
    return '\n'.join(html)

def process_table(table_lines):
    """Convert markdown table to HTML."""
    if len(table_lines) < 2:
        return ''

    html = ['<table>']

    # Header row
    header = table_lines[0].split('|')[1:-1]  # Remove first and last empty elements
    html.append('<thead><tr>')
    for cell in header:
        html.append(f'<th>{cell.strip()}</th>')
    html.append('</tr></thead>')

    # Skip separator line (index 1)
    # Data rows
    if len(table_lines) > 2:
        html.append('<tbody>')
        for line in table_lines[2:]:
            cells = line.split('|')[1:-1]
            html.append('<tr>')
            for cell in cells:
                html.append(f'<td>{process_inline(cell.strip())}</td>')
            html.append('</tr>')
        html.append('</tbody>')

    html.append('</table>')
    return '\n'.join(html)

def process_inline(text):
    """Process inline markdown formatting."""
    # Bold
    text = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', text)
    # Italic
    text = re.sub(r'\*(.+?)\*', r'<em>\1</em>', text)
    # Inline code
    text = re.sub(r'`(.+?)`', r'<code>\1</code>', text)
    # Links
    text = re.sub(r'\[(.+?)\]\((.+?)\)', r'<a href="\2">\1</a>', text)
    return text

def escape_html(text):
    """Escape HTML special characters."""
    return text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python3 md_to_html.py input.md output.html")
        sys.exit(1)

    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        md_content = f.read()

    html_content = markdown_to_html(md_content)

    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        f.write(html_content)

    print(f"HTML created: {sys.argv[2]}")
    print("To convert to PDF, use: wkhtmltopdf output.html output.pdf")
    print("Or open in a browser and print to PDF")
