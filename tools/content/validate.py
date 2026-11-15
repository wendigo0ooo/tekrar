#!/usr/bin/env python3
"""Dil Okulu içerik doğrulayıcı.

Kullanım:
  python3 tools/content/validate.py
  python3 tools/content/validate.py --unit app/src/main/assets/course/a1/u01/unit.json
  python3 tools/content/validate.py --lesson tools/content/ornek-ders.json
  python3 tools/content/validate.py --strict

Kurallar: docs/dil-okulu-kontrat.md
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_COURSE = ROOT / "app/src/main/assets/course"
CEFR = {"A1", "A2", "B1", "B2"}
ALLOWED_TYPES = {
    "choice",
    "listen_choice",
    "dialogue_choice",
    "match",
    "word_order",
    "cloze",
    "translate",
    "speak",
    "write",
}
PRODUCTION_TYPES = {"translate", "speak", "write"}

errors: list[str] = []
warnings: list[str] = []
stats: Counter[str] = Counter()


def err(ctx: str, msg: str) -> None:
    errors.append(f"{ctx}: {msg}")


def warn(ctx: str, msg: str) -> None:
    warnings.append(f"{ctx}: {msg}")


def load_json(path: Path) -> dict | None:
    if not path.exists():
        err(str(path), "dosya yok")
        return None
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
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


def as_text(
    obj: dict,
    key: str,
    ctx: str,
    *,
    min_len: int = 1,
    max_len: int | None = None,
    required: bool = True,
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
    if "<" in value or ">" in value:
        err(ctx, f"'{key}' HTML içermemeli")
    return value


def as_list(obj: dict, key: str, ctx: str, *, min_n: int, max_n: int) -> list | None:
    value = need(obj, key, ctx, list)
    if value is None:
        return None
    if not (min_n <= len(value) <= max_n):
        err(ctx, f"'{key}' {min_n}-{max_n} öğe içermeli (şu an {len(value)})")
    return value


def check_example(value, ctx: str) -> None:
    if not isinstance(value, dict):
        err(ctx, "örnek bir nesne olmalı")
        return
    as_text(value, "en", ctx)
    as_text(value, "tr", ctx)


def check_lesson(data: dict, ctx: str, *, expect_unit: str | None = None) -> None:
    stats["lesson"] += 1
    if need(data, "schema", ctx) != 1:
        err(ctx, "'schema' 1 olmalı")

    lesson_id = as_text(data, "id", ctx)
    unit_id = as_text(data, "unit", ctx)
    cefr = as_text(data, "cefr", ctx)
    as_text(data, "title", ctx)
    as_text(data, "objective_tr", ctx)

    if cefr is not None and cefr not in CEFR:
        err(ctx, f"geçersiz cefr '{cefr}'")
    if lesson_id and unit_id and not lesson_id.startswith(unit_id + "-l"):
        err(ctx, f"id '{lesson_id}' unit '{unit_id}' ile uyumsuz")
    if expect_unit and unit_id != expect_unit:
        err(ctx, f"unit '{unit_id}' beklenen '{expect_unit}' değil")

    est = need(data, "est_min", ctx, int)
    if isinstance(est, int) and not (3 <= est <= 6):
        err(ctx, "est_min 3-6 aralığında olmalı")

    # Kelimeler
    vocab = as_list(data, "vocab", ctx, min_n=5, max_n=10)
    if vocab is not None:
        seen_terms: set[str] = set()
        for i, item in enumerate(vocab):
            ictx = f"{ctx} vocab[{i}]"
            if not isinstance(item, dict):
                err(ictx, "nesne olmalı")
                continue
            term = as_text(item, "term", ictx)
            as_text(item, "meaning_tr", ictx)
            as_text(item, "pos", ictx)
            example = as_text(item, "example", ictx)
            as_text(item, "example_tr", ictx)
            if term:
                key = term.casefold().strip()
                if key in seen_terms:
                    err(ictx, f"kelime tekrarı '{term}'")
                seen_terms.add(key)
            if term and example and term.casefold() not in example.casefold():
                warn(ictx, f"örnek cümlede '{term}' geçmiyor")
            if "ipa" in item and not isinstance(item["ipa"], str):
                err(ictx, "'ipa' metin olmalı")
            stats["vocab"] += 1

    # Dil bilgisi
    grammar = data.get("grammar", [])
    if not isinstance(grammar, list) or len(grammar) > 2:
        err(ctx, "'grammar' en fazla 2 öğe içeren liste olmalı")
    else:
        for i, item in enumerate(grammar):
            ictx = f"{ctx} grammar[{i}]"
            if not isinstance(item, dict):
                err(ictx, "nesne olmalı")
                continue
            as_text(item, "title_tr", ictx)
            as_text(item, "explanation_tr", ictx, max_len=400)
            examples = as_list(item, "examples", ictx, min_n=1, max_n=4)
            for j, example in enumerate(examples or []):
                check_example(example, f"{ictx} examples[{j}]")

    # Öğretim kartları
    teach = as_list(data, "teach", ctx, min_n=0, max_n=2)
    for i, item in enumerate(teach or []):
        ictx = f"{ctx} teach[{i}]"
        if not isinstance(item, dict):
            err(ictx, "nesne olmalı")
            continue
        as_text(item, "title_tr", ictx)
        as_text(item, "body_tr", ictx, max_len=500)
        examples = as_list(item, "examples", ictx, min_n=1, max_n=4)
        for j, example in enumerate(examples or []):
            check_example(example, f"{ictx} examples[{j}]")
        if "tip_tr" in item:
            as_text(item, "tip_tr", ictx, required=False)

    # Diyalog
    if "dialogue" in data:
        dialogue = data["dialogue"]
        dctx = f"{ctx} dialogue"
        if not isinstance(dialogue, dict):
            err(dctx, "nesne olmalı")
        else:
            as_text(dialogue, "title", dctx)
            lines = as_list(dialogue, "lines", dctx, min_n=4, max_n=8)
            for i, line in enumerate(lines or []):
                lctx = f"{dctx} lines[{i}]"
                if not isinstance(line, dict):
                    err(lctx, "nesne olmalı")
                    continue
                as_text(line, "speaker", lctx)
                as_text(line, "en", lctx)
                as_text(line, "tr", lctx)

    # Alıştırmalar
    exercises = as_list(data, "exercises", ctx, min_n=8, max_n=12)
    type_counts: Counter[str] = Counter()
    for i, ex in enumerate(exercises or []):
        ectx = f"{ctx} exercises[{i}]"
        if not isinstance(ex, dict):
            err(ectx, "nesne olmalı")
            continue
        typ = as_text(ex, "type", ectx)
        if typ is None:
            continue
        if typ not in ALLOWED_TYPES:
            err(ectx, f"bilinmeyen tip '{typ}'")
            continue
        type_counts[typ] += 1
        as_text(ex, "prompt_tr", ectx)
        check_exercise(ex, typ, ectx)

    if exercises:
        if sum(type_counts.values()) and len(type_counts) < 4:
            warn(ctx, f"ders yalnız {len(type_counts)} alıştırma tipi içeriyor")
        prod = sum(type_counts[t] for t in PRODUCTION_TYPES)
        if prod < 2:
            err(ctx, f"en az 2 üretim alıştırması gerekli (şu an {prod})")
        stats["exercise"] += len(exercises)


def check_exercise(ex: dict, typ: str, ctx: str) -> None:
    if typ in {"choice", "listen_choice", "dialogue_choice"}:
        options = as_list(ex, "options", ctx, min_n=2, max_n=4)
        if options is not None:
            for i, option in enumerate(options):
                if not isinstance(option, str) or not option.strip():
                    err(f"{ctx} options[{i}]", "boş seçenek")
        answer = need(ex, "answer", ctx, int)
        if isinstance(answer, int) and options is not None and not (0 <= answer < len(options)):
            err(ctx, "answer seçenek aralığında değil")
        if typ == "listen_choice":
            as_text(ex, "audio_text", ctx, max_len=160)
        elif typ == "dialogue_choice" and "audio_text" in ex:
            as_text(ex, "audio_text", ctx, max_len=160, required=False)
        if "explanation_tr" in ex:
            as_text(ex, "explanation_tr", ctx, max_len=400, required=False)

    elif typ == "match":
        pairs = as_list(ex, "pairs", ctx, min_n=3, max_n=5)
        lefts: set[str] = set()
        rights: set[str] = set()
        for i, pair in enumerate(pairs or []):
            pctx = f"{ctx} pairs[{i}]"
            if not isinstance(pair, dict):
                err(pctx, "nesne olmalı")
                continue
            left = as_text(pair, "left", pctx)
            right = as_text(pair, "right", pctx)
            if left:
                if left.casefold() in lefts:
                    warn(pctx, f"sol tarafta tekrar '{left}'")
                lefts.add(left.casefold())
            if right:
                if right.casefold() in rights:
                    warn(pctx, f"sağ tarafta tekrar '{right}'")
                rights.add(right.casefold())

    elif typ == "word_order":
        tokens = as_list(ex, "tokens", ctx, min_n=3, max_n=8)
        answer = as_list(ex, "answer", ctx, min_n=3, max_n=8)
        if tokens is not None and answer is not None:
            if Counter(tokens) != Counter(answer):
                err(ctx, "tokens ve answer aynı kelime çokluğuna sahip olmalı")
            if tokens == answer:
                warn(ctx, "tokens zaten doğru sırada; karıştır")
        if "translation_tr" in ex:
            as_text(ex, "translation_tr", ctx, required=False)

    elif typ == "cloze":
        sentence = as_text(ex, "sentence", ctx, max_len=240)
        if sentence and sentence.count("___") != 1:
            err(ctx, "cloze cümlesinde tam olarak bir ___ olmalı")
        has_options = "options" in ex
        has_answers = "answers" in ex
        if has_options == has_answers:
            err(ctx, "cloze 'options+answer' veya 'answers' biçimlerinden birini kullanmalı")
        if has_options:
            options = as_list(ex, "options", ctx, min_n=2, max_n=4)
            answer = need(ex, "answer", ctx, int)
            if isinstance(answer, int) and options is not None and not (0 <= answer < len(options)):
                err(ctx, "answer seçenek aralığında değil")
        if has_answers:
            answers = as_list(ex, "answers", ctx, min_n=1, max_n=3)
            for i, value in enumerate(answers or []):
                if not isinstance(value, str) or not value.strip():
                    err(f"{ctx} answers[{i}]", "boş cevap")

    elif typ == "translate":
        answers = as_list(ex, "answers", ctx, min_n=1, max_n=3)
        for i, value in enumerate(answers or []):
            if not isinstance(value, str) or not value.strip():
                err(f"{ctx} answers[{i}]", "boş cevap")
        if "hint_tr" in ex:
            as_text(ex, "hint_tr", ctx, required=False)

    elif typ == "speak":
        target = as_text(ex, "target", ctx, max_len=120)
        if target and len(target.split()) > 12:
            err(ctx, "speak target en fazla 12 kelime olmalı")
        if "hint_tr" in ex:
            as_text(ex, "hint_tr", ctx, required=False)

    elif typ == "write":
        min_words = need(ex, "min_words", ctx, int)
        if isinstance(min_words, int) and not (5 <= min_words <= 30):
            err(ctx, "min_words 5-30 aralığında olmalı")
        if "keywords" in ex:
            keywords = as_list(ex, "keywords", ctx, min_n=1, max_n=5)
            for i, value in enumerate(keywords or []):
                if not isinstance(value, str) or not value.strip():
                    err(f"{ctx} keywords[{i}]", "boş anahtar kelime")
        if "sample" in ex:
            as_text(ex, "sample", ctx, required=False)
        if "rubric_tr" in ex:
            as_text(ex, "rubric_tr", ctx, required=False)


def check_roleplay(data: dict, ctx: str, *, expect_unit: str | None = None) -> None:
    stats["roleplay"] += 1
    if need(data, "schema", ctx) != 1:
        err(ctx, "'schema' 1 olmalı")
    rid = as_text(data, "id", ctx)
    unit = as_text(data, "unit", ctx)
    cefr = as_text(data, "cefr", ctx)
    as_text(data, "title", ctx)
    as_text(data, "setting_tr", ctx, max_len=500)
    as_text(data, "role_en", ctx, max_len=500)
    as_text(data, "goal_tr", ctx, max_len=500)
    opening = as_text(data, "opening_en", ctx, max_len=200)
    if cefr is not None and cefr not in CEFR:
        err(ctx, f"geçersiz cefr '{cefr}'")
    if expect_unit and unit != expect_unit:
        err(ctx, f"unit '{unit}' beklenen '{expect_unit}' değil")
    if rid and unit and not rid.startswith(unit + "-rp"):
        err(ctx, f"id '{rid}' unit '{unit}' ile uyumsuz")
    if opening and opening.count(".") > 2:
        warn(ctx, "opening_en tek cümle olmalı")
    phrases = as_list(data, "target_phrases", ctx, min_n=2, max_n=6)
    for i, value in enumerate(phrases or []):
        if not isinstance(value, str) or not value.strip():
            err(f"{ctx} target_phrases[{i}]", "boş ifade")
    turns = need(data, "suggested_turns", ctx, int)
    if isinstance(turns, int) and not (4 <= turns <= 10):
        err(ctx, "suggested_turns 4-10 aralığında olmalı")
    criteria = as_list(data, "success_criteria_tr", ctx, min_n=2, max_n=4)
    for i, value in enumerate(criteria or []):
        if not isinstance(value, str) or not value.strip():
            err(f"{ctx} success_criteria_tr[{i}]", "boş madde")


def check_unit(path: Path) -> None:
    data = load_json(path)
    if data is None:
        return
    ctx = str(path)
    stats["unit"] += 1
    if need(data, "schema", ctx) != 1:
        err(ctx, "'schema' 1 olmalı")
    unit_id = as_text(data, "id", ctx)
    cefr = as_text(data, "cefr", ctx)
    as_text(data, "title", ctx)
    as_text(data, "subtitle", ctx)
    if cefr is not None and cefr not in CEFR:
        err(ctx, f"geçersiz cefr '{cefr}'")
    if unit_id and not re.fullmatch(r"[a-z0-9]+-u\d{2}", unit_id):
        err(ctx, f"unit id biçimi yanlış: '{unit_id}'")

    lessons = as_list(data, "lessons", ctx, min_n=1, max_n=20)
    seen: set[str] = set()
    for i, item in enumerate(lessons or []):
        lctx = f"{ctx} lessons[{i}]"
        if not isinstance(item, dict):
            err(lctx, "nesne olmalı")
            continue
        lid = as_text(item, "id", lctx)
        as_text(item, "title", lctx)
        filename = as_text(item, "file", lctx)
        est = need(item, "est_min", lctx, int)
        if isinstance(est, int) and not (3 <= est <= 6):
            err(lctx, "est_min 3-6 aralığında olmalı")
        if lid:
            if lid in seen:
                err(lctx, f"ders id tekrarı '{lid}'")
            seen.add(lid)
        if filename:
            lesson_path = path.parent / filename
            lesson = load_json(lesson_path)
            if lesson is not None:
                check_lesson(lesson, str(lesson_path), expect_unit=unit_id)
                if lid and lesson.get("id") != lid:
                    err(lctx, f"dosyadaki id '{lesson.get('id')}' ile eşleşmiyor")
                if lesson.get("title") != item.get("title"):
                    warn(lctx, "unit ve ders başlığı farklı")

    if "roleplay" in data:
        rp = data["roleplay"]
        rctx = f"{ctx} roleplay"
        if not isinstance(rp, dict):
            err(rctx, "nesne olmalı")
        else:
            as_text(rp, "title", rctx)
            filename = as_text(rp, "file", rctx)
            if filename:
                rp_path = path.parent / filename
                rp_data = load_json(rp_path)
                if rp_data is not None:
                    check_roleplay(rp_data, str(rp_path), expect_unit=unit_id)


def check_course(root: Path) -> None:
    index_path = root / "index.json"
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
    levels = as_list(data, "levels", ctx, min_n=1, max_n=6)
    seen_units: set[str] = set()
    for i, level in enumerate(levels or []):
        lctx = f"{ctx} levels[{i}]"
        if not isinstance(level, dict):
            err(lctx, "nesne olmalı")
            continue
        level_id = as_text(level, "id", lctx)
        as_text(level, "title", lctx)
        if level_id and level_id.upper() not in CEFR:
            err(lctx, f"geçersiz seviye '{level_id}'")
        units = as_list(level, "units", lctx, min_n=1, max_n=30)
        for j, unit_ref in enumerate(units or []):
            uctx = f"{lctx} units[{j}]"
            if not isinstance(unit_ref, dict):
                err(uctx, "nesne olmalı")
                continue
            unit_id = as_text(unit_ref, "id", uctx)
            filename = as_text(unit_ref, "file", uctx)
            if unit_id:
                if unit_id in seen_units:
                    err(uctx, f"ünite tekrarı '{unit_id}'")
                seen_units.add(unit_id)
            if filename:
                unit_path = root / filename
                unit = load_json(unit_path)
                if unit is not None:
                    check_unit(unit_path)
                    if unit_id and unit.get("id") != unit_id:
                        err(uctx, f"dosyadaki id '{unit.get('id')}' ile eşleşmiyor")
                    if level_id and unit.get("cefr") != level_id.upper():
                        err(uctx, "ünite cefr seviye ile uyuşmuyor")


def run_grammar_validator() -> int:
    """Gramer içeriğini ayrı doğrulayıcıyla denetler (varsa)."""
    script = ROOT / "tools/content/validate_grammar.py"
    if not script.exists():
        print("UYARI gramer doğrulayıcı bulunamadı", file=sys.stderr)
        return 0
    return subprocess.call([sys.executable, str(script)])


def main() -> int:
    parser = argparse.ArgumentParser(description="Dil Okulu içerik doğrulayıcı")
    parser.add_argument("--course", type=Path, default=DEFAULT_COURSE)
    parser.add_argument("--unit", type=Path)
    parser.add_argument("--lesson", type=Path)
    parser.add_argument("--strict", action="store_true", help="uyarıları da hata say")
    args = parser.parse_args()

    if args.lesson:
        data = load_json(args.lesson)
        if data is not None:
            check_lesson(data, str(args.lesson))
    elif args.unit:
        check_unit(args.unit)
    else:
        check_course(args.course)

    grammar_rc = 0
    if not args.lesson and not args.unit:
        grammar_rc = run_grammar_validator()

    for message in errors:
        print(f"HATA  {message}", file=sys.stderr)
    for message in warnings:
        print(f"UYARI {message}", file=sys.stderr)

    print(
        "Özet: "
        f"{stats['unit']} ünite, {stats['lesson']} ders, "
        f"{stats['roleplay']} rol yapma, {stats['vocab']} kelime, "
        f"{stats['exercise']} alıştırma | "
        f"{len(errors)} hata, {len(warnings)} uyarı"
    )
    if errors or (args.strict and warnings) or grammar_rc != 0:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
