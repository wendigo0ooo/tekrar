# Dil Okulu Kontratı (v1)

Tekrar'ı Türkçe arayüzlü bir İngilizce öğrenme uygulamasına dönüştürme sözleşmesi.
Bu dosya, M0 sonrası tüm lane'lerin tek kaynağıdır. Çelişki olursa bu dosya kazanır.

- Hedef: TR arayüz, TR→EN, A1→A2 içerik; ders + FSRS tekrar + gerçek zamanlı AI koç.
- Paket/veri/CI korunur: `com.wendigo.tekrar`, Room v4 → v5, mevcut kelimeler ve tekrar geçmişi korunur.
- Sürüm hedefi: 3.0.0 (versionCode 4). Etiket şimdilik **Tekrar**.
- Kural: dersler/tekrar tamamen çevrimdışı; AI isteğe bağlı ve internet gerektirir.
- Kural: kalp/enerji/lig/paywall/reklam yok. Seri + günlük/haftalık hedef var.

## 1. Ürün ve navigasyon

4 sekme (alt bar):

1. **Öğren** — ünite yolu, sıradaki ders, günlük hedef, AI pratik kartı, bekleyen tekrar kartı.
2. **Pratik** — günlük FSRS tekrarı + AI koç (serbest sohbet, rol yapma, cümle düzeltme) + telaffuz/okuma/dinleme.
3. **Sözlük** — mevcut kütüphane, kelime ekleme, metinden kelime çıkarma.
4. **Profil** — istatistik, seri/hedef, yedekleme, AI ayarları, hakkında.

Ana ekran kuralı: tek birincil CTA ("Devam et" / "Derse başla"). Aynı ekranda 3'ten fazla büyük kart olmasın.

### Ders akışı

- 3-6 dakika, 8-12 etkileşimli alıştırma + 0-2 öğretim kartı.
- Sıra: öğretim → tanıma → kontrollü üretim → serbest üretim → kısa kapanış.
- Tek görev/ekran; süre baskısı yok; çıkışta ilerleme korunur.
- Her cevaptan sonra anında geri bildirim; yanlışta doğru cevap + kısa Türkçe açıklama.
- Ders sonu: doğruluk, öğrenilen kelimeler, "Tekrara eklendi" bilgisi, sıradaki adım.
- Ders tamamlanınca o dersin kelimeleri FSRS'e `course:<unit>` destesiyle eklenir (varsa dokunulmaz).

### Ünite/ilerleme

- Ünite içinde dersler sıralı açılır; ünite, önceki ünite tamamlanınca açılır.
- Ünite sonunda rol yapma senaryosu; tam A1'de ayrıca ustalık dersi olacak.
- Durumlar: `locked`, `available`, `in_progress`, `completed`, `mastered`.
- İlk sürümde "seviye atla" yok; placement testi sonraki faza.

## 2. İçerik yapısı

Kök: `app/src/main/assets/course/`

```
course/
  index.json
  a1/
    u01/
      unit.json
      l01.json ... l04.json
      rp01.json
    u02/...
    u03/...
```

### 2.1 Kök index (`index.json`)

```json
{
  "schema": 1,
  "language": "en",
  "native": "tr",
  "levels": [
    {
      "id": "a1",
      "title": "A1 · Başlangıç",
      "units": [
        { "id": "a1-u01", "file": "a1/u01/unit.json" },
        { "id": "a1-u02", "file": "a1/u02/unit.json" },
        { "id": "a1-u03", "file": "a1/u03/unit.json" }
      ]
    }
  ]
}
```

Yollar kök dizine göredir. `levels` en az bir seviye, her seviye en az bir ünite içerir.

### 2.2 Ünite (`unit.json`)

```json
{
  "schema": 1,
  "id": "a1-u01",
  "cefr": "A1",
  "title": "Selamlaşma",
  "subtitle": "Merhaba, nasılsın?",
  "icon": "wave",
  "color": "#4F8EF7",
  "lessons": [
    { "id": "a1-u01-l01", "title": "Merhaba", "file": "l01.json", "est_min": 4 }
  ],
  "roleplay": { "id": "a1-u01-rp", "title": "İlk tanışma", "file": "rp01.json", "est_min": 5 }
}
```

`lessons` en az 1, sıralı; `file` ünite dizinine göredir. `roleplay` opsiyonel.

### 2.3 Ders (`lNN.json`)

```json
{
  "schema": 1,
  "id": "a1-u01-l01",
  "unit": "a1-u01",
  "cefr": "A1",
  "title": "Merhaba",
  "objective_tr": "Selamlaşma ve vedalaşma ifadelerini kullan.",
  "est_min": 4,
  "vocab": [
    {
      "term": "hello",
      "meaning_tr": "merhaba",
      "pos": "ünlem",
      "example": "Hello, how are you?",
      "example_tr": "Merhaba, nasılsın?",
      "ipa": "həˈloʊ"
    }
  ],
  "grammar": [
    {
      "title_tr": "am / is / are",
      "explanation_tr": "Kendini tanıtırken 'I am ...' kullanılır.",
      "examples": [{ "en": "I am Anna.", "tr": "Ben Anna." }]
    }
  ],
  "dialogue": {
    "title": "İlk karşılaşma",
    "lines": [
      { "speaker": "Anna", "en": "Hi! I'm Anna.", "tr": "Merhaba! Ben Anna." }
    ]
  },
  "teach": [
    {
      "title_tr": "Selamlaşma",
      "body_tr": "Tanıdık biriyle karşılaşınca 'Hello' veya 'Hi' denir.",
      "examples": [{ "en": "Hello!", "tr": "Merhaba!" }],
      "tip_tr": "'Hi' daha samimi, 'Hello' daha nötrdür."
    }
  ],
  "exercises": [
    {
      "type": "choice",
      "prompt_tr": "'hello' ne demek?",
      "options": ["merhaba", "hoşça kal", "teşekkürler"],
      "answer": 0,
      "explanation_tr": "'Hello' merhaba demektir."
    }
  ]
}
```

Zorunlu alanlar: `schema`, `id`, `unit`, `cefr`, `title`, `objective_tr`, `est_min`, `vocab`, `teach`, `exercises`.
`grammar` ve `dialogue` opsiyonel.

Sınırlar (v1):

- `est_min` 3-6; `vocab` 5-10; `teach` 0-2; `exercises` 8-12; `grammar` 0-2; `dialogue.lines` 4-8.
- `vocab.term`, `meaning_tr`, `pos`, `example`, `example_tr` boş olamaz. Ders içinde `term` tekrarı yok.
- `teach` kartında `title_tr`, `body_tr`, en az 1 örnek.
- `grammar` kartında `title_tr`, `explanation_tr`, en az 1 örnek.
- Tüm `prompt_tr`, `title_tr`, `body_tr`, `explanation_tr`, `meaning_tr`, `tr` alanları Türkçe ve düz metin.
- HTML/markdown yok; düz metin. Emoji yok.
- İngilizce hedef cümleler doğal ve kısa (A1: çoğunlukla ≤8 kelime).

### 2.4 Alıştırma tipleri (v1)

| Tip | Alanlar | Puanlama |
|---|---|---|
| `choice` | `prompt_tr`, `options` (2-4), `answer` (index), `explanation_tr?` | Tek doğru |
| `listen_choice` | `prompt_tr`, `audio_text`, `options`, `answer`, `explanation_tr?` | Tek doğru; `audio_text` sistem TTS ile okunur |
| `dialogue_choice` | `prompt_tr`, `audio_text?`, `options`, `answer`, `explanation_tr?` | Tek doğru |
| `match` | `prompt_tr`, `pairs` (3-5 `{left,right}`) | Tüm eşleşmeler doğruysa |
| `word_order` | `prompt_tr`, `tokens` (3-8), `answer` (string dizisi), `translation_tr?` | Token multiset'i aynı ve sıra doğruysa |
| `cloze` | `prompt_tr`, `sentence` (tek `___`), `options`+`answer` VEYA `answers` | Seçenekli veya yazmalı |
| `translate` | `prompt_tr`, `answers` (1-3 kabul), `hint_tr?` | Normalize edilmiş eşleşme |
| `speak` | `prompt_tr`, `target`, `hint_tr?` | STT metni + kelime benzerliği; skor "benzerlik" olarak gösterilir |
| `write` | `prompt_tr`, `min_words` (5-30), `keywords?`, `sample?`, `rubric_tr?` | min_words + anahtar kelime; AI geri bildirimi opsiyonel, geçişi engellemez |

Kurallar:

- `choice`/`listen_choice`/`dialogue_choice`: `answer` 0..options.size-1.
- `match`: `left` ve `right` boş olamaz; aynı ders içinde birebir eşleşme.
- `word_order`: `tokens` karışık verilir; `answer` doğru sıradır; ikisinin multiset'i eşit olmalı.
- `cloze`: `sentence` içinde tam olarak bir `___` olmalı. Seçenek varsa `options` 2-4 ve `answer`; yoksa `answers` 1-3.
- `translate`: `answers` en az 1; büyük/küçük harf, noktalama ve baştaki/sondaki boşluk normalize edilir; kabul edilen varyantlar yazılır.
- `speak`: `target` en fazla 12 kelime; cihazda STT yoksa "yazarak devam et" fallback'i gösterilir.
- `write`: `min_words` dışında katı dilbilgisi puanı yok; AI yalnız yorum verir.

Puanlama/normalizasyon (v1):

- `AnswerCheck.matches` doğrudan kullanılmaz; ders motoru kendi deterministik `ExerciseGrader`'ını kullanır.
- Normalizasyon: NFC, `Locale.ROOT`, baştaki/sondaki boşluk, çoklu boşluk, büyük/küçük harf, düz/akıllı apostrof ve tire eşitleme.
- Kelime sırası, cloze ve dilbilgisi hedeflerinde yazım toleransı **0**; yalnız `translate` için tam eşleşme + `answers` varyantları. Levenshtein/typo toleransı yok.
- `write` ve `speak` geçişi deterministik ön koşula bağlıdır (min kelime / benzerlik eşiği); AI değerlendirmesi geçişi engellemez, yalnız geri bildirim verir.
- Her ders tamamlanabilir olmalı; AI kapalıyken de tüm alıştırmalar çalışır.

### 2.5 Rol yapma (`rpNN.json`)

```json
{
  "schema": 1,
  "id": "a1-u01-rp",
  "unit": "a1-u01",
  "cefr": "A1",
  "title": "İlk tanışma",
  "setting_tr": "Bir arkadaşının partisinde yeni biriyle tanışıyorsun.",
  "role_en": "Sam, a friendly new acquaintance at a party.",
  "goal_tr": "Selamlaş, adını söyle, nereden geldiğini sor.",
  "opening_en": "Hi! I'm Sam. Great party, right?",
  "target_phrases": ["Hi", "My name is ...", "Where are you from?"],
  "suggested_turns": 6,
  "success_criteria_tr": ["Selamlaştın", "Adını söyledin", "Karşı tarafın adını sordun"]
}
```

Kurallar: `target_phrases` 2-6; `suggested_turns` 4-10; `opening_en` tek cümle; `role_en` İngilizce; diğer alanlar Türkçe.

## 3. İçerik üretim kuralları

Ayrıntı: `docs/dil-okulu-icerik.md`. Özet:

- Dil bilgisi Türkçe ve kısa (en fazla 3 cümle); örnekler İngilizce + Türkçe.
- Her derste en az 2 üretim alıştırması (`speak`, `write` veya `translate`).
- Aynı kelimeyi aynı derste 2'den fazla MCQ ile sorma; tipleri karıştır.
- Kelime örnekleri günlük hayattan; kültürel/tartışmalı içerik yok.
- A1'de basit şimdiki zaman; A2'de geçmiş/gelecek/karşılaştırma.
- Ünite başına 20-30 yeni öğe hedefi (tam sürümde); dikey dilimde ders başına 5-10.

## 4. Veri modeli (Room v5)

Mevcut: `words`, `review_logs`. Eklenecek:

### `words` ek alanı (v5)

Kurs kelimesinin kimliği `term` metnine bırakılmaz. `words` tablosuna nullable `sourceKey` eklenir:

- Biçim: `course:<lessonId>:<term-casefold>` (örn. `course:a1-u01-l01:hello`).
- `sourceKey` nullable + **unique index**; SQLite çoklu NULL'a izin verir, mevcut kullanıcı kelimeleri etkilenmez.
- Aynı `term` zaten varsa (TermKey) yeni kayıt açılmaz; mevcut kelime korunur, yalnız `course` etiketi eklenir.
- Çok anlamlı kelime politikası v1: tek kart; ders anlamı mevcut kartla çelişirse kartın anlamı ezilmez, ders notu `notes` alanına eklenmez. (İleride `senseKey` ayrı iş.)
- Kurs tohumlaması `seedIfEmpty()` içine bağlanmaz; ayrı `CourseSeeder` + `contentVersion` ile çalışır. Ders tamamlanınca yalnız o dersin kelimeleri eklenir; tekrar çalıştırma idempotenttir.

### `lesson_progress`

| Alan | Tip | Not |
|---|---|---|
| `lessonId` | TEXT PK | `a1-u01-l01` |
| `unitId` | TEXT | index |
| `status` | TEXT | locked/available/in_progress/completed/mastered |
| `bestScore` | INT | 0-100 |
| `lastScore` | INT | 0-100 |
| `attempts` | INT | |
| `completedAt` | INTEGER? | epoch ms |
| `completedEpochDay` | INTEGER? | tek gün anahtarı; seri için |
| `contentVersion` | TEXT? | içerik sürümü |
| `lastOpenedAt` | INTEGER? | epoch ms |
| `updatedAt` | INTEGER | epoch ms |

İndeks: `unitId`, `status`.

### `ai_session`

| Alan | Tip | Not |
|---|---|---|
| `id` | TEXT PK | `ses_...` |
| `mode` | TEXT | chat/roleplay/correction |
| `unitId` | TEXT? | |
| `scenarioId` | TEXT? | |
| `title` | TEXT | |
| `model` | TEXT | |
| `createdAt` | INTEGER | |
| `updatedAt` | INTEGER | |
| `summaryJson` | TEXT? | oturum sonu özeti |
| `costMicros` | INTEGER | 0 ise bilinmiyor |
| `promptVersion` | TEXT | prompt şablon sürümü |
| `messageCount` | INTEGER | |

### `ai_message`

| Alan | Tip | Not |
|---|---|---|
| `id` | INTEGER PK autogenerate | |
| `sessionId` | TEXT indexed | |
| `role` | TEXT | user/assistant/system |
| `content` | TEXT | |
| `correctionJson` | TEXT? | düzeltme kartı |
| `turnIndex` | INTEGER | oturum içi sıra |
| `status` | TEXT | pending/streaming/done/error |
| `tokens` | INTEGER? | varsa |
| `promptVersion` | TEXT? | |
| `createdAt` | INTEGER | |

Kurallar:

- Migration `4→5`: `ALTER TABLE words ADD COLUMN sourceKey TEXT`; üç yeni tablo; indeksler (`words.sourceKey` unique, `lesson_progress.unitId`, `lesson_progress.status`, `ai_message.sessionId`). Mevcut satırlar korunur.
- `exportSchema = true` + `schemas/` dizini repoda; CI'da schema dosyası üretilir. Cihazda v2.0 → 3.0 yükseltme testi zorunlu (kullanıcı yapar); migration SQL'i ayrıca incelenir.
- Yedek formatı **v4**: `words`, `review_logs`, ayarlar, `lesson_progress`. AI oturumları yedeğe girmez (yerel geçmiş). Eski v3 yedeği okunur, ders ilerlemesi boş gelir.
- Geri yükleme all-or-nothing: tek transaction; hata olursa mevcut veri korunur. Yedek sürümü/alan doğrulaması yükleme öncesi yapılır.
- Silme: AI geçmişini temizleme ayarı tüm `ai_message` + `ai_session` kayıtlarını siler.
- Sürüm: `versionCode 4`, `versionName 3.0.0`; CI release etiketi `v3.0-build<N>` olarak güncellenir.

### FSRS / seri

- Ders tamamlanınca `vocab` kayıtları `WordRepository` üzerinden eklenir:
  `deck = "course:<unitId>"`, `cefr`, `pos`, `example`, `exampleTr`, `tags = "course,<unitId>"`, `sourceKey`.
- Var olan kelime (TermKey) atlanır; mevcut FSRS planı bozulmaz.
- Kurs kelimeleri normal FSRS kuyruğuna girer; günlük yeni kart kotası paylaşılır. Ders başına 5-10 kelime olduğu için v1'de ayrı kota yok; `Pratik` ekranı `course:` destesine göre filtreleme sunar ve yığılma görülürse ayrı kota eklenir.
- Seri: `review_logs` günleri ∪ `lesson_progress.completedEpochDay` günleri. Tek gün anahtarı `StatsMath.dayKey`/`startOfDay` üzerinden; mevcut 04:00 gün sınırı borcu bu sürümde değiştirilmez.
- Günlük hedef mevcut `dailyGoal` (tekrar kartı) olarak kalır; Öğren ekranında ayrıca ders sayısı gösterilir. Ders hedefi ayrı bir ayar olarak sonraki fazda eklenir.
- Haftalık hedef aynı aktivite birleşimini kullanır.
- AI hiçbir ders geçme/kalma, seri veya kilit kararını etkilemez; ders motoru tamamen deterministiktir.

## 5. AI sözleşmesi

### 5.1 Sağlayıcı

- Varsayılan: **OpenCode Go** (OpenAI uyumlu).
- Base URL: `https://opencode.ai/inference/go`
- Chat: `POST /openai/v1/chat/completions`
- Auth: `Authorization: Bearer oc_sk_...`
- Zorunlu başlıklar: `Content-Type: application/json`, `x-opencode-session: ses_<uuid>`, `User-Agent: Tekrar/3.0 (Android)`.
- Model varsayılanı: `gpt-6-luna` (hızlı). Seçenekler: `glm-5.3-flash`, `deepseek-v4.1-flash`, `minimax-m3.1-flash`. Endpoint gerçek anahtarla doğrulandı (HTTP 200; ilk token ~1.2 sn / GPT-6 Luna). Model listesi uzaktan yapılandırılabilir olmalı; kimlik koda gömülmez.
- İstek: `stream: true`, `max_tokens` 120-200 (sohbet), 250 (düzeltme), 500 (özet); `temperature` 0.6 (sohbet), 0.2 (düzeltme/özet).
- Anahtar uygulama ayarlarından girilir; APK içine gömülmez, repoya yazılmaz.

### 5.2 Streaming

- SSE satırları `data: ...`; `[DONE]` bitiş.
- `choices[0].delta.content` ekrana akar; `reasoning_content` gösterilmez (varsa yalnız token sayımı).
- Hata gövdesi JSON ise `error.message` gösterilir; ham gövde loglanmaz.
- Timeout: bağlantı 15 sn, okuma 60 sn; ekran kapanınca/iptal edilince istek iptal.
- 401: "Anahtar geçersiz"; 429: "Kota/limit"; ağ: "İnternet yok"; timeout: "Yanıt gecikti".
- 429/5xx için 1 kez geri çekilme (1.5 sn); 401'de retry yok.
- Düzeltme çağrısı ana yanıttan **sonra** başlar; kullanıcı yeni tur başlatırsa eski düzeltme iptal edilir. Düzeltme kartı yanıt bitmeden gösterilmez.
- Düzeltme cache: normalize edilmiş kullanıcı mesajı + prompt sürümü anahtarı; aynı mesaj için ikinci kez çağrı yapılmaz.
- Streaming sırasında her delta DB'ye yazılmaz; yanıt tamamlanınca tek `ai_message` satırı yazılır. Hata olursa `status=error` + kısmi metin korunur.
- AI katmanı `AiCoach` arayüzü arkasında; `NoopAiCoach` ile anahtarsız/çevrimdışı çalışır. AI yokken ders/tekrar/seri tam çalışır; yalnız AI ekranları "anahtar gerekli" der.

### 5.3 Promptlar

Tutor (sohbet/rol yapma) system promptu şunları içerir:

- Rol: "Deniz", sabırlı İngilizce öğretmeni; öğrenci Türk.
- Seviye (A1/A2), senaryo, hedef ifadeler, hedef kelimeler.
- Yanıt en fazla 2 kısa cümle + 1 soru; öğrenciyi konuştur.
- Hataları akışı kesmeden doğal biçimde tekrar ifade et (recast); gramer dersi verme.
- Öğrenci Türkçe sorarsa kısa Türkçe ipucu ver, sonra İngilizceye dön.
- Kişisel veri isteme; senaryodan çıkma; uygunsuz içerik yok.

Düzeltme çağrısı (ayrı, kısa):

```
Kullanıcının İngilizce mesajını değerlendir. Yalnız JSON döndür:
{"has_error": bool, "corrected": "düzeltilmiş cümle", "note_tr": "kısa Türkçe açıklama", "severity": "low|medium|high"}
Hata yoksa has_error=false, corrected=orijinal, note_tr="".
```

Özet çağrısı (oturum sonu):

```
Yalnız JSON döndür:
{"strengths": ["..."], "fixes": ["..."], "next_focus": "...", "cefr_note": "A1"}
strengths 2-3, fixes 2-3, next_focus tek cümle.
```

Özet kuralı: yalnız AI'nın serbest yorumu değil; ders/alıştırma kayıtlarından deterministik bir taslak çıkarılır (doğru/yanlış sayısı, düzeltme kartları, hedef ifadeler). AI bu taslağı cümleleştirir; JSON şeması doğrulanır, 1 retry, olmazsa kural tabanlı özet gösterilir. Hata uydurma riski bu yüzden azaltılır.

### 5.4 Anahtar saklama

- Android Keystore AES/GCM; alias `tekrar_ai_key`; prefs `ai_secure`.
- Kayıt biçimi: `base64(iv):base64(ciphertext)`; anahtar yoksa kullanıcıdan istenir.
- Keystore hatasında düz metne düşme yok; "anahtar saklanamadı" hatası.
- Anahtar hiçbir log, hata mesajı, yedek veya analitikte yer almaz.

### 5.5 Ses

- TTS: sistem `TextToSpeech` (en-US); hız ayarı mevcut `speechRate` ile.
- STT: `SpeechRecognizer` (en-US), her turda yeniden başlat; `RECORD_AUDIO` izni.
- Telaffuz: STT metni ile `target` normalize edilip kelime benzerliği hesaplanır; sonuç "telaffuz benzerliği %X" olarak gösterilir. Fonem skoru iddia edilmez.
- AI sesli yanıt: v1'de ayrı realtime ses yok; TTS ile okunur, tur bazlı.

## 6. UI sözleşmesi

- Mevcut `TekrarTheme` kullanılır; yeni renk paleti icat edilmez.
- Min yazı 16sp (yardımcı metin 14sp istisna), dokunma hedefi ≥48dp, yüksek kontrast.
- Öğren ekranı: üstte seri + günlük hedef; "Devam et" kartı; ünite yolu; AI kartı; tekrar kartı.
- Ders ekranı: tam ekran; üstte ilerleme + kapat; ortada tek görev; altta ana buton; geri bildirim alt kart.
- AI sohbet: başlık (persona/senaryo), mesaj balonları, düzeltme kartı (katlanabilir), giriş + mikrofon + gönder, "oturumu bitir".
- Ayarlar: sağlayıcı, anahtar (maskeli), model, "Bağlantıyı test et", günlük AI limiti, geçmişi temizle.
- Tüm yeni metinler Türkçe; `strings_learn.xml` ve `strings_ai.xml` ayrı dosyalarda (çakışma önleme).
- Navigasyon: her sekme kendi nested graph'ına sahip (Öğren: yol → ünite → ders → sonuç; Pratik: bugün → AI/tekrar; Profil: istatistik/ayarlar). Üst seviye sekmeler arası geçişte ders durumu korunur; dersten çıkışta onay istenir.
- Mevcut deep link'ler korunur: widget/bildirimden gelen `StudyRequest` doğrudan Pratik/tekrar oturumunu açar; Sözlük ekleme ve paylaşım intent'i çalışmaya devam eder.
- `ViewModels.kt` büyütülmez; ders/AI/yol ViewModel'ları kendi dosyalarında. Mevcut `Srs`, `StudySession`, `StudyViewModel` fork'lanmaz; tekrar akışı yeniden kullanılır.

## 7. Lane sahipliği

| Lane | Sahip olduğu dosyalar | Dokunmayacağı |
|---|---|---|
| L1 core | `core/learn/**`, `data/course/**`, `data/LessonProgress.kt`, `data/AiSession.kt`, `data/AiMessage.kt`, ilgili DAO'lar, `app/src/test/**/learn/**` | `TekrarNavHost.kt`, `AppDatabase.kt`, `build.gradle.kts`, UI |
| L2 içerik | `app/src/main/assets/course/a1/uNN/**` | `index.json`, validator, kod |
| L3 AI | `ai/**`, `app/src/test/**/ai/**` | Room, UI, nav |
| L4 UI | `ui/learn/**`, `ui/ai/**`, `ui/components/learn/**`, `res/values/strings_learn.xml`, `strings_ai.xml` | nav, DB, build, mevcut ekranlar |
| Lead | `TekrarNavHost.kt`, `AppDatabase.kt`, `build.gradle.kts`, `AndroidManifest.xml`, `TekrarApp.kt`, `index.json`, validator, CI, docs | lane dosyaları (birleştirme hariç) |

- Lane'ler ayrı kopyalarda çalışır: `$PREFIX/tmp/opencode/tekrar-<lane>`.
- Her lane kendi birim testini/validator çalıştırmasını yazar ve çıktısını raporlar.
- Lead diff + tam doğrulama yapar; lane'ler birbirinin dosyasını düzenlemez.

## 8. Doğrulama

- `python3 tools/content/validate.py` → tüm kurs.
- `python3 tools/content/validate.py --unit app/src/main/assets/course/a1/u01/unit.json` → tek ünite.
- `python3 tools/content/validate.py --lesson tools/content/ornek-ders.json` → tek ders.
- JVM testleri: ders motoru puanlama, normalize, FSRS'e kelime ekleme, seri birleşimi, SSE parser, prompt/JSON parser.
- CI: `gradle testDebugUnitTest assembleDebug assembleRelease`; content validator adımı eklenir.
- Cihaz testi: 3 ünite, ders tamamlama, tekrar kartı, AI anahtar testi, bir sohbet, rol yapma.

## 9. Aşamalar

- **M0 (bu tur):** kontrat, şema, validator, örnek ders, iskelet, AI istemci doğrulaması.
- **M1a:** L1 motor/DB + L3 AI çekirdeği + yalnız U01 içeriği (kalibrasyon; süre/kelime yoğunluğu cihazda ölçülür).
- **M1b:** U02-U03 içerik + L4 Öğren yolu/ders ekranı; lead birleştirme ve tam doğrulama.
- **M2:** Öğren yolu + ders UI + FSRS + APK cihaz testi.
- **M3:** AI sohbet/ayarlar + ses + oturum özeti.
- **M4:** A1 tam (12-16 ünite), sonra A2 (16-20).
- **M5:** cila, erişilebilirlik, cihaz testi, sürüm 3.0.

## 10. Riskler / kararlar

- OpenCode Go "coding agent" kullanımı için konumlandırılmıştır; kişisel kullanımda risk düşük, dağıtımda uygun sağlayıcıya geçilir. Sağlayıcı soyutlaması korunur.
- İlk token ölçümü 1.2-3.7 sn; streaming + kısa yanıt hedefi <2 sn. Tam duplex ses v1 kapsamı dışı.
- İçerik hacmi en büyük risk; validator + örneklem denetimi olmadan içerik birleştirilmez.
- STT cihazda Google uygulamasına bağlı; yoksa yazma fallback.
- Mevcut kullanıcı verisi: Room migration + yedek uyumu test edilir. `exportSchema=true`, schema dosyası repoda; v2.0 → 3.0 cihaz yükseltmesi test edilmeden yayın yok.
- Yedek v4 all-or-nothing; v3 yedeği ders ilerlemesi olmadan geri yüklenir. AI geçmişi yedeğe girmez.
- Kurs kelimeleri normal FSRS kuyruğuna girer; yığılma olursa `course:` filtresi/ayrı kota sonraki turda eklenir.
- `ViewModels.kt` monoliti büyütülmez; yeni ekranlar kendi ViewModel dosyalarında, tekrar akışı yeniden kullanılır.
- İnternet izni eklenir; ayarlardaki "tamamen çevrimdışı" metni güncellenir.
- İlk iki hafta kapsamı: AI spike, içerik şeması/validator, 1 ünite, Room v5 + yedek. Tam A1/A2, görsel yeniden tasarım ve STT bu tura girmez.

## 11. Durum (2026-11-14)

- M0 tamam: kontrat, içerik şeması, validator, örnek ders, AI endpoint doğrulaması.
- L1 tamam ve ana dala alındı: `core/learn` motoru, Room v5 entity/DAO, `Word.sourceKey`, 52 JVM testi (stub ile derlendi).
- İçerik: U01-U03 hazır; 3 ünite, 12 ders, 3 rol yapma, 97 kelime, 130 alıştırma; `validate.py --strict` 0 hata.
- Yedek v4: `lesson_progress` + `words.sourceKey` eklendi; AI geçmişi yedeğe girmiyor.
- AI çekirdeği (L3), AI UI (L5) ve Öğren UI (L4) tamamlandı; NavHost/DI birleşti.
- CI run `43591820961` yeşil; APK 3.0.0 (code 4):
  `/storage/emulated/0/Download/Tekrar-release.apk` (7,8 MB) ve `Tekrar-debug.apk`.
- Sırada: cihaz testi (v2.0→3.0 Room göçü, ders akışı, TTS/STT, AI anahtarı,
  sohbet/rol yapma, 4 sekme) → geri bildirim düzeltmeleri → A1 genişletme + A2.
- **3.1.0 (2026-11-15):** ayrı 5. sekme **Gramer** eklendi; 21 grammar notu 16 konuya
  dönüştürüldü, 73 kart + 48 yazma görevi. Çevrimdışı checker hatalı aralıkları
  kırmızı/altı çizili, doğrusunu yeşil gösterir; opsiyonel AI derin kontrolü var.
  İlerleme `lesson_progress` (`grammar:<topicId>`), yeni Room migration yok.
  Ayrıntı: `docs/gramer-kontrat.md`.
