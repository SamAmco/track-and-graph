local core = require("tng.core")
local enum = require("tng.config").enum

local PLACEMENT_MAP = {
  _window_start = "start",
  _window_midpoint = "mid",
  _window_end = "end",
}

local get_aggregator_factory = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local placement = PLACEMENT_MAP[config.placement] or "end"
  local aggregator

  if type == "_min" then
    aggregator = function() return aggregation.simple_min_aggregator2(placement) end
  elseif type == "_max" then
    aggregator = function() return aggregation.simple_max_aggregator2(placement) end
  elseif type == "_average" then
    aggregator = function() return aggregation.avg_aggregator2(placement) end
  elseif type == "_sum" then
    aggregator = function() return aggregation.sum_aggregator2(placement) end
  elseif type == "_variance" then
    aggregator = function() return aggregation.variance_aggregator2(placement) end
  elseif type == "_standard_deviation" then
    aggregator = function() return aggregation.stdev_aggregator2(placement) end
  elseif type == "_count" then
    aggregator = function() return aggregation.count_aggregator2(placement) end
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_period = function(config)
  if type(config.period) ~= "string" then
    error("config.period is not a string")
  end

  if config.period == "_days" then
    return core.PERIOD.DAY
  elseif config.period == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.period == "_months" then
    return core.PERIOD.MONTH
  elseif config.period == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown period: " .. tostring(config.period))
  end
end

local get_placement_time = function(config, window_start, window_end)
  local placement = config.placement or "_window_end"

  if placement == "_window_start" then
    return window_start
  elseif placement == "_window_midpoint" then
    local half_window = math.floor((window_end.timestamp - window_start.timestamp) / 2)
    return core.shift(window_start, half_window)
  elseif placement == "_window_end" then
    return core.shift(window_end, -1)
  else
    error("Unknown placement: " .. tostring(placement))
  end
end

return {
  id = "periodic-aggregation-v4",
  version = "4.0.4",
  inputCount = 1,
  title = {
  	["en"] = "Periodic Aggregation",
  	["af"] = "Periodieke Aggregasie",
  	["sq"] = "Grumbullim periodik",
  	["am"] = "ወቅታዊ ማጠቃለያ",
  	["hy"] = "Պարբերական միավորում",
  	["az"] = "Dövri birləşdirmə",
  	["bn"] = "পর্যায়ক্রমিক একত্রীকরণ",
  	["eu"] = "Aldizkako agregazioa",
  	["be"] = "Перыядычная агрэгацыя",
  	["bg"] = "Периодично обобщаване",
  	["my"] = "ကာလအလိုက် စုစည်းခြင်း",
  	["ca"] = "Agregació periòdica",
  	["zh-Hans"] = "周期性聚合",
  	["zh-Hant"] = "週期性彙總",
  	["hr"] = "Periodična agregacija",
  	["cs"] = "Periodická agregace",
  	["da"] = "Periodisk sammenlægning",
  	["nl"] = "Periodieke aggregatie",
  	["et"] = "Perioodiline koondamine",
  	["fil"] = "Pana-panahong Pagsasama",
  	["fi"] = "Jaksottainen yhdistäminen",
  	["fr"] = "Agrégation périodique",
  	["gl"] = "Agregación periódica",
  	["ka"] = "პერიოდული აგრეგაცია",
  	["de"] = "Periodische Aggregation",
  	["el"] = "Περιοδική συνάθροιση",
  	["gu"] = "આવર્તક એકત્રીકરણ",
  	["hi"] = "आवधिक समेकन",
  	["hu"] = "Időszakos összesítés",
  	["is"] = "Lotubundin söfnun",
  	["id"] = "Agregasi Berkala",
  	["it"] = "Aggregazione periodica",
  	["ja"] = "周期的集約",
  	["kn"] = "ಆವರ್ತಕ ಒಟ್ಟುಗೂಡಿಸುವಿಕೆ",
  	["kk"] = "Мерзімдік біріктіру",
  	["km"] = "ការប្រមូលផ្តុំតាមកាលកំណត់",
  	["ko"] = "주기적 집계",
  	["ky"] = "Мезгилдик бириктирүү",
  	["lo"] = "ການລວມຂໍ້ມູນແບບເປັນໄລຍະ",
  	["lv"] = "Periodiska apkopošana",
  	["lt"] = "Periodinis apibendrinimas",
  	["mk"] = "Периодична агрегација",
  	["ms"] = "Pengagregatan Berkala",
  	["ml"] = "ആവർത്തനകാല അഗ്രിഗേഷൻ",
  	["mr"] = "नियतकालिक एकत्रीकरण",
  	["mn"] = "Үечилсэн нэгтгэл",
  	["ne"] = "आवधिक समेकन",
  	["no"] = "Periodisk aggregering",
  	["pl"] = "Agregacja okresowa",
  	["pt"] = "Agregação periódica",
  	["pa"] = "ਆਵਧਿਕ ਇਕੱਠ",
  	["ro"] = "Agregare periodică",
  	["rm"] = "Agregaziun periodica",
  	["ru"] = "Периодическая агрегация",
  	["sr"] = "Periodično objedinjavanje",
  	["si"] = "කාලාන්තර එකතු කිරීම",
  	["sk"] = "Periodická agregácia",
  	["sl"] = "Periodično združevanje",
  	["es"] = "Agregación periódica",
  	["sw"] = "Ujumlishaji wa Vipindi",
  	["sv"] = "Periodisk sammanställning",
  	["ta"] = "காலமுறைத் தொகுத்தல்",
  	["te"] = "కాలానుగత సమగ్రణ",
  	["th"] = "การรวมค่าตามช่วงเวลา",
  	["tr"] = "Dönemsel Toplama",
  	["uk"] = "Періодична агрегація",
  	["vi"] = "Tổng hợp theo chu kỳ",
  },
  categories = { "_aggregation" },
  description = {
  	["en"] = [[
Aggregates data points into fixed calendar periods (days, weeks, months, or years). Each period produces one aggregated value containing all data points that fall within that period's boundaries.

For example, with a daily period and average aggregation, all measurements from each calendar day are combined into a single value representing the average for that day.

**Configuration Options:**

- **Period**: The calendar period to aggregate by (days, weeks, months, or years)

- **Aggregation**: The operation to perform on values in each period:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Placement**: Where in the time window to place each output data point:
  - Window End: At the end of the calendar period (default)
  - Window Midpoint: At the temporal center of the calendar period
  - Window Start: At the start of the calendar period
  	]],
  	["af"] = [[
Kombineer datapunte in vaste kalenderperiodes (dae, weke, maande of jare). Elke periode lewer een saamgevoegde waarde wat alle datapunte bevat wat binne die grense van daardie periode val.

Byvoorbeeld, met ’n daaglikse periode en gemiddelde aggregasie word alle metings van elke kalenderdag gekombineer in een waarde wat die gemiddelde vir daardie dag verteenwoordig.

**Konfigurasie-opsies:**

- **Periode**: Die kalenderperiode waarvolgens geaggregeer word (dae, weke, maande of jare)

- **Aggregasie**: Die bewerking wat op waardes in elke periode uitgevoer word:
  - Min: Minimumwaarde
  - Maks: Maksimumwaarde
  - Gemiddeld: Gemiddelde van alle waardes
  - Som: Totaal van alle waardes
  - Tel: Aantal datapunte
  - Variansie: Statistiese variansie
  - Standaardafwyking: Statistiese standaardafwyking

- **Plasing**: Waar in die tydvenster elke uitvoerdatapunt geplaas word:
  - Venster-einde: Aan die einde van die kalenderperiode (verstek)
  - Venster-middelpunt: By die tydelike middelpunt van die kalenderperiode
  - Venster-begin: Aan die begin van die kalenderperiode
  	]],
  	["sq"] = [[
Grumbullon pikat e të dhënave në periudha fikse kalendarike (ditë, javë, muaj ose vite). Çdo periudhë prodhon një vlerë të vetme të grumbulluar që përmban të gjitha pikat e të dhënave brenda kufijve të asaj periudhe.

Për shembull, me një periudhë ditore dhe grumbullim mesatar, të gjitha matjet nga çdo ditë kalendarike kombinohen në një vlerë të vetme që përfaqëson mesataren e asaj dite.

**Opsionet e konfigurimit:**

- **Periudha**: Periudha kalendarike sipas së cilës grumbullohen të dhënat (ditë, javë, muaj ose vite)

- **Grumbullimi**: Veprimi që kryhet mbi vlerat në çdo periudhë:
  - Min: Vlera minimale
  - Max: Vlera maksimale
  - Mesatarja: Mesatarja e të gjitha vlerave
  - Shuma: Totali i të gjitha vlerave
  - Numërimi: Numri i pikave të të dhënave
  - Varianca: Varianca statistikore
  - Devijimi standard: Devijimi standard statistikor

- **Pozicionimi**: Ku të vendoset çdo pikë dalëse e të dhënave në dritaren kohore:
  - Fundi i dritares: Në fund të periudhës kalendarike (parazgjedhje)
  - Mesii i dritares: Në qendrën kohore të periudhës kalendarike
  - Fillimi i dritares: Në fillim të periudhës kalendarike
  	]],
  	["am"] = [[
የውሂብ ነጥቦችን ወደ ቋሚ የቀን መቁጠሪያ ወቅቶች (ቀናት፣ ሳምንታት፣ ወራት ወይም ዓመታት) ያጠቃልላል። እያንዳንዱ ወቅት በወቅቱ ወሰኖች ውስጥ የሚወድቁ ሁሉንም የውሂብ ነጥቦች የያዘ አንድ የተጠቃለለ እሴት ያመነጫል።

ለምሳሌ፣ ዕለታዊ ወቅትና አማካይ ማጠቃለያ ሲመረጥ፣ ከእያንዳንዱ የቀን መቁጠሪያ ቀን የተወሰዱ ሁሉም ልኬቶች የዚያን ቀን አማካይ የሚወክል አንድ እሴት ሆነው ይጣመራሉ።

**የውቅር አማራጮች፦**

- **ወቅት**፦ ማጠቃለያው የሚደረግበት የቀን መቁጠሪያ ወቅት (ቀናት፣ ሳምንታት፣ ወራት ወይም ዓመታት)

- **ማጠቃለያ**፦ በእያንዳንዱ ወቅት ባሉ እሴቶች ላይ የሚከናወነው ክዋኔ፦
  - Min፦ ዝቅተኛ እሴት
  - Max፦ ከፍተኛ እሴት
  - Average፦ የሁሉም እሴቶች አማካይ
  - Sum፦ የሁሉም እሴቶች ድምር
  - Count፦ የውሂብ ነጥቦች ብዛት
  - Variance፦ ስታቲስቲካዊ ልዩነት
  - Standard Deviation፦ ስታቲስቲካዊ መደበኛ ልዩነት

- **አቀማመጥ**፦ እያንዳንዱ የውጤት የውሂብ ነጥብ በጊዜ መስኮቱ ውስጥ የሚቀመጥበት ቦታ፦
  - Window End፦ በቀን መቁጠሪያ ወቅቱ መጨረሻ (ነባሪ)
  - Window Midpoint፦ በቀን መቁጠሪያ ወቅቱ የጊዜ መሃል
  - Window Start፦ በቀን መቁጠሪያ ወቅቱ መጀመሪያ
  	]],
  	["hy"] = [[
Տվյալակետերը միավորում է օրացուցային հաստատուն ժամանակահատվածներում (օրեր, շաբաթներ, ամիսներ կամ տարիներ)։ Յուրաքանչյուր ժամանակահատված ստեղծում է մեկ միավորված արժեք՝ ներառելով դրա սահմաններում ընկնող բոլոր տվյալակետերը։

Օրինակ՝ օրական ժամանակահատվածի և միջին միավորման դեպքում յուրաքանչյուր օրացուցային օրվա բոլոր չափումները միավորվում են մեկ արժեքի մեջ, որը ներկայացնում է այդ օրվա միջինը։

**Կազմաձևման ընտրանքներ․**

- **Ժամանակահատված**․ օրացուցային ժամանակահատվածը, ըստ որի կատարվում է միավորումը (օրեր, շաբաթներ, ամիսներ կամ տարիներ)

- **Միավորում**․ յուրաքանչյուր ժամանակահատվածի արժեքների նկատմամբ կատարվող գործողությունը՝
  - Min՝ նվազագույն արժեք
  - Max՝ առավելագույն արժեք
  - Average՝ բոլոր արժեքների միջին
  - Sum՝ բոլոր արժեքների գումար
  - Count՝ տվյալակետերի քանակ
  - Variance՝ վիճակագրական դիսպերսիա
  - Standard Deviation՝ վիճակագրական ստանդարտ շեղում

- **Տեղադրում**․ ժամանակային պատուհանի որ հատվածում տեղադրել յուրաքանչյուր ելքային տվյալակետը՝
  - Window End՝ օրացուցային ժամանակահատվածի վերջում (կանխադրված)
  - Window Midpoint՝ օրացուցային ժամանակահատվածի ժամանակային կենտրոնում
  - Window Start՝ օրացուցային ժամանակահատվածի սկզբում
  	]],
  	["az"] = [[
Məlumat nöqtələrini sabit təqvim dövrlərində (günlər, həftələr, aylar və ya illər) birləşdirir. Hər dövr həmin dövrün sərhədləri daxilində olan bütün məlumat nöqtələrini ehtiva edən bir birləşdirilmiş qiymət yaradır.

Məsələn, gündəlik dövr və orta qiymət birləşdirməsi ilə hər təqvim gününün bütün ölçmələri həmin günün ortalamasını göstərən bir qiymətdə birləşdirilir.

**Konfiqurasiya seçimləri:**

- **Dövr**: Birləşdirmə üçün təqvim dövrü (günlər, həftələr, aylar və ya illər)

- **Birləşdirmə**: Hər dövrdəki qiymətlər üzərində icra ediləcək əməliyyat:
  - Min: Minimum qiymət
  - Max: Maksimum qiymət
  - Average: Bütün qiymətlərin ortalaması
  - Sum: Bütün qiymətlərin cəmi
  - Count: Məlumat nöqtələrinin sayı
  - Variance: Statistik dispersiya
  - Standard Deviation: Statistik standart kənarlaşma

- **Yerləşdirmə**: Hər çıxış məlumat nöqtəsinin zaman pəncərəsində yerləşdiriləcəyi yer:
  - Window End: Təqvim dövrünün sonunda (standart)
  - Window Midpoint: Təqvim dövrünün zaman mərkəzində
  - Window Start: Təqvim dövrünün əvvəlində
  	]],
  	["bn"] = [[
ডেটা পয়েন্টগুলোকে নির্দিষ্ট ক্যালেন্ডার সময়সীমায় (দিন, সপ্তাহ, মাস বা বছর) একত্রিত করে। প্রতিটি সময়সীমা তার সীমানার মধ্যে পড়া সব ডেটা পয়েন্টের একটি একত্রীকৃত মান তৈরি করে।

উদাহরণস্বরূপ, দৈনিক সময়সীমা ও গড় একত্রীকরণ হলে, প্রতিটি ক্যালেন্ডার দিনের সব পরিমাপ একত্রিত হয়ে ওই দিনের গড় নির্দেশকারী একটি মান তৈরি করে।

**কনফিগারেশন অপশন:**

- **সময়সীমা**: যে ক্যালেন্ডার সময়সীমা অনুযায়ী একত্রীকরণ করা হবে (দিন, সপ্তাহ, মাস বা বছর)

- **একত্রীকরণ**: প্রতিটি সময়সীমার মানের ওপর সম্পাদিত অপারেশন:
  - Min: সর্বনিম্ন মান
  - Max: সর্বোচ্চ মান
  - Average: সব মানের গড়
  - Sum: সব মানের মোট
  - Count: ডেটা পয়েন্টের সংখ্যা
  - Variance: পরিসংখ্যানগত বিচরণ
  - Standard Deviation: পরিসংখ্যানগত মান বিচ্যুতি

- **অবস্থান**: সময়ের উইন্ডোর কোথায় প্রতিটি আউটপুট ডেটা পয়েন্ট রাখা হবে:
  - Window End: ক্যালেন্ডার সময়সীমার শেষে (ডিফল্ট)
  - Window Midpoint: ক্যালেন্ডার সময়সীমার সময়গত কেন্দ্রে
  - Window Start: ক্যালেন্ডার সময়সীমার শুরুতে
  	]],
  	["eu"] = [[
Datu-puntuak egutegiko aldi finkoetan (egunak, asteak, hilabeteak edo urteak) agregatzen ditu. Aldi bakoitzak aldi horren mugen barruan dauden datu-puntu guztiak dituen balio agregatu bat sortzen du.

Adibidez, eguneroko aldi batekin eta batez besteko agregazioarekin, egutegiko egun bakoitzeko neurketa guztiak balio bakar batean konbinatzen dira, egun horretako batez bestekoa adierazteko.

**Konfigurazio-aukerak:**

- **Aldia**: Agregatzeko egutegi-aldia (egunak, asteak, hilabeteak edo urteak)

- **Agregazioa**: Aldi bakoitzeko balioekin egin beharreko eragiketa:
  - Min: Gutxieneko balioa
  - Max: Gehieneko balioa
  - Batez bestekoa: Balio guztien batez bestekoa
  - Batura: Balio guztien batura
  - Zenbaketa: Datu-puntuen kopurua
  - Bariantza: Bariantza estatistikoa
  - Desbideratze estandarra: Desbideratze estandarra estatistikoa

- **Kokapena**: Irteerako datu-puntu bakoitza denbora-leihoan non kokatu:
  - Leihoaren amaiera: Egutegi-aldiaren amaieran (lehenetsia)
  - Leihoaren erdiko puntua: Egutegi-aldiaren erdigune kronologikoan
  - Leihoaren hasiera: Egutegi-aldiaren hasieran
  	]],
  	["be"] = [[
Аб’ядноўвае кропкі даных у фіксаваныя каляндарныя перыяды (дні, тыдні, месяцы або гады). Кожны перыяд стварае адно аб’яднанае значэнне, якое змяшчае ўсе кропкі даных у межах гэтага перыяду.

Напрыклад, пры сутачным перыядзе і агрэгацыі сярэдняга ўсе вымярэнні за кожны каляндарны дзень аб’ядноўваюцца ў адно значэнне, якое паказвае сярэдняе за гэты дзень.

**Параметры канфігурацыі:**

- **Перыяд**: каляндарны перыяд для агрэгацыі (дні, тыдні, месяцы або гады)

- **Агрэгацыя**: аперацыя над значэннямі ў кожным перыядзе:
  - Мін.: мінімальнае значэнне
  - Макс.: максімальнае значэнне
  - Сярэдняе: сярэдняе ўсіх значэнняў
  - Сума: сума ўсіх значэнняў
  - Колькасць: колькасць кропак даных
  - Дысперсія: статыстычная дысперсія
  - Стандартнае адхіленне: статыстычнае стандартнае адхіленне

- **Размяшчэнне**: дзе ў часовым акне размясціць кожную выходную кропку даных:
  - Канец акна: у канцы каляндарнага перыяду (па змаўчанні)
  - Сярэдзіна акна: у часовым цэнтры каляндарнага перыяду
  - Пачатак акна: у пачатку каляндарнага перыяду
  	]],
  	["bg"] = [[
Обобщава точките от данни във фиксирани календарни периоди (дни, седмици, месеци или години). Всеки период генерира една обобщена стойност, съдържаща всички точки от данни, които попадат в границите му.

Например при дневен период и обобщаване чрез средна стойност всички измервания от всеки календарен ден се комбинират в една стойност, представяща средната стойност за този ден.

**Опции за конфигуриране:**

- **Период**: Календарният период, по който да се извършва обобщаването (дни, седмици, месеци или години)

- **Обобщаване**: Операцията, която се извършва върху стойностите във всеки период:
  - Min: Минимална стойност
  - Max: Максимална стойност
  - Average: Средна стойност на всички стойности
  - Sum: Сбор на всички стойности
  - Count: Брой точки от данни
  - Variance: Статистическа дисперсия
  - Standard Deviation: Статистическо стандартно отклонение

- **Позициониране**: Къде във времевия прозорец да бъде поставена всяка изходна точка от данни:
  - Край на прозореца: В края на календарния период (по подразбиране)
  - Среда на прозореца: Във времевия център на календарния период
  - Начало на прозореца: В началото на календарния период
  	]],
  	["my"] = [[
ဒေတာအမှတ်များကို သတ်မှတ်ထားသော ပြက္ခဒိန်ကာလများ (နေ့၊ အပတ်၊ လ သို့မဟုတ် နှစ်) အတွင်း စုစည်းသည်။ ကာလတစ်ခုစီအတွက် ထိုကာလ၏ နယ်နိမိတ်အတွင်း ကျရောက်သော ဒေတာအမှတ်အားလုံးပါဝင်သည့် စုစည်းတန်ဖိုးတစ်ခု ထုတ်ပေးသည်။

ဥပမာအားဖြင့်၊ နေ့စဉ်ကာလနှင့် ပျမ်းမျှစုစည်းမှုကို အသုံးပြုပါက ပြက္ခဒိန်နေ့တစ်နေ့စီမှ တိုင်းတာချက်အားလုံးကို ထိုနေ့၏ ပျမ်းမျှကို ကိုယ်စားပြုသည့် တန်ဖိုးတစ်ခုအဖြစ် ပေါင်းစည်းသည်။

**ပြင်ဆင်သတ်မှတ်မှု ရွေးချယ်စရာများ:**

- **ကာလ**: စုစည်းမည့် ပြက္ခဒိန်ကာလ (နေ့၊ အပတ်၊ လ သို့မဟုတ် နှစ်)

- **စုစည်းမှု**: ကာလတစ်ခုစီရှိ တန်ဖိုးများအပေါ် လုပ်ဆောင်မည့် လုပ်ဆောင်ချက်:
  - Min: အနည်းဆုံးတန်ဖိုး
  - Max: အများဆုံးတန်ဖိုး
  - Average: တန်ဖိုးအားလုံး၏ ပျမ်းမျှ
  - Sum: တန်ဖိုးအားလုံး၏ စုစုပေါင်း
  - Count: ဒေတာအမှတ်အရေအတွက်
  - Variance: စာရင်းအင်းကွဲပြားမှု
  - Standard Deviation: စာရင်းအင်းစံသွေဖည်မှု

- **နေရာချထားမှု**: ထုတ်ပေးမည့် ဒေတာအမှတ်တစ်ခုစီကို အချိန်ဝင်းဒိုးအတွင်း မည်သည့်နေရာတွင် ထားမည်နည်း:
  - Window End: ပြက္ခဒိန်ကာလ၏ အဆုံးတွင် (မူလ)
  - Window Midpoint: ပြက္ခဒိန်ကာလ၏ အချိန်အလယ်ဗဟိုတွင်
  - Window Start: ပြက္ခဒိန်ကာလ၏ အစတွင်
  	]],
  	["ca"] = [[
Agrega els punts de dades en períodes de calendari fixos (dies, setmanes, mesos o anys). Cada període produeix un valor agregat que conté tots els punts de dades que es troben dins dels límits d’aquest període.

Per exemple, amb un període diari i una agregació mitjana, totes les mesures de cada dia natural es combinen en un únic valor que representa la mitjana d’aquell dia.

**Opcions de configuració:**

- **Període**: El període de calendari pel qual cal agregar (dies, setmanes, mesos o anys)

- **Agregació**: L’operació que cal fer amb els valors de cada període:
  - Mínim: Valor mínim
  - Màxim: Valor màxim
  - Mitjana: Mitjana de tots els valors
  - Suma: Total de tots els valors
  - Recompte: Nombre de punts de dades
  - Variància: Variància estadística
  - Desviació estàndard: Desviació estàndard estadística

- **Posicionament**: On col·locar cada punt de dades de sortida dins de la finestra de temps:
  - Final de la finestra: Al final del període de calendari (per defecte)
  - Punt mitjà de la finestra: Al centre temporal del període de calendari
  - Inici de la finestra: A l’inici del període de calendari
  	]],
  	["zh-Hans"] = [[
将数据点聚合到固定的日历周期（日、周、月或年）中。每个周期会生成一个聚合值，其中包含落在该周期边界内的所有数据点。

例如，使用按日周期和平均值聚合时，每个日历日的所有测量值会合并为一个代表该日平均值的单一值。

**配置选项：**

- **周期**：用于聚合的日历周期（日、周、月或年）

- **聚合**：对每个周期中的值执行的操作：
  - Min：最小值
  - Max：最大值
  - Average：所有值的平均值
  - Sum：所有值的总和
  - Count：数据点数量
  - Variance：统计方差
  - Standard Deviation：统计标准差

- **放置位置**：每个输出数据点在时间窗口中的放置位置：
  - 窗口结束：位于日历周期结束处（默认）
  - 窗口中点：位于日历周期的时间中心
  - 窗口开始：位于日历周期开始处
  	]],
  	["zh-Hant"] = [[
將資料點彙總至固定的曆法期間（日、週、月或年）。每個期間會產生一個彙總值，包含落在該期間界線內的所有資料點。

例如，若期間為每日且彙總方式為平均值，則每個曆法日的所有測量值會合併為代表當日平均值的單一值。

**設定選項：**

- **期間**：要依據彙總的曆法期間（日、週、月或年）

- **彙總**：對每個期間中的值執行的操作：
  - 最小值：最小值
  - 最大值：最大值
  - 平均值：所有值的平均值
  - 總和：所有值的總和
  - 計數：資料點數量
  - 變異數：統計變異數
  - 標準差：統計標準差

- **放置位置**：每個輸出資料點在時間視窗中的放置位置：
  - 視窗結束：位於曆法期間結束時（預設）
  - 視窗中點：位於曆法期間的時間中心
  - 視窗開始：位於曆法期間開始時
  	]],
  	["hr"] = [[
Agregira podatkovne točke u fiksna kalendarska razdoblja (dane, tjedne, mjesece ili godine). Svako razdoblje daje jednu agregiranu vrijednost koja sadrži sve podatkovne točke unutar granica tog razdoblja.

Na primjer, s dnevnim razdobljem i agregacijom prosjeka, sva mjerenja iz svakog kalendarskog dana kombiniraju se u jednu vrijednost koja predstavlja prosjek za taj dan.

**Mogućnosti konfiguracije:**

- **Razdoblje**: Kalendarsko razdoblje prema kojem se agregira (dani, tjedni, mjeseci ili godine)

- **Agregacija**: Operacija koja se izvodi nad vrijednostima u svakom razdoblju:
  - Min: Minimalna vrijednost
  - Max: Maksimalna vrijednost
  - Prosjek: Srednja vrijednost svih vrijednosti
  - Zbroj: Zbroj svih vrijednosti
  - Broj: Broj podatkovnih točaka
  - Varijanca: Statistička varijanca
  - Standardna devijacija: Statistička standardna devijacija

- **Smještaj**: Gdje u vremenski prozor smjestiti svaku izlaznu podatkovnu točku:
  - Kraj prozora: Na kraju kalendarskog razdoblja (zadano)
  - Sredina prozora: U vremensko središte kalendarskog razdoblja
  - Početak prozora: Na početku kalendarskog razdoblja
  	]],
  	["cs"] = [[
Agreguje datové body do pevných kalendářních období (dnů, týdnů, měsíců nebo roků). Každé období vytvoří jednu agregovanou hodnotu obsahující všechny datové body, které spadají do jeho hranic.

Například při denním období a průměrné agregaci se všechna měření z každého kalendářního dne spojí do jedné hodnoty představující průměr za daný den.

**Možnosti konfigurace:**

- **Období**: Kalendářní období, podle kterého se má agregovat (dny, týdny, měsíce nebo roky)

- **Agregace**: Operace prováděná s hodnotami v každém období:
  - Minimum: Nejnižší hodnota
  - Maximum: Nejvyšší hodnota
  - Průměr: Průměr všech hodnot
  - Součet: Součet všech hodnot
  - Počet: Počet datových bodů
  - Rozptyl: Statistický rozptyl
  - Směrodatná odchylka: Statistická směrodatná odchylka

- **Umístění**: Kam v časovém okně umístit každý výstupní datový bod:
  - Konec okna: Na konec kalendářního období (výchozí)
  - Střed okna: Do časového středu kalendářního období
  - Začátek okna: Na začátek kalendářního období
  	]],
  	["da"] = [[
Sammenlægger datapunkter i faste kalenderperioder (dage, uger, måneder eller år). Hver periode producerer én sammenlagt værdi, der indeholder alle datapunkter, som falder inden for periodens grænser.

For eksempel: Med en daglig periode og gennemsnitlig sammenlægning kombineres alle målinger fra hver kalenderdag til én værdi, der repræsenterer gennemsnittet for den dag.

**Konfigurationsindstillinger:**

- **Periode**: Den kalenderperiode, der skal sammenlægges efter (dage, uger, måneder eller år)

- **Sammenlægning**: Handlingen, der udføres på værdierne i hver periode:
  - Min: Minimumsværdi
  - Max: Maksimumsværdi
  - Gennemsnit: Gennemsnittet af alle værdier
  - Sum: Summen af alle værdier
  - Antal: Antallet af datapunkter
  - Varians: Statistisk varians
  - Standardafvigelse: Statistisk standardafvigelse

- **Placering**: Hvor i tidsvinduet hvert outputdatapunkt placeres:
  - Vinduets slutning: Ved kalenderperiodens slutning (standard)
  - Vinduets midtpunkt: Ved kalenderperiodens tidsmæssige centrum
  - Vinduets begyndelse: Ved kalenderperiodens begyndelse
  	]],
  	["nl"] = [[
Aggregeert datapunten in vaste kalenderperioden (dagen, weken, maanden of jaren). Elke periode levert één geaggregeerde waarde op met alle datapunten die binnen de grenzen van die periode vallen.

Bijvoorbeeld: bij een dagelijkse periode en gemiddelde aggregatie worden alle metingen van elke kalenderdag gecombineerd tot één waarde die het gemiddelde van die dag weergeeft.

**Configuratieopties:**

- **Periode**: De kalenderperiode waarop moet worden geaggregeerd (dagen, weken, maanden of jaren)

- **Aggregatie**: De bewerking die moet worden uitgevoerd op waarden in elke periode:
  - Min: Minimumwaarde
  - Max: Maximumwaarde
  - Gemiddelde: Gemiddelde van alle waarden
  - Som: Totaal van alle waarden
  - Aantal: Aantal datapunten
  - Variantie: Statistische variantie
  - Standaarddeviatie: Statistische standaarddeviatie

- **Plaatsing**: Waar in het tijdvenster elk uitgevoerd datapunt wordt geplaatst:
  - Einde van venster: Aan het einde van de kalenderperiode (standaard)
  - Middelpunt van venster: In het temporele midden van de kalenderperiode
  - Begin van venster: Aan het begin van de kalenderperiode
  	]],
  	["et"] = [[
Koondab andmepunktid fikseeritud kalendriperioodidesse (päevad, nädalad, kuud või aastad). Iga periood annab ühe koondatud väärtuse, mis sisaldab kõiki selle perioodi piiridesse jäävaid andmepunkte.

Näiteks päevase perioodi ja keskmise koondamise korral ühendatakse iga kalendripäeva kõik mõõtmised üheks selle päeva keskmist esindavaks väärtuseks.

**Seadistusvalikud:**

- **Periood**: kalendriperiood, mille järgi koondada (päevad, nädalad, kuud või aastad)

- **Koondamine**: igas perioodis olevate väärtustega tehtav toiming:
  - Miinimum: väikseim väärtus
  - Maksimum: suurim väärtus
  - Keskmine: kõigi väärtuste keskmine
  - Summa: kõigi väärtuste summa
  - Loendus: andmepunktide arv
  - Dispersioon: statistiline dispersioon
  - Standardhälve: statistiline standardhälve

- **Paigutus**: kuhu ajavahemikus paigutada iga väljundi andmepunkt:
  - Akna lõpp: kalendriperioodi lõppu (vaikimisi)
  - Akna keskpunkt: kalendriperioodi ajalisse keskpunkti
  - Akna algus: kalendriperioodi algusse
  	]],
  	["fil"] = [[
Pinagsasama ang mga data point sa mga nakapirming panahon ng kalendaryo (araw, linggo, buwan, o taon). Gumagawa ang bawat panahon ng isang pinagsama-samang halaga na naglalaman ng lahat ng data point na nasa loob ng mga hangganan nito.

Halimbawa, sa pang-araw-araw na panahon at average aggregation, pinagsasama ang lahat ng sukat mula sa bawat araw ng kalendaryo sa iisang halagang kumakatawan sa average ng araw na iyon.

**Mga Opsyon sa Configuration:**

- **Period**: Panahon ng kalendaryong gagamiting batayan (araw, linggo, buwan, o taon)

- **Aggregation**: Operasyong isasagawa sa mga halaga sa bawat panahon:
  - Min: Pinakamababang halaga
  - Max: Pinakamataas na halaga
  - Average: Mean ng lahat ng halaga
  - Sum: Kabuuan ng lahat ng halaga
  - Count: Bilang ng mga data point
  - Variance: Estadistikal na variance
  - Standard Deviation: Estadistikal na standard deviation

- **Placement**: Kung saan ilalagay ang bawat output data point sa time window:
  - Window End: Sa dulo ng panahon ng kalendaryo (default)
  - Window Midpoint: Sa gitnang oras ng panahon ng kalendaryo
  - Window Start: Sa simula ng panahon ng kalendaryo
  	]],
  	["fi"] = [[
Yhdistää datapisteet kiinteiksi kalenterijaksoiksi (päivät, viikot, kuukaudet tai vuodet). Jokainen jakso tuottaa yhden yhdistetyn arvon, joka sisältää kaikki jakson rajojen sisälle osuvat datapisteet.

Esimerkiksi päivittäisellä jaksolla ja keskiarvoyhdistämisellä kunkin kalenteripäivän kaikki mittaukset yhdistetään yhdeksi kyseisen päivän keskiarvoa edustavaksi arvoksi.

**Määritysasetukset:**

- **Jakso**: Kalenterijakso, jonka mukaan yhdistetään (päivät, viikot, kuukaudet tai vuodet)

- **Yhdistämistapa**: Kunkin jakson arvoille suoritettava toiminto:
  - Min: Pienin arvo
  - Max: Suurin arvo
  - Keskiarvo: Kaikkien arvojen keskiarvo
  - Summa: Kaikkien arvojen summa
  - Lukumäärä: Datapisteiden määrä
  - Varianssi: Tilastollinen varianssi
  - Keskihajonta: Tilastollinen keskihajonta

- **Sijoittelu**: Mihin kohtaan aikaikkunaa kukin tuloksena syntyvä datapiste sijoitetaan:
  - Aikaikkunan loppu: Kalenterijakson loppuun (oletus)
  - Aikaikkunan keskipiste: Kalenterijakson ajalliseen keskikohtaan
  - Aikaikkunan alku: Kalenterijakson alkuun
  	]],
  	["fr"] = [[
Agrège les points de données en périodes calendaires fixes (jours, semaines, mois ou années). Chaque période produit une valeur agrégée contenant tous les points de données compris dans ses limites.

Par exemple, avec une période quotidienne et une agrégation par moyenne, toutes les mesures de chaque jour calendaire sont combinées en une valeur représentant la moyenne de ce jour.

**Options de configuration :**

- **Période** : Période calendaire selon laquelle effectuer l’agrégation (jours, semaines, mois ou années)

- **Agrégation** : Opération à effectuer sur les valeurs de chaque période :
  - Min : Valeur minimale
  - Max : Valeur maximale
  - Moyenne : Moyenne de toutes les valeurs
  - Somme : Total de toutes les valeurs
  - Compte : Nombre de points de données
  - Variance : Variance statistique
  - Écart type : Écart type statistique

- **Positionnement** : Position de chaque point de données produit dans la fenêtre temporelle :
  - Fin de la fenêtre : À la fin de la période calendaire (par défaut)
  - Milieu de la fenêtre : Au centre temporel de la période calendaire
  - Début de la fenêtre : Au début de la période calendaire
  	]],
  	["gl"] = [[
Agrega os puntos de datos en períodos naturais fixos (días, semanas, meses ou anos). Cada período produce un valor agregado que contén todos os puntos de datos que caen dentro dos seus límites.

Por exemplo, cun período diario e unha agregación de media, todas as medicións de cada día natural combínanse nun único valor que representa a media dese día.

**Opcións de configuración:**

- **Período**: O período natural polo que se agregará (días, semanas, meses ou anos)

- **Agregación**: A operación que se realizará sobre os valores de cada período:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Media: Media de todos os valores
  - Suma: Total de todos os valores
  - Contaxe: Número de puntos de datos
  - Varianza: Varianza estatística
  - Desviación estándar: Desviación estándar estatística

- **Posición**: Onde colocar cada punto de datos de saída na xanela temporal:
  - Final da xanela: Ao final do período natural (predeterminado)
  - Punto medio da xanela: No centro temporal do período natural
  - Inicio da xanela: Ao inicio do período natural
  	]],
  	["ka"] = [[
მონაცემთა წერტილებს აერთიანებს ფიქსირებულ კალენდარულ პერიოდებად (დღეები, კვირები, თვეები ან წლები). თითოეული პერიოდი წარმოქმნის ერთ აგრეგირებულ მნიშვნელობას, რომელიც ამ პერიოდის საზღვრებში მოქცეულ ყველა მონაცემის წერტილს შეიცავს.

მაგალითად, დღიური პერიოდისა და საშუალო აგრეგაციის შემთხვევაში, თითოეული კალენდარული დღის ყველა გაზომვა ერთ მნიშვნელობად ერთიანდება, რომელიც იმ დღის საშუალოს წარმოადგენს.

**კონფიგურაციის პარამეტრები:**

- **პერიოდი**: კალენდარული პერიოდი, რომლის მიხედვითაც ხდება აგრეგაცია (დღეები, კვირები, თვეები ან წლები)

- **აგრეგაცია**: თითოეულ პერიოდში მნიშვნელობებზე შესასრულებელი ოპერაცია:
  - Min: მინიმალური მნიშვნელობა
  - Max: მაქსიმალური მნიშვნელობა
  - Average: ყველა მნიშვნელობის საშუალო
  - Sum: ყველა მნიშვნელობის ჯამი
  - Count: მონაცემთა წერტილების რაოდენობა
  - Variance: სტატისტიკური დისპერსია
  - Standard Deviation: სტატისტიკური სტანდარტული გადახრა

- **განთავსება**: დროის ფანჯარაში თითოეული გამოტანილი მონაცემის წერტილის განთავსების ადგილი:
  - Window End: კალენდარული პერიოდის ბოლოს (ნაგულისხმევი)
  - Window Midpoint: კალენდარული პერიოდის დროით შუა წერტილში
  - Window Start: კალენდარული პერიოდის დასაწყისში
  	]],
  	["de"] = [[
Aggregiert Datenpunkte in feste Kalenderzeiträume (Tage, Wochen, Monate oder Jahre). Jeder Zeitraum erzeugt einen aggregierten Wert, der alle Datenpunkte innerhalb seiner Grenzen enthält.

Bei einem täglichen Zeitraum und einer Durchschnittsaggregation werden beispielsweise alle Messungen jedes Kalendertags zu einem einzelnen Wert zusammengefasst, der den Durchschnitt dieses Tages darstellt.

**Konfigurationsoptionen:**

- **Zeitraum**: Der Kalenderzeitraum, nach dem aggregiert wird (Tage, Wochen, Monate oder Jahre)

- **Aggregation**: Der Vorgang, der auf die Werte in jedem Zeitraum angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Average: Mittelwert aller Werte
  - Sum: Summe aller Werte
  - Count: Anzahl der Datenpunkte
  - Variance: Statistische Varianz
  - Standard Deviation: Statistische Standardabweichung

- **Positionierung**: Wo jeder Ausgabedatenpunkt im Zeitfenster platziert wird:
  - Fensterende: Am Ende des Kalenderzeitraums (Standard)
  - Fenstermitte: In der zeitlichen Mitte des Kalenderzeitraums
  - Fensteranfang: Am Anfang des Kalenderzeitraums
  	]],
  	["el"] = [[
Συναθροίζει σημεία δεδομένων σε σταθερές ημερολογιακές περιόδους (ημέρες, εβδομάδες, μήνες ή έτη). Κάθε περίοδος παράγει μία συναθροισμένη τιμή που περιέχει όλα τα σημεία δεδομένων εντός των ορίων της περιόδου.

Για παράδειγμα, με ημερήσια περίοδο και συνάθροιση μέσου όρου, όλες οι μετρήσεις κάθε ημερολογιακής ημέρας συνδυάζονται σε μία τιμή που αντιπροσωπεύει τον μέσο όρο της ημέρας.

**Επιλογές διαμόρφωσης:**

- **Περίοδος**: Η ημερολογιακή περίοδος βάσει της οποίας θα γίνει η συνάθροιση (ημέρες, εβδομάδες, μήνες ή έτη)

- **Συνάθροιση**: Η πράξη που θα εκτελεστεί στις τιμές κάθε περιόδου:
  - Min: Ελάχιστη τιμή
  - Max: Μέγιστη τιμή
  - Average: Μέσος όρος όλων των τιμών
  - Sum: Σύνολο όλων των τιμών
  - Count: Αριθμός σημείων δεδομένων
  - Variance: Στατιστική διακύμανση
  - Standard Deviation: Στατιστική τυπική απόκλιση

- **Τοποθέτηση**: Πού μέσα στο χρονικό παράθυρο θα τοποθετείται κάθε σημείο δεδομένων εξόδου:
  - Τέλος παραθύρου: Στο τέλος της ημερολογιακής περιόδου (προεπιλογή)
  - Μέσο παραθύρου: Στο χρονικό κέντρο της ημερολογιακής περιόδου
  - Αρχή παραθύρου: Στην αρχή της ημερολογιακής περιόδου
  	]],
  	["gu"] = [[
ડેટા પોઇન્ટ્સને નિશ્ચિત કૅલેન્ડર સમયગાળાઓમાં (દિવસો, અઠવાડિયા, મહિના અથવા વર્ષો) એકત્રિત કરે છે. દરેક સમયગાળો તેની સીમાઓની અંદર આવતા તમામ ડેટા પોઇન્ટ્સ ધરાવતું એક એકત્રિત મૂલ્ય બનાવે છે.

ઉદાહરણ તરીકે, દૈનિક સમયગાળા અને સરેરાશ એકત્રીકરણ સાથે, દરેક કૅલેન્ડર દિવસના તમામ માપનોને તે દિવસના સરેરાશનું પ્રતિનિધિત્વ કરતા એક મૂલ્યમાં જોડવામાં આવે છે.

**ગોઠવણી વિકલ્પો:**

- **સમયગાળો**: કયા કૅલેન્ડર સમયગાળા પ્રમાણે એકત્રીકરણ કરવું (દિવસો, અઠવાડિયા, મહિના અથવા વર્ષો)

- **એકત્રીકરણ**: દરેક સમયગાળાના મૂલ્યો પર કરવાની ક્રિયા:
  - Min: લઘુત્તમ મૂલ્ય
  - Max: મહત્તમ મૂલ્ય
  - Average: બધા મૂલ્યોનો સરેરાશ
  - Sum: બધા મૂલ્યોનો સરવાળો
  - Count: ડેટા પોઇન્ટ્સની સંખ્યા
  - Variance: આંકડાકીય વિચલન
  - Standard Deviation: આંકડાકીય પ્રમાણભૂત વિચલન

- **સ્થાનનિર્ધારણ**: દરેક આઉટપુટ ડેટા પોઇન્ટને સમય વિન્ડોમાં ક્યાં મૂકવો:
  - Window End: કૅલેન્ડર સમયગાળાના અંતે (ડિફૉલ્ટ)
  - Window Midpoint: કૅલેન્ડર સમયગાળાના સમયગત મધ્યબિંદુ પર
  - Window Start: કૅલેન્ડર સમયગાળાની શરૂઆતમાં
  	]],
  	["hi"] = [[
डेटा पॉइंट को निश्चित कैलेंडर अवधियों (दिन, सप्ताह, महीने या वर्ष) में समेकित करता है। प्रत्येक अवधि एक समेकित मान बनाती है जिसमें उस अवधि की सीमाओं के भीतर आने वाले सभी डेटा पॉइंट शामिल होते हैं।

उदाहरण के लिए, दैनिक अवधि और औसत समेकन के साथ, प्रत्येक कैलेंडर दिन के सभी मापों को उस दिन के औसत को दर्शाने वाले एक मान में मिला दिया जाता है।

**कॉन्फ़िगरेशन विकल्प:**

- **अवधि**: जिस कैलेंडर अवधि के आधार पर समेकित करना है (दिन, सप्ताह, महीने या वर्ष)

- **समेकन**: प्रत्येक अवधि के मानों पर किया जाने वाला संचालन:
  - न्यूनतम: न्यूनतम मान
  - अधिकतम: अधिकतम मान
  - औसत: सभी मानों का माध्य
  - योग: सभी मानों का कुल
  - गणना: डेटा पॉइंट की संख्या
  - विचरण: सांख्यिकीय विचरण
  - मानक विचलन: सांख्यिकीय मानक विचलन

- **स्थान**: प्रत्येक आउटपुट डेटा पॉइंट को समय विंडो में कहाँ रखना है:
  - विंडो का अंत: कैलेंडर अवधि के अंत पर (डिफ़ॉल्ट)
  - विंडो का मध्यबिंदु: कैलेंडर अवधि के समयगत केंद्र पर
  - विंडो की शुरुआत: कैलेंडर अवधि की शुरुआत पर
  	]],
  	["hu"] = [[
Az adatpontokat rögzített naptári időszakokba (napok, hetek, hónapok vagy évek) összesíti. Minden időszak egy összesített értéket eredményez, amely az időszak határain belül eső összes adatpontot tartalmazza.

Például napi időszak és átlagos összesítés esetén minden naptári nap összes mérését egyetlen, az adott nap átlagát képviselő értékké egyesíti.

**Konfigurációs beállítások:**

- **Időszak**: Az összesítés naptári időszaka (napok, hetek, hónapok vagy évek)

- **Összesítés**: Az időszak értékein végrehajtandó művelet:
  - Min: Minimumérték
  - Max: Maximumérték
  - Átlag: Az összes érték átlaga
  - Összeg: Az összes érték összege
  - Darabszám: Az adatpontok száma
  - Variancia: Statisztikai variancia
  - Szórás: Statisztikai szórás

- **Elhelyezés**: Az egyes kimeneti adatpontok elhelyezése az időablakban:
  - Ablak vége: A naptári időszak végén (alapértelmezett)
  - Ablak közepe: A naptári időszak időbeli középpontjánál
  - Ablak eleje: A naptári időszak kezdetén
  	]],
  	["is"] = [[
Safnar gagnapunktum saman í föst almanakstímabil (daga, vikur, mánuði eða ár). Hvert tímabil gefur eitt safnað gildi sem inniheldur alla gagnapunkta sem falla innan marka þess tímabils.

Til dæmis, með daglegu tímabili og meðaltalssöfnun eru allar mælingar hvers almanaksdags sameinaðar í eitt gildi sem táknar meðaltal dagsins.

**Stillingar:**

- **Tímabil**: Almanakstímabilið sem safna á eftir (dagar, vikur, mánuðir eða ár)

- **Söfnun**: Aðgerðin sem á að framkvæma á gildum í hverju tímabili:
  - Lágmark: Lægsta gildi
  - Hámark: Hæsta gildi
  - Meðaltal: Meðaltal allra gilda
  - Summa: Summa allra gilda
  - Fjöldi: Fjöldi gagnapunkta
  - Dreifni: Tölfræðileg dreifni
  - Staðalfrávik: Tölfræðilegt staðalfrávik

- **Staðsetning**: Hvar í tímaglugganum hver úttaksgagnapunktur er staðsettur:
  - Lok glugga: Við lok almanakstímabilsins (sjálfgefið)
  - Miðja glugga: Í tímalegri miðju almanakstímabilsins
  - Upphaf glugga: Við upphaf almanakstímabilsins
  	]],
  	["id"] = [[
Menggabungkan titik data ke dalam periode kalender tetap (hari, minggu, bulan, atau tahun). Setiap periode menghasilkan satu nilai gabungan yang berisi semua titik data yang berada dalam batas periode tersebut.

Misalnya, dengan periode harian dan agregasi rata-rata, semua pengukuran dari setiap hari kalender digabungkan menjadi satu nilai yang mewakili rata-rata hari tersebut.

**Opsi Konfigurasi:**

- **Periode**: Periode kalender yang digunakan untuk agregasi (hari, minggu, bulan, atau tahun)

- **Agregasi**: Operasi yang dilakukan pada nilai dalam setiap periode:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Rata-rata: Rata-rata semua nilai
  - Jumlah: Total semua nilai
  - Hitungan: Jumlah titik data
  - Varians: Varians statistik
  - Deviasi Standar: Deviasi standar statistik

- **Penempatan**: Lokasi penempatan setiap titik data keluaran dalam jendela waktu:
  - Akhir Jendela: Pada akhir periode kalender (default)
  - Titik Tengah Jendela: Pada pusat waktu periode kalender
  - Awal Jendela: Pada awal periode kalender
  	]],
  	["it"] = [[
Aggrega i punti dati in periodi di calendario fissi (giorni, settimane, mesi o anni). Ogni periodo produce un valore aggregato contenente tutti i punti dati che rientrano nei limiti di quel periodo.

Ad esempio, con un periodo giornaliero e un'aggregazione media, tutte le misurazioni di ogni giorno di calendario vengono combinate in un singolo valore che rappresenta la media di quel giorno.

**Opzioni di configurazione:**

- **Periodo**: il periodo di calendario per l'aggregazione (giorni, settimane, mesi o anni)

- **Aggregazione**: l'operazione da eseguire sui valori di ogni periodo:
  - Min: valore minimo
  - Max: valore massimo
  - Media: media di tutti i valori
  - Somma: totale di tutti i valori
  - Conteggio: numero di punti dati
  - Varianza: varianza statistica
  - Deviazione standard: deviazione standard statistica

- **Posizionamento**: dove collocare ogni punto dati di output nella finestra temporale:
  - Fine della finestra: alla fine del periodo di calendario (predefinito)
  - Punto centrale della finestra: al centro temporale del periodo di calendario
  - Inizio della finestra: all'inizio del periodo di calendario
  	]],
  	["ja"] = [[
データポイントを固定された暦の期間（日、週、月、年）に集約します。各期間から、その期間の境界内にあるすべてのデータポイントを含む1つの集約値が生成されます。

たとえば、日単位の期間と平均集約を使用すると、各暦日のすべての測定値が、その日の平均を表す1つの値にまとめられます。

**設定オプション:**

- **期間**: 集約の単位となる暦の期間（日、週、月、年）

- **集約**: 各期間の値に対して実行する操作:
  - Min: 最小値
  - Max: 最大値
  - Average: すべての値の平均
  - Sum: すべての値の合計
  - Count: データポイント数
  - Variance: 統計的分散
  - Standard Deviation: 統計的標準偏差

- **配置**: 各出力データポイントを時間枠内のどこに配置するか:
  - Window End: 暦の期間の終了時点（デフォルト）
  - Window Midpoint: 暦の期間の時間的な中央
  - Window Start: 暦の期間の開始時点
  	]],
  	["kn"] = [[
ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ನಿಗದಿತ ಕ್ಯಾಲೆಂಡರ್ ಅವಧಿಗಳಾಗಿ (ದಿನಗಳು, ವಾರಗಳು, ತಿಂಗಳುಗಳು ಅಥವಾ ವರ್ಷಗಳು) ಒಟ್ಟುಗೂಡಿಸುತ್ತದೆ. ಪ್ರತಿ ಅವಧಿಯ ಗಡಿಗಳೊಳಗೆ ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಒಳಗೊಂಡ ಒಂದೇ ಒಟ್ಟುಗೂಡಿಸಿದ ಮೌಲ್ಯವನ್ನು ಪ್ರತಿ ಅವಧಿ ಉತ್ಪಾದಿಸುತ್ತದೆ.

ಉದಾಹರಣೆಗೆ, ದೈನಂದಿನ ಅವಧಿ ಮತ್ತು ಸರಾಸರಿ ಒಟ್ಟುಗೂಡಿಸುವಿಕೆಯೊಂದಿಗೆ, ಪ್ರತಿ ಕ್ಯಾಲೆಂಡರ್ ದಿನದ ಎಲ್ಲಾ ಮಾಪನಗಳನ್ನು ಆ ದಿನದ ಸರಾಸರಿಯನ್ನು ಪ್ರತಿನಿಧಿಸುವ ಒಂದೇ ಮೌಲ್ಯವಾಗಿ ಸಂಯೋಜಿಸಲಾಗುತ್ತದೆ.

**ಕಾನ್ಫಿಗರೇಶನ್ ಆಯ್ಕೆಗಳು:**

- **ಅವಧಿ**: ಒಟ್ಟುಗೂಡಿಸಬೇಕಾದ ಕ್ಯಾಲೆಂಡರ್ ಅವಧಿ (ದಿನಗಳು, ವಾರಗಳು, ತಿಂಗಳುಗಳು ಅಥವಾ ವರ್ಷಗಳು)

- **ಒಟ್ಟುಗೂಡಿಸುವಿಕೆ**: ಪ್ರತಿ ಅವಧಿಯ ಮೌಲ್ಯಗಳ ಮೇಲೆ ನಡೆಸುವ ಕಾರ್ಯಾಚರಣೆ:
  - Min: ಕನಿಷ್ಠ ಮೌಲ್ಯ
  - Max: ಗರಿಷ್ಠ ಮೌಲ್ಯ
  - Average: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಸರಾಸರಿ
  - Sum: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಮೊತ್ತ
  - Count: ಡೇಟಾ ಬಿಂದುಗಳ ಸಂಖ್ಯೆ
  - Variance: ಸಾಂಖ್ಯಿಕ ವ್ಯತ್ಯಾಸ
  - Standard Deviation: ಸಾಂಖ್ಯಿಕ ಪ್ರಮಾಣಿತ ವಿಚಲನ

- **ಸ್ಥಾನ**: ಪ್ರತಿ ಔಟ್‌ಪುಟ್ ಡೇಟಾ ಬಿಂದುವನ್ನು ಸಮಯ ವಿಂಡೋದಲ್ಲಿ ಇರಿಸುವ ಸ್ಥಳ:
  - Window End: ಕ್ಯಾಲೆಂಡರ್ ಅವಧಿಯ ಅಂತ್ಯದಲ್ಲಿ (ಡೀಫಾಲ್ಟ್)
  - Window Midpoint: ಕ್ಯಾಲೆಂಡರ್ ಅವಧಿಯ ಸಮಯದ ಮಧ್ಯಭಾಗದಲ್ಲಿ
  - Window Start: ಕ್ಯಾಲೆಂಡರ್ ಅವಧಿಯ ಪ್ರಾರಂಭದಲ್ಲಿ
  	]],
  	["kk"] = [[
Дерек нүктелерін тұрақты күнтізбелік кезеңдерге (күндер, апталар, айлар немесе жылдар) біріктіреді. Әр кезең сол кезең шегіне түсетін барлық дерек нүктелерін қамтитын бір біріктірілген мән береді.

Мысалы, күнделікті кезең және орташа мәнді біріктіру таңдалса, әр күнтізбелік күндегі барлық өлшемдер сол күннің орташа мәнін көрсететін бір мәнге біріктіріледі.

**Конфигурация параметрлері:**

- **Кезең**: Біріктіру жүргізілетін күнтізбелік кезең (күндер, апталар, айлар немесе жылдар)

- **Біріктіру**: Әр кезеңдегі мәндерге орындалатын операция:
  - Min: Ең төменгі мән
  - Max: Ең жоғары мән
  - Average: Барлық мәндердің орташа мәні
  - Sum: Барлық мәндердің қосындысы
  - Count: Дерек нүктелерінің саны
  - Variance: Статистикалық дисперсия
  - Standard Deviation: Статистикалық стандартты ауытқу

- **Орналастыру**: Әр шығыс дерек нүктесін уақыт терезесінің қай жеріне орналастыру керек:
  - Window End: Күнтізбелік кезеңнің соңында (әдепкі)
  - Window Midpoint: Күнтізбелік кезеңнің уақыт бойынша ортасында
  - Window Start: Күнтізбелік кезеңнің басында
  	]],
  	["km"] = [[
ប្រមូលផ្តុំចំណុចទិន្នន័យទៅក្នុងរយៈពេលប្រតិទិនថេរ (ថ្ងៃ សប្តាហ៍ ខែ ឬឆ្នាំ)។ រយៈពេលនីមួយៗបង្កើតតម្លៃប្រមូលផ្តុំមួយ ដែលរួមបញ្ចូលចំណុចទិន្នន័យទាំងអស់ក្នុងព្រំដែនរបស់រយៈពេលនោះ។

ឧទាហរណ៍ ជាមួយរយៈពេលប្រចាំថ្ងៃ និងការប្រមូលផ្តុំជាមធ្យម ការវាស់វែងទាំងអស់ពីថ្ងៃប្រតិទិននីមួយៗត្រូវបានបញ្ចូលជាតម្លៃតែមួយ ដែលតំណាងឱ្យមធ្យមភាគសម្រាប់ថ្ងៃនោះ។

**ជម្រើសការកំណត់៖**

- **រយៈពេល**៖ រយៈពេលប្រតិទិនដែលត្រូវប្រមូលផ្តុំតាម (ថ្ងៃ សប្តាហ៍ ខែ ឬឆ្នាំ)

- **ការប្រមូលផ្តុំ**៖ ប្រតិបត្តិការដែលត្រូវអនុវត្តលើតម្លៃក្នុងរយៈពេលនីមួយៗ៖
  - Min៖ តម្លៃអប្បបរមា
  - Max៖ តម្លៃអតិបរមា
  - Average៖ មធ្យមភាគនៃតម្លៃទាំងអស់
  - Sum៖ ផលបូកសរុបនៃតម្លៃទាំងអស់
  - Count៖ ចំនួនចំណុចទិន្នន័យ
  - Variance៖ វ៉ារ្យង់ស្ថិតិ
  - Standard Deviation៖ គម្លាតស្តង់ដារស្ថិតិ

- **ទីតាំង**៖ កន្លែងក្នុងបង្អួចពេលវេលាដែលត្រូវដាក់ចំណុចទិន្នន័យលទ្ធផលនីមួយៗ៖
  - Window End៖ នៅចុងរយៈពេលប្រតិទិន (លំនាំដើម)
  - Window Midpoint៖ នៅចំណុចកណ្តាលពេលវេលានៃរយៈពេលប្រតិទិន
  - Window Start៖ នៅដើមរយៈពេលប្រតិទិន
  	]],
  	["ko"] = [[
데이터 포인트를 고정된 달력 기간(일, 주, 월 또는 년)으로 집계합니다. 각 기간은 해당 기간의 경계 안에 속하는 모든 데이터 포인트를 포함한 하나의 집계 값을 생성합니다.

예를 들어 일 단위 기간과 평균 집계를 사용하면 각 날짜의 모든 측정값을 해당 날짜의 평균을 나타내는 하나의 값으로 결합합니다.

**구성 옵션:**

- **기간**: 집계할 달력 기간(일, 주, 월 또는 년)

- **집계**: 각 기간의 값에 수행할 작업:
  - Min: 최솟값
  - Max: 최댓값
  - Average: 모든 값의 평균
  - Sum: 모든 값의 합계
  - Count: 데이터 포인트 수
  - Variance: 통계적 분산
  - Standard Deviation: 통계적 표준편차

- **배치**: 각 출력 데이터 포인트를 시간 창 내 어디에 배치할지:
  - Window End: 달력 기간의 끝(기본값)
  - Window Midpoint: 달력 기간의 시간상 중앙
  - Window Start: 달력 기간의 시작
  	]],
  	["ky"] = [[
Маалымат чекиттерин белгиленген календардык мезгилдерге (күндөргө, жумаларга, айларга же жылдарга) бириктирет. Ар бир мезгил ошол мезгилдин чектерине кирген бардык маалымат чекиттерин камтыган бир жыйынтык маанини түзөт.

Мисалы, күндүк мезгил жана орточо бириктирүү тандалса, ар бир календардык күндөгү бардык өлчөөлөр ошол күндүн орточо маанисин көрсөткөн бир мааниге бириктирилет.

**Тууралоо параметрлери:**

- **Мезгил**: Бириктириле турган календардык мезгил (күндөр, жумалар, айлар же жылдар)

- **Бириктирүү**: Ар бир мезгилдеги маанилерге аткарылуучу операция:
  - Min: Минималдуу маани
  - Max: Максималдуу маани
  - Average: Бардык маанилердин орточо мааниси
  - Sum: Бардык маанилердин суммасы
  - Count: Маалымат чекиттеринин саны
  - Variance: Статистикалык дисперсия
  - Standard Deviation: Статистикалык стандарттык четтөө

- **Жайгаштыруу**: Ар бир чыгаруу маалымат чекитин убакыт терезесинде жайгаштыруу орду:
  - Терезенин аягы: Календардык мезгилдин аягында (демейки)
  - Терезенин ортосу: Календардык мезгилдин убакыттык ортосунда
  - Терезенин башы: Календардык мезгилдин башында
  	]],
  	["lo"] = [[
ລວມຈຸດຂໍ້ມູນເຂົ້າເປັນໄລຍະຕາມປະຕິທິນທີ່ກຳນົດຕາຍຕົວ (ມື້, ອາທິດ, ເດືອນ ຫຼື ປີ). ແຕ່ລະໄລຍະຈະສ້າງຄ່າທີ່ລວມແລ້ວໜຶ່ງຄ່າ ເຊິ່ງບັນຈຸຈຸດຂໍ້ມູນທັງໝົດທີ່ຢູ່ພາຍໃນຂອບເຂດຂອງໄລຍະນັ້ນ.

ຕົວຢ່າງ: ສຳລັບໄລຍະລາຍວັນ ແລະ ການລວມແບບສະເລ່ຍ, ຄ່າວັດແທກທັງໝົດຈາກແຕ່ລະວັນຕາມປະຕິທິນຈະຖືກລວມເປັນຄ່າດຽວທີ່ສະແດງຄ່າສະເລ່ຍຂອງວັນນັ້ນ.

**ຕົວເລືອກການຕັ້ງຄ່າ:**

- **ໄລຍະ**: ໄລຍະຕາມປະຕິທິນທີ່ຈະໃຊ້ລວມຂໍ້ມູນ (ມື້, ອາທິດ, ເດືອນ ຫຼື ປີ)

- **ການລວມ**: ການດຳເນີນການທີ່ຈະເຮັດກັບຄ່າໃນແຕ່ລະໄລຍະ:
  - ຕ່ຳສຸດ: ຄ່າຕ່ຳສຸດ
  - ສູງສຸດ: ຄ່າສູງສຸດ
  - ສະເລ່ຍ: ຄ່າສະເລ່ຍຂອງທຸກຄ່າ
  - ຜົນລວມ: ຜົນລວມຂອງທຸກຄ່າ
  - ຈຳນວນ: ຈຳນວນຈຸດຂໍ້ມູນ
  - ຄວາມແປປວນ: ຄວາມແປປວນທາງສະຖິຕິ
  - ຄ່າເບ່ຽງເບນມາດຕະຖານ: ຄ່າເບ່ຽງເບນມາດຕະຖານທາງສະຖິຕິ

- **ຕຳແໜ່ງ**: ບ່ອນທີ່ຈະວາງຈຸດຂໍ້ມູນຜົນລັບແຕ່ລະຈຸດໃນໜ້າຕ່າງເວລາ:
  - ທ້າຍໜ້າຕ່າງ: ທີ່ທ້າຍໄລຍະຕາມປະຕິທິນ (ຄ່າເລີ່ມຕົ້ນ)
  - ຈຸດກາງໜ້າຕ່າງ: ທີ່ຈຸດກາງດ້ານເວລາຂອງໄລຍະຕາມປະຕິທິນ
  - ເລີ່ມໜ້າຕ່າງ: ທີ່ຕົ້ນໄລຍະຕາມປະຕິທິນ
  	]],
  	["lv"] = [[
Apkopo datu punktus noteiktos kalendāra periodos (dienās, nedēļās, mēnešos vai gados). Katram periodam tiek izveidota viena apkopota vērtība, kas ietver visus datu punktus, kuri ietilpst šī perioda robežās.

Piemēram, izmantojot dienas periodu un vidējās vērtības apkopošanu, visi katras kalendārās dienas mērījumi tiek apvienoti vienā vērtībā, kas attēlo šīs dienas vidējo vērtību.

**Konfigurācijas opcijas:**

- **Periods**: Kalendāra periods, pēc kura apkopot (dienas, nedēļas, mēneši vai gadi)

- **Apkopošana**: Darbība, kas jāveic ar katra perioda vērtībām:
  - Minimums: Minimālā vērtība
  - Maksimums: Maksimālā vērtība
  - Vidējais: Visu vērtību vidējā vērtība
  - Summa: Visu vērtību summa
  - Skaits: Datu punktu skaits
  - Dispersija: Statistiskā dispersija
  - Standartnovirze: Statistiskā standartnovirze

- **Novietojums**: Kur laika logā novietot katru izvades datu punktu:
  - Loga beigas: Kalendāra perioda beigās (noklusējums)
  - Loga viduspunkts: Kalendāra perioda laika vidū
  - Loga sākums: Kalendāra perioda sākumā
  	]],
  	["lt"] = [[
Apibendrina duomenų taškus į fiksuotus kalendorinius laikotarpius (dienas, savaites, mėnesius arba metus). Kiekvienas laikotarpis sukuria vieną apibendrintą reikšmę, apimančią visus į jo ribas patenkančius duomenų taškus.

Pavyzdžiui, pasirinkus dienos laikotarpį ir vidurkio apibendrinimą, visi kiekvienos kalendorinės dienos matavimai sujungiami į vieną tos dienos vidurkį reiškiančią reikšmę.

**Konfigūracijos parinktys:**

- **Laikotarpis**: Kalendorinis laikotarpis, pagal kurį apibendrinti (dienos, savaitės, mėnesiai arba metai)

- **Apibendrinimas**: Kiekvieno laikotarpio reikšmėms taikoma operacija:
  - Min.: Mažiausia reikšmė
  - Maks.: Didžiausia reikšmė
  - Vidurkis: Visų reikšmių vidurkis
  - Suma: Visų reikšmių suma
  - Kiekis: Duomenų taškų skaičius
  - Dispersija: Statistinė dispersija
  - Standartinis nuokrypis: Statistinis standartinis nuokrypis

- **Vieta**: Kur laiko lange patalpinti kiekvieną išvesties duomenų tašką:
  - Lango pabaiga: Kalendorinio laikotarpio pabaigoje (numatyta)
  - Lango vidurys: Kalendorinio laikotarpio laiko viduryje
  - Lango pradžia: Kalendorinio laikotarpio pradžioje
  	]],
  	["mk"] = [[
Ги собира точките на податоци во фиксни календарски периоди (денови, недели, месеци или години). Секој период создава една агрегирана вредност што ги содржи сите точки на податоци што спаѓаат во границите на тој период.

На пример, со дневен период и просечна агрегација, сите мерења од секој календарски ден се комбинираат во една вредност што го претставува просекот за тој ден.

**Опции за конфигурирање:**

- **Период**: Календарскиот период според кој се врши агрегацијата (денови, недели, месеци или години)

- **Агрегација**: Операцијата што се извршува врз вредностите во секој период:
  - Min: Минимална вредност
  - Max: Максимална вредност
  - Average: Просек од сите вредности
  - Sum: Збир од сите вредности
  - Count: Број на точки на податоци
  - Variance: Статистичка варијанса
  - Standard Deviation: Статистичко стандардно отстапување

- **Поставување**: Каде во временскиот прозорец да се постави секоја излезна точка на податоци:
  - Крај на прозорецот: На крајот од календарскиот период (стандардно)
  - Средина на прозорецот: Во временскиот центар на календарскиот период
  - Почеток на прозорецот: На почетокот од календарскиот период
  	]],
  	["ms"] = [[
Mengagregat titik data ke dalam tempoh kalendar tetap (hari, minggu, bulan atau tahun). Setiap tempoh menghasilkan satu nilai agregat yang mengandungi semua titik data dalam sempadan tempoh tersebut.

Sebagai contoh, dengan tempoh harian dan pengagregatan purata, semua ukuran daripada setiap hari kalendar digabungkan menjadi satu nilai yang mewakili purata hari tersebut.

**Pilihan Konfigurasi:**

- **Tempoh**: Tempoh kalendar untuk pengagregatan (hari, minggu, bulan atau tahun)

- **Pengagregatan**: Operasi yang hendak dilaksanakan pada nilai dalam setiap tempoh:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Purata: Min semua nilai
  - Jumlah: Jumlah semua nilai
  - Kiraan: Bilangan titik data
  - Varians: Varians statistik
  - Sisihan Piawai: Sisihan piawai statistik

- **Penempatan**: Tempat untuk meletakkan setiap titik data output dalam tetingkap masa:
  - Hujung Tetingkap: Pada penghujung tempoh kalendar (lalai)
  - Titik Tengah Tetingkap: Pada pusat masa tempoh kalendar
  - Permulaan Tetingkap: Pada permulaan tempoh kalendar
  	]],
  	["ml"] = [[
ഡാറ്റാ പോയിന്റുകളെ നിശ്ചിത കലണ്ടർ കാലയളവുകളായി (ദിവസങ്ങൾ, ആഴ്ചകൾ, മാസങ്ങൾ, അല്ലെങ്കിൽ വർഷങ്ങൾ) സംഗ്രഹിക്കുന്നു. ഓരോ കാലയളവും അതിന്റെ പരിധിക്കുള്ളിൽ വരുന്ന എല്ലാ ഡാറ്റാ പോയിന്റുകളും ഉൾക്കൊള്ളുന്ന ഒരു സംഗ്രഹ മൂല്യം സൃഷ്ടിക്കുന്നു.

ഉദാഹരണത്തിന്, പ്രതിദിന കാലയളവും ശരാശരി അഗ്രിഗേഷനും ഉപയോഗിക്കുമ്പോൾ, ഓരോ കലണ്ടർ ദിവസത്തിലെയും എല്ലാ അളവുകളും ആ ദിവസത്തെ ശരാശരിയെ പ്രതിനിധീകരിക്കുന്ന ഒരൊറ്റ മൂല്യമായി സംയോജിപ്പിക്കുന്നു.

**കോൺഫിഗറേഷൻ ഓപ്ഷനുകൾ:**

- **Period**: സംഗ്രഹിക്കേണ്ട കലണ്ടർ കാലയളവ് (ദിവസങ്ങൾ, ആഴ്ചകൾ, മാസങ്ങൾ, അല്ലെങ്കിൽ വർഷങ്ങൾ)

- **Aggregation**: ഓരോ കാലയളവിലെയും മൂല്യങ്ങളിൽ നടത്തേണ്ട പ്രവർത്തനം:
  - Min: കുറഞ്ഞ മൂല്യം
  - Max: കൂടിയ മൂല്യം
  - Average: എല്ലാ മൂല്യങ്ങളുടെയും ശരാശരി
  - Sum: എല്ലാ മൂല്യങ്ങളുടെയും ആകെ തുക
  - Count: ഡാറ്റാ പോയിന്റുകളുടെ എണ്ണം
  - Variance: സ്ഥിതിവിവര വ്യതിയാനം
  - Standard Deviation: സ്ഥിതിവിവര സ്റ്റാൻഡേർഡ് ഡിവിയേഷൻ

- **Placement**: ഓരോ ഔട്ട്പുട്ട് ഡാറ്റാ പോയിന്റും സമയ വിൻഡോയിൽ സ്ഥാപിക്കേണ്ട സ്ഥാനം:
  - Window End: കലണ്ടർ കാലയളവിന്റെ അവസാനം (സ്ഥിരസ്ഥിതി)
  - Window Midpoint: കലണ്ടർ കാലയളവിന്റെ സമയമധ്യത്തിൽ
  - Window Start: കലണ്ടർ കാലയളവിന്റെ ആരംഭത്തിൽ
  	]],
  	["mr"] = [[
डेटा पॉइंट्सना निश्चित कॅलेंडर कालावधींमध्ये (दिवस, आठवडे, महिने किंवा वर्षे) एकत्रित करते. प्रत्येक कालावधीसाठी त्या कालावधीच्या मर्यादांमध्ये येणारे सर्व डेटा पॉइंट्स असलेले एक एकत्रित मूल्य तयार होते.

उदाहरणार्थ, दैनिक कालावधी आणि सरासरी एकत्रीकरण वापरल्यास, प्रत्येक कॅलेंडर दिवसातील सर्व मोजमापे त्या दिवसाची सरासरी दर्शविणाऱ्या एका मूल्यामध्ये एकत्रित केली जातात.

**कॉन्फिगरेशन पर्याय:**

- **कालावधी**: ज्यानुसार एकत्रीकरण करायचे तो कॅलेंडर कालावधी (दिवस, आठवडे, महिने किंवा वर्षे)

- **एकत्रीकरण**: प्रत्येक कालावधीतील मूल्यांवर करायची क्रिया:
  - किमान: किमान मूल्य
  - कमाल: कमाल मूल्य
  - सरासरी: सर्व मूल्यांची सरासरी
  - बेरीज: सर्व मूल्यांची एकूण बेरीज
  - संख्या: डेटा पॉइंट्सची संख्या
  - विचरण: सांख्यिकीय विचरण
  - प्रमाणित विचलन: सांख्यिकीय प्रमाणित विचलन

- **स्थाननियोजन**: प्रत्येक आउटपुट डेटा पॉइंट वेळेच्या पट्टीत कुठे ठेवायचा:
  - पट्टीचा शेवट: कॅलेंडर कालावधीच्या शेवटी (डीफॉल्ट)
  - पट्टीचा मध्यबिंदू: कॅलेंडर कालावधीच्या कालगत मध्यभागी
  - पट्टीची सुरुवात: कॅलेंडर कालावधीच्या सुरुवातीला
  	]],
  	["mn"] = [[
Өгөгдлийн цэгүүдийг тогтмол календарийн үеүдэд (өдөр, долоо хоног, сар эсвэл жил) нэгтгэнэ. Үе бүр тухайн үеийн хил хязгаарт багтах бүх өгөгдлийн цэгийг агуулсан нэг нэгтгэсэн утга үүсгэнэ.

Жишээлбэл, өдөр бүрийн үе ба дундаж нэгтгэл ашиглавал календарийн өдөр бүрийн бүх хэмжилтийг тухайн өдрийн дунджийг илэрхийлэх нэг утга болгон нэгтгэнэ.

**Тохиргооны сонголтууд:**

- **Үе**: Нэгтгэх календарийн үе (өдөр, долоо хоног, сар эсвэл жил)

- **Нэгтгэл**: Үе бүрийн утгад хийх үйлдэл:
  - Min: Хамгийн бага утга
  - Max: Хамгийн их утга
  - Average: Бүх утгын дундаж
  - Sum: Бүх утгын нийлбэр
  - Count: Өгөгдлийн цэгийн тоо
  - Variance: Статистикийн дисперс
  - Standard Deviation: Статистикийн стандарт хазайлт

- **Байрлал**: Гаралтын өгөгдлийн цэгийг хугацааны цонхны хаана байрлуулах:
  - Window End: Календарийн үеийн төгсгөлд (анхдагч)
  - Window Midpoint: Календарийн үеийн хугацааны дунд цэгт
  - Window Start: Календарийн үеийн эхлэлд
  	]],
  	["ne"] = [[
डेटा बिन्दुहरूलाई निश्चित पात्रो अवधिमा (दिन, हप्ता, महिना वा वर्ष) समेकित गर्छ। प्रत्येक अवधिले त्यसका सीमाभित्र पर्ने सबै डेटा बिन्दु समेटिएको एउटा समेकित मान उत्पादन गर्छ।

उदाहरणका लागि, दैनिक अवधि र औसत समेकन हुँदा प्रत्येक पात्रो दिनका सबै मापनलाई त्यस दिनको औसत जनाउने एउटै मानमा मिलाइन्छ।

**कन्फिगरेसन विकल्पहरू:**

- **अवधि**: समेकन गर्ने पात्रो अवधि (दिन, हप्ता, महिना वा वर्ष)

- **समेकन**: प्रत्येक अवधिका मानमा लागू गर्ने कार्य:
  - Min: न्यूनतम मान
  - Max: अधिकतम मान
  - Average: सबै मानहरूको औसत
  - Sum: सबै मानहरूको योग
  - Count: डेटा बिन्दुहरूको संख्या
  - Variance: सांख्यिकीय विचरण
  - Standard Deviation: सांख्यिकीय मानक विचलन

- **स्थान**: प्रत्येक आउटपुट डेटा बिन्दुलाई समय विन्डोमा राख्ने स्थान:
  - Window End: पात्रो अवधिको अन्त्यमा (पूर्वनिर्धारित)
  - Window Midpoint: पात्रो अवधिको समयगत केन्द्रमा
  - Window Start: पात्रो अवधिको सुरुवातमा
  	]],
  	["no"] = [[
Aggregerer datapunkter i faste kalenderperioder (dager, uker, måneder eller år). Hver periode produserer én aggregert verdi som inneholder alle datapunkter som faller innenfor periodens grenser.

Med en daglig periode og gjennomsnittsaggregering kombineres for eksempel alle målinger fra hver kalenderdag til én enkelt verdi som representerer gjennomsnittet for den dagen.

**Konfigurasjonsalternativer:**

- **Periode**: Kalenderperioden det skal aggregeres etter (dager, uker, måneder eller år)

- **Aggregering**: Operasjonen som skal utføres på verdiene i hver periode:
  - Min: Minimumsverdi
  - Maks: Maksimumsverdi
  - Gjennomsnitt: Gjennomsnittet av alle verdier
  - Sum: Summen av alle verdier
  - Antall: Antall datapunkter
  - Varians: Statistisk varians
  - Standardavvik: Statistisk standardavvik

- **Plassering**: Hvor i tidsvinduet hvert utgående datapunkt skal plasseres:
  - Slutt på vinduet: Ved slutten av kalenderperioden (standard)
  - Midtpunkt i vinduet: Ved kalenderperiodens tidsmessige sentrum
  - Start på vinduet: Ved starten av kalenderperioden
  	]],
  	["pl"] = [[
Agreguje punkty danych do stałych okresów kalendarzowych (dni, tygodni, miesięcy lub lat). Każdy okres generuje jedną zagregowaną wartość zawierającą wszystkie punkty danych mieszczące się w granicach tego okresu.

Na przykład przy okresie dziennym i agregacji średniej wszystkie pomiary z każdego dnia kalendarzowego są łączone w jedną wartość reprezentującą średnią dla tego dnia.

**Opcje konfiguracji:**

- **Okres**: Okres kalendarzowy używany do agregacji (dni, tygodnie, miesiące lub lata)

- **Agregacja**: Operacja wykonywana na wartościach w każdym okresie:
  - Min: Wartość minimalna
  - Max: Wartość maksymalna
  - Średnia: Średnia wszystkich wartości
  - Suma: Suma wszystkich wartości
  - Liczba: Liczba punktów danych
  - Wariancja: Wariancja statystyczna
  - Odchylenie standardowe: Odchylenie standardowe

- **Położenie**: Miejsce umieszczenia każdego wyjściowego punktu danych w oknie czasowym:
  - Koniec okna: Na końcu okresu kalendarzowego (domyślnie)
  - Środek okna: W czasowym środku okresu kalendarzowego
  - Początek okna: Na początku okresu kalendarzowego
  	]],
  	["pt"] = [[
Agrega pontos de dados em períodos de calendário fixos (dias, semanas, meses ou anos). Cada período produz um valor agregado contendo todos os pontos de dados que se encontram dentro dos limites desse período.

Por exemplo, com um período diário e uma agregação pela média, todas as medições de cada dia do calendário são combinadas num único valor que representa a média desse dia.

**Opções de configuração:**

- **Período**: O período de calendário pelo qual agregar (dias, semanas, meses ou anos)

- **Agregação**: A operação a executar nos valores de cada período:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Média: Média de todos os valores
  - Soma: Total de todos os valores
  - Contagem: Número de pontos de dados
  - Variância: Variância estatística
  - Desvio padrão: Desvio padrão estatístico

- **Posicionamento**: Onde colocar cada ponto de dados de saída na janela temporal:
  - Fim da janela: No fim do período de calendário (predefinição)
  - Ponto médio da janela: No centro temporal do período de calendário
  - Início da janela: No início do período de calendário
  	]],
  	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਨਿਸ਼ਚਿਤ ਕੈਲੰਡਰ ਅਵਧੀਆਂ (ਦਿਨ, ਹਫ਼ਤੇ, ਮਹੀਨੇ ਜਾਂ ਸਾਲ) ਵਿੱਚ ਇਕੱਠਾ ਕਰਦਾ ਹੈ। ਹਰੇਕ ਅਵਧੀ ਇੱਕ ਇਕੱਠਾ ਕੀਤਾ ਮੁੱਲ ਤਿਆਰ ਕਰਦੀ ਹੈ ਜਿਸ ਵਿੱਚ ਉਸ ਅਵਧੀ ਦੀਆਂ ਹੱਦਾਂ ਅੰਦਰ ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਹੁੰਦੇ ਹਨ।

ਉਦਾਹਰਨ ਲਈ, ਰੋਜ਼ਾਨਾ ਅਵਧੀ ਅਤੇ ਔਸਤ ਇਕੱਠ ਨਾਲ, ਹਰੇਕ ਕੈਲੰਡਰ ਦਿਨ ਦੀਆਂ ਸਾਰੀਆਂ ਮਾਪਾਂ ਨੂੰ ਉਸ ਦਿਨ ਦੀ ਔਸਤ ਦਰਸਾਉਣ ਵਾਲੇ ਇੱਕ ਮੁੱਲ ਵਿੱਚ ਮਿਲਾਇਆ ਜਾਂਦਾ ਹੈ।

**ਕੌਂਫਿਗਰੇਸ਼ਨ ਵਿਕਲਪ:**

- **ਅਵਧੀ**: ਜਿਸ ਕੈਲੰਡਰ ਅਵਧੀ ਅਨੁਸਾਰ ਇਕੱਠ ਕਰਨਾ ਹੈ (ਦਿਨ, ਹਫ਼ਤੇ, ਮਹੀਨੇ ਜਾਂ ਸਾਲ)

- **ਇਕੱਠ**: ਹਰੇਕ ਅਵਧੀ ਦੇ ਮੁੱਲਾਂ 'ਤੇ ਕੀਤੀ ਜਾਣ ਵਾਲੀ ਕਾਰਵਾਈ:
  - Min: ਨਿਊਨਤਮ ਮੁੱਲ
  - Max: ਅਧਿਕਤਮ ਮੁੱਲ
  - Average: ਸਾਰੇ ਮੁੱਲਾਂ ਦੀ ਔਸਤ
  - Sum: ਸਾਰੇ ਮੁੱਲਾਂ ਦਾ ਜੋੜ
  - Count: ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੀ ਗਿਣਤੀ
  - Variance: ਅੰਕੜਾਤਮਕ ਵੈਰੀਅੰਸ
  - Standard Deviation: ਅੰਕੜਾਤਮਕ ਮਿਆਰੀ ਵਿਸਥਾਪਨ

- **ਸਥਿਤੀ**: ਹਰੇਕ ਆਉਟਪੁੱਟ ਡਾਟਾ ਪੁਆਇੰਟ ਨੂੰ ਸਮਾਂ-ਖਿੜਕੀ ਵਿੱਚ ਕਿੱਥੇ ਰੱਖਣਾ ਹੈ:
  - Window End: ਕੈਲੰਡਰ ਅਵਧੀ ਦੇ ਅੰਤ 'ਤੇ (ਮੂਲ)
  - Window Midpoint: ਕੈਲੰਡਰ ਅਵਧੀ ਦੇ ਸਮੇਂਕ ਕੇਂਦਰ 'ਤੇ
  - Window Start: ਕੈਲੰਡਰ ਅਵਧੀ ਦੀ ਸ਼ੁਰੂਆਤ 'ਤੇ
  	]],
  	["ro"] = [[
Agregă punctele de date în perioade calendaristice fixe (zile, săptămâni, luni sau ani). Fiecare perioadă produce o valoare agregată care conține toate punctele de date din limitele acelei perioade.

De exemplu, cu o perioadă zilnică și o agregare prin medie, toate măsurătorile din fiecare zi calendaristică sunt combinate într-o singură valoare care reprezintă media zilei respective.

**Opțiuni de configurare:**

- **Perioadă**: Perioada calendaristică după care se face agregarea (zile, săptămâni, luni sau ani)

- **Agregare**: Operația efectuată asupra valorilor din fiecare perioadă:
  - Min: Valoarea minimă
  - Max: Valoarea maximă
  - Medie: Media tuturor valorilor
  - Sumă: Totalul tuturor valorilor
  - Număr: Numărul de puncte de date
  - Varianță: Varianța statistică
  - Deviație standard: Deviația standard statistică

- **Poziționare**: Unde să fie plasat fiecare punct de date rezultat în fereastra de timp:
  - Sfârșitul ferestrei: La sfârșitul perioadei calendaristice (implicit)
  - Mijlocul ferestrei: În centrul temporal al perioadei calendaristice
  - Începutul ferestrei: La începutul perioadei calendaristice
  	]],
  	["rm"] = [[
Agregescha puncts da datas en periodas fixas dal chalender (dis, emnas, mais u onns). Mintga perioda producescha ina valur agregada che cuntegna tut ils puncts da datas che crodan entaifer ils cunfins da questa perioda.

Per exempel, cun ina perioda quotidiana ed ina agregaziun da la media, vegnan tut las mesiraziuns da mintga di dal chalender cumbinadas en ina singula valur che represchenta la media da quel di.

**Opziuns da configuraziun:**

- **Perioda**: La perioda dal chalender tenor la quala vegnir agregà (dis, emnas, mais u onns)

- **Agregaziun**: L'operaziun da far sin las valurs en mintga perioda:
  - Min: Valur minimala
  - Max: Valur maximala
  - Media: Media da tut las valurs
  - Summa: Total da tut las valurs
  - Dumber: Dumber da puncts da datas
  - Varianta: Varianta statistica
  - Deviaziun standard: Deviaziun standard statistica

- **Posiziun**: Nua plazzar mintga punct da datas d'output en la fanestra temporala:
  - Fin da la fanestra: A la fin da la perioda dal chalender (standard)
  - Mez da la fanestra: Al center temporal da la perioda dal chalender
  - Cumenzament da la fanestra: Al cumenzament da la perioda dal chalender
  	]],
  	["ru"] = [[
Объединяет точки данных в фиксированные календарные периоды (дни, недели, месяцы или годы). Каждый период создаёт одно агрегированное значение, содержащее все точки данных, попавшие в его границы.

Например, при дневном периоде и агрегации среднего все измерения за каждый календарный день объединяются в одно значение, представляющее среднее за этот день.

**Параметры конфигурации:**

- **Период**: Календарный период агрегации (дни, недели, месяцы или годы)

- **Агрегация**: Операция над значениями в каждом периоде:
  - Минимум: Минимальное значение
  - Максимум: Максимальное значение
  - Среднее: Среднее всех значений
  - Сумма: Сумма всех значений
  - Количество: Число точек данных
  - Дисперсия: Статистическая дисперсия
  - Стандартное отклонение: Статистическое стандартное отклонение

- **Размещение**: Где размещать каждую выходную точку данных во временном окне:
  - Конец окна: В конце календарного периода (по умолчанию)
  - Середина окна: В центре календарного периода
  - Начало окна: В начале календарного периода
  	]],
  	["sr"] = [[
Objedinjuje tačke podataka u fiksne kalendarske periode (dane, nedelje, mesece ili godine). Svaki period daje jednu objedinjenu vrednost koja sadrži sve tačke podataka unutar granica tog perioda.

Na primer, uz dnevni period i objedinjavanje prosekom, sva merenja iz svakog kalendarskog dana kombinuju se u jednu vrednost koja predstavlja prosek za taj dan.

**Opcije konfiguracije:**

- **Period**: Kalendarski period po kojem se vrši objedinjavanje (dani, nedelje, meseci ili godine)

- **Objedinjavanje**: Operacija koja se izvršava nad vrednostima u svakom periodu:
  - Min: Minimalna vrednost
  - Max: Maksimalna vrednost
  - Prosek: Srednja vrednost svih vrednosti
  - Zbir: Zbir svih vrednosti
  - Broj: Broj tačaka podataka
  - Varijansa: Statistička varijansa
  - Standardna devijacija: Statistička standardna devijacija

- **Položaj**: Mesto u vremenskom prozoru na koje se postavlja svaka izlazna tačka podataka:
  - Kraj prozora: Na kraju kalendarskog perioda (podrazumevano)
  - Sredina prozora: Na vremenskoj sredini kalendarskog perioda
  - Početak prozora: Na početku kalendarskog perioda
  	]],
  	["si"] = [[
දත්ත ලක්ෂ්‍ය ස්ථාවර දින දර්ශන කාලාන්තරවලට (දින, සති, මාස හෝ වසර) එකතු කරයි. සෑම කාලාන්තරයකටම එහි සීමාවන් තුළ වැටෙන සියලු දත්ත ලක්ෂ්‍ය අඩංගු එක් එකතු කළ අගයක් නිපදවයි.

උදාහරණයක් ලෙස, දෛනික කාලාන්තරයක් සහ සාමාන්‍ය එකතු කිරීමක් සමඟ, සෑම දින දර්ශන දිනකම සියලු මිනුම් එම දිනේ සාමාන්‍යය නිරූපණය කරන එක් අගයකට ඒකාබද්ධ වේ.

**වින්‍යාස විකල්ප:**

- **කාලාන්තරය**: එකතු කළ යුතු දින දර්ශන කාලාන්තරය (දින, සති, මාස හෝ වසර)

- **එකතු කිරීම**: සෑම කාලාන්තරයකම අගයන් මත සිදු කළ යුතු ක්‍රියාව:
  - අවමය: අවම අගය
  - උපරිමය: උපරිම අගය
  - සාමාන්‍යය: සියලු අගයන්ගේ මධ්‍යන්‍යය
  - එකතුව: සියලු අගයන්ගේ එකතුව
  - ගණන: දත්ත ලක්ෂ්‍ය ගණන
  - විචලතාව: සංඛ්‍යාන විචලතාව
  - සම්මත අපගමනය: සංඛ්‍යාන සම්මත අපගමනය

- **ස්ථානගත කිරීම**: එක් එක් ප්‍රතිදාන දත්ත ලක්ෂ්‍යය කාල කවුළුව තුළ ස්ථානගත කළ යුතු ස්ථානය:
  - කවුළු අවසානය: දින දර්ශන කාලාන්තරයේ අවසානයේ (පෙරනිමිය)
  - කවුළු මධ්‍ය ලක්ෂ්‍යය: දින දර්ශන කාලාන්තරයේ කාලමය මධ්‍යයේ
  - කවුළු ආරම්භය: දින දර්ශන කාලාන්තරයේ ආරම්භයේ
  	]],
  	["sk"] = [[
Agreguje údajové body do pevných kalendárnych období (dni, týždne, mesiace alebo roky). Každé obdobie vytvorí jednu agregovanú hodnotu obsahujúcu všetky údajové body spadajúce do jeho hraníc.

Napríklad pri dennom období a agregácii priemerom sa všetky merania z každého kalendárneho dňa spoja do jednej hodnoty predstavujúcej priemer za daný deň.

**Možnosti konfigurácie:**

- **Obdobie**: Kalendárne obdobie, podľa ktorého sa má agregovať (dni, týždne, mesiace alebo roky)

- **Agregácia**: Operácia vykonaná s hodnotami v každom období:
  - Minimum: Najnižšia hodnota
  - Maximum: Najvyššia hodnota
  - Priemer: Priemer všetkých hodnôt
  - Súčet: Súčet všetkých hodnôt
  - Počet: Počet údajových bodov
  - Variancia: Štatistická variancia
  - Smerodajná odchýlka: Štatistická smerodajná odchýlka

- **Umiestnenie**: Kam v časovom okne umiestniť každý výstupný údajový bod:
  - Koniec okna: Na koniec kalendárneho obdobia (predvolené)
  - Stred okna: Do časového stredu kalendárneho obdobia
  - Začiatok okna: Na začiatok kalendárneho obdobia
  	]],
  	["sl"] = [[
Združi podatkovne točke v fiksna koledarska obdobja (dni, tedne, mesece ali leta). Vsako obdobje ustvari eno združeno vrednost, ki vsebuje vse podatkovne točke znotraj meja tega obdobja.

Če je na primer obdobje dnevno in je združevanje povprečje, se vse meritve posameznega koledarskega dne združijo v eno vrednost, ki predstavlja povprečje za ta dan.

**Možnosti konfiguracije:**

- **Obdobje**: Koledarsko obdobje za združevanje (dnevi, tedni, meseci ali leta)

- **Združevanje**: Operacija, ki se izvede nad vrednostmi v vsakem obdobju:
  - Min: Najmanjša vrednost
  - Max: Največja vrednost
  - Povprečje: Srednja vrednost vseh vrednosti
  - Vsota: Skupna vrednost vseh vrednosti
  - Število: Število podatkovnih točk
  - Varianca: Statistična varianca
  - Standardni odklon: Statistični standardni odklon

- **Postavitev**: Kam v časovno okno se postavi vsaka izhodna podatkovna točka:
  - Konec okna: Na konec koledarskega obdobja (privzeto)
  - Sredina okna: Na časovno sredino koledarskega obdobja
  - Začetek okna: Na začetek koledarskega obdobja
  	]],
  	["es"] = [[
Agrega los puntos de datos en períodos naturales fijos (días, semanas, meses o años). Cada período produce un valor agregado que contiene todos los puntos de datos que se encuentran dentro de sus límites.

Por ejemplo, con un período diario y una agregación de promedio, todas las mediciones de cada día natural se combinan en un único valor que representa el promedio de ese día.

**Opciones de configuración:**

- **Período**: El período natural por el que se agregará (días, semanas, meses o años)

- **Agregación**: La operación que se realizará sobre los valores de cada período:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación estándar: Desviación estándar estadística

- **Ubicación**: Dónde colocar cada punto de datos de salida en la ventana temporal:
  - Fin de la ventana: Al final del período natural (predeterminado)
  - Punto medio de la ventana: En el centro temporal del período natural
  - Inicio de la ventana: Al inicio del período natural
  	]],
  	["sw"] = [[
Hujumlisha nukta za data katika vipindi maalum vya kalenda (siku, wiki, miezi au miaka). Kila kipindi huzalisha thamani moja iliyojumlishwa iliyo na nukta zote za data zinazoangukia ndani ya mipaka ya kipindi hicho.

Kwa mfano, kwa kipindi cha kila siku na ujumuishaji wa wastani, vipimo vyote vya kila siku ya kalenda huunganishwa kuwa thamani moja inayowakilisha wastani wa siku hiyo.

**Chaguo za Usanidi:**

- **Kipindi**: Kipindi cha kalenda cha kujumlishia (siku, wiki, miezi au miaka)

- **Ujumlishaji**: Operesheni ya kufanya kwenye thamani za kila kipindi:
  - Min: Thamani ya chini kabisa
  - Max: Thamani ya juu kabisa
  - Average: Wastani wa thamani zote
  - Sum: Jumla ya thamani zote
  - Count: Idadi ya nukta za data
  - Variance: Tofauti ya kitakwimu
  - Standard Deviation: Mkengeuko wa kawaida wa kitakwimu

- **Mahali**: Mahali pa kuweka kila nukta ya data ya matokeo ndani ya dirisha la muda:
  - Mwisho wa Dirisha: Mwisho wa kipindi cha kalenda (chaguo-msingi)
  - Katikati ya Dirisha: Katikati ya muda ya kipindi cha kalenda
  - Mwanzo wa Dirisha: Mwanzo wa kipindi cha kalenda
  	]],
  	["sv"] = [[
Sammanställer datapunkter i fasta kalenderperioder (dagar, veckor, månader eller år). Varje period ger ett sammanställt värde som innehåller alla datapunkter som faller inom periodens gränser.

Till exempel: med en daglig period och genomsnittlig sammanställning kombineras alla mätningar från varje kalenderdag till ett enda värde som representerar genomsnittet för den dagen.

**Konfigurationsalternativ:**

- **Period**: Kalenderperioden som sammanställningen ska grupperas efter (dagar, veckor, månader eller år)

- **Sammanställning**: Åtgärden som utförs på värdena i varje period:
  - Min: Minimivärde
  - Max: Maximivärde
  - Genomsnitt: Medelvärdet av alla värden
  - Summa: Summan av alla värden
  - Antal: Antalet datapunkter
  - Varians: Statistisk varians
  - Standardavvikelse: Statistisk standardavvikelse

- **Placering**: Var i tidsfönstret varje utgående datapunkt placeras:
  - Fönstrets slut: Vid kalenderperiodens slut (standard)
  - Fönstrets mittpunkt: Vid kalenderperiodens tidsmässiga mittpunkt
  - Fönstrets början: Vid kalenderperiodens början
  	]],
  	["ta"] = [[
தரவுப் புள்ளிகளை நிலையான நாட்காட்டிக் காலப்பகுதிகளாக (நாட்கள், வாரங்கள், மாதங்கள் அல்லது ஆண்டுகள்) தொகுக்கிறது. ஒவ்வொரு காலப்பகுதியும் அதன் எல்லைகளுக்குள் வரும் அனைத்து தரவுப் புள்ளிகளையும் கொண்ட ஒரு தொகுக்கப்பட்ட மதிப்பை உருவாக்கும்.

எடுத்துக்காட்டாக, தினசரி காலப்பகுதியும் சராசரி தொகுத்தலும் பயன்படுத்தப்பட்டால், ஒவ்வொரு நாட்காட்டி நாளின் அனைத்து அளவீடுகளும் அந்த நாளின் சராசரியைப் பிரதிபலிக்கும் ஒரே மதிப்பாக இணைக்கப்படும்.

**உள்ளமைவு விருப்பங்கள்:**

- **காலப்பகுதி**: தொகுக்க வேண்டிய நாட்காட்டிக் காலப்பகுதி (நாட்கள், வாரங்கள், மாதங்கள் அல்லது ஆண்டுகள்)

- **தொகுத்தல்**: ஒவ்வொரு காலப்பகுதியிலும் உள்ள மதிப்புகளில் செய்யப்படும் செயல்பாடு:
  - Min: குறைந்தபட்ச மதிப்பு
  - Max: அதிகபட்ச மதிப்பு
  - Average: அனைத்து மதிப்புகளின் சராசரி
  - Sum: அனைத்து மதிப்புகளின் மொத்தம்
  - Count: தரவுப் புள்ளிகளின் எண்ணிக்கை
  - Variance: புள்ளியியல் மாறுபாடு
  - Standard Deviation: புள்ளியியல் திட்ட விலகல்

- **நிலைநிறுத்தல்**: ஒவ்வொரு வெளியீட்டுத் தரவுப் புள்ளியையும் நேரச் சாளரத்தில் வைக்கும் இடம்:
  - Window End: நாட்காட்டிக் காலப்பகுதியின் முடிவில் (இயல்புநிலை)
  - Window Midpoint: நாட்காட்டிக் காலப்பகுதியின் கால மையத்தில்
  - Window Start: நாட்காட்டிக் காலப்பகுதியின் தொடக்கத்தில்
  	]],
  	["te"] = [[
డేటా పాయింట్లను స్థిర క్యాలెండర్ కాలవ్యవధులుగా (రోజులు, వారాలు, నెలలు లేదా సంవత్సరాలు) సమగ్రపరుస్తుంది. ప్రతి కాలవ్యవధి తన సరిహద్దుల్లోకి వచ్చే అన్ని డేటా పాయింట్లను కలిగిన ఒక సమగ్ర విలువను ఉత్పత్తి చేస్తుంది.

ఉదాహరణకు, రోజువారీ కాలవ్యవధి మరియు సగటు సమగ్రణతో, ప్రతి క్యాలెండర్ రోజులోని అన్ని కొలతలు ఆ రోజు సగటును సూచించే ఒకే విలువగా కలపబడతాయి.

**కాన్ఫిగరేషన్ ఎంపికలు:**

- **కాలవ్యవధి**: సమగ్రపరచాల్సిన క్యాలెండర్ కాలవ్యవధి (రోజులు, వారాలు, నెలలు లేదా సంవత్సరాలు)

- **సమగ్రణ**: ప్రతి కాలవ్యవధిలోని విలువలపై నిర్వహించే ఆపరేషన్:
  - Min: కనిష్ఠ విలువ
  - Max: గరిష్ఠ విలువ
  - Average: అన్ని విలువల సగటు
  - Sum: అన్ని విలువల మొత్తం
  - Count: డేటా పాయింట్ల సంఖ్య
  - Variance: గణాంక వ్యత్యాసం
  - Standard Deviation: గణాంక ప్రామాణిక విచలనం

- **స్థానం**: ప్రతి అవుట్‌పుట్ డేటా పాయింట్‌ను సమయ విండోలో ఎక్కడ ఉంచాలి:
  - Window End: క్యాలెండర్ కాలవ్యవధి ముగింపులో (డిఫాల్ట్)
  - Window Midpoint: క్యాలెండర్ కాలవ్యవధి కాలకేంద్రంలో
  - Window Start: క్యాలెండర్ కాలవ్యవధి ప్రారంభంలో
  	]],
  	["th"] = [[
รวมจุดข้อมูลเป็นช่วงเวลาปฏิทินคงที่ (วัน สัปดาห์ เดือน หรือปี) แต่ละช่วงเวลาจะสร้างค่ารวมหนึ่งค่าซึ่งมีจุดข้อมูลทั้งหมดที่อยู่ภายในขอบเขตของช่วงเวลานั้น

ตัวอย่างเช่น เมื่อใช้ช่วงเวลารายวันและการรวมแบบค่าเฉลี่ย การวัดทั้งหมดของแต่ละวันตามปฏิทินจะถูกรวมเป็นค่าเดียวที่แสดงค่าเฉลี่ยของวันนั้น

**ตัวเลือกการกำหนดค่า:**

- **ช่วงเวลา**: ช่วงเวลาปฏิทินที่ใช้รวมค่า (วัน สัปดาห์ เดือน หรือปี)

- **การรวมค่า**: การดำเนินการกับค่าในแต่ละช่วงเวลา:
  - Min: ค่าต่ำสุด
  - Max: ค่าสูงสุด
  - Average: ค่าเฉลี่ยของค่าทั้งหมด
  - Sum: ผลรวมของค่าทั้งหมด
  - Count: จำนวนจุดข้อมูล
  - Variance: ความแปรปรวนทางสถิติ
  - Standard Deviation: ส่วนเบี่ยงเบนมาตรฐานทางสถิติ

- **ตำแหน่ง**: ตำแหน่งในช่วงเวลาที่จะวางจุดข้อมูลผลลัพธ์แต่ละจุด:
  - สิ้นสุดช่วงเวลา: ณ จุดสิ้นสุดของช่วงเวลาปฏิทิน (ค่าเริ่มต้น)
  - จุดกึ่งกลางช่วงเวลา: ณ กึ่งกลางตามเวลาของช่วงเวลาปฏิทิน
  - เริ่มต้นช่วงเวลา: ณ จุดเริ่มต้นของช่วงเวลาปฏิทิน
  	]],
  	["tr"] = [[
Veri noktalarını sabit takvim dönemleri (günler, haftalar, aylar veya yıllar) halinde toplar. Her dönem, sınırları içine düşen tüm veri noktalarını içeren tek bir toplu değer üretir.

Örneğin günlük dönem ve ortalama toplama ile her takvim günündeki tüm ölçümler, o günün ortalamasını temsil eden tek bir değerde birleştirilir.

**Yapılandırma Seçenekleri:**

- **Dönem**: Toplamanın yapılacağı takvim dönemi (günler, haftalar, aylar veya yıllar)

- **Toplama**: Her dönemdeki değerlere uygulanacak işlem:
  - Min: Minimum değer
  - Max: Maksimum değer
  - Average: Tüm değerlerin ortalaması
  - Sum: Tüm değerlerin toplamı
  - Count: Veri noktası sayısı
  - Variance: İstatistiksel varyans
  - Standard Deviation: İstatistiksel standart sapma

- **Yerleştirme**: Her çıktı veri noktasının zaman penceresinde yerleştirileceği konum:
  - Pencere Sonu: Takvim döneminin sonunda (varsayılan)
  - Pencere Ortası: Takvim döneminin zamansal merkezinde
  - Pencere Başlangıcı: Takvim döneminin başlangıcında
  	]],
  	["uk"] = [[
Об’єднує точки даних у фіксовані календарні періоди (дні, тижні, місяці або роки). Кожен період створює одне агреговане значення, що містить усі точки даних у межах цього періоду.

Наприклад, за добового періоду й агрегації середнього всі вимірювання за кожен календарний день об’єднуються в одне значення, що представляє середнє за цей день.

**Параметри конфігурації:**

- **Період**: Календарний період для агрегації (дні, тижні, місяці або роки)

- **Агрегація**: Операція, яку потрібно виконати над значеннями в кожному періоді:
  - Мінімум: Мінімальне значення
  - Максимум: Максимальне значення
  - Середнє: Середнє всіх значень
  - Сума: Сума всіх значень
  - Кількість: Кількість точок даних
  - Дисперсія: Статистична дисперсія
  - Стандартне відхилення: Статистичне стандартне відхилення

- **Розміщення**: Де в часовому вікні розміщувати кожну вихідну точку даних:
  - Кінець вікна: Наприкінці календарного періоду (типово)
  - Середина вікна: У часовому центрі календарного періоду
  - Початок вікна: На початку календарного періоду
  	]],
  	["vi"] = [[
Tổng hợp các điểm dữ liệu vào những khoảng thời gian lịch cố định (ngày, tuần, tháng hoặc năm). Mỗi khoảng thời gian tạo ra một giá trị tổng hợp chứa tất cả điểm dữ liệu nằm trong ranh giới của khoảng đó.

Ví dụ: với chu kỳ ngày và phép tổng hợp trung bình, tất cả phép đo trong mỗi ngày lịch được kết hợp thành một giá trị duy nhất đại diện cho giá trị trung bình của ngày đó.

**Tùy chọn cấu hình:**

- **Chu kỳ**: Khoảng thời gian lịch dùng để tổng hợp (ngày, tuần, tháng hoặc năm)

- **Tổng hợp**: Phép toán thực hiện trên các giá trị trong mỗi khoảng thời gian:
  - Min: Giá trị nhỏ nhất
  - Max: Giá trị lớn nhất
  - Average: Trung bình của tất cả giá trị
  - Sum: Tổng của tất cả giá trị
  - Count: Số lượng điểm dữ liệu
  - Variance: Phương sai thống kê
  - Standard Deviation: Độ lệch chuẩn thống kê

- **Vị trí**: Vị trí đặt mỗi điểm dữ liệu đầu ra trong cửa sổ thời gian:
  - Cuối cửa sổ: Ở cuối khoảng thời gian lịch (mặc định)
  - Trung điểm cửa sổ: Tại trung tâm thời gian của khoảng thời gian lịch
  - Đầu cửa sổ: Ở đầu khoảng thời gian lịch
  	]],
  },
  config = {
    enum {
      id = "period",
      name = "_period",
      options = { "_days", "_weeks", "_months", "_years" },
      default = "_days"
    },
    enum {
      id = "aggregation_type",
      name = "_aggregation",
      options = { "_min", "_max", "_average", "_sum", "_count", "_variance", "_standard_deviation" },
      default = "_average"
    },
    enum {
      id = "placement",
      name = "_placement",
      options = { "_window_end", "_window_midpoint", "_window_start" },
      default = "_window_end"
    },
  },
  generator = function(source, config)
    local agg_factory = get_aggregator_factory(config)
    local period = get_period(config)
    local carry = source.dp()

    local current_window_start
    local current_window_end

    if carry ~= nil then
      current_window_end = core.time(core.get_end_of_period(period, carry))
      current_window_start = core.shift(current_window_end, period, -1)
    end

    return function()
      if carry == nil then
        return nil
      end

      if carry.timestamp < current_window_start.timestamp then
        -- No data points in this window, return empty aggregation
        local placement_time = get_placement_time(config, current_window_start, current_window_end)
        current_window_end = current_window_start
        current_window_start = core.shift(current_window_start, period, -1)
        return {
          timestamp = placement_time.timestamp,
          offset = placement_time.offset,
          value = 0,
        }
      end

      local aggregator = agg_factory()
      aggregator:push(carry)

      local window_data_points = source.dpafter(current_window_start)

      for _, dp in ipairs(window_data_points) do
        aggregator:push(dp)
      end

      local placement_time = get_placement_time(config, current_window_start, current_window_end)

      current_window_end = current_window_start
      current_window_start = core.shift(current_window_start, period, -1)

      carry = source.dp()

      local aggregate = aggregator:run()

      return {
        timestamp = placement_time.timestamp,
        offset = placement_time.offset,
        value = aggregate.value,
        label = aggregate.label,
        note = aggregate.note
      }
    end
  end
}
