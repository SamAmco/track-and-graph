-- Lua Function to generate periodic data points at regular intervals
-- This function creates data points with value=1 at deterministic timestamps

local core = require("tng.core")
local enum = require("tng.config").enum
local uint = require("tng.config").uint
local instant = require("tng.config").instant

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "periodic-data-points",
    version = "1.1.2",
    inputCount = 0, -- This is a generator, not a transformer
    categories = { "_generators" },
    title = {
    	["en"] = "Periodic Data Points",
    	["af"] = "Periodieke Datapunte",
    	["sq"] = "Pika periodike të të dhënave",
    	["am"] = "ወቅታዊ የውሂብ ነጥቦች",
    	["hy"] = "Պարբերական տվյալակետեր",
    	["az"] = "Dövri məlumat nöqtələri",
    	["bn"] = "পর্যায়ক্রমিক ডেটা পয়েন্ট",
    	["eu"] = "Aldizkako datu-puntuak",
    	["be"] = "Перыядычныя кропкі даных",
    	["bg"] = "Периодични точки от данни",
    	["my"] = "ကာလအလိုက် ဒေတာအမှတ်များ",
    	["ca"] = "Punts de dades periòdics",
    	["zh-Hans"] = "周期性数据点",
    	["zh-Hant"] = "週期性資料點",
    	["hr"] = "Periodične podatkovne točke",
    	["cs"] = "Periodické datové body",
    	["da"] = "Periodiske datapunkter",
    	["nl"] = "Periodieke datapunten",
    	["et"] = "Perioodilised andmepunktid",
    	["fil"] = "Pana-panahong mga Data Point",
    	["fi"] = "Jaksottaiset datapisteet",
    	["fr"] = "Points de données périodiques",
    	["gl"] = "Puntos de datos periódicos",
    	["ka"] = "პერიოდული მონაცემთა წერტილები",
    	["de"] = "Periodische Datenpunkte",
    	["el"] = "Περιοδικά σημεία δεδομένων",
    	["gu"] = "આવર્તક ડેટા પોઇન્ટ્સ",
    	["hi"] = "आवधिक डेटा पॉइंट",
    	["hu"] = "Időszakos adatpontok",
    	["is"] = "Lotubundnir gagnapunktar",
    	["id"] = "Titik Data Berkala",
    	["it"] = "Punti dati periodici",
    	["ja"] = "周期的データポイント",
    	["kn"] = "ಆವರ್ತಕ ಡೇಟಾ ಬಿಂದುಗಳು",
    	["kk"] = "Мерзімдік дерек нүктелері",
    	["km"] = "ចំណុចទិន្នន័យតាមកាលកំណត់",
    	["ko"] = "주기적 데이터 포인트",
    	["ky"] = "Мезгилдик маалымат чекиттери",
    	["lo"] = "ຈຸດຂໍ້ມູນແບບເປັນໄລຍະ",
    	["lv"] = "Periodiskie datu punkti",
    	["lt"] = "Periodiniai duomenų taškai",
    	["mk"] = "Периодични точки на податоци",
    	["ms"] = "Titik Data Berkala",
    	["ml"] = "ആവർത്തനകാല ഡാറ്റാ പോയിന്റുകൾ",
    	["mr"] = "नियतकालिक डेटा पॉइंट्स",
    	["mn"] = "Үечилсэн өгөгдлийн цэгүүд",
    	["ne"] = "आवधिक डेटा बिन्दुहरू",
    	["no"] = "Periodiske datapunkter",
    	["pl"] = "Okresowe punkty danych",
    	["pt"] = "Pontos de dados periódicos",
    	["pa"] = "ਆਵਧਿਕ ਡਾਟਾ ਪੁਆਇੰਟ",
    	["ro"] = "Puncte de date periodice",
    	["rm"] = "Puncts da datas periodics",
    	["ru"] = "Периодические точки данных",
    	["sr"] = "Periodične tačke podataka",
    	["si"] = "කාලාන්තර දත්ත ලක්ෂ්‍ය",
    	["sk"] = "Periodické údajové body",
    	["sl"] = "Periodične podatkovne točke",
    	["es"] = "Puntos de datos periódicos",
    	["sw"] = "Nukta za Data za Vipindi",
    	["sv"] = "Periodiska datapunkter",
    	["ta"] = "காலமுறைத் தரவுப் புள்ளிகள்",
    	["te"] = "కాలానుగత డేటా పాయింట్లు",
    	["th"] = "จุดข้อมูลตามช่วงเวลา",
    	["tr"] = "Dönemsel Veri Noktaları",
    	["uk"] = "Періодичні точки даних",
    	["vi"] = "Điểm dữ liệu theo chu kỳ",
    },
    description = {
    	["en"] = [[
Generates data points with value=1 at regular intervals going back in time.

Configuration:
- **Period**: Time period unit (Day, Week, Month, Year)
- **Period Multiplier**: Generate data point every N periods (e.g., every 2 days)
- **Cutoff**: Stop generating data points at this date/time

Generated data points will have:
- value = 1.0
- label = "" (empty)
- note = "" (empty)
    	]],
    	["af"] = [[
Genereer datapunte met waarde=1 met gereelde tussenposes wat teruggaan in tyd.

Konfigurasie:
- **Periode**: Tydeenheid (Dag, Week, Maand, Jaar)
- **Periodevermenigvuldiger**: Genereer elke N periodes ’n datapunt (bv. elke 2 dae)
- **Afsnytyd**: Hou op om datapunte op hierdie datum/tyd te genereer

Gegenereerde datapunte sal hê:
- value = 1.0
- label = "" (leeg)
- note = "" (leeg)
    	]],
    	["sq"] = [[
Gjeneron pika të të dhënave me value=1 në intervale të rregullta duke u kthyer pas në kohë.

Konfigurimi:
- **Periudha**: Njësia e periudhës kohore (Ditë, Javë, Muaj, Vit)
- **Shumëzuesi i periudhës**: Gjenero një pikë të dhënash çdo N periudha (p.sh., çdo 2 ditë)
- **Kufiri**: Ndalo gjenerimin e pikave të të dhënave në këtë datë/orë

Pikat e gjeneruara të të dhënave do të kenë:
- value = 1.0
- label = "" (bosh)
- note = "" (bosh)
    	]],
    	["am"] = [[
በመደበኛ ክፍተቶች ወደ ያለፈው ጊዜ በመመለስ value=1 ያላቸው የውሂብ ነጥቦችን ይፈጥራል።

ውቅር፦
- **Period**፦ የጊዜ ክፍል (Day, Week, Month, Year)
- **Period Multiplier**፦ በእያንዳንዱ N የጊዜ ክፍሎች የውሂብ ነጥብ ይፍጠር (ለምሳሌ፣ በየ2 ቀኑ)
- **Cutoff**፦ በዚህ ቀን/ሰዓት የውሂብ ነጥቦችን መፍጠር ያቁም

የሚፈጠሩ የውሂብ ነጥቦች፦
- value = 1.0
- label = "" (ባዶ)
- note = "" (ባዶ)
    	]],
    	["hy"] = [[
Ստեղծում է կանոնավոր ընդմիջումներով՝ ժամանակի մեջ հետ գնալով, value=1 արժեքով տվյալակետեր։

Կազմաձևում՝
- **Ժամանակահատված**․ ժամանակահատվածի միավորը (օր, շաբաթ, ամիս, տարի)
- **Ժամանակահատվածի բազմապատկիչ**․ ստեղծել տվյալակետ յուրաքանչյուր N ժամանակահատվածը մեկ (օրինակ՝ երկու օրը մեկ)
- **Վերջնակետ**․ դադարեցնել տվյալակետերի ստեղծումը այս ամսաթվին/ժամին

Ստեղծված տվյալակետերը կունենան՝
- value = 1.0
- label = "" (դատարկ)
- note = "" (դատարկ)
    	]],
    	["az"] = [[
Geriyə doğru müntəzəm intervallarda value=1 qiymətinə malik məlumat nöqtələri yaradır.

Konfiqurasiya:
- **Dövr**: Zaman müddəti vahidi (Gün, Həftə, Ay, İl)
- **Dövr vurğusu**: Hər N dövrdən bir məlumat nöqtəsi yarat (məsələn, hər 2 gündən bir)
- **Kəsim**: Bu tarixdə/vaxtda məlumat nöqtələri yaratmağı dayandır

Yaradılan məlumat nöqtələri:
- value = 1.0
- label = "" (boş)
- note = "" (boş)
    	]],
    	["bn"] = [[
সময়ের পেছনের দিকে নিয়মিত ব্যবধানে value=1 সহ ডেটা পয়েন্ট তৈরি করে।

কনফিগারেশন:
- **সময়সীমা**: সময়সীমার একক (দিন, সপ্তাহ, মাস, বছর)
- **সময়সীমার গুণক**: প্রতি N সময়সীমায় একটি ডেটা পয়েন্ট তৈরি করুন (যেমন, প্রতি ২ দিনে)
- **কাটঅফ**: এই তারিখ/সময়ে ডেটা পয়েন্ট তৈরি করা বন্ধ করুন

তৈরি হওয়া ডেটা পয়েন্টে থাকবে:
- value = 1.0
- label = "" (খালি)
- note = "" (খালি)
    	]],
    	["eu"] = [[
value=1 balioa duten datu-puntuak sortzen ditu, denbora-atzerako tarte erregularretan.

Konfigurazioa:
- **Aldia**: Denbora-aldiaren unitatea (eguna, astea, hilabetea, urtea)
- **Aldiaren biderkatzailea**: Sortu datu-puntu bat N aldiro (adib., 2 egunero)
- **Muga**: Utzi datu-puntuak sortzeari data/ordu honetan

Sortutako datu-puntuek hauek izango dituzte:
- value = 1.0
- label = "" (hutsik)
- note = "" (hutsik)
    	]],
    	["be"] = [[
Стварае кропкі даных са значэннем=1 праз рэгулярныя інтэрвалы ў мінулым.

Канфігурацыя:
- **Перыяд**: адзінка перыяду (дзень, тыдзень, месяц, год)
- **Множнік перыяду**: ствараць кропку даных кожныя N перыядаў (напрыклад, кожныя 2 дні)
- **Гранічны час**: спыніць стварэнне кропак даных у гэтую дату/час

Створаныя кропкі даных будуць мець:
- value = 1.0
- label = "" (пусты)
- note = "" (пустая)
    	]],
    	["bg"] = [[
Генерира точки от данни със стойност=1 на редовни интервали назад във времето.

Конфигурация:
- **Период**: Единица за времевия период (ден, седмица, месец, година)
- **Множител на периода**: Генерира точка от данни на всеки N периода (напр. на всеки 2 дни)
- **Краен момент**: Спира генерирането на точки от данни на тази дата/час

Генерираните точки от данни ще имат:
- value = 1.0
- label = "" (празно)
- note = "" (празно)
    	]],
    	["my"] = [[
အချိန်နောက်ပြန်သွား၍ ပုံမှန်ကြားကာလများတွင် တန်ဖိုး=1 ရှိသော ဒေတာအမှတ်များကို ဖန်တီးသည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **ကာလ**: အချိန်ကာလယူနစ် (နေ့၊ အပတ်၊ လ၊ နှစ်)
- **ကာလမြှောက်ကိန်း**: N ကာလတိုင်း ဒေတာအမှတ်တစ်ခု ဖန်တီးရန် (ဥပမာ၊ ၂ ရက်တိုင်း)
- **ဖြတ်တောက်ချိန်**: ဤရက်စွဲ/အချိန်တွင် ဒေတာအမှတ်ဖန်တီးခြင်းကို ရပ်ရန်

ဖန်တီးထားသော ဒေတာအမှတ်များတွင်:
- value = 1.0
- label = "" (ဗလာ)
- note = "" (ဗလာ)
    	]],
    	["ca"] = [[
Genera punts de dades amb value=1 en intervals regulars cap enrere en el temps.

Configuració:
- **Període**: Unitat del període de temps (dia, setmana, mes, any)
- **Multiplicador del període**: Genera un punt de dades cada N períodes (p. ex., cada 2 dies)
- **Límit**: Deixa de generar punts de dades en aquesta data/hora

Els punts de dades generats tindran:
- value = 1.0
- label = "" (buit)
- note = "" (buit)
    	]],
    	["zh-Hans"] = [[
按固定间隔向过去生成值为 1 的数据点。

配置：
- **周期**：时间段单位（日、周、月、年）
- **周期倍数**：每 N 个周期生成一个数据点（例如每 2 天）
- **截止时间**：在此日期/时间停止生成数据点

生成的数据点将具有：
- value = 1.0
- label = ""（空）
- note = ""（空）
    	]],
    	["zh-Hant"] = [[
以固定間隔向過去產生值為 1 的資料點。

設定：
- **期間**：時間期間單位（日、週、月、年）
- **期間倍數**：每 N 個期間產生一個資料點（例如每 2 天）
- **截止時間**：在此日期／時間停止產生資料點

產生的資料點將具有：
- value = 1.0
- label = ""（空白）
- note = ""（空白）
    	]],
    	["hr"] = [[
Generira podatkovne točke s vrijednošću=1 u pravilnim intervalima unatrag kroz vrijeme.

Konfiguracija:
- **Razdoblje**: Jedinica vremenskog razdoblja (dan, tjedan, mjesec, godina)
- **Množitelj razdoblja**: Generira podatkovnu točku svakih N razdoblja (npr. svaka 2 dana)
- **Granično vrijeme**: Zaustavlja generiranje podatkovnih točaka na ovaj datum/vrijeme

Generirane podatkovne točke imat će:
- value = 1.0
- label = "" (prazno)
- note = "" (prazno)
    	]],
    	["cs"] = [[
Generuje datové body s hodnotou 1 v pravidelných intervalech směrem do minulosti.

Konfigurace:
- **Období**: Jednotka časového období (den, týden, měsíc, rok)
- **Násobitel období**: Generovat datový bod každých N období (např. každé 2 dny)
- **Mezní čas**: Přestat generovat datové body k tomuto datu/času

Generované datové body budou mít:
- value = 1.0
- label = "" (prázdný)
- note = "" (prázdná)
    	]],
    	["da"] = [[
Genererer datapunkter med værdi=1 med regelmæssige intervaller bagud i tiden.

Konfiguration:
- **Periode**: Tidsperiodeenhed (dag, uge, måned, år)
- **Periodemultiplikator**: Generér et datapunkt for hver N perioder (f.eks. hver 2. dag)
- **Skæringstidspunkt**: Stop genereringen af datapunkter på denne dato/dette tidspunkt

Genererede datapunkter har:
- value = 1.0
- label = "" (tom)
- note = "" (tom)
    	]],
    	["nl"] = [[
Genereert datapunten met waarde=1 op regelmatige intervallen terug in de tijd.

Configuratie:
- **Periode**: Eenheid van de periode (dag, week, maand, jaar)
- **Periodevermenigvuldiger**: Om de hoeveel perioden een datapunt wordt gegenereerd (bijv. elke 2 dagen)
- **Grens**: Stop met het genereren van datapunten op deze datum/tijd

Gegenereerde datapunten hebben:
- value = 1.0
- label = "" (leeg)
- note = "" (leeg)
    	]],
    	["et"] = [[
Genereerib ajas tagasi liikudes regulaarsete intervallidega andmepunkte väärtusega 1.

Seadistus:
- **Periood**: ajavahemiku ühik (päev, nädal, kuu, aasta)
- **Perioodi kordaja**: genereeri andmepunkt iga N perioodi järel (nt iga 2 päeva järel)
- **Lõppaeg**: lõpeta andmepunktide genereerimine sellel kuupäeval/kellaajal

Genereeritud andmepunktidel on:
- väärtus = 1,0
- silt = "" (tühi)
- märkus = "" (tühi)
    	]],
    	["fil"] = [[
Gumagawa ng mga data point na may value=1 sa mga regular na pagitan habang umaatras sa nakaraan.

Configuration:
- **Period**: Yunit ng panahon (Day, Week, Month, Year)
- **Period Multiplier**: Gumawa ng data point bawat N panahon (hal., bawat 2 araw)
- **Cutoff**: Itigil ang paggawa ng mga data point sa petsa/oras na ito

Ang mga ginawang data point ay magkakaroon ng:
- value = 1.0
- label = "" (walang laman)
- note = "" (walang laman)
    	]],
    	["fi"] = [[
Luo datapisteitä, joiden arvo=1, säännöllisin väliajoin taaksepäin ajassa.

Määritys:
- **Jakso**: Ajanjakson yksikkö (päivä, viikko, kuukausi, vuosi)
- **Jakson kerroin**: Luo datapiste joka N. jakson välein (esim. joka 2. päivä)
- **Katkaisuaika**: Lopeta datapisteiden luominen tässä päivämäärässä/ajassa

Luotujen datapisteiden ominaisuudet:
- value = 1.0
- label = "" (tyhjä)
- note = "" (tyhjä)
    	]],
    	["fr"] = [[
Génère des points de données avec value=1 à intervalles réguliers en remontant dans le temps.

Configuration :
- **Période** : Unité de période (jour, semaine, mois, année)
- **Multiplicateur de période** : Générer un point de données toutes les N périodes (par ex. tous les 2 jours)
- **Limite** : Arrêter la génération des points de données à cette date/heure

Les points de données générés auront :
- value = 1.0
- label = "" (vide)
- note = "" (vide)
    	]],
    	["gl"] = [[
Xera puntos de datos con value=1 en intervalos regulares retrocedendo no tempo.

Configuración:
- **Período**: Unidade do período temporal (día, semana, mes, ano)
- **Multiplicador do período**: Xerar un punto de datos cada N períodos (por exemplo, cada 2 días)
- **Límite**: Deter a xeración de puntos de datos nesta data/hora

Os puntos de datos xerados terán:
- value = 1.0
- label = "" (baleiro)
- note = "" (baleiro)
    	]],
    	["ka"] = [[
წარსულში, რეგულარული ინტერვალებით, ქმნის მონაცემთა წერტილებს value=1 მნიშვნელობით.

კონფიგურაცია:
- **პერიოდი**: დროის პერიოდის ერთეული (დღე, კვირა, თვე, წელი)
- **პერიოდის გამამრავლებელი**: მონაცემთა წერტილის შექმნა ყოველ N პერიოდში (მაგ., ყოველ 2 დღეში)
- **საბოლოო დრო**: ამ თარიღზე/დროზე მონაცემთა წერტილების შექმნის შეწყვეტა

შექმნილ მონაცემთა წერტილებს ექნებათ:
- value = 1.0
- label = "" (ცარიელი)
- note = "" (ცარიელი)
    	]],
    	["de"] = [[
Erzeugt in regelmäßigen Abständen rückwärts in der Zeit Datenpunkte mit value=1.

Konfiguration:
- **Zeitraum**: Zeiteinheit (Tag, Woche, Monat, Jahr)
- **Zeitraummultiplikator**: Alle N Zeiträume einen Datenpunkt erzeugen (z. B. alle 2 Tage)
- **Stichtag**: Die Erzeugung von Datenpunkten an diesem Datum/Zeitpunkt beenden

Erzeugte Datenpunkte haben:
- value = 1.0
- label = "" (leer)
- note = "" (leer)
    	]],
    	["el"] = [[
Δημιουργεί σημεία δεδομένων με value=1 σε τακτά χρονικά διαστήματα προς τα πίσω στον χρόνο.

Διαμόρφωση:
- **Περίοδος**: Μονάδα χρονικής περιόδου (Ημέρα, Εβδομάδα, Μήνας, Έτος)
- **Πολλαπλασιαστής περιόδου**: Δημιουργία σημείου δεδομένων κάθε N περιόδους (π.χ. κάθε 2 ημέρες)
- **Όριο**: Διακοπή δημιουργίας σημείων δεδομένων σε αυτήν την ημερομηνία/ώρα

Τα δημιουργημένα σημεία δεδομένων θα έχουν:
- value = 1.0
- label = "" (κενό)
- note = "" (κενό)
    	]],
    	["gu"] = [[
સમયમાં પાછળ જતા નિયમિત અંતરાલે value=1 ધરાવતા ડેટા પોઇન્ટ્સ બનાવે છે.

ગોઠવણી:
- **સમયગાળો**: સમયગાળાનો એકમ (દિવસ, અઠવાડિયું, મહિનો, વર્ષ)
- **સમયગાળા ગુણક**: દર N સમયગાળે ડેટા પોઇન્ટ બનાવો (દા.ત., દર 2 દિવસે)
- **કટઑફ**: આ તારીખ/સમયે ડેટા પોઇન્ટ બનાવવાનું બંધ કરો

બનાવેલા ડેટા પોઇન્ટ્સમાં હશે:
- value = 1.0
- label = "" (ખાલી)
- note = "" (ખાલી)
    	]],
    	["hi"] = [[
समय में पीछे जाते हुए नियमित अंतराल पर value=1 वाले डेटा पॉइंट बनाता है।

कॉन्फ़िगरेशन:
- **अवधि**: समय अवधि इकाई (दिन, सप्ताह, महीना, वर्ष)
- **अवधि गुणक**: प्रत्येक N अवधियों पर डेटा पॉइंट बनाएँ (जैसे, हर 2 दिन)
- **कटऑफ़**: इस तारीख/समय पर डेटा पॉइंट बनाना रोकें

बनाए गए डेटा पॉइंट में होंगे:
- value = 1.0
- label = "" (खाली)
- note = "" (खाली)
    	]],
    	["hu"] = [[
Rendszeres időközönként 1-es értékű adatpontokat generál a múltba visszamenőleg.

Konfiguráció:
- **Időszak**: Időszakegység (nap, hét, hónap, év)
- **Időszak szorzója**: Adatpont generálása minden N. időszakban (például 2 naponta)
- **Határidő**: Az adatpontok generálásának leállítása ezen a dátumon/időponton

A generált adatpontok:
- érték = 1.0
- címke = "" (üres)
- megjegyzés = "" (üres)
    	]],
    	["is"] = [[
Býr til gagnapunkta með gildi=1 með reglulegu millibili aftur í tímann.

Stillingar:
- **Tímabil**: Eining tímabils (dagur, vika, mánuður, ár)
- **Margfeldi tímabils**: Búa til gagnapunkt á N tímabila fresti (t.d. á 2 daga fresti)
- **Lokatími**: Hætta að búa til gagnapunkta á þessari dagsetningu/tíma

Búnir gagnapunktar hafa:
- value = 1.0
- label = "" (tómt)
- note = "" (tómt)
    	]],
    	["id"] = [[
Menghasilkan titik data dengan value=1 pada interval teratur ke belakang dalam waktu.

Konfigurasi:
- **Periode**: Satuan periode waktu (Hari, Minggu, Bulan, Tahun)
- **Pengali Periode**: Menghasilkan titik data setiap N periode (misalnya, setiap 2 hari)
- **Batas Waktu**: Berhenti menghasilkan titik data pada tanggal/waktu ini

Titik data yang dihasilkan akan memiliki:
- value = 1.0
- label = "" (kosong)
- note = "" (kosong)
    	]],
    	["it"] = [[
Genera punti dati con valore=1 a intervalli regolari andando a ritroso nel tempo.

Configurazione:
- **Periodo**: unità del periodo di tempo (giorno, settimana, mese, anno)
- **Moltiplicatore del periodo**: genera un punto dati ogni N periodi (ad es. ogni 2 giorni)
- **Limite**: interrompe la generazione dei punti dati a questa data/ora

I punti dati generati avranno:
- value = 1.0
- label = "" (vuoto)
- note = "" (vuoto)
    	]],
    	["ja"] = [[
過去にさかのぼり、一定間隔で値=1のデータポイントを生成します。

設定:
- **期間**: 期間の単位（日、週、月、年）
- **期間の乗数**: N期間ごとにデータポイントを生成（例: 2日ごと）
- **カットオフ**: この日時でデータポイントの生成を停止

生成されるデータポイント:
- value = 1.0
- label = "" (空)
- note = "" (空)
    	]],
    	["kn"] = [[
ಹಿಂದಿನ ಸಮಯಕ್ಕೆ ಹೋಗುವ ನಿಯಮಿತ ಅಂತರಗಳಲ್ಲಿ value=1 ಹೊಂದಿರುವ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಸೃಷ್ಟಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಅವಧಿ**: ಸಮಯಾವಧಿ ಘಟಕ (ದಿನ, ವಾರ, ತಿಂಗಳು, ವರ್ಷ)
- **ಅವಧಿ ಗುಣಕ**: ಪ್ರತಿ N ಅವಧಿಗಳಿಗೆ ಒಂದು ಡೇಟಾ ಬಿಂದುವನ್ನು ಸೃಷ್ಟಿಸಿ (ಉದಾ., ಪ್ರತಿ 2 ದಿನಗಳಿಗೊಮ್ಮೆ)
- **ಕಟ್‌ಆಫ್**: ಈ ದಿನಾಂಕ/ಸಮಯದಲ್ಲಿ ಡೇಟಾ ಬಿಂದುಗಳ ಸೃಷ್ಟಿಯನ್ನು ನಿಲ್ಲಿಸಿ

ಸೃಷ್ಟಿಸಲಾದ ಡೇಟಾ ಬಿಂದುಗಳು ಹೊಂದಿರುತ್ತವೆ:
- value = 1.0
- label = "" (ಖಾಲಿ)
- note = "" (ಖಾಲಿ)
    	]],
    	["kk"] = [[
Уақыт бойынша кері бағытта тұрақты аралықтарда мәні 1 болатын дерек нүктелерін жасайды.

Конфигурация:
- **Кезең**: Уақыт кезеңінің бірлігі (күн, апта, ай, жыл)
- **Кезең көбейткіші**: Әр N кезең сайын дерек нүктесін жасау (мысалы, әр 2 күн сайын)
- **Шек**: Осы күнде/уақытта дерек нүктелерін жасауды тоқтату

Жасалған дерек нүктелерінде:
- value = 1.0
- label = "" (бос)
- note = "" (бос)
    	]],
    	["km"] = [[
បង្កើតចំណុចទិន្នន័យដែលមាន value=1 តាមចន្លោះពេលទៀងទាត់ ដោយរំកិលថយក្រោយតាមពេលវេលា។

ការកំណត់រចនា៖
- **រយៈពេល**៖ ឯកតារយៈពេល (ថ្ងៃ សប្តាហ៍ ខែ ឆ្នាំ)
- **មេគុណរយៈពេល**៖ បង្កើតចំណុចទិន្នន័យរៀងរាល់ N រយៈពេល (ឧ. រៀងរាល់ ២ ថ្ងៃ)
- **កាលបរិច្ឆេទកាត់**៖ ឈប់បង្កើតចំណុចទិន្នន័យនៅកាលបរិច្ឆេទ/ពេលវេលានេះ

ចំណុចទិន្នន័យដែលបានបង្កើតនឹងមាន៖
- value = 1.0
- label = "" (ទទេ)
- note = "" (ទទេ)
    	]],
    	["ko"] = [[
과거로 거슬러 올라가며 일정한 간격으로 value=1인 데이터 포인트를 생성합니다.

구성:
- **기간**: 기간 단위(일, 주, 월, 년)
- **기간 배수**: N개 기간마다 데이터 포인트 생성(예: 2일마다)
- **기준 시점**: 이 날짜/시간에서 데이터 포인트 생성을 중지합니다

생성되는 데이터 포인트:
- value = 1.0
- label = "" (비어 있음)
- note = "" (비어 있음)
    	]],
    	["ky"] = [[
Убакыт боюнча артка кайтып, үзгүлтүксүз интервалдарда value=1 мааниси бар маалымат чекиттерин түзөт.

Тууралоо:
- **Мезгил**: Убакыт мезгилинин бирдиги (Күн, Жума, Ай, Жыл)
- **Мезгил көбөйткүчү**: Ар N мезгил сайын маалымат чекитин түзүү (мисалы, ар 2 күндө)
- **Чек**: Бул күндө/убакытта маалымат чекиттерин түзүүнү токтотуу

Түзүлгөн маалымат чекиттеринде төмөнкүлөр болот:
- value = 1.0
- label = "" (бош)
- note = "" (бош)
    	]],
    	["lo"] = [[
ສ້າງຈຸດຂໍ້ມູນທີ່ມີຄ່າ=1 ໃນໄລຍະຫ່າງສະໝ່ຳສະເໝີໂດຍຍ້ອນກັບໄປຕາມເວລາ.

ການກຳນົດຄ່າ:
- **ໄລຍະ**: ໜ່ວຍໄລຍະເວລາ (ມື້, ອາທິດ, ເດືອນ, ປີ)
- **ຕົວຄູນໄລຍະ**: ສ້າງຈຸດຂໍ້ມູນທຸກໆ N ໄລຍະ (ເຊັ່ນ, ທຸກໆ 2 ມື້)
- **ຈຸດຕັດ**: ຢຸດການສ້າງຈຸດຂໍ້ມູນໃນວັນທີ/ເວລານີ້

ຈຸດຂໍ້ມູນທີ່ສ້າງຈະມີ:
- value = 1.0
- label = "" (ຫວ່າງ)
- note = "" (ຫວ່າງ)
    	]],
    	["lv"] = [[
Ģenerē datu punktus ar vērtību=1 regulāros intervālos, sākot no tagadnes un virzoties atpakaļ laikā.

Konfigurācija:
- **Periods**: Laika perioda vienība (diena, nedēļa, mēnesis, gads)
- **Perioda reizinātājs**: Ģenerēt datu punktu ik pēc N periodiem (piem., ik pēc 2 dienām)
- **Beigu datums**: Pārtraukt datu punktu ģenerēšanu šajā datumā/laikā

Ģenerētajiem datu punktiem būs:
- value = 1.0
- label = "" (tukšs)
- note = "" (tukša)
    	]],
    	["lt"] = [[
Generuoja duomenų taškus, kurių reikšmė = 1, reguliariais intervalais grįžtant į praeitį.

Konfigūracija:
- **Laikotarpis**: Laikotarpio vienetas (diena, savaitė, mėnuo, metai)
- **Laikotarpio daugiklis**: Generuoti duomenų tašką kas N laikotarpių (pvz., kas 2 dienas)
- **Riba**: Nustoti generuoti duomenų taškus nuo šios datos ir laiko

Sugeneruoti duomenų taškai turės:
- value = 1.0
- label = "" (tuščia)
- note = "" (tuščia)
    	]],
    	["mk"] = [[
Генерира точки на податоци со value=1 во редовни интервали наназад низ времето.

Конфигурација:
- **Период**: Единица на временскиот период (ден, недела, месец, година)
- **Множител на периодот**: Генерирај точка на податоци на секои N периоди (на пр., на секои 2 дена)
- **Гранично време**: Прекини со генерирање точки на податоци на овој датум/време

Генерираните точки на податоци ќе имаат:
- value = 1.0
- label = "" (празно)
- note = "" (празно)
    	]],
    	["ms"] = [[
Menjana titik data dengan nilai=1 pada selang tetap ke belakang dalam masa.

Konfigurasi:
- **Tempoh**: Unit tempoh masa (Hari, Minggu, Bulan, Tahun)
- **Pengganda Tempoh**: Jana titik data setiap N tempoh (contohnya, setiap 2 hari)
- **Pemotongan**: Hentikan penjanaan titik data pada tarikh/masa ini

Titik data yang dijana akan mempunyai:
- value = 1.0
- label = "" (kosong)
- note = "" (kosong)
    	]],
    	["ml"] = [[
കാലക്രമത്തിൽ പിന്നോട്ടുപോയി സ്ഥിരമായ ഇടവേളകളിൽ value=1 ഉള്ള ഡാറ്റാ പോയിന്റുകൾ സൃഷ്ടിക്കുന്നു.

കോൺഫിഗറേഷൻ:
- **Period**: സമയകാലയളവിന്റെ യൂണിറ്റ് (ദിവസം, ആഴ്ച, മാസം, വർഷം)
- **Period Multiplier**: ഓരോ N കാലയളവിലും ഒരു ഡാറ്റാ പോയിന്റ് സൃഷ്ടിക്കുക (ഉദാ., ഓരോ 2 ദിവസത്തിലും)
- **Cutoff**: ഈ തീയതി/സമയത്ത് ഡാറ്റാ പോയിന്റുകൾ സൃഷ്ടിക്കുന്നത് നിർത്തുക

സൃഷ്ടിക്കുന്ന ഡാറ്റാ പോയിന്റുകൾക്ക് ഉണ്ടായിരിക്കുക:
- value = 1.0
- label = "" (ശൂന്യം)
- note = "" (ശൂന്യം)
    	]],
    	["mr"] = [[
मागील काळात नियमित अंतराने value=1 असलेले डेटा पॉइंट्स तयार करते.

कॉन्फिगरेशन:
- **कालावधी**: कालावधीचे एकक (दिवस, आठवडा, महिना, वर्ष)
- **कालावधी गुणक**: प्रत्येक N कालावधीनंतर डेटा पॉइंट तयार करा (उदा., प्रत्येक 2 दिवसांनी)
- **कटऑफ**: या तारीख/वेळेवर डेटा पॉइंट्स तयार करणे थांबवा

तयार होणाऱ्या डेटा पॉइंट्समध्ये असेल:
- value = 1.0
- label = "" (रिक्त)
- note = "" (रिक्त)
    	]],
    	["mn"] = [[
Өнгөрсөн хугацаанд тогтмол интервалаар value=1 утгатай өгөгдлийн цэгүүд үүсгэнэ.

Тохиргоо:
- **Үе**: Хугацааны нэгж (Өдөр, Долоо хоног, Сар, Жил)
- **Хугацааны үржүүлэгч**: N үе тутамд өгөгдлийн цэг үүсгэх (жишээ нь, 2 өдөр тутамд)
- **Таслах хугацаа**: Өгөгдлийн цэг үүсгэхийг зогсоох огноо/цаг

Үүсгэсэн өгөгдлийн цэгүүд:
- value = 1.0
- label = "" (хоосон)
- note = "" (хоосон)
    	]],
    	["ne"] = [[
समयमा पछाडि जाँदै नियमित अन्तरालमा value=1 भएका डेटा बिन्दुहरू उत्पन्न गर्छ।

कन्फिगरेसन:
- **अवधि**: समय अवधि एकाइ (दिन, हप्ता, महिना, वर्ष)
- **अवधि गुणक**: प्रत्येक N अवधिमा डेटा बिन्दु उत्पन्न गर्ने (जस्तै, प्रत्येक २ दिनमा)
- **कटअफ**: यस मिति/समयमा डेटा बिन्दु उत्पन्न गर्न रोक्ने

उत्पन्न डेटा बिन्दुहरूमा हुनेछ:
- value = 1.0
- label = "" (खाली)
- note = "" (खाली)
    	]],
    	["no"] = [[
Genererer datapunkter med verdi=1 med jevne mellomrom bakover i tid.

Konfigurasjon:
- **Periode**: Tidsperiodens enhet (dag, uke, måned, år)
- **Periodemultiplikator**: Generer datapunkt for hver N. periode (f.eks. annenhver dag)
- **Grense**: Stopp genereringen av datapunkter på denne datoen/tidspunktet

Genererte datapunkter har:
- value = 1.0
- label = "" (tom)
- note = "" (tom)
    	]],
    	["pl"] = [[
Generuje punkty danych o wartości=1 w regularnych odstępach, cofając się w czasie.

Konfiguracja:
- **Okres**: Jednostka okresu (dzień, tydzień, miesiąc, rok)
- **Mnożnik okresu**: Generuj punkt danych co N okresów (np. co 2 dni)
- **Czas graniczny**: Zatrzymaj generowanie punktów danych w tej dacie/godzinie

Generowane punkty danych będą mieć:
- value = 1.0
- label = "" (puste)
- note = "" (puste)
    	]],
    	["pt"] = [[
Gera pontos de dados com valor=1 em intervalos regulares retrocedendo no tempo.

Configuração:
- **Período**: Unidade do período (dia, semana, mês, ano)
- **Multiplicador do período**: Gerar um ponto de dados a cada N períodos (por exemplo, a cada 2 dias)
- **Limite**: Parar de gerar pontos de dados nesta data/hora

Os pontos de dados gerados terão:
- value = 1.0
- label = "" (vazio)
- note = "" (vazio)
    	]],
    	["pa"] = [[
ਨਿਯਮਿਤ ਅੰਤਰਾਲਾਂ 'ਤੇ ਸਮੇਂ ਵਿੱਚ ਪਿੱਛੇ ਜਾਂਦੇ ਹੋਏ value=1 ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਤਿਆਰ ਕਰਦਾ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਅਵਧੀ**: ਸਮੇਂ ਦੀ ਇਕਾਈ (ਦਿਨ, ਹਫ਼ਤਾ, ਮਹੀਨਾ, ਸਾਲ)
- **ਅਵਧੀ ਗੁਣਕ**: ਹਰ N ਅਵਧੀਆਂ 'ਤੇ ਡਾਟਾ ਪੁਆਇੰਟ ਤਿਆਰ ਕਰੋ (ਉਦਾਹਰਨ ਲਈ, ਹਰ 2 ਦਿਨ)
- **ਕਟਆਫ਼**: ਇਸ ਮਿਤੀ/ਸਮੇਂ 'ਤੇ ਡਾਟਾ ਪੁਆਇੰਟ ਤਿਆਰ ਕਰਨਾ ਰੋਕੋ

ਤਿਆਰ ਕੀਤੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿੱਚ ਹੋਵੇਗਾ:
- value = 1.0
- label = "" (ਖਾਲੀ)
- note = "" (ਖਾਲੀ)
    	]],
    	["ro"] = [[
Generează puncte de date cu valoarea=1 la intervale regulate, mergând înapoi în timp.

Configurare:
- **Perioadă**: Unitatea perioadei (zi, săptămână, lună, an)
- **Multiplicator perioadă**: Generează un punct de date la fiecare N perioade (de exemplu, la fiecare 2 zile)
- **Limită**: Oprește generarea punctelor de date la această dată/oră

Punctele de date generate vor avea:
- value = 1.0
- label = "" (gol)
- note = "" (gol)
    	]],
    	["rm"] = [[
Generescha puncts da datas cun valur=1 ad intervalls regulars enavos en il temp.

Configuraziun:
- **Perioda**: Unità dal temp (di, emna, mais, onn)
- **Multiplicatur da la perioda**: Generar in punct da datas mintga N periodas (p.ex. mintga 2 dis)
- **Limita**: Stoppar la generaziun da puncts da datas a questa data/ura

Ils puncts da datas generads han:
- value = 1.0
- label = "" (vid)
- note = "" (vid)
    	]],
    	["ru"] = [[
Создаёт точки данных со значением=1 через регулярные интервалы в прошлом.

Конфигурация:
- **Период**: Единица периода (день, неделя, месяц, год)
- **Множитель периода**: Создавать точку данных каждые N периодов (например, каждые 2 дня)
- **Отсечение**: Прекратить создание точек данных в эту дату/время

Созданные точки данных будут иметь:
- value = 1.0
- label = "" (пусто)
- note = "" (пусто)
    	]],
    	["sr"] = [[
Generiše tačke podataka sa vrednošću=1 u pravilnim intervalima unazad kroz vreme.

Konfiguracija:
- **Period**: Jedinica vremenskog perioda (dan, nedelja, mesec, godina)
- **Množilac perioda**: Generiše tačku podataka svakih N perioda (npr. svaka 2 dana)
- **Krajnji rok**: Prekida generisanje tačaka podataka na ovaj datum/vreme

Generisane tačke podataka imaće:
- value = 1.0
- label = "" (prazno)
- note = "" (prazno)
    	]],
    	["si"] = [[
කාලය පසුපසට යමින් නිත්‍ය අන්තරවල value=1 සහිත දත්ත ලක්ෂ්‍ය ජනනය කරයි.

වින්‍යාසය:
- **කාලාන්තරය**: කාල පරාස ඒකකය (දවස, සතිය, මාසය, වසර)
- **කාලාන්තර ගුණකය**: සෑම N කාලාන්තරයකටම දත්ත ලක්ෂ්‍යයක් ජනනය කරන්න (උදා: සෑම දින 2කටම)
- **කඩඉම**: මෙම දිනය/වේලාවේදී දත්ත ලක්ෂ්‍ය ජනනය නවත්වන්න

ජනනය කළ දත්ත ලක්ෂ්‍යවල:
- value = 1.0
- label = "" (හිස්)
- note = "" (හිස්)
    	]],
    	["sk"] = [[
Vytvára údajové body s hodnotou=1 v pravidelných intervaloch smerom do minulosti.

Konfigurácia:
- **Obdobie**: Jednotka časového obdobia (deň, týždeň, mesiac, rok)
- **Násobiteľ obdobia**: Vytvoriť údajový bod každých N období (napr. každé 2 dni)
- **Hraničný čas**: Zastaviť vytváranie údajových bodov k tomuto dátumu/času

Vytvorené údajové body budú mať:
- value = 1.0
- label = "" (prázdne)
- note = "" (prázdne)
    	]],
    	["sl"] = [[
Ustvari podatkovne točke z vrednostjo=1 v rednih intervalih za nazaj v času.

Konfiguracija:
- **Obdobje**: Enota časovnega obdobja (dan, teden, mesec, leto)
- **Množitelj obdobja**: Ustvari podatkovno točko vsakih N obdobij (npr. vsaka 2 dni)
- **Presečni čas**: Nehaj ustvarjati podatkovne točke na ta datum/čas

Ustvarjene podatkovne točke bodo imele:
- value = 1.0
- label = "" (prazno)
- note = "" (prazno)
    	]],
    	["es"] = [[
Genera puntos de datos con valor=1 a intervalos regulares retrocediendo en el tiempo.

Configuración:
- **Período**: Unidad del período (día, semana, mes, año)
- **Multiplicador del período**: Generar un punto de datos cada N períodos (por ejemplo, cada 2 días)
- **Límite**: Dejar de generar puntos de datos en esta fecha/hora

Los puntos de datos generados tendrán:
- value = 1.0
- label = "" (vacío)
- note = "" (vacío)
    	]],
    	["sw"] = [[
Huzalisha nukta za data zenye value=1 katika vipindi vya kawaida vinavyorudi nyuma kwa wakati.

Usanidi:
- **Kipindi**: Kipimo cha kipindi cha muda (Siku, Wiki, Mwezi, Mwaka)
- **Kizidishi cha Kipindi**: Zalisha nukta ya data kila vipindi N (kwa mfano, kila siku 2)
- **Kikomo**: Acha kuzalisha nukta za data katika tarehe/saa hii

Nukta za data zinazozalishwa zitakuwa na:
- value = 1.0
- label = "" (tupu)
- note = "" (tupu)
    	]],
    	["sv"] = [[
Genererar datapunkter med värdet=1 vid regelbundna intervall bakåt i tiden.

Konfiguration:
- **Period**: Tidsperiodsenhet (dag, vecka, månad, år)
- **Periodmultiplikator**: Generera en datapunkt var N:e period (t.ex. varannan dag)
- **Gräns**: Sluta generera datapunkter vid detta datum/klockslag

Genererade datapunkter har:
- value = 1.0
- label = "" (tom)
- note = "" (tom)
    	]],
    	["ta"] = [[
காலத்தைப் பின்னோக்கிச் செல்லும்போது சீரான இடைவெளிகளில் value=1 கொண்ட தரவுப் புள்ளிகளை உருவாக்குகிறது.

உள்ளமைவு:
- **காலப்பகுதி**: காலப்பகுதி அலகு (நாள், வாரம், மாதம், ஆண்டு)
- **காலப்பகுதி பெருக்கி**: ஒவ்வொரு N காலப்பகுதிக்கும் ஒரு தரவுப் புள்ளியை உருவாக்கு (எ.கா., 2 நாட்களுக்கு ஒருமுறை)
- **காலவரம்பு**: இந்த தேதி/நேரத்தில் தரவுப் புள்ளிகளை உருவாக்குவதை நிறுத்து

உருவாக்கப்படும் தரவுப் புள்ளிகளில்:
- value = 1.0
- label = "" (காலி)
- note = "" (காலி)
    	]],
    	["te"] = [[
కాలంలో వెనక్కి వెళ్తూ క్రమమైన వ్యవధుల్లో value=1 ఉన్న డేటా పాయింట్లను ఉత్పత్తి చేస్తుంది.

కాన్ఫిగరేషన్:
- **కాలవ్యవధి**: సమయ కాలవ్యవధి యూనిట్ (రోజు, వారం, నెల, సంవత్సరం)
- **కాలవ్యవధి గుణకం**: ప్రతి N కాలవ్యవధులకు ఒక డేటా పాయింట్‌ను ఉత్పత్తి చేయి (ఉదా., ప్రతి 2 రోజులకు)
- **కట్‌ఆఫ్**: ఈ తేదీ/సమయానికి డేటా పాయింట్ల ఉత్పత్తిని ఆపండి

ఉత్పత్తి చేయబడిన డేటా పాయింట్లలో:
- value = 1.0
- label = "" (ఖాళీ)
- note = "" (ఖాళీ)
    	]],
    	["th"] = [[
สร้างจุดข้อมูลที่มีค่า=1 ในช่วงเวลาสม่ำเสมอย้อนกลับไปตามเวลา

การกำหนดค่า:
- **ช่วงเวลา**: หน่วยช่วงเวลา (วัน สัปดาห์ เดือน ปี)
- **ตัวคูณช่วงเวลา**: สร้างจุดข้อมูลทุก N ช่วงเวลา (เช่น ทุก 2 วัน)
- **เวลาตัด**: หยุดสร้างจุดข้อมูล ณ วันที่/เวลานี้

จุดข้อมูลที่สร้างจะมี:
- value = 1.0
- label = "" (ว่าง)
- note = "" (ว่าง)
    	]],
    	["tr"] = [[
Zamanda geriye doğru düzenli aralıklarla value=1 değerine sahip veri noktaları oluşturur.

Yapılandırma:
- **Dönem**: Zaman dönemi birimi (Gün, Hafta, Ay, Yıl)
- **Dönem Çarpanı**: Her N dönemde bir veri noktası oluştur (ör. 2 günde bir)
- **Kesim**: Veri noktası oluşturmayı bu tarih/saatte durdur

Oluşturulan veri noktaları şunlara sahip olur:
- value = 1.0
- label = "" (boş)
- note = "" (boş)
    	]],
    	["uk"] = [[
Створює точки даних зі значенням=1 через регулярні інтервали в минулому.

Конфігурація:
- **Період**: Одиниця періоду часу (день, тиждень, місяць, рік)
- **Множник періоду**: Створювати точку даних кожні N періодів (наприклад, кожні 2 дні)
- **Граничний час**: Припинити створення точок даних у цю дату/час

Створені точки даних матимуть:
- value = 1.0
- label = "" (порожнє)
- note = "" (порожнє)
    	]],
    	["vi"] = [[
Tạo các điểm dữ liệu có value=1 theo các khoảng thời gian đều đặn lùi về quá khứ.

Cấu hình:
- **Chu kỳ**: Đơn vị khoảng thời gian (Ngày, Tuần, Tháng, Năm)
- **Hệ số chu kỳ**: Tạo điểm dữ liệu sau mỗi N chu kỳ (ví dụ: mỗi 2 ngày)
- **Thời điểm giới hạn**: Dừng tạo điểm dữ liệu tại ngày/giờ này

Các điểm dữ liệu được tạo sẽ có:
- value = 1.0
- label = "" (trống)
- note = "" (trống)
    	]],
    },
    config = {
        enum {
            id = "period",
            name = "_period",
            options = { "_day", "_week", "_month", "_year" },
            default = "_day",
        },
        uint {
            id = "period_multiplier",
            name = "_period_multiplier",
            default = 1,
        },
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (365 * core.DURATION.DAY),
        },
    },

    -- Generator function
    generator = function(_, config)
        -- Parse configuration with defaults
        local period_str = config and config.period or error("Period configuration is required")
        local period_multiplier = (config and config.period_multiplier) or 1
        -- Don't allow 0 multiplier, fallback to 1
        if period_multiplier == 0 then
            period_multiplier = 1
        end
        local cutoff_timestamp = config and config.cutoff or error("Cutoff configuration is required")

        -- Map enum string to core.PERIOD constant
        local period_map = {
            ["_day"] = core.PERIOD.DAY,
            ["_week"] = core.PERIOD.WEEK,
            ["_month"] = core.PERIOD.MONTH,
            ["_year"] = core.PERIOD.YEAR,
        }
        local period = period_map[period_str]

        -- Get current time for comparison
        local now = core.time().timestamp

        -- If cutoff is in the future, no data points to generate
        if cutoff_timestamp > now then
            return function()
                return nil
            end
        end

        -- Estimate number of periods elapsed since anchor
        local elapsed_ms = now - cutoff_timestamp
        local estimated_periods
        local period_duration_ms

        if period == core.PERIOD.DAY then
            period_duration_ms = period_multiplier * core.DURATION.DAY
        elseif period == core.PERIOD.WEEK then
            period_duration_ms = period_multiplier * core.DURATION.WEEK
        elseif period == core.PERIOD.MONTH then
            -- Average month length: 30.44 days
            period_duration_ms = period_multiplier * 30.44 * core.DURATION.DAY
        elseif period == core.PERIOD.YEAR then
            -- Average year length: 365.25 days
            period_duration_ms = period_multiplier * 365.25 * core.DURATION.DAY
        else
            error("Invalid period: " .. tostring(period_str))
        end

        estimated_periods = math.floor(elapsed_ms / period_duration_ms)

        local cutoff_date = core.date(cutoff_timestamp)

        -- Jump close to now with one large shift
        local candidate = core.shift(cutoff_date, period, estimated_periods * period_multiplier)

        -- Fine-tune: shift forward until we pass "now"
        while candidate.timestamp <= now do
            candidate = core.shift(candidate, period, period_multiplier)
        end

        -- Back up one step to get the most recent data point <= now
        local current = core.shift(candidate, period, -period_multiplier)

        -- Return iterator function
        return function()
            -- Check if we've gone past the cutoff (with 1 second tolerance for millisecond precision loss)
            if current.timestamp < cutoff_timestamp - 1000 then
                return nil
            end

            -- Create data point at current timestamp
            local data_point = {
                timestamp = current.timestamp,
                offset = current.offset,
                value = 1.0,
                label = "",
                note = "",
            }

            -- Shift backwards by period * period_multiplier for next iteration
            current = core.shift(current, period, -period_multiplier)

            return data_point
        end
    end,
}
