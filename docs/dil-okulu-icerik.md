# Dil Okulu İçerik Rehberi (v1)

Bu rehber, dikey dilimdeki 3 ünitenin içeriğini üretirken uyulacak kuralları verir.
Şema ve teknik kurallar için `docs/dil-okulu-kontrat.md` geçerlidir.

## Dikey dilim kapsamı

| Ünite | Konu | Dersler | Rol yapma |
|---|---|---|---|
| A1-U01 | Selamlaşma, nasılsın, tanışma, vedalaşma | l01-l04 | rp01: ilk tanışma |
| A1-U02 | Kendini tanıtma, nerelisin, meslek, yaş/sayılar | l01-l04 | rp02: tanışma partisi |
| A1-U03 | Günlük rutin, saatler, sabah/akşam, iş/okul | l01-l04 | rp03: bir gününü anlat |

Her ders 3-6 dakika; 5-10 kelime/ifade; 1-2 gramer notu; 0-2 öğretim kartı; 8-12 alıştırma; varsa 4-8 satırlık diyalog.

## Öğretim ilkesi

1. **Kısa ve somut:** Türkçe açıklama en fazla 3 cümle; ardından İngilizce örnek + Türkçe çeviri.
2. **Kullanım odaklı:** "Bu yapı ne?" değil, "Ne zaman, ne söylerim?" sorusuna cevap ver.
3. **Az ama tekrar:** Yeni kelime 5-10; bunların en az %70'i ders içinde farklı alıştırmalarda tekrar eder.
4. **Üretim:** Her derste en az 2 üretim alıştırması (`speak`, `write` veya `translate`).
5. **Doğal dil:** A1'de kısa cümleler, günlük ifadeler; argo ve tartışmalı konu yok.
6. **Türkçe açıklama, İngilizce hedef:** Soru yönergeleri Türkçe; öğrenilecek cümleler İngilizce.

## Ders akışı (önerilen)

- `teach` 1: hedef ifadeler + 2-3 örnek.
- `choice`/`match`: tanıma.
- `listen_choice`: duyma.
- `word_order`/`cloze`: kontrollü üretim.
- `translate`: anlamdan üretim.
- `speak`: sesli üretim.
- `write`: kişisel üretim.
- Kapanış: `teach` veya kısa özet (opsiyonel).

Aynı tip art arda 3'ten fazla olmasın. MCQ'lar aynı kelimeyi tekrar tekrar sormasın.

## Dil bilgisi kapsamı (dikey dilim)

- U01: `I am / you are`, `my / your`, selamlaşma/vedalaşma, `How are you?`, `Nice to meet you`.
- U02: `What is your name?`, `Where are you from?`, ülkeler/milliyetler, `I am a ...`, sayılar 1-20, `How old are you?`.
- U03: present simple (I/you/we/they), saat söyleme, günün bölümleri, `I work / I study`, `I get up at ...`, sıklık zarfları (always/usually/sometimes).

Yeni dil bilgisi bir derste 1 konu; grammar kartı en fazla 2 örnek içersin.

## Kelime seçimi

- A1 yüksek sıklıklı, somut, günlük kelimeler.
- Her kelime için: `term` (İngilizce), `meaning_tr`, `pos` (Türkçe: isim/fiil/sıfat/ünlem/ifade), `example`, `example_tr`.
- `example` hedef kelimeyi içermeli (çekimli hâli kabul; örn. "studies" için term "study").
- `ipa` opsiyonel; emin değilsen yazma.
- Aynı ders içinde aynı `term` iki kez yok; farklı derslerde tekrar serbest.

## Alıştırma yazım kuralları

- `prompt_tr` net ve tek görevli: "Doğru cevabı seç.", "Cümleyi kur.", "Türkçesini yaz." gibi.
- Seçenekler birbirinden açıkça farklı; çeldiriciler aynı türde.
- `explanation_tr` yanlış cevapta kısa neden verir; doğru cevabı tekrar etmekle yetinmez.
- `translate.answers` doğal varyantları içerir (I am / I'm; Hello / Hi). Gereksiz varyant çokluğu yok.
- `speak.target` kısa (≤12 kelime); öğrencinin söyleyebileceği doğal cümle.
- `write` kişisel ve basit: "Kendini 2 cümleyle tanıt."; `keywords` 2-4; `min_words` 5-15.
- `word_order.tokens` karışık sırada; `answer` doğru sırada; gereksiz büyük harf/noktalama tutarlı.
- `cloze.sentence` tam olarak bir `___`; boşluk cümlenin doğal yerinde.

## Ton ve dil

- Öğrenciye "siz" değil "sen" (uygulama genç/yetişkin, samimi ama saygılı).
- Korkutucu/utandırıcı geri bildirim yok: "Tekrar deneyelim", "Neredeyse oldu".
- Kültürel genelleme, siyaset, din, şiddet, cinsellik, sağlık tavsiyesi yok.
- İngilizce örneklerde kişi adları çeşitli ve nötr (Anna, Ben, Sam, Mia, Leo...).
- Emoji yok.

## Kalite kontrol listesi (her ders)

- [ ] `id`, `unit`, `cefr` doğru; dosya adı `lNN.json`.
- [ ] 5-10 kelime; her örnek hedef kelimeyi içeriyor.
- [ ] 1-2 grammar/teach kartı; açıklamalar Türkçe ve ≤3 cümle.
- [ ] 8-12 alıştırma; en az 2 üretim; en az 4 farklı tip.
- [ ] Tüm `answer` indeksleri doğru; `word_order` multiset eşit; `cloze` tek boşluk.
- [ ] Diyalog varsa 4-8 satır, doğal ve ders hedefiyle uyumlu.
- [ ] `python3 tools/content/validate.py --lesson <dosya>` temiz.
- [ ] Rol yapma varsa hedef ifadeler dersteki kelimelerle uyumlu.

## Ünite sonu rol yapma

- Senaryo, ünitenin iletişim hedefini kullanır; yeni dil bilgisi yüklemez.
- `opening_en` tek cümle; öğrenciyi konuşturur.
- `success_criteria_tr` 2-4 madde; değerlendirme oturum sonu özetinde kullanılır.
- `role_en` yapay zekânın oynayacağı karakteri net tanımlar (ad, bağlam, ton).
