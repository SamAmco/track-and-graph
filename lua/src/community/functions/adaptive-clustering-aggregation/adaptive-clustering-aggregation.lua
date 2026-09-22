local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint

local get_aggregator_factory = function(config)
  local aggregation = require("tng.aggregation")
  local type = config.aggregation_type or error("aggregation_type required")
  local aggregator

  if type == "_min" then
    aggregator = aggregation.running_min_aggregator
  elseif type == "_max" then
    aggregator = aggregation.running_max_aggregator
  elseif type == "_average" then
    aggregator = aggregation.avg_aggregator
  elseif type == "_sum" then
    aggregator = aggregation.sum_aggregator
  elseif type == "_variance" then
    aggregator = aggregation.variance_aggregator
  elseif type == "_standard_deviation" then
    aggregator = aggregation.stdev_aggregator
  elseif type == "_count" then
    aggregator = aggregation.count_aggregator
  else
    error("Unknown aggregation_type " .. tostring(type))
  end

  return aggregator
end

local get_threshold = function(config)
  if type(config.threshold) ~= "string" then
    error("config.threshold is not a string")
  end

  if config.threshold == "_seconds" then
    return core.DURATION.SECOND
  elseif config.threshold == "_minutes" then
    return core.DURATION.MINUTE
  elseif config.threshold == "_hours" then
    return core.DURATION.HOUR
  elseif config.threshold == "_days" then
    return core.PERIOD.DAY
  elseif config.threshold == "_weeks" then
    return core.PERIOD.WEEK
  elseif config.threshold == "_months" then
    return core.PERIOD.MONTH
  elseif config.threshold == "_years" then
    return core.PERIOD.YEAR
  else
    error("Unknown threshold: " .. tostring(config.threshold))
  end
end

return {
  id = "adaptve-clustering-aggregation",
  version = "3.0.2",
  deprecated = 4,
  inputCount = 1,
  title = {
  	["en"] = "Adaptive Clustering",
  	["af"] = "Aanpasbare Groepering",
  	["sq"] = "Klasifikim adaptiv",
  	["am"] = "ተለዋዋጭ ማሰባሰብ",
  	["hy"] = "Հարմարվող խմբավորում",
  	["az"] = "Adaptiv klasterləşdirmə",
  	["bn"] = "অভিযোজিত ক্লাস্টারিং",
  	["eu"] = "Multzokatze moldakorra",
  	["be"] = "Адаптыўная кластарызацыя",
  	["bg"] = "Адаптивно групиране",
  	["my"] = "လိုက်လျောညီထွေ အစုဖွဲ့ခြင်း",
  	["ca"] = "Agrupació adaptativa",
  	["zh-Hans"] = "自适应聚类",
  	["zh-Hant"] = "自適應分群",
  	["hr"] = "Prilagodljivo grupiranje",
  	["cs"] = "Adaptivní shlukování",
  	["da"] = "Adaptiv klyngedannelse",
  	["nl"] = "Adaptieve clustering",
  	["et"] = "Kohanduv klasterdamine",
  	["fil"] = "Adaptive na Pagkumpol",
  	["fi"] = "Mukautuva ryhmittely",
  	["fr"] = "Regroupement adaptatif",
  	["gl"] = "Agrupación adaptativa",
  	["ka"] = "ადაპტური კლასტერული აგრეგაცია",
  	["de"] = "Adaptive Clusterbildung",
  	["el"] = "Προσαρμοστική ομαδοποίηση",
  	["gu"] = "અનુકૂલનશીલ ક્લસ્ટરિંગ",
  	["hi"] = "अनुकूली क्लस्टरिंग",
  	["hu"] = "Adaptív klaszterezés",
  	["is"] = "Aðlögunarhæf klasasöfnun",
  	["id"] = "Pengelompokan Adaptif",
  	["it"] = "Raggruppamento adattivo",
  	["ja"] = "適応型クラスタリング",
  	["kn"] = "ಹೊಂದಿಕೊಳ್ಳುವ ಕ್ಲಸ್ಟರಿಂಗ್",
  	["kk"] = "Бейімделмелі кластерлеу",
  	["km"] = "ការដាក់ជាក្រុមសម្របខ្លួន",
  	["ko"] = "적응형 클러스터링",
  	["ky"] = "Ыңгайлашма кластерлөө",
  	["lo"] = "ການຈັດກຸ່ມແບບປັບຕົວ",
  	["lv"] = "Adaptīvā grupēšana",
  	["lt"] = "Adaptyvus grupavimas",
  	["mk"] = "Адаптивно групирање",
  	["ms"] = "Pengelompokan Adaptif",
  	["ml"] = "അനുസരണീയ ക്ലസ്റ്ററിംഗ്",
  	["mr"] = "अनुकूली क्लस्टरिंग",
  	["mn"] = "Дасан зохицох бүлэглэлт",
  	["ne"] = "अनुकूल क्लस्टरिङ",
  	["no"] = "Adaptiv klynging",
  	["pl"] = "Adaptacyjne grupowanie",
  	["pt"] = "Agrupamento adaptativo",
  	["pa"] = "ਅਨੁਕੂਲ ਕਲੱਸਟਰਿੰਗ",
  	["ro"] = "Grupare adaptivă",
  	["rm"] = "Clustering adaptiv",
  	["ru"] = "Адаптивная кластеризация",
  	["sr"] = "Adaptivno grupisanje",
  	["si"] = "අනුවර්තී පොකුරුකරණය",
  	["sk"] = "Adaptívne zoskupovanie",
  	["sl"] = "Prilagodljivo združevanje v skupine",
  	["es"] = "Agrupación adaptativa",
  	["sw"] = "Uundaji wa Makundi Unaobadilika",
  	["sv"] = "Adaptiv klustring",
  	["ta"] = "தகவமைப்பு குழுவாக்கம்",
  	["te"] = "అనుకూల క్లస్టరింగ్",
  	["th"] = "การจัดกลุ่มแบบปรับตามข้อมูล",
  	["tr"] = "Uyarlanabilir Kümeleme",
  	["uk"] = "Адаптивне кластеризування",
  	["vi"] = "Phân cụm thích ứng",
  },
  categories = { "_aggregation" },
  description = {
  	["en"] = [[
Groups nearby data points into clusters and aggregates each cluster into a single value. The function groups any sequence of data points with less than the given time threshold between each data point.

For example, with a 1-hour threshold, if you have several data points at 9:00am, 9:30am, and 10:15am, they will be grouped into a single cluster. If the next data point was at 11:16am it would start a new cluster.

**Configuration Options:**

- **Aggregation**: The operation to perform on values in each cluster:
  - Min: Minimum value
  - Max: Maximum value
  - Average: Mean of all values
  - Sum: Total of all values
  - Count: Number of data points
  - Variance: Statistical variance
  - Standard Deviation: Statistical standard deviation

- **Threshold Units**: The time unit for clustering proximity (seconds, minutes, hours, days, weeks, months, or years)

- **Multiplier**: How many threshold size units define the clustering threshold (e.g., multiplier of 2 with threshold units "hours" = 2-hour clustering threshold)
  	]],
  	["af"] = [[
Groepeer nabygeleë datapunte in klusters en kombineer elke kluster tot een waarde. Die funksie groepeer enige reeks datapunte met minder as die gegewe tydsdrempel tussen opeenvolgende datapunte.

Byvoorbeeld, met ’n drempel van 1 uur sal verskeie datapunte om 09:00, 09:30 en 10:15 in een kluster gegroepeer word. As die volgende datapunt om 11:16 was, sou dit ’n nuwe kluster begin.

**Konfigurasie-opsies:**

- **Aggregasie**: Die bewerking wat op waardes in elke kluster uitgevoer word:
  - Min: Minimumwaarde
  - Maks: Maksimumwaarde
  - Gemiddeld: Gemiddelde van alle waardes
  - Som: Totaal van alle waardes
  - Tel: Aantal datapunte
  - Variansie: Statistiese variansie
  - Standaardafwyking: Statistiese standaardafwyking

- **Drempeleenhede**: Die tydeenheid vir nabyheid tydens groepering (sekondes, minute, ure, dae, weke, maande of jare)

- **Vermenigvuldiger**: Hoeveel drempelgrootte-eenhede die groeperingsdrempel bepaal (bv. ’n vermenigvuldiger van 2 met drempeleenhede "ure" = ’n groeperingsdrempel van 2 uur)
  	]],
  	["sq"] = [[
Grupon pikat e afërta të të dhënave në klasterë dhe grumbullon çdo klaster në një vlerë të vetme. Funksioni grupon çdo varg pikash të të dhënave që kanë më pak se pragu kohor i dhënë midis pikave.

Për shembull, me një prag prej 1 ore, nëse keni disa pika të dhënash në orën 9:00, 9:30 dhe 10:15, ato do të grupohen në një klaster të vetëm. Nëse pika tjetër do të ishte në 11:16, ajo do të fillonte një klaster të ri.

**Opsionet e konfigurimit:**

- **Grumbullimi**: Veprimi që kryhet mbi vlerat në çdo klaster:
  - Min: Vlera minimale
  - Max: Vlera maksimale
  - Mesatarja: Mesatarja e të gjitha vlerave
  - Shuma: Totali i të gjitha vlerave
  - Numërimi: Numri i pikave të të dhënave
  - Varianca: Varianca statistikore
  - Devijimi standard: Devijimi standard statistikor

- **Njësitë e pragut**: Njësia kohore për afërsinë e klasterizimit (sekonda, minuta, orë, ditë, javë, muaj ose vite)

- **Shumëzuesi**: Sa njësi të madhësisë së pragut përcaktojnë pragun e klasterizimit (p.sh., shumëzuesi 2 me njësinë e pragut "orë" = prag klasterizimi 2-orësh)
  	]],
  	["am"] = [[
ቅርብ የሆኑ የውሂብ ነጥቦችን ወደ ስብስቦች ያደራጃል፣ እና እያንዳንዱን ስብስብ ወደ አንድ ነጠላ ዋጋ ያጠቃልላል። ተግባሩ በእያንዳንዱ የውሂብ ነጥብ መካከል ከተሰጠው የጊዜ ገደብ ያነሰ ልዩነት ያላቸውን ተከታታይ የውሂብ ነጥቦች ያሰባስባል።

ለምሳሌ፣ የ1 ሰዓት ገደብ ካለ፣ በ9:00am፣ 9:30am እና 10:15am ላይ ያሉ በርካታ የውሂብ ነጥቦች ወደ አንድ ስብስብ ይደራጃሉ። ቀጣዩ የውሂብ ነጥብ በ11:16am ላይ ቢሆን አዲስ ስብስብ ይጀምራል።

**የማዋቀሪያ አማራጮች፦**

- **ማጠቃለያ**፦ በእያንዳንዱ ስብስብ ውስጥ ባሉ ዋጋዎች ላይ የሚፈጸም ክዋኔ፦
  - Min፦ ዝቅተኛው ዋጋ
  - Max፦ ከፍተኛው ዋጋ
  - Average፦ የሁሉም ዋጋዎች አማካይ
  - Sum፦ የሁሉም ዋጋዎች ድምር
  - Count፦ የውሂብ ነጥቦች ብዛት
  - Variance፦ ስታቲስቲካዊ ልዩነት
  - Standard Deviation፦ ስታቲስቲካዊ መደበኛ ልዩነት

- **የገደብ አሃዶች**፦ ለስብስብ ቅርበት የሚጠቀሙበት የጊዜ አሃድ (ሰከንዶች፣ ደቂቃዎች፣ ሰዓቶች፣ ቀናት፣ ሳምንታት፣ ወራት ወይም ዓመታት)

- **ማባዣ**፦ የስብስብ ገደቡን ለመወሰን የሚያስፈልጉ የገደብ መጠን አሃዶች ብዛት (ለምሳሌ፣ ማባዣው 2 እና የገደብ አሃዱ "hours" ከሆነ = የ2 ሰዓት የስብስብ ገደብ)
  	]],
  	["hy"] = [[
Մոտ տվյալակետերը խմբավորում է կլաստերներում և յուրաքանչյուր կլաստերը միավորում մեկ արժեքի մեջ։ Ֆունկցիան խմբավորում է տվյալակետերի ցանկացած հաջորդականություն, եթե յուրաքանչյուր երկու հաջորդական տվյալակետի միջև ժամանակային տարբերությունը նշված շեմից փոքր է։

Օրինակ՝ 1 ժամ շեմի դեպքում 09:00-ին, 09:30-ին և 10:15-ին գրանցված տվյալակետերը կմիավորվեն մեկ կլաստերում։ Եթե հաջորդ տվյալակետը լինի 11:16-ին, այն կսկսի նոր կլաստեր։

**Կազմաձևման ընտրանքներ․**

- **Միավորում**․ յուրաքանչյուր կլաստերի արժեքների նկատմամբ կատարվող գործողությունը՝
  - Min՝ նվազագույն արժեք
  - Max՝ առավելագույն արժեք
  - Average՝ բոլոր արժեքների միջին
  - Sum՝ բոլոր արժեքների գումար
  - Count՝ տվյալակետերի քանակ
  - Variance՝ վիճակագրական դիսպերսիա
  - Standard Deviation՝ վիճակագրական ստանդարտ շեղում

- **Շեմի միավորներ**․ մոտիկությունը որոշող ժամանակի միավորը (վայրկյաններ, րոպեներ, ժամեր, օրեր, շաբաթներ, ամիսներ կամ տարիներ)

- **Բազմապատկիչ**․ շեմի չափի քանի միավորն է սահմանում խմբավորման շեմը (օրինակ՝ «ժամեր» միավորով 2 բազմապատկիչը նշանակում է 2-ժամյա խմբավորման շեմ)
  	]],
  	["az"] = [[
Yaxın məlumat nöqtələrini klasterlərdə qruplaşdırır və hər klasteri tək qiymətə birləşdirir. Funksiya hər iki ardıcıl məlumat nöqtəsi arasındakı müddət verilən zaman həddindən az olduqda onları qruplaşdırır.

Məsələn, 1 saatlıq hədd ilə saat 9:00, 9:30 və 10:15-dəki məlumat nöqtələri bir klasterdə qruplaşdırılacaq. Növbəti məlumat nöqtəsi saat 11:16-da olsaydı, yeni klaster başlayardı.

**Konfiqurasiya seçimləri:**

- **Birləşdirmə**: Hər klasterdəki qiymətlər üzərində icra ediləcək əməliyyat:
  - Min: Minimum qiymət
  - Max: Maksimum qiymət
  - Average: Bütün qiymətlərin ortalaması
  - Sum: Bütün qiymətlərin cəmi
  - Count: Məlumat nöqtələrinin sayı
  - Variance: Statistik dispersiya
  - Standard Deviation: Statistik standart kənarlaşma

- **Hədd vahidləri**: Klasterləşdirmə yaxınlığı üçün zaman vahidi (saniyə, dəqiqə, saat, gün, həftə, ay və ya il)

- **Vurğu**: Klasterləşdirmə həddini müəyyən edən hədd vahidlərinin sayı (məsələn, hədd vahidi "hours" və vurğu 2 olduqda = 2 saatlıq klasterləşdirmə həddi)
  	]],
  	["bn"] = [[
কাছাকাছি ডেটা পয়েন্টগুলোকে ক্লাস্টারে групп করে এবং প্রতিটি ক্লাস্টারকে একটি একক মানে একত্রিত করে। প্রতিটি ডেটা পয়েন্টের মধ্যে প্রদত্ত সময়সীমার চেয়ে কম ব্যবধান থাকা যেকোনো ধারাবাহিক ডেটা পয়েন্টকে ফাংশনটি একই ক্লাস্টারে রাখে।

উদাহরণস্বরূপ, ১ ঘণ্টার সীমা হলে ৯:০০, ৯:৩০ এবং ১০:১৫-এ থাকা একাধিক ডেটা পয়েন্ট একটি ক্লাস্টারে যুক্ত হবে। পরের ডেটা পয়েন্টটি ১১:১৬-এ হলে সেটি নতুন ক্লাস্টার শুরু করবে।

**কনফিগারেশন অপশন:**

- **একত্রীকরণ**: প্রতিটি ক্লাস্টারের মানের ওপর সম্পাদিত অপারেশন:
  - Min: সর্বনিম্ন মান
  - Max: সর্বোচ্চ মান
  - Average: সব মানের গড়
  - Sum: সব মানের মোট
  - Count: ডেটা পয়েন্টের সংখ্যা
  - Variance: পরিসংখ্যানগত বিচরণ
  - Standard Deviation: পরিসংখ্যানগত মান বিচ্যুতি

- **সীমার একক**: ক্লাস্টারের নৈকট্য নির্ধারণের সময়ের একক (সেকেন্ড, মিনিট, ঘণ্টা, দিন, সপ্তাহ, মাস বা বছর)

- **গুণক**: কতটি সীমা-আকারের একক ক্লাস্টারিং সীমা নির্ধারণ করবে (যেমন, সীমার একক "ঘণ্টা" এবং গুণক ২ হলে = ২ ঘণ্টার ক্লাস্টারিং সীমা)
  	]],
  	["eu"] = [[
Hurbil dauden datu-puntuak multzotan biltzen ditu, eta multzo bakoitza balio bakar batean laburbiltzen du. Funtzioak datu-puntuen arteko denbora-tartea emandako atalasea baino txikiagoa den edozein datu-puntu-sekuentzia multzokatzen du.

Adibidez, ordubeteko atalasearekin, 09:00etan, 09:30ean eta 10:15ean hainbat datu-puntu badituzu, multzo bakarrean bilduko dira. Hurrengo datu-puntua 11:16an balego, multzo berri bat hasiko luke.

**Konfigurazio-aukerak:**

- **Agregazioa**: Multzo bakoitzeko balioekin egin beharreko eragiketa:
  - Min: Gutxieneko balioa
  - Max: Gehieneko balioa
  - Batez bestekoa: Balio guztien batez bestekoa
  - Batura: Balio guztien batura
  - Zenbaketa: Datu-puntuen kopurua
  - Bariantza: Bariantza estatistikoa
  - Desbideratze estandarra: Desbideratze estandarra estatistikoa

- **Atalasearen unitateak**: Hurbiltasuna multzokatzeko denbora-unitatea (segundoak, minutuak, orduak, egunak, asteak, hilabeteak edo urteak)

- **Biderkatzailea**: Atalasearen tamainako zenbat unitatek definitzen duten multzokatze-atalasea (adib., 2ko biderkatzailea eta "orduak" atalase-unitate gisa = 2 orduko multzokatze-atalasea)
  	]],
  	["be"] = [[
Групуе блізкія кропкі даных у кластары і аб’ядноўвае кожны кластар у адно значэнне. Функцыя групуе любую паслядоўнасць кропак даных, паміж якімі інтэрвал меншы за зададзены часавы парог.

Напрыклад, пры парозе ў 1 гадзіну некалькі кропак даных у 09:00, 09:30 і 10:15 будуць згрупаваныя ў адзін кластар. Калі наступная кропка будзе ў 11:16, яна пачне новы кластар.

**Параметры канфігурацыі:**

- **Агрэгацыя**: аперацыя над значэннямі ў кожным кластары:
  - Мін.: мінімальнае значэнне
  - Макс.: максімальнае значэнне
  - Сярэдняе: сярэдняе ўсіх значэнняў
  - Сума: сума ўсіх значэнняў
  - Колькасць: колькасць кропак даных
  - Дысперсія: статыстычная дысперсія
  - Стандартнае адхіленне: статыстычнае стандартнае адхіленне

- **Адзінкі парога**: адзінка часу для блізкасці пры кластарацыі (секунды, хвіліны, гадзіны, дні, тыдні, месяцы або гады)

- **Множнік**: колькасць адзінак памеру парога, якія вызначаюць парог кластарацыі (напрыклад, множнік 2 і адзінкі парога «гадзіны» = парог кластарацыі 2 гадзіны)
  	]],
  	["bg"] = [[
Групира близки точки от данни в клъстери и обобщава всеки клъстер в една стойност. Функцията групира всяка последователност от точки от данни, при която интервалът между всяка две точки е по-малък от зададения времеви праг.

Например при праг от 1 час няколко точки от данни в 9:00, 9:30 и 10:15 ще бъдат групирани в един клъстер. Ако следващата точка е в 11:16, тя ще започне нов клъстер.

**Опции за конфигуриране:**

- **Обобщаване**: Операцията, която се извършва върху стойностите във всеки клъстер:
  - Min: Минимална стойност
  - Max: Максимална стойност
  - Average: Средна стойност на всички стойности
  - Sum: Сбор на всички стойности
  - Count: Брой точки от данни
  - Variance: Статистическа дисперсия
  - Standard Deviation: Статистическо стандартно отклонение

- **Единици на прага**: Времевата единица за близостта при групиране (секунди, минути, часове, дни, седмици, месеци или години)

- **Множител**: Колко единици от размера на прага определят прага за групиране (напр. множител 2 с единици „часове“ = праг за групиране от 2 часа)
  	]],
  	["my"] = [[
နီးစပ်သော ဒေတာမှတ်များကို အစုများအဖြစ် ဖွဲ့စည်းပြီး အစုတစ်ခုစီကို တန်ဖိုးတစ်ခုတည်းအဖြစ် စုစည်းသည်။ ဒေတာမှတ်တစ်ခုနှင့်တစ်ခုကြား အချိန်ကာလသည် သတ်မှတ်ထားသော အချိန်ကန့်သတ်ချက်ထက် နည်းသော ဒေတာမှတ်အစီအစဉ်များကို အစုဖွဲ့သည်။

ဥပမာအားဖြင့် ၁ နာရီကန့်သတ်ချက်ဖြင့် နံနက် ၉:၀၀၊ ၉:၃၀ နှင့် ၁၀:၁၅ တို့တွင် ဒေတာမှတ်များရှိပါက ၎င်းတို့ကို အစုတစ်ခုတည်းအဖြစ် ဖွဲ့မည်။ နောက်ဒေတာမှတ်သည် ၁၁:၁၆ တွင်ရှိပါက အစုအသစ်တစ်ခု စတင်မည်။

**ဖွဲ့စည်းမှု ရွေးချယ်စရာများ:**

- **စုစည်းမှု**: အစုတစ်ခုစီရှိ တန်ဖိုးများအပေါ် လုပ်ဆောင်မည့် လုပ်ဆောင်ချက်:
  - Min: အနည်းဆုံးတန်ဖိုး
  - Max: အများဆုံးတန်ဖိုး
  - Average: တန်ဖိုးအားလုံး၏ ပျမ်းမျှ
  - Sum: တန်ဖိုးအားလုံး၏ စုစုပေါင်း
  - Count: ဒေတာမှတ်အရေအတွက်
  - Variance: စာရင်းအင်း ကွဲလွဲမှု
  - Standard Deviation: စာရင်းအင်း စံသွေဖည်မှု

- **ကန့်သတ်ချက် ယူနစ်များ**: အစုဖွဲ့ရာတွင် နီးစပ်မှုအတွက် အချိန်ယူနစ် (စက္ကန့်၊ မိနစ်၊ နာရီ၊ ရက်၊ အပတ်၊ လ သို့မဟုတ် နှစ်)

- **မြှောက်ကိန်း**: အစုဖွဲ့ကန့်သတ်ချက်ကို သတ်မှတ်သည့် ကန့်သတ်ချက်အရွယ်အစားယူနစ် အရေအတွက် (ဥပမာ၊ ကန့်သတ်ချက်ယူနစ် "နာရီ" နှင့် မြှောက်ကိန်း ၂ = ၂ နာရီ အစုဖွဲ့ကန့်သတ်ချက်)
  	]],
  	["ca"] = [[
Agrupa els punts de dades propers en clústers i agrega cada clúster en un únic valor. La funció agrupa qualsevol seqüència de punts de dades amb menys del llindar de temps indicat entre cada punt de dades.

Per exemple, amb un llindar d’1 hora, si tens diversos punts de dades a les 9:00, les 9:30 i les 10:15, s’agruparan en un únic clúster. Si el punt de dades següent fos a les 11:16, iniciaria un clúster nou.

**Opcions de configuració:**

- **Agregació**: L’operació que cal fer amb els valors de cada clúster:
  - Mínim: Valor mínim
  - Màxim: Valor màxim
  - Mitjana: Mitjana de tots els valors
  - Suma: Total de tots els valors
  - Recompte: Nombre de punts de dades
  - Variància: Variància estadística
  - Desviació estàndard: Desviació estàndard estadística

- **Unitats del llindar**: La unitat de temps per a la proximitat de l’agrupació (segons, minuts, hores, dies, setmanes, mesos o anys)

- **Multiplicador**: Quantes unitats de la mida del llindar defineixen el llindar d’agrupació (p. ex., un multiplicador de 2 amb unitats del llindar «hores» = llindar d’agrupació de 2 hores)
  	]],
  	["zh-Hans"] = [[
将相近的数据点分组为聚类，并将每个聚类聚合为单个值。该函数会将数据点之间时间间隔小于指定时间阈值的连续数据点分为同一组。

例如，若阈值为 1 小时，时间为上午 9:00、9:30 和 10:15 的多个数据点将被归入同一聚类。如果下一个数据点的时间为 11:16，则会开始新的聚类。

**配置选项：**

- **聚合**：对每个聚类中的值执行的操作：
  - Min：最小值
  - Max：最大值
  - Average：所有值的平均值
  - Sum：所有值的总和
  - Count：数据点数量
  - Variance：统计方差
  - Standard Deviation：统计标准差

- **阈值单位**：用于判断聚类接近程度的时间单位（秒、分钟、小时、天、周、月或年）

- **倍数**：定义聚类阈值的单位数量（例如，阈值单位为“小时”、倍数为 2 = 2 小时的聚类阈值）
  	]],
  	["zh-Hant"] = [[
將相近的資料點分組成叢集，並將每個叢集彙總為單一值。此函式會將資料點之間時間差小於指定時間閾值的連續資料點分組。

例如，若閾值為 1 小時，時間為上午 9:00、9:30 和 10:15 的多個資料點會被分到同一個叢集。如果下一個資料點位於上午 11:16，則會開始新的叢集。

**設定選項：**

- **彙總**：對每個叢集中的值執行的操作：
  - 最小值：最小值
  - 最大值：最大值
  - 平均值：所有值的平均值
  - 總和：所有值的總和
  - 計數：資料點數量
  - 變異數：統計變異數
  - 標準差：統計標準差

- **閾值單位**：用於判定分群接近程度的時間單位（秒、分鐘、小時、天、週、月或年）

- **倍數**：定義分群閾值的單位數量（例如閾值單位為「小時」且倍數為 2 = 2 小時的分群閾值）
  	]],
  	["hr"] = [[
Grupira obližnje podatkovne točke u klastere i svaki klaster agregira u jednu vrijednost. Funkcija grupira svaki niz podatkovnih točaka između kojih je vrijeme kraće od zadanog praga.

Na primjer, s pragom od 1 sata, ako imate nekoliko podatkovnih točaka u 9:00, 9:30 i 10:15, one će biti grupirane u jedan klaster. Ako je sljedeća podatkovna točka u 11:16, započet će novi klaster.

**Mogućnosti konfiguracije:**

- **Agregacija**: Operacija koja se izvodi nad vrijednostima u svakom klasteru:
  - Min: Minimalna vrijednost
  - Max: Maksimalna vrijednost
  - Prosjek: Srednja vrijednost svih vrijednosti
  - Zbroj: Zbroj svih vrijednosti
  - Broj: Broj podatkovnih točaka
  - Varijanca: Statistička varijanca
  - Standardna devijacija: Statistička standardna devijacija

- **Jedinice praga**: Vremenska jedinica za blizinu grupiranja (sekunde, minute, sati, dani, tjedni, mjeseci ili godine)

- **Množitelj**: Broj jedinica veličine praga koje određuju prag grupiranja (npr. množitelj 2 s jedinicama praga „sati” = prag grupiranja od 2 sata)
  	]],
  	["cs"] = [[
Seskupí blízké datové body do shluků a každý shluk agreguje do jedné hodnoty. Funkce seskupí každou posloupnost datových bodů, mezi nimiž je časový rozdíl menší než zadaný práh.

Například při prahu 1 hodina se několik datových bodů v 9:00, 9:30 a 10:15 seskupí do jednoho shluku. Pokud by další datový bod byl v 11:16, začal by nový shluk.

**Možnosti konfigurace:**

- **Agregace**: Operace prováděná s hodnotami v každém shluku:
  - Minimum: Nejnižší hodnota
  - Maximum: Nejvyšší hodnota
  - Průměr: Průměr všech hodnot
  - Součet: Součet všech hodnot
  - Počet: Počet datových bodů
  - Rozptyl: Statistický rozptyl
  - Směrodatná odchylka: Statistická směrodatná odchylka

- **Jednotky prahu**: Časová jednotka pro blízkost při shlukování (sekundy, minuty, hodiny, dny, týdny, měsíce nebo roky)

- **Násobitel**: Počet jednotek velikosti prahu, které určují práh shlukování (např. násobitel 2 s jednotkami prahu „hodiny“ = dvouhodinový práh shlukování)
  	]],
  	["da"] = [[
Grupperer nærliggende datapunkter i klynger og sammenlægger hver klynge til én værdi. Funktionen grupperer enhver sekvens af datapunkter, hvor der er mindre end den angivne tidsgrænse mellem hvert datapunkt.

For eksempel: Med en grænse på 1 time grupperes flere datapunkter kl. 9.00, 9.30 og 10.15 i én klynge. Hvis det næste datapunkt var kl. 11.16, ville det starte en ny klynge.

**Konfigurationsindstillinger:**

- **Sammenlægning**: Handlingen, der udføres på værdierne i hver klynge:
  - Min: Minimumsværdi
  - Max: Maksimumsværdi
  - Gennemsnit: Gennemsnittet af alle værdier
  - Sum: Summen af alle værdier
  - Antal: Antallet af datapunkter
  - Varians: Statistisk varians
  - Standardafvigelse: Statistisk standardafvigelse

- **Grænseenheder**: Tidsenheden for nærhed ved klyngedannelse (sekunder, minutter, timer, dage, uger, måneder eller år)

- **Multiplikator**: Hvor mange enheder af grænsestørrelsen der definerer klyngedannelsesgrænsen (f.eks. multiplikator 2 med grænsenheden "timer" = 2 timers klyngedannelsesgrænse)
  	]],
  	["nl"] = [[
Groepeert nabije gegevenspunten in clusters en voegt elke cluster samen tot één waarde. De functie groepeert elke reeks gegevenspunten waarbij de tijd tussen opeenvolgende gegevenspunten kleiner is dan de opgegeven drempel.

Bijvoorbeeld: met een drempel van 1 uur worden meerdere gegevenspunten om 9:00, 9:30 en 10:15 in één cluster gegroepeerd. Als het volgende gegevenspunt om 11:16 was, zou dit een nieuwe cluster starten.

**Configuratieopties:**

- **Aggregatie**: De bewerking die op de waarden in elke cluster wordt uitgevoerd:
  - Min: Minimumwaarde
  - Max: Maximumwaarde
  - Gemiddelde: Gemiddelde van alle waarden
  - Som: Totaal van alle waarden
  - Aantal: Aantal gegevenspunten
  - Variantie: Statistische variantie
  - Standaardafwijking: Statistische standaardafwijking

- **Eenheden voor drempel**: De tijdseenheid voor de nabijheid van clusters (seconden, minuten, uren, dagen, weken, maanden of jaren)

- **Vermenigvuldiger**: Hoeveel eenheden van de drempelgrootte de clusterdrempel bepalen (bijv. een vermenigvuldiger van 2 met drempeleenheid "uren" = een clusterdrempel van 2 uur)
  	]],
  	["et"] = [[
Rühmitab lähedased andmepunktid klastritesse ja koondab iga klastri üheks väärtuseks. Funktsioon rühmitab kõik andmepunktide jadad, mille järjestikuste punktide vaheline aeg on antud lävest väiksem.

Näiteks 1-tunnise läve korral rühmitatakse kell 9.00, 9.30 ja 10.15 olevad andmepunktid ühte klastrisse. Kui järgmine andmepunkt oleks kell 11.16, alustaks see uut klastrit.

**Seadistusvalikud:**

- **Koondamine**: Igas klastris olevate väärtustega tehtav toiming:
  - Miinimum: väikseim väärtus
  - Maksimum: suurim väärtus
  - Keskmine: kõigi väärtuste keskmine
  - Summa: kõigi väärtuste summa
  - Loendus: andmepunktide arv
  - Dispersioon: statistiline dispersioon
  - Standardhälve: statistiline standardhälve

- **Läve ühikud**: klastrite läheduse ajaühik (sekundid, minutid, tunnid, päevad, nädalad, kuud või aastad)

- **Kordaja**: mitu läve suurusühikut määrab klasterdamisläve (nt kordaja 2 ja läveühikud „tunnid” = 2-tunnine klasterdamislävi)
  	]],
  	["fil"] = [[
Pinagkak kumpol ang magkakalapit na data point at pinagsasama ang bawat kumpol sa iisang halaga. Pinagkak kumpol ng function ang anumang magkakasunod na data point na may pagitan na mas mababa sa ibinigay na time threshold.

Halimbawa, sa 1-oras na threshold, kung mayroon kang ilang data point sa 9:00am, 9:30am, at 10:15am, pagsasamahin ang mga ito sa iisang kumpol. Kung ang susunod na data point ay nasa 11:16am, magsisimula ito ng bagong kumpol.

**Mga Opsyon sa Configuration:**

- **Aggregation**: Operasyong isasagawa sa mga halaga sa bawat kumpol:
  - Min: Pinakamababang halaga
  - Max: Pinakamataas na halaga
  - Average: Mean ng lahat ng halaga
  - Sum: Kabuuan ng lahat ng halaga
  - Count: Bilang ng mga data point
  - Variance: Estadistikal na variance
  - Standard Deviation: Estadistikal na standard deviation

- **Threshold Units**: Yunit ng oras para sa lapit ng pagkumpol (segundo, minuto, oras, araw, linggo, buwan, o taon)

- **Multiplier**: Ilang yunit ng laki ng threshold ang bubuo sa clustering threshold (hal., multiplier na 2 at threshold units na "hours" = 2-oras na clustering threshold)
  	]],
  	["fi"] = [[
Ryhmittelee lähekkäiset datapisteet klustereiksi ja yhdistää kunkin klusterin yhdeksi arvoksi. Funktio ryhmittelee kaikki datapisteiden sarjat, joissa peräkkäisten datapisteiden välinen aika on annettua kynnysarvoa lyhyempi.

Esimerkiksi yhden tunnin kynnysarvolla datapisteet kello 9.00, 9.30 ja 10.15 ryhmitellään yhdeksi klusteriksi. Jos seuraava datapiste olisi kello 11.16, se aloittaisi uuden klusterin.

**Määritysasetukset:**

- **Yhdistämistapa**: Kunkin klusterin arvoille suoritettava toiminto:
  - Min: Pienin arvo
  - Max: Suurin arvo
  - Keskiarvo: Kaikkien arvojen keskiarvo
  - Summa: Kaikkien arvojen summa
  - Lukumäärä: Datapisteiden määrä
  - Varianssi: Tilastollinen varianssi
  - Keskihajonta: Tilastollinen keskihajonta

- **Kynnysarvon yksiköt**: Klusterien läheisyyden aikayksikkö (sekunnit, minuutit, tunnit, päivät, viikot, kuukaudet tai vuodet)

- **Kerroin**: Kuinka monta kynnysarvon yksikköä muodostaa ryhmittelyn kynnysarvon (esim. kerroin 2 ja kynnysarvon yksikkönä "tunnit" = 2 tunnin ryhmittelykynnysarvo)
  	]],
  	["fr"] = [[
Regroupe les points de données proches en groupes et agrège chaque groupe en une seule valeur. La fonction regroupe toute séquence de points de données séparés par moins que le seuil temporel indiqué.

Par exemple, avec un seuil d’une heure, plusieurs points de données à 9 h 00, 9 h 30 et 10 h 15 seront regroupés en un seul groupe. Si le point suivant était à 11 h 16, il commencerait un nouveau groupe.

**Options de configuration :**

- **Agrégation** : Opération à effectuer sur les valeurs de chaque groupe :
  - Min : Valeur minimale
  - Max : Valeur maximale
  - Moyenne : Moyenne de toutes les valeurs
  - Somme : Total de toutes les valeurs
  - Compte : Nombre de points de données
  - Variance : Variance statistique
  - Écart type : Écart type statistique

- **Unités du seuil** : Unité de temps utilisée pour la proximité des groupes (secondes, minutes, heures, jours, semaines, mois ou années)

- **Multiplicateur** : Nombre d’unités définissant le seuil de regroupement (par ex. un multiplicateur de 2 avec l’unité « heures » = un seuil de regroupement de 2 heures)
  	]],
  	["gl"] = [[
Agrupa os puntos de datos próximos en grupos e agrega cada grupo nun único valor. A función agrupa calquera secuencia de puntos de datos que teña entre cada punto un intervalo inferior ao limiar temporal indicado.

Por exemplo, cun limiar dunha hora, se tes varios puntos de datos ás 9:00, 9:30 e 10:15, agruparanse nun único grupo. Se o seguinte punto de datos fose ás 11:16, iniciaría un novo grupo.

**Opcións de configuración:**

- **Agregación**: A operación que se realizará sobre os valores de cada grupo:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Media: Media de todos os valores
  - Suma: Total de todos os valores
  - Contaxe: Número de puntos de datos
  - Varianza: Varianza estatística
  - Desviación estándar: Desviación estándar estatística

- **Unidades do limiar**: A unidade temporal para a proximidade da agrupación (segundos, minutos, horas, días, semanas, meses ou anos)

- **Multiplicador**: Cantas unidades do tamaño do limiar definen o limiar de agrupación (por exemplo, un multiplicador de 2 con unidades do limiar «horas» = limiar de agrupación de 2 horas)
  	]],
  	["ka"] = [[
ახლომდებარე მონაცემთა წერტილებს აჯგუფებს კლასტერებად და თითოეულ კლასტერს ერთ მნიშვნელობად აერთიანებს. ფუნქცია აჯგუფებს მონაცემთა წერტილების ნებისმიერ მიმდევრობას, სადაც მეზობელ წერტილებს შორის დროის შუალედი მითითებულ ზღვარზე ნაკლებია.

მაგალითად, 1-საათიანი ზღვრის შემთხვევაში, თუ მონაცემთა რამდენიმე წერტილი გაქვთ 9:00, 9:30 და 10:15 საათზე, ისინი ერთ კლასტერად დაჯგუფდება. თუ შემდეგი წერტილი 11:16-ზე იქნება, ის ახალ კლასტერს დაიწყებს.

**კონფიგურაციის პარამეტრები:**

- **აგრეგაცია**: თითოეულ კლასტერში მნიშვნელობებზე შესასრულებელი ოპერაცია:
  - Min: მინიმალური მნიშვნელობა
  - Max: მაქსიმალური მნიშვნელობა
  - Average: ყველა მნიშვნელობის საშუალო
  - Sum: ყველა მნიშვნელობის ჯამი
  - Count: მონაცემთა წერტილების რაოდენობა
  - Variance: სტატისტიკური დისპერსია
  - Standard Deviation: სტატისტიკური სტანდარტული გადახრა

- **ზღვრის ერთეულები**: კლასტერის სიახლოვისთვის დროის ერთეული (წამები, წუთები, საათები, დღეები, კვირები, თვეები ან წლები)

- **გამამრავლებელი**: ზღვრის ზომის რამდენი ერთეული განსაზღვრავს კლასტერიზაციის ზღვარს (მაგ., 2-ის გამამრავლებელი და "hours" = 2-საათიანი კლასტერიზაციის ზღვარი)
  	]],
  	["de"] = [[
Gruppiert nahe beieinanderliegende Datenpunkte zu Clustern und aggregiert jeden Cluster zu einem einzelnen Wert. Die Funktion gruppiert jede Folge von Datenpunkten, zwischen denen der Zeitabstand unter dem angegebenen Schwellenwert liegt.

Wenn beispielsweise ein Schwellenwert von 1 Stunde festgelegt ist und Datenpunkte um 9:00 Uhr, 9:30 Uhr und 10:15 Uhr vorliegen, werden sie zu einem einzelnen Cluster gruppiert. Ein weiterer Datenpunkt um 11:16 Uhr würde einen neuen Cluster beginnen.

**Konfigurationsoptionen:**

- **Aggregation**: Der Vorgang, der auf die Werte in jedem Cluster angewendet wird:
  - Min: Minimalwert
  - Max: Maximalwert
  - Average: Mittelwert aller Werte
  - Sum: Summe aller Werte
  - Count: Anzahl der Datenpunkte
  - Variance: Statistische Varianz
  - Standard Deviation: Statistische Standardabweichung

- **Schwelleneinheiten**: Die Zeiteinheit für die Nähe bei der Clusterbildung (Sekunden, Minuten, Stunden, Tage, Wochen, Monate oder Jahre)

- **Multiplikator**: Wie viele Einheiten der Schwellengröße den Schwellenwert für die Clusterbildung definieren (z. B. Multiplikator 2 mit Schwelleneinheit „Stunden“ = 2-Stunden-Schwellenwert)
  	]],
  	["el"] = [[
Ομαδοποιεί κοντινά σημεία δεδομένων σε συστάδες και συγκεντρώνει κάθε συστάδα σε μία τιμή. Η συνάρτηση ομαδοποιεί κάθε ακολουθία σημείων δεδομένων με χρονική απόσταση μικρότερη από το καθορισμένο όριο μεταξύ διαδοχικών σημείων.

Για παράδειγμα, με όριο 1 ώρας, αν έχετε αρκετά σημεία δεδομένων στις 9:00 π.μ., στις 9:30 π.μ. και στις 10:15 π.μ., θα ομαδοποιηθούν σε μία συστάδα. Αν το επόμενο σημείο δεδομένων ήταν στις 11:16 π.μ., θα ξεκινούσε νέα συστάδα.

**Επιλογές διαμόρφωσης:**

- **Συγκέντρωση**: Η πράξη που εκτελείται στις τιμές κάθε συστάδας:
  - Ελάχιστο: Ελάχιστη τιμή
  - Μέγιστο: Μέγιστη τιμή
  - Μέσος όρος: Μέσος όρος όλων των τιμών
  - Άθροισμα: Σύνολο όλων των τιμών
  - Πλήθος: Αριθμός σημείων δεδομένων
  - Διακύμανση: Στατιστική διακύμανση
  - Τυπική απόκλιση: Στατιστική τυπική απόκλιση

- **Μονάδες ορίου**: Η μονάδα χρόνου για την εγγύτητα ομαδοποίησης (δευτερόλεπτα, λεπτά, ώρες, ημέρες, εβδομάδες, μήνες ή έτη)

- **Πολλαπλασιαστής**: Πόσες μονάδες μεγέθους ορίου καθορίζουν το όριο ομαδοποίησης (π.χ. πολλαπλασιαστής 2 με μονάδες ορίου «ώρες» = όριο ομαδοποίησης 2 ωρών)
  	]],
  	["gu"] = [[
નજીકના ડેટા પોઇન્ટ્સને ક્લસ્ટરમાં જૂથબદ્ધ કરે છે અને દરેક ક્લસ્ટરને એક મૂલ્યમાં એકત્રિત કરે છે. દરેક ડેટા પોઇન્ટ વચ્ચે આપેલ સમય મર્યાદા કરતાં ઓછું અંતર ધરાવતા ડેટા પોઇન્ટ્સની કોઈપણ શ્રેણીને ફંક્શન જૂથબદ્ધ કરે છે.

ઉદાહરણ તરીકે, 1 કલાકની મર્યાદા સાથે, જો તમારી પાસે સવારે 9:00, 9:30 અને 10:15 વાગ્યે ઘણા ડેટા પોઇન્ટ્સ હોય, તો તેઓ એક જ ક્લસ્ટરમાં જૂથબદ્ધ થશે. આગળનો ડેટા પોઇન્ટ સવારે 11:16 વાગ્યે હોય, તો તે નવું ક્લસ્ટર શરૂ કરશે.

**ગોઠવણી વિકલ્પો:**

- **એકત્રીકરણ**: દરેક ક્લસ્ટરના મૂલ્યો પર કરવાની ક્રિયા:
  - Min: લઘુત્તમ મૂલ્ય
  - Max: મહત્તમ મૂલ્ય
  - Average: બધા મૂલ્યોનો સરેરાશ
  - Sum: બધા મૂલ્યોનો સરવાળો
  - Count: ડેટા પોઇન્ટ્સની સંખ્યા
  - Variance: આંકડાકીય વિચલન
  - Standard Deviation: આંકડાકીય પ્રમાણભૂત વિચલન

- **મર્યાદાના એકમો**: ક્લસ્ટરિંગની નજીકતા માટેનો સમય એકમ (સેકન્ડ, મિનિટ, કલાક, દિવસ, અઠવાડિયા, મહિના અથવા વર્ષ)

- **ગુણક**: ક્લસ્ટરિંગ મર્યાદા નક્કી કરતા મર્યાદા-કદના એકમોની સંખ્યા (દા.ત., "કલાક" એકમ સાથે 2 ગુણક = 2 કલાકની ક્લસ્ટરિંગ મર્યાદા)
  	]],
  	["hi"] = [[
नज़दीकी डेटा पॉइंट को क्लस्टर में समूहित करता है और प्रत्येक क्लस्टर को एक मान में समेकित करता है। यह फ़ंक्शन उन डेटा पॉइंट की किसी भी श्रृंखला को समूहित करता है जिनके बीच का समय-अंतर दिए गए थ्रेशोल्ड से कम हो।

उदाहरण के लिए, 1 घंटे के थ्रेशोल्ड के साथ, यदि आपके पास सुबह 9:00, 9:30 और 10:15 बजे कई डेटा पॉइंट हैं, तो उन्हें एक ही क्लस्टर में समूहित किया जाएगा। यदि अगला डेटा पॉइंट सुबह 11:16 बजे हो, तो वह नया क्लस्टर शुरू करेगा।

**कॉन्फ़िगरेशन विकल्प:**

- **समेकन**: प्रत्येक क्लस्टर के मानों पर किया जाने वाला संचालन:
  - न्यूनतम: न्यूनतम मान
  - अधिकतम: अधिकतम मान
  - औसत: सभी मानों का माध्य
  - योग: सभी मानों का कुल
  - गणना: डेटा पॉइंट की संख्या
  - विचरण: सांख्यिकीय विचरण
  - मानक विचलन: सांख्यिकीय मानक विचलन

- **थ्रेशोल्ड इकाइयाँ**: क्लस्टरिंग निकटता के लिए समय इकाई (सेकंड, मिनट, घंटे, दिन, सप्ताह, महीने या वर्ष)

- **गुणक**: थ्रेशोल्ड आकार की कितनी इकाइयाँ क्लस्टरिंग थ्रेशोल्ड निर्धारित करती हैं (जैसे, थ्रेशोल्ड इकाई "घंटे" और गुणक 2 = 2 घंटे का क्लस्टरिंग थ्रेशोल्ड)
  	]],
  	["hu"] = [[
A közeli adatpontokat klaszterekbe csoportosítja, majd minden klasztert egyetlen értékké összesít. A függvény minden olyan adatpontsorozatot csoportosít, amelyben az egymást követő adatpontok között az időeltérés kisebb a megadott küszöbnél.

Például 1 órás küszöb esetén a 9:00-kor, 9:30-kor és 10:15-kor rögzített adatpontok egyetlen klaszterbe kerülnek. Ha a következő adatpont 11:16-kor lenne, új klasztert kezdene.

**Konfigurációs beállítások:**

- **Összesítés**: A klaszter értékein végrehajtandó művelet:
  - Min: Minimumérték
  - Max: Maximumérték
  - Átlag: Az összes érték átlaga
  - Összeg: Az összes érték összege
  - Darabszám: Az adatpontok száma
  - Variancia: Statisztikai variancia
  - Szórás: Statisztikai szórás

- **Küszöb mértékegysége**: A közelség klaszterezéséhez használt időegység (másodperc, perc, óra, nap, hét, hónap vagy év)

- **Szorzó**: A klaszterezési küszöböt meghatározó egységek száma (például 2-es szorzó és „óra” küszöbegység = 2 órás klaszterezési küszöb)
  	]],
  	["is"] = [[
Hópar nálæga gagnapunkta í klasa og safnar hverjum klasa saman í eitt gildi. Aðgerðin hópar saman sérhverja röð gagnapunkta þar sem tímabilið milli hvers gagnapunkts er styttra en tilgreind tímamörk.

Til dæmis, með 1 klukkustundar tímamörkum verða nokkrir gagnapunktar klukkan 9:00, 9:30 og 10:15 sameinaðir í einn klasa. Ef næsti gagnapunktur væri klukkan 11:16 myndi hann hefja nýjan klasa.

**Stillingar:**

- **Söfnun**: Aðgerðin sem á að framkvæma á gildum í hverjum klasa:
  - Lágmark: Lægsta gildi
  - Hámark: Hæsta gildi
  - Meðaltal: Meðaltal allra gilda
  - Summa: Summa allra gilda
  - Fjöldi: Fjöldi gagnapunkta
  - Dreifni: Tölfræðileg dreifni
  - Staðalfrávik: Tölfræðilegt staðalfrávik

- **Einingar tímamarka**: Tímaeining fyrir nálægð innan klasa (sekúndur, mínútur, klukkustundir, dagar, vikur, mánuðir eða ár)

- **Margfeldir**: Hve margar einingar af tímamörkunum skilgreina klasamörkin (t.d. margfeldirinn 2 með einingunni „klukkustundir“ = 2 klukkustunda klasamörk)
  	]],
  	["id"] = [[
Mengelompokkan titik data yang berdekatan ke dalam klaster dan menggabungkan setiap klaster menjadi satu nilai. Fungsi ini mengelompokkan setiap urutan titik data dengan jarak waktu antar titik data kurang dari ambang waktu yang diberikan.

Misalnya, dengan ambang 1 jam, jika Anda memiliki beberapa titik data pada pukul 09.00, 09.30, dan 10.15, semuanya akan dikelompokkan ke dalam satu klaster. Jika titik data berikutnya berada pada pukul 11.16, titik tersebut akan memulai klaster baru.

**Opsi Konfigurasi:**

- **Agregasi**: Operasi yang dilakukan pada nilai dalam setiap klaster:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Rata-rata: Rata-rata semua nilai
  - Jumlah: Total semua nilai
  - Hitungan: Jumlah titik data
  - Varians: Varians statistik
  - Deviasi Standar: Deviasi standar statistik

- **Satuan Ambang**: Satuan waktu untuk kedekatan pengelompokan (detik, menit, jam, hari, minggu, bulan, atau tahun)

- **Pengali**: Jumlah satuan ukuran ambang yang menentukan ambang pengelompokan (misalnya, pengali 2 dengan satuan ambang "jam" = ambang pengelompokan 2 jam)
  	]],
  	["it"] = [[
Raggruppa i punti dati vicini in cluster e aggrega ogni cluster in un singolo valore. La funzione raggruppa qualsiasi sequenza di punti dati con un intervallo inferiore alla soglia temporale specificata tra ciascun punto dati.

Ad esempio, con una soglia di 1 ora, se hai diversi punti dati alle 9:00, alle 9:30 e alle 10:15, verranno raggruppati in un singolo cluster. Se il punto dati successivo fosse alle 11:16, inizierebbe un nuovo cluster.

**Opzioni di configurazione:**

- **Aggregazione**: l'operazione da eseguire sui valori di ogni cluster:
  - Min: valore minimo
  - Max: valore massimo
  - Media: media di tutti i valori
  - Somma: totale di tutti i valori
  - Conteggio: numero di punti dati
  - Varianza: varianza statistica
  - Deviazione standard: deviazione standard statistica

- **Unità della soglia**: l'unità di tempo per la vicinanza dei cluster (secondi, minuti, ore, giorni, settimane, mesi o anni)

- **Moltiplicatore**: quante unità della dimensione della soglia definiscono la soglia di raggruppamento (ad es. moltiplicatore 2 con unità della soglia "ore" = soglia di raggruppamento di 2 ore)
  	]],
  	["ja"] = [[
近接するデータポイントをクラスタにまとめ、各クラスタを単一の値に集約します。この関数は、各データポイント間の時間が指定したしきい値未満である一連のデータポイントをグループ化します。

たとえば、しきい値を1時間に設定した場合、午前9:00、9:30、10:15のデータポイントは1つのクラスタにまとめられます。次のデータポイントが11:16にある場合は、新しいクラスタが開始されます。

**設定オプション:**

- **集約**: 各クラスタの値に対して実行する操作:
  - Min: 最小値
  - Max: 最大値
  - Average: すべての値の平均
  - Sum: すべての値の合計
  - Count: データポイント数
  - Variance: 統計的分散
  - Standard Deviation: 統計的標準偏差

- **しきい値の単位**: クラスタリングの近接性に使用する時間単位（秒、分、時間、日、週、月、年）

- **乗数**: クラスタリングのしきい値を定義する単位数（例: しきい値の単位が「時間」で乗数が2の場合、クラスタリングのしきい値は2時間）
  	]],
  	["kn"] = [[
ಹತ್ತಿರದ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಕ್ಲಸ್ಟರ್‌ಗಳಾಗಿ ಗುಂಪುಮಾಡಿ, ಪ್ರತಿ ಕ್ಲಸ್ಟರ್ ಅನ್ನು ಒಂದೇ ಮೌಲ್ಯವಾಗಿ ಒಟ್ಟುಗೂಡಿಸುತ್ತದೆ. ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ನಡುವೆ ನೀಡಿರುವ ಸಮಯ ಮಿತಿಗಿಂತ ಕಡಿಮೆ ಅಂತರವಿರುವ ಯಾವುದೇ ಸರಣಿ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಈ ಫಂಕ್ಷನ್ ಗುಂಪುಮಾಡುತ್ತದೆ.

ಉದಾಹರಣೆಗೆ, 1-ಗಂಟೆಯ ಮಿತಿಯೊಂದಿಗೆ, ಬೆಳಿಗ್ಗೆ 9:00, 9:30 ಮತ್ತು 10:15ಕ್ಕೆ ಹಲವಾರು ಡೇಟಾ ಬಿಂದುಗಳಿದ್ದರೆ, ಅವುಗಳನ್ನು ಒಂದೇ ಕ್ಲಸ್ಟರ್‌ಗೆ ಗುಂಪುಮಾಡಲಾಗುತ್ತದೆ. ಮುಂದಿನ ಡೇಟಾ ಬಿಂದು 11:16ಕ್ಕೆ ಇದ್ದರೆ, ಅದು ಹೊಸ ಕ್ಲಸ್ಟರ್ ಪ್ರಾರಂಭಿಸುತ್ತದೆ.

**ಕಾನ್ಫಿಗರೇಶನ್ ಆಯ್ಕೆಗಳು:**

- **ಒಟ್ಟುಗೂಡಿಸುವಿಕೆ**: ಪ್ರತಿ ಕ್ಲಸ್ಟರ್‌ನ ಮೌಲ್ಯಗಳ ಮೇಲೆ ನಡೆಸುವ ಕಾರ್ಯಾಚರಣೆ:
  - Min: ಕನಿಷ್ಠ ಮೌಲ್ಯ
  - Max: ಗರಿಷ್ಠ ಮೌಲ್ಯ
  - Average: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಸರಾಸರಿ
  - Sum: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳ ಮೊತ್ತ
  - Count: ಡೇಟಾ ಬಿಂದುಗಳ ಸಂಖ್ಯೆ
  - Variance: ಸಾಂಖ್ಯಿಕ ವ್ಯತ್ಯಾಸ
  - Standard Deviation: ಸಾಂಖ್ಯಿಕ ಪ್ರಮಾಣಿತ ವಿಚಲನ

- **ಮಿತಿ ಘಟಕಗಳು**: ಕ್ಲಸ್ಟರ್ ಸಾಮೀಪ್ಯಕ್ಕಾಗಿ ಸಮಯ ಘಟಕ (ಸೆಕೆಂಡುಗಳು, ನಿಮಿಷಗಳು, ಗಂಟೆಗಳು, ದಿನಗಳು, ವಾರಗಳು, ತಿಂಗಳುಗಳು ಅಥವಾ ವರ್ಷಗಳು)

- **ಗುಣಕ**: ಕ್ಲಸ್ಟರಿಂಗ್ ಮಿತಿಯನ್ನು ನಿರ್ಧರಿಸುವ ಮಿತಿ ಗಾತ್ರದ ಘಟಕಗಳ ಸಂಖ್ಯೆ (ಉದಾ., ಮಿತಿ ಘಟಕಗಳು "ಗಂಟೆಗಳು" ಮತ್ತು ಗುಣಕ 2 = 2-ಗಂಟೆಗಳ ಕ್ಲಸ್ಟರಿಂಗ್ ಮಿತಿ)
  	]],
  	["kk"] = [[
Жақын дерек нүктелерін кластерлерге топтап, әр кластерді бір мәнге біріктіреді. Функция әр дерек нүктесінің арасында берілген уақыт шегінен аз аралық бар кез келген дерек нүктелері тізбегін топтайды.

Мысалы, 1 сағаттық шекпен 9:00, 9:30 және 10:15 уақыттарындағы бірнеше дерек нүктесі бір кластерге біріктіріледі. Келесі дерек нүктесі 11:16-да болса, жаңа кластер басталады.

**Конфигурация параметрлері:**

- **Біріктіру**: Әр кластердегі мәндерге орындалатын операция:
  - Min: Ең төменгі мән
  - Max: Ең жоғары мән
  - Average: Барлық мәндердің орташа мәні
  - Sum: Барлық мәндердің қосындысы
  - Count: Дерек нүктелерінің саны
  - Variance: Статистикалық дисперсия
  - Standard Deviation: Статистикалық стандартты ауытқу

- **Шек бірліктері**: Кластердегі жақындықты анықтайтын уақыт бірлігі (секунд, минут, сағат, күн, апта, ай немесе жыл)

- **Көбейткіш**: Кластерлеу шегін анықтайтын шек бірліктерінің саны (мысалы, шек бірлігі "сағат", көбейткіш 2 болса = 2 сағаттық кластерлеу шегі)
  	]],
  	["km"] = [[
ដាក់ចំណុចទិន្នន័យដែលនៅជិតគ្នាទៅជាក្រុម ហើយបូកសរុបក្រុមនីមួយៗទៅជាតម្លៃតែមួយ។ អនុគមន៍នេះដាក់លំដាប់ចំណុចទិន្នន័យណាមួយដែលមានចន្លោះពេលរវាងចំណុចនីមួយៗតិចជាងកម្រិតពេលវេលាដែលបានកំណត់ទៅជាក្រុមតែមួយ។

ឧទាហរណ៍ ជាមួយកម្រិត 1 ម៉ោង ប្រសិនបើអ្នកមានចំណុចទិន្នន័យនៅម៉ោង 9:00 ព្រឹក 9:30 ព្រឹក និង 10:15 ព្រឹក វានឹងត្រូវបានដាក់ជាក្រុមតែមួយ។ ប្រសិនបើចំណុចទិន្នន័យបន្ទាប់នៅម៉ោង 11:16 ព្រឹក វានឹងចាប់ផ្តើមក្រុមថ្មី។

**ជម្រើសកំណត់រចនាសម្ព័ន្ធ៖**

- **ការបូកសរុប**៖ ប្រតិបត្តិការដែលត្រូវអនុវត្តលើតម្លៃក្នុងក្រុមនីមួយៗ៖
  - Min៖ តម្លៃអប្បបរមា
  - Max៖ តម្លៃអតិបរមា
  - Average៖ មធ្យមភាគនៃតម្លៃទាំងអស់
  - Sum៖ ផលបូកតម្លៃទាំងអស់
  - Count៖ ចំនួនចំណុចទិន្នន័យ
  - Variance៖ វ៉ារ្យង់ស្ថិតិ
  - Standard Deviation៖ គម្លាតស្តង់ដារស្ថិតិ

- **ឯកតាកម្រិត**៖ ឯកតាពេលវេលាសម្រាប់ភាពជិតគ្នាក្នុងការដាក់ជាក្រុម (វិនាទី នាទី ម៉ោង ថ្ងៃ សប្តាហ៍ ខែ ឬឆ្នាំ)

- **មេគុណ**៖ ចំនួនឯកតាទំហំកម្រិតដែលកំណត់កម្រិតការដាក់ជាក្រុម (ឧ. មេគុណ 2 ជាមួយឯកតាកម្រិត "ម៉ោង" = កម្រិតដាក់ជាក្រុម 2 ម៉ោង)
  	]],
  	["ko"] = [[
가까운 데이터 포인트를 클러스터로 그룹화하고 각 클러스터를 하나의 값으로 집계합니다. 각 데이터 포인트 사이의 시간이 지정된 임계값보다 짧은 데이터 포인트 시퀀스를 그룹화합니다.

예를 들어 임계값이 1시간이고 오전 9시, 오전 9시 30분, 오전 10시 15분에 여러 데이터 포인트가 있으면 하나의 클러스터로 그룹화됩니다. 다음 데이터 포인트가 오전 11시 16분이면 새 클러스터가 시작됩니다.

**구성 옵션:**

- **집계**: 각 클러스터의 값에 수행할 작업:
  - Min: 최솟값
  - Max: 최댓값
  - Average: 모든 값의 평균
  - Sum: 모든 값의 합계
  - Count: 데이터 포인트 수
  - Variance: 통계적 분산
  - Standard Deviation: 통계적 표준편차

- **임계값 단위**: 클러스터 근접성에 사용할 시간 단위(초, 분, 시간, 일, 주, 월 또는 년)

- **배수**: 클러스터링 임계값을 정의하는 단위 수(예: 임계값 단위가 "시간"이고 배수가 2이면 클러스터링 임계값은 2시간)
  	]],
  	["ky"] = [[
Жакын жайгашкан маалымат чекиттерин кластерлерге топтоп, ар бир кластерди бир мааниге бириктирет. Функция ар бир маалымат чекитинин ортосундагы убакыт берилген чектен аз болгон ырааттуулуктардын баарын топтойт.

Мисалы, 1 сааттык чек менен 9:00дө, 9:30да жана 10:15те бир нече маалымат чекити болсо, алар бир кластерге топтолот. Кийинки маалымат чекити 11:16да болсо, ал жаңы кластерди баштайт.

**Тууралоо параметрлери:**

- **Бириктирүү**: Ар бир кластердеги маанилерге аткарылуучу операция:
  - Min: Минималдуу маани
  - Max: Максималдуу маани
  - Average: Бардык маанилердин орточо мааниси
  - Sum: Бардык маанилердин суммасы
  - Count: Маалымат чекиттеринин саны
  - Variance: Статистикалык дисперсия
  - Standard Deviation: Статистикалык стандарттык четтөө

- **Чектин бирдиги**: Жакындыкты аныктоочу убакыт бирдиги (секунд, мүнөт, саат, күн, жума, ай же жыл)

- **Көбөйткүч**: Кластерлөө чегин аныктаган чек бирдиктеринин саны (мисалы, чектин бирдиги "саат", көбөйткүч 2 болсо = 2 сааттык кластерлөө чеги)
  	]],
  	["lo"] = [[
ຈັດຈຸດຂໍ້ມູນທີ່ຢູ່ໃກ້ກັນເປັນກຸ່ມ ແລະລວມແຕ່ລະກຸ່ມເປັນຄ່າດຽວ. ຟັງຊັນນີ້ຈັດກຸ່ມລຳດັບຈຸດຂໍ້ມູນໃດໆທີ່ມີໄລຍະຫ່າງລະຫວ່າງແຕ່ລະຈຸດນ້ອຍກວ່າເກນເວລາທີ່ກຳນົດ.

ຕົວຢ່າງ: ດ້ວຍເກນ 1 ຊົ່ວໂມງ, ຖ້າມີຈຸດຂໍ້ມູນຫຼາຍຈຸດຢູ່ 9:00, 9:30 ແລະ 10:15 ໂມງເຊົ້າ, ຈຸດເຫຼົ່ານັ້ນຈະຖືກຈັດເປັນກຸ່ມດຽວ. ຖ້າຈຸດຂໍ້ມູນຕໍ່ໄປຢູ່ 11:16 ໂມງເຊົ້າ, ມັນຈະເລີ່ມກຸ່ມໃໝ່.

**ຕົວເລືອກການກຳນົດຄ່າ:**

- **ການລວມຄ່າ**: ການດຳເນີນການກັບຄ່າໃນແຕ່ລະກຸ່ມ:
  - Min: ຄ່າຕ່ຳສຸດ
  - Max: ຄ່າສູງສຸດ
  - Average: ຄ່າສະເລ່ຍຂອງທຸກຄ່າ
  - Sum: ຜົນລວມຂອງທຸກຄ່າ
  - Count: ຈຳນວນຈຸດຂໍ້ມູນ
  - Variance: ຄ່າຄວາມແປປວນທາງສະຖິຕິ
  - Standard Deviation: ຄ່າບ່ຽງເບນມາດຕະຖານທາງສະຖິຕິ

- **ໜ່ວຍຂອງເກນ**: ໜ່ວຍເວລາສຳລັບຄວາມໃກ້ຄຽງໃນການຈັດກຸ່ມ (ວິນາທີ, ນາທີ, ຊົ່ວໂມງ, ມື້, ອາທິດ, ເດືອນ ຫຼື ປີ)

- **ຕົວຄູນ**: ຈຳນວນໜ່ວຍຂະໜາດເກນທີ່ກຳນົດເປັນເກນການຈັດກຸ່ມ (ເຊັ່ນ ຕົວຄູນ 2 ກັບໜ່ວຍເກນ "ຊົ່ວໂມງ" = ເກນການຈັດກຸ່ມ 2 ຊົ່ວໂມງ)
  	]],
  	["lv"] = [[
Grupē tuvumā esošus datu punktus klasteros un apkopo katru klasteri vienā vērtībā. Funkcija grupē jebkuru datu punktu secību, kurā starp katriem datu punktiem ir mazāks laika intervāls par norādīto slieksni.

Piemēram, ja slieksnis ir 1 stunda un jums ir vairāki datu punkti plkst. 9:00, 9:30 un 10:15, tie tiks sagrupēti vienā klasterī. Ja nākamais datu punkts būtu plkst. 11:16, tas sāktu jaunu klasteri.

**Konfigurācijas opcijas:**

- **Apkopošana**: Katras klastera vērtībām veicamā darbība:
  - Min.: Minimālā vērtība
  - Maks.: Maksimālā vērtība
  - Vidējā: Visu vērtību vidējā vērtība
  - Summa: Visu vērtību summa
  - Skaits: Datu punktu skaits
  - Variance: Statistiskā variance
  - Standartnovirze: Statistiskā standartnovirze

- **Sliekšņa vienības**: Laika vienība tuvuma noteikšanai grupēšanā (sekundes, minūtes, stundas, dienas, nedēļas, mēneši vai gadi)

- **Reizinātājs**: Cik sliekšņa lieluma vienības nosaka grupēšanas slieksni (piem., reizinātājs 2 un sliekšņa vienība “stundas” = 2 stundu grupēšanas slieksnis)
  	]],
  	["lt"] = [[
Sugrupuoja netoliese esančius duomenų taškus į grupes ir kiekvieną grupę apibendrina į vieną reikšmę. Funkcija sugrupuoja bet kokią duomenų taškų seką, kurioje tarp kiekvienų duomenų taškų yra mažesnis nei nurodyta laiko riba tarpas.

Pavyzdžiui, jei riba yra 1 valanda, keli duomenų taškai 9:00, 9:30 ir 10:15 bus sugrupuoti į vieną grupę. Jei kitas duomenų taškas būtų 11:16, jis pradėtų naują grupę.

**Konfigūracijos parinktys:**

- **Apibendrinimas**: Kiekvienos grupės reikšmėms taikoma operacija:
  - Min.: Mažiausia reikšmė
  - Maks.: Didžiausia reikšmė
  - Vidurkis: Visų reikšmių vidurkis
  - Suma: Visų reikšmių suma
  - Kiekis: Duomenų taškų skaičius
  - Dispersija: Statistinė dispersija
  - Standartinis nuokrypis: Statistinis standartinis nuokrypis

- **Ribos vienetai**: Laiko vienetas grupavimo artumui (sekundės, minutės, valandos, dienos, savaitės, mėnesiai arba metai)

- **Daugiklis**: Kiek ribos vienetų sudaro grupavimo ribą (pvz., daugiklis 2 ir ribos vienetai „valandos“ = 2 valandų grupavimo riba)
  	]],
  	["mk"] = [[
Ги групира блиските точки на податоци во кластери и го собира секој кластер во една вредност. Функцијата ја групира секоја низа точки на податоци меѓу кои има помалку од зададениот временски праг.

На пример, со праг од 1 час, ако имате неколку точки на податоци во 9:00, 9:30 и 10:15 часот, тие ќе бидат групирани во еден кластер. Ако следната точка е во 11:16 часот, таа ќе започне нов кластер.

**Опции за конфигурирање:**

- **Агрегација**: Операцијата што се извршува врз вредностите во секој кластер:
  - Min: Минимална вредност
  - Max: Максимална вредност
  - Average: Просек од сите вредности
  - Sum: Збир од сите вредности
  - Count: Број на точки на податоци
  - Variance: Статистичка варијанса
  - Standard Deviation: Статистичко стандардно отстапување

- **Единици на праг**: Временската единица за близината при групирање (секунди, минути, часови, денови, недели, месеци или години)

- **Множител**: Колку единици на големината на прагот го определуваат прагот за групирање (на пр., множител 2 со единици „часови“ = праг за групирање од 2 часа)
  	]],
  	["ms"] = [[
Mengumpulkan titik data berdekatan ke dalam kelompok dan mengagregatkan setiap kelompok menjadi satu nilai. Fungsi ini mengumpulkan sebarang urutan titik data yang mempunyai jarak masa kurang daripada ambang masa yang diberikan antara setiap titik data.

Sebagai contoh, dengan ambang 1 jam, jika anda mempunyai beberapa titik data pada 9:00 pagi, 9:30 pagi dan 10:15 pagi, semuanya akan dikumpulkan ke dalam satu kelompok. Jika titik data seterusnya ialah pada 11:16 pagi, ia akan memulakan kelompok baharu.

**Pilihan Konfigurasi:**

- **Agregasi**: Operasi yang dilakukan pada nilai dalam setiap kelompok:
  - Min: Nilai minimum
  - Maks: Nilai maksimum
  - Purata: Min bagi semua nilai
  - Jumlah: Jumlah semua nilai
  - Kiraan: Bilangan titik data
  - Varians: Varians statistik
  - Sisihan Piawai: Sisihan piawai statistik

- **Unit Ambang**: Unit masa untuk kedekatan pengelompokan (saat, minit, jam, hari, minggu, bulan atau tahun)

- **Pengganda**: Bilangan unit saiz ambang yang mentakrifkan ambang pengelompokan (contohnya, pengganda 2 dengan unit ambang "jam" = ambang pengelompokan 2 jam)
  	]],
  	["ml"] = [[
അടുത്തടുത്ത ഡാറ്റാ പോയിന്റുകളെ ക്ലസ്റ്ററുകളായി ഗ്രൂപ്പുചെയ്ത് ഓരോ ക്ലസ്റ്ററിനെയും ഒരൊറ്റ മൂല്യമായി സംഗ്രഹിക്കുന്നു. ഓരോ ഡാറ്റാ പോയിന്റുകൾക്കുമിടയിലെ ഇടവേള നൽകിയ സമയപരിധിയേക്കാൾ കുറവായിരിക്കുന്ന തുടർച്ചയായ ഡാറ്റാ പോയിന്റുകളെ ഫംഗ്ഷൻ ഗ്രൂപ്പുചെയ്യുന്നു.

ഉദാഹരണത്തിന്, സമയപരിധി 1 മണിക്കൂർ ആണെങ്കിൽ, രാവിലെ 9:00, 9:30, 10:15 എന്നിവയിലെ ഡാറ്റാ പോയിന്റുകൾ ഒരൊറ്റ ക്ലസ്റ്ററായി ഗ്രൂപ്പുചെയ്യപ്പെടും. അടുത്ത ഡാറ്റാ പോയിന്റ് 11:16-ന് ആണെങ്കിൽ അത് പുതിയ ക്ലസ്റ്റർ ആരംഭിക്കും.

**കോൺഫിഗറേഷൻ ഓപ്ഷനുകൾ:**

- **അഗ്രിഗേഷൻ**: ഓരോ ക്ലസ്റ്ററിലെയും മൂല്യങ്ങളിൽ നടത്തേണ്ട പ്രവർത്തനം:
  - Min: കുറഞ്ഞ മൂല്യം
  - Max: കൂടിയ മൂല്യം
  - Average: എല്ലാ മൂല്യങ്ങളുടെയും ശരാശരി
  - Sum: എല്ലാ മൂല്യങ്ങളുടെയും ആകെ തുക
  - Count: ഡാറ്റാ പോയിന്റുകളുടെ എണ്ണം
  - Variance: സ്ഥിതിവിവര വ്യതിയാനം
  - Standard Deviation: സ്ഥിതിവിവര സ്റ്റാൻഡേർഡ് ഡിവിയേഷൻ

- **Threshold Units**: ക്ലസ്റ്ററിംഗിലെ സമീപ്യതയ്ക്കുള്ള സമയ യൂണിറ്റ് (സെക്കൻഡ്, മിനിറ്റ്, മണിക്കൂർ, ദിവസം, ആഴ്ച, മാസം, അല്ലെങ്കിൽ വർഷം)

- **Multiplier**: ക്ലസ്റ്ററിംഗ് പരിധി നിർവചിക്കുന്ന threshold യൂണിറ്റുകളുടെ എണ്ണം (ഉദാ., threshold units "hours" ഉം multiplier 2 ഉം = 2 മണിക്കൂർ ക്ലസ്റ്ററിംഗ് പരിധി)
  	]],
  	["mr"] = [[
जवळील डेटा बिंदूंना क्लस्टरमध्ये गटबद्ध करते आणि प्रत्येक क्लस्टरचे एका मूल्यामध्ये एकत्रीकरण करते. प्रत्येक डेटा बिंदूमधील वेळेचा फरक दिलेल्या मर्यादेपेक्षा कमी असल्यास, फंक्शन त्या डेटा बिंदूंच्या क्रमाला एकाच क्लस्टरमध्ये गटबद्ध करते.

उदाहरणार्थ, 1 तासाची मर्यादा असल्यास, सकाळी 9:00, 9:30 आणि 10:15 वाजताचे अनेक डेटा बिंदू एकाच क्लस्टरमध्ये गटबद्ध केले जातील. पुढील डेटा बिंदू सकाळी 11:16 वाजता असल्यास, तो नवीन क्लस्टर सुरू करेल.

**कॉन्फिगरेशन पर्याय:**

- **एकत्रीकरण**: प्रत्येक क्लस्टरमधील मूल्यांवर करायची प्रक्रिया:
  - किमान: किमान मूल्य
  - कमाल: कमाल मूल्य
  - सरासरी: सर्व मूल्यांची सरासरी
  - बेरीज: सर्व मूल्यांची एकूण बेरीज
  - संख्या: डेटा बिंदूंची संख्या
  - विचरण: सांख्यिकीय विचरण
  - प्रमाणित विचलन: सांख्यिकीय प्रमाणित विचलन

- **मर्यादा एकके**: क्लस्टरिंगमधील जवळीक मोजण्यासाठीचे वेळेचे एकक (सेकंद, मिनिटे, तास, दिवस, आठवडे, महिने किंवा वर्षे)

- **गुणक**: क्लस्टरिंगची मर्यादा ठरवणाऱ्या मर्यादा-एककांच्या आकारांची संख्या (उदा., मर्यादा एकके "तास" आणि गुणक 2 असल्यास = 2 तासांची क्लस्टरिंग मर्यादा)
  	]],
  	["mn"] = [[
Ойролцоох өгөгдлийн цэгүүдийг бүлэглэж, бүлэг бүрийг нэг утга болгон нэгтгэнэ. Өгөгдлийн цэгүүдийн хоорондох хугацаа өгөгдсөн босгоос бага байвал дараалсан цэгүүдийг нэг бүлэгт оруулна.

Жишээлбэл, босго нь 1 цаг бөгөөд 9:00, 9:30, 10:15 цагт хэд хэдэн өгөгдлийн цэг байвал тэдгээрийг нэг бүлэгт оруулна. Дараагийн цэг 11:16 цагт байвал шинэ бүлэг эхэлнэ.

**Тохиргооны сонголтууд:**

- **Нэгтгэл**: Бүлэг бүрийн утгад хийх үйлдэл:
  - Min: Хамгийн бага утга
  - Max: Хамгийн их утга
  - Average: Бүх утгын дундаж
  - Sum: Бүх утгын нийлбэр
  - Count: Өгөгдлийн цэгийн тоо
  - Variance: Статистикийн дисперс
  - Standard Deviation: Статистикийн стандарт хазайлт

- **Босгоны нэгж**: Ойролцоо байдлыг бүлэглэх хугацааны нэгж (секунд, минут, цаг, өдөр, долоо хоног, сар эсвэл жил)

- **Үржүүлэгч**: Бүлэглэх босгыг тодорхойлох босгоны нэгжийн тоо (жишээ нь, босгоны нэгж нь "цаг", үржүүлэгч нь 2 бол 2 цагийн босго)
  	]],
  	["ne"] = [[
नजिकका डेटा बिन्दुहरूलाई क्लस्टरमा समूहबद्ध गरी प्रत्येक क्लस्टरलाई एउटै मानमा समेकित गर्छ। प्रत्येक डेटा बिन्दुबीच दिइएको समय सीमाभन्दा कम अन्तर भएको कुनै पनि क्रमलाई यसले समूहबद्ध गर्छ।

उदाहरणका लागि, १ घण्टाको सीमा हुँदा बिहान ९:००, ९:३० र १०:१५ का डेटा बिन्दुहरू एउटै क्लस्टरमा समूहबद्ध हुन्छन्। अर्को डेटा बिन्दु बिहान ११:१६ मा भएमा नयाँ क्लस्टर सुरु हुन्छ।

**कन्फिगरेसन विकल्पहरू:**

- **समेकन**: प्रत्येक क्लस्टरका मानमा लागू गर्ने कार्य:
  - Min: न्यूनतम मान
  - Max: अधिकतम मान
  - Average: सबै मानहरूको औसत
  - Sum: सबै मानहरूको योग
  - Count: डेटा बिन्दुहरूको संख्या
  - Variance: सांख्यिकीय विचरण
  - Standard Deviation: सांख्यिकीय मानक विचलन

- **सीमा एकाइहरू**: क्लस्टरको निकटताका लागि समय एकाइ (सेकेन्ड, मिनेट, घण्टा, दिन, हप्ता, महिना वा वर्ष)

- **गुणक**: क्लस्टरिङ सीमा निर्धारण गर्ने सीमा-आकारका एकाइहरूको संख्या (जस्तै, सीमा एकाइ "घण्टा" र गुणक २ = २ घण्टाको क्लस्टरिङ सीमा)
  	]],
  	["no"] = [[
Grupperer datapunkter som ligger nær hverandre, i klynger og aggregerer hver klynge til én enkelt verdi. Funksjonen grupperer alle sekvenser av datapunkter der det er mindre enn den angitte tidsgrensen mellom hvert datapunkt.

Hvis du for eksempel har en tidsgrense på 1 time og flere datapunkter kl. 09:00, 09:30 og 10:15, grupperes de i én klynge. Hvis neste datapunkt var kl. 11:16, ville det startet en ny klynge.

**Konfigurasjonsalternativer:**

- **Aggregering**: Operasjonen som skal utføres på verdiene i hver klynge:
  - Min: Minimumsverdi
  - Maks: Maksimumsverdi
  - Gjennomsnitt: Gjennomsnittet av alle verdier
  - Sum: Summen av alle verdier
  - Antall: Antall datapunkter
  - Varians: Statistisk varians
  - Standardavvik: Statistisk standardavvik

- **Enheter for terskel**: Tidsenheten for hvor nær hverandre datapunktene må være (sekunder, minutter, timer, dager, uker, måneder eller år)

- **Multiplikator**: Hvor mange enheter av terskelstørrelsen som utgjør klyngingsterskelen (f.eks. multiplikator 2 med terskelenheten «timer» = klyngingsterskel på 2 timer)
  	]],
  	["pl"] = [[
Grupuje pobliskie punkty danych w klastry i agreguje każdy klaster do jednej wartości. Funkcja grupuje każdą sekwencję punktów danych, w której odstęp czasu między kolejnymi punktami jest mniejszy od podanego progu czasowego.

Na przykład przy progu wynoszącym 1 godzinę kilka punktów danych z godzin 9:00, 9:30 i 10:15 zostanie pogrupowanych w jeden klaster. Jeśli następny punkt danych przypadałby na 11:16, rozpocząłby nowy klaster.

**Opcje konfiguracji:**

- **Agregacja**: Operacja wykonywana na wartościach w każdym klastrze:
  - Min: Wartość minimalna
  - Max: Wartość maksymalna
  - Średnia: Średnia wszystkich wartości
  - Suma: Suma wszystkich wartości
  - Liczba: Liczba punktów danych
  - Wariancja: Wariancja statystyczna
  - Odchylenie standardowe: Odchylenie standardowe

- **Jednostki progu**: Jednostka czasu używana do określania bliskości punktów (sekundy, minuty, godziny, dni, tygodnie, miesiące lub lata)

- **Mnożnik**: Liczba jednostek rozmiaru progu definiująca próg grupowania (np. mnożnik 2 i jednostka progu „godziny” = próg grupowania wynoszący 2 godziny)
  	]],
  	["pt"] = [[
Agrupa pontos de dados próximos em grupos e agrega cada grupo num único valor. A função agrupa qualquer sequência de pontos de dados com menos do que o limite de tempo indicado entre cada ponto.

Por exemplo, com um limite de 1 hora, se tiver vários pontos de dados às 09:00, 09:30 e 10:15, eles serão agrupados num único grupo. Se o ponto seguinte fosse às 11:16, iniciaria um novo grupo.

**Opções de configuração:**

- **Agregação**: A operação a executar nos valores de cada grupo:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Média: Média de todos os valores
  - Soma: Total de todos os valores
  - Contagem: Número de pontos de dados
  - Variância: Variância estatística
  - Desvio padrão: Desvio padrão estatístico

- **Unidades do limite**: A unidade de tempo para a proximidade do agrupamento (segundos, minutos, horas, dias, semanas, meses ou anos)

- **Multiplicador**: Quantas unidades do tamanho do limite definem o limite de agrupamento (por exemplo, multiplicador 2 com unidades do limite "horas" = limite de agrupamento de 2 horas)
  	]],
  	["pa"] = [[
ਨੇੜਲੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਕਲੱਸਟਰਾਂ ਵਿੱਚ ਸਮੂਹਬੱਧ ਕਰਦਾ ਹੈ ਅਤੇ ਹਰੇਕ ਕਲੱਸਟਰ ਨੂੰ ਇੱਕ ਮੁੱਲ ਵਿੱਚ ਇਕੱਠਾ ਕਰਦਾ ਹੈ। ਇਹ ਫੰਕਸ਼ਨ ਉਹਨਾਂ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੀ ਕਿਸੇ ਵੀ ਲੜੀ ਨੂੰ ਸਮੂਹਬੱਧ ਕਰਦਾ ਹੈ ਜਿਨ੍ਹਾਂ ਵਿਚਕਾਰ ਸਮਾਂ ਦਿੱਤੀ ਗਈ ਸੀਮਾ ਤੋਂ ਘੱਟ ਹੋਵੇ।

ਉਦਾਹਰਨ ਵਜੋਂ, 1 ਘੰਟੇ ਦੀ ਸੀਮਾ ਨਾਲ, ਜੇ ਤੁਹਾਡੇ ਕੋਲ ਸਵੇਰੇ 9:00, 9:30 ਅਤੇ 10:15 ਵਜੇ ਕਈ ਡਾਟਾ ਪੁਆਇੰਟ ਹਨ, ਤਾਂ ਉਹ ਇੱਕੋ ਕਲੱਸਟਰ ਵਿੱਚ ਸਮੂਹਬੱਧ ਕੀਤੇ ਜਾਣਗੇ। ਜੇ ਅਗਲਾ ਡਾਟਾ ਪੁਆਇੰਟ ਸਵੇਰੇ 11:16 ਵਜੇ ਹੋਵੇ, ਤਾਂ ਇਹ ਨਵਾਂ ਕਲੱਸਟਰ ਸ਼ੁਰੂ ਕਰੇਗਾ।

**ਕਨਫਿਗਰੇਸ਼ਨ ਵਿਕਲਪ:**

- **ਇਕੱਠਾ ਕਰਨ ਦੀ ਵਿਧੀ**: ਹਰੇਕ ਕਲੱਸਟਰ ਦੇ ਮੁੱਲਾਂ ਉੱਤੇ ਕੀਤੀ ਜਾਣ ਵਾਲੀ ਕਾਰਵਾਈ:
  - Min: ਘੱਟੋ-ਘੱਟ ਮੁੱਲ
  - Max: ਵੱਧ ਤੋਂ ਵੱਧ ਮੁੱਲ
  - Average: ਸਾਰੇ ਮੁੱਲਾਂ ਦਾ ਔਸਤ
  - Sum: ਸਾਰੇ ਮੁੱਲਾਂ ਦਾ ਜੋੜ
  - Count: ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੀ ਗਿਣਤੀ
  - Variance: ਅੰਕੜਾਤਮਕ ਵੈਰੀਅੰਸ
  - Standard Deviation: ਅੰਕੜਾਤਮਕ ਮਿਆਰੀ ਵਿਸਥਾਪਨ

- **ਸੀਮਾ ਇਕਾਈਆਂ**: ਨੇੜਤਾ ਅਨੁਸਾਰ ਕਲੱਸਟਰਿੰਗ ਲਈ ਸਮੇਂ ਦੀ ਇਕਾਈ (ਸਕਿੰਟ, ਮਿੰਟ, ਘੰਟੇ, ਦਿਨ, ਹਫ਼ਤੇ, ਮਹੀਨੇ ਜਾਂ ਸਾਲ)

- **ਗੁਣਕ**: ਕਿੰਨੀਆਂ ਸੀਮਾ-ਆਕਾਰ ਇਕਾਈਆਂ ਕਲੱਸਟਰਿੰਗ ਸੀਮਾ ਬਣਾਉਂਦੀਆਂ ਹਨ (ਉਦਾਹਰਨ ਲਈ, "ਘੰਟੇ" ਸੀਮਾ ਇਕਾਈ ਅਤੇ 2 ਗੁਣਕ = 2 ਘੰਟਿਆਂ ਦੀ ਕਲੱਸਟਰਿੰਗ ਸੀਮਾ)
  	]],
  	["ro"] = [[
Grupează punctele de date apropiate în clustere și agregă fiecare cluster într-o singură valoare. Funcția grupează orice secvență de puncte de date între care există mai puțin decât pragul de timp specificat.

De exemplu, cu un prag de 1 oră, dacă ai mai multe puncte de date la 09:00, 09:30 și 10:15, acestea vor fi grupate într-un singur cluster. Dacă următorul punct de date ar fi la 11:16, acesta ar începe un cluster nou.

**Opțiuni de configurare:**

- **Agregare**: Operația efectuată asupra valorilor din fiecare cluster:
  - Min: Valoarea minimă
  - Max: Valoarea maximă
  - Medie: Media tuturor valorilor
  - Sumă: Totalul tuturor valorilor
  - Număr: Numărul de puncte de date
  - Varianță: Varianța statistică
  - Deviație standard: Deviația standard statistică

- **Unități pentru prag**: Unitatea de timp pentru apropierea în grupare (secunde, minute, ore, zile, săptămâni, luni sau ani)

- **Multiplicator**: Numărul de unități de dimensiune a pragului care definesc pragul de grupare (de exemplu, un multiplicator de 2 cu unitatea de prag „ore” = prag de grupare de 2 ore)
  	]],
  	["rm"] = [[
Ragruppa puncts da datas vischins en clusters e fa la aggregaziun da mintga cluster en ina singula valur. La funcziun ragruppa mintga sequenza da puncts da datas cun damain che la limita temporala dada tranter mintga punct.

Per exempel, cun ina limita d’ina ura, vegnan plirs puncts da datas a las 09:00, 09:30 e 10:15 ragruppads en in singul cluster. Sche’l proxim punct fiss a las 11:16, entschevi in nov cluster.

**Opziuns da configuraziun:**

- **Aggregaziun**: L’operaziun da far sin las valurs en mintga cluster:
  - Min: Valur minimala
  - Max: Valur maximala
  - Media: Media da tut las valurs
  - Summa: Total da tut las valurs
  - Dumber: Dumber da puncts da datas
  - Varianza: Varianza statistica
  - Deviaziun standard: Deviaziun standard statistica

- **Unitads da la limita**: L’unitad temporala per la vischinanza dals clusters (secundas, minutas, uras, dis, emnas, mais u onns)

- **Multiplicatur**: Quants units da la grondezza da la limita defineschan la limita dal clustering (p. ex., multiplicatur 2 cun unitads «uras» = limita da clustering da 2 uras)
  	]],
  	["ru"] = [[
Группирует близкие точки данных в кластеры и объединяет каждый кластер в одно значение. Функция объединяет любую последовательность точек данных, если интервал между соседними точками меньше заданного временного порога.

Например, при пороге в 1 час несколько точек данных в 9:00, 9:30 и 10:15 будут объединены в один кластер. Если следующая точка будет в 11:16, начнётся новый кластер.

**Параметры конфигурации:**

- **Агрегация**: Операция над значениями в каждом кластере:
  - Минимум: Минимальное значение
  - Максимум: Максимальное значение
  - Среднее: Среднее всех значений
  - Сумма: Сумма всех значений
  - Количество: Число точек данных
  - Дисперсия: Статистическая дисперсия
  - Стандартное отклонение: Статистическое стандартное отклонение

- **Единицы порога**: Единица времени для близости точек (секунды, минуты, часы, дни, недели, месяцы или годы)

- **Множитель**: Количество единиц размера порога, определяющее порог кластеризации (например, множитель 2 и единицы порога «часы» = порог кластеризации 2 часа)
  	]],
  	["sr"] = [[
Grupiše obližnje tačke podataka u klastere i objedinjuje svaki klaster u jednu vrednost. Funkcija grupiše svaki niz tačaka podataka između kojih je vremenski razmak manji od zadatog praga.

Na primer, uz prag od 1 sata, ako imate nekoliko tačaka podataka u 9:00, 9:30 i 10:15, one će biti grupisane u jedan klaster. Ako je sledeća tačka podataka u 11:16, započeće novi klaster.

**Opcije konfiguracije:**

- **Objedinjavanje**: Operacija koja se izvršava nad vrednostima u svakom klasteru:
  - Min: Minimalna vrednost
  - Max: Maksimalna vrednost
  - Prosek: Srednja vrednost svih vrednosti
  - Zbir: Zbir svih vrednosti
  - Broj: Broj tačaka podataka
  - Varijansa: Statistička varijansa
  - Standardna devijacija: Statistička standardna devijacija

- **Jedinice praga**: Vremenska jedinica za blizinu pri grupisanju (sekunde, minuti, sati, dani, nedelje, meseci ili godine)

- **Množilac**: Broj jedinica veličine praga koje određuju prag grupisanja (npr. množilac 2 uz jedinice praga „sati“ = prag grupisanja od 2 sata)
  	]],
  	["si"] = [[
ආසන්න දත්ත ලක්ෂ්‍ය පොකුරුවලට සමූහගත කර, සෑම පොකුරක්ම තනි අගයකට එකතු කරයි. සෑම අනුගාමී දත්ත ලක්ෂ්‍ය දෙකක් අතර කාල පරතරය දී ඇති සීමාවට වඩා අඩු නම්, එම ලක්ෂ්‍ය සමූහගත කරයි.

උදාහරණයක් ලෙස, පැය 1ක සීමාවක් සමඟ, පෙ.ව. 9:00, පෙ.ව. 9:30 සහ පෙ.ව. 10:15 යන වේලාවල දත්ත ලක්ෂ්‍ය කිහිපයක් තිබේ නම්, ඒවා තනි පොකුරකට සමූහගත වේ. ඊළඟ දත්ත ලක්ෂ්‍යය පෙ.ව. 11:16ට තිබුණේ නම්, එය නව පොකුරක් ආරම්භ කරයි.

**වින්‍යාස විකල්ප:**

- **එකතු කිරීම**: සෑම පොකුරකම අගයන් මත සිදු කරන මෙහෙයුම:
  - අවම: අවම අගය
  - උපරිම: උපරිම අගය
  - සාමාන්‍යය: සියලු අගයන්ගේ මධ්‍යන්‍යය
  - එකතුව: සියලු අගයන්ගේ එකතුව
  - ගණන: දත්ත ලක්ෂ්‍ය ගණන
  - විචලතාව: සංඛ්‍යානමය විචලතාව
  - සම්මත අපගමනය: සංඛ්‍යානමය සම්මත අපගමනය

- **සීමා ඒකක**: පොකුරුකරණයේ ආසන්නතාව සඳහා කාල ඒකකය (තත්පර, මිනිත්තු, පැය, දින, සති, මාස හෝ වසර)

- **ගුණකය**: පොකුරුකරණ සීමාව නිර්ණය කරන සීමා ප්‍රමාණ ඒකක ගණන (උදා: සීමා ඒකක "පැය" සහ ගුණකය 2 = පැය 2ක පොකුරුකරණ සීමාව)
  	]],
  	["sk"] = [[
Zoskupí blízke údajové body do zhlukov a každý zhluk agreguje do jednej hodnoty. Funkcia zoskupí každú postupnosť údajových bodov, medzi ktorými je časový rozdiel menší než zadaný prah.

Napríklad pri hodinovom prahu sa údajové body o 9:00, 9:30 a 10:15 zoskupia do jedného zhluku. Ak by bol ďalší údajový bod o 11:16, začal by nový zhluk.

**Možnosti konfigurácie:**

- **Agregácia**: Operácia vykonaná s hodnotami v každom zhluku:
  - Minimum: Najnižšia hodnota
  - Maximum: Najvyššia hodnota
  - Priemer: Priemer všetkých hodnôt
  - Súčet: Súčet všetkých hodnôt
  - Počet: Počet údajových bodov
  - Variancia: Štatistická variancia
  - Smerodajná odchýlka: Štatistická smerodajná odchýlka

- **Jednotky prahu**: Časová jednotka pre blízkosť zhlukov (sekundy, minúty, hodiny, dni, týždne, mesiace alebo roky)

- **Násobiteľ**: Počet jednotiek veľkosti prahu určujúcich prah zhlukovania (napr. násobiteľ 2 s jednotkami prahu „hodiny“ = 2-hodinový prah zhlukovania)
  	]],
  	["sl"] = [[
Združi bližnje podatkovne točke v skupine in vsako skupino združi v eno samo vrednost. Funkcija združi vsako zaporedje podatkovnih točk, med katerimi je časovni razmik manjši od podanega praga.

Če je na primer prag 1 ura in imate podatkovne točke ob 9.00, 9.30 in 10.15, bodo združene v eno skupino. Če je naslednja podatkovna točka ob 11.16, bo začela novo skupino.

**Možnosti konfiguracije:**

- **Združevanje**: Operacija, ki se izvede nad vrednostmi v vsaki skupini:
  - Min: Najmanjša vrednost
  - Max: Največja vrednost
  - Povprečje: Srednja vrednost vseh vrednosti
  - Vsota: Skupna vrednost vseh vrednosti
  - Število: Število podatkovnih točk
  - Varianca: Statistična varianca
  - Standardni odklon: Statistični standardni odklon

- **Enote praga**: Časovna enota za bližino pri združevanju (sekunde, minute, ure, dnevi, tedni, meseci ali leta)

- **Množitelj**: Koliko enot velikosti praga določa prag združevanja (npr. množitelj 2 z enoto praga »ure« = 2-urni prag združevanja)
  	]],
  	["es"] = [[
Agrupa los puntos de datos cercanos en grupos y agrega cada grupo en un único valor. La función agrupa cualquier secuencia de puntos de datos que tenga menos del umbral de tiempo indicado entre cada punto.

Por ejemplo, con un umbral de 1 hora, si tienes varios puntos de datos a las 9:00, 9:30 y 10:15, se agruparán en un único grupo. Si el siguiente punto de datos fuera a las 11:16, comenzaría un nuevo grupo.

**Opciones de configuración:**

- **Agregación**: La operación que se realizará sobre los valores de cada grupo:
  - Mínimo: Valor mínimo
  - Máximo: Valor máximo
  - Promedio: Media de todos los valores
  - Suma: Total de todos los valores
  - Recuento: Número de puntos de datos
  - Varianza: Varianza estadística
  - Desviación estándar: Desviación estándar estadística

- **Unidades del umbral**: La unidad de tiempo para la proximidad de agrupación (segundos, minutos, horas, días, semanas, meses o años)

- **Multiplicador**: Cuántas unidades del tamaño del umbral definen el umbral de agrupación (por ejemplo, un multiplicador de 2 con unidades de umbral «horas» = umbral de agrupación de 2 horas)
  	]],
  	["sw"] = [[
Hupanga nukta za data zilizo karibu katika makundi na kujumlisha kila kundi kuwa thamani moja. Function hupanga mfululizo wowote wa nukta za data zilizo na tofauti ya muda iliyo chini ya kikomo cha muda kilichotolewa kati ya kila nukta.

Kwa mfano, ukiwa na kikomo cha saa 1 na nukta za data saa 9:00, 9:30, na 10:15, zitapangwa katika kundi moja. Ikiwa nukta inayofuata itakuwa saa 11:16, itaanzisha kundi jipya.

**Chaguo za Usanidi:**

- **Ujumlishaji**: Operesheni ya kufanya kwenye thamani za kila kundi:
  - Min: Thamani ya chini kabisa
  - Max: Thamani ya juu kabisa
  - Average: Wastani wa thamani zote
  - Sum: Jumla ya thamani zote
  - Count: Idadi ya nukta za data
  - Variance: Tofauti ya kitakwimu
  - Standard Deviation: Mkengeuko wa kawaida wa kitakwimu

- **Vipimo vya Kikomo**: Kipimo cha muda cha ukaribu wa makundi (sekunde, dakika, saa, siku, wiki, miezi, au miaka)

- **Kizidishi**: Idadi ya vipimo vya ukubwa wa kikomo vinavyofafanua kikomo cha makundi (kwa mfano, kizidishi cha 2 chenye vipimo vya kikomo vya "saa" = kikomo cha makundi cha saa 2)
  	]],
  	["sv"] = [[
Grupperar närliggande datapunkter i kluster och sammanställer varje kluster till ett enda värde. Funktionen grupperar alla sekvenser av datapunkter där tiden mellan datapunkterna understiger det angivna tidsgränsvärdet.

Till exempel: med en gräns på 1 timme grupperas datapunkter klockan 09:00, 09:30 och 10:15 i ett enda kluster. Om nästa datapunkt infaller klockan 11:16 börjar ett nytt kluster.

**Konfigurationsalternativ:**

- **Sammanställning**: Åtgärden som utförs på värdena i varje kluster:
  - Min: Minimivärde
  - Max: Maximivärde
  - Genomsnitt: Medelvärdet av alla värden
  - Summa: Summan av alla värden
  - Antal: Antalet datapunkter
  - Varians: Statistisk varians
  - Standardavvikelse: Statistisk standardavvikelse

- **Enheter för tröskelvärde**: Tidsenheten för närhetsklustring (sekunder, minuter, timmar, dagar, veckor, månader eller år)

- **Multiplikator**: Hur många enheter av tröskelvärdet som definierar klustringströskeln (t.ex. multiplikator 2 med tröskelenheten "timmar" = 2 timmars klustringströskel)
  	]],
  	["ta"] = [[
அருகிலுள்ள தரவுப் புள்ளிகளை குழுக்களாகப் பிரித்து, ஒவ்வொரு குழுவையும் ஒரே மதிப்பாகத் தொகுக்கிறது. ஒவ்வொரு தரவுப் புள்ளிக்கும் இடையிலான நேர இடைவெளி கொடுக்கப்பட்ட வரம்பைவிடக் குறைவாக இருக்கும் தொடரை இந்த Function குழுவாக்குகிறது.

எடுத்துக்காட்டாக, 1-மணிநேர வரம்பில், காலை 9:00, 9:30 மற்றும் 10:15 மணிகளில் உள்ள தரவுப் புள்ளிகள் ஒரே குழுவாகப் பிரிக்கப்படும். அடுத்த தரவுப் புள்ளி 11:16 மணியில் இருந்தால், அது புதிய குழுவைத் தொடங்கும்.

**உள்ளமைவு விருப்பங்கள்:**

- **தொகுத்தல்**: ஒவ்வொரு குழுவிலும் உள்ள மதிப்புகளில் செய்யப்படும் செயல்பாடு:
  - Min: குறைந்தபட்ச மதிப்பு
  - Max: அதிகபட்ச மதிப்பு
  - Average: அனைத்து மதிப்புகளின் சராசரி
  - Sum: அனைத்து மதிப்புகளின் மொத்தம்
  - Count: தரவுப் புள்ளிகளின் எண்ணிக்கை
  - Variance: புள்ளியியல் மாறுபாடு
  - Standard Deviation: புள்ளியியல் திட்ட விலகல்

- **வரம்பு அலகுகள்**: குழுவாக்க நெருக்கத்திற்கான நேர அலகு (வினாடிகள், நிமிடங்கள், மணிநேரங்கள், நாட்கள், வாரங்கள், மாதங்கள் அல்லது ஆண்டுகள்)

- **பெருக்கி**: குழுவாக்க வரம்பை வரையறுக்கும் வரம்பு அளவு அலகுகளின் எண்ணிக்கை (எ.கா., வரம்பு அலகுகள் "மணிநேரங்கள்" மற்றும் பெருக்கி 2 என்றால் = 2-மணிநேர குழுவாக்க வரம்பு)
  	]],
  	["te"] = [[
సమీపంలోని డేటా పాయింట్లను క్లస్టర్లుగా సమూహపరచి, ప్రతి క్లస్టర్‌ను ఒకే విలువగా సమగ్రపరుస్తుంది. ప్రతి డేటా పాయింట్ మధ్య వ్యవధి ఇచ్చిన సమయ పరిమితి కంటే తక్కువగా ఉన్న వరుస డేటా పాయింట్లను ఈ ఫంక్షన్ సమూహపరుస్తుంది.

ఉదాహరణకు, 1 గంట పరిమితితో 9:00am, 9:30am, 10:15am వద్ద అనేక డేటా పాయింట్లు ఉంటే, అవి ఒకే క్లస్టర్‌గా సమూహపరచబడతాయి. తదుపరి డేటా పాయింట్ 11:16am వద్ద ఉంటే, అది కొత్త క్లస్టర్‌ను ప్రారంభిస్తుంది.

**కాన్ఫిగరేషన్ ఎంపికలు:**

- **సమగ్రణ**: ప్రతి క్లస్టర్‌లోని విలువలపై నిర్వహించే ఆపరేషన్:
  - Min: కనిష్ఠ విలువ
  - Max: గరిష్ఠ విలువ
  - Average: అన్ని విలువల సగటు
  - Sum: అన్ని విలువల మొత్తం
  - Count: డేటా పాయింట్ల సంఖ్య
  - Variance: గణాంక వ్యత్యాసం
  - Standard Deviation: గణాంక ప్రామాణిక విచలనం

- **పరిమితి యూనిట్లు**: క్లస్టరింగ్ సమీపతకు సమయ యూనిట్ (సెకన్లు, నిమిషాలు, గంటలు, రోజులు, వారాలు, నెలలు లేదా సంవత్సరాలు)

- **గుణకం**: క్లస్టరింగ్ పరిమితిని నిర్వచించే పరిమితి పరిమాణ యూనిట్ల సంఖ్య (ఉదా., పరిమితి యూనిట్లు "గంటలు", గుణకం 2 = 2 గంటల క్లస్టరింగ్ పరిమితి)
  	]],
  	["th"] = [[
จัดกลุ่มจุดข้อมูลที่อยู่ใกล้กันเป็นคลัสเตอร์ และรวมแต่ละคลัสเตอร์เป็นค่าเดียว ฟังก์ชันนี้จะจัดกลุ่มลำดับจุดข้อมูลที่มีช่วงเวลาระหว่างแต่ละจุดน้อยกว่าเกณฑ์เวลาที่กำหนด

ตัวอย่างเช่น หากกำหนดเกณฑ์เป็น 1 ชั่วโมง และมีจุดข้อมูลเวลา 9:00 น., 9:30 น. และ 10:15 น. จุดข้อมูลเหล่านี้จะถูกรวมเป็นคลัสเตอร์เดียว หากจุดข้อมูลถัดไปอยู่ที่ 11:16 น. จุดข้อมูลนั้นจะเริ่มคลัสเตอร์ใหม่

**ตัวเลือกการกำหนดค่า:**

- **การรวมค่า**: การดำเนินการกับค่าในแต่ละคลัสเตอร์:
  - Min: ค่าต่ำสุด
  - Max: ค่าสูงสุด
  - Average: ค่าเฉลี่ยของค่าทั้งหมด
  - Sum: ผลรวมของค่าทั้งหมด
  - Count: จำนวนจุดข้อมูล
  - Variance: ความแปรปรวนทางสถิติ
  - Standard Deviation: ส่วนเบี่ยงเบนมาตรฐานทางสถิติ

- **หน่วยเกณฑ์**: หน่วยเวลาสำหรับความใกล้กันในการจัดกลุ่ม (วินาที นาที ชั่วโมง วัน สัปดาห์ เดือน หรือปี)

- **ตัวคูณ**: จำนวนหน่วยขนาดเกณฑ์ที่ใช้กำหนดเกณฑ์การจัดกลุ่ม (เช่น ตัวคูณ 2 กับหน่วยเกณฑ์ "ชั่วโมง" = เกณฑ์การจัดกลุ่ม 2 ชั่วโมง)
  	]],
  	["tr"] = [[
Yakındaki veri noktalarını kümeler halinde gruplandırır ve her kümeyi tek bir değerde toplar. İşlev, her veri noktası arasında verilen zaman eşiğinden daha kısa süre bulunan tüm veri noktası dizilerini gruplandırır.

Örneğin, 1 saatlik eşikle saat 09:00, 09:30 ve 10:15'te birkaç veri noktanız varsa bunlar tek bir kümede gruplandırılır. Sonraki veri noktası 11:16'da olsaydı yeni bir küme başlatırdı.

**Yapılandırma Seçenekleri:**

- **Toplama**: Her kümedeki değerlere uygulanacak işlem:
  - Min: Minimum değer
  - Max: Maksimum değer
  - Average: Tüm değerlerin ortalaması
  - Sum: Tüm değerlerin toplamı
  - Count: Veri noktası sayısı
  - Variance: İstatistiksel varyans
  - Standard Deviation: İstatistiksel standart sapma

- **Eşik Birimleri**: Kümeleme yakınlığı için zaman birimi (saniye, dakika, saat, gün, hafta, ay veya yıl)

- **Çarpan**: Kümeleme eşiğini tanımlayan eşik boyutu birimlerinin sayısı (ör. eşik birimleri "saat" ve çarpan 2 ise = 2 saatlik kümeleme eşiği)
  	]],
  	["uk"] = [[
Групує близькі точки даних у кластери та об’єднує кожен кластер в одне значення. Функція групує будь-яку послідовність точок даних, між якими проміжок часу менший за вказаний поріг.

Наприклад, за порогу в 1 годину кілька точок даних о 9:00, 9:30 і 10:15 буде об’єднано в один кластер. Якщо наступна точка даних буде об 11:16, вона започаткує новий кластер.

**Параметри конфігурації:**

- **Агрегація**: Операція, яку потрібно виконати над значеннями в кожному кластері:
  - Мінімум: Мінімальне значення
  - Максимум: Максимальне значення
  - Середнє: Середнє всіх значень
  - Сума: Сума всіх значень
  - Кількість: Кількість точок даних
  - Дисперсія: Статистична дисперсія
  - Стандартне відхилення: Статистичне стандартне відхилення

- **Одиниці порогу**: Одиниця часу для близькості точок у кластері (секунди, хвилини, години, дні, тижні, місяці або роки)

- **Множник**: Кількість одиниць розміру порогу, що визначають поріг кластеризації (наприклад, множник 2 з одиницями порогу «години» = поріг кластеризації 2 години)
  	]],
  	["vi"] = [[
Nhóm các điểm dữ liệu gần nhau thành các cụm và tổng hợp mỗi cụm thành một giá trị duy nhất. Hàm nhóm mọi chuỗi điểm dữ liệu có khoảng thời gian giữa các điểm liên tiếp nhỏ hơn ngưỡng thời gian đã cho.

Ví dụ: với ngưỡng 1 giờ, nếu bạn có các điểm dữ liệu lúc 9:00, 9:30 và 10:15, chúng sẽ được nhóm thành một cụm duy nhất. Nếu điểm dữ liệu tiếp theo là 11:16, điểm đó sẽ bắt đầu một cụm mới.

**Tùy chọn cấu hình:**

- **Tổng hợp**: Phép toán thực hiện trên các giá trị trong mỗi cụm:
  - Min: Giá trị nhỏ nhất
  - Max: Giá trị lớn nhất
  - Average: Trung bình của tất cả giá trị
  - Sum: Tổng của tất cả giá trị
  - Count: Số lượng điểm dữ liệu
  - Variance: Phương sai thống kê
  - Standard Deviation: Độ lệch chuẩn thống kê

- **Đơn vị ngưỡng**: Đơn vị thời gian để xác định độ gần khi phân cụm (giây, phút, giờ, ngày, tuần, tháng hoặc năm)

- **Hệ số**: Số đơn vị kích thước ngưỡng tạo nên ngưỡng phân cụm (ví dụ: hệ số 2 với đơn vị ngưỡng "giờ" = ngưỡng phân cụm 2 giờ)
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
      id = "threshold",
      name = "_threshold_units",
      options = { "_seconds", "_minutes", "_hours", "_days", "_weeks", "_months", "_years", },
      default = "_hours"
    },
  },
  generator = function(source, config)
    local agg_factory = get_aggregator_factory(config)
    local threshold = get_threshold(config)
    local multiplier = config.multiplier
    local carry = nil

    return function()
      local aggregator = agg_factory()

      local cutoff

      if carry ~= nil then
        aggregator:push(carry)
        cutoff = core.shift(carry, threshold, -multiplier)
        carry = nil
      else
        local ref = source.dp()

        if ref == nil then
          return nil
        end

        aggregator:push(ref)
        cutoff = core.shift(ref, threshold, -multiplier)
      end

      while true do
        local next_dp = source.dp()

        if next_dp == nil then break end

        if next_dp.timestamp >= cutoff.timestamp then
          aggregator:push(next_dp)
          cutoff = core.shift(next_dp, threshold, -multiplier)
        else
          carry = next_dp
          break
        end
      end

      return aggregator:run()
    end
  end
}
