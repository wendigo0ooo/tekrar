# Gramer Modülü Kontratı (v1)

Tekrar / Dil Okulu'na ayrı **Gramer** sekmesi eklenir. Amaç: mevcut 21 gramer
notunu (12 dersin `grammar` alanı) kısa, sıkıcı olmayan kartlara dönüştürmek;
ardından kullanıcı 3 cümle yazar, cümleler kontrol edilir, hatalı kelimeler
kırmızı işaretlenir ve doğrusu öğretilir.

Kaynak: `~/tekrar` · branch `feat/dil-okulu` · Room **v5** (yeni migration yok) ·
sürüm hedefi **3.1.0** (versionCode 5) · 5 sekme.

> Not: Mevcut içerikte 21 `grammar` notu var. Birebir örtüşen notlar
> birleştirilerek **16 konu** üretilir; 21 notun tamamı `source_notes` ile
> izlenir (aşağıdaki tablo).

## 1. Ürün akışı

1. Alt menüde 5. sekme: **Gramer** (Öğren · Gramer · Bugün · Sözlük · Profil).
2. Gramer ana ekranı: ilerleme (X/16), "Kaldığın yerden devam" kartı, 16 konu
   listesi. Konular kilitli değil; istenen konu açılabilir.
3. Konu ekranı: 3-6 kısa kart (tek ekranda bir fikir). Kart tipleri:
   `hook`, `rule`, `examples`, `mistake`, `tip`. Örneklerde TTS düğmesi.
4. Konu sonunda "Sıra sende": 3 yazma görevi, tek tek.
5. Her görevde "Kontrol et":
   - Kullanıcının cümlesi gösterilir; hatalı kelimeler **kırmızı** ve altı çizili.
   - `Doğrusu:` satırı **yeşil** düzeltilmiş cümleyi gösterir.
   - Hata listesi: `I is → I am · 'I' ile 'am' kullanılır.` gibi.
   - "Tekrar dene" ve "Sonraki cümle" yan yana.
6. 3 görev bitince özet: doğru cümle sayısı, hata listesi, "Konu tamam".
   Konu `lesson_progress` tablosuna `grammar:<topicId>` olarak yazılır.

Süre hedefi konu başına 3-5 dk. Duvara yazı yok: kart gövdesi en fazla
**60 kelime**, kart başına en fazla **3 örnek**. Kullanıcıyı zorlayan süre
baskısı yok; her görev atlanabilir (yine de kontrol edilmeden geçilmez).

## 2. İçerik yapısı

Kök: `app/src/main/assets/grammar/`

```
grammar/
  index.json
  a1/
    g01-to-be.json
    g02-how-are-you.json
    ...
    g16-do-questions.json
```

### 2.1 `index.json`

```json
{
  "schema": 1,
  "language": "en",
  "native": "tr",
  "topics": [
    {
      "id": "g-a1-to-be",
      "title_tr": "am / is / are",
      "subtitle_tr": "Kendini ve çevreni tanıt",
      "cefr": "A1",
      "file": "a1/g01-to-be.json",
      "icon": "person",
      "color": "#4F8EF7",
      "est_min": 4,
      "linked_lessons": ["a1-u01-l01", "a1-u01-l02"]
    }
  ]
}
```

- `topics` sırası müfredat sırasıdır; 16 öğe zorunlu.
- `id` benzersiz; `file` kök dizine göre.
- `icon` `person|chat|tag|home|clock|sun|briefcase|question|book|flag` gibi
  kapalı bir kümeden; bilinmeyen ikon `book` sayılır.
- `color` `#RRGGBB`.

### 2.2 Konu (`gNN-*.json`)

```json
{
  "schema": 1,
  "id": "g-a1-to-be",
  "cefr": "A1",
  "title_tr": "am / is / are",
  "subtitle_tr": "Kendini ve çevreni tanıt",
  "objective_tr": "Bu konudan sonra am/is/are ile 3 cümle kurabileceksin.",
  "est_min": 4,
  "source_notes": ["a1-u01-l01#0", "a1-u01-l02#0"],
  "steps": [
    {
      "type": "hook",
      "title_tr": "Neden önemli?",
      "body_tr": "İngilizcede 'olmak' fiili kendini tanıtmanın temelidir.",
      "examples": [{ "en": "I am Ali.", "tr": "Ben Ali." }]
    },
    {
      "type": "rule",
      "title_tr": "Kural",
      "body_tr": "Özneye göre fiil değişir.",
      "pattern": "I + am · you/we/they + are · he/she/it + is",
      "examples": [
        { "en": "I am a student.", "tr": "Öğrenciyim." },
        { "en": "She is a teacher.", "tr": "O bir öğretmen." }
      ]
    },
    {
      "type": "examples",
      "title_tr": "Örnekler",
      "examples": [
        { "en": "We are ready.", "tr": "Hazırız." },
        { "en": "They are at home.", "tr": "Onlar evde." }
      ]
    },
    {
      "type": "mistake",
      "title_tr": "Sık yapılan hata",
      "wrong_en": "I is a student.",
      "right_en": "I am a student.",
      "body_tr": "I ile her zaman am kullanılır."
    },
    {
      "type": "tip",
      "title_tr": "Akılda tut",
      "body_tr": "Kısa biçimler: I'm, you're, he's, she's, we're, they're."
    }
  ],
  "rules": [
    {
      "pattern": "\\bI\\s+(is|are)\\b",
      "replace": "I am",
      "note_tr": "'I' ile 'am' kullanılır."
    }
  ],
  "write": {
    "intro_tr": "Şimdi sıra sende. 3 cümle yaz; kontrol edince hatalarını kırmızı göreceksin.",
    "tasks": [
      {
        "id": "w1",
        "prompt_tr": "Kendini tanıt: adın + öğrenci/meslek.",
        "hint_tr": "I am ... / I'm ...",
        "sample": "I am Deniz. I am a student.",
        "min_words": 3,
        "expect": [
          {
            "pattern": "\\bI\\s*(am|'m)\\b",
            "anchor": "\\bI\\b",
            "note_tr": "Kendini tanıtırken 'I am' kullan."
          }
        ]
      }
    ]
  }
}
```

Zorunlu alanlar: `schema`, `id`, `cefr`, `title_tr`, `subtitle_tr`,
`objective_tr`, `est_min`, `source_notes`, `steps`, `rules`, `write`.
`write.tasks` tam **3** öğe; `steps` 3-6 öğe.

Kart tipleri:

| `type` | Zorunlu alanlar | Not |
|---|---|---|
| `hook` | `title_tr`, `body_tr`, 1-2 `examples` | Merak uyandırır |
| `rule` | `title_tr`, `body_tr`, `pattern`, 1-3 `examples` | `pattern` düz metin |
| `examples` | `title_tr`, 1-3 `examples` | İsteğe bağlı `body_tr` |
| `mistake` | `title_tr`, `wrong_en`, `right_en`, `body_tr` | Yanlış/doğru vurgusu |
| `tip` | `title_tr`, `body_tr` | İsteğe bağlı 1 örnek |

Yazma görevi:

- `id` konu içinde benzersiz (`w1..w3`).
- `prompt_tr` Türkçe, tek cümle; `hint_tr` isteğe bağlı.
- `sample` doğal ve A1; en fazla 14 kelime.
- `min_words` 2-12.
- `expect` 1-3 kural; her biri `pattern` + `anchor` + `note_tr`.
  `anchor`, beklenen yapı yoksa kırmızı işaretlenecek kelime/ifadeyi bulur.
- `rules` konu düzeyinde 0-8 regex; `pattern` Kotlin `Regex` uyumlu,
  `replace` `$1` gruplarını kullanabilir. Her kuralın `note_tr` alanı vardır.

Metin kuralları: HTML/markdown/emoji yok; Türkçe alanlar düz metin; İngilizce
örnekler doğal ve A1 (çoğunlukla ≤10 kelime). Kart gövdesi ≤60 kelime.

### 2.3 Kaynak not kapsamı (21 not → 16 konu)

| Konu | Kaynak notlar |
|---|---|
| `g-a1-to-be` — am / is / are | `a1-u01-l01#0`, `a1-u01-l02#0` |
| `g-a1-how-are-you` — How are you? | `a1-u01-l02#1` |
| `g-a1-names` — Ad sorma/söyleme | `a1-u01-l03#0`, `a1-u02-l01#0` |
| `g-a1-possessives` — my / your | `a1-u01-l03#1`, `a1-u02-l01#1` |
| `g-a1-farewells` — Vedalaşma | `a1-u01-l04#0` |
| `g-a1-origin` — Where are you from? | `a1-u02-l02#0` |
| `g-a1-nationality` — Ülke ve milliyet | `a1-u02-l02#1` |
| `g-a1-jobs` — What do you do? | `a1-u02-l03#0` |
| `g-a1-articles` — a / an | `a1-u02-l03#1` |
| `g-a1-age` — How old are you? | `a1-u02-l04#0` |
| `g-a1-time` — Saat sorma | `a1-u03-l01#0` |
| `g-a1-day-parts` — Günün bölümleri | `a1-u03-l01#1`, `a1-u03-l04#1` |
| `g-a1-present-simple` — Present simple | `a1-u03-l02#0`, `a1-u03-l04#0` |
| `g-a1-frequency` — Sıklık zarfları | `a1-u03-l02#1` |
| `g-a1-work-study` — work at / study at | `a1-u03-l03#0` |
| `g-a1-do-questions` — Do you ...? | `a1-u03-l03#1` |

`linked_lessons`, ilgili derslerde "Gramer" bağlantısı için kullanılır;
v1'de yalnız index'te taşınır (ders ekranı değişmez).

## 3. Kontrol motoru (çevrimdışı)

`core/grammar/GrammarChecker` saf Kotlin'dir; Android/Room/AI bağımlılığı yoktur.
Aynı girdi her zaman aynı sonucu üretir.

### 3.1 Sonuç modeli

```kotlin
enum class GrammarIssueKind { GRAMMAR, SPELLING, CAPITALIZATION, PUNCTUATION, MISSING }

data class GrammarIssue(
    val start: Int,          // orijinal cümlede karakter aralığı
    val end: Int,
    val wrong: String,
    val correct: String,
    val noteTr: String,
    val kind: GrammarIssueKind,
)

data class GrammarCheckResult(
    val original: String,
    val corrected: String,
    val issues: List<GrammarIssue>,
    val correct: Boolean,     // issues.isEmpty()
    val scorePercent: Int,    // 100 - ceza, 0..100
    val hintTr: String? = null, // eksik yapı vb. (kırmızı aralık yoksa)
)
```

### 3.2 Genel kontroller (her konuda)

1. **Büyük harf:** İlk harf küçükse `CAPITALIZATION`; düzeltilmiş cümlede büyütülür.
2. **Noktalama:** Cümle `.`/`?`/`!` ile bitmiyorsa `PUNCTUATION`; soru
   işareti beklenen görevde `?` eklenir.
3. **Tekrarlanan kelime:** `the the` → tek kelime; `PUNCTUATION` değil
   `GRAMMAR` sayılır, not: "Kelime tekrarını kaldır."
4. **Çift boşluk:** düzeltilir, issue üretilmez (sessiz).
5. **Türkçe karakter:** `ı ş ğ ü ö ç` içeren kelime varsa `hintTr`:
   "İngilizce yazmayı dene." Cümle tamamen Türkçeyse skor 0.
6. **Yazım:** `SpellingLexicon` ile; aşağıya bak.
7. **Kısa cümle:** `min_words` altındaysa `hintTr`, skor 0; kırmızı aralık yok.

### 3.3 Konu kuralları

- `rules` sırayla uygulanır. Her eşleşme `GRAMMAR` issue üretir:
  `wrong` = eşleşen metin, `correct` = `replace` uygulanmış hali, `note_tr`.
- Aynı aralığa denk gelen sonraki kural atlanır (çakışma yok).
- Düzeltilmiş cümle, çakışmayan değişiklikler sondan başa uygulanarak üretilir.
- `expect`: metin `pattern` ile eşleşmiyorsa `anchor` aranır. Bulunursa o aralık
  `MISSING` issue olur (`wrong` = anchor metni, `correct` = anchor,
  `note_tr` = expect notu). Bulunamazsa yalnız `hintTr` gösterilir.
- Görev bazlı `expect` kontrolü skoru düşürür (her eksik -20).

### 3.4 Puanlama

```
score = 100
- her GRAMMAR/MISSING: -20
- her SPELLING: -15
- her CAPITALIZATION/PUNCTUATION: -5
- görev expect eksiği: -20
min 0
```

`correct = issues.isEmpty() && hintTr == null`. Görev geçme eşiği yok; kullanıcı
"Sonraki" ile ilerleyebilir. Konu skoru 3 görevin ortalamasıdır.

### 3.5 Yazım sözlüğü

`SpellingLexicon` iki kaynaktan beslenir:

1. `assets/course/**` içindeki tüm `vocab.term`, `example`, `example_tr`,
   `teach`, `grammar`, `dialogue`, `exercises` metinlerindeki İngilizce kelimeler.
2. `assets/dictionary/en_tr.json` içindeki `term` alanları.
3. `assets/grammar/**` içindeki tüm İngilizce örnek/örnek cümleler.
4. Kotlin içinde sabit **function words** listesi: zamirler, artikel, yardımcı
   fiiller, edatlar, bağlaçlar, soru kelimeleri, sık zarflar, sayılar, gün/ay,
   aile ve temel A1 kelimeleri (~250 kelime).

Kurallar:

- Token'lar `[A-Za-z']+`; kesme işaretli biçimler normalize edilir
  (`I'm` → `I am` gibi) ve bilinen contraction listesi kabul edilir.
- Çekimler için basit gövde denemesi: `-s`, `-es`, `-ies`, `-ed`, `-ing`,
  çift ünsüz, `y→i`. Gövde sözlükteyse doğru sayılır.
- Bilinmeyen token yalnız şu durumda `SPELLING` olur:
  - tamamen harf, uzunluk ≥3,
  - cümle başında değilse büyük harfle başlayan özel ad değil,
  - sözlükteki bir kelimeye Levenshtein mesafesi ≤2 (uzunluk ≤4 için ≤1),
  - veya yaygın yazım hatası haritasında (`im`, `dont`, `cant`, `wont`,
    `thats`, `alot`, `becuase`, `tommorow`, `studnt` ...).
- Öneri: en küçük mesafeli sözlük kelimesi; eşitlikte daha sık olan (sabit
  sıralama) seçilir.
- Emin olunamayan bilinmeyen kelime **işaretlenmez** (yanlış pozitif yok).

## 4. İlerleme ve kayıt

- Yeni Room tablosu **yok**. Mevcut `lesson_progress` kullanılır:
  - `lessonId = "grammar:<topicId>"`
  - `unitId = "grammar"`
  - `status = in_progress|completed`
  - `bestScore`, `lastScore`, `attempts`, `completedAt`, `completedEpochDay`.
- Konu açılınca `markOpened`, 3 görev bitince `completeLesson` çağrılır.
  Bu, günlük seriye ve "bugün tamamlanan" sayacına katkı verir.
- Yedek v4 aynen çalışır; `lesson_progress` zaten yedekte.
- `CoursePath`/`unitCompleted` etkilenmez; `grammar:` kimlikleri yalnız
  Gramer ekranı tarafından okunur.

## 5. AI (opsiyonel)

Yerel kontrol her zaman çalışır. AI anahtarı varsa sonuç kartında ikincil
**"AI ile derin kontrol"** düğmesi görünür. AI yoksa/başarısızsa yerel sonuç
geçerlidir; hata kullanıcıya "AI kontrolü yapılamadı; yerel kontrol geçerli."
olarak gösterilir.

Prompt (yalnız JSON):

```
Kullanıcının İngilizce cümlesini A1 seviyesinde kontrol et. Konu: {topic}.
Yalnızca JSON döndür:
{"has_error": true, "corrected": "...", "issues": [{"wrong": "...", "correct": "...", "note_tr": "..."}], "verdict_tr": "..."}
Kurallar: corrected tek cümle; issues en fazla 4; note_tr en fazla 12 kelime;
yazım hatalarını da düzelt; emin olmadığın şeyi uydurma; JSON dışında metin yazma.
Cümle: ...
```

- `AiRepository.checkGrammar(text, topicTitleTr, level, sessionId)` yeni public
  suspend fonksiyon; `client.streamChat` + `PromptBuilder` kullanır.
- `temperature = 0.2`, `maxTokens = 300`; oturum DB'ye yazılmaz.
- Parse `GrammarAiResult(hasError, corrected, issues, verdictTr)`; JSON
  çözülemezse `Result.failure`.
- AI sonucu ayrı kartta gösterilir; yerel skoru değiştirmez. AI'ın
  `corrected` cümlesi yeşil, `issues[].wrong` kelimeleri kırmızı gösterilir.

## 6. UI sözleşmesi

- Mevcut `TekrarTheme`, `LearnCard`, `LearnPrimaryButton`, `LearnProgressBar`
  kullanılır; yeni palet yok. Gramer vurgusu için mevcut `primary`/`secondary`
  ve `tertiary` yeterli.
- Alt menü 5 öğe: **Öğren · Gramer · Bugün · Sözlük · Profil**. Yeni
  `NavKind.GRAMMAR` ikonu (kitap/açık kitap ya da "A" harfi) çizilir.
- Gramer ana ekranı:
  - Başlık + kısa açıklama.
  - İlerleme kartı: `X / 16 konu`, ince çubuk, "Devam et" CTA.
  - Konu kartları: başlık, alt başlık, süre, durum rozeti (Tamam/Devam/Açık).
  - Kartlar tek sütun; 16 konu için bölüm başlıkları (Temel, Tanışma,
    Günlük rutin).
- Konu ekranı:
  - Üstte geri + başlık + `adım/toplam` göstergesi (noktalar veya çubuk).
  - Ortada tek kart; `LearnCard` içinde tip etiketi, başlık, gövde.
  - `rule` kartında `pattern` vurgulu bir şerit; `mistake` kartında
    kırmızı `✗` / yeşil `✓` satırları; örneklerde ses düğmesi.
  - Altta "Devam" / "Yazmaya geç".
- Yazma ekranı:
  - Görev `1/3`, prompt, ipucu, çok satırlı metin alanı.
  - "Kontrol et" -> sonuç kartı:
    - `Yazdığın cümle`: hatalı aralıklar kırmızı + altı çizili.
    - `Doğrusu`: yeşil düzeltilmiş cümle.
    - Hata satırları: `I is → I am` + Türkçe not.
    - "Tekrar dene" + "Sonraki cümle".
  - 3. görevden sonra özet kartı: `3/3`, doğru sayısı, hatalar, "Konu tamam".
- Erişilebilirlik: dokunma hedefi ≥48dp, gövde 16sp, kırmızı/yeşil yalnız
  renkle anlatılmaz (✗/✓ ve altı çizgi de var).
- Tüm metinler Türkçe; `res/values/strings_grammar.xml` ayrı dosya.
- `strings_nav.xml` içine yalnız `nav_grammar` eklenir.

## 7. Navigasyon ve DI

- `TekrarNavHost.kt` (lead):
  - `ROUTE_GRAMMAR = "grammar"`, `ROUTE_GRAMMAR_TOPIC = "grammar/topic"`.
  - `tabs = listOf(ROUTE_LEARN, ROUTE_GRAMMAR, ROUTE_TODAY, ROUTE_LIBRARY, ROUTE_PROFILE)`.
  - Gramer root -> `GrammarHomeScreen`; topic -> `GrammarTopicScreen`.
  - `GrammarHomeViewModel(GrammarRepository, GrammarProgressRepository)`.
  - `GrammarTopicViewModel(GrammarRepository, GrammarProgressRepository, AiRepository, Speaker)`.
- `TekrarApp.kt` (lead): `grammarRepository` lazy; mevcut `courseRepository`,
  `courseProgress`, `aiRepository` yeniden kullanılır.
- `Components.kt` (UI lane): `NavKind.GRAMMAR` + ikon + `navKinds` 5 öğe.
- Geri tuşu: konu ekranından Gramer ana ekranına; sekme değişiminde durum korunur.

## 8. Doğrulama

- `python3 tools/content/validate.py`:
  - mevcut kurs doğrulaması aynen,
  - `grammar/index.json` + 16 konu şema doğrulaması,
  - regex derleme kontrolü (Python `re`; Kotlin uyumu için basit desenler),
  - `source_notes` kapsamı: 21 notun tamamı en az bir konuda,
  - `write.tasks` 3, `steps` 3-6, metin sınırları, emoji/HTML yok.
- JVM testleri:
  - `GrammarParserTest`: index/topic parse, eksik alan hataları.
  - `GrammarCheckerTest`: aşağıdaki örnekler + en az 20 vaka.
  - `GrammarSessionTest`: 3 görev akışı, skor ortalaması, tamamlanma.
  - `GrammarAiTest`: prompt/JSON parse, hatalı JSON.
- CI: `python3 tools/content/validate.py` + `gradle testDebugUnitTest
  assembleDebug assembleRelease`.
- Cihaz testi: 5 sekme, bir konu anlatımı, 3 cümle, kırmızı/yeşil işaretleme,
  AI düğmesi (anahtar varsa), konu tamamlama ve ilerleme.

### Zorunlu checker vakaları

| Girdi | Beklenen |
|---|---|
| `I is a student.` | `I am a student.` · `is→am` |
| `He are tired.` | `He is tired.` · `are→is` |
| `i am happy` | `I am happy` · büyük harf |
| `I am happy` | 100, issue yok |
| `I amm happy.` | `amm→am` yazım |
| `She go to school.` | `go→goes` |
| `Does he likes tea?` | `likes→like` |
| `There is two cats.` | `is→are` |
| `I can to swim.` | `to` silinir |
| `a engineer` | `a→an` |
| `an teacher` | `an→a` |
| `I from Turkey.` | `I am from Turkey.` |
| `How you are?` | `How are you?` |
| `I have 20 years.` | `I am 20 years old.` |
| `in night` | `at night` |
| `I name is Ali.` | `My name is Ali.` |
| `You like tea?` | `Do you like tea?` |
| `I am student.` (articles konusu) | `I am a student.` |
| `I am Deniz` | noktalama eklenir |
| `Ben öğrenciyim.` | Türkçe uyarısı, skor 0 |

## 9. Lane sahipliği

| Lane | Dosyalar | Dokunmaz |
|---|---|---|
| Core | `core/grammar/**`, `repo/GrammarRepository.kt`, `repo/GrammarProgressRepository.kt`, `ai/GrammarAi.kt`, `ai/PromptBuilder.kt` (yalnız ekleme), `ai/AiRepository.kt` (yalnız ekleme), `app/src/test/**/grammar/**` | UI, nav, content, validator |
| Content | `assets/grammar/**`, `tools/content/validate_grammar.py` | Kotlin, nav, validator çekirdeği |
| UI | `ui/grammar/**`, `ui/components/Components.kt` (yalnız NavKind/ikon), `res/values/strings_grammar.xml` | nav, DI, DB, content, core |
| Lead | `TekrarNavHost.kt`, `TekrarApp.kt`, `tools/content/validate.py`, `strings_nav.xml`, `build.gradle.kts`, CI, docs, sürüm | lane dosyaları (birleştirme hariç) |

- Lane'ler ayrı worktree'lerde: `$PREFIX/tmp/opencode/tekrar-grammar-{core,content,ui}`.
- Her lane kendi testini/validator çalıştırmasını yazar ve raporlar.
- Lead diff + tam doğrulama yapar.

## 10. Kabul kriterleri

- [ ] 5. sekme Gramer görünür; 16 konu listelenir; 5 sekme taşmaz.
- [ ] Her konu 3-6 kart + 3 yazma görevi; kartlar tek ekran, ≤60 kelime.
- [ ] Kullanıcı 3 cümle yazar; her cümle anında kontrol edilir.
- [ ] Hatalı kelimeler kırmızı + altı çizili; doğrusu yeşil gösterilir.
- [ ] En az 18 checker testi geçer; yanlış pozitif üretmez.
- [ ] AI anahtarı yokken tüm akış çalışır; AI düğmesi görünmez.
- [ ] `lesson_progress` kaydı `grammar:<id>` olarak yazılır; yedek uyumlu.
- [ ] `python3 tools/content/validate.py` 0 hata; CI yeşil; APK 3.1.0.
