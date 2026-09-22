local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint

local PLACEMENT_MAP = {
  _window_start = "start",
  _window_midpoint = "mid",
  _window_end = "end",
}

local get_aggregator = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local placement = PLACEMENT_MAP[config.placement] or "end"
  local aggregator

  if type == "_min" then
    aggregator = aggregation.running_min_aggregator2(placement)
  elseif type == "_max" then
    aggregator = aggregation.running_max_aggregator2(placement)
  elseif type == "_average" then
    aggregator = aggregation.avg_aggregator2(placement)
  elseif type == "_sum" then
    aggregator = aggregation.sum_aggregator2(placement)
  elseif type == "_variance" then
    aggregator = aggregation.variance_aggregator2(placement)
  elseif type == "_standard_deviation" then
    aggregator = aggregation.stdev_aggregator2(placement)
  elseif type == "_count" then
    aggregator = aggregation.count_aggregator2(placement)
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_window = function(config)
  if type(config.window) ~= "string" then
    error("config.window is not a string")
  end

  if config.window == "_seconds" then
    return core.DURATION.SECOND
  elseif config.window == "_minutes" then
    return core.DURATION.MINUTE
  elseif config.window == "_hours" then
    return core.DURATION.HOUR
  elseif config.window == "_days" then
    return core.PERIOD.DAY
  elseif config.window == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.window == "_months" then
    return core.PERIOD.MONTH
  elseif config.window == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown window: " .. tostring(config.window))
  end
end

return {
  id = "rolling-window-aggregation-v4",
  version = "4.0.2",
  inputCount = 1,
  title = {
  	["en"] = "Rolling Window",
  	["af"] = "Rollende Venster",
  	["sq"] = "Dritare rrëshqitëse",
  	["am"] = "ተንሸራታች መስኮት",
  	["hy"] = "Շարժական պատուհան",
  	["az"] = "Sürüşən pəncərə",
  	["bn"] = "চলমান উইন্ডো",
  	["eu"] = "Leiho mugikorra",
  	["be"] = "Рухомае акно",
  	["bg"] = "Плъзгащ се прозорец",
  	["my"] = "ရွေ့လျားဝင်းဒိုး",
  	["ca"] = "Finestra mòbil",
  	["zh-Hans"] = "滚动窗口",
  	["zh-Hant"] = "滾動視窗",
  	["hr"] = "Klizni prozor",
  	["cs"] = "Klouzavé okno",
  	["da"] = "Rullende vindue",
  	["nl"] = "Schuivend venster",
  	["et"] = "Liikuv aken",
  	["fil"] = "Rolling Window",
  	["fi"] = "Liukuva aikaikkuna",
  	["fr"] = "Fenêtre glissante",
  	["gl"] = "Xanela móbil",
  	["ka"] = "მცოცავი ფანჯარა",
  	["de"] = "Gleitendes Fenster",
  	["el"] = "Κυλιόμενο παράθυρο",
  	["gu"] = "રોલિંગ વિન્ડો",
  	["hi"] = "रोलिंग विंडो",
  	["hu"] = "Gördülő ablak",
  	["is"] = "Rennandi gluggi",
  	["id"] = "Jendela Bergulir",
  	["it"] = "Finestra mobile",
  	["ja"] = "移動ウィンドウ",
  	["kn"] = "ಚಲಿಸುವ ವಿಂಡೋ",
  	["kk"] = "Жылжымалы терезе",
  	["km"] = "បង្អួចរំកិល",
  	["ko"] = "이동 창",
  	["ky"] = "Жылма терезе",
  	["lo"] = "ໜ້າຕ່າງເລື່ອນ",
  	["lv"] = "Slīdošais logs",
  	["lt"] = "Slenkantis langas",
  	["mk"] = "Подвижен прозорец",
  	["ms"] = "Tetingkap Bergerak",
  	["ml"] = "റോളിംഗ് വിൻഡോ",
  	["mr"] = "सरकती विंडो",
  	["mn"] = "Гулсах цонх",
  	["ne"] = "चलायमान विन्डो",
  	["no"] = "Rullerende vindu",
  	["pl"] = "Okno kroczące",
  	["pt"] = "Janela móvel",
  	["pa"] = "ਰੋਲਿੰਗ ਵਿੰਡੋ",
  	["ro"] = "Fereastră glisantă",
  	["rm"] = "Fanestra glischanta",
  	["ru"] = "Скользящее окно",
  	["sr"] = "Klizni prozor",
  	["si"] = "ස්ලයිඩින් කවුළුව",
  	["sk"] = "Posuvné okno",
  	["sl"] = "Drseče okno",
  	["es"] = "Ventana móvil",
  	["sw"] = "Dirisha Linalosogea",
  	["sv"] = "Rullande fönster",
  	["ta"] = "நகரும் சாளரம்",
  	["te"] = "రోలింగ్ విండో",
  	["th"] = "หน้าต่างเลื่อน",
  	["tr"] = "Kayan Pencere",
  	["uk"] = "Ковзне вікно",
  	["vi"] = "Cửa sổ trượt",
  },
  categories = { "_aggregation" },
  description = {
  	["en"] = [[
Calculates aggregate statistics over a moving time window for each data point. The function looks backward in time from each point and aggregates all values within the specified window period.

For example, with a 7-day window and average aggregation, each output point represents the average of all values in the 7 days leading up to that point.

**Configuration Options:**

- **Aggregation**: The operation to perform on values in each window:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Window Size**: The time unit for the lookback period (seconds, minutes, hours, days, weeks, months, or years)

- **Multiplier**: How many window size units to look back (e.g., multiplier of 3 with window size "days" = 3-day window)

- **Placement**: Where in the time window to place each output data point:
  - Window End: At the most recent data point in the window (default)
  - Window Midpoint: At the temporal center of the window
  - Window Start: At the oldest data point in the window
  	]],
  	["af"] = [[
Bereken saamgevoegde statistieke oor ’n bewegende tydvenster vir elke datapunt. Die funksie kyk vanaf elke punt terug in tyd en kombineer alle waardes binne die gespesifiseerde vensterperiode.

Byvoorbeeld, met ’n 7-dae-venster en gemiddelde aggregasie verteenwoordig elke uitvoerpunt die gemiddelde van alle waardes in die 7 dae voor daardie punt.

**Konfigurasie-opsies:**

- **Aggregasie**: Die bewerking wat op waardes in elke venster uitgevoer word:
  - Min: Minimumwaarde
  - Maks: Maksimumwaarde
  - Gemiddeld: Gemiddelde van alle waardes
  - Som: Totaal van alle waardes
  - Tel: Aantal datapunte
  - Variansie: Statistiese variansie
  - Standaardafwyking: Statistiese standaardafwyking

- **Venstergrootte**: Die tydeenheid vir die terugkykperiode (sekondes, minute, ure, dae, weke, maande of jare)

- **Vermenigvuldiger**: Hoeveel venstergrootte-eenhede om terug te kyk (bv. ’n vermenigvuldiger van 3 met venstergrootte "dae" = ’n 3-dae-venster)

- **Plasing**: Waar in die tydvenster elke uitvoerdatapunt geplaas word:
  - Venster-einde: By die mees onlangse datapunt in die venster (verstek)
  - Venster-middelpunt: By die tydelike middelpunt van die venster
  - Venster-begin: By die oudste datapunt in die venster
  	]],
  	["sq"] = [[
Llogarit statistika të grumbulluara mbi një dritare kohore lëvizëse për çdo pikë të të dhënave. Funksioni shikon prapa në kohë nga çdo pikë dhe grumbullon të gjitha vlerat brenda periudhës së specifikuar të dritares.

Për shembull, me një dritare 7-ditore dhe grumbullim mesatar, çdo pikë dalëse përfaqëson mesataren e të gjitha vlerave në 7 ditët para asaj pike.

**Opsionet e konfigurimit:**

- **Grumbullimi**: Veprimi që kryhet mbi vlerat në çdo dritare:
  - Min: Vlera minimale
  - Max: Vlera maksimale
  - Mesatarja: Mesatarja e të gjitha vlerave
  - Shuma: Totali i të gjitha vlerave
  - Numërimi: Numri i pikave të të dhënave
  - Varianca: Varianca statistikore
  - Devijimi standard: Devijimi standard statistikor

- **Madhësia e dritares**: Njësia kohore për periudhën e shikimit prapa (sekonda, minuta, orë, ditë, javë, muaj ose vite)

- **Shumëzuesi**: Sa njësi të madhësisë së dritares të shikohen prapa (p.sh., shumëzuesi 3 me madhësinë e dritares "ditë" = dritare 3-ditore)

- **Pozicionimi**: Ku të vendoset çdo pikë dalëse e të dhënave në dritaren kohore:
  - Fundi i dritares: Te pika më e fundit e të dhënave në dritare (parazgjedhje)
  - Mesii i dritares: Në qendrën kohore të dritares
  - Fillimi i dritares: Te pika më e vjetër e të dhënave në dritare
  	]],
  	["am"] = [[
ለእያንዳንዱ የውሂብ ነጥብ በሚንቀሳቀስ የጊዜ መስኮት ላይ የተጠቃለሉ ስታቲስቲካዊ መለኪያዎችን ያሰላል። ተግባሩ ከእያንዳንዱ ነጥብ ወደ ያለፈው ጊዜ በመመለስ በተገለጸው የመስኮት ጊዜ ውስጥ ያሉ ሁሉንም እሴቶች ያጠቃልላል።

ለምሳሌ፣ የ7 ቀን መስኮት እና አማካይ ማጠቃለያ ሲኖር፣ እያንዳንዱ የውጤት ነጥብ እስከዚያ ነጥብ ድረስ ባሉት 7 ቀናት ውስጥ ያሉ ሁሉንም እሴቶች አማካይ ይወክላል።

**የውቅር አማራጮች፦**

- **Aggregation**፦ በእያንዳንዱ መስኮት ውስጥ ባሉ እሴቶች ላይ የሚፈጸም ክንውን፦
  - Min፦ ዝቅተኛው እሴት
  - Max፦ ከፍተኛው እሴት
  - Average፦ የሁሉም እሴቶች አማካይ
  - Sum፦ የሁሉም እሴቶች ድምር
  - Count፦ የውሂብ ነጥቦች ብዛት
  - Variance፦ ስታቲስቲካዊ ልዩነት
  - Standard Deviation፦ ስታቲስቲካዊ መደበኛ ልዩነት

- **Window Size**፦ ለመልሶ መመልከቻ ጊዜ የሚያገለግል የጊዜ ክፍል (ሰከንዶች፣ ደቂቃዎች፣ ሰዓቶች፣ ቀናት፣ ሳምንታት፣ ወራት ወይም ዓመታት)

- **Multiplier**፦ ወደ ያለፈው ጊዜ ስንት የመስኮት መጠን ክፍሎች እንደሚመለከት (ለምሳሌ፣ የመስኮት መጠን "days" እና multiplier 3 = የ3 ቀን መስኮት)

- **Placement**፦ እያንዳንዱ የውጤት የውሂብ ነጥብ በጊዜ መስኮቱ ውስጥ የት እንደሚቀመጥ፦
  - Window End፦ በመስኮቱ ውስጥ ባለው በጣም የቅርብ የውሂብ ነጥብ (ነባሪ)
  - Window Midpoint፦ በመስኮቱ የጊዜ መሃል
  - Window Start፦ በመስኮቱ ውስጥ ባለው በጣም የቆየ የውሂብ ነጥብ
  	]],
  	["hy"] = [[
Յուրաքանչյուր տվյալակետի համար հաշվարկում է միավորված վիճակագրություն շարժական ժամանակային պատուհանի վրա։ Ֆունկցիան յուրաքանչյուր կետից ժամանակի մեջ հետ է գնում և միավորում նշված պատուհանի ժամանակահատվածում ընկնող բոլոր արժեքները։

Օրինակ՝ 7-օրյա պատուհանի և միջին միավորման դեպքում յուրաքանչյուր ելքային կետ ներկայացնում է այդ կետին նախորդող 7 օրվա բոլոր արժեքների միջինը։

**Կազմաձևման ընտրանքներ․**

- **Միավորում**․ յուրաքանչյուր պատուհանի արժեքների նկատմամբ կատարվող գործողությունը՝
  - Min՝ նվազագույն արժեք
  - Max՝ առավելագույն արժեք
  - Average՝ բոլոր արժեքների միջին
  - Sum՝ բոլոր արժեքների գումար
  - Count՝ տվյալակետերի քանակ
  - Variance՝ վիճակագրական դիսպերսիա
  - Standard Deviation՝ վիճակագրական ստանդարտ շեղում

- **Պատուհանի չափ**․ հետադարձ ժամանակահատվածի ժամանակի միավորը (վայրկյաններ, րոպեներ, ժամեր, օրեր, շաբաթներ, ամիսներ կամ տարիներ)

- **Բազմապատկիչ**․ քանի պատուհանի չափի միավոր հետ գնալ (օրինակ՝ «օրեր» պատուհանի չափով 3 բազմապատկիչը նշանակում է 3-օրյա պատուհան)

- **Տեղադրում**․ ժամանակային պատուհանի որ հատվածում տեղադրել յուրաքանչյուր ելքային տվյալակետը՝
  - Window End՝ պատուհանի ամենավերջին տվյալակետի պահին (կանխադրված)
  - Window Midpoint՝ պատուհանի ժամանակային կենտրոնում
  - Window Start՝ պատուհանի ամենահին տվյալակետի պահին
  	]],
  	["az"] = [[
Hər məlumat nöqtəsi üçün hərəkətli zaman pəncərəsi üzrə ümumiləşdirilmiş statistikaları hesablayır. Funksiya hər nöqtədən geriyə doğru baxır və göstərilən pəncərə müddətindəki bütün qiymətləri birləşdirir.

Məsələn, 7 günlük pəncərə və orta qiymət birləşdirməsi ilə hər çıxış nöqtəsi həmin nöqtəyə qədərki 7 gündəki bütün qiymətlərin ortalamasını göstərir.

**Konfiqurasiya seçimləri:**

- **Birləşdirmə**: Hər pəncərədəki qiymətlər üzərində icra ediləcək əməliyyat:
  - Min: Minimum qiymət
  - Max: Maksimum qiymət
  - Average: Bütün qiymətlərin ortalaması
  - Sum: Bütün qiymətlərin cəmi
  - Count: Məlumat nöqtələrinin sayı
  - Variance: Statistik dispersiya
  - Standard Deviation: Statistik standart kənarlaşma

- **Pəncərə ölçüsü**: Geri baxış müddəti üçün zaman vahidi (saniyə, dəqiqə, saat, gün, həftə, ay və ya il)

- **Vurğu**: Geri baxılacaq pəncərə vahidlərinin sayı (məsələn, pəncərə ölçüsü "days" və vurğu 3 olduqda = 3 günlük pəncərə)

- **Yerləşdirmə**: Hər çıxış məlumat nöqtəsinin zaman pəncərəsində yerləşdiriləcəyi yer:
  - Window End: Pəncərədəki ən son məlumat nöqtəsində (standart)
  - Window Midpoint: Pəncərənin zaman mərkəzində
  - Window Start: Pəncərədəki ən köhnə məlumat nöqtəsində
  	]],
  	["bn"] = [[
প্রতিটি ডেটা পয়েন্টের জন্য চলমান সময়ের উইন্ডোতে একত্রীকৃত পরিসংখ্যান গণনা করে। প্রতিটি পয়েন্ট থেকে সময়ের পেছনে গিয়ে নির্দিষ্ট উইন্ডো সময়সীমার মধ্যে থাকা সব মান একত্রিত করে।

উদাহরণস্বরূপ, ৭ দিনের উইন্ডো ও গড় একত্রীকরণ হলে, প্রতিটি আউটপুট পয়েন্ট ওই পয়েন্টের আগের ৭ দিনে থাকা সব মানের গড় নির্দেশ করে।

**কনফিগারেশন অপশন:**

- **একত্রীকরণ**: প্রতিটি উইন্ডোর মানের ওপর সম্পাদিত অপারেশন:
  - Min: সর্বনিম্ন মান
  - Max: সর্বোচ্চ মান
  - Average: সব মানের গড়
  - Sum: সব মানের মোট
  - Count: ডেটা পয়েন্টের সংখ্যা
  - Variance: পরিসংখ্যানগত বিচরণ
  - Standard Deviation: পরিসংখ্যানগত মান বিচ্যুতি

- **উইন্ডোর আকার**: পেছনে দেখার সময়সীমার একক (সেকেন্ড, মিনিট, ঘণ্টা, দিন, সপ্তাহ, মাস বা বছর)

- **গুণক**: কতটি উইন্ডো-আকারের একক পেছনে দেখা হবে (যেমন, উইন্ডোর আকার "দিন" এবং গুণক ৩ হলে = ৩ দিনের উইন্ডো)

- **অবস্থান**: সময়ের উইন্ডোর কোথায় প্রতিটি আউটপুট ডেটা পয়েন্ট রাখা হবে:
  - Window End: উইন্ডোর সর্বশেষ ডেটা পয়েন্টে (ডিফল্ট)
  - Window Midpoint: উইন্ডোর সময়গত কেন্দ্রে
  - Window Start: উইন্ডোর প্রাচীনতম ডেটা পয়েন্টে
  	]],
  	["eu"] = [[
Datu-puntu bakoitzerako denbora-leiho mugikor baten gaineko estatistika agregatuak kalkulatzen ditu. Funtzioak puntu bakoitzetik denboran atzera begiratzen du, eta zehaztutako leiho-aldian dauden balio guztiak agregatzen ditu.

Adibidez, 7 eguneko leihoarekin eta batez besteko agregazioarekin, irteerako puntu bakoitzak puntu horren aurreko 7 egunetako balio guztien batez bestekoa adierazten du.

**Konfigurazio-aukerak:**

- **Agregazioa**: Leiho bakoitzeko balioekin egin beharreko eragiketa:
  - Min: Gutxieneko balioa
  - Max: Gehieneko balioa
  - Batez bestekoa: Balio guztien batez bestekoa
  - Batura: Balio guztien batura
  - Zenbaketa: Datu-puntuen kopurua
  - Bariantza: Bariantza estatistikoa
  - Desbideratze estandarra: Desbideratze estandarra estatistikoa

- **Leihoaren tamaina**: Atzera begiratzeko aldiaren denbora-unitatea (segundoak, minutuak, orduak, egunak, asteak, hilabeteak edo urteak)

- **Biderkatzailea**: Atzera begiratzeko leihoaren tamainako zenbat unitate (adib., 3ko biderkatzailea eta "egunak" leiho-tamaina gisa = 3 eguneko leihoa)

- **Kokapena**: Irteerako datu-puntu bakoitza denbora-leihoan non kokatu:
  - Leihoaren amaiera: Leihoan dagoen azken datu-puntuan (lehenetsia)
  - Leihoaren erdiko puntua: Leihoaren erdigune kronologikoan
  - Leihoaren hasiera: Leihoan dagoen datu-puntu zaharrenean
  	]],
  	["be"] = [[
Вылічвае агрэгаваныя статыстыкі ў рухомым часовым акне для кожнай кропкі даных. Функцыя рухаецца назад у часе ад кожнай кропкі і аб’ядноўвае ўсе значэнні ў зададзеным перыядзе акна.

Напрыклад, пры 7-дзённым акне і агрэгацыі сярэдняга кожная выходная кропка паказвае сярэдняе ўсіх значэнняў за 7 дзён да яе.

**Параметры канфігурацыі:**

- **Агрэгацыя**: аперацыя над значэннямі ў кожным акне:
  - Мін.: мінімальнае значэнне
  - Макс.: максімальнае значэнне
  - Сярэдняе: сярэдняе ўсіх значэнняў
  - Сума: сума ўсіх значэнняў
  - Колькасць: колькасць кропак даных
  - Дысперсія: статыстычная дысперсія
  - Стандартнае адхіленне: статыстычнае стандартнае адхіленне

- **Памер акна**: адзінка часу для перыяду прагляду назад (секунды, хвіліны, гадзіны, дні, тыдні, месяцы або гады)

- **Множнік**: колькасць адзінак памеру акна для прагляду назад (напрыклад, множнік 3 і памер акна «дні» = 3-дзённае акно)

- **Размяшчэнне**: дзе ў часовым акне размясціць кожную выходную кропку даных:
  - Канец акна: у самай позняй кропцы даных у акне (па змаўчанні)
  - Сярэдзіна акна: у часовым цэнтры акна
  - Пачатак акна: у самай ранняй кропцы даных у акне
  	]],
  	["bg"] = [[
Изчислява обобщени статистики върху плъзгащ се времеви прозорец за всяка точка от данни. Функцията разглежда времето назад от всяка точка и обобщава всички стойности в зададения период на прозореца.

Например при 7-дневен прозорец и обобщаване чрез средна стойност всяка изходна точка представя средната стойност на всички стойности през 7-те дни, водещи до тази точка.

**Опции за конфигуриране:**

- **Обобщаване**: Операцията, която се извършва върху стойностите във всеки прозорец:
  - Min: Минимална стойност
  - Max: Максимална стойност
  - Average: Средна стойност на всички стойности
  - Sum: Сбор на всички стойности
  - Count: Брой точки от данни
  - Variance: Статистическа дисперсия
  - Standard Deviation: Статистическо стандартно отклонение

- **Размер на прозореца**: Времевата единица за периода назад във времето (секунди, минути, часове, дни, седмици, месеци или години)

- **Множител**: Колко единици от размера на прозореца да се обхванат назад във времето (напр. множител 3 с размер на прозореца „дни“ = 3-дневен прозорец)

- **Позициониране**: Къде във времевия прозорец да бъде поставена всяка изходна точка от данни:
  - Край на прозореца: При най-новата точка от данни в прозореца (по подразбиране)
  - Среда на прозореца: Във времевия център на прозореца
  - Начало на прозореца: При най-старата точка от данни в прозореца
  	]],
  	["my"] = [[
ဒေတာအမှတ်တစ်ခုစီအတွက် ရွေ့လျားနေသော အချိန်ဝင်းဒိုးအတွင်း စုစည်းစာရင်းအင်းများကို တွက်ချက်သည်။ function သည် ဒေတာအမှတ်တစ်ခုစီမှ အချိန်နောက်ပြန်ကြည့်၍ သတ်မှတ်ထားသော ဝင်းဒိုးကာလအတွင်းရှိ တန်ဖိုးအားလုံးကို စုစည်းသည်။

ဥပမာအားဖြင့်၊ ၇ ရက်ဝင်းဒိုးနှင့် ပျမ်းမျှစုစည်းမှုကို အသုံးပြုပါက ထုတ်ပေးသည့် ဒေတာအမှတ်တစ်ခုစီသည် ထိုအမှတ်မတိုင်မီ ၇ ရက်အတွင်းရှိ တန်ဖိုးအားလုံး၏ ပျမ်းမျှကို ကိုယ်စားပြုသည်။

**ပြင်ဆင်သတ်မှတ်မှု ရွေးချယ်စရာများ:**

- **စုစည်းမှု**: ဝင်းဒိုးတစ်ခုစီရှိ တန်ဖိုးများအပေါ် လုပ်ဆောင်မည့် လုပ်ဆောင်ချက်:
  - Min: အနည်းဆုံးတန်ဖိုး
  - Max: အများဆုံးတန်ဖိုး
  - Average: တန်ဖိုးအားလုံး၏ ပျမ်းမျှ
  - Sum: တန်ဖိုးအားလုံး၏ စုစုပေါင်း
  - Count: ဒေတာအမှတ်အရေအတွက်
  - Variance: စာရင်းအင်းကွဲပြားမှု
  - Standard Deviation: စာရင်းအင်းစံသွေဖည်မှု

- **ဝင်းဒိုးအရွယ်အစား**: နောက်ပြန်ကြည့်မည့်ကာလ၏ အချိန်ယူနစ် (စက္ကန့်၊ မိနစ်၊ နာရီ၊ နေ့၊ အပတ်၊ လ သို့မဟုတ် နှစ်)

- **မြှောက်ကိန်း**: နောက်ပြန်ကြည့်မည့် ဝင်းဒိုးအရွယ်အစားယူနစ် အရေအတွက် (ဥပမာ၊ ဝင်းဒိုးအရွယ်အစား "days" နှင့် မြှောက်ကိန်း ၃ = ၃ ရက်ဝင်းဒိုး)

- **နေရာချထားမှု**: ထုတ်ပေးမည့် ဒေတာအမှတ်တစ်ခုစီကို အချိန်ဝင်းဒိုးအတွင်း မည်သည့်နေရာတွင် ထားမည်နည်း:
  - Window End: ဝင်းဒိုးအတွင်း နောက်ဆုံးဒေတာအမှတ်တွင် (မူလ)
  - Window Midpoint: ဝင်းဒိုး၏ အချိန်အလယ်ဗဟိုတွင်
  - Window Start: ဝင်းဒိုးအတွင်း အဟောင်းဆုံးဒေတာအမှတ်တွင်
  	]],
  	["ca"] = [[
Calcula estadístiques agregades en una finestra de temps mòbil per a cada punt de dades. La funció mira enrere en el temps des de cada punt i agrega tots els valors dins del període de la finestra especificat.

Per exemple, amb una finestra de 7 dies i una agregació mitjana, cada punt de sortida representa la mitjana de tots els valors dels 7 dies anteriors a aquell punt.

**Opcions de configuració:**

- **Agregació**: L’operació que cal fer amb els valors de cada finestra:
  - Mínim: Valor mínim
  - Màxim: Valor màxim
  - Mitjana: Mitjana de tots els valors
  - Suma: Total de tots els valors
  - Recompte: Nombre de punts de dades
  - Variància: Variància estadística
  - Desviació estàndard: Desviació estàndard estadística

- **Mida de la finestra**: La unitat de temps del període retrospectiu (segons, minuts, hores, dies, setmanes, mesos o anys)

- **Multiplicador**: Quantes unitats de la mida de la finestra cal mirar enrere (p. ex., un multiplicador de 3 amb una mida de finestra de «dies» = finestra de 3 dies)

- **Posicionament**: On col·locar cada punt de dades de sortida dins de la finestra de temps:
  - Final de la finestra: Al punt de dades més recent de la finestra (per defecte)
  - Punt mitjà de la finestra: Al centre temporal de la finestra
  - Inici de la finestra: Al punt de dades més antic de la finestra
  	]],
  	["zh-Hans"] = [[
为每个数据点计算移动时间窗口内的聚合统计值。该函数会从每个数据点向过去回溯，并聚合指定窗口时间段内的所有值。

例如，使用 7 天窗口和平均值聚合时，每个输出数据点代表该数据点之前 7 天内所有值的平均值。

**配置选项：**

- **聚合**：对每个窗口中的值执行的操作：
  - Min：最小值
  - Max：最大值
  - Average：所有值的平均值
  - Sum：所有值的总和
  - Count：数据点数量
  - Variance：统计方差
  - Standard Deviation：统计标准差

- **窗口大小**：回溯时间段的时间单位（秒、分钟、小时、天、周、月或年）

- **倍数**：要回溯的窗口单位数量（例如，窗口大小为“天”、倍数为 3 = 3 天窗口）

- **放置位置**：每个输出数据点在时间窗口中的放置位置：
  - 窗口结束：位于窗口中最新的数据点处（默认）
  - 窗口中点：位于窗口的时间中心
  - 窗口开始：位于窗口中最早的数据点处
  	]],
  	["zh-Hant"] = [[
為每個資料點計算移動時間視窗中的彙總統計。此函式會從每個資料點向過去回溯，彙總指定視窗期間內的所有值。

例如，若視窗為 7 天且彙總方式為平均值，每個輸出點代表截至該點前 7 天內所有值的平均值。

**設定選項：**

- **彙總**：對每個視窗中的值執行的操作：
  - 最小值：最小值
  - 最大值：最大值
  - 平均值：所有值的平均值
  - 總和：所有值的總和
  - 計數：資料點數量
  - 變異數：統計變異數
  - 標準差：統計標準差

- **視窗大小**：回溯期間的時間單位（秒、分鐘、小時、天、週、月或年）

- **倍數**：要回溯的視窗大小單位數量（例如視窗大小為「天」且倍數為 3 = 3 天視窗）

- **放置位置**：每個輸出資料點在時間視窗中的放置位置：
  - 視窗結束：位於視窗中的最新資料點（預設）
  - 視窗中點：位於視窗的時間中心
  - 視窗開始：位於視窗中的最舊資料點
  	]],
  	["hr"] = [[
Izračunava agregatne statistike kroz pomični vremenski prozor za svaku podatkovnu točku. Funkcija gleda unatrag u vremenu od svake točke i agregira sve vrijednosti unutar navedenog razdoblja prozora.

Na primjer, s prozorom od 7 dana i agregacijom prosjeka, svaka izlazna točka predstavlja prosjek svih vrijednosti u 7 dana koja prethode toj točki.

**Mogućnosti konfiguracije:**

- **Agregacija**: Operacija koja se izvodi nad vrijednostima u svakom prozoru:
  - Min: Minimalna vrijednost
  - Max: Maksimalna vrijednost
  - Prosjek: Srednja vrijednost svih vrijednosti
  - Zbroj: Zbroj svih vrijednosti
  - Broj: Broj podatkovnih točaka
  - Varijanca: Statistička varijanca
  - Standardna devijacija: Statistička standardna devijacija

- **Veličina prozora**: Vremenska jedinica za razdoblje gledanja unatrag (sekunde, minute, sati, dani, tjedni, mjeseci ili godine)

- **Množitelj**: Broj jedinica veličine prozora za gledanje unatrag (npr. množitelj 3 s veličinom prozora „dani” = prozor od 3 dana)

- **Smještaj**: Gdje u vremenski prozor smjestiti svaku izlaznu podatkovnu točku:
  - Kraj prozora: Na najnoviju podatkovnu točku u prozoru (zadano)
  - Sredina prozora: U vremensko središte prozora
  - Početak prozora: Na najstariju podatkovnu točku u prozoru
  	]],
  	["cs"] = [[
Vypočítá souhrnné statistiky v pohyblivém časovém okně pro každý datový bod. Funkce se od každého bodu dívá zpět v čase a agreguje všechny hodnoty v zadaném období okna.

Například při sedmidenním okně a průměrné agregaci každý výstupní bod představuje průměr všech hodnot za 7 dnů předcházejících danému bodu.

**Možnosti konfigurace:**

- **Agregace**: Operace prováděná s hodnotami v každém okně:
  - Minimum: Nejnižší hodnota
  - Maximum: Nejvyšší hodnota
  - Průměr: Průměr všech hodnot
  - Součet: Součet všech hodnot
  - Počet: Počet datových bodů
  - Rozptyl: Statistický rozptyl
  - Směrodatná odchylka: Statistická směrodatná odchylka

- **Velikost okna**: Časová jednotka období zpětného pohledu (sekundy, minuty, hodiny, dny, týdny, měsíce nebo roky)

- **Násobitel**: Počet jednotek velikosti okna, o které se má vrátit zpět (např. násobitel 3 s velikostí okna „dny“ = třídenní okno)

- **Umístění**: Kam v časovém okně umístit každý výstupní datový bod:
  - Konec okna: K nejnovějšímu datovému bodu v okně (výchozí)
  - Střed okna: Do časového středu okna
  - Začátek okna: K nejstaršímu datovému bodu v okně
  	]],
  	["da"] = [[
Beregner aggregerede statistikker over et bevægeligt tidsvindue for hvert datapunkt. Funktionen ser bagud i tiden fra hvert punkt og sammenlægger alle værdier inden for den angivne vinduesperiode.

For eksempel: Med et 7-dages vindue og gennemsnitlig sammenlægning repræsenterer hvert outputpunkt gennemsnittet af alle værdier i de 7 dage op til det pågældende punkt.

**Konfigurationsindstillinger:**

- **Sammenlægning**: Handlingen, der udføres på værdierne i hvert vindue:
  - Min: Minimumsværdi
  - Max: Maksimumsværdi
  - Gennemsnit: Gennemsnittet af alle værdier
  - Sum: Summen af alle værdier
  - Antal: Antallet af datapunkter
  - Varians: Statistisk varians
  - Standardafvigelse: Statistisk standardafvigelse

- **Vinduesstørrelse**: Tidsenheden for tilbageblikperioden (sekunder, minutter, timer, dage, uger, måneder eller år)

- **Multiplikator**: Hvor mange vinduesstørrelsesenheder der skal ses tilbage (f.eks. multiplikator 3 med vinduesstørrelse "dage" = 3-dages vindue)

- **Placering**: Hvor i tidsvinduet hvert outputdatapunkt placeres:
  - Vinduets slutning: Ved det seneste datapunkt i vinduet (standard)
  - Vinduets midtpunkt: Ved vinduets tidsmæssige centrum
  - Vinduets begyndelse: Ved det ældste datapunkt i vinduet
  	]],
  	["nl"] = [[
Berekent geaggregeerde statistieken over een bewegend tijdvenster voor elk datapunt. De functie kijkt vanaf elk punt terug in de tijd en aggregeert alle waarden binnen de opgegeven vensterperiode.

Bijvoorbeeld: bij een venster van 7 dagen en gemiddelde aggregatie vertegenwoordigt elk uitgevoerd datapunt het gemiddelde van alle waarden in de 7 dagen voorafgaand aan dat punt.

**Configuratieopties:**

- **Aggregatie**: De bewerking die moet worden uitgevoerd op waarden in elk venster:
  - Min: Minimumwaarde
  - Max: Maximumwaarde
  - Gemiddelde: Gemiddelde van alle waarden
  - Som: Totaal van alle waarden
  - Aantal: Aantal datapunten
  - Variantie: Statistische variantie
  - Standaarddeviatie: Statistische standaarddeviatie

- **Venstergrootte**: De tijdseenheid voor de terugkijkperiode (seconden, minuten, uren, dagen, weken, maanden of jaren)

- **Vermenigvuldiger**: Hoeveel eenheden van de venstergrootte moet worden teruggekeken (bijv. vermenigvuldiger 3 met venstergrootte "dagen" = venster van 3 dagen)

- **Plaatsing**: Waar in het tijdvenster elk uitgevoerd datapunt wordt geplaatst:
  - Einde van venster: Bij het meest recente datapunt in het venster (standaard)
  - Middelpunt van venster: In het temporele midden van het venster
  - Begin van venster: Bij het oudste datapunt in het venster
  	]],
  	["et"] = [[
Arvutab iga andmepunkti jaoks liikuva ajaakna koondstatistika. Funktsioon vaatab igast punktist ajas tagasi ja koondab kõik määratud aknaperioodi jäävad väärtused.

Näiteks 7-päevase akna ja keskmise koondamise korral esindab iga väljundpunkt kõigi sellele punktile eelnenud 7 päeva väärtuste keskmist.

**Seadistusvalikud:**

- **Koondamine**: igas aknas olevate väärtustega tehtav toiming:
  - Miinimum: väikseim väärtus
  - Maksimum: suurim väärtus
  - Keskmine: kõigi väärtuste keskmine
  - Summa: kõigi väärtuste summa
  - Loendus: andmepunktide arv
  - Dispersioon: statistiline dispersioon
  - Standardhälve: statistiline standardhälve

- **Akna suurus**: tagasivaatamise perioodi ajaühik (sekundid, minutid, tunnid, päevad, nädalad, kuud või aastad)

- **Kordaja**: mitu akna suurusühikut tagasi vaadata (nt kordaja 3 ja akna suurus „päevad” = 3-päevane aken)

- **Paigutus**: kuhu ajavahemikus paigutada iga väljundi andmepunkt:
  - Akna lõpp: akna kõige uuema andmepunkti juurde (vaikimisi)
  - Akna keskpunkt: akna ajalisse keskpunkti
  - Akna algus: akna vanima andmepunkti juurde
  	]],
  	["fil"] = [[
Kinakalkula ang pinagsama-samang estadistika sa gumagalaw na time window para sa bawat data point. Tumingin ang function pabalik sa oras mula sa bawat punto at pinagsasama ang lahat ng halaga sa loob ng tinukoy na window period.

Halimbawa, sa 7-araw na window at average aggregation, kinakatawan ng bawat output point ang average ng lahat ng halaga sa 7 araw bago ang puntong iyon.

**Mga Opsyon sa Configuration:**

- **Aggregation**: Operasyong isasagawa sa mga halaga sa bawat window:
  - Min: Pinakamababang halaga
  - Max: Pinakamataas na halaga
  - Average: Mean ng lahat ng halaga
  - Sum: Kabuuan ng lahat ng halaga
  - Count: Bilang ng mga data point
  - Variance: Estadistikal na variance
  - Standard Deviation: Estadistikal na standard deviation

- **Window Size**: Yunit ng oras para sa lookback period (segundo, minuto, oras, araw, linggo, buwan, o taon)

- **Multiplier**: Ilang yunit ng window size ang titingnan pabalik (hal., multiplier na 3 at window size na "days" = 3-araw na window)

- **Placement**: Kung saan ilalagay ang bawat output data point sa time window:
  - Window End: Sa pinakabagong data point sa window (default)
  - Window Midpoint: Sa gitnang oras ng window
  - Window Start: Sa pinakamatandang data point sa window
  	]],
  	["fi"] = [[
Laskee yhdistetyt tilastot liukuvassa aikaikkunassa kullekin datapisteelle. Funktio tarkastelee kustakin pisteestä taaksepäin ja yhdistää kaikki määritetyn aikaikkunan sisällä olevat arvot.

Esimerkiksi 7 päivän aikaikkunalla ja keskiarvoyhdistämisellä kukin tulospiste edustaa kaikkien kyseistä pistettä edeltävien 7 päivän arvojen keskiarvoa.

**Määritysasetukset:**

- **Yhdistämistapa**: Kunkin aikaikkunan arvoille suoritettava toiminto:
  - Min: Pienin arvo
  - Max: Suurin arvo
  - Keskiarvo: Kaikkien arvojen keskiarvo
  - Summa: Kaikkien arvojen summa
  - Lukumäärä: Datapisteiden määrä
  - Varianssi: Tilastollinen varianssi
  - Keskihajonta: Tilastollinen keskihajonta

- **Aikaikkunan koko**: Taaksepäin tarkasteltavan ajanjakson aikayksikkö (sekunnit, minuutit, tunnit, päivät, viikot, kuukaudet tai vuodet)

- **Kerroin**: Kuinka monen aikaikkunan yksikön verran tarkastellaan taaksepäin (esim. kerroin 3 ja ikkunan kokona "päivät" = 3 päivän aikaikkuna)

- **Sijoittelu**: Mihin kohtaan aikaikkunaa kukin tuloksena syntyvä datapiste sijoitetaan:
  - Aikaikkunan loppu: Aikaikkunan uusimman datapisteen kohdalle (oletus)
  - Aikaikkunan keskipiste: Aikaikkunan ajalliseen keskikohtaan
  - Aikaikkunan alku: Aikaikkunan vanhimman datapisteen kohdalle
  	]],
  	["fr"] = [[
Calcule des statistiques agrégées sur une fenêtre temporelle mobile pour chaque point de données. La fonction remonte dans le temps à partir de chaque point et agrège toutes les valeurs comprises dans la période de fenêtre indiquée.

Par exemple, avec une fenêtre de 7 jours et une agrégation par moyenne, chaque point produit représente la moyenne de toutes les valeurs des 7 jours précédant ce point.

**Options de configuration :**

- **Agrégation** : Opération à effectuer sur les valeurs de chaque fenêtre :
  - Min : Valeur minimale
  - Max : Valeur maximale
  - Moyenne : Moyenne de toutes les valeurs
  - Somme : Total de toutes les valeurs
  - Compte : Nombre de points de données
  - Variance : Variance statistique
  - Écart type : Écart type statistique

- **Taille de la fenêtre** : Unité de temps de la période rétrospective (secondes, minutes, heures, jours, semaines, mois ou années)

- **Multiplicateur** : Nombre d’unités de taille de fenêtre à prendre en compte (par ex. un multiplicateur de 3 avec une taille de fenêtre en « jours » = une fenêtre de 3 jours)

- **Positionnement** : Position de chaque point de données produit dans la fenêtre temporelle :
  - Fin de la fenêtre : Au point de données le plus récent de la fenêtre (par défaut)
  - Milieu de la fenêtre : Au centre temporel de la fenêtre
  - Début de la fenêtre : Au point de données le plus ancien de la fenêtre
  	]],
  	["gl"] = [[
Calcula estatísticas agregadas sobre unha xanela temporal móbil para cada punto de datos. A función retrocede no tempo desde cada punto e agrega todos os valores dentro do período da xanela especificado.

Por exemplo, cunha xanela de 7 días e unha agregación de media, cada punto de saída representa a media de todos os valores dos 7 días anteriores a ese punto.

**Opcións de configuración:**

- **Agregación**: A operación que se realizará sobre os valores de cada xanela:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Media: Media de todos os valores
  - Suma: Total de todos os valores
  - Contaxe: Número de puntos de datos
  - Varianza: Varianza estatística
  - Desviación estándar: Desviación estándar estatística

- **Tamaño da xanela**: A unidade temporal do período de consulta cara atrás (segundos, minutos, horas, días, semanas, meses ou anos)

- **Multiplicador**: Cantas unidades do tamaño da xanela retroceder (por exemplo, un multiplicador de 3 cun tamaño de xanela de «días» = xanela de 3 días)

- **Posición**: Onde colocar cada punto de datos de saída na xanela temporal:
  - Final da xanela: No punto de datos máis recente da xanela (predeterminado)
  - Punto medio da xanela: No centro temporal da xanela
  - Inicio da xanela: No punto de datos máis antigo da xanela
  	]],
  	["ka"] = [[
თითოეული მონაცემის წერტილისთვის მოძრავ დროის ფანჯარაში აგრეგირებულ სტატისტიკას ითვლის. ფუნქცია თითოეული წერტილიდან დროში უკან იყურება და მითითებული ფანჯრის პერიოდში მოქცეულ ყველა მნიშვნელობას აერთიანებს.

მაგალითად, 7-დღიანი ფანჯრისა და საშუალო აგრეგაციის შემთხვევაში, თითოეული გამოტანილი წერტილი წარმოადგენს ამ წერტილამდე ბოლო 7 დღეში არსებული ყველა მნიშვნელობის საშუალოს.

**კონფიგურაციის პარამეტრები:**

- **აგრეგაცია**: თითოეულ ფანჯარაში მნიშვნელობებზე შესასრულებელი ოპერაცია:
  - Min: მინიმალური მნიშვნელობა
  - Max: მაქსიმალური მნიშვნელობა
  - Average: ყველა მნიშვნელობის საშუალო
  - Sum: ყველა მნიშვნელობის ჯამი
  - Count: მონაცემთა წერტილების რაოდენობა
  - Variance: სტატისტიკური დისპერსია
  - Standard Deviation: სტატისტიკური სტანდარტული გადახრა

- **ფანჯრის ზომა**: უკან დასათვლელი პერიოდის დროის ერთეული (წამები, წუთები, საათები, დღეები, კვირები, თვეები ან წლები)

- **გამამრავლებელი**: უკან დასათვლელი ფანჯრის ზომის ერთეულების რაოდენობა (მაგ., გამამრავლებელი 3 და ფანჯრის ზომა "days" = 3-დღიანი ფანჯარა)

- **განთავსება**: დროის ფანჯარაში თითოეული გამოტანილი მონაცემის წერტილის განთავსების ადგილი:
  - Window End: ფანჯრის უახლეს მონაცემის წერტილზე (ნაგულისხმევი)
  - Window Midpoint: ფანჯრის დროით შუა წერტილში
  - Window Start: ფანჯრის უძველეს მონაცემის წერტილზე
  	]],
  	["de"] = [[
Berechnet für jeden Datenpunkt aggregierte Statistiken über ein gleitendes Zeitfenster. Die Funktion blickt von jedem Punkt aus in der Zeit zurück und aggregiert alle Werte innerhalb des angegebenen Zeitraums.

Bei einem 7-Tage-Fenster und einer Durchschnittsaggregation stellt beispielsweise jeder Ausgabepunkt den Durchschnitt aller Werte in den 7 Tagen bis zu diesem Punkt dar.

**Konfigurationsoptionen:**

- **Aggregation**: Der Vorgang, der auf die Werte in jedem Fenster angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Average: Mittelwert aller Werte
  - Sum: Summe aller Werte
  - Count: Anzahl der Datenpunkte
  - Variance: Statistische Varianz
  - Standard Deviation: Statistische Standardabweichung

- **Fenstergröße**: Die Zeiteinheit für den Rückblickzeitraum (Sekunden, Minuten, Stunden, Tage, Wochen, Monate oder Jahre)

- **Multiplikator**: Wie viele Fenstereinheiten zurückgeblickt wird (z. B. Multiplikator 3 mit Fenstergröße „Tage“ = 3-Tage-Fenster)

- **Positionierung**: Wo jeder Ausgabedatenpunkt im Zeitfenster platziert wird:
  - Fensterende: Beim neuesten Datenpunkt im Fenster (Standard)
  - Fenstermitte: In der zeitlichen Mitte des Fensters
  - Fensteranfang: Beim ältesten Datenpunkt im Fenster
  	]],
  	["el"] = [[
Υπολογίζει συναθροιστικά στατιστικά σε ένα μετακινούμενο χρονικό παράθυρο για κάθε σημείο δεδομένων. Η συνάρτηση κοιτάζει προς τα πίσω στον χρόνο από κάθε σημείο και συναθροίζει όλες τις τιμές εντός της καθορισμένης περιόδου παραθύρου.

Για παράδειγμα, με παράθυρο 7 ημερών και συνάθροιση μέσου όρου, κάθε σημείο εξόδου αντιπροσωπεύει τον μέσο όρο όλων των τιμών των 7 ημερών που προηγούνται αυτού του σημείου.

**Επιλογές διαμόρφωσης:**

- **Συνάθροιση**: Η πράξη που θα εκτελεστεί στις τιμές κάθε παραθύρου:
  - Min: Ελάχιστη τιμή
  - Max: Μέγιστη τιμή
  - Average: Μέσος όρος όλων των τιμών
  - Sum: Σύνολο όλων των τιμών
  - Count: Αριθμός σημείων δεδομένων
  - Variance: Στατιστική διακύμανση
  - Standard Deviation: Στατιστική τυπική απόκλιση

- **Μέγεθος παραθύρου**: Η μονάδα χρόνου για την περίοδο αναδρομής (δευτερόλεπτα, λεπτά, ώρες, ημέρες, εβδομάδες, μήνες ή έτη)

- **Πολλαπλασιαστής**: Πόσες μονάδες μεγέθους παραθύρου θα εξετάζονται προς τα πίσω (π.χ. πολλαπλασιαστής 3 με μέγεθος παραθύρου «ημέρες» = παράθυρο 3 ημερών)

- **Τοποθέτηση**: Πού μέσα στο χρονικό παράθυρο θα τοποθετείται κάθε σημείο δεδομένων εξόδου:
  - Τέλος παραθύρου: Στο πιο πρόσφατο σημείο δεδομένων του παραθύρου (προεπιλογή)
  - Μέσο παραθύρου: Στο χρονικό κέντρο του παραθύρου
  - Αρχή παραθύρου: Στο παλαιότερο σημείο δεδομένων του παραθύρου
  	]],
  	["gu"] = [[
દરેક ડેટા પોઇન્ટ માટે ખસતી સમય વિન્ડો પર એકત્રિત આંકડાઓની ગણતરી કરે છે. ફંક્શન દરેક પોઇન્ટથી સમયની દિશામાં પાછળ જુએ છે અને નિર્દિષ્ટ વિન્ડો સમયગાળાની અંદરના તમામ મૂલ્યોને એકત્રિત કરે છે.

ઉદાહરણ તરીકે, 7 દિવસની વિન્ડો અને સરેરાશ એકત્રીકરણ સાથે, દરેક આઉટપુટ પોઇન્ટ તે પોઇન્ટ સુધીના અગાઉના 7 દિવસના તમામ મૂલ્યોનો સરેરાશ દર્શાવે છે.

**ગોઠવણી વિકલ્પો:**

- **એકત્રીકરણ**: દરેક વિન્ડોના મૂલ્યો પર કરવાની ક્રિયા:
  - Min: લઘુત્તમ મૂલ્ય
  - Max: મહત્તમ મૂલ્ય
  - Average: બધા મૂલ્યોનો સરેરાશ
  - Sum: બધા મૂલ્યોનો સરવાળો
  - Count: ડેટા પોઇન્ટ્સની સંખ્યા
  - Variance: આંકડાકીય વિચલન
  - Standard Deviation: આંકડાકીય પ્રમાણભૂત વિચલન

- **વિન્ડો કદ**: પાછળ જોવાના સમયગાળા માટેનો સમય એકમ (સેકન્ડ, મિનિટ, કલાક, દિવસ, અઠવાડિયા, મહિના અથવા વર્ષ)

- **ગુણક**: પાછળ જોવા માટેના વિન્ડો કદના એકમોની સંખ્યા (દા.ત., "દિવસ" વિન્ડો કદ સાથે 3 ગુણક = 3 દિવસની વિન્ડો)

- **સ્થાનનિર્ધારણ**: દરેક આઉટપુટ ડેટા પોઇન્ટને સમય વિન્ડોમાં ક્યાં મૂકવો:
  - Window End: વિન્ડોના સૌથી તાજેતરના ડેટા પોઇન્ટ પર (ડિફૉલ્ટ)
  - Window Midpoint: વિન્ડોના સમયગત મધ્યબિંદુ પર
  - Window Start: વિન્ડોના સૌથી જૂના ડેટા પોઇન્ટ પર
  	]],
  	["hi"] = [[
प्रत्येक डेटा पॉइंट के लिए चलती समय विंडो पर समेकित आँकड़ों की गणना करता है। यह फ़ंक्शन प्रत्येक पॉइंट से समय में पीछे जाता है और निर्दिष्ट विंडो अवधि के भीतर सभी मानों को समेकित करता है।

उदाहरण के लिए, 7-दिन की विंडो और औसत समेकन के साथ, प्रत्येक आउटपुट पॉइंट उस पॉइंट तक के पिछले 7 दिनों के सभी मानों का औसत दर्शाता है।

**कॉन्फ़िगरेशन विकल्प:**

- **समेकन**: प्रत्येक विंडो के मानों पर किया जाने वाला संचालन:
  - न्यूनतम: न्यूनतम मान
  - अधिकतम: अधिकतम मान
  - औसत: सभी मानों का माध्य
  - योग: सभी मानों का कुल
  - गणना: डेटा पॉइंट की संख्या
  - विचरण: सांख्यिकीय विचरण
  - मानक विचलन: सांख्यिकीय मानक विचलन

- **विंडो आकार**: पीछे देखने की अवधि के लिए समय इकाई (सेकंड, मिनट, घंटे, दिन, सप्ताह, महीने या वर्ष)

- **गुणक**: पीछे देखने के लिए विंडो आकार की कितनी इकाइयाँ (जैसे, विंडो आकार "दिन" और गुणक 3 = 3-दिन की विंडो)

- **स्थान**: प्रत्येक आउटपुट डेटा पॉइंट को समय विंडो में कहाँ रखना है:
  - विंडो का अंत: विंडो के सबसे हाल के डेटा पॉइंट पर (डिफ़ॉल्ट)
  - विंडो का मध्यबिंदु: विंडो के समयगत केंद्र पर
  - विंडो की शुरुआत: विंडो के सबसे पुराने डेटा पॉइंट पर
  	]],
  	["hu"] = [[
Minden adatponthoz összesített statisztikákat számít egy mozgó időablakban. A függvény minden ponttól visszafelé tekint az időben, és összesíti a megadott időablakon belüli összes értéket.

Például 7 napos ablak és átlagos összesítés esetén minden kimeneti pont az adott pontot megelőző 7 nap összes értékének átlagát jelenti.

**Konfigurációs beállítások:**

- **Összesítés**: Az egyes ablakok értékein végrehajtandó művelet:
  - Min: Minimumérték
  - Max: Maximumérték
  - Átlag: Az összes érték átlaga
  - Összeg: Az összes érték összege
  - Darabszám: Az adatpontok száma
  - Variancia: Statisztikai variancia
  - Szórás: Statisztikai szórás

- **Ablak mérete**: A visszatekintési időszak időegysége (másodperc, perc, óra, nap, hét, hónap vagy év)

- **Szorzó**: A visszatekintéshez használt ablakegységek száma (például 3-as szorzó és „nap” ablakméret = 3 napos ablak)

- **Elhelyezés**: Az egyes kimeneti adatpontok elhelyezése az időablakban:
  - Ablak vége: Az ablak legutóbbi adatpontjánál (alapértelmezett)
  - Ablak közepe: Az ablak időbeli középpontjánál
  - Ablak eleje: Az ablak legrégebbi adatpontjánál
  	]],
  	["is"] = [[
Reiknar út safntölfræði yfir hreyfanlegan tímaglugga fyrir hvern gagnapunkt. Aðgerðin lítur aftur í tímann frá hverjum punkti og safnar saman öllum gildum innan tilgreinds tímabils gluggans.

Til dæmis, með 7 daga glugga og meðaltalssöfnun táknar hver úttakspunktur meðaltal allra gilda á 7 dögunum fyrir þann punkt.

**Stillingar:**

- **Söfnun**: Aðgerðin sem á að framkvæma á gildum í hverjum glugga:
  - Lágmark: Lægsta gildi
  - Hámark: Hæsta gildi
  - Meðaltal: Meðaltal allra gilda
  - Summa: Summa allra gilda
  - Fjöldi: Fjöldi gagnapunkta
  - Dreifni: Tölfræðileg dreifni
  - Staðalfrávik: Tölfræðilegt staðalfrávik

- **Stærð glugga**: Tímaeining fyrir tímabilið sem litið er aftur um (sekúndur, mínútur, klukkustundir, dagar, vikur, mánuðir eða ár)

- **Margfeldir**: Hve margar einingar af gluggastærð á að líta aftur um (t.d. margfeldirinn 3 með gluggastærðinni „dagar“ = 3 daga gluggi)

- **Staðsetning**: Hvar í tímaglugganum hver úttaksgagnapunktur er staðsettur:
  - Lok glugga: Við nýjasta gagnapunkt gluggans (sjálfgefið)
  - Miðja glugga: Í tímalegri miðju gluggans
  - Upphaf glugga: Við elsta gagnapunkt gluggans
  	]],
  	["id"] = [[
Menghitung statistik agregat dalam jendela waktu bergerak untuk setiap titik data. Fungsi ini melihat ke belakang dari setiap titik dan menggabungkan semua nilai dalam periode jendela yang ditentukan.

Misalnya, dengan jendela 7 hari dan agregasi rata-rata, setiap titik keluaran mewakili rata-rata semua nilai dalam 7 hari sebelum titik tersebut.

**Opsi Konfigurasi:**

- **Agregasi**: Operasi yang dilakukan pada nilai dalam setiap jendela:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Rata-rata: Rata-rata semua nilai
  - Jumlah: Total semua nilai
  - Hitungan: Jumlah titik data
  - Varians: Varians statistik
  - Deviasi Standar: Deviasi standar statistik

- **Ukuran Jendela**: Satuan waktu untuk periode peninjauan ke belakang (detik, menit, jam, hari, minggu, bulan, atau tahun)

- **Pengali**: Jumlah satuan ukuran jendela untuk ditinjau ke belakang (misalnya, pengali 3 dengan ukuran jendela "hari" = jendela 3 hari)

- **Penempatan**: Lokasi penempatan setiap titik data keluaran dalam jendela waktu:
  - Akhir Jendela: Pada titik data terbaru dalam jendela (default)
  - Titik Tengah Jendela: Pada pusat waktu jendela
  - Awal Jendela: Pada titik data tertua dalam jendela
  	]],
  	["it"] = [[
Calcola statistiche aggregate su una finestra temporale mobile per ogni punto dati. La funzione guarda indietro nel tempo da ogni punto e aggrega tutti i valori all'interno del periodo della finestra specificato.

Ad esempio, con una finestra di 7 giorni e un'aggregazione media, ogni punto di output rappresenta la media di tutti i valori nei 7 giorni precedenti quel punto.

**Opzioni di configurazione:**

- **Aggregazione**: l'operazione da eseguire sui valori di ogni finestra:
  - Min: valore minimo
  - Max: valore massimo
  - Media: media di tutti i valori
  - Somma: totale di tutti i valori
  - Conteggio: numero di punti dati
  - Varianza: varianza statistica
  - Deviazione standard: deviazione standard statistica

- **Dimensione della finestra**: l'unità di tempo per il periodo retrospettivo (secondi, minuti, ore, giorni, settimane, mesi o anni)

- **Moltiplicatore**: quante unità della dimensione della finestra considerare a ritroso (ad es. moltiplicatore 3 con dimensione della finestra "giorni" = finestra di 3 giorni)

- **Posizionamento**: dove collocare ogni punto dati di output nella finestra temporale:
  - Fine della finestra: sul punto dati più recente della finestra (predefinito)
  - Punto centrale della finestra: al centro temporale della finestra
  - Inizio della finestra: sul punto dati più vecchio della finestra
  	]],
  	["ja"] = [[
各データポイントについて、移動する時間枠で集約統計を計算します。この関数は各ポイントから時間をさかのぼり、指定したウィンドウ期間内のすべての値を集約します。

たとえば、7日間のウィンドウと平均集約を使用すると、各出力ポイントはそのポイントまでの7日間にあるすべての値の平均を表します。

**設定オプション:**

- **集約**: 各ウィンドウの値に対して実行する操作:
  - Min: 最小値
  - Max: 最大値
  - Average: すべての値の平均
  - Sum: すべての値の合計
  - Count: データポイント数
  - Variance: 統計的分散
  - Standard Deviation: 統計的標準偏差

- **ウィンドウサイズ**: さかのぼる期間の時間単位（秒、分、時間、日、週、月、年）

- **乗数**: さかのぼるウィンドウサイズ単位の数（例: ウィンドウサイズが「日」で乗数が3の場合、3日間のウィンドウ）

- **配置**: 各出力データポイントを時間枠内のどこに配置するか:
  - Window End: ウィンドウ内の最も新しいデータポイント（デフォルト）
  - Window Midpoint: ウィンドウの時間的な中央
  - Window Start: ウィンドウ内の最も古いデータポイント
  	]],
  	["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿಗೆ ಚಲಿಸುವ ಸಮಯ ವಿಂಡೋದಲ್ಲಿ ಒಟ್ಟುಗೂಡಿಸಿದ ಅಂಕಿಅಂಶಗಳನ್ನು ಲೆಕ್ಕಹಾಕುತ್ತದೆ. ಪ್ರತಿ ಬಿಂದುವಿನಿಂದ ಸಮಯದಲ್ಲಿ ಹಿಂದಕ್ಕೆ ನೋಡಿ, ನಿರ್ದಿಷ್ಟ ವಿಂಡೋ ಅವಧಿಯೊಳಗಿನ ಎಲ್ಲಾ ಮೌಲ್ಯಗಳನ್ನು ಈ ಫಂಕ್ಷನ್ ಒಟ್ಟುಗೂಡಿಸುತ್ತದೆ.

ಉದಾಹರಣೆಗೆ, 7-ದಿನಗಳ ವಿಂಡೋ ಮತ್ತು ಸರಾಸರಿ ಒಟ್ಟುಗೂಡಿಸುವಿಕೆಯೊಂದಿಗೆ, ಪ್ರತಿ ಔಟ್‌ಪುಟ್ ಬಿಂದುವು ಆ ಬಿಂದುವಿಗೆ ಮುಂಚಿನ 7 ದಿನಗಳಲ್ಲಿನ ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಸರಾಸರಿಯನ್ನು ಪ್ರತಿನಿಧಿಸುತ್ತದೆ.

**ಕಾನ್ಫಿಗರೇಶನ್ ಆಯ್ಕೆಗಳು:**

- **ಒಟ್ಟುಗೂಡಿಸುವಿಕೆ**: ಪ್ರತಿ ವಿಂಡೋದಲ್ಲಿನ ಮೌಲ್ಯಗಳ ಮೇಲೆ ನಡೆಸುವ ಕಾರ್ಯಾಚರಣೆ:
  - Min: ಕನಿಷ್ಠ ಮೌಲ್ಯ
  - Max: ಗರಿಷ್ಠ ಮೌಲ್ಯ
  - Average: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಸರಾಸರಿ
  - Sum: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಮೊತ್ತ
  - Count: ಡೇಟಾ ಬಿಂದುಗಳ ಸಂಖ್ಯೆ
  - Variance: ಸಾಂಖ್ಯಿಕ ವ್ಯತ್ಯಾಸ
  - Standard Deviation: ಸಾಂಖ್ಯಿಕ ಪ್ರಮಾಣಿತ ವಿಚಲನ

- **ವಿಂಡೋ ಗಾತ್ರ**: ಹಿಂದಿನ ಅವಧಿಗಾಗಿ ಸಮಯ ಘಟಕ (ಸೆಕೆಂಡುಗಳು, ನಿಮಿಷಗಳು, ಗಂಟೆಗಳು, ದಿನಗಳು, ವಾರಗಳು, ತಿಂಗಳುಗಳು ಅಥವಾ ವರ್ಷಗಳು)

- **ಗುಣಕ**: ಹಿಂದಕ್ಕೆ ನೋಡಬೇಕಾದ ವಿಂಡೋ ಗಾತ್ರದ ಘಟಕಗಳ ಸಂಖ್ಯೆ (ಉದಾ., ವಿಂಡೋ ಗಾತ್ರ "ದಿನಗಳು" ಮತ್ತು ಗುಣಕ 3 = 3-ದಿನಗಳ ವಿಂಡೋ)

- **ಸ್ಥಾನ**: ಪ್ರತಿ ಔಟ್‌ಪುಟ್ ಡೇಟಾ ಬಿಂದುವನ್ನು ಸಮಯ ವಿಂಡೋದಲ್ಲಿ ಇರಿಸುವ ಸ್ಥಳ:
  - Window End: ವಿಂಡೋದಲ್ಲಿನ ಅತ್ಯಂತ ಇತ್ತೀಚಿನ ಡೇಟಾ ಬಿಂದುವಿನಲ್ಲಿ (ಡೀಫಾಲ್ಟ್)
  - Window Midpoint: ವಿಂಡೋದ ಸಮಯದ ಮಧ್ಯಭಾಗದಲ್ಲಿ
  - Window Start: ವಿಂಡೋದ ಅತಿ ಹಳೆಯ ಡೇಟಾ ಬಿಂದುವಿನಲ್ಲಿ
  	]],
  	["kk"] = [[
Әр дерек нүктесі үшін жылжымалы уақыт терезесі бойынша жиынтық статистиканы есептейді. Функция әр нүктеден уақыт бойынша кері қарап, көрсетілген терезе кезеңіндегі барлық мәндерді біріктіреді.

Мысалы, 7 күндік терезе және орташа біріктіру қолданылса, әр шығыс нүктесі сол нүктеге дейінгі 7 күндегі барлық мәндердің орташа мәнін көрсетеді.

**Конфигурация параметрлері:**

- **Біріктіру**: Әр терезедегі мәндерге орындалатын операция:
  - Min: Ең төменгі мән
  - Max: Ең жоғары мән
  - Average: Барлық мәндердің орташа мәні
  - Sum: Барлық мәндердің қосындысы
  - Count: Дерек нүктелерінің саны
  - Variance: Статистикалық дисперсия
  - Standard Deviation: Статистикалық стандартты ауытқу

- **Терезе өлшемі**: Артқа қарау кезеңінің уақыт бірлігі (секунд, минут, сағат, күн, апта, ай немесе жыл)

- **Көбейткіш**: Артқа қарау үшін қолданылатын терезе бірліктерінің саны (мысалы, терезе өлшемі "күндер", көбейткіш 3 болса = 3 күндік терезе)

- **Орналастыру**: Әр шығыс дерек нүктесін уақыт терезесінің қай жеріне орналастыру керек:
  - Window End: Терезедегі ең соңғы дерек нүктесінде (әдепкі)
  - Window Midpoint: Терезенің уақыт бойынша ортасында
  - Window Start: Терезедегі ең ескі дерек нүктесінде
  	]],
  	["km"] = [[
គណនាស្ថិតិប្រមូលផ្តុំលើបង្អួចពេលវេលាដែលរំកិល សម្រាប់ចំណុចទិន្នន័យនីមួយៗ។ មុខងារមើលថយក្រោយតាមពេលវេលាពីចំណុចនីមួយៗ ហើយប្រមូលផ្តុំតម្លៃទាំងអស់ក្នុងរយៈពេលបង្អួចដែលបានបញ្ជាក់។

ឧទាហរណ៍ ជាមួយបង្អួច ៧ ថ្ងៃ និងការប្រមូលផ្តុំជាមធ្យម ចំណុចលទ្ធផលនីមួយៗតំណាងឱ្យមធ្យមភាគនៃតម្លៃទាំងអស់ក្នុង ៧ ថ្ងៃមុនចំណុចនោះ។

**ជម្រើសការកំណត់៖**

- **ការប្រមូលផ្តុំ**៖ ប្រតិបត្តិការដែលត្រូវអនុវត្តលើតម្លៃក្នុងបង្អួចនីមួយៗ៖
  - Min៖ តម្លៃអប្បបរមា
  - Max៖ តម្លៃអតិបរមា
  - Average៖ មធ្យមភាគនៃតម្លៃទាំងអស់
  - Sum៖ ផលបូកសរុបនៃតម្លៃទាំងអស់
  - Count៖ ចំនួនចំណុចទិន្នន័យ
  - Variance៖ វ៉ារ្យង់ស្ថិតិ
  - Standard Deviation៖ គម្លាតស្តង់ដារស្ថិតិ

- **ទំហំបង្អួច**៖ ឯកតាពេលវេលាសម្រាប់រយៈពេលមើលថយក្រោយ (វិនាទី នាទី ម៉ោង ថ្ងៃ សប្តាហ៍ ខែ ឬឆ្នាំ)

- **មេគុណ**៖ ចំនួនឯកតាទំហំបង្អួចដែលត្រូវមើលថយក្រោយ (ឧ. មេគុណ ៣ ជាមួយទំហំបង្អួច "days" = បង្អួច ៣ ថ្ងៃ)

- **ទីតាំង**៖ កន្លែងក្នុងបង្អួចពេលវេលាដែលត្រូវដាក់ចំណុចទិន្នន័យលទ្ធផលនីមួយៗ៖
  - Window End៖ នៅចំណុចទិន្នន័យថ្មីបំផុតក្នុងបង្អួច (លំនាំដើម)
  - Window Midpoint៖ នៅចំណុចកណ្តាលពេលវេលានៃបង្អួច
  - Window Start៖ នៅចំណុចទិន្នន័យចាស់បំផុតក្នុងបង្អួច
  	]],
  	["ko"] = [[
각 데이터 포인트에 대해 이동 시간 창의 집계 통계를 계산합니다. 각 포인트에서 시간을 거슬러 올라가 지정된 창 기간 내의 모든 값을 집계합니다.

예를 들어 7일 창과 평균 집계를 사용하면 각 출력 포인트는 해당 포인트까지 이어지는 7일 동안의 모든 값의 평균을 나타냅니다.

**구성 옵션:**

- **집계**: 각 창의 값에 수행할 작업:
  - Min: 최솟값
  - Max: 최댓값
  - Average: 모든 값의 평균
  - Sum: 모든 값의 합계
  - Count: 데이터 포인트 수
  - Variance: 통계적 분산
  - Standard Deviation: 통계적 표준편차

- **창 크기**: 과거 조회 기간에 사용할 시간 단위(초, 분, 시간, 일, 주, 월 또는 년)

- **배수**: 조회할 창 크기 단위 수(예: 창 크기가 "일"이고 배수가 3이면 3일 창)

- **배치**: 각 출력 데이터 포인트를 시간 창 내 어디에 배치할지:
  - Window End: 창에서 가장 최근 데이터 포인트 위치(기본값)
  - Window Midpoint: 창의 시간상 중앙
  - Window Start: 창에서 가장 오래된 데이터 포인트 위치
  	]],
  	["ky"] = [[
Ар бир маалымат чекити үчүн жылма убакыт терезесиндеги жыйынтык статистикаларды эсептейт. Функция ар бир чекиттен убакыт боюнча артка карап, көрсөтүлгөн терезе мезгилиндеги бардык маанилерди бириктирет.

Мисалы, 7 күндүк терезе жана орточо бириктирүү тандалса, ар бир чыгаруу чекити ошол чекитке чейинки 7 күндөгү бардык маанилердин орточосун көрсөтөт.

**Тууралоо параметрлери:**

- **Бириктирүү**: Ар бир терезедеги маанилерге аткарылуучу операция:
  - Min: Минималдуу маани
  - Max: Максималдуу маани
  - Average: Бардык маанилердин орточо мааниси
  - Sum: Бардык маанилердин суммасы
  - Count: Маалымат чекиттеринин саны
  - Variance: Статистикалык дисперсия
  - Standard Deviation: Статистикалык стандарттык четтөө

- **Терезенин өлчөмү**: Артка каралуучу мезгилдин убакыт бирдиги (секунд, мүнөт, саат, күн, жума, ай же жыл)

- **Көбөйткүч**: Артка каралуучу терезе бирдиктеринин саны (мисалы, терезе өлчөмү "күн", көбөйткүч 3 болсо = 3 күндүк терезе)

- **Жайгаштыруу**: Ар бир чыгаруу маалымат чекитин убакыт терезесинде жайгаштыруу орду:
  - Терезенин аягы: Терезедеги эң акыркы маалымат чекитинде (демейки)
  - Терезенин ортосу: Терезенин убакыттык ортосунда
  - Терезенин башы: Терезедеги эң эски маалымат чекитинде
  	]],
  	["lo"] = [[
ຄຳນວນສະຖິຕິລວມໃນໜ້າຕ່າງເວລາທີ່ເຄື່ອນໄປສຳລັບແຕ່ລະຈຸດຂໍ້ມູນ. ຟັງຊັນຈະເບິ່ງຍ້ອນກັບໄປຕາມເວລາຈາກແຕ່ລະຈຸດ ແລະລວມຄ່າທັງໝົດພາຍໃນໄລຍະໜ້າຕ່າງທີ່ກຳນົດ.

ຕົວຢ່າງ, ດ້ວຍໜ້າຕ່າງ 7 ມື້ ແລະການລວມແບບສະເລ່ຍ, ແຕ່ລະຈຸດຜົນລັບຈະແທນຄ່າສະເລ່ຍຂອງຄ່າທັງໝົດໃນ 7 ມື້ກ່ອນໜ້າຈຸດນັ້ນ.

**ຕົວເລືອກການກຳນົດຄ່າ:**

- **ການລວມ**: ການດຳເນີນການກັບຄ່າໃນແຕ່ລະໜ້າຕ່າງ:
  - Min: ຄ່າຕ່ຳສຸດ
  - Max: ຄ່າສູງສຸດ
  - Average: ຄ່າສະເລ່ຍຂອງຄ່າທັງໝົດ
  - Sum: ຜົນລວມຂອງຄ່າທັງໝົດ
  - Count: ຈຳນວນຈຸດຂໍ້ມູນ
  - Variance: ຄວາມແປປວນທາງສະຖິຕິ
  - Standard Deviation: ຄ່າບ່ຽງເບນມາດຕະຖານທາງສະຖິຕິ

- **ຂະໜາດໜ້າຕ່າງ**: ໜ່ວຍເວລາສຳລັບໄລຍະຍ້ອນກັບ (ວິນາທີ, ນາທີ, ຊົ່ວໂມງ, ມື້, ອາທິດ, ເດືອນ ຫຼື ປີ)

- **ຕົວຄູນ**: ຈຳນວນໜ່ວຍຂະໜາດໜ້າຕ່າງທີ່ຈະຍ້ອນກັບ (ເຊັ່ນ, ຕົວຄູນ 3 ກັບຂະໜາດໜ້າຕ່າງ "ມື້" = ໜ້າຕ່າງ 3 ມື້)

- **ຕຳແໜ່ງ**: ບ່ອນວາງແຕ່ລະຈຸດຜົນລັບໃນໜ້າຕ່າງເວລາ:
  - Window End: ຢູ່ຈຸດຂໍ້ມູນຫຼ້າສຸດໃນໜ້າຕ່າງ (ຄ່າເລີ່ມຕົ້ນ)
  - Window Midpoint: ຢູ່ຈຸດກາງດ້ານເວລາຂອງໜ້າຕ່າງ
  - Window Start: ຢູ່ຈຸດຂໍ້ມູນເກົ່າສຸດໃນໜ້າຕ່າງ
  	]],
  	["lv"] = [[
Aprēķina apkopotu statistiku slīdošā laika logā katram datu punktam. Funkcija skatās atpakaļ laikā no katra punkta un apkopo visas vērtības norādītajā loga periodā.

Piemēram, ar 7 dienu logu un vidējās vērtības apkopošanu katrs izvades punkts atspoguļo visu 7 dienu laikā līdz šim punktam esošo vērtību vidējo.

**Konfigurācijas opcijas:**

- **Apkopošana**: Darbība, kas jāveic ar vērtībām katrā logā:
  - Min: Minimālā vērtība
  - Max: Maksimālā vērtība
  - Average: Visu vērtību vidējā vērtība
  - Sum: Visu vērtību summa
  - Count: Datu punktu skaits
  - Variance: Statistiskā dispersija
  - Standard Deviation: Statistiskā standartnovirze

- **Loga lielums**: Laika vienība atpakaļskatīšanās periodam (sekundes, minūtes, stundas, dienas, nedēļas, mēneši vai gadi)

- **Reizinātājs**: Cik loga lieluma vienību skatīties atpakaļ (piem., reizinātājs 3 un loga lielums "dienas" = 3 dienu logs)

- **Novietojums**: Kur laika logā novietot katru izvades datu punktu:
  - Loga beigas: Pie jaunākā datu punkta logā (pēc noklusējuma)
  - Loga viduspunkts: Laika loga centrā
  - Loga sākums: Pie vecākā datu punkta logā
  	]],
  	["lt"] = [[
Apskaičiuoja apibendrintą statistiką slenkančiame laiko lange kiekvienam duomenų taškui. Funkcija nuo kiekvieno taško žvelgia atgal laike ir apibendrina visas reikšmes nurodytame lango laikotarpyje.

Pavyzdžiui, pasirinkus 7 dienų langą ir vidurkio apibendrinimą, kiekvienas išvesties taškas reiškia visų per 7 dienas iki to taško sukauptų reikšmių vidurkį.

**Konfigūracijos parinktys:**

- **Apibendrinimas**: Kiekvieno lango reikšmėms taikoma operacija:
  - Min.: Mažiausia reikšmė
  - Maks.: Didžiausia reikšmė
  - Vidurkis: Visų reikšmių vidurkis
  - Suma: Visų reikšmių suma
  - Kiekis: Duomenų taškų skaičius
  - Dispersija: Statistinė dispersija
  - Standartinis nuokrypis: Statistinis standartinis nuokrypis

- **Lango dydis**: Atgalinio laikotarpio laiko vienetas (sekundės, minutės, valandos, dienos, savaitės, mėnesiai arba metai)

- **Daugiklis**: Kiek lango dydžio vienetų grįžti atgal (pvz., daugiklis 3 ir lango dydis „dienos“ = 3 dienų langas)

- **Vieta**: Kur laiko lange patalpinti kiekvieną išvesties duomenų tašką:
  - Lango pabaiga: Ties naujausiu lango duomenų tašku (numatyta)
  - Lango vidurys: Laikiniame lango centre
  - Lango pradžia: Ties seniausiu lango duomenų tašku
  	]],
  	["mk"] = [[
Пресметува збирни статистики во подвижен временски прозорец за секоја точка на податоци. Функцијата се движи наназад низ времето од секоја точка и ги собира сите вредности во зададениот период на прозорецот.

На пример, со 7-дневен прозорец и просечна агрегација, секоја излезна точка го претставува просекот од сите вредности во 7-те дена што ѝ претходат.

**Опции за конфигурирање:**

- **Агрегација**: Операцијата што се извршува врз вредностите во секој прозорец:
  - Min: Минимална вредност
  - Max: Максимална вредност
  - Average: Просек од сите вредности
  - Sum: Збир од сите вредности
  - Count: Број на точки на податоци
  - Variance: Статистичка варијанса
  - Standard Deviation: Статистичко стандардно отстапување

- **Големина на прозорецот**: Временската единица за периодот наназад (секунди, минути, часови, денови, недели, месеци или години)

- **Множител**: Колку единици од големината на прозорецот да се разгледаат наназад (на пр., множител 3 со големина „денови“ = 3-дневен прозорец)

- **Поставување**: Каде во временскиот прозорец да се постави секоја излезна точка на податоци:
  - Крај на прозорецот: Кај најновата точка на податоци во прозорецот (стандардно)
  - Средина на прозорецот: Во временскиот центар на прозорецот
  - Почеток на прозорецот: Кај најстарата точка на податоци во прозорецот
  	]],
  	["ms"] = [[
Mengira statistik agregat dalam tetingkap masa bergerak untuk setiap titik data. Fungsi ini melihat ke belakang dari setiap titik dan mengagregat semua nilai dalam tempoh tetingkap yang ditentukan.

Sebagai contoh, dengan tetingkap 7 hari dan pengagregatan purata, setiap titik output mewakili purata semua nilai dalam 7 hari sebelum titik tersebut.

**Pilihan Konfigurasi:**

- **Pengagregatan**: Operasi yang hendak dilaksanakan pada nilai dalam setiap tetingkap:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Purata: Min semua nilai
  - Jumlah: Jumlah semua nilai
  - Kiraan: Bilangan titik data
  - Varians: Varians statistik
  - Sisihan Piawai: Sisihan piawai statistik

- **Saiz Tetingkap**: Unit masa untuk tempoh lihat semula (saat, minit, jam, hari, minggu, bulan atau tahun)

- **Pengganda**: Bilangan unit saiz tetingkap untuk dilihat semula (contohnya, pengganda 3 dengan saiz tetingkap "hari" = tetingkap 3 hari)

- **Penempatan**: Tempat untuk meletakkan setiap titik data output dalam tetingkap masa:
  - Hujung Tetingkap: Pada titik data paling terkini dalam tetingkap (lalai)
  - Titik Tengah Tetingkap: Pada pusat masa tetingkap
  - Permulaan Tetingkap: Pada titik data paling lama dalam tetingkap
  	]],
  	["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിനുമായി സഞ്ചരിക്കുന്ന സമയ വിൻഡോയിലെ സംഗ്രഹ സ്ഥിതിവിവരക്കണക്കുകൾ കണക്കാക്കുന്നു. ഓരോ പോയിന്റിൽ നിന്നും സമയത്തിൽ പിന്നോട്ടുനോക്കി, നിർദ്ദിഷ്ട വിൻഡോ കാലയളവിനുള്ളിലെ എല്ലാ മൂല്യങ്ങളും ഫംഗ്ഷൻ സംഗ്രഹിക്കുന്നു.

ഉദാഹരണത്തിന്, 7 ദിവസത്തെ വിൻഡോയും ശരാശരി അഗ്രിഗേഷനും ഉപയോഗിക്കുമ്പോൾ, ഓരോ ഔട്ട്പുട്ട് പോയിന്റും ആ പോയിന്റിന് മുമ്പുള്ള 7 ദിവസങ്ങളിലെ എല്ലാ മൂല്യങ്ങളുടെയും ശരാശരിയെ പ്രതിനിധീകരിക്കുന്നു.

**കോൺഫിഗറേഷൻ ഓപ്ഷനുകൾ:**

- **Aggregation**: ഓരോ വിൻഡോയിലെയും മൂല്യങ്ങളിൽ നടത്തേണ്ട പ്രവർത്തനം:
  - Min: കുറഞ്ഞ മൂല്യം
  - Max: കൂടിയ മൂല്യം
  - Average: എല്ലാ മൂല്യങ്ങളുടെയും ശരാശരി
  - Sum: എല്ലാ മൂല്യങ്ങളുടെയും ആകെ തുക
  - Count: ഡാറ്റാ പോയിന്റുകളുടെ എണ്ണം
  - Variance: സ്ഥിതിവിവര വ്യതിയാനം
  - Standard Deviation: സ്ഥിതിവിവര സ്റ്റാൻഡേർഡ് ഡിവിയേഷൻ

- **Window Size**: പിന്നോട്ടുനോക്കേണ്ട കാലയളവിന്റെ സമയ യൂണിറ്റ് (സെക്കൻഡ്, മിനിറ്റ്, മണിക്കൂർ, ദിവസം, ആഴ്ച, മാസം, അല്ലെങ്കിൽ വർഷം)

- **Multiplier**: പിന്നോട്ടുനോക്കേണ്ട വിൻഡോ യൂണിറ്റുകളുടെ എണ്ണം (ഉദാ., window size "days" ഉം multiplier 3 ഉം = 3 ദിവസത്തെ വിൻഡോ)

- **Placement**: ഓരോ ഔട്ട്പുട്ട് ഡാറ്റാ പോയിന്റും സമയ വിൻഡോയിൽ സ്ഥാപിക്കേണ്ട സ്ഥാനം:
  - Window End: വിൻഡോയിലെ ഏറ്റവും പുതിയ ഡാറ്റാ പോയിന്റിൽ (സ്ഥിരസ്ഥിതി)
  - Window Midpoint: വിൻഡോയുടെ സമയമധ്യത്തിൽ
  - Window Start: വിൻഡോയിലെ ഏറ്റവും പഴയ ഡാറ്റാ പോയിന്റിൽ
  	]],
  	["mr"] = [[
प्रत्येक डेटा पॉइंटसाठी सरकत्या कालावधी-विंडोवर एकत्रित सांख्यिकी मोजते. प्रत्येक पॉइंटपासून मागील काळात पाहून, निर्दिष्ट विंडो कालावधीतील सर्व मूल्ये एकत्रित करते.

उदाहरणार्थ, 7 दिवसांची विंडो आणि सरासरी एकत्रीकरण वापरल्यास, प्रत्येक आउटपुट पॉइंट त्या पॉइंटपर्यंतच्या मागील 7 दिवसांतील सर्व मूल्यांची सरासरी दर्शवतो.

**कॉन्फिगरेशन पर्याय:**

- **एकत्रीकरण**: प्रत्येक विंडोतील मूल्यांवर करावयाची क्रिया:
  - किमान: किमान मूल्य
  - कमाल: कमाल मूल्य
  - सरासरी: सर्व मूल्यांचा मध्य
  - बेरीज: सर्व मूल्यांची एकूण बेरीज
  - संख्या: डेटा पॉइंट्सची संख्या
  - विचरण: सांख्यिकीय विचरण
  - प्रमाणित विचलन: सांख्यिकीय प्रमाणित विचलन

- **विंडो आकार**: मागील कालावधीचे वेळेचे एकक (सेकंद, मिनिटे, तास, दिवस, आठवडे, महिने किंवा वर्षे)

- **गुणक**: किती विंडो-आकाराच्या एककांइतका मागील कालावधी पाहायचा (उदा., विंडो आकार "दिवस" आणि गुणक 3 = 3 दिवसांची विंडो)

- **स्थान**: प्रत्येक आउटपुट डेटा पॉइंट विंडोमध्ये कुठे ठेवायचा:
  - विंडोचा शेवट: विंडोतील सर्वात अलीकडील डेटा पॉइंटवर (डीफॉल्ट)
  - विंडोचा मध्यबिंदू: विंडोच्या कालगत मध्यावर
  - विंडोची सुरुवात: विंडोतील सर्वात जुना डेटा पॉइंटवर
  	]],
  	["mn"] = [[
Өгөгдлийн цэг бүрийн хувьд шилжих хугацааны цонхон дахь нэгтгэсэн статистикийг тооцоолно. Функц цэг бүрээс хугацааны хувьд ухарч, заасан цонхны хугацаанд багтах бүх утгыг нэгтгэнэ.

Жишээлбэл, 7 хоногийн цонх ба дундаж нэгтгэл ашиглавал гаралтын цэг бүр тухайн цэгээс өмнөх 7 хоногийн бүх утгын дунджийг илэрхийлнэ.

**Тохиргооны сонголтууд:**

- **Нэгтгэл**: Цонх бүрийн утгад хийх үйлдэл:
  - Min: Хамгийн бага утга
  - Max: Хамгийн их утга
  - Average: Бүх утгын дундаж
  - Sum: Бүх утгын нийлбэр
  - Count: Өгөгдлийн цэгийн тоо
  - Variance: Статистикийн дисперс
  - Standard Deviation: Статистикийн стандарт хазайлт

- **Цонхны хэмжээ**: Өмнөх хугацааг тооцох хугацааны нэгж (секунд, минут, цаг, өдөр, долоо хоног, сар эсвэл жил)

- **Үржүүлэгч**: Буцаж тооцох цонхны нэгжийн тоо (жишээ нь, цонхны хэмжээ "өдөр", үржүүлэгч 3 бол 3 өдрийн цонх)

- **Байрлал**: Гаралтын өгөгдлийн цэгийг хугацааны цонхны хаана байрлуулах:
  - Window End: Цонхны хамгийн сүүлийн өгөгдлийн цэг дээр (анхдагч)
  - Window Midpoint: Цонхны хугацааны дунд цэгт
  - Window Start: Цонхны хамгийн эхний өгөгдлийн цэг дээр
  	]],
  	["ne"] = [[
प्रत्येक डेटा बिन्दुका लागि चलायमान समय विन्डोमा समेकित तथ्याङ्क गणना गर्छ। प्रत्येक बिन्दुबाट समयमै पछाडि हेरेर निर्दिष्ट विन्डो अवधिभित्रका सबै मान समेकित गर्छ।

उदाहरणका लागि, ७-दिने विन्डो र औसत समेकन हुँदा प्रत्येक आउटपुट बिन्दुले त्यस बिन्दुसम्मका अघिल्ला ७ दिनका सबै मानको औसत जनाउँछ।

**कन्फिगरेसन विकल्पहरू:**

- **समेकन**: प्रत्येक विन्डोका मानमा लागू गर्ने कार्य:
  - Min: न्यूनतम मान
  - Max: अधिकतम मान
  - Average: सबै मानहरूको औसत
  - Sum: सबै मानहरूको योग
  - Count: डेटा बिन्दुहरूको संख्या
  - Variance: सांख्यिकीय विचरण
  - Standard Deviation: सांख्यिकीय मानक विचलन

- **विन्डो आकार**: पछाडि हेर्ने अवधिको समय एकाइ (सेकेन्ड, मिनेट, घण्टा, दिन, हप्ता, महिना वा वर्ष)

- **गुणक**: पछाडि हेर्ने विन्डो आकारका एकाइहरूको संख्या (जस्तै, विन्डो आकार "दिन" र गुणक ३ = ३-दिने विन्डो)

- **स्थान**: प्रत्येक आउटपुट डेटा बिन्दुलाई समय विन्डोमा राख्ने स्थान:
  - Window End: विन्डोको सबैभन्दा पछिल्लो डेटा बिन्दुमा (पूर्वनिर्धारित)
  - Window Midpoint: विन्डोको समयगत केन्द्रमा
  - Window Start: विन्डोको सबैभन्दा पुरानो डेटा बिन्दुमा
  	]],
  	["no"] = [[
Beregner aggregerte statistikker over et bevegelig tidsvindu for hvert datapunkt. Funksjonen ser bakover i tid fra hvert punkt og aggregerer alle verdier innenfor den angitte vindusperioden.

Med et 7-dagers vindu og gjennomsnittsaggregering representerer for eksempel hvert utgående punkt gjennomsnittet av alle verdier i de 7 dagene frem til punktet.

**Konfigurasjonsalternativer:**

- **Aggregering**: Operasjonen som skal utføres på verdiene i hvert vindu:
  - Min: Minimumsverdi
  - Maks: Maksimumsverdi
  - Gjennomsnitt: Gjennomsnittet av alle verdier
  - Sum: Summen av alle verdier
  - Antall: Antall datapunkter
  - Varians: Statistisk varians
  - Standardavvik: Statistisk standardavvik

- **Vindusstørrelse**: Tidsenheten for tilbakeblikkperioden (sekunder, minutter, timer, dager, uker, måneder eller år)

- **Multiplikator**: Hvor mange vindusstørrelsesenheter det skal ses tilbake (f.eks. multiplikator 3 med vindusstørrelse «dager» = 3-dagers vindu)

- **Plassering**: Hvor i tidsvinduet hvert utgående datapunkt skal plasseres:
  - Slutt på vinduet: Ved det nyeste datapunktet i vinduet (standard)
  - Midtpunkt i vinduet: Ved vinduets tidsmessige sentrum
  - Start på vinduet: Ved det eldste datapunktet i vinduet
  	]],
  	["pl"] = [[
Oblicza zagregowane statystyki w ruchomym oknie czasowym dla każdego punktu danych. Funkcja cofa się w czasie od każdego punktu i agreguje wszystkie wartości mieszczące się w określonym okresie okna.

Na przykład przy oknie 7-dniowym i agregacji średniej każdy punkt wyjściowy reprezentuje średnią wszystkich wartości z 7 dni poprzedzających ten punkt.

**Opcje konfiguracji:**

- **Agregacja**: Operacja wykonywana na wartościach w każdym oknie:
  - Min: Wartość minimalna
  - Max: Wartość maksymalna
  - Średnia: Średnia wszystkich wartości
  - Suma: Suma wszystkich wartości
  - Liczba: Liczba punktów danych
  - Wariancja: Wariancja statystyczna
  - Odchylenie standardowe: Odchylenie standardowe

- **Rozmiar okna**: Jednostka czasu okresu analizy wstecz (sekundy, minuty, godziny, dni, tygodnie, miesiące lub lata)

- **Mnożnik**: Liczba jednostek rozmiaru okna uwzględnianych wstecz (np. mnożnik 3 i rozmiar okna „dni” = okno 3-dniowe)

- **Położenie**: Miejsce umieszczenia każdego wyjściowego punktu danych w oknie czasowym:
  - Koniec okna: Przy najnowszym punkcie danych w oknie (domyślnie)
  - Środek okna: W czasowym środku okna
  - Początek okna: Przy najstarszym punkcie danych w oknie
  	]],
  	["pt"] = [[
Calcula estatísticas agregadas numa janela temporal móvel para cada ponto de dados. A função recua no tempo a partir de cada ponto e agrega todos os valores dentro do período da janela especificado.

Por exemplo, com uma janela de 7 dias e uma agregação pela média, cada ponto de saída representa a média de todos os valores nos 7 dias anteriores a esse ponto.

**Opções de configuração:**

- **Agregação**: A operação a executar nos valores de cada janela:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Média: Média de todos os valores
  - Soma: Total de todos os valores
  - Contagem: Número de pontos de dados
  - Variância: Variância estatística
  - Desvio padrão: Desvio padrão estatístico

- **Tamanho da janela**: A unidade de tempo do período de retrocesso (segundos, minutos, horas, dias, semanas, meses ou anos)

- **Multiplicador**: Quantas unidades do tamanho da janela retroceder (por exemplo, multiplicador 3 com tamanho da janela "dias" = janela de 3 dias)

- **Posicionamento**: Onde colocar cada ponto de dados de saída na janela temporal:
  - Fim da janela: No ponto de dados mais recente da janela (predefinição)
  - Ponto médio da janela: No centro temporal da janela
  - Início da janela: No ponto de dados mais antigo da janela
  	]],
  	["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਲਈ ਚਲਦੀ ਸਮਾਂ-ਖਿੜਕੀ 'ਤੇ ਇਕੱਠੇ ਕੀਤੇ ਅੰਕੜੇ ਗਣਨਾ ਕਰਦਾ ਹੈ। ਫੰਕਸ਼ਨ ਹਰੇਕ ਪੁਆਇੰਟ ਤੋਂ ਸਮੇਂ ਵਿੱਚ ਪਿੱਛੇ ਵੱਲ ਦੇਖਦਾ ਹੈ ਅਤੇ ਨਿਰਧਾਰਤ ਵਿੰਡੋ ਅਵਧੀ ਅੰਦਰਲੇ ਸਾਰੇ ਮੁੱਲਾਂ ਨੂੰ ਇਕੱਠਾ ਕਰਦਾ ਹੈ।

ਉਦਾਹਰਨ ਲਈ, 7-ਦਿਨਾਂ ਦੀ ਵਿੰਡੋ ਅਤੇ ਔਸਤ ਇਕੱਠ ਨਾਲ, ਹਰੇਕ ਆਉਟਪੁੱਟ ਪੁਆਇੰਟ ਉਸ ਪੁਆਇੰਟ ਤੋਂ ਪਹਿਲਾਂ ਦੇ 7 ਦਿਨਾਂ ਦੇ ਸਾਰੇ ਮੁੱਲਾਂ ਦੀ ਔਸਤ ਦਰਸਾਉਂਦਾ ਹੈ।

**ਕੌਂਫਿਗਰੇਸ਼ਨ ਵਿਕਲਪ:**

- **ਇਕੱਠ**: ਹਰੇਕ ਵਿੰਡੋ ਦੇ ਮੁੱਲਾਂ 'ਤੇ ਕੀਤੀ ਜਾਣ ਵਾਲੀ ਕਾਰਵਾਈ:
  - Min: ਨਿਊਨਤਮ ਮੁੱਲ
  - Max: ਅਧਿਕਤਮ ਮੁੱਲ
  - Average: ਸਾਰੇ ਮੁੱਲਾਂ ਦੀ ਔਸਤ
  - Sum: ਸਾਰੇ ਮੁੱਲਾਂ ਦਾ ਜੋੜ
  - Count: ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੀ ਗਿਣਤੀ
  - Variance: ਅੰਕੜਾਤਮਕ ਵੈਰੀਅੰਸ
  - Standard Deviation: ਅੰਕੜਾਤਮਕ ਮਿਆਰੀ ਵਿਸਥਾਪਨ

- **ਵਿੰਡੋ ਆਕਾਰ**: ਪਿੱਛੇ ਦੇਖਣ ਵਾਲੀ ਅਵਧੀ ਦੀ ਸਮਾਂ ਇਕਾਈ (ਸਕਿੰਟ, ਮਿੰਟ, ਘੰਟੇ, ਦਿਨ, ਹਫ਼ਤੇ, ਮਹੀਨੇ ਜਾਂ ਸਾਲ)

- **ਗੁਣਕ**: ਵਿੰਡੋ ਆਕਾਰ ਦੀਆਂ ਕਿੰਨੀਆਂ ਇਕਾਈਆਂ ਪਿੱਛੇ ਦੇਖਣੀਆਂ ਹਨ (ਉਦਾਹਰਨ ਲਈ, ਵਿੰਡੋ ਆਕਾਰ "ਦਿਨ" ਅਤੇ ਗੁਣਕ 3 = 3-ਦਿਨਾਂ ਦੀ ਵਿੰਡੋ)

- **ਸਥਿਤੀ**: ਹਰੇਕ ਆਉਟਪੁੱਟ ਡਾਟਾ ਪੁਆਇੰਟ ਨੂੰ ਸਮਾਂ-ਖਿੜਕੀ ਵਿੱਚ ਕਿੱਥੇ ਰੱਖਣਾ ਹੈ:
  - Window End: ਵਿੰਡੋ ਦੇ ਸਭ ਤੋਂ ਨਵੇਂ ਡਾਟਾ ਪੁਆਇੰਟ 'ਤੇ (ਮੂਲ)
  - Window Midpoint: ਵਿੰਡੋ ਦੇ ਸਮੇਂਕ ਕੇਂਦਰ 'ਤੇ
  - Window Start: ਵਿੰਡੋ ਦੇ ਸਭ ਤੋਂ ਪੁਰਾਣੇ ਡਾਟਾ ਪੁਆਇੰਟ 'ਤੇ
  	]],
  	["ro"] = [[
Calculează statistici agregate într-o fereastră de timp mobilă pentru fiecare punct de date. Funcția se uită înapoi în timp de la fiecare punct și agregă toate valorile din perioada ferestrei specificate.

De exemplu, cu o fereastră de 7 zile și o agregare prin medie, fiecare punct rezultat reprezintă media tuturor valorilor din cele 7 zile premergătoare acelui punct.

**Opțiuni de configurare:**

- **Agregare**: Operația efectuată asupra valorilor din fiecare fereastră:
  - Min: Valoarea minimă
  - Max: Valoarea maximă
  - Medie: Media tuturor valorilor
  - Sumă: Totalul tuturor valorilor
  - Număr: Numărul de puncte de date
  - Varianță: Varianța statistică
  - Deviație standard: Deviația standard statistică

- **Dimensiunea ferestrei**: Unitatea de timp pentru perioada retrospectivă (secunde, minute, ore, zile, săptămâni, luni sau ani)

- **Multiplicator**: Numărul de unități ale dimensiunii ferestrei pentru retrospectivă (de exemplu, un multiplicator de 3 cu dimensiunea ferestrei „zile” = fereastră de 3 zile)

- **Poziționare**: Unde să fie plasat fiecare punct de date rezultat în fereastra de timp:
  - Sfârșitul ferestrei: La cel mai recent punct de date din fereastră (implicit)
  - Mijlocul ferestrei: În centrul temporal al ferestrei
  - Începutul ferestrei: La cel mai vechi punct de date din fereastră
  	]],
  	["rm"] = [[
Calculescha statisticas agregadas sur ina fanestra temporala movibla per mintga punct da datas. La funcziun guarda enavos en il temp a partir da mintga punct e agregescha tut las valurs entaifer la fanestra spezificada.

Per exempel, cun ina fanestra da 7 dis ed ina agregaziun da la media, represchenta mintga punct d'output la media da tut las valurs dals 7 dis avant quel punct.

**Opziuns da configuraziun:**

- **Agregaziun**: L'operaziun da far sin las valurs en mintga fanestra:
  - Min: Valur minimala
  - Max: Valur maximala
  - Media: Media da tut las valurs
  - Summa: Total da tut las valurs
  - Dumber: Dumber da puncts da datas
  - Varianta: Varianta statistica
  - Deviaziun standard: Deviaziun standard statistica

- **Grondezza da la fanestra**: L'unità da temp per la perioda retrospectiva (secundas, minutas, uras, dis, emnas, mais u onns)

- **Multiplicatur**: Quants units da la grondezza da la fanestra guardar enavos (p.ex. multiplicatur 3 cun grondezza "dis" = fanestra da 3 dis)

- **Posiziun**: Nua plazzar mintga punct da datas d'output en la fanestra temporala:
  - Fin da la fanestra: A l'ultim punct da datas en la fanestra (standard)
  - Mez da la fanestra: Al center temporal da la fanestra
  - Cumenzament da la fanestra: Al pli vegl punct da datas en la fanestra
  	]],
  	["ru"] = [[
Вычисляет агрегированные статистики в скользящем временном окне для каждой точки данных. Функция отсчитывает время назад от каждой точки и объединяет все значения в указанном периоде окна.

Например, при окне в 7 дней и агрегации среднего каждая выходная точка представляет среднее всех значений за 7 дней, предшествующих этой точке.

**Параметры конфигурации:**

- **Агрегация**: Операция над значениями в каждом окне:
  - Минимум: Минимальное значение
  - Максимум: Максимальное значение
  - Среднее: Среднее всех значений
  - Сумма: Сумма всех значений
  - Количество: Число точек данных
  - Дисперсия: Статистическая дисперсия
  - Стандартное отклонение: Статистическое стандартное отклонение

- **Размер окна**: Единица времени для периода ретроспективы (секунды, минуты, часы, дни, недели, месяцы или годы)

- **Множитель**: Количество единиц размера окна для ретроспективы (например, множитель 3 и размер окна «дни» = окно в 3 дня)

- **Размещение**: Где размещать каждую выходную точку данных во временном окне:
  - Конец окна: В момент самой поздней точки данных окна (по умолчанию)
  - Середина окна: В центре временного окна
  - Начало окна: В момент самой ранней точки данных окна
  	]],
  	["sr"] = [[
Izračunava zbirnu statistiku tokom pomerajućeg vremenskog prozora za svaku tačku podataka. Funkcija gleda unazad kroz vreme od svake tačke i objedinjuje sve vrednosti unutar navedenog perioda prozora.

Na primer, uz prozor od 7 dana i objedinjavanje prosekom, svaka izlazna tačka predstavlja prosek svih vrednosti u 7 dana koji prethode toj tački.

**Opcije konfiguracije:**

- **Objedinjavanje**: Operacija koja se izvršava nad vrednostima u svakom prozoru:
  - Min: Minimalna vrednost
  - Max: Maksimalna vrednost
  - Prosek: Srednja vrednost svih vrednosti
  - Zbir: Zbir svih vrednosti
  - Broj: Broj tačaka podataka
  - Varijansa: Statistička varijansa
  - Standardna devijacija: Statistička standardna devijacija

- **Veličina prozora**: Vremenska jedinica perioda gledanja unazad (sekunde, minuti, sati, dani, nedelje, meseci ili godine)

- **Množilac**: Broj jedinica veličine prozora za gledanje unazad (npr. množilac 3 uz veličinu prozora „dani“ = prozor od 3 dana)

- **Položaj**: Mesto u vremenskom prozoru na koje se postavlja svaka izlazna tačka podataka:
  - Kraj prozora: Na najnoviju tačku podataka u prozoru (podrazumevano)
  - Sredina prozora: Na vremensku sredinu prozora
  - Početak prozora: Na najstariju tačku podataka u prozoru
  	]],
  	["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයක් සඳහාම චලනය වන කාල කවුළුවක් හරහා එකතු කළ සංඛ්‍යාන ගණනය කරයි. ශ්‍රිතය සෑම ලක්ෂ්‍යයකින්ම කාලය පසුපසට බලා, නිශ්චිත කවුළු කාලාන්තරය තුළ ඇති සියලු අගයන් එකතු කරයි.

උදාහරණයක් ලෙස, දින 7ක කවුළුවක් සහ සාමාන්‍ය එකතු කිරීමක් සමඟ, එක් එක් ප්‍රතිදාන ලක්ෂ්‍යය එම ලක්ෂ්‍යයට පෙර දින 7 තුළ ඇති සියලු අගයන්ගේ සාමාන්‍යය නිරූපණය කරයි.

**වින්‍යාස විකල්ප:**

- **එකතු කිරීම**: සෑම කවුළුවකම අගයන් මත සිදු කළ යුතු ක්‍රියාව:
  - අවමය: අවම අගය
  - උපරිමය: උපරිම අගය
  - සාමාන්‍යය: සියලු අගයන්ගේ මධ්‍යන්‍යය
  - එකතුව: සියලු අගයන්ගේ එකතුව
  - ගණන: දත්ත ලක්ෂ්‍ය ගණන
  - විචලතාව: සංඛ්‍යාන විචලතාව
  - සම්මත අපගමනය: සංඛ්‍යාන සම්මත අපගමනය

- **කවුළු ප්‍රමාණය**: පසුපස බැලීමේ කාල පරාසයේ කාල ඒකකය (තත්පර, මිනිත්තු, පැය, දින, සති, මාස හෝ වසර)

- **ගුණකය**: පසුපස බැලිය යුතු කවුළු ප්‍රමාණ ඒකක ගණන (උදා: “දින” කවුළු ප්‍රමාණය සමඟ ගුණකය 3 = දින 3ක කවුළුව)

- **ස්ථානගත කිරීම**: එක් එක් ප්‍රතිදාන දත්ත ලක්ෂ්‍යය කාල කවුළුව තුළ ස්ථානගත කළ යුතු ස්ථානය:
  - කවුළු අවසානය: කවුළුවේ නවතම දත්ත ලක්ෂ්‍යයේදී (පෙරනිමිය)
  - කවුළු මධ්‍ය ලක්ෂ්‍යය: කවුළුවේ කාලමය මධ්‍යයේ
  - කවුළු ආරම්භය: කවුළුවේ පැරණිතම දත්ත ලක්ෂ්‍යයේදී
  	]],
  	["sk"] = [[
Vypočíta agregované štatistiky v pohyblivom časovom okne pre každý údajový bod. Funkcia sa pri každom bode pozerá späť v čase a agreguje všetky hodnoty v určenom období okna.

Napríklad pri 7-dňovom okne a priemernej agregácii predstavuje každý výstupný bod priemer všetkých hodnôt zo 7 dní predchádzajúcich danému bodu.

**Možnosti konfigurácie:**

- **Agregácia**: Operácia vykonaná s hodnotami v každom okne:
  - Minimum: Najnižšia hodnota
  - Maximum: Najvyššia hodnota
  - Priemer: Priemer všetkých hodnôt
  - Súčet: Súčet všetkých hodnôt
  - Počet: Počet údajových bodov
  - Variancia: Štatistická variancia
  - Smerodajná odchýlka: Štatistická smerodajná odchýlka

- **Veľkosť okna**: Časová jednotka obdobia spätného pohľadu (sekundy, minúty, hodiny, dni, týždne, mesiace alebo roky)

- **Násobiteľ**: Počet jednotiek veľkosti okna, o ktoré sa má pozerať späť (napr. násobiteľ 3 s veľkosťou okna „dni“ = 3-dňové okno)

- **Umiestnenie**: Kam v časovom okne umiestniť každý výstupný údajový bod:
  - Koniec okna: Pri najnovšom údajovom bode v okne (predvolené)
  - Stred okna: V časovom strede okna
  - Začiatok okna: Pri najstaršom údajovom bode v okne
  	]],
  	["sl"] = [[
Izračuna združene statistike v drsečem časovnem oknu za vsako podatkovno točko. Funkcija se od vsake točke pomika nazaj v času in združi vse vrednosti znotraj določenega obdobja okna.

Če je na primer okno dolgo 7 dni in je združevanje povprečje, vsaka izhodna točka predstavlja povprečje vseh vrednosti v 7 dneh pred to točko.

**Možnosti konfiguracije:**

- **Združevanje**: Operacija, ki se izvede nad vrednostmi v vsakem oknu:
  - Min: Najmanjša vrednost
  - Max: Največja vrednost
  - Povprečje: Srednja vrednost vseh vrednosti
  - Vsota: Skupna vrednost vseh vrednosti
  - Število: Število podatkovnih točk
  - Varianca: Statistična varianca
  - Standardni odklon: Statistični standardni odklon

- **Velikost okna**: Časovna enota za obdobje pogleda nazaj (sekunde, minute, ure, dnevi, tedni, meseci ali leta)

- **Množitelj**: Koliko enot velikosti okna pogledati nazaj (npr. množitelj 3 z velikostjo okna »dnevi« = 3-dnevno okno)

- **Postavitev**: Kam v časovno okno se postavi vsaka izhodna podatkovna točka:
  - Konec okna: Ob najnovejši podatkovni točki v oknu (privzeto)
  - Sredina okna: Na časovno sredino okna
  - Začetek okna: Ob najstarejši podatkovni točki v oknu
  	]],
  	["es"] = [[
Calcula estadísticas agregadas en una ventana temporal móvil para cada punto de datos. La función retrocede en el tiempo desde cada punto y agrega todos los valores dentro del período de ventana especificado.

Por ejemplo, con una ventana de 7 días y una agregación de promedio, cada punto de salida representa el promedio de todos los valores de los 7 días anteriores a ese punto.

**Opciones de configuración:**

- **Agregación**: La operación que se realizará sobre los valores de cada ventana:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación estándar: Desviación estándar estadística

- **Tamaño de ventana**: La unidad de tiempo del período retrospectivo (segundos, minutos, horas, días, semanas, meses o años)

- **Multiplicador**: Cuántas unidades del tamaño de ventana retroceder (por ejemplo, un multiplicador de 3 con un tamaño de ventana de «días» = ventana de 3 días)

- **Ubicación**: Dónde colocar cada punto de datos de salida en la ventana temporal:
  - Fin de la ventana: En el punto de datos más reciente de la ventana (predeterminado)
  - Punto medio de la ventana: En el centro temporal de la ventana
  - Inicio de la ventana: En el punto de datos más antiguo de la ventana
  	]],
  	["sw"] = [[
Hukokotoa takwimu zilizojumlishwa kwenye dirisha la muda linalosogea kwa kila nukta ya data. Function hutazama nyuma kwa wakati kutoka kila nukta na kujumlisha thamani zote ndani ya kipindi maalum cha dirisha.

Kwa mfano, kwa dirisha la siku 7 na ujumuishaji wa wastani, kila nukta ya matokeo huwakilisha wastani wa thamani zote katika siku 7 zinazoelekea kwenye nukta hiyo.

**Chaguo za Usanidi:**

- **Ujumlishaji**: Operesheni ya kufanya kwenye thamani za kila dirisha:
  - Min: Thamani ya chini kabisa
  - Max: Thamani ya juu kabisa
  - Average: Wastani wa thamani zote
  - Sum: Jumla ya thamani zote
  - Count: Idadi ya nukta za data
  - Variance: Tofauti ya kitakwimu
  - Standard Deviation: Mkengeuko wa kawaida wa kitakwimu

- **Ukubwa wa Dirisha**: Kipimo cha muda cha kipindi cha kuangalia nyuma (sekunde, dakika, saa, siku, wiki, miezi au miaka)

- **Kizidishi**: Idadi ya vipimo vya ukubwa wa dirisha vya kuangalia nyuma (kwa mfano, kizidishi cha 3 chenye ukubwa wa dirisha wa "siku" = dirisha la siku 3)

- **Mahali**: Mahali pa kuweka kila nukta ya data ya matokeo ndani ya dirisha la muda:
  - Mwisho wa Dirisha: Kwenye nukta ya data ya hivi karibuni katika dirisha (chaguo-msingi)
  - Katikati ya Dirisha: Katikati ya muda ya dirisha
  - Mwanzo wa Dirisha: Kwenye nukta ya data ya zamani zaidi katika dirisha
  	]],
  	["sv"] = [[
Beräknar sammanställd statistik över ett rörligt tidsfönster för varje datapunkt. Funktionen ser bakåt i tiden från varje punkt och sammanställer alla värden inom den angivna fönsterperioden.

Till exempel: med ett 7-dagarsfönster och genomsnittlig sammanställning representerar varje utgående punkt genomsnittet av alla värden under de 7 dagar som leder fram till den punkten.

**Konfigurationsalternativ:**

- **Sammanställning**: Åtgärden som utförs på värdena i varje fönster:
  - Min: Minimivärde
  - Max: Maximivärde
  - Genomsnitt: Medelvärdet av alla värden
  - Summa: Summan av alla värden
  - Antal: Antalet datapunkter
  - Varians: Statistisk varians
  - Standardavvikelse: Statistisk standardavvikelse

- **Fönsterstorlek**: Tidsenheten för bakåtblicksperioden (sekunder, minuter, timmar, dagar, veckor, månader eller år)

- **Multiplikator**: Hur många enheter av fönsterstorleken som ska inkluderas bakåt i tiden (t.ex. multiplikator 3 med fönsterstorleken "dagar" = 3-dagarsfönster)

- **Placering**: Var i tidsfönstret varje utgående datapunkt placeras:
  - Fönstrets slut: Vid den senaste datapunkten i fönstret (standard)
  - Fönstrets mittpunkt: Vid fönstrets tidsmässiga mittpunkt
  - Fönstrets början: Vid den äldsta datapunkten i fönstret
  	]],
  	["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளிக்கும் நகரும் நேரச் சாளரத்தில் தொகுக்கப்பட்ட புள்ளிவிவரங்களைக் கணக்கிடுகிறது. ஒவ்வொரு புள்ளியிலிருந்தும் காலத்தில் பின்னோக்கிப் பார்த்து, குறிப்பிட்ட சாளரக் காலப்பகுதிக்குள் உள்ள அனைத்து மதிப்புகளையும் இந்த Function தொகுக்கிறது.

எடுத்துக்காட்டாக, 7-நாள் சாளரமும் சராசரி தொகுத்தலும் பயன்படுத்தப்பட்டால், ஒவ்வொரு வெளியீட்டுப் புள்ளியும் அந்தப் புள்ளிக்கு முன் வரும் 7 நாட்களில் உள்ள அனைத்து மதிப்புகளின் சராசரியைப் பிரதிபலிக்கும்.

**உள்ளமைவு விருப்பங்கள்:**

- **தொகுத்தல்**: ஒவ்வொரு சாளரத்திலும் உள்ள மதிப்புகளில் செய்யப்படும் செயல்பாடு:
  - Min: குறைந்தபட்ச மதிப்பு
  - Max: அதிகபட்ச மதிப்பு
  - Average: அனைத்து மதிப்புகளின் சராசரி
  - Sum: அனைத்து மதிப்புகளின் மொத்தம்
  - Count: தரவுப் புள்ளிகளின் எண்ணிக்கை
  - Variance: புள்ளியியல் மாறுபாடு
  - Standard Deviation: புள்ளியியல் திட்ட விலகல்

- **சாளர அளவு**: பின்னோக்கிப் பார்க்க வேண்டிய காலப்பகுதியின் நேர அலகு (வினாடிகள், நிமிடங்கள், மணிநேரங்கள், நாட்கள், வாரங்கள், மாதங்கள் அல்லது ஆண்டுகள்)

- **பெருக்கி**: பின்னோக்கிப் பார்க்க வேண்டிய சாளர அளவு அலகுகளின் எண்ணிக்கை (எ.கா., சாளர அளவு "நாட்கள்" மற்றும் பெருக்கி 3 என்றால் = 3-நாள் சாளரம்)

- **நிலைநிறுத்தல்**: ஒவ்வொரு வெளியீட்டுத் தரவுப் புள்ளியையும் நேரச் சாளரத்தில் வைக்கும் இடம்:
  - Window End: சாளரத்தில் உள்ள சமீபத்திய தரவுப் புள்ளியில் (இயல்புநிலை)
  - Window Midpoint: சாளரத்தின் கால மையத்தில்
  - Window Start: சாளரத்தில் உள்ள பழைய தரவுப் புள்ளியில்
  	]],
  	["te"] = [[
ప్రతి డేటా పాయింట్‌కు కదిలే సమయ విండోలో సమగ్ర గణాంకాలను లెక్కిస్తుంది. ప్రతి పాయింట్ నుండి కాలంలో వెనక్కి చూసి, పేర్కొన్న విండో కాలవ్యవధిలోని అన్ని విలువలను సమగ్రపరుస్తుంది.

ఉదాహరణకు, 7 రోజుల విండో మరియు సగటు సమగ్రణతో, ప్రతి అవుట్‌పుట్ పాయింట్ ఆ పాయింట్‌కు ముందు ఉన్న 7 రోజుల్లోని అన్ని విలువల సగటును సూచిస్తుంది.

**కాన్ఫిగరేషన్ ఎంపికలు:**

- **సమగ్రణ**: ప్రతి విండోలోని విలువలపై నిర్వహించే ఆపరేషన్:
  - Min: కనిష్ఠ విలువ
  - Max: గరిష్ఠ విలువ
  - Average: అన్ని విలువల సగటు
  - Sum: అన్ని విలువల మొత్తం
  - Count: డేటా పాయింట్ల సంఖ్య
  - Variance: గణాంక వ్యత్యాసం
  - Standard Deviation: గణాంక ప్రామాణిక విచలనం

- **విండో పరిమాణం**: వెనక్కి చూసే కాలవ్యవధికి సమయ యూనిట్ (సెకన్లు, నిమిషాలు, గంటలు, రోజులు, వారాలు, నెలలు లేదా సంవత్సరాలు)

- **గుణకం**: వెనక్కి చూడాల్సిన విండో పరిమాణ యూనిట్ల సంఖ్య (ఉదా., విండో పరిమాణం "రోజులు", గుణకం 3 = 3 రోజుల విండో)

- **స్థానం**: ప్రతి అవుట్‌పుట్ డేటా పాయింట్‌ను సమయ విండోలో ఎక్కడ ఉంచాలి:
  - Window End: విండోలోని అత్యంత ఇటీవలి డేటా పాయింట్ వద్ద (డిఫాల్ట్)
  - Window Midpoint: విండో కాలకేంద్రంలో
  - Window Start: విండోలోని ప్రాచీన డేటా పాయింట్ వద్ద
  	]],
  	["th"] = [[
คำนวณสถิติรวมภายในหน้าต่างเวลาที่เลื่อนสำหรับจุดข้อมูลแต่ละจุด ฟังก์ชันจะมองย้อนกลับไปจากแต่ละจุดและรวมค่าทั้งหมดภายในช่วงเวลาหน้าต่างที่กำหนด

ตัวอย่างเช่น เมื่อใช้หน้าต่าง 7 วันและการรวมแบบค่าเฉลี่ย จุดผลลัพธ์แต่ละจุดจะแทนค่าเฉลี่ยของค่าทั้งหมดในช่วง 7 วันที่นำไปสู่จุดนั้น

**ตัวเลือกการกำหนดค่า:**

- **การรวมค่า**: การดำเนินการกับค่าในแต่ละหน้าต่าง:
  - Min: ค่าต่ำสุด
  - Max: ค่าสูงสุด
  - Average: ค่าเฉลี่ยของค่าทั้งหมด
  - Sum: ผลรวมของค่าทั้งหมด
  - Count: จำนวนจุดข้อมูล
  - Variance: ความแปรปรวนทางสถิติ
  - Standard Deviation: ส่วนเบี่ยงเบนมาตรฐานทางสถิติ

- **ขนาดหน้าต่าง**: หน่วยเวลาสำหรับช่วงเวลาย้อนดู (วินาที นาที ชั่วโมง วัน สัปดาห์ เดือน หรือปี)

- **ตัวคูณ**: จำนวนหน่วยขนาดหน้าต่างที่ย้อนดู (เช่น ตัวคูณ 3 กับขนาดหน้าต่าง "วัน" = หน้าต่าง 3 วัน)

- **ตำแหน่ง**: ตำแหน่งในหน้าต่างเวลาที่จะวางจุดข้อมูลผลลัพธ์แต่ละจุด:
  - สิ้นสุดช่วงเวลา: ที่จุดข้อมูลล่าสุดในหน้าต่าง (ค่าเริ่มต้น)
  - จุดกึ่งกลางช่วงเวลา: ที่กึ่งกลางตามเวลาของหน้าต่าง
  - เริ่มต้นช่วงเวลา: ที่จุดข้อมูลเก่าที่สุดในหน้าต่าง
  	]],
  	["tr"] = [[
Her veri noktası için hareketli bir zaman penceresi üzerindeki toplu istatistikleri hesaplar. İşlev, her noktadan geriye doğru bakar ve belirtilen pencere dönemi içindeki tüm değerleri toplar.

Örneğin 7 günlük pencere ve ortalama toplama ile her çıktı noktası, o noktaya kadar olan 7 gündeki tüm değerlerin ortalamasını temsil eder.

**Yapılandırma Seçenekleri:**

- **Toplama**: Her penceredeki değerlere uygulanacak işlem:
  - Min: Minimum değer
  - Max: Maksimum değer
  - Average: Tüm değerlerin ortalaması
  - Sum: Tüm değerlerin toplamı
  - Count: Veri noktası sayısı
  - Variance: İstatistiksel varyans
  - Standard Deviation: İstatistiksel standart sapma

- **Pencere Boyutu**: Geriye bakma dönemi için zaman birimi (saniye, dakika, saat, gün, hafta, ay veya yıl)

- **Çarpan**: Geriye bakılacak pencere boyutu birimlerinin sayısı (ör. pencere boyutu "gün" ve çarpan 3 ise = 3 günlük pencere)

- **Yerleştirme**: Her çıktı veri noktasının zaman penceresinde yerleştirileceği konum:
  - Pencere Sonu: Penceredeki en yeni veri noktasında (varsayılan)
  - Pencere Ortası: Pencerenin zamansal merkezinde
  - Pencere Başlangıcı: Penceredeki en eski veri noktasında
  	]],
  	["uk"] = [[
Обчислює агреговану статистику в ковзному часовому вікні для кожної точки даних. Функція рухається назад у часі від кожної точки й агрегує всі значення в межах вказаного періоду вікна.

Наприклад, за 7-денного вікна й агрегації середнього кожна вихідна точка представляє середнє всіх значень за 7 днів до цієї точки.

**Параметри конфігурації:**

- **Агрегація**: Операція, яку потрібно виконати над значеннями в кожному вікні:
  - Мінімум: Мінімальне значення
  - Максимум: Максимальне значення
  - Середнє: Середнє всіх значень
  - Сума: Сума всіх значень
  - Кількість: Кількість точок даних
  - Дисперсія: Статистична дисперсія
  - Стандартне відхилення: Статистичне стандартне відхилення

- **Розмір вікна**: Одиниця часу для періоду перегляду назад (секунди, хвилини, години, дні, тижні, місяці або роки)

- **Множник**: Кількість одиниць розміру вікна для перегляду назад (наприклад, множник 3 з розміром вікна «дні» = 3-денне вікно)

- **Розміщення**: Де в часовому вікні розміщувати кожну вихідну точку даних:
  - Кінець вікна: Біля найновішої точки даних у вікні (типово)
  - Середина вікна: У часовому центрі вікна
  - Початок вікна: Біля найстарішої точки даних у вікні
  	]],
  	["vi"] = [[
Tính các thống kê tổng hợp trong một cửa sổ thời gian di động cho mỗi điểm dữ liệu. Hàm xem ngược về quá khứ từ mỗi điểm và tổng hợp tất cả giá trị trong khoảng thời gian cửa sổ đã chỉ định.

Ví dụ: với cửa sổ 7 ngày và phép tổng hợp trung bình, mỗi điểm đầu ra đại diện cho giá trị trung bình của tất cả giá trị trong 7 ngày trước điểm đó.

**Tùy chọn cấu hình:**

- **Tổng hợp**: Phép toán thực hiện trên các giá trị trong mỗi cửa sổ:
  - Min: Giá trị nhỏ nhất
  - Max: Giá trị lớn nhất
  - Average: Trung bình của tất cả giá trị
  - Sum: Tổng của tất cả giá trị
  - Count: Số lượng điểm dữ liệu
  - Variance: Phương sai thống kê
  - Standard Deviation: Độ lệch chuẩn thống kê

- **Kích thước cửa sổ**: Đơn vị thời gian của khoảng thời gian xem ngược (giây, phút, giờ, ngày, tuần, tháng hoặc năm)

- **Hệ số**: Số đơn vị kích thước cửa sổ cần xem ngược (ví dụ: hệ số 3 với kích thước cửa sổ "ngày" = cửa sổ 3 ngày)

- **Vị trí**: Vị trí đặt mỗi điểm dữ liệu đầu ra trong cửa sổ thời gian:
  - Cuối cửa sổ: Tại điểm dữ liệu gần đây nhất trong cửa sổ (mặc định)
  - Trung điểm cửa sổ: Tại trung tâm thời gian của cửa sổ
  - Đầu cửa sổ: Tại điểm dữ liệu cũ nhất trong cửa sổ
  	]],
  },
  config = {
    enum {
      id = "aggregation_type",
      name = "_aggregation",
      options = { "_min", "_max", "_average", "_sum", "_count", "_variance", "_standard_deviation" },
      default = "_average"
    },
    uint {
      id = "multiplier",
      name = "_multiplier",
      default = 1
    },
    enum {
      id = "window",
      name = "_window_size",
      options = { "_seconds", "_minutes", "_hours", "_days", "_weeks", "_months", "_years", },
      default = "_weeks"
    },
    enum {
      id = "placement",
      name = "_placement",
      options = { "_window_end", "_window_midpoint", "_window_start" },
      default = "_window_end"
    },
  },
  generator = function(source, config)
    local aggregator = get_aggregator(config)
    local window = get_window(config)
    local multiplier = config.multiplier
    local carry = nil

    return function()
      if #aggregator.window > 0 then
        aggregator:pop()
      end

      if #aggregator.window == 0 then
        if carry ~= nil then
          aggregator:push(carry)
          carry = nil
        else
          local next_dp = source.dp()
          if next_dp == nil then
            return nil
          end
          aggregator:push(next_dp)
        end
      end

      local last_dp = aggregator.window[1]
      if last_dp == nil then
        return nil
      end

      local new_window_start = core.shift(last_dp, window, -multiplier)

      while true do
        if carry ~= nil then
          if carry.timestamp >= new_window_start.timestamp then
            aggregator:push(carry)
            carry = nil
          else
            break
          end
        end

        local next_dp = source.dp()

        if next_dp == nil then
          break
        elseif next_dp.timestamp >= new_window_start.timestamp then
          aggregator:push(next_dp)
        else
          carry = next_dp
          break
        end
      end

      return aggregator:run()
    end
  end
}
