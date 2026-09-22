-- Lua Function to filter data points by value (greater than threshold)
-- Only passes through data points with values greater than a threshold

local tng_config = require("tng.config")
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    -- Configuration metadata
    id = "filter-greater-than",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_filter"},
    title = {
    	["en"] = "Filter Greater Than",
    	["af"] = "Filtreer Groter As",
    	["sq"] = "Filtro më të mëdha se",
    	["am"] = "ከዚህ በላይ አጣራ",
    	["hy"] = "Զտել մեծ արժեքները",
    	["az"] = "Böyük olanları süzgəcdən keçir",
    	["bn"] = "এর চেয়ে বেশি ফিল্টার করুন",
    	["eu"] = "Iragazi hau baino handiagoa",
    	["be"] = "Фільтраваць большыя за",
    	["bg"] = "Филтриране над",
    	["my"] = "ထက်ကြီးသော တန်ဖိုးများကို စစ်ထုတ်ရန်",
    	["ca"] = "Filtra els valors superiors a",
    	["zh-Hans"] = "筛选大于",
    	["zh-Hant"] = "篩選大於",
    	["hr"] = "Filtriraj veće od",
    	["cs"] = "Filtrovat větší než",
    	["da"] = "Filtrér større end",
    	["nl"] = "Groter dan filteren",
    	["et"] = "Filtreeri suuremad kui",
    	["fil"] = "Salain ang Mas Mataas sa",
    	["fi"] = "Suodata suuremmat kuin",
    	["fr"] = "Filtrer supérieur à",
    	["gl"] = "Filtrar maiores que",
    	["ka"] = "მეტზე დიდი მნიშვნელობების ფილტრაცია",
    	["de"] = "Größer als filtern",
    	["el"] = "Φιλτράρισμα μεγαλύτερων τιμών",
    	["gu"] = "કરતાં વધુ ફિલ્ટર કરો",
    	["hi"] = "इससे अधिक फ़िल्टर करें",
    	["hu"] = "Szűrés nagyobb értékre",
    	["is"] = "Sía stærra en",
    	["id"] = "Saring Lebih Besar dari",
    	["it"] = "Filtra maggiore di",
    	["ja"] = "指定値より大きい値をフィルタ",
    	["kn"] = "ಇದಕ್ಕಿಂತ ಹೆಚ್ಚಿನದನ್ನು ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Үлкен мәндерді сүзу",
    	["km"] = "ត្រងធំជាង",
    	["ko"] = "초과 값 필터링",
    	["ky"] = "Чоң маанилерди чыпкалоо",
    	["lo"] = "ກັ່ນຕອງຫຼາຍກວ່າ",
    	["lv"] = "Filtrēt lielākus par",
    	["lt"] = "Filtruoti didesnes už",
    	["mk"] = "Филтрирај поголемо од",
    	["ms"] = "Tapis Lebih Besar Daripada",
    	["ml"] = "ഇതിലും കൂടുതലുള്ളവ ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "यापेक्षा मोठे फिल्टर करा",
    	["mn"] = "Их утгаар шүүх",
    	["ne"] = "भन्दा ठूलो फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer større enn",
    	["pl"] = "Filtruj większe niż",
    	["pt"] = "Filtrar maior que",
    	["pa"] = "ਤੋਂ ਵੱਧ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează mai mare decât",
    	["rm"] = "Filtrar pli grond che",
    	["ru"] = "Фильтр: больше",
    	["sr"] = "Filtriraj veće od",
    	["si"] = "වඩා වැඩි දේ පෙරහන් කරන්න",
    	["sk"] = "Filtrovať väčšie než",
    	["sl"] = "Filtriraj večje od",
    	["es"] = "Filtrar mayores que",
    	["sw"] = "Chuja Kubwa Kuliko",
    	["sv"] = "Filtrera större än",
    	["ta"] = "விட அதிகமானவற்றை வடிகட்டு",
    	["te"] = "కంటే ఎక్కువను ఫిల్టర్ చేయి",
    	["th"] = "กรองค่ามากกว่า",
    	["tr"] = "Büyüktür Filtresi",
    	["uk"] = "Фільтрувати більше ніж",
    	["vi"] = "Lọc lớn hơn",
    },
    description = {
    	["en"] = [[
Filters data points by value. Only data points with values greater than the threshold will pass through.

Configuration:
- **Threshold**: The minimum value (exclusive by default)
- **Include Equal**: Also include values equal to the threshold (default: false)
    	]],
    	["af"] = [[
Filtreer datapunte volgens waarde. Slegs datapunte met waardes groter as die drempel word deurgegee.

Konfigurasie:
- **Drempel**: Die minimumwaarde (standaard eksklusief)
- **Sluit Gelykes In**: Sluit ook waardes in wat gelyk aan die drempel is (verstek: vals)
    	]],
    	["sq"] = [[
Filtron pikat e të dhënave sipas vlerës. Vetëm pikat e të dhënave me vlera më të mëdha se pragu do të kalojnë.

Konfigurimi:
- **Pragu**: Vlera minimale (përjashtuese si parazgjedhje)
- **Përfshi të barabartat**: Përfshin edhe vlerat të barabarta me pragun (parazgjedhja: false)
    	]],
    	["am"] = [[
የውሂብ ነጥቦችን በእሴት ያጣራል። ከመነሻ ዋጋው በላይ ያላቸው የውሂብ ነጥቦች ብቻ ያልፋሉ።

ውቅር፦
- **መነሻ ዋጋ**፦ ዝቅተኛው እሴት (በነባሪ እኩል እሴትን አያካትትም)
- **እኩል የሆነውን አካትት**፦ ከመነሻ ዋጋው ጋር እኩል የሆኑ እሴቶችንም ማካተት (ነባሪ፦ false)
    	]],
    	["hy"] = [[
Զտում է տվյալակետերն ըստ արժեքի։ Անցնում են միայն շեմից մեծ արժեք ունեցող տվյալակետերը։

Կազմաձևում՝
- **Շեմ**․ նվազագույն արժեքը (կանխադրված՝ բացառող)
- **Հավասարը ներառել**․ ներառել նաև շեմին հավասար արժեքները (կանխադրված՝ false)
    	]],
    	["az"] = [[
Məlumat nöqtələrini qiymətə görə süzgəcdən keçirir. Yalnız qiyməti həddən böyük olan məlumat nöqtələri keçir.

Konfiqurasiya:
- **Hədd**: Minimum qiymət (standart olaraq daxil edilmir)
- **Bərabər olanı daxil et**: Həddə bərabər qiymətləri də daxil et (standart: false)
    	]],
    	["bn"] = [[
মান অনুযায়ী ডেটা পয়েন্ট ফিল্টার করে। কেবল সীমার চেয়ে বেশি মানের ডেটা পয়েন্টগুলোই পাস করবে।

কনফিগারেশন:
- **সীমা**: সর্বনিম্ন মান (ডিফল্টভাবে সমান মান অন্তর্ভুক্ত নয়)
- **সমান অন্তর্ভুক্ত করুন**: সীমার সমান মানও অন্তর্ভুক্ত করুন (ডিফল্ট: false)
    	]],
    	["eu"] = [[
Datu-puntuak balioaren arabera iragazten ditu. Atalasea baino balio handiagoa duten datu-puntuak soilik igaroko dira.

Konfigurazioa:
- **Atalasea**: Gutxieneko balioa (lehenespenez, barne hartu gabe)
- **Berdina barne hartu**: Atalasearen berdinak diren balioak ere sartzea (lehenetsia: false)
    	]],
    	["be"] = [[
Фільтруе кропкі даных па значэнні. Прапускаюцца толькі кропкі даных са значэннямі, большымі за парог.

Канфігурацыя:
- **Парог**: мінімальнае значэнне (па змаўчанні не ўключае роўныя)
- **Уключаць роўныя**: таксама ўключаць значэнні, роўныя парогу (па змаўчанні: false)
    	]],
    	["bg"] = [[
Филтрира точките от данни по стойност. Преминават само точките от данни със стойности над прага.

Конфигурация:
- **Праг**: Минималната стойност (по подразбиране изключва равните)
- **Включване на равните**: Включва и стойностите, равни на прага (по подразбиране: false)
    	]],
    	["my"] = [[
ဒေတာမှတ်များကို တန်ဖိုးအပေါ် အခြေခံ၍ စစ်ထုတ်သည်။ ကန့်သတ်ချက်ထက် ကြီးသော တန်ဖိုးရှိ ဒေတာမှတ်များသာ ဖြတ်သန်းမည်။

ဖွဲ့စည်းမှု:
- **ကန့်သတ်ချက်**: အနည်းဆုံးတန်ဖိုး (မူလအားဖြင့် သီးသန့်)
- **တူညီသောတန်ဖိုး ပါဝင်ရန်**: ကန့်သတ်ချက်နှင့် တူညီသော တန်ဖိုးများကိုလည်း ပါဝင်စေသည် (မူလ: false)
    	]],
    	["ca"] = [[
Filtra els punts de dades pel valor. Només passen els punts de dades amb valors superiors al llindar.

Configuració:
- **Llindar**: El valor mínim (exclusiu per defecte)
- **Inclou els iguals**: Inclou també els valors iguals al llindar (per defecte: false)
    	]],
    	["zh-Hans"] = [[
根据值筛选数据点。只有值大于阈值的数据点才会通过。

配置：
- **阈值**：最小值（默认不包含等于阈值的值）
- **包含相等值**：同时包含等于阈值的值（默认：false）
    	]],
    	["zh-Hant"] = [[
依值篩選資料點。只有值大於閾值的資料點才會通過。

設定：
- **閾值**：最小值（預設為不包含等於）
- **包含相等值**：也包含等於閾值的值（預設：false）
    	]],
    	["hr"] = [[
Filtrira podatkovne točke prema vrijednosti. Prolaze samo podatkovne točke čije su vrijednosti veće od praga.

Konfiguracija:
- **Prag**: Najmanja vrijednost (zadano isključivo)
- **Uključi jednako**: Uključi i vrijednosti jednake pragu (zadano: false)
    	]],
    	["cs"] = [[
Filtruje datové body podle hodnoty. Projdou pouze datové body s hodnotami vyššími než práh.

Konfigurace:
- **Práh**: Minimální hodnota (ve výchozím nastavení bez rovnosti)
- **Včetně rovnosti**: Zahrnout také hodnoty rovné prahu (výchozí: false)
    	]],
    	["da"] = [[
Filtrerer datapunkter efter værdi. Kun datapunkter med værdier over grænsen går videre.

Konfiguration:
- **Grænse**: Minimumsværdien (eksklusiv som standard)
- **Medtag lig med**: Medtag også værdier, der er lig med grænsen (standard: false)
    	]],
    	["nl"] = [[
Filtert gegevenspunten op waarde. Alleen gegevenspunten met waarden groter dan de drempel worden doorgelaten.

Configuratie:
- **Drempel**: De minimumwaarde (standaard exclusief)
- **Gelijk aan opnemen**: Ook waarden opnemen die gelijk zijn aan de drempel (standaard: false)
    	]],
    	["et"] = [[
Filtreerib andmepunkte väärtuse alusel. Läbivad ainult lävest suurema väärtusega andmepunktid.

Seadistus:
- **Lävi**: minimaalne väärtus (vaikimisi välistav)
- **Kaasa võrdsed**: kaasa ka lävega võrdsed väärtused (vaikimisi: väär)
    	]],
    	["fil"] = [[
Sinasala ang mga data point ayon sa value. Tanging mga data point na may halagang higit sa threshold ang magpapatuloy.

Configuration:
- **Threshold**: Pinakamababang halaga (hindi kasama bilang default)
- **Include Equal**: Isama rin ang mga halagang katumbas ng threshold (default: false)
    	]],
    	["fi"] = [[
Suodattaa datapisteet arvon perusteella. Vain kynnysarvoa suuremmat arvot päästetään läpi.

Määritys:
- **Kynnysarvo**: Vähimmäisarvo (oletuksena rajaa ei sisällytetä)
- **Sisällytä yhtä suuret**: Sisällytä myös kynnysarvon kanssa yhtä suuret arvot (oletus: false)
    	]],
    	["fr"] = [[
Filtre les points de données selon leur valeur. Seuls les points dont la valeur est supérieure au seuil sont conservés.

Configuration :
- **Seuil** : Valeur minimale (exclue par défaut)
- **Inclure l’égalité** : Inclure également les valeurs égales au seuil (par défaut : false)
    	]],
    	["gl"] = [[
Filtra os puntos de datos polo valor. Só pasan os puntos de datos con valores superiores ao limiar.

Configuración:
- **Limiar**: O valor mínimo (exclusivo de forma predeterminada)
- **Incluír iguais**: Incluír tamén os valores iguais ao limiar (predeterminado: false)
    	]],
    	["ka"] = [[
ფილტრავს მონაცემთა წერტილებს მნიშვნელობის მიხედვით. გაივლის მხოლოდ ზღვარზე მეტი მნიშვნელობის მქონე მონაცემთა წერტილები.

კონფიგურაცია:
- **ზღვარი**: მინიმალური მნიშვნელობა (ნაგულისხმევად, ზღვარი არ შედის)
- **ტოლი მნიშვნელობის ჩართვა**: ასევე ჩართოს ზღვრის ტოლი მნიშვნელობები (ნაგულისხმევი: false)
    	]],
    	["de"] = [[
Filtert Datenpunkte nach ihrem Wert. Nur Datenpunkte mit Werten über dem Schwellenwert werden weitergegeben.

Konfiguration:
- **Schwellenwert**: Der Mindestwert (standardmäßig exklusiv)
- **Gleich einschließen**: Auch Werte einschließen, die dem Schwellenwert entsprechen (Standard: false)
    	]],
    	["el"] = [[
Φιλτράρει τα σημεία δεδομένων με βάση την τιμή. Περνούν μόνο τα σημεία δεδομένων με τιμές μεγαλύτερες από το όριο.

Διαμόρφωση:
- **Όριο**: Η ελάχιστη τιμή (αποκλειστικό από προεπιλογή)
- **Συμπερίληψη ίσων**: Συμπερίληψη και τιμών ίσων με το όριο (προεπιλογή: false)
    	]],
    	["gu"] = [[
ડેટા પોઇન્ટ્સને મૂલ્ય દ્વારા ફિલ્ટર કરે છે. મર્યાદા કરતાં વધુ મૂલ્ય ધરાવતા ડેટા પોઇન્ટ્સ જ પસાર થશે.

ગોઠવણી:
- **મર્યાદા**: લઘુત્તમ મૂલ્ય (ડિફૉલ્ટ રીતે સમાન મૂલ્ય સામેલ નથી)
- **સમાન મૂલ્ય સામેલ કરો**: મર્યાદા સમાન મૂલ્યો પણ સામેલ કરો (ડિફૉલ્ટ: false)
    	]],
    	["hi"] = [[
डेटा पॉइंट को मान के आधार पर फ़िल्टर करता है। केवल थ्रेशोल्ड से अधिक मान वाले डेटा पॉइंट आगे जाते हैं।

कॉन्फ़िगरेशन:
- **थ्रेशोल्ड**: न्यूनतम मान (डिफ़ॉल्ट रूप से अनन्य)
- **बराबर शामिल करें**: थ्रेशोल्ड के बराबर मान भी शामिल करें (डिफ़ॉल्ट: false)
    	]],
    	["hu"] = [[
Az adatpontokat érték alapján szűri. Csak a küszöbnél nagyobb értékű adatpontok haladnak tovább.

Konfiguráció:
- **Küszöbérték**: A minimális érték (alapértelmezés szerint kizáró)
- **Egyenlő értékek belefoglalása**: A küszöbértékkel egyenlő értékeket is belefoglalja (alapértelmezett: false)
    	]],
    	["is"] = [[
Síar gagnapunkta eftir gildi. Aðeins gagnapunktar með gildi yfir mörkunum fara áfram.

Stillingar:
- **Mörk**: Lágmarksgildi (útilokað sjálfgefið)
- **Taka með jafnt**: Taka einnig með gildi sem eru jöfn mörkunum (sjálfgefið: false)
    	]],
    	["id"] = [[
Menyaring titik data berdasarkan nilai. Hanya titik data dengan nilai lebih besar dari ambang yang akan diteruskan.

Konfigurasi:
- **Ambang**: Nilai minimum (secara default tidak termasuk nilai yang sama)
- **Sertakan yang Sama**: Sertakan juga nilai yang sama dengan ambang (default: false)
    	]],
    	["it"] = [[
Filtra i punti dati in base al valore. Passano solo i punti dati con valori superiori alla soglia.

Configurazione:
- **Soglia**: il valore minimo (esclusivo per impostazione predefinita)
- **Includi uguale**: include anche i valori uguali alla soglia (predefinito: false)
    	]],
    	["ja"] = [[
値に基づいてデータポイントを絞り込みます。しきい値より大きい値を持つデータポイントだけが通過します。

設定:
- **しきい値**: 最小値（デフォルトでは除外）
- **等しい値を含める**: しきい値と等しい値も含める（デフォルト: false）
    	]],
    	["kn"] = [[
ಮೌಲ್ಯದ ಆಧಾರದ ಮೇಲೆ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ. ಮಿತಿಗಿಂತ ಹೆಚ್ಚಿನ ಮೌಲ್ಯವಿರುವ ಡೇಟಾ ಬಿಂದುಗಳು ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತವೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಮಿತಿ**: ಕನಿಷ್ಠ ಮೌಲ್ಯ (ಡೀಫಾಲ್ಟ್ ಆಗಿ ಒಳಗೊಂಡಿಲ್ಲ)
- **ಸಮನನ್ನು ಒಳಗೊಂಡಿರಿ**: ಮಿತಿಗೆ ಸಮನಾದ ಮೌಲ್ಯಗಳನ್ನೂ ಒಳಗೊಂಡಿರಿ (ಡೀಫಾಲ್ಟ್: false)
    	]],
    	["kk"] = [[
Дерек нүктелерін мәні бойынша сүзеді. Шектен жоғары мәні бар дерек нүктелері ғана өтеді.

Конфигурация:
- **Шек**: Ең төменгі мән (әдепкі бойынша қосылмайды)
- **Тең мәнді қосу**: Шекке тең мәндерді де қосу (әдепкі: false)
    	]],
    	["km"] = [[
ត្រងចំណុចទិន្នន័យតាមតម្លៃ។ មានតែចំណុចទិន្នន័យដែលមានតម្លៃធំជាងកម្រិតប៉ុណ្ណោះ នឹងត្រូវបានបញ្ជូនបន្ត។

ការកំណត់៖
- **កម្រិត**៖ តម្លៃអប្បបរមា (តាមលំនាំដើម មិនរាប់តម្លៃស្មើ)
- **រួមបញ្ចូលតម្លៃស្មើ**៖ រួមបញ្ចូលតម្លៃដែលស្មើនឹងកម្រិតផងដែរ (លំនាំដើម៖ false)
    	]],
    	["ko"] = [[
값으로 데이터 포인트를 필터링합니다. 임계값보다 큰 값을 가진 데이터 포인트만 통과합니다.

구성:
- **임계값**: 최소값(기본적으로 초과)
- **같은 값 포함**: 임계값과 같은 값도 포함합니다(기본값: false)
    	]],
    	["ky"] = [[
Маалымат чекиттерин мааниси боюнча чыпкалайт. Чектен чоң мааниге ээ болгон маалымат чекиттери гана өткөрүлөт.

Тууралоо:
- **Чек**: Минималдуу маани (демейкиде кошулбайт)
- **Теңдерди кошуу**: Чекке тең маанилерди да кошуу (демейки: false)
    	]],
    	["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນຕາມຄ່າ. ສະເພາະຈຸດຂໍ້ມູນທີ່ມີຄ່າຫຼາຍກວ່າຄ່າເກນເທົ່ານັ້ນຈຶ່ງຈະຜ່ານ.

ການຕັ້ງຄ່າ:
- **ຄ່າເກນ**: ຄ່າຕ່ຳສຸດ (ໂດຍຄ່າເລີ່ມຕົ້ນບໍ່ລວມຄ່າເທົ່າກັນ)
- **ລວມຄ່າເທົ່າກັນ**: ລວມຄ່າທີ່ເທົ່າກັບຄ່າເກນ (ຄ່າເລີ່ມຕົ້ນ: false)
    	]],
    	["lv"] = [[
Filtrē datu punktus pēc vērtības. Tiks nodoti tikai datu punkti, kuru vērtības pārsniedz slieksni.

Konfigurācija:
- **Slieksnis**: Minimālā vērtība (pēc noklusējuma neietverot vienādu vērtību)
- **Iekļaut vienādus**: Iekļaut arī slieksnim vienādas vērtības (noklusējums: false)
    	]],
    	["lt"] = [[
Filtruoja duomenų taškus pagal reikšmę. Toliau perduodami tik duomenų taškai, kurių reikšmės didesnės už ribą.

Konfigūracija:
- **Riba**: Mažiausia reikšmė (pagal numatytuosius nustatymus neįtraukiama)
- **Įtraukti lygiąsias**: Taip pat įtraukti ribai lygias reikšmes (numatyta: false)
    	]],
    	["mk"] = [[
Ги филтрира точките на податоци според вредноста. Само точките на податоци со вредности поголеми од прагот ќе поминат.

Конфигурација:
- **Праг**: Минималната вредност (стандардно исклучителна)
- **Вклучи еднакви**: Вклучи ги и вредностите еднакви на прагот (стандардно: false)
    	]],
    	["ms"] = [[
Menapis titik data berdasarkan nilai. Hanya titik data dengan nilai yang melebihi ambang akan diteruskan.

Konfigurasi:
- **Ambang**: Nilai minimum (eksklusif secara lalai)
- **Sertakan Sama**: Sertakan juga nilai yang sama dengan ambang (lalai: false)
    	]],
    	["ml"] = [[
മൂല്യം പ്രകാരം ഡാറ്റാ പോയിന്റുകൾ ഫിൽട്ടർ ചെയ്യുന്നു. പരിധിയേക്കാൾ കൂടുതലുള്ള മൂല്യമുള്ള ഡാറ്റാ പോയിന്റുകൾ മാത്രമേ കടന്നുപോകൂ.

കോൺഫിഗറേഷൻ:
- **Threshold**: കുറഞ്ഞ മൂല്യം (സ്ഥിരസ്ഥിതിയിൽ സമതുല്യം ഉൾപ്പെടില്ല)
- **Include Equal**: പരിധിക്ക് തുല്യമായ മൂല്യങ്ങളും ഉൾപ്പെടുത്തുക (സ്ഥിരസ്ഥിതി: false)
    	]],
    	["mr"] = [[
डेटा पॉइंट्स मूल्यांनुसार फिल्टर करतो. उंबरठ्यापेक्षा जास्त मूल्य असलेले डेटा पॉइंट्सच पुढे जातील.

कॉन्फिगरेशन:
- **उंबरठा**: किमान मूल्य (डीफॉल्टनुसार वगळून)
- **समान समाविष्ट करा**: उंबरठ्याइतकी मूल्येही समाविष्ट करा (डीफॉल्ट: false)
    	]],
    	["mn"] = [[
Өгөгдлийн цэгүүдийг утгаар нь шүүнэ. Босгоноос их утгатай өгөгдлийн цэгүүд л нэвтэрнэ.

Тохиргоо:
- **Босго**: Хамгийн бага утга (анхдагчаар тэнцүү утгыг оруулахгүй)
- **Тэнцүүг оруулах**: Босготой тэнцүү утгуудыг мөн оруулах (анхдагч: false)
    	]],
    	["ne"] = [[
डेटा बिन्दुहरूलाई मानअनुसार फिल्टर गर्छ। सीमाभन्दा ठूलो मान भएका डेटा बिन्दुहरू मात्र अघि पठाइन्छन्।

कन्फिगरेसन:
- **सीमा**: न्यूनतम मान (पूर्वनिर्धारित रूपमा समावेश हुँदैन)
- **बराबर समावेश गर्ने**: सीमासँग बराबर मानहरू पनि समावेश गर्ने (पूर्वनिर्धारित: false)
    	]],
    	["no"] = [[
Filtrerer datapunkter etter verdi. Bare datapunkter med verdier større enn terskelen, slipper gjennom.

Konfigurasjon:
- **Terskel**: Minimumsverdien (eksklusiv som standard)
- **Inkluder lik**: Inkluder også verdier som er lik terskelen (standard: false)
    	]],
    	["pl"] = [[
Filtruje punkty danych według wartości. Przechodzą tylko punkty danych o wartościach większych od progu.

Konfiguracja:
- **Próg**: Minimalna wartość (domyślnie wykluczająca równą wartość)
- **Uwzględnij równe**: Uwzględniaj także wartości równe progowi (domyślnie: false)
    	]],
    	["pt"] = [[
Filtra os pontos de dados pelo valor. Apenas os pontos de dados com valores superiores ao limite passam.

Configuração:
- **Limite**: O valor mínimo (exclusivo por predefinição)
- **Incluir iguais**: Incluir também valores iguais ao limite (predefinição: false)
    	]],
    	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਮੁੱਲ ਅਨੁਸਾਰ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਸਿਰਫ਼ ਸੀਮਾ ਤੋਂ ਵੱਧ ਮੁੱਲ ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਅੱਗੇ ਜਾਣਗੇ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਸੀਮਾ**: ਘੱਟੋ-ਘੱਟ ਮੁੱਲ (ਡਿਫਾਲਟ ਤੌਰ ’ਤੇ ਵਿਸ਼ੇਸ਼)
- **ਬਰਾਬਰ ਸ਼ਾਮਲ ਕਰੋ**: ਸੀਮਾ ਦੇ ਬਰਾਬਰ ਮੁੱਲ ਵੀ ਸ਼ਾਮਲ ਕਰੋ (ਡਿਫਾਲਟ: false)
    	]],
    	["ro"] = [[
Filtrează punctele de date după valoare. Trec mai departe doar punctele de date cu valori mai mari decât pragul.

Configurare:
- **Prag**: Valoarea minimă (exclusiv în mod implicit)
- **Include egal**: Include și valorile egale cu pragul (implicit: false)
    	]],
    	["rm"] = [[
Filtra ils puncts da datas tenor la valur. Mo ils puncts da datas cun valurs pli grondas che la limita vegnan transmess.

Configuraziun:
- **Limita**: La valur minimala (exclusiva per default)
- **Includer egual**: Includer era las valurs egualas a la limita (default: false)
    	]],
    	["ru"] = [[
Фильтрует точки данных по значению. Пропускаются только точки данных со значениями выше порога.

Конфигурация:
- **Порог**: Минимальное значение (по умолчанию исключительно)
- **Включать равные**: Также включать значения, равные порогу (по умолчанию: false)
    	]],
    	["sr"] = [[
Filtrira tačke podataka prema vrednosti. Prolaze samo tačke podataka čije su vrednosti veće od praga.

Konfiguracija:
- **Prag**: Minimalna vrednost (podrazumevano isključiva)
- **Uključi jednake**: Uključuje i vrednosti jednake pragu (podrazumevano: false)
    	]],
    	["si"] = [[
දත්ත ලක්ෂ්‍ය ඒවායේ අගය අනුව පෙරහන් කරයි. සීමාවට වඩා වැඩි අගයන් සහිත දත්ත ලක්ෂ්‍ය පමණක් ඉදිරියට යවයි.

වින්‍යාසය:
- **සීමාව**: අවම අගය (පෙරනිමියෙන් සමාන අගය ඇතුළත් නොවේ)
- **සමාන ඇතුළත් කරන්න**: සීමාවට සමාන අගයන් ද ඇතුළත් කිරීම (පෙරනිමිය: false)
    	]],
    	["sk"] = [[
Filtruje údajové body podľa hodnoty. Prejdú iba údajové body s hodnotami vyššími než prah.

Konfigurácia:
- **Prah**: Minimálna hodnota (predvolene bez rovnosti)
- **Zahrnúť rovné**: Zahrnúť aj hodnoty rovné prahu (predvolené: false)
    	]],
    	["sl"] = [[
Filtrira podatkovne točke glede na vrednost. Prepustijo se le podatkovne točke z vrednostmi, večjimi od praga.

Konfiguracija:
- **Prag**: Najmanjša vrednost (privzeto izključna)
- **Vključi enako**: Vključi tudi vrednosti, enake pragu (privzeto: false)
    	]],
    	["es"] = [[
Filtra los puntos de datos por valor. Solo pasan los puntos de datos cuyos valores son mayores que el umbral.

Configuración:
- **Umbral**: El valor mínimo (exclusivo de forma predeterminada)
- **Incluir iguales**: Incluir también los valores iguales al umbral (predeterminado: false)
    	]],
    	["sw"] = [[
Huchuja nukta za data kulingana na thamani. Ni nukta za data zenye thamani kubwa kuliko kikomo pekee ndizo zitapitishwa.

Usanidi:
- **Kikomo**: Thamani ya chini kabisa (haitajumuisha kikomo kwa chaguo-msingi)
- **Jumuisha Sawa**: Pia jumuisha thamani zilizo sawa na kikomo (chaguo-msingi: false)
    	]],
    	["sv"] = [[
Filtrerar datapunkter efter värde. Endast datapunkter med värden större än tröskeln släpps igenom.

Konfiguration:
- **Tröskel**: Det minsta värdet (exklusivt som standard)
- **Inkludera lika med**: Inkludera även värden som är lika med tröskeln (standard: false)
    	]],
    	["ta"] = [[
மதிப்பின் அடிப்படையில் தரவுப் புள்ளிகளை வடிகட்டுகிறது. வரம்பைவிட அதிகமான மதிப்புகளைக் கொண்ட புள்ளிகள் மட்டுமே தொடரும்.

உள்ளமைவு:
- **வரம்பு**: குறைந்தபட்ச மதிப்பு (இயல்புநிலையில் வரம்பு சேர்க்கப்படாது)
- **சமமானவற்றைச் சேர்**: வரம்பிற்குச் சமமான மதிப்புகளையும் சேர்க்கவும் (இயல்புநிலை: false)
    	]],
    	["te"] = [[
విలువ ఆధారంగా డేటా పాయింట్లను ఫిల్టర్ చేస్తుంది. పరిమితి కంటే ఎక్కువ విలువ ఉన్న డేటా పాయింట్లు మాత్రమే కొనసాగుతాయి.

కాన్ఫిగరేషన్:
- **పరిమితి**: కనిష్ఠ విలువ (డిఫాల్ట్‌గా సమాన విలువను మినహాయిస్తుంది)
- **సమానమైనదాన్ని చేర్చు**: పరిమితికి సమానమైన విలువలను కూడా చేర్చండి (డిఫాల్ట్: false)
    	]],
    	["th"] = [[
กรองจุดข้อมูลตามค่า เฉพาะจุดข้อมูลที่มีค่ามากกว่าเกณฑ์เท่านั้นที่จะถูกส่งต่อ

การกำหนดค่า:
- **เกณฑ์**: ค่าต่ำสุด (โดยค่าเริ่มต้นไม่รวมค่าที่เท่ากัน)
- **รวมค่าที่เท่ากัน**: รวมค่าที่เท่ากับเกณฑ์ด้วย (ค่าเริ่มต้น: false)
    	]],
    	["tr"] = [[
Veri noktalarını değerlerine göre filtreler. Yalnızca eşikten büyük değerlere sahip veri noktaları geçer.

Yapılandırma:
- **Eşik**: Minimum değer (varsayılan olarak hariç)
- **Eşit Olanları Dahil Et**: Eşiğe eşit değerleri de dahil et (varsayılan: false)
    	]],
    	["uk"] = [[
Фільтрує точки даних за значенням. Пропускаються лише точки даних зі значеннями, більшими за поріг.

Конфігурація:
- **Поріг**: Мінімальне значення (типово без включення порогу)
- **Включати рівні**: Також включати значення, рівні порогу (типово: false)
    	]],
    	["vi"] = [[
Lọc các điểm dữ liệu theo giá trị. Chỉ những điểm dữ liệu có giá trị lớn hơn ngưỡng mới được giữ lại.

Cấu hình:
- **Ngưỡng**: Giá trị tối thiểu (mặc định không bao gồm giá trị bằng ngưỡng)
- **Bao gồm bằng**: Cũng bao gồm các giá trị bằng ngưỡng (mặc định: false)
    	]],
    },
    config = {
        number {
            id = "threshold",
            name = {
            	["en"] = "Threshold",
            	["af"] = "Drempel",
            	["sq"] = "Pragu",
            	["am"] = "መነሻ ዋጋ",
            	["hy"] = "Շեմ",
            	["az"] = "Hədd",
            	["bn"] = "সীমা",
            	["eu"] = "Atalasea",
            	["be"] = "Парог",
            	["bg"] = "Праг",
            	["my"] = "ကန့်သတ်ချက်",
            	["ca"] = "Llindar",
            	["zh-Hans"] = "阈值",
            	["zh-Hant"] = "閾值",
            	["hr"] = "Prag",
            	["cs"] = "Práh",
            	["da"] = "Grænse",
            	["nl"] = "Drempel",
            	["et"] = "Lävi",
            	["fil"] = "Threshold",
            	["fi"] = "Kynnysarvo",
            	["fr"] = "Seuil",
            	["gl"] = "Limiar",
            	["ka"] = "ზღვარი",
            	["de"] = "Schwellenwert",
            	["el"] = "Όριο",
            	["gu"] = "મર્યાદા",
            	["hi"] = "थ्रेशोल्ड",
            	["hu"] = "Küszöbérték",
            	["is"] = "Mörk",
            	["id"] = "Ambang",
            	["it"] = "Soglia",
            	["ja"] = "しきい値",
            	["kn"] = "ಮಿತಿ",
            	["kk"] = "Шек",
            	["km"] = "កម្រិត",
            	["ko"] = "임계값",
            	["ky"] = "Чек",
            	["lo"] = "ຄ່າເກນ",
            	["lv"] = "Slieksnis",
            	["lt"] = "Riba",
            	["mk"] = "Праг",
            	["ms"] = "Ambang",
            	["ml"] = "പരിധി",
            	["mr"] = "उंबरठा",
            	["mn"] = "Босго",
            	["ne"] = "सीमा",
            	["no"] = "Terskel",
            	["pl"] = "Próg",
            	["pt"] = "Limite",
            	["pa"] = "ਸੀਮਾ",
            	["ro"] = "Prag",
            	["rm"] = "Limita",
            	["ru"] = "Порог",
            	["sr"] = "Prag",
            	["si"] = "සීමාව",
            	["sk"] = "Prah",
            	["sl"] = "Prag",
            	["es"] = "Umbral",
            	["sw"] = "Kikomo",
            	["sv"] = "Tröskel",
            	["ta"] = "வரம்பு",
            	["te"] = "పరిమితి",
            	["th"] = "เกณฑ์",
            	["tr"] = "Eşik",
            	["uk"] = "Поріг",
            	["vi"] = "Ngưỡng",
            },
        },
        checkbox {
            id = "include_equal",
            name = {
            	["en"] = "Include Equal",
            	["af"] = "Sluit Gelykes In",
            	["sq"] = "Përfshi të barabartat",
            	["am"] = "እኩል የሆነውን አካትት",
            	["hy"] = "Հավասարը ներառել",
            	["az"] = "Bərabər olanı daxil et",
            	["bn"] = "সমান অন্তর্ভুক্ত করুন",
            	["eu"] = "Berdina barne hartu",
            	["be"] = "Уключаць роўныя",
            	["bg"] = "Включване на равните",
            	["my"] = "တူညီသောတန်ဖိုး ပါဝင်ရန်",
            	["ca"] = "Inclou els iguals",
            	["zh-Hans"] = "包含相等值",
            	["zh-Hant"] = "包含相等值",
            	["hr"] = "Uključi jednako",
            	["cs"] = "Včetně rovnosti",
            	["da"] = "Medtag lig med",
            	["nl"] = "Gelijk aan opnemen",
            	["et"] = "Kaasa võrdsed",
            	["fil"] = "Isama ang Kapantay",
            	["fi"] = "Sisällytä yhtä suuret",
            	["fr"] = "Inclure l’égalité",
            	["gl"] = "Incluír iguais",
            	["ka"] = "ტოლი მნიშვნელობის ჩართვა",
            	["de"] = "Gleich einschließen",
            	["el"] = "Συμπερίληψη ίσων",
            	["gu"] = "સમાન મૂલ્ય સામેલ કરો",
            	["hi"] = "बराबर शामिल करें",
            	["hu"] = "Egyenlő értékek belefoglalása",
            	["is"] = "Taka með jafnt",
            	["id"] = "Sertakan yang Sama",
            	["it"] = "Includi uguale",
            	["ja"] = "等しい値を含める",
            	["kn"] = "ಸಮನನ್ನು ಒಳಗೊಂಡಿರಿ",
            	["kk"] = "Тең мәнді қосу",
            	["km"] = "រួមបញ្ចូលតម្លៃស្មើ",
            	["ko"] = "같은 값 포함",
            	["ky"] = "Теңдерди кошуу",
            	["lo"] = "ລວມຄ່າເທົ່າກັນ",
            	["lv"] = "Iekļaut vienādus",
            	["lt"] = "Įtraukti lygiąsias",
            	["mk"] = "Вклучи еднакви",
            	["ms"] = "Sertakan Sama",
            	["ml"] = "തുല്യമായവ ഉൾപ്പെടുത്തുക",
            	["mr"] = "समान समाविष्ट करा",
            	["mn"] = "Тэнцүүг оруулах",
            	["ne"] = "बराबर समावेश गर्ने",
            	["no"] = "Inkluder lik",
            	["pl"] = "Uwzględnij równe",
            	["pt"] = "Incluir iguais",
            	["pa"] = "ਬਰਾਬਰ ਸ਼ਾਮਲ ਕਰੋ",
            	["ro"] = "Include egal",
            	["rm"] = "Includer egual",
            	["ru"] = "Включать равные",
            	["sr"] = "Uključi jednake",
            	["si"] = "සමාන ඇතුළත් කරන්න",
            	["sk"] = "Zahrnúť rovné",
            	["sl"] = "Vključi enako",
            	["es"] = "Incluir iguales",
            	["sw"] = "Jumuisha Sawa",
            	["sv"] = "Inkludera lika med",
            	["ta"] = "சமமானவற்றைச் சேர்",
            	["te"] = "సమానమైనదాన్ని చేర్చు",
            	["th"] = "รวมค่าที่เท่ากัน",
            	["tr"] = "Eşit Olanları Dahil Et",
            	["uk"] = "Включати рівні",
            	["vi"] = "Bao gồm bằng",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local threshold = config and config.threshold or 0.0
        local include_equal = config and config.include_equal or false

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                local passes
                if include_equal then
                    passes = data_point.value >= threshold
                else
                    passes = data_point.value > threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
