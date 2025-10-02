#!/usr/bin/env python3
"""
Converts markdown to PDF using markdown and reportlab.
"""

import sys
import markdown
from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Preformatted
from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_JUSTIFY
from reportlab.pdfgen import canvas
from html.parser import HTMLParser
import re

class MarkdownToPDF:
    def __init__(self, input_file, output_file):
        self.input_file = input_file
        self.output_file = output_file
        self.story = []

        # Create PDF document
        self.doc = SimpleDocTemplate(
            output_file,
            pagesize=letter,
            rightMargin=72,
            leftMargin=72,
            topMargin=72,
            bottomMargin=72
        )

        # Define styles
        self.styles = getSampleStyleSheet()
        self.styles.add(ParagraphStyle(
            name='CodeBlock',
            parent=self.styles['Code'],
            fontSize=8,
            leftIndent=20,
            rightIndent=20,
            spaceAfter=10,
            spaceBefore=10
        ))
        self.styles.add(ParagraphStyle(
            name='Heading1Custom',
            parent=self.styles['Heading1'],
            fontSize=18,
            spaceAfter=12,
            spaceBefore=12,
            textColor='#1a1a1a'
        ))
        self.styles.add(ParagraphStyle(
            name='Heading2Custom',
            parent=self.styles['Heading2'],
            fontSize=14,
            spaceAfter=10,
            spaceBefore=10,
            textColor='#2a2a2a'
        ))
        self.styles.add(ParagraphStyle(
            name='Heading3Custom',
            parent=self.styles['Heading3'],
            fontSize=12,
            spaceAfter=8,
            spaceBefore=8,
            textColor='#3a3a3a'
        ))

    def convert(self):
        """Convert markdown file to PDF."""
        # Read markdown content
        with open(self.input_file, 'r', encoding='utf-8') as f:
            md_content = f.read()

        # Parse markdown to simple structure
        lines = md_content.split('\n')
        in_code_block = False
        code_lines = []

        for line in lines:
            # Handle code blocks
            if line.startswith('```'):
                if in_code_block:
                    # End of code block
                    if code_lines:
                        code_text = '\n'.join(code_lines)
                        # Escape special characters for reportlab
                        code_text = code_text.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
                        self.story.append(Preformatted(code_text, self.styles['CodeBlock']))
                        self.story.append(Spacer(1, 0.2*inch))
                        code_lines = []
                    in_code_block = False
                else:
                    in_code_block = True
                continue

            if in_code_block:
                code_lines.append(line)
                continue

            # Handle headings
            if line.startswith('# '):
                text = line[2:].strip()
                self.story.append(Paragraph(text, self.styles['Heading1Custom']))
                self.story.append(Spacer(1, 0.2*inch))
            elif line.startswith('## '):
                text = line[3:].strip()
                self.story.append(Paragraph(text, self.styles['Heading2Custom']))
                self.story.append(Spacer(1, 0.15*inch))
            elif line.startswith('### '):
                text = line[4:].strip()
                self.story.append(Paragraph(text, self.styles['Heading3Custom']))
                self.story.append(Spacer(1, 0.1*inch))
            elif line.startswith('---'):
                self.story.append(Spacer(1, 0.3*inch))
            elif line.strip() == '':
                self.story.append(Spacer(1, 0.1*inch))
            elif line.startswith('- ') or line.startswith('* '):
                text = '• ' + line[2:].strip()
                text = self._escape_html(text)
                self.story.append(Paragraph(text, self.styles['Normal']))
            elif line.startswith('| '):
                # Simple table handling - just format as text
                text = self._escape_html(line)
                self.story.append(Paragraph(text, self.styles['Normal']))
            else:
                # Regular paragraph
                if line.strip():
                    text = self._escape_html(line)
                    self.story.append(Paragraph(text, self.styles['Normal']))

        # Build PDF
        self.doc.build(self.story)
        print(f"PDF created: {self.output_file}")

    def _escape_html(self, text):
        """Escape HTML special characters but preserve some markdown formatting."""
        # Handle inline code
        text = re.sub(r'`([^`]+)`', r'<font name="Courier" size="9">\1</font>', text)

        # Handle bold
        text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)

        # Handle italic
        text = re.sub(r'\*([^*]+)\*', r'<i>\1</i>', text)

        # Handle links - just show text
        text = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', text)

        # Escape remaining special characters
        text = text.replace('&', '&amp;')
        text = text.replace('<', '&lt;').replace('>', '&gt;')

        # Restore our formatted tags
        text = text.replace('&lt;b&gt;', '<b>').replace('&lt;/b&gt;', '</b>')
        text = text.replace('&lt;i&gt;', '<i>').replace('&lt;/i&gt;', '</i>')
        text = text.replace('&lt;font', '<font').replace('&lt;/font&gt;', '</font>')

        return text

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python3 md_to_pdf.py input.md output.pdf")
        sys.exit(1)

    converter = MarkdownToPDF(sys.argv[1], sys.argv[2])
    converter.convert()
