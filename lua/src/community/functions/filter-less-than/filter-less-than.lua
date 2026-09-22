-- Lua Function to filter data points by value (less than threshold)
-- Only passes through data points with values less than a threshold

local tng_config = require("tng.config")
local number = tng_config.number
local checkbox = tng_config.checkbox

return {
    -- Configuration metadata
    id = "filter-less-than",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_filter"},
    title = {
    	["en"] = "Filter Less Than",
    	["af"] = "Filtreer Kleiner As",
    	["sq"] = "Filtro më të vogla se",
    	["am"] = "ከዚህ በታች አጣራ",
    	["hy"] = "Զտել փոքր արժեքները",
    	["az"] = "Kiçik olanları süzgəcdən keçir",
    	["bn"] = "এর চেয়ে কম ফিল্টার করুন",
    	["eu"] = "Iragazi hau baino txikiagoa",
    	["be"] = "Фільтраваць меншыя за",
    	["bg"] = "Филтриране под",
    	["my"] = "ထက်ငယ်သော တန်ဖိုးများကို စစ်ထုတ်ရန်",
    	["ca"] = "Filtra els valors inferiors a",
    	["zh-Hans"] = "筛选小于",
    	["zh-Hant"] = "篩選小於",
    	["hr"] = "Filtriraj manje od",
    	["cs"] = "Filtrovat menší než",
    	["da"] = "Filtrér mindre end",
    	["nl"] = "Kleiner dan filteren",
    	["et"] = "Filtreeri väiksemad kui",
    	["fil"] = "Salain ang Mas Mababa sa",
    	["fi"] = "Suodata pienemmät kuin",
    	["fr"] = "Filtrer inférieur à",
    	["gl"] = "Filtrar menores que",
    	["ka"] = "ნაკლებზე მცირე მნიშვნელობების ფილტრაცია",
    	["de"] = "Kleiner als filtern",
    	["el"] = "Φιλτράρισμα μικρότερων τιμών",
    	["gu"] = "કરતાં ઓછું ફિલ્ટર કરો",
    	["hi"] = "इससे कम फ़िल्टर करें",
    	["hu"] = "Szűrés kisebb értékre",
    	["is"] = "Sía minna en",
    	["id"] = "Saring Lebih Kecil dari",
    	["it"] = "Filtra minore di",
    	["ja"] = "指定値より小さい値をフィルタ",
    	["kn"] = "ಇದಕ್ಕಿಂತ ಕಡಿಮೆಯನ್ನು ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Кіші мәндерді сүзу",
    	["km"] = "ត្រងតូចជាង",
    	["ko"] = "미만 값 필터링",
    	["ky"] = "Кичине маанилерди чыпкалоо",
    	["lo"] = "ກັ່ນຕອງໜ້ອຍກວ່າ",
    	["lv"] = "Filtrēt mazākus par",
    	["lt"] = "Filtruoti mažesnes už",
    	["mk"] = "Филтрирај помало од",
    	["ms"] = "Tapis Kurang Daripada",
    	["ml"] = "ഇതിലും കുറവുള്ളവ ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "यापेक्षा कमी फिल्टर करा",
    	["mn"] = "Бага утгаар шүүх",
    	["ne"] = "भन्दा सानो फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer mindre enn",
    	["pl"] = "Filtruj mniejsze niż",
    	["pt"] = "Filtrar menor que",
    	["pa"] = "ਤੋਂ ਘੱਟ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează mai mic decât",
    	["rm"] = "Filtrar pli pitschen che",
    	["ru"] = "Фильтр: меньше",
    	["sr"] = "Filtriraj manje od",
    	["si"] = "වඩා අඩු දේ පෙරහන් කරන්න",
    	["sk"] = "Filtrovať menšie než",
    	["sl"] = "Filtriraj manjše od",
    	["es"] = "Filtrar menores que",
    	["sw"] = "Chuja Ndogo Kuliko",
    	["sv"] = "Filtrera mindre än",
    	["ta"] = "விட குறைவானவற்றை வடிகட்டு",
    	["te"] = "కంటే తక్కువను ఫిల్టర్ చేయి",
    	["th"] = "กรองค่าน้อยกว่า",
    	["tr"] = "Küçüktür Filtresi",
    	["uk"] = "Фільтрувати менше ніж",
    	["vi"] = "Lọc nhỏ hơn",
    },
    description = {
    	["en"] = [[
Filters data points by value. Only data points with values less than the threshold will pass through.

Configuration:
- **Threshold**: The maximum value (exclusive by default)
- **Include Equal**: Also include values equal to the threshold (default: false)
    	]],
    	["af"] = [[
Filtreer datapunte volgens waarde. Slegs datapunte met waardes kleiner as die drempel word deurgegee.

Konfigurasie:
- **Drempel**: Die maksimumwaarde (standaard eksklusief)
- **Sluit Gelykes In**: Sluit ook waardes in wat gelyk aan die drempel is (verstek: vals)
    	]],
    	["sq"] = [[
Filtron pikat e të dhënave sipas vlerës. Vetëm pikat e të dhënave me vlera më të vogla se pragu do të kalojnë.

Konfigurimi:
- **Pragu**: Vlera maksimale (përjashtuese si parazgjedhje)
- **Përfshi të barabartat**: Përfshin edhe vlerat të barabarta me pragun (parazgjedhja: false)
    	]],
    	["am"] = [[
የውሂብ ነጥቦችን በእሴታቸው ያጣራል። ከገደቡ ያነሱ እሴቶች ያላቸው የውሂብ ነጥቦች ብቻ ያልፋሉ።

ውቅር፦
- **ገደብ**፦ ከፍተኛው እሴት (በነባሪ እኩል እሴትን አያካትትም)
- **እኩል አካትት**፦ ከገደቡ ጋር እኩል የሆኑ እሴቶችንም ያካትታል (ነባሪ፦ false)
    	]],
    	["hy"] = [[
Զտում է տվյալակետերն ըստ արժեքի։ Անցնում են միայն շեմից փոքր արժեք ունեցող տվյալակետերը։

Կազմաձևում՝
- **Շեմ**․ առավելագույն արժեքը (կանխադրված՝ բացառող)
- **Հավասարը ներառել**․ ներառել նաև շեմին հավասար արժեքները (կանխադրված՝ false)
    	]],
    	["az"] = [[
Məlumat nöqtələrini qiymətə görə süzgəcdən keçirir. Yalnız qiyməti həddən kiçik olan məlumat nöqtələri keçir.

Konfiqurasiya:
- **Hədd**: Maksimum qiymət (standart olaraq daxil edilmir)
- **Bərabər olanı daxil et**: Həddə bərabər qiymətləri də daxil et (standart: false)
    	]],
    	["bn"] = [[
মান অনুযায়ী ডেটা পয়েন্ট ফিল্টার করে। কেবল সীমার চেয়ে কম মানের ডেটা পয়েন্টগুলোই পাস করবে।

কনফিগারেশন:
- **সীমা**: সর্বোচ্চ মান (ডিফল্টভাবে সমান মান অন্তর্ভুক্ত নয়)
- **সমান অন্তর্ভুক্ত করুন**: সীমার সমান মানও অন্তর্ভুক্ত করুন (ডিফল্ট: false)
    	]],
    	["eu"] = [[
Datu-puntuak balioaren arabera iragazten ditu. Atalasea baino balio txikiagoa duten datu-puntuak soilik igaroko dira.

Konfigurazioa:
- **Atalasea**: Gehieneko balioa (lehenespenez, barne hartu gabe)
- **Berdina barne hartu**: Atalasearen berdinak diren balioak ere sartzea (lehenetsia: false)
    	]],
    	["be"] = [[
Фільтруе кропкі даных па значэнні. Прапускаюцца толькі кропкі даных са значэннямі, меншымі за парог.

Канфігурацыя:
- **Парог**: максімальнае значэнне (па змаўчанні не ўключае роўныя)
- **Уключаць роўныя**: таксама ўключаць значэнні, роўныя парогу (па змаўчанні: false)
    	]],
    	["bg"] = [[
Филтрира точките от данни по стойност. Преминават само точките от данни със стойности под прага.

Конфигурация:
- **Праг**: Максималната стойност (по подразбиране изключва равните)
- **Включване на равните**: Включва и стойностите, равни на прага (по подразбиране: false)
    	]],
    	["my"] = [[
ဒေတာအမှတ်များကို တန်ဖိုးအလိုက် စစ်ထုတ်သည်။ သတ်မှတ်ထားသော ကန့်သတ်တန်ဖိုးထက် နည်းသော တန်ဖိုးရှိသည့် ဒေတာအမှတ်များသာ ဖြတ်သန်းမည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **ကန့်သတ်တန်ဖိုး**: အများဆုံးတန်ဖိုး (မူလအားဖြင့် တူညီသောတန်ဖိုး မပါဝင်)
- **တူညီသောတန်ဖိုး ပါဝင်ရန်**: ကန့်သတ်တန်ဖိုးနှင့် တူညီသော တန်ဖိုးများကိုလည်း ထည့်သွင်းရန် (မူလ: false)
    	]],
    	["ca"] = [[
Filtra els punts de dades pel valor. Només passen els punts de dades amb valors inferiors al llindar.

Configuració:
- **Llindar**: El valor màxim (exclusiu per defecte)
- **Inclou els iguals**: Inclou també els valors iguals al llindar (per defecte: false)
    	]],
    	["zh-Hans"] = [[
根据值筛选数据点。只有值小于阈值的数据点才会通过。

配置：
- **阈值**：最大值（默认不包含等于阈值的值）
- **包含相等值**：同时包含等于阈值的值（默认：false）
    	]],
    	["zh-Hant"] = [[
依值篩選資料點。只有值小於閾值的資料點才會通過。

設定：
- **閾值**：最大值（預設為不包含等於）
- **包含相等值**：也包含等於閾值的值（預設：false）
    	]],
    	["hr"] = [[
Filtrira podatkovne točke prema vrijednosti. Prolaze samo podatkovne točke čije su vrijednosti manje od praga.

Konfiguracija:
- **Prag**: Najveća vrijednost (zadano isključivo)
- **Uključi jednako**: Uključi i vrijednosti jednake pragu (zadano: false)
    	]],
    	["cs"] = [[
Filtruje datové body podle hodnoty. Projdou pouze datové body s hodnotami nižšími než práh.

Konfigurace:
- **Práh**: Maximální hodnota (ve výchozím nastavení bez rovnosti)
- **Včetně rovnosti**: Zahrnout také hodnoty rovné prahu (výchozí: false)
    	]],
    	["da"] = [[
Filtrerer datapunkter efter værdi. Kun datapunkter med værdier under grænsen går videre.

Konfiguration:
- **Grænse**: Maksimumsværdien (eksklusiv som standard)
- **Medtag lig med**: Medtag også værdier, der er lig med grænsen (standard: false)
    	]],
    	["nl"] = [[
Filtert datapunten op waarde. Alleen datapunten met waarden kleiner dan de drempel worden doorgelaten.

Configuratie:
- **Drempel**: De maximale waarde (standaard exclusief)
- **Gelijk aan opnemen**: Ook waarden opnemen die gelijk zijn aan de drempel (standaard: false)
    	]],
    	["et"] = [[
Filtreerib andmepunkte väärtuse alusel. Läbivad ainult lävest väiksema väärtusega andmepunktid.

Seadistus:
- **Lävi**: maksimaalne väärtus (vaikimisi välistav)
- **Kaasa võrdsed**: kaasa ka lävega võrdsed väärtused (vaikimisi: väär)
    	]],
    	["fil"] = [[
Sinasala ang mga data point ayon sa value. Tanging mga data point na may halagang mas mababa sa threshold ang magpapatuloy.

Configuration:
- **Threshold**: Pinakamataas na halaga (hindi kasama bilang default)
- **Include Equal**: Isama rin ang mga halagang katumbas ng threshold (default: false)
    	]],
    	["fi"] = [[
Suodattaa datapisteet arvon perusteella. Vain kynnysarvoa pienemmät arvot päästetään läpi.

Määritys:
- **Kynnysarvo**: Enimmäisarvo (oletuksena rajaa ei sisällytetä)
- **Sisällytä yhtä suuret**: Sisällytä myös kynnysarvon kanssa yhtä suuret arvot (oletus: false)
    	]],
    	["fr"] = [[
Filtre les points de données selon leur valeur. Seuls les points dont la valeur est inférieure au seuil sont conservés.

Configuration :
- **Seuil** : Valeur maximale (exclue par défaut)
- **Inclure l’égalité** : Inclure également les valeurs égales au seuil (par défaut : false)
    	]],
    	["gl"] = [[
Filtra os puntos de datos polo valor. Só pasan os puntos de datos con valores inferiores ao limiar.

Configuración:
- **Limiar**: O valor máximo (exclusivo de forma predeterminada)
- **Incluír iguais**: Incluír tamén os valores iguais ao limiar (predeterminado: false)
    	]],
    	["ka"] = [[
ფილტრავს მონაცემთა წერტილებს მნიშვნელობის მიხედვით. გაივლის მხოლოდ ზღვარზე ნაკლები მნიშვნელობის მქონე მონაცემთა წერტილები.

კონფიგურაცია:
- **ზღვარი**: მაქსიმალური მნიშვნელობა (ნაგულისხმევად, ზღვარი არ შედის)
- **ტოლი მნიშვნელობის ჩართვა**: ასევე ჩართოს ზღვრის ტოლი მნიშვნელობები (ნაგულისხმევი: false)
    	]],
    	["de"] = [[
Filtert Datenpunkte nach ihrem Wert. Nur Datenpunkte mit Werten unter dem Schwellenwert werden weitergegeben.

Konfiguration:
- **Schwellenwert**: Der Höchstwert (standardmäßig exklusiv)
- **Gleich einschließen**: Auch Werte einschließen, die dem Schwellenwert entsprechen (Standard: false)
    	]],
    	["el"] = [[
Φιλτράρει τα σημεία δεδομένων με βάση την τιμή τους. Μόνο τα σημεία δεδομένων με τιμές μικρότερες από το όριο θα περνούν.

Διαμόρφωση:
- **Όριο**: Η μέγιστη τιμή (εξαιρείται από προεπιλογή)
- **Συμπερίληψη ίσων**: Συμπεριλαμβάνει επίσης τιμές ίσες με το όριο (προεπιλογή: false)
    	]],
    	["gu"] = [[
ડેટા પોઇન્ટ્સને મૂલ્ય દ્વારા ફિલ્ટર કરે છે. મર્યાદા કરતાં ઓછું મૂલ્ય ધરાવતા ડેટા પોઇન્ટ્સ જ પસાર થશે.

ગોઠવણી:
- **મર્યાદા**: મહત્તમ મૂલ્ય (ડિફૉલ્ટ રીતે સમાન મૂલ્ય સામેલ નથી)
- **સમાન મૂલ્ય સામેલ કરો**: મર્યાદા સમાન મૂલ્યો પણ સામેલ કરો (ડિફૉલ્ટ: false)
    	]],
    	["hi"] = [[
डेटा पॉइंट को मान के आधार पर फ़िल्टर करता है। केवल थ्रेशोल्ड से कम मान वाले डेटा पॉइंट आगे जाते हैं।

कॉन्फ़िगरेशन:
- **थ्रेशोल्ड**: अधिकतम मान (डिफ़ॉल्ट रूप से अनन्य)
- **बराबर शामिल करें**: थ्रेशोल्ड के बराबर मान भी शामिल करें (डिफ़ॉल्ट: false)
    	]],
    	["hu"] = [[
Az adatpontokat érték alapján szűri. Csak a küszöbnél kisebb értékű adatpontok haladnak tovább.

Konfiguráció:
- **Küszöbérték**: A maximális érték (alapértelmezés szerint kizáró)
- **Egyenlő értékek belefoglalása**: A küszöbértékkel egyenlő értékeket is belefoglalja (alapértelmezett: false)
    	]],
    	["is"] = [[
Síar gagnapunkta eftir gildi. Aðeins gagnapunktar með gildi undir mörkunum fara áfram.

Stillingar:
- **Mörk**: Hámarksgildi (útilokað sjálfgefið)
- **Taka með jafnt**: Taka einnig með gildi sem eru jöfn mörkunum (sjálfgefið: false)
    	]],
    	["id"] = [[
Menyaring titik data berdasarkan nilai. Hanya titik data dengan nilai kurang dari ambang yang akan diteruskan.

Konfigurasi:
- **Ambang**: Nilai maksimum (secara default tidak termasuk nilai yang sama)
- **Sertakan yang Sama**: Sertakan juga nilai yang sama dengan ambang (default: false)
    	]],
    	["it"] = [[
Filtra i punti dati in base al valore. Passano solo i punti dati con valori inferiori alla soglia.

Configurazione:
- **Soglia**: il valore massimo (esclusivo per impostazione predefinita)
- **Includi uguale**: include anche i valori uguali alla soglia (predefinito: false)
    	]],
    	["ja"] = [[
値に基づいてデータポイントを絞り込みます。しきい値より小さい値を持つデータポイントだけが通過します。

設定:
- **しきい値**: 最大値（デフォルトでは除外）
- **等しい値を含める**: しきい値と等しい値も含める（デフォルト: false）
    	]],
    	["kn"] = [[
ಮೌಲ್ಯದ ಆಧಾರದ ಮೇಲೆ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ. ಮಿತಿಗಿಂತ ಕಡಿಮೆ ಮೌಲ್ಯವಿರುವ ಡೇಟಾ ಬಿಂದುಗಳು ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತವೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಮಿತಿ**: ಗರಿಷ್ಠ ಮೌಲ್ಯ (ಡೀಫಾಲ್ಟ್ ಆಗಿ ಒಳಗೊಂಡಿಲ್ಲ)
- **ಸಮನನ್ನು ಒಳಗೊಂಡಿರಿ**: ಮಿತಿಗೆ ಸಮನಾದ ಮೌಲ್ಯಗಳನ್ನೂ ಒಳಗೊಂಡಿರಿ (ಡೀಫಾಲ್ಟ್: false)
    	]],
    	["kk"] = [[
Дерек нүктелерін мәні бойынша сүзеді. Шектен төмен мәні бар дерек нүктелері ғана өтеді.

Конфигурация:
- **Шек**: Ең жоғары мән (әдепкі бойынша қосылмайды)
- **Тең мәнді қосу**: Шекке тең мәндерді де қосу (әдепкі: false)
    	]],
    	["km"] = [[
ត្រងចំណុចទិន្នន័យតាមតម្លៃ។ មានតែចំណុចទិន្នន័យដែលមានតម្លៃតិចជាងកម្រិតកំណត់ប៉ុណ្ណោះដែលនឹងឆ្លងកាត់។

ការកំណត់រចនា៖
- **កម្រិតកំណត់**៖ តម្លៃអតិបរមា (តាមលំនាំដើម មិនរាប់បញ្ចូលតម្លៃស្មើ)
- **រាប់បញ្ចូលតម្លៃស្មើ**៖ រាប់បញ្ចូលតម្លៃដែលស្មើនឹងកម្រិតកំណត់ផងដែរ (លំនាំដើម៖ false)
    	]],
    	["ko"] = [[
값으로 데이터 포인트를 필터링합니다. 임계값보다 작은 값을 가진 데이터 포인트만 통과합니다.

구성:
- **임계값**: 최대값(기본적으로 미만)
- **같은 값 포함**: 임계값과 같은 값도 포함합니다(기본값: false)
    	]],
    	["ky"] = [[
Маалымат чекиттерин мааниси боюнча чыпкалайт. Чектен кичине мааниге ээ болгон маалымат чекиттери гана өткөрүлөт.

Тууралоо:
- **Чек**: Максималдуу маани (демейкиде кошулбайт)
- **Теңдерди кошуу**: Чекке тең маанилерди да кошуу (демейки: false)
    	]],
    	["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນຕາມຄ່າ. ຈະຜ່ານສະເພາະຈຸດຂໍ້ມູນທີ່ມີຄ່ານ້ອຍກວ່າຄ່າກຳນົດເທົ່ານັ້ນ.

ການກຳນົດຄ່າ:
- **ຄ່າກຳນົດ**: ຄ່າສູງສຸດ (ບໍ່ລວມຄ່ານີ້ໂດຍຄ່າເລີ່ມຕົ້ນ)
- **ລວມຄ່າເທົ່າກັນ**: ລວມຄ່າທີ່ເທົ່າກັບຄ່າກຳນົດ (ຄ່າເລີ່ມຕົ້ນ: false)
    	]],
    	["lv"] = [[
Filtrē datu punktus pēc vērtības. Tiks nodoti tikai datu punkti, kuru vērtības ir mazākas par slieksni.

Konfigurācija:
- **Slieksnis**: Maksimālā vērtība (pēc noklusējuma neieskaitot slieksni)
- **Iekļaut vienādas vērtības**: Iekļaut arī vērtības, kas vienādas ar slieksni (pēc noklusējuma: false)
    	]],
    	["lt"] = [[
Filtruoja duomenų taškus pagal reikšmę. Toliau perduodami tik duomenų taškai, kurių reikšmės mažesnės už ribą.

Konfigūracija:
- **Riba**: Didžiausia reikšmė (pagal numatytuosius nustatymus neįtraukiama)
- **Įtraukti lygiąsias**: Taip pat įtraukti ribai lygias reikšmes (numatyta: false)
    	]],
    	["mk"] = [[
Ги филтрира точките на податоци според вредноста. Само точките на податоци со вредности помали од прагот ќе поминат.

Конфигурација:
- **Праг**: Максималната вредност (стандардно исклучителна)
- **Вклучи еднакви**: Вклучи ги и вредностите еднакви на прагот (стандардно: false)
    	]],
    	["ms"] = [[
Menapis titik data mengikut nilai. Hanya titik data dengan nilai yang kurang daripada ambang akan diteruskan.

Konfigurasi:
- **Ambang**: Nilai maksimum (eksklusif secara lalai)
- **Sertakan Sama**: Sertakan juga nilai yang sama dengan ambang (lalai: false)
    	]],
    	["ml"] = [[
മൂല്യം പ്രകാരം ഡാറ്റാ പോയിന്റുകൾ ഫിൽട്ടർ ചെയ്യുന്നു. പരിധിയേക്കാൾ കുറഞ്ഞ മൂല്യമുള്ള ഡാറ്റാ പോയിന്റുകൾ മാത്രമേ കടന്നുപോകൂ.

കോൺഫിഗറേഷൻ:
- **Threshold**: പരമാവധി മൂല്യം (സ്ഥിരസ്ഥിതിയിൽ സമതുല്യം ഉൾപ്പെടില്ല)
- **Include Equal**: പരിധിക്ക് തുല്യമായ മൂല്യങ്ങളും ഉൾപ്പെടുത്തുക (സ്ഥിരസ്ഥിതി: false)
    	]],
    	["mr"] = [[
डेटा पॉइंट्सना मूल्यांनुसार फिल्टर करते. थ्रेशोल्डपेक्षा कमी मूल्य असलेले डेटा पॉइंट्सच पुढे पाठवले जातील.

कॉन्फिगरेशन:
- **थ्रेशोल्ड**: कमाल मूल्य (डीफॉल्टनुसार वगळून)
- **समान मूल्य समाविष्ट करा**: थ्रेशोल्डइतकी मूल्येही समाविष्ट करा (डीफॉल्ट: false)
    	]],
    	["mn"] = [[
Өгөгдлийн цэгүүдийг утгаар нь шүүнэ. Босгоноос бага утгатай өгөгдлийн цэгүүд л нэвтэрнэ.

Тохиргоо:
- **Босго**: Хамгийн их утга (анхдагчаар тэнцүү утгыг оруулахгүй)
- **Тэнцүүг оруулах**: Босготой тэнцүү утгуудыг мөн оруулах (анхдагч: false)
    	]],
    	["ne"] = [[
डेटा बिन्दुहरूलाई मानअनुसार फिल्टर गर्छ। सीमाभन्दा सानो मान भएका डेटा बिन्दुहरू मात्र अघि पठाइन्छन्।

कन्फिगरेसन:
- **सीमा**: अधिकतम मान (पूर्वनिर्धारित रूपमा समावेश हुँदैन)
- **बराबर समावेश गर्ने**: सीमासँग बराबर मानहरू पनि समावेश गर्ने (पूर्वनिर्धारित: false)
    	]],
    	["no"] = [[
Filtrerer datapunkter etter verdi. Bare datapunkter med verdier mindre enn terskelen, slipper gjennom.

Konfigurasjon:
- **Terskel**: Maksimumsverdien (eksklusiv som standard)
- **Inkluder lik**: Inkluder også verdier som er lik terskelen (standard: false)
    	]],
    	["pl"] = [[
Filtruje punkty danych według wartości. Przechodzą tylko punkty danych o wartościach mniejszych od progu.

Konfiguracja:
- **Próg**: Maksymalna wartość (domyślnie wykluczająca równą wartość)
- **Uwzględnij równe**: Uwzględniaj także wartości równe progowi (domyślnie: false)
    	]],
    	["pt"] = [[
Filtra os pontos de dados pelo valor. Apenas os pontos de dados com valores inferiores ao limite passam.

Configuração:
- **Limite**: O valor máximo (exclusivo por predefinição)
- **Incluir iguais**: Incluir também valores iguais ao limite (predefinição: false)
    	]],
    	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਮੁੱਲ ਦੇ ਆਧਾਰ 'ਤੇ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਸਿਰਫ਼ ਉਹ ਡਾਟਾ ਪੁਆਇੰਟ ਅੱਗੇ ਭੇਜੇ ਜਾਣਗੇ ਜਿਨ੍ਹਾਂ ਦੇ ਮੁੱਲ ਥ੍ਰੈਸ਼ਹੋਲਡ ਤੋਂ ਘੱਟ ਹਨ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਥ੍ਰੈਸ਼ਹੋਲਡ**: ਅਧਿਕਤਮ ਮੁੱਲ (ਮੂਲ ਰੂਪ ਵਿੱਚ ਵਿਸ਼ੇਸ਼)
- **ਬਰਾਬਰ ਸ਼ਾਮਲ ਕਰੋ**: ਥ੍ਰੈਸ਼ਹੋਲਡ ਦੇ ਬਰਾਬਰ ਮੁੱਲ ਵੀ ਸ਼ਾਮਲ ਕਰੋ (ਮੂਲ: false)
    	]],
    	["ro"] = [[
Filtrează punctele de date după valoare. Trec mai departe doar punctele de date cu valori mai mici decât pragul.

Configurare:
- **Prag**: Valoarea maximă (exclusiv în mod implicit)
- **Include egal**: Include și valorile egale cu pragul (implicit: false)
    	]],
    	["rm"] = [[
Filtra puncts da datas tenor a lur valur. Mo che puncts da datas cun valurs pli pitschnas che la limita passan vinavant.

Configuraziun:
- **Limita**: La valur maximala (exclusiva per standard)
- **Includer egual**: Includer era valurs egualas a la limita (standard: false)
    	]],
    	["ru"] = [[
Фильтрует точки данных по значению. Пропускаются только точки данных со значениями ниже порога.

Конфигурация:
- **Порог**: Максимальное значение (по умолчанию исключительно)
- **Включать равные**: Также включать значения, равные порогу (по умолчанию: false)
    	]],
    	["sr"] = [[
Filtrira tačke podataka prema vrednosti. Prolaze samo tačke podataka čije su vrednosti manje od praga.

Konfiguracija:
- **Prag**: Maksimalna vrednost (podrazumevano isključiva)
- **Uključi jednake**: Uključuje i vrednosti jednake pragu (podrazumevano: false)
    	]],
    	["si"] = [[
අගය අනුව දත්ත ලක්ෂ්‍ය පෙරහන් කරයි. සීමාවට වඩා අඩු අගයන් සහිත දත්ත ලක්ෂ්‍ය පමණක් ඉදිරියට යයි.

වින්‍යාසය:
- **සීමාව**: උපරිම අගය (පෙරනිමියෙන් සමාන අගය ඇතුළත් නොවේ)
- **සමාන අගයන් ඇතුළත් කරන්න**: සීමාවට සමාන අගයන් ද ඇතුළත් කරන්න (පෙරනිමිය: false)
    	]],
    	["sk"] = [[
Filtruje údajové body podľa hodnoty. Prejdú iba údajové body s hodnotami nižšími než prah.

Konfigurácia:
- **Prah**: Maximálna hodnota (predvolene bez rovnosti)
- **Zahrnúť rovné**: Zahrnúť aj hodnoty rovné prahu (predvolené: false)
    	]],
    	["sl"] = [[
Filtrira podatkovne točke glede na vrednost. Prepustijo se le podatkovne točke z vrednostmi, manjšimi od praga.

Konfiguracija:
- **Prag**: Največja vrednost (privzeto izključna)
- **Vključi enako**: Vključi tudi vrednosti, enake pragu (privzeto: false)
    	]],
    	["es"] = [[
Filtra los puntos de datos por valor. Solo pasan los puntos de datos cuyos valores son menores que el umbral.

Configuración:
- **Umbral**: El valor máximo (exclusivo de forma predeterminada)
- **Incluir iguales**: Incluir también los valores iguales al umbral (predeterminado: false)
    	]],
    	["sw"] = [[
Huchuja nukta za data kulingana na thamani. Ni nukta za data zenye thamani ndogo kuliko kikomo pekee ndizo zitapitishwa.

Usanidi:
- **Kikomo**: Thamani ya juu kabisa (haitajumuisha kikomo kwa chaguo-msingi)
- **Jumuisha Sawa**: Pia jumuisha thamani zilizo sawa na kikomo (chaguo-msingi: false)
    	]],
    	["sv"] = [[
Filtrerar datapunkter efter värde. Endast datapunkter med värden mindre än tröskeln släpps igenom.

Konfiguration:
- **Tröskel**: Det största värdet (exklusivt som standard)
- **Inkludera lika med**: Inkludera även värden som är lika med tröskeln (standard: false)
    	]],
    	["ta"] = [[
மதிப்பின் அடிப்படையில் தரவுப் புள்ளிகளை வடிகட்டுகிறது. வரம்பைவிட குறைவான மதிப்புகளைக் கொண்ட புள்ளிகள் மட்டுமே தொடரும்.

உள்ளமைவு:
- **வரம்பு**: அதிகபட்ச மதிப்பு (இயல்புநிலையில் வரம்பு சேர்க்கப்படாது)
- **சமமானவற்றைச் சேர்**: வரம்பிற்குச் சமமான மதிப்புகளையும் சேர்க்கவும் (இயல்புநிலை: false)
    	]],
    	["te"] = [[
విలువ ఆధారంగా డేటా పాయింట్లను ఫిల్టర్ చేస్తుంది. పరిమితి కంటే తక్కువ విలువ ఉన్న డేటా పాయింట్లు మాత్రమే కొనసాగుతాయి.

కాన్ఫిగరేషన్:
- **పరిమితి**: గరిష్ఠ విలువ (డిఫాల్ట్‌గా సమాన విలువను మినహాయిస్తుంది)
- **సమానమైనదాన్ని చేర్చు**: పరిమితికి సమానమైన విలువలను కూడా చేర్చండి (డిఫాల్ట్: false)
    	]],
    	["th"] = [[
กรองจุดข้อมูลตามค่า เฉพาะจุดข้อมูลที่มีค่าน้อยกว่าเกณฑ์เท่านั้นที่จะถูกส่งต่อ

การกำหนดค่า:
- **เกณฑ์**: ค่าสูงสุด (โดยค่าเริ่มต้นไม่รวมค่าที่เท่ากัน)
- **รวมค่าที่เท่ากัน**: รวมค่าที่เท่ากับเกณฑ์ด้วย (ค่าเริ่มต้น: false)
    	]],
    	["tr"] = [[
Veri noktalarını değerlerine göre filtreler. Yalnızca eşikten küçük değerlere sahip veri noktaları geçer.

Yapılandırma:
- **Eşik**: Maksimum değer (varsayılan olarak hariç)
- **Eşit Olanları Dahil Et**: Eşiğe eşit değerleri de dahil et (varsayılan: false)
    	]],
    	["uk"] = [[
Фільтрує точки даних за значенням. Пропускаються лише точки даних зі значеннями, меншими за поріг.

Конфігурація:
- **Поріг**: Максимальне значення (типово без включення порогу)
- **Включати рівні**: Також включати значення, рівні порогу (типово: false)
    	]],
    	["vi"] = [[
Lọc các điểm dữ liệu theo giá trị. Chỉ những điểm dữ liệu có giá trị nhỏ hơn ngưỡng mới được giữ lại.

Cấu hình:
- **Ngưỡng**: Giá trị tối đa (mặc định không bao gồm giá trị bằng ngưỡng)
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
                    passes = data_point.value <= threshold
                else
                    passes = data_point.value < threshold
                end

                if passes then
                    return data_point
                end
            end
        end
    end,
}
