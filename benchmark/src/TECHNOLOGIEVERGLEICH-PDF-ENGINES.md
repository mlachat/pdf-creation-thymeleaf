# Technologievergleich PDF-Engines

> **Bewertung von JasperReports, OpenHTMLtoPDF und Flying Saucer + OpenPDF**
> für die Migration eines bestehenden Schreibgenerators (7 Vorlagen, max. 3 Seiten)

| | |
|---|---|
| **Datum** | 09.03.2026 |
| **Kontext** | Bestehendes Projekt mit JasperReports, Evaluierung einer Migration |
| **Umfang** | 7 Briefvorlagen (Rechnungen, Anschreiben, Bescheide), je max. 3 Seiten |
| **Laufzeitanforderung** | Unkritisch — Nachtverarbeitung mit ausreichend Zeitfenster |
| **Java-Version** | 21 |
| **Framework** | Spring Boot 4.x |

---

## Inhaltsverzeichnis

1. [Ausgangslage](#1--ausgangslage)
2. [Die drei Kandidaten im Überblick](#2--die-drei-kandidaten-im-überblick)
3. [Detailbewertung nach Kategorien](#3--detailbewertung-nach-kategorien)
4. [Punktevergabe und Gewichtung](#4--punktevergabe-und-gewichtung)
5. [Fazit und Empfehlung](#5--fazit-und-empfehlung)

---

## 1 · Ausgangslage

Das bestehende System erzeugt 7 verschiedene Schreiben (Rechnungen, Anschreiben, Bescheide u. a.)
auf Basis von **JasperReports 6.17.0**. Die Vorlagen sind als JRXML-Dateien implementiert und werden
im Nachtlauf mit Daten befüllt und als PDF exportiert.

**Anlass der Evaluierung:**

- JasperReports 6.17.0 ist stark veraltet (Release 2021)
- Migration auf 7.x erfordert Breaking Changes (neues Binärformat, Package-Umbenennungen)
- Kein zertifizierter Java-21-Support
- Hohe Sicherheitslücke CVE-2025-10492 (CVSS 8.7)
- Hoher Wartungsaufwand durch JRXML-Spezialwissen und Jaspersoft Studio

Die Bewertung prüft, ob ein **Technologiewechsel** zu einer HTML/CSS-basierten Engine
wirtschaftlich und technisch sinnvoll ist.

---

## 2 · Die drei Kandidaten im Überblick

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   JasperReports     │    │   OpenHTMLtoPDF      │    │   Flying Saucer     │
│   ─────────────     │    │   ─────────────      │    │   + OpenPDF         │
│                     │    │                      │    │   ─────────────     │
│   JRXML → compile   │    │   HTML/CSS → PDF     │    │   HTML/CSS → PDF    │
│   → fill → export   │    │   via PDFBox 3.x     │    │   via OpenPDF       │
│                     │    │                      │    │                     │
│   ▸ Proprietär      │    │   ▸ Webstandard      │    │   ▸ Webstandard     │
│   ▸ Pixel-Layout    │    │   ▸ CSS 2.1          │    │   ▸ CSS 2.1 + CSS3  │
│   ▸ LGPL-2.1+       │    │   ▸ LGPL-2.1+        │    │   ▸ LGPL-2.1+/MPL   │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
```

| Eigenschaft | JasperReports | OpenHTMLtoPDF | Flying Saucer + OpenPDF |
|:---|:---:|:---:|:---:|
| **Aktuelle Version** | 7.0.4 (Mär 2026) | 1.1.37 (Jan 2026) | 10.0.0 (Sep 2025) |
| **Eingesetzte Version** | 6.17.0 ⚠️ | — | — |
| **Lizenz** | LGPL-2.1+ | LGPL-2.1+ | LGPL-2.1+ / MPL |
| **Template-Format** | JRXML (proprietär) | HTML + CSS | HTML + CSS |
| **PDF-Backend** | Eingebaut | Apache PDFBox 3.x | OpenPDF (iText-4-Fork) |
| **Java-21-Support** | ⚠️ nicht zertifiziert | ⚠️ offenes Issue | ✅ ab v10.0.0 |
| **Maintainer** | TIBCO (kommerziell) | Community Fork | Community |

---

## 3 · Detailbewertung nach Kategorien

### 3.1 · Architektur, Technologie & Integrationsfähigkeit

```
─────────────────────────────────────────────────────────────
```

#### JasperReports

- **Pipeline:** JRXML → Kompilierung → `.jasper`-Binärdatei → Fill mit Daten → PDF-Export
- **Template-Design:** Erfordert Jaspersoft Studio oder tiefes JRXML-Wissen
- **Positionierung:** Absolute Pixel-Koordinaten (x, y, width, height)
- **Spring Boot:** Kein offizieller Starter vorhanden — manuelle Bean-Konfiguration nötig
- **Template Engine:** Keine separate — Logik ist in JRXML eingebettet (PrintWhenExpression, Java-Expressions)
- **Kopplung:** Template = Rendering = Engine (monolithisch)

#### OpenHTMLtoPDF

- **Pipeline:** Template Engine (FreeMarker/Thymeleaf) → HTML-String → PDF via PDFBox
- **Template-Design:** Standard-XHTML mit CSS — jeder Texteditor genügt
- **Positionierung:** CSS-basiertes Fließlayout mit `@page`-Rules für Drucklayout
- **Spring Boot:** Triviale Integration — passt natürlich in die Web-Architektur
- **Template Engine:** Frei wählbar (FreeMarker, Thymeleaf, Mustache…)
- **Kopplung:** Saubere Trennung von Rendering und PDF-Erzeugung

#### Flying Saucer + OpenPDF

- **Pipeline:** Identisch zu OpenHTMLtoPDF — HTML/CSS → PDF
- **Template-Design:** Standard-XHTML mit CSS
- **Besonderheit:** Einige CSS3-Erweiterungen (Named Pages, Running Elements, Margin Boxes)
- **Spring Boot:** Ebenso trivial integrierbar
- **Template Engine:** Frei wählbar
- **Kopplung:** Saubere Trennung wie OpenHTMLtoPDF

> **Bewertung:** HTML/CSS-basierte Engines fügen sich nahtlos in moderne Java-Architekturen
> ein. JasperReports erfordert eine Sonderwelt mit eigenem Tooling und eigener Denkweise.

```
─────────────────────────────────────────────────────────────
```

### 3.2 · Funktionalität für selten geänderte Vorlagen

Bei 7 Vorlagen, die sich selten ändern, steht die **Wartbarkeit der Templates** im Vordergrund.

| Kriterium | JasperReports | OpenHTMLtoPDF | Flying Saucer |
|:---|:---|:---|:---|
| Vorlage bearbeiten | Jaspersoft Studio oder JRXML-Expertise | Texteditor | Texteditor |
| Vorschau | Nur in Jaspersoft Studio | Browser | Browser |
| Versionierung (Diff) | XML, aber komplex (~200 Zeilen für einen Brief) | HTML — kompakt, lesbar | HTML — kompakt, lesbar |
| Bedingte Abschnitte | `<printWhenExpression>` (Java-Expression) | `<#if>` / `th:if` | `<#if>` / `th:if` |
| Seitenumbruch | Band-basiert (automatisch) | `page-break-before/after` (CSS) | `page-break-before/after` (CSS) |
| Tabellen & Listen | JRXML-Subreports | `<table>` + `<#list>` | `<table>` + `<#list>` |
| QR-Codes / Bilder | Base64-Decode in Expression | Base64 Data-URI im HTML | Base64 Data-URI im HTML |
| Schriftarten | `fonts.xml` + Font-Extension | Font-Datei einbetten (TTF) | Font-Datei einbetten (TTF) |

> **Bewertung:** Für 7 stabile Briefvorlagen bieten HTML/CSS-Templates erhebliche
> Vorteile bei Lesbarkeit, Bearbeitbarkeit und Vorschau. Die Stärken von JasperReports
> (Subreports, Charts, Crosstabs) werden bei einfachen Schreiben nicht benötigt.

```
─────────────────────────────────────────────────────────────
```

### 3.3 · Performance

> **Hinweis:** Die Laufzeit ist in diesem Szenario unkritisch (Nachtverarbeitung).
> Die Bewertung erfolgt dennoch der Vollständigkeit halber.

| Kriterium | JasperReports | OpenHTMLtoPDF | Flying Saucer |
|:---|:---|:---|:---|
| Kaltstart | ⚠️ Schwer (Kompilierung + Fonts) | Moderat | Moderat |
| Durchsatz (warmgelaufen) | Gut (compile-once/fill-many) | Gut (Fast Renderer) | Moderat (GC-Druck) |
| Speicher pro Dokument | ⚠️ Heavyweight-Objektmodell | Moderat (DOM + Layout) | Moderat (DOM + Layout) |
| Speicher bei 3 Seiten | Unkritisch bei allen dreien | Unkritisch | Unkritisch |
| Batch-Optimierung | Virtualizer (Swap to Disk) | Standard | Virtual Threads (OpenPDF 3.0) |

> **Bewertung:** Bei 7 Vorlagen mit max. 3 Seiten und Nachtlauf ist Performance kein
> Differenzierungsmerkmal. Alle drei Engines bewältigen dieses Volumen problemlos.

```
─────────────────────────────────────────────────────────────
```

### 3.4 · Lizenz & Wartbarkeit

| Kriterium | JasperReports | OpenHTMLtoPDF | Flying Saucer |
|:---|:---|:---|:---|
| **Lizenz** | LGPL-2.1+ | LGPL-2.1+ | LGPL-2.1+ / MPL |
| **Lizenzrisiko** | Gering | Gering | Gering |
| **Release-Kadenz** | Selten (jährlich) | Sehr aktiv (2–3 Wochen) | Quartalsweise |
| **Letztes Release** | 7.0.4 (Mär 2026) | 1.1.37 (Jan 2026) | 10.0.0 (Sep 2025) |
| **Bekannte CVEs** | ❌ CVE-2025-10492 (HIGH) | ✅ Keine bekannten | ✅ Keine bekannten |
| **Migration aktuell → neu** | ⚠️ Breaking (6.x → 7.x) | Nicht nötig | Smooth (versionierte Branches) |
| **GitHub-Community** | ~1.244 Stars | Aktiver Community Fork | ~2.200 Stars |
| **Maintainer-Typ** | Kommerziell (TIBCO) | Freiwillige | Freiwillige |

> **Bewertung:** JasperReports hat ein offenes Sicherheitsproblem und erfordert eine
> aufwändige Major-Migration. Die HTML-Engines sind sicherheitstechnisch unauffällig
> und werden aktiver gepflegt.

```
─────────────────────────────────────────────────────────────
```

### 3.5 · Betrieb (Fehleranalyse, Speicher, Monitoring)

| Kriterium | JasperReports | OpenHTMLtoPDF | Flying Saucer |
|:---|:---|:---|:---|
| **Debugging** | ❌ Schwierig — `.jasper` binär, kryptische Fehlermeldungen | ✅ HTML/CSS im Browser prüfbar | ✅ HTML/CSS im Browser prüfbar |
| **Fehleranalyse** | Round-Trip durch Jaspersoft Studio nötig | CSS-Warnungen im Log, klare Exceptions | CSS-Warnungen im Log |
| **Speicherprofil** | Heavyweight-Objektmodell | Moderat | Moderat (GC-Druck bei großen Docs) |
| **Logging** | Eingeschränkt | Gute CSS-Diagnose im Log | Gute CSS-Diagnose im Log |
| **Monitoring** | Kein Built-in | Kein Built-in | Kein Built-in |
| **Bekannte Fallstricke** | Parameter-Map wird intern mutiert | CSS-2.1-Limits beachten | CJK-Fonts brauchen explizite Config |

> **Bewertung:** Im Betrieb sind HTML/CSS-Engines deutlich transparenter.
> Template-Fehler lassen sich im Browser reproduzieren, ohne Spezial-Tooling.
> Für ein Ops-Team ohne JasperReports-Erfahrung ist das ein erheblicher Vorteil.

```
─────────────────────────────────────────────────────────────
```

### 3.6 · Zukunftssicherheit

| Kriterium | JasperReports | OpenHTMLtoPDF | Flying Saucer |
|:---|:---|:---|:---|
| **Java 21** | ⚠️ Nicht zertifiziert | ⚠️ Offenes Issue (#951) | ✅ Voll unterstützt |
| **Aktive Entwicklung** | Moderat (TIBCO-Roadmap) | Sehr aktiv | Aktiv |
| **Vendor-Risiko** | ⚠️ Moderat (Broadcom/TIBCO) | Niedrig–moderat | Niedrig |
| **Lock-in** | ❌ Hoch (JRXML proprietär) | ✅ Niedrig (HTML/CSS) | ✅ Niedrig (HTML/CSS) |
| **PDF/UA (Barrierefreiheit)** | ⚠️ Eingeschränkt | ✅ Stark (Section 508, WCAG) | ⚠️ Eingeschränkt |
| **PDF/A (Archivierung)** | ✅ Ja | ✅ Ja | ⚠️ Eingeschränkt |
| **Portabilität** | ⚠️ Nur mit JRXML-Ökosystem | ✅ Templates wiederverwendbar | ✅ Templates wiederverwendbar |

> **Bewertung:** HTML/CSS-Templates sind zukunftssicher, weil sie auf offenen Standards
> basieren. Ein Wechsel zwischen OpenHTMLtoPDF und Flying Saucer ist mit identischen
> Templates möglich — das eliminiert Vendor-Lock-in nahezu vollständig.

---

## 4 · Punktevergabe und Gewichtung

### Bewertungsskala

| Punkte | Bedeutung |
|:---:|:---|
| 5 | Hervorragend — klarer Vorteil, keine Einschränkungen |
| 4 | Gut — leichte Einschränkungen, aber praxistauglich |
| 3 | Befriedigend — funktional, aber mit spürbaren Nachteilen |
| 2 | Ausreichend — nutzbar, aber deutliche Schwächen |
| 1 | Mangelhaft — erhebliche Probleme, hoher Aufwand |

### Gewichtung der Kategorien

Die Gewichtung orientiert sich am konkreten Szenario:
7 stabile Briefvorlagen, Nachtverarbeitung, Java 21, langfristiger Betrieb.

| # | Kategorie | Gewicht | Begründung |
|:---:|:---|:---:|:---|
| A | Architektur & Integration | 15 % | Einmalige Integrationsarbeit, danach stabil |
| B | Funktionalität für Vorlagen | 25 % | Kernaufgabe — Templates müssen wartbar sein |
| C | Performance | 5 % | Unkritisch bei Nachtverarbeitung und kleinem Volumen |
| D | Lizenz & Wartbarkeit | 20 % | Langfristiger Betrieb erfordert gepflegte Dependencies |
| E | Betrieb & Fehleranalyse | 20 % | Ops-Aufwand ist laufender Kostentreiber |
| F | Zukunftssicherheit | 15 % | Java-21-Support und Vendor-Unabhängigkeit |
| | **Summe** | **100 %** | |

### Punktetabelle

| # | Kategorie | Gew. | JasperReports | | OpenHTMLtoPDF | | Flying Saucer | |
|:---:|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| | | | Pkt | Gew. | Pkt | Gew. | Pkt | Gew. |
| A | Architektur & Integration | 15 % | 2 | 0,30 | 5 | 0,75 | 5 | 0,75 |
| B | Funktionalität Vorlagen | 25 % | 2 | 0,50 | 4 | 1,00 | 4 | 1,00 |
| C | Performance | 5 % | 4 | 0,20 | 4 | 0,20 | 3 | 0,15 |
| D | Lizenz & Wartbarkeit | 20 % | 2 | 0,40 | 5 | 1,00 | 4 | 0,80 |
| E | Betrieb & Fehleranalyse | 20 % | 2 | 0,40 | 5 | 1,00 | 4 | 0,80 |
| F | Zukunftssicherheit | 15 % | 2 | 0,30 | 4 | 0,60 | 5 | 0,75 |
| | | | | | | | | |
| | **Gesamtergebnis** | | | **2,10** | | **4,55** | | **4,25** |

### Ergebnisübersicht

```
  JasperReports     ██████░░░░░░░░░░░░░░░░░░  2,10 / 5,00
  OpenHTMLtoPDF     ██████████████████████░░░  4,55 / 5,00  ◀ Empfehlung
  Flying Saucer     ████████████████████░░░░░  4,25 / 5,00
```

### Erläuterung der Einzelbewertungen

**JasperReports — 2,10 Punkte**

- *Architektur (2):* Proprietäres JRXML-Format, kein Spring-Boot-Starter, manuelle Integration, monolithische Kopplung
- *Vorlagen (2):* JRXML ist für einfache Briefe überdimensioniert, Bearbeitung erfordert Spezial-Tooling, Vorschau nur in Jaspersoft Studio
- *Performance (4):* Compile-once/fill-many ist effizient; Kaltstart schwer, aber bei Nachtlauf irrelevant
- *Wartbarkeit (2):* CVE-2025-10492 offen, Breaking Migration 6→7 nötig, seltene Releases
- *Betrieb (2):* Binäre `.jasper`-Dateien nicht inspizierbar, kryptische Fehler, Spezialwissen nötig
- *Zukunft (2):* Java 21 nicht zertifiziert, hoher Vendor-Lock-in durch JRXML, Broadcom/TIBCO-Risiko

**OpenHTMLtoPDF — 4,55 Punkte**

- *Architektur (5):* HTML/CSS-Pipeline passt perfekt in Spring Boot, freie Template-Engine-Wahl, saubere Trennung
- *Vorlagen (4):* HTML/CSS-Templates sind lesbar, diff-bar, im Browser vorschaubar; Abzug für CSS-2.1-Limit (kein Flexbox)
- *Performance (4):* Fast Renderer, moderater Speicher; für 7×3 Seiten mehr als ausreichend
- *Wartbarkeit (5):* Sehr aktive Community, Releases alle 2–3 Wochen, keine bekannten CVEs
- *Betrieb (5):* Templates im Browser debuggbar, klare Exceptions, keine Spezial-Tools nötig
- *Zukunft (4):* Aktive Entwicklung, kein Lock-in; Abzug für offenes Java-21-Issue (#951)

**Flying Saucer + OpenPDF — 4,25 Punkte**

- *Architektur (5):* Gleiche Vorteile wie OpenHTMLtoPDF — HTML/CSS-Standard, freie Template-Engine-Wahl
- *Vorlagen (4):* Identische Template-Vorteile; zusätzlich einige CSS3-Features (Named Pages, Margin Boxes)
- *Performance (3):* GC-Druck bei `collectLayers` bekannt; für 3-Seiten-Dokumente unkritisch, aber messbar schlechter
- *Wartbarkeit (4):* Quartalsweise Releases, keine CVEs; etwas weniger aktiv als OpenHTMLtoPDF
- *Betrieb (4):* Gleiche Debugging-Vorteile; Abzug für CJK-Font-Konfigurationsaufwand und kleinere Dokumentationslücken
- *Zukunft (5):* Voller Java-21-Support ab v10.0.0, klare Versionsstrategie, geringes Vendor-Risiko

---

## 5 · Fazit und Empfehlung

### Empfehlung: OpenHTMLtoPDF mit FreeMarker oder Thymeleaf

```
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║   ▸ Empfohlene Engine:   OpenHTMLtoPDF  (4,55 / 5,00 Punkte)    ║
║   ▸ Zweitplatziert:      Flying Saucer  (4,25 / 5,00 Punkte)    ║
║   ▸ Nicht empfohlen:     JasperReports  (2,10 / 5,00 Punkte)    ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝
```

### Begründung

**① Der Use Case spricht gegen JasperReports.**
Die 7 Briefvorlagen sind einfache, selten geänderte Schreiben mit max. 3 Seiten.
JasperReports' Stärken — komplexe datengetriebene Reports mit Subreports, Charts und
Crosstabs — werden nicht benötigt. Die Nachteile (JRXML-Komplexität, Tooling-Abhängigkeit,
schwieriges Debugging) werden aber in vollem Umfang mitgetragen.

**② HTML/CSS-Templates senken den Wartungsaufwand erheblich.**
Jeder Java- oder Frontend-Entwickler kann HTML/CSS-Templates bearbeiten und im Browser
vorab prüfen. JRXML erfordert dagegen Spezialwissen und Jaspersoft Studio. Bei 7 Vorlagen,
die gelegentlich angepasst werden, ist das ein entscheidender Produktivitätsfaktor.

**③ OpenHTMLtoPDF bietet die beste Kombination aus Aktivität und Stabilität.**
Mit Releases alle 2–3 Wochen ist die Community hochaktiv. Keine bekannten CVEs,
starke PDF/UA-Unterstützung und ein bewährter Fast Renderer machen es zur
zuverlässigsten Wahl. Das offene Java-21-Issue ist ein Watchitem, aber kein Blocker.

**④ Flying Saucer ist ein gleichwertiger Fallback.**
Da beide HTML-Engines identische Templates verwenden, ist ein späterer Wechsel zu
Flying Saucer mit minimalem Aufwand möglich — etwa falls der Java-21-Support bei
OpenHTMLtoPDF nicht zeitnah gelöst wird. Dieses Sicherheitsnetz eliminiert das
Migrationsrisiko nahezu vollständig.

**⑤ Die JasperReports-Migration ist ohnehin fällig.**
Unabhängig von der Engine-Wahl muss JasperReports 6.17.0 aktualisiert werden —
die offene CVE-2025-10492 (CVSS 8.7) und der fehlende Java-21-Support erzwingen das.
Die Migration auf 7.x ist dabei ebenso aufwändig wie ein Technologiewechsel,
bietet aber keinen der oben genannten Vorteile.

### Migrationsaufwand (Schätzung)

| Arbeitspaket | Aufwand |
|:---|:---|
| Engine-Integration (Maven, Spring-Beans) | Gering — wenige Klassen |
| 7 JRXML-Vorlagen → HTML/CSS konvertieren | Mittel — je Vorlage ca. ½–1 Tag |
| Font-Konfiguration (DejaVuSans o.ä.) | Gering — einmalig |
| Test-Suite aufbauen (PDF-Validierung) | Mittel — pro Vorlage Regressionstests |
| Abnahme & Vergleich mit Bestandsoutput | Mittel — visueller Abgleich je Vorlage |

### Risikominimierung

| Risiko | Maßnahme |
|:---|:---|
| CSS-2.1-Limit reicht nicht aus | Vor Migration: Probekonvertierung einer komplexen Vorlage |
| OpenHTMLtoPDF Java-21-Inkompatibilität | Fallback auf Flying Saucer 10.0.0 (gleiche Templates) |
| Optische Abweichungen nach Migration | Pixel-Vergleich alter vs. neuer PDFs pro Vorlage |
| Wissensverlust JasperReports | JRXML-Vorlagen archivieren, nicht löschen |

---

> *Erstellt am 09.03.2026 auf Basis aktueller Versionsstände und öffentlich verfügbarer Informationen.*
