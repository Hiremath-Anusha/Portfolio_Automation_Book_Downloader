# Automated Web Book Downloader & PDF Compiler

An automated Java utility that authenticates into online web portals, captures pages from dynamic web viewers (such as Adobe InDesign readers), dynamically crops out dark margins using custom computer vision pixel analysis, and compiles clean pages into a single PDF.

---

## Key Features

- **Automated Authentication:** Logs into web portals securely using Selenium WebDriver.
- **Dynamic Pixel-Based Auto-Cropping:** Uses Java `BufferedImage` RGB color scanning to dynamically detect page boundaries (handling left, right, and dual-page spread layouts) without relying on fragile DOM/Shadow DOM selectors.
- **PDF Compilation:** Utilizes Apache PDFBox to stitch captured high-resolution pages into a formatted PDF document.
- **Clean Environment Cleanup:** Automatically purges temporary image files after PDF generation.

---

## Tech Stack

- **Language:** Java 11+
- **Browser Automation:** Selenium WebDriver (Chrome)
- **PDF Generation:** Apache PDFBox
- **Image Processing:** Java AWT (`BufferedImage`, `ImageIO`)

---

## AI Collaboration & Acknowledgments

This project was built through an iterative development process in collaboration with **Google Gemini**. Gemini provided technical support for troubleshooting DOM shadow root encapsulation, refining keyboard navigation handling, and designing the RGB pixel-boundary detection algorithm to isolate shifting page alignments cleanly.

---
