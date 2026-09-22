-- Lua Function to filter data points within a specified period from now
-- This function calculates a cutoff timestamp by subtracting a specified period from "now" and only passes through data points at or after that cutoff
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local core = require("tng.core")

return {
    -- Configuration metadata
    id = "filter-within-period",
    version = "1.0.1",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
    	["en"] = "Filter Within Period",
    	["af"] = "Filtreer Binne Tydperk",
    	["sq"] = "Filtro brenda periudhës",
    	["am"] = "በጊዜ ክልል ውስጥ አጣራ",
    	["hy"] = "Զտել ժամանակահատվածում",
    	["az"] = "Müddət daxilindəkiləri süzgəcdən keçir",
    	["bn"] = "সময়ের মধ্যে ফিল্টার করুন",
    	["eu"] = "Iragazi aldiaren barruan",
    	["be"] = "Фільтраваць у межах перыяду",
    	["bg"] = "Филтриране в рамките на период",
    	["my"] = "ကာလအတွင်း စစ်ထုတ်ရန်",
    	["ca"] = "Filtra dins del període",
    	["zh-Hans"] = "筛选时间段内",
    	["zh-Hant"] = "篩選期間內",
    	["hr"] = "Filtriraj unutar razdoblja",
    	["cs"] = "Filtrovat v období",
    	["da"] = "Filtrér inden for periode",
    	["nl"] = "Binnen periode filteren",
    	["et"] = "Filtreeri perioodi piires",
    	["fil"] = "Salain sa Loob ng Panahon",
    	["fi"] = "Suodata ajanjakson sisältä",
    	["fr"] = "Filtrer dans une période",
    	["gl"] = "Filtrar dentro do período",
    	["ka"] = "პერიოდში ფილტრაცია",
    	["de"] = "Innerhalb eines Zeitraums filtern",
    	["el"] = "Φιλτράρισμα εντός περιόδου",
    	["gu"] = "સમયગાળામાં ફિલ્ટર કરો",
    	["hi"] = "अवधि के भीतर फ़िल्टर करें",
    	["hu"] = "Szűrés időszakon belül",
    	["is"] = "Sía innan tímabils",
    	["id"] = "Saring dalam Periode",
    	["it"] = "Filtra entro il periodo",
    	["ja"] = "期間内をフィルタ",
    	["kn"] = "ಅವಧಿಯೊಳಗೆ ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Кезең ішінде сүзу",
    	["km"] = "ត្រងក្នុងរយៈពេល",
    	["ko"] = "기간 내 필터링",
    	["ky"] = "Мезгил ичиндегини чыпкалоо",
    	["lo"] = "ກັ່ນຕອງພາຍໃນໄລຍະເວລາ",
    	["lv"] = "Filtrēt periodā",
    	["lt"] = "Filtruoti laikotarpiu",
    	["mk"] = "Филтрирај во период",
    	["ms"] = "Tapis Dalam Tempoh",
    	["ml"] = "കാലയളവിനുള്ളിലെവ ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "कालावधीतील फिल्टर",
    	["mn"] = "Хугацааны дотор шүүх",
    	["ne"] = "अवधिभित्र फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer innenfor periode",
    	["pl"] = "Filtruj w obrębie okresu",
    	["pt"] = "Filtrar dentro do período",
    	["pa"] = "ਅਵਧੀ ਅੰਦਰ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează în perioadă",
    	["rm"] = "Filtrar entaifer il temp",
    	["ru"] = "Фильтр за период",
    	["sr"] = "Filtriraj unutar perioda",
    	["si"] = "කාල පරාසය තුළ පෙරහන් කරන්න",
    	["sk"] = "Filtrovať v období",
    	["sl"] = "Filtriraj znotraj obdobja",
    	["es"] = "Filtrar dentro del período",
    	["sw"] = "Chuja Ndani ya Kipindi",
    	["sv"] = "Filtrera inom period",
    	["ta"] = "காலப்பகுதிக்குள் வடிகட்டு",
    	["te"] = "కాలవ్యవధిలో ఫిల్టర్ చేయి",
    	["th"] = "กรองภายในช่วงเวลา",
    	["tr"] = "Dönem İçinde Filtrele",
    	["uk"] = "Фільтрувати в межах періоду",
    	["vi"] = "Lọc trong khoảng thời gian",
    },

    description = {
    	["en"] = [[
Filters data points to only include those within the specified time period from now.
The cutoff is calculated by subtracting the specified period from the current time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Number of periods to include (e.g., 2 weeks = Week + Multiplier 2)

For example, with Period=Day and Multiplier=7, only data from within the last 7 days will pass through.
    	]],
    	["af"] = [[
Filtreer datapunte om slegs dié in te sluit wat binne die gespesifiseerde tydperk vanaf nou val.
Die afsnytyd word bereken deur die gespesifiseerde tydperk van die huidige tyd af te trek.

Konfigurasie:
- **Tydperk**: Tydeenheid (dag, week, maand, jaar)
- **Tydperkvermenigvuldiger**: Aantal tydperke om in te sluit (bv. 2 weke = week + vermenigvuldiger 2)

Byvoorbeeld, met Tydperk=Dag en Vermenigvuldiger=7 sal slegs data van die afgelope 7 dae deurgegee word.
    	]],
    	["sq"] = [[
Filtron pikat e të dhënave për të përfshirë vetëm ato brenda periudhës kohore të specifikuar nga tani.
Kufiri llogaritet duke zbritur periudhën e specifikuar nga koha aktuale.

Konfigurimi:
- **Periudha**: Njësia e periudhës kohore (Ditë, Javë, Muaj, Vit)
- **Shumëzuesi i periudhës**: Numri i periudhave për t'u përfshirë (p.sh., 2 javë = Javë + Shumëzuesi 2)

Për shembull, me Periudha=Ditë dhe Shumëzuesi=7, do të kalojnë vetëm të dhënat e 7 ditëve të fundit.
    	]],
    	["am"] = [[
ከአሁኑ ጊዜ ጀምሮ በተገለጸው የጊዜ ክልል ውስጥ ያሉትን ብቻ ለማካተት የውሂብ ነጥቦችን ያጣራል።
መቁረጫው የሚሰላው የተገለጸውን ጊዜ ከአሁኑ ጊዜ በመቀነስ ነው።

ውቅር፦
- **ጊዜ ክልል**፦ የጊዜ ክፍል (ቀን፣ ሳምንት፣ ወር፣ ዓመት)
- **የጊዜ ክልል ብዜት**፦ የሚካተቱ የጊዜ ክፍሎች ብዛት (ለምሳሌ፣ 2 ሳምንታት = ሳምንት + ብዜት 2)

ለምሳሌ፣ ጊዜ ክልል=ቀን እና ብዜት=7 ከሆነ፣ ባለፉት 7 ቀናት ውስጥ ያለ ውሂብ ብቻ ያልፋል።
    	]],
    	["hy"] = [[
Զտում է տվյալակետերը՝ ներառելով միայն այն տվյալակետերը, որոնք այժմից հաշված նշված ժամանակահատվածում են։
Վերջնակետը հաշվարկվում է ընթացիկ ժամանակից նշված ժամանակահատվածը հանելով։

Կազմաձևում՝
- **Ժամանակահատված**․ ժամանակահատվածի միավորը (օր, շաբաթ, ամիս, տարի)
- **Ժամանակահատվածի բազմապատկիչ**․ ներառվող ժամանակահատվածների քանակը (օրինակ՝ 2 շաբաթ = Շաբաթ + Բազմապատկիչ 2)

Օրինակ՝ Ժամանակահատված=Օր և Բազմապատկիչ=7 դեպքում կանցնեն միայն վերջին 7 օրվա տվյալները։
    	]],
    	["az"] = [[
Məlumat nöqtələrini yalnız indidən etibarən göstərilən zaman müddəti daxilində olanları daxil edəcək şəkildə süzgəcdən keçirir.
Kəsim vaxtı göstərilən müddətin cari vaxtdan çıxılması ilə hesablanır.

Konfiqurasiya:
- **Müddət**: Zaman müddəti vahidi (Gün, Həftə, Ay, İl)
- **Müddət vurğusu**: Daxil ediləcək müddətlərin sayı (məsələn, 2 həftə = Həftə + Vurğu 2)

Məsələn, Müddət=Gün və Vurğu=7 olduqda yalnız son 7 günün məlumatları keçir.
    	]],
    	["bn"] = [[
এখন থেকে নির্দিষ্ট সময়সীমার মধ্যে থাকা ডেটা পয়েন্টগুলোই রাখে।
বর্তমান সময় থেকে নির্দিষ্ট সময়সীমা বিয়োগ করে কাটঅফ গণনা করা হয়।

কনফিগারেশন:
- **সময়সীমা**: সময়সীমার একক (দিন, সপ্তাহ, মাস, বছর)
- **সময়সীমার গুণক**: অন্তর্ভুক্ত করার সময়সীমার সংখ্যা (যেমন, ২ সপ্তাহ = সপ্তাহ + গুণক ২)

উদাহরণস্বরূপ, সময়সীমা=দিন এবং গুণক=৭ হলে, কেবল গত ৭ দিনের ডেটাই পাস করবে।
    	]],
    	["eu"] = [[
Datu-puntuak iragazten ditu, hemendik zehaztutako denbora-aldira artekoak soilik sartzeko.
Muga-denbora zehaztutako aldia uneko unetik kenduta kalkulatzen da.

Konfigurazioa:
- **Aldia**: Denbora-aldiaren unitatea (eguna, astea, hilabetea, urtea)
- **Aldiaren biderkatzailea**: Sartu beharreko aldien kopurua (adib., 2 aste = Astea + 2ko biderkatzailea)

Adibidez, Aldia=Eguna eta Biderkatzailea=7 izanik, azken 7 egunetako datuak soilik igaroko dira.
    	]],
    	["be"] = [[
Фільтруе кропкі даных, пакідаючы толькі тыя, што знаходзяцца ў зададзеным перыядзе ад цяперашняга моманту.
Гранічны час вылічваецца адніманнем зададзенага перыяду ад бягучага часу.

Канфігурацыя:
- **Перыяд**: адзінка перыяду (дзень, тыдзень, месяц, год)
- **Множнік перыяду**: колькасць перыядаў для ўключэння (напрыклад, 2 тыдні = тыдзень + множнік 2)

Напрыклад, пры Перыядзе=дзень і Множніку=7 будуць прапушчаныя толькі даныя за апошнія 7 дзён.
    	]],
    	["bg"] = [[
Филтрира точките от данни така, че да включва само тези в зададения период назад от настоящия момент.
Крайният момент се изчислява чрез изваждане на зададения период от текущото време.

Конфигурация:
- **Период**: Единица за времевия период (ден, седмица, месец, година)
- **Множител на периода**: Брой периоди за включване (напр. 2 седмици = Седмица + Множител 2)

Например при Период=Ден и Множител=7 ще преминат само данните от последните 7 дни.
    	]],
    	["my"] = [[
ယခုအချိန်မှ သတ်မှတ်ထားသော အချိန်ကာလအတွင်းရှိသည့် ဒေတာအမှတ်များကိုသာ ထည့်သွင်းရန် စစ်ထုတ်သည်။
ဖြတ်တောက်ချိန်ကို လက်ရှိအချိန်မှ သတ်မှတ်ထားသော ကာလကို နုတ်၍ တွက်ချက်သည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **ကာလ**: အချိန်ကာလယူနစ် (နေ့၊ အပတ်၊ လ၊ နှစ်)
- **ကာလမြှောက်ကိန်း**: ထည့်သွင်းမည့် ကာလအရေအတွက် (ဥပမာ၊ ၂ ပတ် = အပတ် + မြှောက်ကိန်း ၂)

ဥပမာအားဖြင့်၊ ကာလ=နေ့ နှင့် မြှောက်ကိန်း=၇ ဖြစ်ပါက လွန်ခဲ့သော ၇ ရက်အတွင်းရှိ ဒေတာများသာ ဖြတ်သန်းမည်။
    	]],
    	["ca"] = [[
Filtra els punts de dades per incloure només els que es troben dins del període de temps especificat a partir d’ara.
El límit es calcula restant el període especificat de l’hora actual.

Configuració:
- **Període**: Unitat del període de temps (dia, setmana, mes, any)
- **Multiplicador del període**: Nombre de períodes que cal incloure (p. ex., 2 setmanes = setmana + multiplicador 2)

Per exemple, amb Període=Dia i Multiplicador=7, només passaran les dades dels darrers 7 dies.
    	]],
    	["zh-Hans"] = [[
仅筛选从现在起指定时间段内的数据点。
截止时间通过从当前时间减去指定时间段计算得出。

配置：
- **时间段**：时间段单位（日、周、月、年）
- **时间段倍数**：要包含的时间段数量（例如，2 周 = 周 + 倍数 2）

例如，时间段=日、倍数=7 时，只有最近 7 天内的数据会通过。
    	]],
    	["zh-Hant"] = [[
篩選資料點，只包含從現在起指定時間期間內的資料點。
截止時間會以目前時間減去指定期間計算。

設定：
- **期間**：時間期間單位（日、週、月、年）
- **期間倍數**：要包含的期間數量（例如 2 週 = 週 + 倍數 2）

例如，當期間=日且倍數=7 時，只有最近 7 天內的資料會通過。
    	]],
    	["hr"] = [[
Filtrira podatkovne točke tako da uključuje samo one unutar navedenog razdoblja od sada.
Granično vrijeme izračunava se oduzimanjem navedenog razdoblja od trenutačnog vremena.

Konfiguracija:
- **Razdoblje**: Jedinica vremenskog razdoblja (dan, tjedan, mjesec, godina)
- **Množitelj razdoblja**: Broj razdoblja koja treba uključiti (npr. 2 tjedna = tjedan + množitelj 2)

Na primjer, s Razdobljem=dan i Množiteljem=7, proći će samo podaci iz posljednjih 7 dana.
    	]],
    	["cs"] = [[
Filtruje datové body tak, aby zahrnoval pouze ty, které spadají do zadaného období od nynějška.
Mezní čas se vypočítá odečtením zadaného období od aktuálního času.

Konfigurace:
- **Období**: Jednotka časového období (den, týden, měsíc, rok)
- **Násobitel období**: Počet zahrnutých období (např. 2 týdny = týden + násobitel 2)

Například při Období=den a Násobitel=7 projdou pouze data z posledních 7 dnů.
    	]],
    	["da"] = [[
Filtrerer datapunkter, så kun dem inden for den angivne tidsperiode fra nu medtages.
Skæringstidspunktet beregnes ved at trække den angivne periode fra det aktuelle tidspunkt.

Konfiguration:
- **Periode**: Tidsperiodeenhed (dag, uge, måned, år)
- **Periodemultiplikator**: Antallet af perioder, der skal medtages (f.eks. 2 uger = uge + multiplikator 2)

For eksempel: Med Periode=Dag og Multiplikator=7 går kun data fra de seneste 7 dage videre.
    	]],
    	["nl"] = [[
Filtert datapunten zodat alleen datapunten binnen de opgegeven periode vanaf nu worden opgenomen.
De grens wordt berekend door de opgegeven periode af te trekken van de huidige tijd.

Configuratie:
- **Periode**: Eenheid van de periode (dag, week, maand, jaar)
- **Periodevermenigvuldiger**: Aantal perioden om op te nemen (bijv. 2 weken = week + vermenigvuldiger 2)

Bijvoorbeeld: met Periode=dag en Vermenigvuldiger=7 worden alleen gegevens uit de afgelopen 7 dagen doorgelaten.
    	]],
    	["et"] = [[
Jätab alles ainult määratud ajavahemikku kuuluvad andmepunktid alates praegusest hetkest.
Lõppaeg arvutatakse määratud perioodi lahutamisel praegusest ajast.

Seadistus:
- **Periood**: ajavahemiku ühik (päev, nädal, kuu, aasta)
- **Perioodi kordaja**: kaasatavate perioodide arv (nt 2 nädalat = nädal + kordaja 2)

Näiteks perioodi „päev” ja kordaja 7 korral läbivad ainult viimase 7 päeva andmed.
    	]],
    	["fil"] = [[
Sinasala ang mga data point upang isama lamang ang mga nasa tinukoy na panahon mula ngayon.
Kinakalkula ang cutoff sa pagbabawas ng tinukoy na panahon mula sa kasalukuyang oras.

Configuration:
- **Period**: Yunit ng panahon (Day, Week, Month, Year)
- **Period Multiplier**: Bilang ng mga panahong isasama (hal., 2 linggo = Week + Multiplier 2)

Halimbawa, kapag Period=Day at Multiplier=7, data lamang mula sa nakaraang 7 araw ang magpapatuloy.
    	]],
    	["fi"] = [[
Suodattaa datapisteet niin, että mukaan otetaan vain määritetyn ajanjakson sisällä tästä hetkestä olevat pisteet.
Katkaisuaika lasketaan vähentämällä määritetty ajanjakso nykyhetkestä.

Määritys:
- **Ajanjakso**: Ajanjakson yksikkö (päivä, viikko, kuukausi, vuosi)
- **Ajanjakson kerroin**: Mukaan otettavien ajanjaksojen määrä (esim. 2 viikkoa = viikko + kerroin 2)

Esimerkiksi kun Ajanjakso=päivä ja Kerroin=7, läpi päästetään vain viimeisten 7 päivän tiedot.
    	]],
    	["fr"] = [[
Filtre les points de données pour ne conserver que ceux situés dans la période indiquée à partir de maintenant.
La limite est calculée en soustrayant la période indiquée de l’heure actuelle.

Configuration :
- **Période** : Unité de période (jour, semaine, mois, année)
- **Multiplicateur de période** : Nombre de périodes à inclure (par ex. 2 semaines = semaine + multiplicateur 2)

Par exemple, avec Période = jour et Multiplicateur = 7, seules les données des 7 derniers jours sont conservées.
    	]],
    	["gl"] = [[
Filtra os puntos de datos para incluír só os que estean dentro do período especificado contado desde agora.
O límite calcúlase restando o período especificado ao momento actual.

Configuración:
- **Período**: Unidade do período temporal (día, semana, mes, ano)
- **Multiplicador do período**: Número de períodos que se incluirán (por exemplo, 2 semanas = Semana + Multiplicador 2)

Por exemplo, con Período=día e Multiplicador=7, só pasarán os datos dos últimos 7 días.
    	]],
    	["ka"] = [[
ფილტრავს მონაცემთა წერტილებს და ტოვებს მხოლოდ ახლანდელი მომენტიდან მითითებულ პერიოდზე ნაკლებ დროში მდებარე წერტილებს.
საბოლოო დრო გამოითვლება მიმდინარე დროიდან მითითებული პერიოდის გამოკლებით.

კონფიგურაცია:
- **პერიოდი**: დროის პერიოდის ერთეული (დღე, კვირა, თვე, წელი)
- **პერიოდის გამამრავლებელი**: ჩასართავი პერიოდების რაოდენობა (მაგ., 2 კვირა = კვირა + გამამრავლებელი 2)

მაგალითად, Period=Day და Multiplier=7 შემთხვევაში გაივლის მხოლოდ ბოლო 7 დღის მონაცემები.
    	]],
    	["de"] = [[
Filtert Datenpunkte so, dass nur diejenigen enthalten sind, die innerhalb des angegebenen Zeitraums ab jetzt liegen.
Der Stichtag wird berechnet, indem der angegebene Zeitraum von der aktuellen Zeit abgezogen wird.

Konfiguration:
- **Zeitraum**: Zeiteinheit (Tag, Woche, Monat, Jahr)
- **Zeitraummultiplikator**: Anzahl der einzuschließenden Zeiträume (z. B. 2 Wochen = Woche + Multiplikator 2)

Bei Zeitraum=Tag und Multiplikator=7 werden beispielsweise nur Daten der letzten 7 Tage weitergegeben.
    	]],
    	["el"] = [[
Φιλτράρει τα σημεία δεδομένων ώστε να περιλαμβάνονται μόνο όσα βρίσκονται εντός της καθορισμένης χρονικής περιόδου από τώρα.
Το όριο υπολογίζεται αφαιρώντας την καθορισμένη περίοδο από την τρέχουσα ώρα.

Διαμόρφωση:
- **Περίοδος**: Μονάδα χρονικής περιόδου (Ημέρα, Εβδομάδα, Μήνας, Έτος)
- **Πολλαπλασιαστής περιόδου**: Αριθμός περιόδων προς συμπερίληψη (π.χ. 2 εβδομάδες = Εβδομάδα + Πολλαπλασιαστής 2)

Για παράδειγμα, με Περίοδο=Ημέρα και Πολλαπλασιαστή=7, θα περνούν μόνο δεδομένα των τελευταίων 7 ημερών.
    	]],
    	["gu"] = [[
ડેટા પોઇન્ટ્સને હમણાથી નિર્દિષ્ટ સમયગાળાની અંદરના પોઇન્ટ્સ સુધી મર્યાદિત કરે છે.
કટઑફની ગણતરી વર્તમાન સમયમાંથી નિર્દિષ્ટ સમયગાળો બાદ કરીને થાય છે.

ગોઠવણી:
- **સમયગાળો**: સમયગાળાનો એકમ (દિવસ, અઠવાડિયું, મહિનો, વર્ષ)
- **સમયગાળા ગુણક**: સામેલ કરવાના સમયગાળાઓની સંખ્યા (દા.ત., 2 અઠવાડિયા = અઠવાડિયું + ગુણક 2)

ઉદાહરણ તરીકે, સમયગાળો=દિવસ અને ગુણક=7 હોય, તો માત્ર છેલ્લા 7 દિવસનો ડેટા પસાર થશે.
    	]],
    	["hi"] = [[
डेटा पॉइंट को केवल अब से निर्दिष्ट समय अवधि के भीतर वाले पॉइंट तक सीमित करता है।
कटऑफ़ की गणना वर्तमान समय में से निर्दिष्ट अवधि घटाकर की जाती है।

कॉन्फ़िगरेशन:
- **अवधि**: समय अवधि इकाई (दिन, सप्ताह, महीना, वर्ष)
- **अवधि गुणक**: शामिल की जाने वाली अवधियों की संख्या (जैसे, 2 सप्ताह = सप्ताह + गुणक 2)

उदाहरण के लिए, अवधि=दिन और गुणक=7 के साथ, केवल पिछले 7 दिनों का डेटा आगे जाएगा।
    	]],
    	["hu"] = [[
Csak azokat az adatpontokat tartja meg, amelyek a jelenlegi időponttól számított megadott időszakon belül vannak.
A határidőt úgy számítja ki, hogy a megadott időszakot levonja a jelenlegi időpontból.

Konfiguráció:
- **Időszak**: Időszakegység (nap, hét, hónap, év)
- **Időszak szorzója**: A belefoglalni kívánt időszakok száma (például 2 hét = Hét + 2-es szorzó)

Például Nap időszak és 7-es szorzó esetén csak az elmúlt 7 nap adatai haladnak tovább.
    	]],
    	["is"] = [[
Síar gagnapunkta þannig að aðeins þeir sem eru innan tilgreinds tímabils frá núinu séu með.
Lokatíminn er reiknaður með því að draga tilgreint tímabil frá núverandi tíma.

Stillingar:
- **Tímabil**: Eining tímabils (dagur, vika, mánuður, ár)
- **Margfeldi tímabils**: Fjöldi tímabila sem taka á með (t.d. 2 vikur = vika + margfeldi 2)

Til dæmis, með Tímabil=Dagur og Margfeldi=7 fara aðeins gögn frá síðustu 7 dögum áfram.
    	]],
    	["id"] = [[
Menyaring titik data agar hanya mencakup titik data dalam periode waktu yang ditentukan dari sekarang.
Batas waktu dihitung dengan mengurangi periode yang ditentukan dari waktu saat ini.

Konfigurasi:
- **Periode**: Satuan periode waktu (Hari, Minggu, Bulan, Tahun)
- **Pengali Periode**: Jumlah periode yang disertakan (misalnya, 2 minggu = Minggu + Pengali 2)

Misalnya, dengan Periode=Hari dan Pengali=7, hanya data dari 7 hari terakhir yang akan diteruskan.
    	]],
    	["it"] = [[
Filtra i punti dati includendo solo quelli all'interno del periodo di tempo specificato a partire da ora.
Il limite viene calcolato sottraendo il periodo specificato dall'ora corrente.

Configurazione:
- **Periodo**: unità del periodo di tempo (giorno, settimana, mese, anno)
- **Moltiplicatore del periodo**: numero di periodi da includere (ad es. 2 settimane = settimana + moltiplicatore 2)

Ad esempio, con Periodo=giorno e Moltiplicatore=7, passeranno solo i dati degli ultimi 7 giorni.
    	]],
    	["ja"] = [[
現在から指定した期間内にあるデータポイントだけに絞り込みます。
カットオフは、現在時刻から指定した期間を引いて計算されます。

設定:
- **期間**: 期間の単位（日、週、月、年）
- **期間の乗数**: 含める期間数（例: 2週間 = 週 + 乗数2）

たとえば、期間=日、乗数=7の場合、過去7日以内のデータだけが通過します。
    	]],
    	["kn"] = [[
ಈಗಿನಿಂದ ನಿರ್ದಿಷ್ಟ ಸಮಯಾವಧಿಯೊಳಗಿನ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಮಾತ್ರ ಒಳಗೊಂಡಂತೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ.
ನಿರ್ದಿಷ್ಟ ಅವಧಿಯನ್ನು ಪ್ರಸ್ತುತ ಸಮಯದಿಂದ ಕಡಿತಗೊಳಿಸಿ ಕಟ್‌ಆಫ್ ಲೆಕ್ಕಹಾಕಲಾಗುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಅವಧಿ**: ಸಮಯಾವಧಿ ಘಟಕ (ದಿನ, ವಾರ, ತಿಂಗಳು, ವರ್ಷ)
- **ಅವಧಿ ಗುಣಕ**: ಒಳಗೊಂಡಿರಬೇಕಾದ ಅವಧಿಗಳ ಸಂಖ್ಯೆ (ಉದಾ., 2 ವಾರಗಳು = ವಾರ + ಗುಣಕ 2)

ಉದಾಹರಣೆಗೆ, ಅವಧಿ=ದಿನ ಮತ್ತು ಗುಣಕ=7 ಇದ್ದರೆ, ಕಳೆದ 7 ದಿನಗಳೊಳಗಿನ ಡೇಟಾ ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತದೆ.
    	]],
    	["kk"] = [[
Дерек нүктелерін осы сәттен бастап көрсетілген уақыт кезеңінің ішіндегілерін ғана қамтитындай сүзеді.
Шек ағымдағы уақыттан көрсетілген кезеңді шегеру арқылы есептеледі.

Конфигурация:
- **Кезең**: Уақыт кезеңінің бірлігі (күн, апта, ай, жыл)
- **Кезең көбейткіші**: Қамтылатын кезеңдер саны (мысалы, 2 апта = апта + көбейткіш 2)

Мысалы, Кезең=күн және Көбейткіш=7 болса, тек соңғы 7 күндегі деректер өтеді.
    	]],
    	["km"] = [[
ត្រងចំណុចទិន្នន័យ ដោយរាប់បញ្ចូលតែចំណុចដែលស្ថិតក្នុងរយៈពេលដែលបានបញ្ជាក់ចាប់ពីពេលនេះ។
កាលបរិច្ឆេទកាត់ត្រូវបានគណនាដោយដករយៈពេលដែលបានបញ្ជាក់ចេញពីពេលបច្ចុប្បន្ន។

ការកំណត់រចនា៖
- **រយៈពេល**៖ ឯកតារយៈពេល (ថ្ងៃ សប្តាហ៍ ខែ ឆ្នាំ)
- **មេគុណរយៈពេល**៖ ចំនួនរយៈពេលដែលត្រូវរាប់បញ្ចូល (ឧ. ២ សប្តាហ៍ = សប្តាហ៍ + មេគុណ ២)

ឧទាហរណ៍ ប្រសិនបើ Period=Day និង Multiplier=7 មានតែទិន្នន័យក្នុងរយៈពេល ៧ ថ្ងៃចុងក្រោយប៉ុណ្ណោះដែលនឹងឆ្លងកាត់។
    	]],
    	["ko"] = [[
지금부터 지정한 기간 이내에 있는 데이터 포인트만 남깁니다.
현재 시각에서 지정한 기간을 빼서 기준 시점을 계산합니다.

구성:
- **기간**: 기간 단위(일, 주, 월, 년)
- **기간 배수**: 포함할 기간 수(예: 2주 = 주 + 배수 2)

예를 들어 기간=일, 배수=7이면 최근 7일 이내의 데이터만 통과합니다.
    	]],
    	["ky"] = [[
Маалымат чекиттеринин ичинен азыркы убакыттан көрсөтүлгөн мезгилдин ичинде болгондорун гана калат.
Чек учурдагы убакыттан көрсөтүлгөн мезгилди алып салуу менен эсептелет.

Тууралоо:
- **Мезгил**: Убакыт мезгилинин бирдиги (Күн, Жума, Ай, Жыл)
- **Мезгил көбөйткүчү**: Кошулуучу мезгилдердин саны (мисалы, 2 жума = Жума + 2 көбөйткүчү)

Мисалы, Мезгил=Күн жана Көбөйткүч=7 болсо, акыркы 7 күндүн ичиндеги маалыматтар гана өткөрүлөт.
    	]],
    	["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນໃຫ້ລວມສະເພາະຈຸດທີ່ຢູ່ພາຍໃນໄລຍະເວລາທີ່ກຳນົດນັບຈາກຕອນນີ້.
ຈຸດຕັດຈະຄຳນວນໂດຍການລົບໄລຍະເວລາທີ່ກຳນົດອອກຈາກເວລາປັດຈຸບັນ.

ການກຳນົດຄ່າ:
- **ໄລຍະເວລາ**: ໜ່ວຍໄລຍະເວລາ (ມື້, ອາທິດ, ເດືອນ, ປີ)
- **ຕົວຄູນໄລຍະເວລາ**: ຈຳນວນໄລຍະເວລາທີ່ຈະລວມ (ຕົວຢ່າງ: 2 ອາທິດ = ອາທິດ + ຕົວຄູນ 2)

ຕົວຢ່າງ, ເມື່ອ ໄລຍະເວລາ=ມື້ ແລະ ຕົວຄູນ=7, ຈະຜ່ານສະເພາະຂໍ້ມູນໃນ 7 ມື້ຜ່ານມາເທົ່ານັ້ນ.
    	]],
    	["lv"] = [[
Filtrē datu punktus, iekļaujot tikai tos, kas atrodas norādītajā laika periodā no pašreizējā brīža.
Nogriešanas laiks tiek aprēķināts, no pašreizējā laika atņemot norādīto periodu.

Konfigurācija:
- **Periods**: Laika perioda vienība (diena, nedēļa, mēnesis, gads)
- **Perioda reizinātājs**: Iekļaujamo periodu skaits (piemēram, 2 nedēļas = nedēļa + reizinātājs 2)

Piemēram, ja Periods=diena un reizinātājs=7, tiks nodoti tikai dati par pēdējām 7 dienām.
    	]],
    	["lt"] = [[
Filtruoja duomenų taškus, palikdama tik esančius nurodytu laikotarpiu nuo dabar.
Riba apskaičiuojama iš dabartinio laiko atėmus nurodytą laikotarpį.

Konfigūracija:
- **Laikotarpis**: Laikotarpio vienetas (diena, savaitė, mėnuo, metai)
- **Laikotarpio daugiklis**: Įtraukiamų laikotarpių skaičius (pvz., 2 savaitės = savaitė + daugiklis 2)

Pavyzdžiui, kai laikotarpis = diena, o daugiklis = 7, bus perduodami tik pastarųjų 7 dienų duomenys.
    	]],
    	["mk"] = [[
Ги филтрира точките на податоци така што ги вклучува само оние во зададениот временски период сметано од сега.
Граничното време се пресметува со одземање на зададениот период од тековното време.

Конфигурација:
- **Период**: Единица на временскиот период (ден, недела, месец, година)
- **Множител на периодот**: Број на периоди што треба да се вклучат (на пр., 2 недели = недела + множител 2)

На пример, со Период=ден и Множител=7, ќе поминат само податоците од последните 7 дена.
    	]],
    	["ms"] = [[
Menapis titik data supaya hanya yang berada dalam tempoh masa yang ditentukan dari sekarang disertakan.
Tarikh pemotongan dikira dengan menolak tempoh yang ditentukan daripada masa semasa.

Konfigurasi:
- **Tempoh**: Unit tempoh masa (Hari, Minggu, Bulan, Tahun)
- **Pengganda Tempoh**: Bilangan tempoh untuk disertakan (contohnya, 2 minggu = Minggu + Pengganda 2)

Sebagai contoh, dengan Tempoh=Hari dan Pengganda=7, hanya data dari 7 hari terakhir akan diteruskan.
    	]],
    	["ml"] = [[
ഇപ്പോൾ മുതൽ നിർദ്ദിഷ്ട സമയകാലയളവിനുള്ളിലുള്ള ഡാറ്റാ പോയിന്റുകൾ മാത്രം ഉൾപ്പെടുത്തുന്നു.
നിലവിലെ സമയത്തിൽ നിന്ന് നിർദ്ദിഷ്ട കാലയളവ് കുറച്ചാണ് കട്ടോഫ് കണക്കാക്കുന്നത്.

കോൺഫിഗറേഷൻ:
- **Period**: സമയകാലയളവിന്റെ യൂണിറ്റ് (ദിവസം, ആഴ്ച, മാസം, വർഷം)
- **Period Multiplier**: ഉൾപ്പെടുത്തേണ്ട കാലയളവുകളുടെ എണ്ണം (ഉദാ., 2 ആഴ്ച = Week + Multiplier 2)

ഉദാഹരണത്തിന്, Period=Day, Multiplier=7 ആണെങ്കിൽ, കഴിഞ്ഞ 7 ദിവസത്തിനുള്ളിലെ ഡാറ്റ മാത്രമേ കടന്നുപോകൂ.
    	]],
    	["mr"] = [[
आतापासून निर्दिष्ट केलेल्या कालावधीत असलेले डेटा पॉइंट्सच समाविष्ट करते.
कटऑफची गणना सध्याच्या वेळेतून निर्दिष्ट कालावधी वजा करून केली जाते.

कॉन्फिगरेशन:
- **कालावधी**: कालावधीचे एकक (दिवस, आठवडा, महिना, वर्ष)
- **कालावधी गुणक**: समाविष्ट करायच्या कालावधींची संख्या (उदा., 2 आठवडे = आठवडा + गुणक 2)

उदाहरणार्थ, Period=Day आणि Multiplier=7 असल्यास, मागील 7 दिवसांतील डेटाच पुढे पाठवला जाईल.
    	]],
    	["mn"] = [[
Одоогоос тооцсон заасан хугацааны доторх өгөгдлийн цэгүүдийг л үлдээнэ.
Таслах хугацааг одоогийн цагаас заасан хугацааг хасаж тооцно.

Тохиргоо:
- **Хугацаа**: Хугацааны нэгж (өдөр, долоо хоног, сар, жил)
- **Хугацааны үржүүлэгч**: Оруулах хугацааны тоо (жишээ нь, 2 долоо хоног = Долоо хоног + үржүүлэгч 2)

Жишээлбэл, Хугацаа=Өдөр, Үржүүлэгч=7 бол зөвхөн сүүлийн 7 хоногийн өгөгдөл нэвтэрнэ.
    	]],
    	["ne"] = [[
अहिलेबाट निर्दिष्ट समय अवधिभित्र भएका डेटा बिन्दुहरू मात्र राखेर फिल्टर गर्छ।
कटअफ निर्दिष्ट अवधि वर्तमान समयबाट घटाएर गणना गरिन्छ।

कन्फिगरेसन:
- **अवधि**: समय अवधि एकाइ (दिन, हप्ता, महिना, वर्ष)
- **अवधि गुणक**: समावेश गर्ने अवधिहरूको संख्या (जस्तै, २ हप्ता = हप्ता + गुणक २)

उदाहरणका लागि, अवधि=दिन र गुणक=७ हुँदा पछिल्ला ७ दिनभित्रका डेटा मात्र अघि पठाइन्छन्।
    	]],
    	["no"] = [[
Filtrerer datapunkter slik at bare de innenfor den angitte tidsperioden fra nå inkluderes.
Grensen beregnes ved å trekke den angitte perioden fra gjeldende tidspunkt.

Konfigurasjon:
- **Periode**: Tidsperiodens enhet (dag, uke, måned, år)
- **Periodemultiplikator**: Antall perioder som skal inkluderes (f.eks. 2 uker = uke + multiplikator 2)

Med Periode=dag og Multiplikator=7 slipper for eksempel bare data fra de siste 7 dagene gjennom.
    	]],
    	["pl"] = [[
Filtruje punkty danych, pozostawiając tylko te z określonego okresu liczonego wstecz od teraz.
Czas graniczny jest obliczany przez odjęcie określonego okresu od bieżącego czasu.

Konfiguracja:
- **Okres**: Jednostka okresu (dzień, tydzień, miesiąc, rok)
- **Mnożnik okresu**: Liczba uwzględnianych okresów (np. 2 tygodnie = Tydzień + mnożnik 2)

Na przykład przy Okres=dzień i Mnożnik=7 przejdą tylko dane z ostatnich 7 dni.
    	]],
    	["pt"] = [[
Filtra os pontos de dados para incluir apenas os que ocorreram dentro do período especificado a partir de agora.
O limite é calculado subtraindo o período especificado à hora atual.

Configuração:
- **Período**: Unidade do período (dia, semana, mês, ano)
- **Multiplicador do período**: Número de períodos a incluir (por exemplo, 2 semanas = Semana + Multiplicador 2)

Por exemplo, com Período=Dia e Multiplicador=7, apenas os dados dos últimos 7 dias passam.
    	]],
    	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿੱਚੋਂ ਸਿਰਫ਼ ਉਹਨਾਂ ਨੂੰ ਸ਼ਾਮਲ ਕਰਦਾ ਹੈ ਜੋ ਹੁਣ ਤੋਂ ਨਿਰਧਾਰਤ ਸਮੇਂ ਦੀ ਅਵਧੀ ਅੰਦਰ ਹਨ।
ਕਟਆਫ਼ ਦੀ ਗਣਨਾ ਮੌਜੂਦਾ ਸਮੇਂ ਵਿੱਚੋਂ ਨਿਰਧਾਰਤ ਅਵਧੀ ਘਟਾ ਕੇ ਕੀਤੀ ਜਾਂਦੀ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਅਵਧੀ**: ਸਮੇਂ ਦੀ ਇਕਾਈ (ਦਿਨ, ਹਫ਼ਤਾ, ਮਹੀਨਾ, ਸਾਲ)
- **ਅਵਧੀ ਗੁਣਕ**: ਸ਼ਾਮਲ ਕੀਤੀਆਂ ਜਾਣ ਵਾਲੀਆਂ ਅਵਧੀਆਂ ਦੀ ਗਿਣਤੀ (ਉਦਾਹਰਨ ਲਈ, 2 ਹਫ਼ਤੇ = ਹਫ਼ਤਾ + ਗੁਣਕ 2)

ਉਦਾਹਰਨ ਲਈ, ਜੇ ਅਵਧੀ=ਦਿਨ ਅਤੇ ਗੁਣਕ=7 ਹੋਵੇ, ਤਾਂ ਸਿਰਫ਼ ਪਿਛਲੇ 7 ਦਿਨਾਂ ਦਾ ਡਾਟਾ ਅੱਗੇ ਭੇਜਿਆ ਜਾਵੇਗਾ।
    	]],
    	["ro"] = [[
Filtrează punctele de date pentru a le include doar pe cele din perioada specificată până în prezent.
Limita este calculată scăzând perioada specificată din momentul curent.

Configurare:
- **Perioadă**: Unitatea perioadei (zi, săptămână, lună, an)
- **Multiplicator perioadă**: Numărul de perioade de inclus (de exemplu, 2 săptămâni = Săptămână + Multiplicator 2)

De exemplu, cu Perioadă=Zi și Multiplicator=7, vor trece mai departe doar datele din ultimele 7 zile.
    	]],
    	["rm"] = [[
Filtra puncts da datas per includer mo quels entaifer il temp spezificà a partir d'ussa.
La limita vegn calculada sottraind il temp spezificà dal temp actual.

Configuraziun:
- **Temp**: Unità dal temp (di, emna, mais, onn)
- **Multiplicatur dal temp**: Dumber da periodas d'includer (p.ex. 2 emnas = Emna + multiplicitur 2)

Per exempel, cun Temp=Di e multiplicitur=7, mo datas dals ultims 7 dis vegnan laschadas passar.
    	]],
    	["ru"] = [[
Оставляет только точки данных за указанный период до текущего момента.
Граница рассчитывается вычитанием указанного периода из текущего времени.

Конфигурация:
- **Период**: Единица периода (день, неделя, месяц, год)
- **Множитель периода**: Количество периодов для включения (например, 2 недели = неделя + множитель 2)

Например, при Периоде=день и Множителе=7 будут пропущены только данные за последние 7 дней.
    	]],
    	["sr"] = [[
Filtrira tačke podataka tako da obuhvati samo one unutar navedenog vremenskog perioda od sada.
Krajnji rok se izračunava oduzimanjem navedenog perioda od trenutnog vremena.

Konfiguracija:
- **Period**: Jedinica vremenskog perioda (dan, nedelja, mesec, godina)
- **Množilac perioda**: Broj perioda koji se uključuju (npr. 2 nedelje = nedelja + množilac 2)

Na primer, uz Period=dan i Množilac=7, proći će samo podaci iz poslednjih 7 dana.
    	]],
    	["si"] = [[
දැන් සිට නිශ්චිත කාල පරාසය තුළ ඇති දත්ත ලක්ෂ්‍ය පමණක් ඇතුළත් වන ලෙස පෙරහන් කරයි.
කඩඉම් කාලය ගණනය කරන්නේ වත්මන් වේලාවෙන් නිශ්චිත කාල පරාසය අඩු කිරීමෙනි.

වින්‍යාසය:
- **කාල පරාසය**: කාල පරාස ඒකකය (දවස, සතිය, මාසය, වසර)
- **කාල පරාස ගුණකය**: ඇතුළත් කළ යුතු කාල පරාස ගණන (උදා: සති 2 = සතිය + ගුණකය 2)

උදාහරණයක් ලෙස, කාල පරාසය=දවස සහ ගුණකය=7 නම්, පසුගිය දින 7 තුළ ඇති දත්ත පමණක් ඉදිරියට යයි.
    	]],
    	["sk"] = [[
Filtruje údajové body tak, aby obsahovali iba tie, ktoré spadajú do určeného obdobia odteraz.
Hraničný čas sa vypočíta odčítaním určeného obdobia od aktuálneho času.

Konfigurácia:
- **Obdobie**: Jednotka časového obdobia (deň, týždeň, mesiac, rok)
- **Násobiteľ obdobia**: Počet období, ktoré sa majú zahrnúť (napr. 2 týždne = týždeň + násobiteľ 2)

Napríklad pri Obdobie=deň a Násobiteľ=7 prejdú iba údaje z posledných 7 dní.
    	]],
    	["sl"] = [[
Filtrira podatkovne točke tako, da vključi le tiste znotraj določenega časovnega obdobja od zdaj.
Presečni čas se izračuna tako, da se od trenutnega časa odšteje določeno obdobje.

Konfiguracija:
- **Obdobje**: Enota časovnega obdobja (dan, teden, mesec, leto)
- **Množitelj obdobja**: Število obdobij, ki jih je treba vključiti (npr. 2 tedna = teden + množitelj 2)

Če je na primer Obdobje=dan in Množitelj=7, se bodo prepustili le podatki iz zadnjih 7 dni.
    	]],
    	["es"] = [[
Filtra los puntos de datos para incluir solo los que se encuentren dentro del período especificado desde ahora.
El límite se calcula restando el período especificado a la hora actual.

Configuración:
- **Período**: Unidad del período (día, semana, mes, año)
- **Multiplicador del período**: Número de períodos que se incluirán (por ejemplo, 2 semanas = Semana + Multiplicador 2)

Por ejemplo, con Período=día y Multiplicador=7, solo pasarán los datos de los últimos 7 días.
    	]],
    	["sw"] = [[
Huchuja nukta za data ili kujumuisha zile zilizo ndani ya kipindi maalum kutoka sasa pekee.
Kikomo hukokotolewa kwa kutoa kipindi maalum kutoka wakati wa sasa.

Usanidi:
- **Kipindi**: Kipimo cha kipindi cha muda (Siku, Wiki, Mwezi, Mwaka)
- **Kizidishi cha Kipindi**: Idadi ya vipindi vya kujumuisha (kwa mfano, wiki 2 = Wiki + Kizidishi 2)

Kwa mfano, kwa Kipindi=Siku na Kizidishi=7, data ya siku 7 zilizopita pekee ndiyo itapitishwa.
    	]],
    	["sv"] = [[
Filtrerar datapunkter så att endast de inom den angivna tidsperioden från nu inkluderas.
Gränsen beräknas genom att subtrahera den angivna perioden från aktuell tid.

Konfiguration:
- **Period**: Tidsperiodsenhet (dag, vecka, månad, år)
- **Periodmultiplikator**: Antal perioder som ska inkluderas (t.ex. 2 veckor = vecka + multiplikator 2)

Till exempel: med Period=Dag och Multiplikator=7 släpps endast data från de senaste 7 dagarna igenom.
    	]],
    	["ta"] = [[
இப்போதிலிருந்து குறிப்பிட்ட காலப்பகுதிக்குள் உள்ள தரவுப் புள்ளிகளை மட்டும் சேர்க்க வடிகட்டுகிறது.
குறிப்பிட்ட காலப்பகுதியை தற்போதைய நேரத்திலிருந்து கழிப்பதன் மூலம் காலவரம்பு கணக்கிடப்படுகிறது.

உள்ளமைவு:
- **காலப்பகுதி**: காலப்பகுதி அலகு (நாள், வாரம், மாதம், ஆண்டு)
- **காலப்பகுதி பெருக்கி**: சேர்க்க வேண்டிய காலப்பகுதிகளின் எண்ணிக்கை (எ.கா., 2 வாரங்கள் = வாரம் + பெருக்கி 2)

எடுத்துக்காட்டாக, காலப்பகுதி=நாள் மற்றும் பெருக்கி=7 என்றால், கடந்த 7 நாட்களுக்குள் உள்ள தரவு மட்டுமே தொடரும்.
    	]],
    	["te"] = [[
ఇప్పటి నుండి పేర్కొన్న కాలవ్యవధిలో ఉన్న డేటా పాయింట్లను మాత్రమే ఉంచుతుంది.
ప్రస్తుత సమయం నుండి పేర్కొన్న కాలవ్యవధిని తీసివేసి కట్‌ఆఫ్ లెక్కించబడుతుంది.

కాన్ఫిగరేషన్:
- **కాలవ్యవధి**: సమయ కాలవ్యవధి యూనిట్ (రోజు, వారం, నెల, సంవత్సరం)
- **కాలవ్యవధి గుణకం**: చేర్చాల్సిన కాలవ్యవధుల సంఖ్య (ఉదా., 2 వారాలు = వారం + గుణకం 2)

ఉదాహరణకు, కాలవ్యవధి=రోజు, గుణకం=7 అయితే, గత 7 రోజుల్లోని డేటా మాత్రమే కొనసాగుతుంది.
    	]],
    	["th"] = [[
กรองจุดข้อมูลให้เหลือเฉพาะจุดที่อยู่ภายในช่วงเวลาที่ระบุย้อนหลังจากตอนนี้
ระบบคำนวณเวลาตัดโดยลบช่วงเวลาที่ระบุออกจากเวลาปัจจุบัน

การกำหนดค่า:
- **ช่วงเวลา**: หน่วยช่วงเวลา (วัน สัปดาห์ เดือน ปี)
- **ตัวคูณช่วงเวลา**: จำนวนช่วงเวลาที่รวม (เช่น 2 สัปดาห์ = สัปดาห์ + ตัวคูณ 2)

ตัวอย่างเช่น เมื่อช่วงเวลา=วัน และตัวคูณ=7 จะส่งต่อเฉพาะข้อมูลในช่วง 7 วันที่ผ่านมา
    	]],
    	["tr"] = [[
Veri noktalarını yalnızca şu andan itibaren belirtilen zaman dönemi içinde olanları içerecek şekilde filtreler.
Kesim, belirtilen dönem şu andan çıkarılarak hesaplanır.

Yapılandırma:
- **Dönem**: Zaman dönemi birimi (Gün, Hafta, Ay, Yıl)
- **Dönem Çarpanı**: Dahil edilecek dönem sayısı (ör. 2 hafta = Hafta + Çarpan 2)

Örneğin Dönem=Gün ve Çarpan=7 olduğunda yalnızca son 7 gün içindeki veriler geçer.
    	]],
    	["uk"] = [[
Фільтрує точки даних, залишаючи лише ті, що потрапляють у вказаний період від поточного моменту.
Граничний час обчислюється відніманням вказаного періоду від поточного часу.

Конфігурація:
- **Період**: Одиниця періоду часу (день, тиждень, місяць, рік)
- **Множник періоду**: Кількість періодів для включення (наприклад, 2 тижні = тиждень + множник 2)

Наприклад, за Період=день і Множник=7 буде пропущено лише дані за останні 7 днів.
    	]],
    	["vi"] = [[
Lọc các điểm dữ liệu để chỉ giữ lại những điểm nằm trong khoảng thời gian đã chỉ định tính từ hiện tại.
Thời điểm giới hạn được tính bằng cách trừ khoảng thời gian đã chỉ định khỏi thời điểm hiện tại.

Cấu hình:
- **Khoảng thời gian**: Đơn vị khoảng thời gian (Ngày, Tuần, Tháng, Năm)
- **Hệ số khoảng thời gian**: Số khoảng thời gian cần bao gồm (ví dụ: 2 tuần = Tuần + Hệ số 2)

Ví dụ: với Khoảng thời gian=Ngày và Hệ số=7, chỉ dữ liệu trong 7 ngày gần nhất mới được giữ lại.
    	]],
    },

    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_month",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
    },

    -- Generator function
    generator = function(source, config)
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 30

        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]
        if not period then
            error("Invalid period: " .. tostring(period_str))
        end

        -- Calculate cutoff timestamp: now - (period * multiplier)
        local now = core.time()
        local cutoff = core.shift(now, period, -period_multiplier)
        local cutoff_timestamp = cutoff.timestamp

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Only return data points at or after the cutoff
            if data_point.timestamp >= cutoff_timestamp then
                return data_point
            end
        end
    end,
}
