-- Lua Function to override data point values with random values
-- This function replaces all incoming data point values with random numbers between min and max

local number = require("tng.config").number
local uint = require("tng.config").uint
local core = require("tng.core")
local random = require("tng.random")

local now = core.time()
local default_seed = now and now.timestamp or 0

return {
    -- Configuration metadata
    id = "random-value",
    version = "2.0.1",
    inputCount = 1,
    categories = { "_randomisers" },
    title = {
    	["en"] = "Random Value",
    	["af"] = "Ewekansige Waarde",
    	["sq"] = "Vlerë e rastësishme",
    	["am"] = "የዘፈቀደ እሴት",
    	["hy"] = "Պատահական արժեք",
    	["az"] = "Təsadüfi qiymət",
    	["bn"] = "এলোমেলো মান",
    	["eu"] = "Ausazko balioa",
    	["be"] = "Выпадковае значэнне",
    	["bg"] = "Случайна стойност",
    	["my"] = "ကျပန်းတန်ဖိုး",
    	["ca"] = "Valor aleatori",
    	["zh-Hans"] = "随机值",
    	["zh-Hant"] = "隨機值",
    	["hr"] = "Nasumična vrijednost",
    	["cs"] = "Náhodná hodnota",
    	["da"] = "Tilfældig værdi",
    	["nl"] = "Willekeurige waarde",
    	["et"] = "Juhuslik väärtus",
    	["fil"] = "Random na Halaga",
    	["fi"] = "Satunnainen arvo",
    	["fr"] = "Valeur aléatoire",
    	["gl"] = "Valor aleatorio",
    	["ka"] = "შემთხვევითი მნიშვნელობა",
    	["de"] = "Zufallswert",
    	["el"] = "Τυχαία τιμή",
    	["gu"] = "રેન્ડમ મૂલ્ય",
    	["hi"] = "यादृच्छिक मान",
    	["hu"] = "Véletlenszerű érték",
    	["is"] = "Handahófsgildi",
    	["id"] = "Nilai Acak",
    	["it"] = "Valore casuale",
    	["ja"] = "ランダム値",
    	["kn"] = "ಯಾದೃಚ್ಛಿಕ ಮೌಲ್ಯ",
    	["kk"] = "Кездейсоқ мән",
    	["km"] = "តម្លៃចៃដន្យ",
    	["ko"] = "무작위 값",
    	["ky"] = "Кокус маани",
    	["lo"] = "ຄ່າສຸ່ມ",
    	["lv"] = "Nejauša vērtība",
    	["lt"] = "Atsitiktinė reikšmė",
    	["mk"] = "Случајна вредност",
    	["ms"] = "Nilai Rawak",
    	["ml"] = "റാൻഡം മൂല്യം",
    	["mr"] = "यादृच्छिक मूल्य",
    	["mn"] = "Санамсаргүй утга",
    	["ne"] = "यादृच्छिक मान",
    	["no"] = "Tilfeldig verdi",
    	["pl"] = "Wartość losowa",
    	["pt"] = "Valor aleatório",
    	["pa"] = "ਬੇਤਰਤੀਬ ਮੁੱਲ",
    	["ro"] = "Valoare aleatorie",
    	["rm"] = "Valur casuala",
    	["ru"] = "Случайное значение",
    	["sr"] = "Nasumična vrednost",
    	["si"] = "අහඹු අගය",
    	["sk"] = "Náhodná hodnota",
    	["sl"] = "Naključna vrednost",
    	["es"] = "Valor aleatorio",
    	["sw"] = "Thamani Nasibu",
    	["sv"] = "Slumpvärde",
    	["ta"] = "சீரற்ற மதிப்பு",
    	["te"] = "యాదృచ్ఛిక విలువ",
    	["th"] = "ค่าสุ่ม",
    	["tr"] = "Rastgele Değer",
    	["uk"] = "Випадкове значення",
    	["vi"] = "Giá trị ngẫu nhiên",
    },
    description = {
    	["en"] = [[
Replaces all incoming data point values with random numbers between min and max.

Configuration:
- **Min Value**: The minimum value for random generation
- **Max Value**: The maximum value for random generation
- **Seed**: Random seed for reproducible results (defaults to current UTC timestamp)

The function automatically swaps min and max if max is smaller than min.
    	]],
    	["af"] = [[
Vervang alle inkomende datapuntwaardes met ewekansige getalle tussen min en maks.

Konfigurasie:
- **Minimumwaarde**: Die minimumwaarde vir ewekansige generering
- **Maksimumwaarde**: Die maksimumwaarde vir ewekansige generering
- **Saad**: Ewekansige saad vir herhaalbare resultate (verstek die huidige UTC-tydstempel)

Die funksie ruil min en maks outomaties om as maks kleiner as min is.
    	]],
    	["sq"] = [[
Zëvendëson të gjitha vlerat hyrëse të pikave të të dhënave me numra të rastësishëm midis minimumit dhe maksimumit.

Konfigurimi:
- **Vlera minimale**: Vlera minimale për gjenerimin e rastësishëm
- **Vlera maksimale**: Vlera maksimale për gjenerimin e rastësishëm
- **Fara**: Fara e rastësishme për rezultate të përsëritshme (parazgjedhja është vula kohore aktuale UTC)

Funksioni ndërron automatikisht minimumin dhe maksimumin nëse maksimumi është më i vogël se minimumi.
    	]],
    	["am"] = [[
ሁሉንም የሚገቡ የውሂብ ነጥብ እሴቶች በmin እና max መካከል ባሉ የዘፈቀደ ቁጥሮች ይተካል።

ውቅር፦
- **Min Value**፦ ለዘፈቀደ ማመንጨት ዝቅተኛው እሴት
- **Max Value**፦ ለዘፈቀደ ማመንጨት ከፍተኛው እሴት
- **Seed**፦ ሊደገም ለሚችል ውጤት የዘፈቀደ መነሻ (በነባሪ የአሁኑ UTC የጊዜ ማህተም)

max ከmin ያነሰ ከሆነ ተግባሩ በራስ-ሰር min እና maxን ይቀያይራል።
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի արժեքները փոխարինում է min-ի և max-ի միջև ընկած պատահական թվերով։

Կազմաձևում՝
- **Նվազագույն արժեք**․ պատահական գեներացման նվազագույն արժեքը
- **Առավելագույն արժեք**․ պատահական գեներացման առավելագույն արժեքը
- **Սերմ**․ վերարտադրելի արդյունքների պատահական սերմը (կանխադրված՝ ընթացիկ UTC ժամանակացույցը)

Ֆունկցիան ավտոմատ կերպով փոխում է min-ի և max-ի տեղերը, եթե max-ը min-ից փոքր է։
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qiymətlərini min və max arasında təsadüfi ədədlərlə əvəz edir.

Konfiqurasiya:
- **Min qiymət**: Təsadüfi yaratma üçün minimum qiymət
- **Max qiymət**: Təsadüfi yaratma üçün maksimum qiymət
- **Toxum**: Təkrarlana bilən nəticələr üçün təsadüfi toxum (standart olaraq cari UTC zaman damğası)

max min-dən kiçik olduqda funksiya avtomatik olaraq min və max-ı yerlərini dəyişir.
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের মানকে min ও max-এর মধ্যে এলোমেলো সংখ্যা দিয়ে প্রতিস্থাপন করে।

কনফিগারেশন:
- **সর্বনিম্ন মান**: এলোমেলো মান তৈরির সর্বনিম্ন মান
- **সর্বোচ্চ মান**: এলোমেলো মান তৈরির সর্বোচ্চ মান
- **সিড**: পুনরুৎপাদনযোগ্য ফলাফলের জন্য এলোমেলো সিড (ডিফল্ট হিসেবে বর্তমান UTC টাইমস্ট্যাম্প)

max, min-এর চেয়ে ছোট হলে ফাংশনটি স্বয়ংক্রিয়ভাবে min ও max অদলবদল করে।
    	]],
    	["eu"] = [[
Sarrerako datu-puntuen balio guztiak min eta max artean dauden ausazko zenbakiekin ordezten ditu.

Konfigurazioa:
- **Gutxieneko balioa**: Ausazko sorkuntzarako gutxieneko balioa
- **Gehieneko balioa**: Ausazko sorkuntzarako gehieneko balioa
- **Hazia**: Emaitza errepikagarriak lortzeko ausazko hazia (lehenetsia: uneko UTC denbora-zigilua)

Funtzioak automatikoki trukatzen ditu min eta max, max min baino txikiagoa bada.
    	]],
    	["be"] = [[
Замяняе ўсе ўваходныя значэнні кропак даных выпадковымі лікамі паміж min і max.

Канфігурацыя:
- **Мінімальнае значэнне**: мінімальнае значэнне для генерацыі выпадковых лікаў
- **Максімальнае значэнне**: максімальнае значэнне для генерацыі выпадковых лікаў
- **Зярно**: выпадковае зярно для ўзнаўляльных вынікаў (па змаўчанні — бягучая метка часу UTC)

Функцыя аўтаматычна мяняе месцамі min і max, калі max меншае за min.
    	]],
    	["bg"] = [[
Заменя всички входящи стойности на точките от данни със случайни числа между min и max.

Конфигурация:
- **Минимална стойност**: Минималната стойност за генериране на случайни числа
- **Максимална стойност**: Максималната стойност за генериране на случайни числа
- **Начална стойност**: Начална стойност за случайните числа за възпроизводими резултати (по подразбиране текущият UTC времеви отпечатък)

Функцията автоматично разменя min и max, ако max е по-малко от min.
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်တန်ဖိုးများအားလုံးကို min နှင့် max ကြားရှိ ကျပန်းနံပါတ်များဖြင့် အစားထိုးသည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **အနည်းဆုံးတန်ဖိုး**: ကျပန်းဖန်တီးရာတွင် အသုံးပြုမည့် အနည်းဆုံးတန်ဖိုး
- **အများဆုံးတန်ဖိုး**: ကျပန်းဖန်တီးရာတွင် အသုံးပြုမည့် အများဆုံးတန်ဖိုး
- **Seed**: ထပ်မံတူညီသောရလဒ်များ ရရှိရန် ကျပန်း seed (မူလအားဖြင့် လက်ရှိ UTC timestamp)

max သည် min ထက်ငယ်ပါက function သည် min နှင့် max ကို အလိုအလျောက် လဲလှယ်သည်။
    	]],
    	["ca"] = [[
Substitueix tots els valors dels punts de dades entrants per nombres aleatoris entre el mínim i el màxim.

Configuració:
- **Valor mínim**: El valor mínim per a la generació aleatòria
- **Valor màxim**: El valor màxim per a la generació aleatòria
- **Llavor**: Llavor aleatòria per obtenir resultats reproduïbles (per defecte, la marca de temps UTC actual)

La funció intercanvia automàticament el mínim i el màxim si el màxim és inferior al mínim.
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的值替换为介于最小值和最大值之间的随机数。

配置：
- **最小值**：随机生成的最小值
- **最大值**：随机生成的最大值
- **种子**：用于生成可复现结果的随机种子（默认为当前 UTC 时间戳）

如果最大值小于最小值，该函数会自动交换两者。
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的值替換為介於最小值和最大值之間的隨機數。

設定：
- **最小值**：隨機產生的最小值
- **最大值**：隨機產生的最大值
- **種子**：用於產生可重現結果的隨機種子（預設為目前 UTC 時間戳記）

如果最大值小於最小值，函式會自動交換兩者。
    	]],
    	["hr"] = [[
Zamjenjuje sve dolazne vrijednosti podatkovnih točaka nasumičnim brojevima između minimuma i maksimuma.

Konfiguracija:
- **Minimalna vrijednost**: Najmanja vrijednost za nasumično generiranje
- **Maksimalna vrijednost**: Najveća vrijednost za nasumično generiranje
- **Sjeme**: Nasumično sjeme za ponovljive rezultate (zadano je trenutačno UTC vrijeme)

Funkcija automatski zamjenjuje minimum i maksimum ako je maksimum manji od minimuma.
    	]],
    	["cs"] = [[
Nahradí všechny příchozí hodnoty datových bodů náhodnými čísly mezi minimem a maximem.

Konfigurace:
- **Minimální hodnota**: Minimální hodnota pro generování náhodných čísel
- **Maximální hodnota**: Maximální hodnota pro generování náhodných čísel
- **Seed**: Náhodné počáteční číslo pro opakovatelné výsledky (výchozí je aktuální časové razítko UTC)

Pokud je maximum menší než minimum, funkce minimum a maximum automaticky prohodí.
    	]],
    	["da"] = [[
Erstatter alle indgående datapunkters værdier med tilfældige tal mellem min og max.

Konfiguration:
- **Minimumsværdi**: Minimumsværdien for tilfældig generering
- **Maksimumsværdi**: Maksimumsværdien for tilfældig generering
- **Seed**: Tilfældighedsseed til reproducerbare resultater (standard er aktuelt UTC-tidsstempel)

Funktionen bytter automatisk om på min og max, hvis max er mindre end min.
    	]],
    	["nl"] = [[
Vervangt alle binnenkomende waarden van datapunten door willekeurige getallen tussen min en max.

Configuratie:
- **Minimumwaarde**: De minimumwaarde voor willekeurige generatie
- **Maximumwaarde**: De maximumwaarde voor willekeurige generatie
- **Seed**: Seed voor reproduceerbare resultaten (standaard de huidige UTC-tijdstempel)

De functie wisselt min en max automatisch om als max kleiner is dan min.
    	]],
    	["et"] = [[
Asendab kõik sisendandmepunktide väärtused juhuslike arvudega min- ja max-väärtuse vahel.

Seadistus:
- **Min väärtus**: juhusliku genereerimise minimaalne väärtus
- **Max väärtus**: juhusliku genereerimise maksimaalne väärtus
- **Seeme**: juhuslikkuse seeme korratavate tulemuste jaoks (vaikimisi praegune UTC ajatempel)

Funktsioon vahetab min- ja max-väärtuse automaatselt, kui max on min-st väiksem.
    	]],
    	["fil"] = [[
Pinapalitan ang lahat ng papasok na halaga ng data point ng mga random na numero sa pagitan ng min at max.

Configuration:
- **Min Value**: Pinakamababang halaga para sa random generation
- **Max Value**: Pinakamataas na halaga para sa random generation
- **Seed**: Random seed para sa nauulit na mga resulta (default ay kasalukuyang UTC timestamp)

Awtomatikong ipinagpapalit ng function ang min at max kung mas maliit ang max kaysa sa min.
    	]],
    	["fi"] = [[
Korvaa kaikki saapuvien datapisteiden arvot satunnaisilla min- ja maksimiarvon välisillä luvuilla.

Määritys:
- **Minimiarvo**: Satunnaislukujen pienin arvo
- **Maksimiarvo**: Satunnaislukujen suurin arvo
- **Siemen**: Satunnaislukujen siemen toistettavia tuloksia varten (oletuksena nykyinen UTC-aikaleima)

Funktio vaihtaa minimi- ja maksimiarvon automaattisesti, jos maksimiarvo on minimiarvoa pienempi.
    	]],
    	["fr"] = [[
Remplace toutes les valeurs des points de données entrants par des nombres aléatoires compris entre min et max.

Configuration :
- **Valeur minimale** : Valeur minimale pour la génération aléatoire
- **Valeur maximale** : Valeur maximale pour la génération aléatoire
- **Graine** : Graine aléatoire pour obtenir des résultats reproductibles (par défaut, horodatage UTC actuel)

La fonction échange automatiquement min et max si max est inférieur à min.
    	]],
    	["gl"] = [[
Substitúe todos os valores dos puntos de datos recibidos por números aleatorios entre o mínimo e o máximo.

Configuración:
- **Valor mínimo**: O valor mínimo para a xeración aleatoria
- **Valor máximo**: O valor máximo para a xeración aleatoria
- **Semente**: Semente aleatoria para obter resultados reproducibles (predeterminada: marca temporal UTC actual)

A función intercambia automaticamente o mínimo e o máximo se o máximo é inferior ao mínimo.
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის მნიშვნელობას ანაცვლებს შემთხვევითი რიცხვებით min-სა და max-ს შორის.

კონფიგურაცია:
- **მინიმალური მნიშვნელობა**: შემთხვევითი გენერირებისთვის მინიმალური მნიშვნელობა
- **მაქსიმალური მნიშვნელობა**: შემთხვევითი გენერირებისთვის მაქსიმალური მნიშვნელობა
- **Seed**: შემთხვევითი გენერირების საწყისი მნიშვნელობა განმეორებადი შედეგებისთვის (ნაგულისხმევად მიმდინარე UTC დროის ნიშნული)

ფუნქცია ავტომატურად ცვლის min-სა და max-ს ადგილებს, თუ max min-ზე ნაკლებია.
    	]],
    	["de"] = [[
Ersetzt alle eingehenden Datenpunktwerte durch Zufallszahlen zwischen Min und Max.

Konfiguration:
- **Minimalwert**: Der Mindestwert für die Zufallserzeugung
- **Maximalwert**: Der Höchstwert für die Zufallserzeugung
- **Seed**: Zufalls-Seed für reproduzierbare Ergebnisse (standardmäßig aktueller UTC-Zeitstempel)

Die Funktion vertauscht Min und Max automatisch, wenn Max kleiner als Min ist.
    	]],
    	["el"] = [[
Αντικαθιστά όλες τις εισερχόμενες τιμές σημείων δεδομένων με τυχαίους αριθμούς μεταξύ min και max.

Διαμόρφωση:
- **Ελάχιστη τιμή**: Η ελάχιστη τιμή για την παραγωγή τυχαίων αριθμών
- **Μέγιστη τιμή**: Η μέγιστη τιμή για την παραγωγή τυχαίων αριθμών
- **Seed**: Τυχαίος σπόρος για αναπαραγώγιμα αποτελέσματα (προεπιλογή: τρέχουσα χρονική σήμανση UTC)

Η συνάρτηση ανταλλάσσει αυτόματα τις τιμές min και max αν το max είναι μικρότερο από το min.
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના મૂલ્યોને min અને max વચ્ચેની રેન્ડમ સંખ્યાઓથી બદલે છે.

ગોઠવણી:
- **લઘુત્તમ મૂલ્ય**: રેન્ડમ જનરેશન માટેનું લઘુત્તમ મૂલ્ય
- **મહત્તમ મૂલ્ય**: રેન્ડમ જનરેશન માટેનું મહત્તમ મૂલ્ય
- **સીડ**: પુનઃઉત્પાદન કરી શકાય તેવા પરિણામો માટેની રેન્ડમ સીડ (ડિફૉલ્ટ વર્તમાન UTC ટાઇમસ્ટેમ્પ)

જો max, min કરતાં નાનું હોય તો ફંક્શન આપમેળે min અને maxની અદલાબદલી કરે છે.
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के मानों को min और max के बीच की यादृच्छिक संख्याओं से बदलता है।

कॉन्फ़िगरेशन:
- **न्यूनतम मान**: यादृच्छिक निर्माण के लिए न्यूनतम मान
- **अधिकतम मान**: यादृच्छिक निर्माण के लिए अधिकतम मान
- **सीड**: दोहराए जा सकने वाले परिणामों के लिए यादृच्छिक सीड (डिफ़ॉल्ट रूप से वर्तमान UTC टाइमस्टैम्प)

यदि max, min से छोटा हो तो फ़ंक्शन अपने-आप min और max को बदल देता है।
    	]],
    	["hu"] = [[
Az összes beérkező adatpont értékét a minimum és maximum közötti véletlen számokra cseréli.

Konfiguráció:
- **Minimális érték**: A véletlengenerálás minimális értéke
- **Maximális érték**: A véletlengenerálás maximális értéke
- **Mag**: Az eredmények reprodukálhatóságát biztosító véletlenmag (alapértelmezés szerint az aktuális UTC-időbélyeg)

A függvény automatikusan felcseréli a minimumot és a maximumot, ha a maximum kisebb a minimumnál.
    	]],
    	["is"] = [[
Skiptir öllum gildum innkomandi gagnapunkta út fyrir handahófskenndar tölur milli lágmarks og hámarks.

Stillingar:
- **Lágmarksgildi**: Lágmarksgildi fyrir handahófsmyndun
- **Hámarksgildi**: Hámarksgildi fyrir handahófsmyndun
- **Sáð**: Handahófssáð fyrir endurtakanlegar niðurstöður (sjálfgefið er núverandi UTC-tímamerki)

Aðgerðin víxlar sjálfkrafa á lágmarki og hámarki ef hámarkið er minna en lágmarkið.
    	]],
    	["id"] = [[
Mengganti semua nilai titik data yang masuk dengan angka acak antara min dan max.

Konfigurasi:
- **Nilai Min**: Nilai minimum untuk menghasilkan angka acak
- **Nilai Maks**: Nilai maksimum untuk menghasilkan angka acak
- **Seed**: Seed acak untuk hasil yang dapat direproduksi (default-nya stempel waktu UTC saat ini)

Fungsi ini secara otomatis menukar min dan max jika max lebih kecil daripada min.
    	]],
    	["it"] = [[
Sostituisce tutti i valori dei punti dati in arrivo con numeri casuali compresi tra min e max.

Configurazione:
- **Valore minimo**: il valore minimo per la generazione casuale
- **Valore massimo**: il valore massimo per la generazione casuale
- **Seed**: seme casuale per risultati riproducibili (per impostazione predefinita, il timestamp UTC corrente)

La funzione scambia automaticamente min e max se max è minore di min.
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントの値を、最小値と最大値の間の乱数に置き換えます。

設定:
- **最小値**: 乱数生成の最小値
- **最大値**: 乱数生成の最大値
- **シード**: 再現可能な結果を得るための乱数シード（デフォルトは現在のUTCタイムスタンプ）

最大値が最小値より小さい場合、関数は最小値と最大値を自動的に入れ替えます。
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳನ್ನು min ಮತ್ತು max ನಡುವಿನ ಯಾದೃಚ್ಛಿಕ ಸಂಖ್ಯೆಗಳೊಂದಿಗೆ ಬದಲಾಯಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಕನಿಷ್ಠ ಮೌಲ್ಯ**: ಯಾದೃಚ್ಛಿಕ ಸೃಷ್ಟಿಗೆ ಕನಿಷ್ಠ ಮೌಲ್ಯ
- **ಗರಿಷ್ಠ ಮೌಲ್ಯ**: ಯಾದೃಚ್ಛಿಕ ಸೃಷ್ಟಿಗೆ ಗರಿಷ್ಠ ಮೌಲ್ಯ
- **ಸೀಡ್**: ಪುನರುತ್ಪಾದಿಸಬಹುದಾದ ಫಲಿತಾಂಶಗಳಿಗಾಗಿ ಯಾದೃಚ್ಛಿಕ ಸೀಡ್ (ಡೀಫಾಲ್ಟ್ ಪ್ರಸ್ತುತ UTC ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್)

max, min ಗಿಂತ ಚಿಕ್ಕದಾಗಿದ್ದರೆ ಫಂಕ್ಷನ್ ಸ್ವಯಂಚಾಲಿತವಾಗಿ min ಮತ್ತು max ಅನ್ನು ವಿನಿಮಯ ಮಾಡುತ್ತದೆ.
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің мәндерін min және max аралығындағы кездейсоқ сандармен алмастырады.

Конфигурация:
- **Ең төменгі мән**: Кездейсоқ мәндер жасауға арналған ең төменгі мән
- **Ең жоғары мән**: Кездейсоқ мәндер жасауға арналған ең жоғары мән
- **Бастапқы мән**: Қайта өндірілетін нәтижелерге арналған кездейсоқтық бастапқы мәні (әдепкісі — ағымдағы UTC уақыт белгісі)

max мәні min мәнінен кіші болса, функция min және max мәндерінің орындарын автоматты түрде ауыстырады.
    	]],
    	["km"] = [[
ជំនួសតម្លៃរបស់ចំណុចទិន្នន័យចូលទាំងអស់ដោយលេខចៃដន្យរវាង min និង max។

ការកំណត់រចនា៖
- **តម្លៃអប្បបរមា**៖ តម្លៃអប្បបរមាសម្រាប់ការបង្កើតលេខចៃដន្យ
- **តម្លៃអតិបរមា**៖ តម្លៃអតិបរមាសម្រាប់ការបង្កើតលេខចៃដន្យ
- **Seed**៖ គ្រាប់ពូជចៃដន្យសម្រាប់បង្កើតលទ្ធផលដដែលៗ (តាមលំនាំដើម គឺត្រាពេលវេលា UTC បច្ចុប្បន្ន)

មុខងារនឹងប្តូរ min និង max ដោយស្វ័យប្រវត្តិ ប្រសិនបើ max តូចជាង min។
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 값을 최솟값과 최댓값 사이의 무작위 숫자로 바꿉니다.

구성:
- **최솟값**: 무작위 생성의 최솟값
- **최댓값**: 무작위 생성의 최댓값
- **시드**: 재현 가능한 결과를 위한 무작위 시드(기본값은 현재 UTC 타임스탬프)

최댓값이 최솟값보다 작으면 함수가 자동으로 최솟값과 최댓값을 바꿉니다.
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин маанилерин min жана max ортосундагы кокус сандарга алмаштырат.

Тууралоо:
- **Минималдуу маани**: Кокус түзүү үчүн минималдуу маани
- **Максималдуу маани**: Кокус түзүү үчүн максималдуу маани
- **Seed**: Натыйжаларды кайра түзүүгө мүмкүндүк берген кокус урук (демейкиде учурдагы UTC убакыт белгиси колдонулат)

max min маанисинен кичине болсо, функция min жана max маанилеринин ордун автоматтык түрдө алмаштырат.
    	]],
    	["lo"] = [[
ແທນຄ່າຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດດ້ວຍຕົວເລກສຸ່ມລະຫວ່າງ min ແລະ max.

ການກຳນົດຄ່າ:
- **ຄ່າຕ່ຳສຸດ**: ຄ່າຕ່ຳສຸດສຳລັບການສຸ່ມ
- **ຄ່າສູງສຸດ**: ຄ່າສູງສຸດສຳລັບການສຸ່ມ
- **Seed**: ຄ່າເລີ່ມຕົ້ນການສຸ່ມສຳລັບຜົນທີ່ສາມາດເຮັດຊ້ຳໄດ້ (ເລີ່ມຕົ້ນເປັນ timestamp UTC ປັດຈຸບັນ)

ຟັງຊັນຈະສັບປ່ຽນ min ແລະ max ອັດຕະໂນມັດ ຖ້າ max ນ້ອຍກວ່າ min.
    	]],
    	["lv"] = [[
Aizstāj visu ienākošo datu punktu vērtības ar nejaušiem skaitļiem diapazonā no min līdz max.

Konfigurācija:
- **Minimālā vērtība**: Minimālā nejauši ģenerējamā vērtība
- **Maksimālā vērtība**: Maksimālā nejauši ģenerējamā vērtība
- **Sēkla**: Nejaušības sēkla atkārtojamu rezultātu iegūšanai (pēc noklusējuma pašreizējā UTC laika zīmogs)

Funkcija automātiski samaina min un max vietām, ja max ir mazāks par min.
    	]],
    	["lt"] = [[
Pakeičia visas gaunamų duomenų taškų reikšmes atsitiktiniais skaičiais tarp minimumo ir maksimumo.

Konfigūracija:
- **Minimali reikšmė**: Mažiausia atsitiktinai generuojama reikšmė
- **Maksimali reikšmė**: Didžiausia atsitiktinai generuojama reikšmė
- **Sėkla**: Atsitiktinių skaičių sėkla atkuriamiems rezultatams (numatyta dabartinė UTC laiko žyma)

Jei maksimumas mažesnis už minimumą, funkcija juos automatiškai sukeičia vietomis.
    	]],
    	["mk"] = [[
Ги заменува сите влезни вредности на точките на податоци со случајни броеви помеѓу min и max.

Конфигурација:
- **Минимална вредност**: Минималната вредност за случајно генерирање
- **Максимална вредност**: Максималната вредност за случајно генерирање
- **Seed**: Случајно семе за повторливи резултати (стандардно на тековниот UTC временски печат)

Функцијата автоматски ги заменува min и max ако max е помал од min.
    	]],
    	["ms"] = [[
Menggantikan semua nilai titik data yang masuk dengan nombor rawak antara min dan maks.

Konfigurasi:
- **Nilai Min**: Nilai minimum untuk penjanaan rawak
- **Nilai Maks**: Nilai maksimum untuk penjanaan rawak
- **Benih**: Benih rawak untuk hasil yang boleh dihasilkan semula (lalai kepada cap masa UTC semasa)

Fungsi ini menukar min dan maks secara automatik jika maks lebih kecil daripada min.
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളെയും min-നും max-നും ഇടയിലുള്ള റാൻഡം സംഖ്യകളാക്കി മാറ്റുന്നു.

കോൺഫിഗറേഷൻ:
- **Min Value**: റാൻഡം സൃഷ്ടിക്കലിനുള്ള കുറഞ്ഞ മൂല്യം
- **Max Value**: റാൻഡം സൃഷ്ടിക്കലിനുള്ള കൂടിയ മൂല്യം
- **Seed**: ആവർത്തിച്ച് ഒരേ ഫലം ലഭിക്കാനുള്ള റാൻഡം seed (സ്ഥിരസ്ഥിതിയിൽ നിലവിലെ UTC timestamp)

max, min-നേക്കാൾ ചെറുതാണെങ്കിൽ ഫംഗ്ഷൻ min, max മൂല്യങ്ങൾ സ്വയമേവ കൈമാറ്റം ചെയ്യും.
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सची मूल्ये min आणि max यांच्या दरम्यानच्या यादृच्छिक संख्यांनी बदलते.

कॉन्फिगरेशन:
- **किमान मूल्य**: यादृच्छिक निर्मितीसाठीचे किमान मूल्य
- **कमाल मूल्य**: यादृच्छिक निर्मितीसाठीचे कमाल मूल्य
- **सीड**: पुन्हा तयार करता येणाऱ्या परिणामांसाठी यादृच्छिक सीड (डीफॉल्टनुसार वर्तमान UTC टाइमस्टॅम्प)

max हे min पेक्षा लहान असल्यास फंक्शन आपोआप min आणि max अदलाबदल करते.
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн утгыг min болон max-ийн хоорондох санамсаргүй тоогоор солино.

Тохиргоо:
- **Хамгийн бага утга**: Санамсаргүй утга үүсгэх хамгийн бага утга
- **Хамгийн их утга**: Санамсаргүй утга үүсгэх хамгийн их утга
- **Seed**: Давтагдах үр дүнд зориулсан санамсаргүй үр (анхдагчаар одоогийн UTC цагийн тэмдэг)

max нь min-ээс бага байвал функц min болон max-ийн байрлалыг автоматаар солино.
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका मानलाई min र max बीचका यादृच्छिक संख्याले प्रतिस्थापन गर्छ।

कन्फिगरेसन:
- **न्यूनतम मान**: यादृच्छिक उत्पादनका लागि न्यूनतम मान
- **अधिकतम मान**: यादृच्छिक उत्पादनका लागि अधिकतम मान
- **सीड**: पुनरुत्पाद्य परिणामका लागि यादृच्छिक सीड (पूर्वनिर्धारित रूपमा हालको UTC टाइमस्ट्याम्प)

max, min भन्दा सानो भएमा फङ्सनले min र max स्वतः साट्छ।
    	]],
    	["no"] = [[
Erstatter verdiene til alle innkommende datapunkter med tilfeldige tall mellom min og maks.

Konfigurasjon:
- **Minimumsverdi**: Minimumsverdien for tilfeldig generering
- **Maksimumsverdi**: Maksimumsverdien for tilfeldig generering
- **Startverdi**: Tilfeldig startverdi for reproduserbare resultater (standard er gjeldende UTC-tidsstempel)

Funksjonen bytter automatisk om min og maks hvis maks er mindre enn min.
    	]],
    	["pl"] = [[
Zastępuje wszystkie przychodzące wartości punktów danych losowymi liczbami z zakresu od min do max.

Konfiguracja:
- **Wartość minimalna**: Minimalna wartość dla generowania losowego
- **Wartość maksymalna**: Maksymalna wartość dla generowania losowego
- **Ziarno**: Ziarno losowości zapewniające powtarzalne wyniki (domyślnie bieżący znacznik czasu UTC)

Funkcja automatycznie zamienia wartości min i max, jeśli max jest mniejsze od min.
    	]],
    	["pt"] = [[
Substitui todos os valores dos pontos de dados recebidos por números aleatórios entre o mínimo e o máximo.

Configuração:
- **Valor mínimo**: O valor mínimo para a geração aleatória
- **Valor máximo**: O valor máximo para a geração aleatória
- **Semente**: Semente aleatória para resultados reproduzíveis (predefinição: carimbo de data/hora UTC atual)

A função troca automaticamente o mínimo e o máximo se o máximo for menor que o mínimo.
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਮੁੱਲਾਂ ਨੂੰ min ਅਤੇ max ਵਿਚਕਾਰ ਬੇਤਰਤੀਬ ਸੰਖਿਆਵਾਂ ਨਾਲ ਬਦਲਦਾ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਨਿਊਨਤਮ ਮੁੱਲ**: ਬੇਤਰਤੀਬ ਉਤਪਤੀ ਲਈ ਨਿਊਨਤਮ ਮੁੱਲ
- **ਅਧਿਕਤਮ ਮੁੱਲ**: ਬੇਤਰਤੀਬ ਉਤਪਤੀ ਲਈ ਅਧਿਕਤਮ ਮੁੱਲ
- **ਸੀਡ**: ਦੁਹਰਾਏ ਜਾ ਸਕਣ ਵਾਲੇ ਨਤੀਜਿਆਂ ਲਈ ਬੇਤਰਤੀਬ ਸੀਡ (ਮੂਲ ਰੂਪ ਵਿੱਚ ਮੌਜੂਦਾ UTC ਟਾਈਮਸਟੈਂਪ)

ਜੇ max, min ਤੋਂ ਛੋਟਾ ਹੋਵੇ ਤਾਂ ਫੰਕਸ਼ਨ ਆਪਣੇ ਆਪ min ਅਤੇ max ਨੂੰ ਬਦਲ ਦਿੰਦਾ ਹੈ।
    	]],
    	["ro"] = [[
Înlocuiește toate valorile punctelor de date primite cu numere aleatorii între min și max.

Configurare:
- **Valoare minimă**: Valoarea minimă pentru generarea aleatorie
- **Valoare maximă**: Valoarea maximă pentru generarea aleatorie
- **Sămânță**: Sămânță aleatorie pentru rezultate reproductibile (implicit, marcajul temporal UTC curent)

Funcția inversează automat valorile min și max dacă max este mai mic decât min.
    	]],
    	["rm"] = [[
Remplazza tut las valurs dals puncts da datas entrants cun numer casuals tranter min e max.

Configuraziun:
- **Valur minimala**: La valur minimala per la generaziun casuala
- **Valur maximala**: La valur maximala per la generaziun casuala
- **Seed**: Seed casual per resultats reproducibels (standard: timestamp UTC actual)

La funcziun baratta automaticamain min e max, sche max è pli pitschen che min.
    	]],
    	["ru"] = [[
Заменяет все входящие значения точек данных случайными числами между минимумом и максимумом.

Конфигурация:
- **Минимальное значение**: Минимальное значение для генерации случайных чисел
- **Максимальное значение**: Максимальное значение для генерации случайных чисел
- **Начальное число**: Начальное число для воспроизводимых результатов (по умолчанию — текущая временная метка UTC)

Функция автоматически меняет местами минимум и максимум, если максимум меньше минимума.
    	]],
    	["sr"] = [[
Zamenjuje sve dolazne vrednosti tačaka podataka nasumičnim brojevima između minimuma i maksimuma.

Konfiguracija:
- **Minimalna vrednost**: Minimalna vrednost za nasumično generisanje
- **Maksimalna vrednost**: Maksimalna vrednost za nasumično generisanje
- **Seme**: Nasumično seme za ponovljive rezultate (podrazumevano je trenutna UTC vremenska oznaka)

Funkcija automatski zamenjuje minimum i maksimum ako je maksimum manji od minimuma.
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය අගයන් min සහ max අතර අහඹු සංඛ්‍යාවලින් ප්‍රතිස්ථාපනය කරයි.

වින්‍යාසය:
- **අවම අගය**: අහඹු ජනනය සඳහා අවම අගය
- **උපරිම අගය**: අහඹු ජනනය සඳහා උපරිම අගය
- **බීජය**: නැවත නිපදවිය හැකි ප්‍රතිඵල සඳහා අහඹු බීජය (පෙරනිමියෙන් වත්මන් UTC timestamp)

max අගය min අගයට වඩා කුඩා නම් ශ්‍රිතය ස්වයංක්‍රීයව min සහ max මාරු කරයි.
    	]],
    	["sk"] = [[
Nahradí všetky prichádzajúce hodnoty údajových bodov náhodnými číslami medzi minimom a maximom.

Konfigurácia:
- **Minimálna hodnota**: Minimálna hodnota pre náhodné generovanie
- **Maximálna hodnota**: Maximálna hodnota pre náhodné generovanie
- **Seed**: Náhodný seed pre opakovateľné výsledky (predvolene aktuálna časová pečiatka UTC)

Funkcia automaticky vymení minimum a maximum, ak je maximum menšie než minimum.
    	]],
    	["sl"] = [[
Zamenja vse vhodne vrednosti podatkovnih točk z naključnimi števili med min in max.

Konfiguracija:
- **Najmanjša vrednost**: Najmanjša vrednost za naključno ustvarjanje
- **Največja vrednost**: Največja vrednost za naključno ustvarjanje
- **Seme**: Naključno seme za ponovljive rezultate (privzeto trenutni časovni žig UTC)

Funkcija samodejno zamenja min in max, če je max manjši od min.
    	]],
    	["es"] = [[
Reemplaza todos los valores de los puntos de datos entrantes por números aleatorios entre el mínimo y el máximo.

Configuración:
- **Valor mínimo**: El valor mínimo para la generación aleatoria
- **Valor máximo**: El valor máximo para la generación aleatoria
- **Semilla**: Semilla aleatoria para obtener resultados reproducibles (predeterminada: marca de tiempo UTC actual)

La función intercambia automáticamente el mínimo y el máximo si el máximo es menor que el mínimo.
    	]],
    	["sw"] = [[
Hubadilisha thamani zote za nukta za data zinazoingia kuwa nambari nasibu kati ya min na max.

Usanidi:
- **Thamani ya Chini**: Thamani ya chini kabisa ya uzalishaji nasibu
- **Thamani ya Juu**: Thamani ya juu kabisa ya uzalishaji nasibu
- **Mbegu**: Mbegu ya nasibu kwa matokeo yanayoweza kurudiwa (chaguo-msingi ni muhuri wa muda wa sasa wa UTC)

Function hubadilisha moja kwa moja min na max ikiwa max ni ndogo kuliko min.
    	]],
    	["sv"] = [[
Ersätter alla inkommande datapunkters värden med slumptal mellan min och max.

Konfiguration:
- **Minvärde**: Det minsta värdet för slumpgenerering
- **Maxvärde**: Det största värdet för slumpgenerering
- **Seed**: Slumptalsfrö för reproducerbara resultat (standard är aktuell UTC-tidsstämpel)

Funktionen byter automatiskt plats på min och max om max är mindre än min.
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி மதிப்புகளையும் min மற்றும் max இடையிலான சீரற்ற எண்களால் மாற்றுகிறது.

உள்ளமைவு:
- **குறைந்தபட்ச மதிப்பு**: சீரற்ற உருவாக்கத்திற்கான குறைந்தபட்ச மதிப்பு
- **அதிகபட்ச மதிப்பு**: சீரற்ற உருவாக்கத்திற்கான அதிகபட்ச மதிப்பு
- **Seed**: மீண்டும் உருவாக்கக்கூடிய முடிவுகளுக்கான சீரற்ற seed (இயல்புநிலையில் தற்போதைய UTC நேரமுத்திரை)

max, min-ஐவிடக் குறைவாக இருந்தால், Function தானாகவே min மற்றும் max-ஐ மாற்றும்.
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ విలువలను min మరియు max మధ్య యాదృచ్ఛిక సంఖ్యలతో భర్తీ చేస్తుంది.

కాన్ఫిగరేషన్:
- **కనిష్ఠ విలువ**: యాదృచ్ఛిక ఉత్పత్తికి కనిష్ఠ విలువ
- **గరిష్ఠ విలువ**: యాదృచ్ఛిక ఉత్పత్తికి గరిష్ఠ విలువ
- **సీడ్**: పునరుత్పాదక ఫలితాల కోసం యాదృచ్ఛిక సీడ్ (డిఫాల్ట్‌గా ప్రస్తుత UTC టైమ్‌స్టాంప్)

max, min కంటే తక్కువగా ఉంటే ఫంక్షన్ స్వయంచాలకంగా min, maxలను మార్చుతుంది.
    	]],
    	["th"] = [[
แทนที่ค่าจุดข้อมูลขาเข้าทั้งหมดด้วยตัวเลขสุ่มระหว่างค่าต่ำสุดและค่าสูงสุด

การกำหนดค่า:
- **ค่าต่ำสุด**: ค่าต่ำสุดสำหรับการสร้างค่าสุ่ม
- **ค่าสูงสุด**: ค่าสูงสุดสำหรับการสร้างค่าสุ่ม
- **Seed**: ค่าเริ่มต้นสุ่มสำหรับผลลัพธ์ที่ทำซ้ำได้ (ค่าเริ่มต้นคือเวลาประทับ UTC ปัจจุบัน)

ฟังก์ชันจะสลับค่าต่ำสุดและค่าสูงสุดโดยอัตโนมัติ หากค่าสูงสุดน้อยกว่าค่าต่ำสุด
    	]],
    	["tr"] = [[
Gelen tüm veri noktası değerlerini min ve max arasında rastgele sayılarla değiştirir.

Yapılandırma:
- **Minimum Değer**: Rastgele oluşturma için minimum değer
- **Maksimum Değer**: Rastgele oluşturma için maksimum değer
- **Tohum**: Tekrarlanabilir sonuçlar için rastgele tohum (varsayılan olarak geçerli UTC zaman damgası)

max, min'den küçükse işlev min ve max değerlerini otomatik olarak yer değiştirir.
    	]],
    	["uk"] = [[
Замінює всі вхідні значення точок даних випадковими числами між мінімумом і максимумом.

Конфігурація:
- **Мінімальне значення**: Мінімальне значення для генерації випадкових чисел
- **Максимальне значення**: Максимальне значення для генерації випадкових чисел
- **Початкове число**: Початкове число для відтворюваних результатів (типово — поточна часова мітка UTC)

Функція автоматично міняє місцями мінімум і максимум, якщо максимум менший за мінімум.
    	]],
    	["vi"] = [[
Thay thế tất cả giá trị điểm dữ liệu đầu vào bằng các số ngẫu nhiên giữa min và max.

Cấu hình:
- **Giá trị tối thiểu**: Giá trị tối thiểu để tạo số ngẫu nhiên
- **Giá trị tối đa**: Giá trị tối đa để tạo số ngẫu nhiên
- **Seed**: Seed ngẫu nhiên để cho ra kết quả có thể tái lập (mặc định là dấu thời gian UTC hiện tại)

Hàm tự động hoán đổi min và max nếu max nhỏ hơn min.
    	]],
    },
    config = {
        number {
            id = "min_value",
            name = "_min_value",
            default = 0.0,
        },
        number {
            id = "max_value",
            name = "_max_value",
            default = 1.0,
        },
        uint {
            id = "seed",
            name = "_seed",
            default = default_seed,
        },
    },

    -- Generator function
    generator = function(source, config)
        local min_val = config and config.min_value or 0.0
        local max_val = config and config.max_value or 1.0
        local seed = config and config.seed or core.time().timestamp

        -- Ensure min is always the smaller value
        if min_val > max_val then
            min_val, max_val = max_val, min_val
        end

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            -- Generate random value between min and max
            local rng = random.new_seeded_random(seed, data_point.timestamp)
            data_point.value = rng:next(min_val, max_val)

            return data_point
        end
    end,
}
