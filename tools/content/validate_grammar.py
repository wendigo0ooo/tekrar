#!/usr/bin/env python3
"""Gramer içerik doğrulayıcı.

Kullanım:
  python3 tools/content/validate_grammar.py
  python3 tools/content/validate_grammar.py --strict

Kurallar: docs/gramer-kontrat.md
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_GRAMMAR = ROOT / "app/src/main/assets/grammar"
DEFAULT_COURSE = ROOT / "app/src/main/assets/course"

CEFR = {"A1", "A2", "B1", "B2"}
STEP_TYPES = {"hook", "rule", "examples", "mistake", "tip"}
ICONS = {
    "person",
    "chat",
    "tag",
    "home",
    "clock",
    "sun",
    "briefcase",
    "question",
    "book",
    "flag",
}
SOURCE_NOTE_RE = re.compile(r"^(a1-u\d{2}-l\d{2})#(\d+)$")
TOPIC_ID_RE = re.compile(r"^g-a1-[a-z0-9-]+$")
FILE_RE = re.compile(r"^a1/g\d{2}-[a-z0-9-]+\.json$")
COLOR_RE = re.compile(r"^#[0-9A-Fa-f]{6}$")
GROUP_REF_RE = re.compile(r"\$(\d+)")
EMOJI_RE = re.compile(
    "[\U0001F000-\U0001FAFF\U00002600-\U000027BF\U0000FE0F\U00002B50\U00002B55]"
)
TURKISH_CHARS_RE = re.compile("[ıİşŞğĞüÜöÖçÇ]")
WORD_RE = re.compile(r"\S+")

EXPECTED_TOPICS: list[tuple[str, str, str, list[str]]] = [
    ("g-a1-to-be", "am / is / are", "a1/g01-to-be.json", ["a1-u01-l01#0", "a1-u01-l02#0"]),
    ("g-a1-how-are-you", "How are you?", "a1/g02-how-are-you.json", ["a1-u01-l02#1"]),
    ("g-a1-names", "Ad sorma/söyleme", "a1/g03-names.json", ["a1-u01-l03#0", "a1-u02-l01#0"]),
    ("g-a1-possessives", "my / your", "a1/g04-possessives.json", ["a1-u01-l03#1", "a1-u02-l01#1"]),
    ("g-a1-farewells", "Vedalaşma", "a1/g05-farewells.json", ["a1-u01-l04#0"]),
    ("g-a1-origin", "Where are you from?", "a1/g06-origin.json", ["a1-u02-l02#0"]),
    ("g-a1-nationality", "Ülke ve milliyet", "a1/g07-nationality.json", ["a1-u02-l02#1"]),
    ("g-a1-jobs", "What do you do?", "a1/g08-jobs.json", ["a1-u02-l03#0"]),
    ("g-a1-articles", "a / an", "a1/g09-articles.json", ["a1-u02-l03#1"]),
    ("g-a1-age", "How old are you?", "a1/g10-age.json", ["a1-u02-l04#0"]),
    ("g-a1-time", "Saat sorma", "a1/g11-time.json", ["a1-u03-l01#0"]),
    ("g-a1-day-parts", "Günün bölümleri", "a1/g12-day-parts.json", ["a1-u03-l01#1", "a1-u03-l04#1"]),
    ("g-a1-present-simple", "Present simple", "a1/g13-present-simple.json", ["a1-u03-l02#0", "a1-u03-l04#0"]),
    ("g-a1-frequency", "Sıklık zarfları", "a1/g14-frequency.json", ["a1-u03-l02#1"]),
    ("g-a1-work-study", "work at / study at", "a1/g15-work-study.json", ["a1-u03-l03#0"]),
    ("g-a1-do-questions", "Do you ...?", "a1/g16-do-questions.json", ["a1-u03-l03#1"]),
]
EXPECTED_BY_ID = {item[0]: item for item in EXPECTED_TOPICS}
EXPECTED_NOTES = [note for item in EXPECTED_TOPICS for note in item[3]]

errors: list[str] = []
warnings: list[str] = []
stats: Counter[str] = Counter()
covered_notes: set[str] = set()


def err(ctx: str, msg: str) -> None:
    errors.append(f"{ctx}: {msg}")


def warn(ctx: str, msg: str) -> None:
    warnings.append(f"{ctx}: {msg}")


def _no_duplicate_keys(pairs):
    seen: set[str] = set()
    obj: dict = {}
    for key, value in pairs:
        if key in seen:
            raise ValueError(f"yinelenen anahtar '{key}'")
        seen.add(key)
        obj[key] = value
    return obj


def load_json(path: Path) -> dict | None:
    if not path.exists():
        err(str(path), "dosya yok")
        return None
    try:
        data = json.loads(
            path.read_text(encoding="utf-8"),
            object_pairs_hook=_no_duplicate_keys,
        )
    except Exception as exc:  # noqa: BLE001 - kullanıcıya net mesaj
        err(str(path), f"JSON okunamadı: {exc}")
        return None
    if not isinstance(data, dict):
        err(str(path), "kök bir JSON nesnesi olmalı")
        return None
    return data


def need(obj: dict, key: str, ctx: str, typ: type | tuple[type, ...] | None = None):
    if key not in obj:
        err(ctx, f"eksik alan '{key}'")
        return None
    value = obj[key]
    if typ is not None and not isinstance(value, typ):
        err(ctx, f"'{key}' tipi yanlış")
        return None
    return value


def word_count(text: str) -> int:
    return len(WORD_RE.findall(text))


def plain_text_ok(value: str, ctx: str, *, max_words: int | None = None) -> bool:
    ok = True
    if EMOJI_RE.search(value):
        err(ctx, "emoji içermemeli")
        ok = False
    for ch in ("<", ">", "*", "`", "#"):
        if ch in value:
            err(ctx, f"markdown/HTML karakteri içermemeli ('{ch}')")
            ok = False
    if max_words is not None and word_count(value) > max_words:
        err(ctx, f"çok uzun ({word_count(value)} > {max_words} kelime)")
        ok = False
    return ok


def as_text(
    obj: dict,
    key: str,
    ctx: str,
    *,
    min_len: int = 1,
    max_len: int | None = None,
    required: bool = True,
    max_words: int | None = None,
    plain: bool = True,
) -> str | None:
    if key not in obj:
        if required:
            err(ctx, f"eksik alan '{key}'")
        return None
    value = obj[key]
    if not isinstance(value, str):
        err(ctx, f"'{key}' metin olmalı")
        return None
    value = value.strip()
    if len(value) < min_len:
        err(ctx, f"'{key}' boş veya çok kısa")
        return None
    if max_len is not None and len(value) > max_len:
        err(ctx, f"'{key}' çok uzun ({len(value)} > {max_len})")
    if plain:
        plain_text_ok(value, ctx, max_words=max_words)
    else:
        if EMOJI_RE.search(value):
            err(ctx, "emoji içermemeli")
        if "<" in value or ">" in value:
            err(ctx, "HTML içermemeli")
    return value


def check_english(value: str, ctx: str) -> None:
    if TURKISH_CHARS_RE.search(value):
        err(ctx, "İngilizce metinde Türkçe karakter olmamalı")


def compile_regex(pattern: str, ctx: str) -> re.Pattern | None:
    if not isinstance(pattern, str) or not pattern.strip():
        err(ctx, "desen boş")
        return None
    if "(?<=" in pattern or "(?<!" in pattern:
        err(ctx, "lookbehind kullanılmamalı (Kotlin uyumu)")
    if EMOJI_RE.search(pattern):
        err(ctx, "desende emoji olmamalı")
    try:
        return re.compile(pattern)
    except re.error as exc:
        err(ctx, f"regex derlenemedi: {exc}")
        return None


def check_example(example, ctx: str) -> None:
    if not isinstance(example, dict):
        err(ctx, "örnek bir nesne olmalı")
        return
    en = as_text(example, "en", ctx, max_len=160)
    tr = as_text(example, "tr", ctx, max_len=160)
    if en:
        check_english(en, ctx)
        if word_count(en) > 12:
            warn(ctx, f"İngilizce örnek 12 kelimeden uzun ({word_count(en)})")
    if tr:
        stats["example_tr"] += 1
    stats["example"] += 1


def check_examples(obj: dict, ctx: str, *, min_n: int, max_n: int) -> None:
    value = need(obj, "examples", ctx, list)
    if value is None:
        return
    if not (min_n <= len(value) <= max_n):
        err(ctx, f"'examples' {min_n}-{max_n} öğe içermeli (şu an {len(value)})")
    for i, example in enumerate(value):
        check_example(example, f"{ctx} examples[{i}]")
        stats["examples"] += 1


def check_step(step, ctx: str) -> None:
    if not isinstance(step, dict):
        err(ctx, "kart bir nesne olmalı")
        return
    typ = as_text(step, "type", ctx)
    as_text(step, "title_tr", ctx, max_len=100)
    if typ is None:
        return
    if typ not in STEP_TYPES:
        err(ctx, f"bilinmeyen kart tipi '{typ}'")
        return
    stats[f"step_{typ}"] += 1
    stats["step"] += 1

    if typ in {"hook", "rule", "examples", "mistake", "tip"}:
        body = step.get("body_tr")
        if typ == "examples":
            as_text(step, "body_tr", ctx, required=False, max_words=60)
        else:
            body = as_text(step, "body_tr", ctx, max_words=60)

    if typ == "hook":
        check_examples(step, ctx, min_n=1, max_n=2)
    elif typ == "rule":
        as_text(step, "pattern", ctx, max_len=200)
        check_examples(step, ctx, min_n=1, max_n=3)
    elif typ == "examples":
        check_examples(step, ctx, min_n=1, max_n=3)
    elif typ == "mistake":
        wrong = as_text(step, "wrong_en", ctx, max_len=160)
        right = as_text(step, "right_en", ctx, max_len=160)
        if wrong:
            check_english(wrong, ctx)
        if right:
            check_english(right, ctx)
        if wrong and right and wrong.strip() == right.strip():
            err(ctx, "wrong_en ve right_en aynı olmamalı")
    elif typ == "tip":
        if "examples" in step:
            check_examples(step, ctx, min_n=1, max_n=1)


def check_rule(rule, ctx: str) -> None:
    if not isinstance(rule, dict):
        err(ctx, "kural bir nesne olmalı")
        return
    pattern = as_text(rule, "pattern", ctx, max_len=300, plain=False)
    replace = as_text(rule, "replace", ctx, max_len=200, plain=False)
    as_text(rule, "note_tr", ctx, max_len=120, max_words=12)
    stats["rule"] += 1
    if pattern is None:
        return
    compiled = compile_regex(pattern, ctx)
    if compiled is None or replace is None:
        return
    if "\\" not in pattern and pattern == replace:
        warn(ctx, "kural deseni ve replace aynı; değişiklik üretmez")
    for ref in GROUP_REF_RE.findall(replace):
        if int(ref) > compiled.groups:
            err(ctx, f"replace '${ref}' desende olmayan grubu kullanıyor")


def check_task(task, ctx: str, seen_ids: set[str]) -> None:
    if not isinstance(task, dict):
        err(ctx, "görev bir nesne olmalı")
        return
    task_id = as_text(task, "id", ctx, max_len=10)
    if task_id:
        if task_id in seen_ids:
            err(ctx, f"görev id tekrarı '{task_id}'")
        seen_ids.add(task_id)
    as_text(task, "prompt_tr", ctx, max_len=160)
    as_text(task, "hint_tr", ctx, max_len=120, required=False)
    sample = as_text(task, "sample", ctx, max_len=200)
    if sample:
        check_english(sample, ctx)
        words = word_count(sample)
        if words > 14:
            err(ctx, f"'sample' en fazla 14 kelime olmalı (şu an {words})")
    min_words = need(task, "min_words", ctx, int)
    if isinstance(min_words, int) and not (2 <= min_words <= 12):
        err(ctx, "min_words 2-12 aralığında olmalı")
    if sample and isinstance(min_words, int):
        words = word_count(sample)
        if words < min_words:
            err(ctx, f"'sample' min_words'ten kısa ({words} < {min_words})")

    expect = need(task, "expect", ctx, list)
    if expect is None:
        return
    if not (1 <= len(expect) <= 3):
        err(ctx, f"'expect' 1-3 kural içermeli (şu an {len(expect)})")
    for i, item in enumerate(expect):
        ictx = f"{ctx} expect[{i}]"
        if not isinstance(item, dict):
            err(ictx, "kural bir nesne olmalı")
            continue
        pattern = as_text(item, "pattern", ictx, max_len=200, plain=False)
        anchor = as_text(item, "anchor", ictx, max_len=120, plain=False)
        as_text(item, "note_tr", ictx, max_len=120, max_words=12)
        compiled = compile_regex(pattern, ictx) if pattern else None
        anchor_compiled = compile_regex(anchor, ictx) if anchor else None
        if compiled is not None and sample and not compiled.search(sample):
            err(ictx, "desen sample ile eşleşmiyor")
        if anchor_compiled is not None and sample and not anchor_compiled.search(sample):
            warn(ictx, "anchor sample içinde geçmiyor")
        stats["expect"] += 1


def check_write(write, ctx: str) -> None:
    if not isinstance(write, dict):
        err(ctx, "'write' bir nesne olmalı")
        return
    as_text(write, "intro_tr", ctx, max_len=300)
    tasks = need(write, "tasks", ctx, list)
    if tasks is None:
        return
    if len(tasks) != 3:
        err(ctx, f"'write.tasks' tam 3 öğe olmalı (şu an {len(tasks)})")
    seen_ids: set[str] = set()
    for i, task in enumerate(tasks):
        check_task(task, f"{ctx} tasks[{i}]", seen_ids)
    if seen_ids and seen_ids != {"w1", "w2", "w3"}:
        err(ctx, f"görev id'leri w1, w2, w3 olmalı (şu an {sorted(seen_ids)})")
    stats["task"] += len(tasks)


def check_topic(data: dict, ctx: str, index_item: dict, expected: tuple) -> None:
    topic_id, title, filename, notes = expected
    stats["topic"] += 1
    if need(data, "schema", ctx) != 1:
        err(ctx, "'schema' 1 olmalı")
    got_id = as_text(data, "id", ctx)
    got_cefr = as_text(data, "cefr", ctx)
    got_title = as_text(data, "title_tr", ctx)
    as_text(data, "subtitle_tr", ctx, max_len=120)
    as_text(data, "objective_tr", ctx, max_len=300)
    est = need(data, "est_min", ctx, int)
    if got_id and got_id != topic_id:
        err(ctx, f"id '{got_id}' beklenen '{topic_id}' değil")
    if got_cefr and got_cefr not in CEFR:
        err(ctx, f"geçersiz cefr '{got_cefr}'")
    if got_cefr != "A1":
        err(ctx, "cefr A1 olmalı")
    if got_title and got_title != title:
        err(ctx, f"title_tr '{got_title}' beklenen '{title}' değil")
    if isinstance(est, int) and not (3 <= est <= 6):
        err(ctx, "est_min 3-6 aralığında olmalı")
    if index_item.get("est_min") != est:
        err(ctx, "est_min index ile uyuşmuyor")

    source_notes = need(data, "source_notes", ctx, list)
    if source_notes is not None:
        if source_notes != notes:
            err(ctx, f"source_notes tablo ile birebir eşleşmiyor: {source_notes}")
        if len(source_notes) != len(set(source_notes)):
            err(ctx, "source_notes içinde tekrar var")
        stats["source_note"] += len(source_notes)
        covered_notes.update(note for note in source_notes if isinstance(note, str))

    steps = need(data, "steps", ctx, list)
    if steps is not None:
        if not (3 <= len(steps) <= 6):
            err(ctx, f"'steps' 3-6 kart içermeli (şu an {len(steps)})")
        for i, step in enumerate(steps):
            check_step(step, f"{ctx} steps[{i}]")

    rules = need(data, "rules", ctx, list)
    if rules is not None:
        if len(rules) > 8:
            err(ctx, f"'rules' en fazla 8 kural içermeli (şu an {len(rules)})")
        seen_patterns: set[str] = set()
        for i, rule in enumerate(rules):
            check_rule(rule, f"{ctx} rules[{i}]")
            if isinstance(rule, dict) and isinstance(rule.get("pattern"), str):
                if rule["pattern"] in seen_patterns:
                    warn(f"{ctx} rules[{i}]", "kural deseni tekrar ediyor")
                seen_patterns.add(rule["pattern"])

    if "write" in data:
        check_write(data["write"], f"{ctx} write")
    else:
        err(ctx, "eksik alan 'write'")

    # Başlık/alt başlık index ile aynı olmalı.
    if index_item.get("title_tr") != data.get("title_tr"):
        err(ctx, "title_tr index ile uyuşmuyor")
    if index_item.get("subtitle_tr") != data.get("subtitle_tr"):
        err(ctx, "subtitle_tr index ile uyuşmuyor")


def lesson_catalog(course: Path) -> tuple[set[str], dict[str, int]]:
    ids: set[str] = set()
    counts: dict[str, int] = {}
    for path in sorted(course.glob("a1/*/*.json")):
        data = load_json(path)
        if data is None:
            continue
        lesson_id = data.get("id")
        if not isinstance(lesson_id, str):
            continue
        if not re.fullmatch(r"a1-u\d{2}-l\d{2}", lesson_id):
            continue
        grammar = data.get("grammar")
        if isinstance(grammar, list):
            ids.add(lesson_id)
            counts[lesson_id] = len(grammar)
    return ids, counts


def check_source_notes(
    source_notes: list[str],
    ctx: str,
    lesson_ids: set[str],
    grammar_counts: dict[str, int],
) -> None:
    for note in source_notes:
        if not isinstance(note, str) or not SOURCE_NOTE_RE.fullmatch(note):
            err(ctx, f"geçersiz source note '{note}'")
            continue
        lesson_id, index_text = SOURCE_NOTE_RE.fullmatch(note).groups()  # type: ignore[union-attr]
        if lesson_id not in lesson_ids:
            err(ctx, f"source note dersi bulunamadı: '{lesson_id}'")
            continue
        if int(index_text) >= grammar_counts.get(lesson_id, 0):
            err(ctx, f"source note indeksi yok: '{note}'")


def check_index(grammar: Path, course: Path) -> None:
    index_path = grammar / "index.json"
    data = load_json(index_path)
    if data is None:
        return
    ctx = str(index_path)
    if need(data, "schema", ctx) != 1:
        err(ctx, "'schema' 1 olmalı")
    if need(data, "language", ctx) != "en":
        err(ctx, "'language' en olmalı")
    if need(data, "native", ctx) != "tr":
        err(ctx, "'native' tr olmalı")
    topics = need(data, "topics", ctx, list)
    if topics is None:
        return
    if len(topics) != len(EXPECTED_TOPICS):
        err(ctx, f"'topics' tam {len(EXPECTED_TOPICS)} öğe olmalı (şu an {len(topics)})")

    lesson_ids, grammar_counts = lesson_catalog(course)
    if not lesson_ids:
        err(str(course), "ders kataloğu boş; linked_lessons doğrulanamaz")
    stats["lesson"] = len(lesson_ids)

    seen_ids: set[str] = set()
    seen_files: set[str] = set()
    seen_titles: set[str] = set()
    expected_files = {item[2] for item in EXPECTED_TOPICS}

    for i, item in enumerate(topics):
        ictx = f"{ctx} topics[{i}]"
        if not isinstance(item, dict):
            err(ictx, "konu bir nesne olmalı")
            continue
        topic_id = as_text(item, "id", ictx)
        title = as_text(item, "title_tr", ictx)
        as_text(item, "subtitle_tr", ictx, max_len=120)
        cefr = as_text(item, "cefr", ictx)
        filename = as_text(item, "file", ictx)
        icon = as_text(item, "icon", ictx)
        color = as_text(item, "color", ictx, plain=False)
        est = need(item, "est_min", ictx, int)
        linked = need(item, "linked_lessons", ictx, list)
        if topic_id:
            if not TOPIC_ID_RE.fullmatch(topic_id):
                err(ictx, f"konu id biçimi yanlış: '{topic_id}'")
            if topic_id in seen_ids:
                err(ictx, f"konu id tekrarı '{topic_id}'")
            seen_ids.add(topic_id)
        if title:
            if title in seen_titles:
                err(ictx, f"konu başlığı tekrarı '{title}'")
            seen_titles.add(title)
        if cefr and cefr not in CEFR:
            err(ictx, f"geçersiz cefr '{cefr}'")
        if icon and icon not in ICONS:
            err(ictx, f"bilinmeyen ikon '{icon}'")
        if color and not COLOR_RE.fullmatch(color):
            err(ictx, f"renk #RRGGBB olmalı: '{color}'")
        if isinstance(est, int) and not (3 <= est <= 6):
            err(ictx, "est_min 3-6 aralığında olmalı")
        if linked is not None:
            if not (1 <= len(linked) <= 4):
                err(ictx, "'linked_lessons' 1-4 ders içermeli")
            if len(linked) != len(set(linked)):
                err(ictx, "'linked_lessons' içinde tekrar var")
            for lesson_id in linked:
                if not isinstance(lesson_id, str) or lesson_id not in lesson_ids:
                    err(ictx, f"linked lesson bulunamadı: '{lesson_id}'")
        if filename:
            if not FILE_RE.fullmatch(filename):
                err(ictx, f"dosya yolu biçimi yanlış: '{filename}'")
            if filename in seen_files:
                err(ictx, f"dosya tekrarı '{filename}'")
            seen_files.add(filename)

        expected = EXPECTED_TOPICS[i] if i < len(EXPECTED_TOPICS) else None
        if expected is None:
            continue
        if topic_id != expected[0]:
            err(ictx, f"id sırası yanlış: '{topic_id}' yerine '{expected[0]}'")
        if title != expected[1]:
            err(ictx, f"başlık tablo ile uyuşmuyor: '{title}'")
        if filename != expected[2]:
            err(ictx, f"dosya tablo ile uyuşmuyor: '{filename}'")
        if filename:
            topic_path = grammar / filename
            topic = load_json(topic_path)
            if topic is not None:
                check_topic(topic, str(topic_path), item, expected)
                check_source_notes(
                    topic.get("source_notes", []),
                    str(topic_path),
                    lesson_ids,
                    grammar_counts,
                )
                if linked is not None:
                    source_lessons = {
                        note.split("#", 1)[0]
                        for note in topic.get("source_notes", [])
                        if isinstance(note, str) and "#" in note
                    }
                    missing = source_lessons - set(linked)
                    if missing:
                        warn(ictx, f"linked_lessons kaynak dersleri kapsamıyor: {sorted(missing)}")

    actual_files = {
        str(path.relative_to(grammar)).replace("\\", "/")
        for path in (grammar / "a1").glob("*.json")
    }
    missing_files = expected_files - actual_files
    extra_files = actual_files - expected_files
    if missing_files:
        err(str(grammar / "a1"), f"eksik konu dosyaları: {sorted(missing_files)}")
    if extra_files:
        err(str(grammar / "a1"), f"fazladan konu dosyaları: {sorted(extra_files)}")

    expected_notes = set(EXPECTED_NOTES)
    if covered_notes != expected_notes:
        missing = sorted(expected_notes - covered_notes)
        extra = sorted(covered_notes - expected_notes)
        if missing:
            err(ctx, f"kapsanmayan source note'lar: {missing}")
        if extra:
            err(ctx, f"tabloda olmayan source note'lar: {extra}")
    if len(EXPECTED_NOTES) != len(set(EXPECTED_NOTES)):
        err(ctx, "kontrat tablosunda source note tekrarı var")
    stats["covered_note"] = len(covered_notes & expected_notes)


def main() -> int:
    parser = argparse.ArgumentParser(description="Gramer içerik doğrulayıcı")
    parser.add_argument("--grammar", type=Path, default=DEFAULT_GRAMMAR)
    parser.add_argument("--course", type=Path, default=DEFAULT_COURSE)
    parser.add_argument("--strict", action="store_true", help="uyarıları da hata say")
    args = parser.parse_args()

    check_index(args.grammar, args.course)

    for message in errors:
        print(f"HATA  {message}", file=sys.stderr)
    for message in warnings:
        print(f"UYARI {message}", file=sys.stderr)

    print(
        "Özet: "
        f"{stats['topic']} konu, {stats['step']} kart, "
        f"{stats['task']} yazma görevi, {stats['rule']} kural, "
        f"{stats['expect']} expect, {stats['example']} örnek, "
        f"{stats['covered_note']}/{len(EXPECTED_NOTES)} source note | "
        f"{len(errors)} hata, {len(warnings)} uyarı"
    )
    if errors or (args.strict and warnings):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
