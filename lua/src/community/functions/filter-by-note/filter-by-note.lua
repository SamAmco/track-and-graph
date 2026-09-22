-- Lua Function to filter data points by note
-- This function filters data points by note

local tng_config = require("tng.config")
local text = tng_config.text
local checkbox = tng_config.checkbox

local function match(data_point, filter_note, case_sensitive, match_exactly)
    if filter_note == nil then
        return true
    end

    local data_note = data_point.note
    if not data_note then return false end

    -- Apply case sensitivity
    if not case_sensitive then
        data_note = string.lower(data_note)
        filter_note = string.lower(filter_note)
    end

    -- Apply matching mode
    if match_exactly then
        return data_note == filter_note
    else
        return string.find(data_note, filter_note, 1, true) ~= nil
    end
end


return {
    -- Configuration metadata
    id = "filter-by-note",
    version = "1.0.2",
    inputCount = 1,
    categories = {"_filter"},
    title = {
    	["en"] = "Filter by Note",
    	["af"] = "Filtreer volgens Nota",
    	["sq"] = "Filtro sipas shënimit",
    	["am"] = "በማስታወሻ አጣራ",
    	["hy"] = "Զտել ըստ նշման",
    	["az"] = "Qeydə görə süzgəcdən keçir",
    	["bn"] = "নোট দিয়ে ফিল্টার করুন",
    	["eu"] = "Iragazi oharraren arabera",
    	["be"] = "Фільтраваць па заўвазе",
    	["bg"] = "Филтриране по бележка",
    	["my"] = "မှတ်စုဖြင့် စစ်ထုတ်ရန်",
    	["ca"] = "Filtra per nota",
    	["zh-Hans"] = "按备注筛选",
    	["zh-Hant"] = "依備註篩選",
    	["hr"] = "Filtriraj prema bilješci",
    	["cs"] = "Filtrovat podle poznámky",
    	["da"] = "Filtrér efter note",
    	["nl"] = "Op notitie filteren",
    	["et"] = "Filtreeri märkuse järgi",
    	["fil"] = "Salain ayon sa Note",
    	["fi"] = "Suodata muistiinpanon perusteella",
    	["fr"] = "Filtrer par note",
    	["gl"] = "Filtrar por nota",
    	["ka"] = "შენიშვნით ფილტრაცია",
    	["de"] = "Nach Notiz filtern",
    	["el"] = "Φιλτράρισμα κατά σημείωση",
    	["gu"] = "નોંધ દ્વારા ફિલ્ટર કરો",
    	["hi"] = "नोट के अनुसार फ़िल्टर करें",
    	["hu"] = "Szűrés megjegyzés alapján",
    	["is"] = "Sía eftir athugasemd",
    	["id"] = "Saring berdasarkan Catatan",
    	["it"] = "Filtra per nota",
    	["ja"] = "メモでフィルタ",
    	["kn"] = "ಟಿಪ್ಪಣಿ ಮೂಲಕ ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Ескертпе бойынша сүзу",
    	["km"] = "ត្រងតាមចំណាំ",
    	["ko"] = "메모로 필터링",
    	["ky"] = "Эскертме боюнча чыпкалоо",
    	["lo"] = "ກັ່ນຕອງຕາມໝາຍເຫດ",
    	["lv"] = "Filtrēt pēc piezīmes",
    	["lt"] = "Filtruoti pagal pastabą",
    	["mk"] = "Филтрирај според белешка",
    	["ms"] = "Tapis mengikut Nota",
    	["ml"] = "കുറിപ്പ് പ്രകാരം ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "नोटनुसार फिल्टर करा",
    	["mn"] = "Тэмдэглэлээр шүүх",
    	["ne"] = "नोटअनुसार फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer etter notat",
    	["pl"] = "Filtruj według notatki",
    	["pt"] = "Filtrar por nota",
    	["pa"] = "ਨੋਟ ਅਨੁਸਾਰ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează după notă",
    	["rm"] = "Filtrar tenor nota",
    	["ru"] = "Фильтр по заметке",
    	["sr"] = "Filtriraj prema belešci",
    	["si"] = "සටහන අනුව පෙරහන් කරන්න",
    	["sk"] = "Filtrovať podľa poznámky",
    	["sl"] = "Filtriraj po opombi",
    	["es"] = "Filtrar por nota",
    	["sw"] = "Chuja kwa Dokezo",
    	["sv"] = "Filtrera efter anteckning",
    	["ta"] = "குறிப்பால் வடிகட்டு",
    	["te"] = "నోట్ ద్వారా ఫిల్టర్ చేయి",
    	["th"] = "กรองตามบันทึก",
    	["tr"] = "Nota Göre Filtrele",
    	["uk"] = "Фільтрувати за приміткою",
    	["vi"] = "Lọc theo ghi chú",
    },
    description = {
    	["en"] = [[
Filters data points by their note field. Only data points matching the filter criteria will pass through.

Configuration:
- **Filter Note**: The text to search for in notes
- **Case Sensitive**: Match case exactly (default: false)
- **Match Exactly**: Require exact match instead of substring (default: false)
- **Invert**: Keep data points that DON'T match instead (default: false)
    	]],
    	["af"] = [[
Filtreer datapunte volgens hul notaveld. Slegs datapunte wat aan die filterkriteria voldoen, word deurgegee.

Konfigurasie:
- **Filternota**: Die teks waarna in notas gesoek word
- **Hooflettergevoelig**: Pas hoofletters presies aan (verstek: vals)
- **Presiese passing**: Vereis ’n presiese passing in plaas van ’n substring (verstek: vals)
- **Inverteer**: Behou eerder datapunte wat NIE pas nie (verstek: vals)
    	]],
    	["sq"] = [[
Filtron pikat e të dhënave sipas fushës së shënimit. Vetëm pikat e të dhënave që përputhen me kriteret e filtrit do të kalojnë.

Konfigurimi:
- **Shënimi i filtrit**: Teksti që kërkohet në shënime
- **Ndjeshmëria ndaj shkronjave**: Përputhje e saktë e shkronjave të mëdha dhe të vogla (parazgjedhja: false)
- **Përputhje e saktë**: Kërkon përputhje të plotë në vend të nënvargut (parazgjedhja: false)
- **Përmbyse**: Mbaj në vend të tyre pikat e të dhënave që NUK përputhen (parazgjedhja: false)
    	]],
    	["am"] = [[
የውሂብ ነጥቦችን በማስታወሻ መስካቸው ያጣራል። የማጣሪያ መስፈርቱን የሚያሟሉ የውሂብ ነጥቦች ብቻ ያልፋሉ።

ውቅር፦
- **የማጣሪያ ማስታወሻ**፦ በማስታወሻዎች ውስጥ የሚፈለገው ጽሑፍ
- **ማክበር የሚገባውን ፊደል መለየት**፦ የፊደል አጠቃቀሙን በትክክል ማዛመድ (ነባሪ፦ false)
- **በትክክል ማዛመድ**፦ ከከፊል ማዛመድ ይልቅ ትክክለኛ ማዛመድን መጠየቅ (ነባሪ፦ false)
- **መቀልበስ**፦ የማይዛመዱ የውሂብ ነጥቦችን መያዝ (ነባሪ፦ false)
    	]],
    	["hy"] = [[
Զտում է տվյալակետերն ըստ դրանց նշման դաշտի։ Անցնում են միայն զտման չափանիշներին համապատասխանող տվյալակետերը։

Կազմաձևում՝
- **Զտման նշում**․ նշումներում որոնվող տեքստը
- **Մեծատառերի զգայունություն**․ ճշգրիտ հաշվի առնել տառերի գրությունը (կանխադրված՝ false)
- **Ճշգրիտ համընկնում**․ պահանջել ճշգրիտ համընկնում՝ ենթատողի փոխարեն (կանխադրված՝ false)
- **Շրջել**․ փոխարենը պահել չհամընկնող տվյալակետերը (կանխադրված՝ false)
    	]],
    	["az"] = [[
Məlumat nöqtələrini qeyd sahəsinə görə süzgəcdən keçirir. Yalnız süzgəc meyarlarına uyğun gələn məlumat nöqtələri keçir.

Konfiqurasiya:
- **Süzgəc qeydi**: Qeydlərdə axtarılacaq mətn
- **Böyük-kiçik hərfə həssas**: Böyük-kiçik hərf uyğunluğunu dəqiq tələb et (standart: false)
- **Dəqiq uyğunluq**: Alt sətir əvəzinə dəqiq uyğunluq tələb et (standart: false)
- **Tərsinə çevir**: Bunun əvəzinə uyğun gəlməyən məlumat nöqtələrini saxla (standart: false)
    	]],
    	["bn"] = [[
ডেটা পয়েন্টের নোট ক্ষেত্র অনুযায়ী ফিল্টার করে। কেবল ফিল্টার মানদণ্ডের সঙ্গে মিলে যাওয়া ডেটা পয়েন্টগুলোই পাস করবে।

কনফিগারেশন:
- **ফিল্টার নোট**: নোটে যে লেখা খোঁজা হবে
- **কেস সংবেদনশীল**: কেস হুবহু মেলান (ডিফল্ট: false)
- **হুবহু মিল**: সাবস্ট্রিংয়ের বদলে হুবহু মিল প্রয়োজন (ডিফল্ট: false)
- **উল্টান**: বরং যে ডেটা পয়েন্টগুলো মেলে না সেগুলো রাখুন (ডিফল্ট: false)
    	]],
    	["eu"] = [[
Datu-puntuak haien ohar-eremuaren arabera iragazten ditu. Iragazki-irizpideekin bat datozen datu-puntuak soilik igaroko dira.

Konfigurazioa:
- **Iragazki-oharra**: Oharretan bilatu beharreko testua
- **Maiuskulak/minuskulak bereizi**: Maiuskulak eta minuskulak zehazki bat etortzea (lehenetsia: false)
- **Zehazki bat etorri**: Azpikate baten ordez bat-etortze zehatza eskatzea (lehenetsia: false)
- **Alderantzikatu**: Horren ordez, bat ez datozen datu-puntuak mantentzea (lehenetsia: false)
    	]],
    	["be"] = [[
Фільтруе кропкі даных па полі заўвагі. Прапускаюцца толькі кропкі даных, якія адпавядаюць крытэрыям фільтра.

Канфігурацыя:
- **Заўвага фільтра**: тэкст для пошуку ў заўвагах
- **Улічваць рэгістр**: дакладна супастаўляць рэгістр (па змаўчанні: false)
- **Дакладнае супадзенне**: патрабаваць дакладнае супадзенне, а не ўваходжанне падрадка (па змаўчанні: false)
- **Інвертаваць**: замест гэтага пакідаць кропкі даных, якія НЕ адпавядаюць (па змаўчанні: false)
    	]],
    	["bg"] = [[
Филтрира точките от данни по полето им за бележка. Преминават само точките от данни, които отговарят на критериите за филтриране.

Конфигурация:
- **Бележка за филтриране**: Текстът, който се търси в бележките
- **Регистърът има значение**: Точно съвпадение на регистъра (по подразбиране: false)
- **Точно съвпадение**: Изисква точно съвпадение вместо съвпадение на подниз (по подразбиране: false)
- **Инвертиране**: Вместо това запазва точките от данни, които НЕ съвпадат (по подразбиране: false)
    	]],
    	["my"] = [[
ဒေတာမှတ်များကို ၎င်းတို့၏ မှတ်စုအကွက်အပေါ် အခြေခံ၍ စစ်ထုတ်သည်။ စစ်ထုတ်မှုစံနှုန်းနှင့် ကိုက်ညီသော ဒေတာမှတ်များသာ ဖြတ်သန်းမည်။

ဖွဲ့စည်းမှု:
- **စစ်ထုတ်မည့် မှတ်စု**: မှတ်စုများတွင် ရှာဖွေမည့် စာသား
- **စာလုံးအကြီးအသေး ခွဲခြားရန်**: စာလုံးအကြီးအသေးကို တိတိကျကျ ကိုက်ညီစေသည် (မူလ: false)
- **တိတိကျကျ ကိုက်ညီရန်**: စာသားတစ်စိတ်တစ်ပိုင်းအစား အတိအကျကိုက်ညီမှု လိုအပ်သည် (မူလ: false)
- **ပြောင်းပြန်**: မကိုက်ညီသော ဒေတာမှတ်များကိုသာ ထားသည် (မူလ: false)
    	]],
    	["ca"] = [[
Filtra els punts de dades pel camp de la nota. Només passen els punts de dades que compleixen els criteris de filtratge.

Configuració:
- **Nota de filtratge**: El text que cal cercar a les notes
- **Distingeix entre majúscules i minúscules**: Coincidència exacta de majúscules i minúscules (per defecte: false)
- **Coincidència exacta**: Requereix una coincidència exacta en lloc d’una subcadena (per defecte: false)
- **Inverteix**: Conserva els punts de dades que NO coincideixen (per defecte: false)
    	]],
    	["zh-Hans"] = [[
根据数据点的备注字段筛选数据点。只有符合筛选条件的数据点才会通过。

配置：
- **筛选备注**：要在备注中搜索的文本
- **区分大小写**：严格匹配大小写（默认：false）
- **完全匹配**：要求完全匹配，而不是子字符串匹配（默认：false）
- **反转**：改为保留不匹配的数据点（默认：false）
    	]],
    	["zh-Hant"] = [[
依資料點的備註欄位篩選。只有符合篩選條件的資料點才會通過。

設定：
- **篩選備註**：要在備註中搜尋的文字
- **區分大小寫**：完全比對大小寫（預設：false）
- **完全比對**：要求完全相符，而非部分文字相符（預設：false）
- **反轉**：改為保留不相符的資料點（預設：false）
    	]],
    	["hr"] = [[
Filtrira podatkovne točke prema polju bilješke. Prolaze samo podatkovne točke koje odgovaraju kriterijima filtra.

Konfiguracija:
- **Bilješka filtra**: Tekst koji se traži u bilješkama
- **Razlikuj velika i mala slova**: Točno podudaranje veličine slova (zadano: false)
- **Točno podudaranje**: Zahtijeva točno podudaranje umjesto podniza (zadano: false)
- **Invertiraj**: Umjesto toga zadrži podatkovne točke koje se NE podudaraju (zadano: false)
    	]],
    	["cs"] = [[
Filtruje datové body podle pole poznámky. Projdou pouze datové body odpovídající kritériím filtru.

Konfigurace:
- **Filtrovaná poznámka**: Text, který se má vyhledávat v poznámkách
- **Rozlišovat velká a malá písmena**: Přesně rozlišovat velikost písmen (výchozí: false)
- **Přesná shoda**: Vyžadovat přesnou shodu namísto podřetězce (výchozí: false)
- **Invertovat**: Ponechat místo toho datové body, které NEodpovídají (výchozí: false)
    	]],
    	["da"] = [[
Filtrerer datapunkter efter deres notefelt. Kun datapunkter, der matcher filterkriterierne, går videre.

Konfiguration:
- **Filternote**: Den tekst, der skal søges efter i noter
- **Forskel på store og små bogstaver**: Match store og små bogstaver nøjagtigt (standard: false)
- **Match nøjagtigt**: Kræv et nøjagtigt match i stedet for delstreng (standard: false)
- **Invertér**: Behold i stedet datapunkter, der IKKE matcher (standard: false)
    	]],
    	["nl"] = [[
Filtert gegevenspunten op basis van hun notitieveld. Alleen gegevenspunten die aan de filtercriteria voldoen, worden doorgelaten.

Configuratie:
- **Filternotitie**: De tekst waarnaar in notities wordt gezocht
- **Hoofdlettergevoelig**: Hoofdletters exact laten overeenkomen (standaard: false)
- **Exact overeenkomen**: Exacte overeenkomst vereisen in plaats van een deelreeks (standaard: false)
- **Omkeren**: In plaats daarvan gegevenspunten behouden die NIET overeenkomen (standaard: false)
    	]],
    	["et"] = [[
Filtreerib andmepunkte märkmevälja alusel. Läbivad ainult filtrikriteeriumidele vastavad andmepunktid.

Seadistus:
- **Filtri märkus**: märkmetest otsitav tekst
- **Tõstutundlik**: täpne tähesuuruse vaste (vaikimisi: väär)
- **Täielik vaste**: nõua alamstringi asemel täielikku vastet (vaikimisi: väär)
- **Pööra ümber**: jäta alles hoopis mittevastavad andmepunktid (vaikimisi: väär)
    	]],
    	["fil"] = [[
Sinasala ang mga data point ayon sa kanilang note field. Tanging mga data point na tumutugma sa pamantayan ng filter ang magpapatuloy.

Configuration:
- **Filter Note**: Tekstong hahanapin sa mga note
- **Case Sensitive**: Eksaktong itugma ang malaking-maliit na titik (default: false)
- **Match Exactly**: Mangailangan ng eksaktong tugma sa halip na substring (default: false)
- **Invert**: Sa halip, panatilihin ang mga data point na HINDI tumutugma (default: false)
    	]],
    	["fi"] = [[
Suodattaa datapisteet niiden muistiinpanokentän perusteella. Vain suodatusehdot täyttävät datapisteet päästetään läpi.

Määritys:
- **Suodatettava muistiinpano**: Muistiinpanoista etsittävä teksti
- **Kirjainkoko merkitsee**: Vastaa kirjainkokoa täsmälleen (oletus: false)
- **Täsmällinen vastaavuus**: Edellytä täsmällistä vastaavuutta osamerkkijonon sijaan (oletus: false)
- **Käänteinen**: Säilytä sen sijaan datapisteet, jotka EIVÄT täsmää (oletus: false)
    	]],
    	["fr"] = [[
Filtre les points de données selon leur champ de note. Seuls les points de données correspondant aux critères du filtre sont conservés.

Configuration :
- **Note du filtre** : Texte à rechercher dans les notes
- **Sensible à la casse** : Respecter exactement la casse (par défaut : false)
- **Correspondance exacte** : Exiger une correspondance exacte plutôt qu’une sous-chaîne (par défaut : false)
- **Inverser** : Conserver plutôt les points de données qui ne correspondent PAS (par défaut : false)
    	]],
    	["gl"] = [[
Filtra os puntos de datos polo seu campo de nota. Só pasan os puntos de datos que coincidan cos criterios do filtro.

Configuración:
- **Nota do filtro**: O texto que se buscará nas notas
- **Distinguir maiúsculas e minúsculas**: Coincidir exactamente no uso de maiúsculas e minúsculas (predeterminado: false)
- **Coincidir exactamente**: Esixir unha coincidencia exacta en lugar dunha subcadea (predeterminado: false)
- **Inverter**: Manter no seu lugar os puntos de datos que NON coincidan (predeterminado: false)
    	]],
    	["ka"] = [[
ფილტრავს მონაცემთა წერტილებს მათი შენიშვნის ველის მიხედვით. გაივლის მხოლოდ ფილტრის კრიტერიუმებთან შესაბამისი მონაცემთა წერტილები.

კონფიგურაცია:
- **გასაფილტრი შენიშვნა**: ტექსტი, რომელიც შენიშვნებში უნდა მოიძებნოს
- **რეგისტრის გათვალისწინება**: რეგისტრის ზუსტად დამთხვევა (ნაგულისხმევი: false)
- **ზუსტი დამთხვევა**: ქვესტრიქონის ნაცვლად ზუსტი დამთხვევის მოთხოვნა (ნაგულისხმევი: false)
- **ინვერსია**: სანაცვლოდ იმ მონაცემთა წერტილების დატოვება, რომლებიც არ ემთხვევა (ნაგულისხმევი: false)
    	]],
    	["de"] = [[
Filtert Datenpunkte anhand ihres Notizfelds. Nur Datenpunkte, die den Filterkriterien entsprechen, werden weitergegeben.

Konfiguration:
- **Filternotiz**: Der in Notizen zu suchende Text
- **Groß-/Kleinschreibung beachten**: Groß-/Kleinschreibung exakt abgleichen (Standard: false)
- **Exakte Übereinstimmung**: Exakte Übereinstimmung statt Teilübereinstimmung verlangen (Standard: false)
- **Umkehren**: Stattdessen Datenpunkte behalten, die NICHT übereinstimmen (Standard: false)
    	]],
    	["el"] = [[
Φιλτράρει τα σημεία δεδομένων με βάση το πεδίο σημείωσής τους. Περνούν μόνο τα σημεία δεδομένων που αντιστοιχούν στα κριτήρια φίλτρου.

Διαμόρφωση:
- **Σημείωση φίλτρου**: Το κείμενο προς αναζήτηση στις σημειώσεις
- **Διάκριση πεζών-κεφαλαίων**: Ακριβής αντιστοίχιση πεζών-κεφαλαίων (προεπιλογή: false)
- **Ακριβής αντιστοίχιση**: Απαίτηση ακριβούς αντιστοίχισης αντί για τμήμα κειμένου (προεπιλογή: false)
- **Αντιστροφή**: Διατήρηση των σημείων δεδομένων που ΔΕΝ αντιστοιχούν (προεπιλογή: false)
    	]],
    	["gu"] = [[
ડેટા પોઇન્ટ્સને તેમના નોંધ ફીલ્ડ દ્વારા ફિલ્ટર કરે છે. ફિલ્ટર માપદંડ સાથે મેળ ખાતા ડેટા પોઇન્ટ્સ જ પસાર થશે.

ગોઠવણી:
- **ફિલ્ટર નોંધ**: નોંધોમાં શોધવાનો ટેક્સ્ટ
- **કેસ સંવેદનશીલ**: કેસને ચોક્કસ રીતે મેળવો (ડિફૉલ્ટ: false)
- **ચોક્કસ મેળ**: સબસ્ટ્રિંગને બદલે ચોક્કસ મેળ જરૂરી કરો (ડિફૉલ્ટ: false)
- **ઉલટાવો**: તેના બદલે મેળ ન ખાતા ડેટા પોઇન્ટ્સ રાખો (ડિફૉલ્ટ: false)
    	]],
    	["hi"] = [[
डेटा पॉइंट को उनके नोट फ़ील्ड के आधार पर फ़िल्टर करता है। केवल फ़िल्टर मानदंड से मेल खाने वाले डेटा पॉइंट आगे जाते हैं।

कॉन्फ़िगरेशन:
- **फ़िल्टर नोट**: नोट में खोजा जाने वाला टेक्स्ट
- **केस संवेदनशील**: केस का हूबहू मिलान करें (डिफ़ॉल्ट: false)
- **सटीक मिलान**: सबस्ट्रिंग के बजाय सटीक मिलान आवश्यक करें (डिफ़ॉल्ट: false)
- **उलटें**: इसके बजाय वे डेटा पॉइंट रखें जो मेल नहीं खाते (डिफ़ॉल्ट: false)
    	]],
    	["hu"] = [[
Az adatpontokat a megjegyzésmezőjük alapján szűri. Csak a szűrési feltételeknek megfelelő adatpontok haladnak tovább.

Konfiguráció:
- **Szűrőmegjegyzés**: A megjegyzésekben keresendő szöveg
- **Kis- és nagybetűk megkülönböztetése**: A kis- és nagybetűk pontos egyezése (alapértelmezett: false)
- **Pontos egyezés**: A részszöveg helyett pontos egyezést követel meg (alapértelmezett: false)
- **Invertálás**: Ehelyett a NEM egyező adatpontokat tartja meg (alapértelmezett: false)
    	]],
    	["is"] = [[
Síar gagnapunkta eftir athugasemdarreit þeirra. Aðeins gagnapunktar sem passa við síuskilyrðin fara áfram.

Stillingar:
- **Síuathugasemd**: Textinn sem leita á að í athugasemdum
- **Háð há- og lágstöfum**: Samsvörun verður að vera nákvæm að há- og lágstöfum (sjálfgefið: false)
- **Nákvæm samsvörun**: Krefst nákvæmrar samsvörunar í stað hlutasamsvörunar (sjálfgefið: false)
- **Snúa við**: Halda í staðinn gagnapunktum sem passa EKKI (sjálfgefið: false)
    	]],
    	["id"] = [[
Menyaring titik data berdasarkan kolom catatannya. Hanya titik data yang sesuai dengan kriteria saringan yang akan diteruskan.

Konfigurasi:
- **Catatan Saringan**: Teks yang dicari dalam catatan
- **Peka Huruf Besar/Kecil**: Mencocokkan huruf besar/kecil secara tepat (default: false)
- **Cocokkan Tepat**: Memerlukan kecocokan tepat, bukan substring (default: false)
- **Balikkan**: Mempertahankan titik data yang TIDAK cocok (default: false)
    	]],
    	["it"] = [[
Filtra i punti dati in base al campo della nota. Passano solo i punti dati che corrispondono ai criteri di filtro.

Configurazione:
- **Nota del filtro**: il testo da cercare nelle note
- **Maiuscole/minuscole**: richiede la corrispondenza esatta tra maiuscole e minuscole (predefinito: false)
- **Corrispondenza esatta**: richiede una corrispondenza esatta invece di una sottostringa (predefinito: false)
- **Inverti**: mantiene invece i punti dati che NON corrispondono (predefinito: false)
    	]],
    	["ja"] = [[
メモフィールドに基づいてデータポイントを絞り込みます。フィルタ条件に一致するデータポイントだけが通過します。

設定:
- **フィルタメモ**: メモ内で検索するテキスト
- **大文字と小文字を区別**: 大文字と小文字を完全に一致させる（デフォルト: false）
- **完全一致**: 部分文字列ではなく完全一致を要求する（デフォルト: false）
- **反転**: 代わりに一致しないデータポイントを保持する（デフォルト: false）
    	]],
    	["kn"] = [[
ಡೇಟಾ ಬಿಂದುಗಳ ಟಿಪ್ಪಣಿ ಕ್ಷೇತ್ರದ ಆಧಾರದ ಮೇಲೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ. ಫಿಲ್ಟರ್ ಮಾನದಂಡಕ್ಕೆ ಹೊಂದುವ ಡೇಟಾ ಬಿಂದುಗಳು ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತವೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಫಿಲ್ಟರ್ ಟಿಪ್ಪಣಿ**: ಟಿಪ್ಪಣಿಗಳಲ್ಲಿ ಹುಡುಕಬೇಕಾದ ಪಠ್ಯ
- **ಅಕ್ಷರ ಗಾತ್ರ ಸಂವೇದಿ**: ಅಕ್ಷರ ಗಾತ್ರವನ್ನು ನಿಖರವಾಗಿ ಹೊಂದಿಸಿ (ಡೀಫಾಲ್ಟ್: false)
- **ನಿಖರವಾಗಿ ಹೊಂದಿಸಿ**: ಉಪಸ್ಟ್ರಿಂಗ್ ಬದಲು ನಿಖರ ಹೊಂದಾಣಿಕೆ ಅಗತ್ಯವಿರಲಿ (ಡೀಫಾಲ್ಟ್: false)
- **ವಿಲೋಮಗೊಳಿಸಿ**: ಹೊಂದದ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಉಳಿಸಿ (ಡೀಫಾಲ್ಟ್: false)
    	]],
    	["kk"] = [[
Дерек нүктелерін ескертпе өрісі бойынша сүзеді. Сүзгі шарттарына сәйкес келетін дерек нүктелері ғана өтеді.

Конфигурация:
- **Сүзгі ескертпесі**: Ескертпелерден ізделетін мәтін
- **Регистрді ескеру**: Регистрді дәл сәйкестендіру (әдепкі: false)
- **Дәл сәйкестік**: Ішкі жолдың орнына дәл сәйкестікті талап ету (әдепкі: false)
- **Кері айналдыру**: Сәйкес КЕЛМЕЙТІН дерек нүктелерін қалдыру (әдепкі: false)
    	]],
    	["km"] = [[
ត្រងចំណុចទិន្នន័យតាមវាលចំណាំរបស់វា។ មានតែចំណុចទិន្នន័យដែលត្រូវនឹងលក្ខខណ្ឌតម្រងប៉ុណ្ណោះ នឹងត្រូវបានបញ្ជូនបន្ត។

ការកំណត់៖
- **ចំណាំតម្រង**៖ អត្ថបទដែលត្រូវស្វែងរកក្នុងចំណាំ
- **ប្រកាន់អក្សរតូចធំ**៖ ផ្គូផ្គងទម្រង់អក្សរឲ្យដូចគ្នាពិតប្រាកដ (លំនាំដើម៖ false)
- **ផ្គូផ្គងពិតប្រាកដ**៖ តម្រូវឲ្យផ្គូផ្គងពិតប្រាកដ ជំនួសឲ្យផ្នែកនៃអត្ថបទ (លំនាំដើម៖ false)
- **បញ្ច្រាស**៖ រក្សាទុកចំណុចទិន្នន័យដែលមិនត្រូវនឹងលក្ខខណ្ឌ (លំនាំដើម៖ false)
    	]],
    	["ko"] = [[
메모 필드로 데이터 포인트를 필터링합니다. 필터 조건과 일치하는 데이터 포인트만 통과합니다.

구성:
- **필터 메모**: 메모에서 검색할 텍스트
- **대소문자 구분**: 대소문자를 정확히 일치시킵니다(기본값: false)
- **완전 일치**: 부분 문자열이 아닌 정확한 일치를 요구합니다(기본값: false)
- **반전**: 일치하지 않는 데이터 포인트를 대신 유지합니다(기본값: false)
    	]],
    	["ky"] = [[
Маалымат чекиттерин эскертме талаасы боюнча чыпкалайт. Чыпка критерийлерине туура келген маалымат чекиттери гана өткөрүлөт.

Тууралоо:
- **Чыпка эскертмеси**: Эскертмелерден изделе турган текст
- **Тамга регистрине сезгич**: Регистрди так дал келтирүү (демейки: false)
- **Так дал келтирүү**: Бөлүк саптын ордуна так дал келүүнү талап кылуу (демейки: false)
- **Тескери**: Анын ордуна дал келбеген маалымат чекиттерин калтыруу (демейки: false)
    	]],
    	["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນຕາມຊ່ອງໝາຍເຫດ. ສະເພາະຈຸດຂໍ້ມູນທີ່ກົງຕາມເງື່ອນໄຂການກັ່ນຕອງເທົ່ານັ້ນຈຶ່ງຈະຜ່ານ.

ການຕັ້ງຄ່າ:
- **ໝາຍເຫດສຳລັບກັ່ນຕອງ**: ຂໍ້ຄວາມທີ່ຈະຄົ້ນຫາໃນໝາຍເຫດ
- **ແຍກຕົວພິມ**: ຈັບຄູ່ຕົວພິມໃຫ້ກົງກັນ (ຄ່າເລີ່ມຕົ້ນ: false)
- **ກົງກັນທຸກຕົວ**: ຕ້ອງການການກົງກັນທັງໝົດແທນການກົງບາງສ່ວນ (ຄ່າເລີ່ມຕົ້ນ: false)
- **ກັບດ້ານ**: ເກັບຈຸດຂໍ້ມູນທີ່ບໍ່ກົງກັນແທນ (ຄ່າເລີ່ມຕົ້ນ: false)
    	]],
    	["lv"] = [[
Filtrē datu punktus pēc to piezīmes lauka. Tiks nodoti tikai filtra kritērijiem atbilstošie datu punkti.

Konfigurācija:
- **Filtrēšanas piezīme**: Piezīmēs meklējamais teksts
- **Reģistrjutīgs**: Precīzi atbilst reģistram (noklusējums: false)
- **Precīza atbilstība**: Nepieciešama precīza atbilstība, nevis apakšvirknes atbilstība (noklusējums: false)
- **Apgriezt**: Tā vietā patur datu punktus, kas NEATBILST (noklusējums: false)
    	]],
    	["lt"] = [[
Filtruoja duomenų taškus pagal jų pastabos lauką. Toliau perduodami tik filtravimo kriterijus atitinkantys duomenų taškai.

Konfigūracija:
- **Filtravimo pastaba**: Pastabose ieškomas tekstas
- **Skirti didžiąsias ir mažąsias raides**: Tiksliai atitikti raidžių dydį (numatyta: false)
- **Tikslus atitikmuo**: Reikalauti tikslaus atitikmens, o ne poeilutės (numatyta: false)
- **Invertuoti**: Vietoj to palikti neatitinkančius duomenų taškus (numatyta: false)
    	]],
    	["mk"] = [[
Ги филтрира точките на податоци според нивното поле за белешка. Само точките на податоци што ги исполнуваат критериумите на филтерот ќе поминат.

Конфигурација:
- **Белешка за филтрирање**: Текстот што се бара во белешките
- **Разликувај големи и мали букви**: Точно совпаѓање на големината на буквите (стандардно: false)
- **Точно совпаѓање**: Барај точно совпаѓање наместо подниза (стандардно: false)
- **Инвертирај**: Наместо тоа, задржи ги точките на податоци што НЕ се совпаѓаат (стандардно: false)
    	]],
    	["ms"] = [[
Menapis titik data berdasarkan medan notanya. Hanya titik data yang sepadan dengan kriteria penapis akan diteruskan.

Konfigurasi:
- **Nota Penapis**: Teks untuk dicari dalam nota
- **Sensitif Huruf Besar/Kecil**: Padankan huruf besar dan kecil dengan tepat (lalai: false)
- **Padan Tepat**: Memerlukan padanan tepat dan bukannya padanan separa (lalai: false)
- **Songsangkan**: Sebaliknya kekalkan titik data yang TIDAK sepadan (lalai: false)
    	]],
    	["ml"] = [[
ഡാറ്റാ പോയിന്റുകളുടെ note ഫീൽഡ് പ്രകാരം ഫിൽട്ടർ ചെയ്യുന്നു. ഫിൽട്ടർ മാനദണ്ഡങ്ങളുമായി പൊരുത്തപ്പെടുന്ന ഡാറ്റാ പോയിന്റുകൾ മാത്രമേ കടന്നുപോകൂ.

കോൺഫിഗറേഷൻ:
- **Filter Note**: കുറിപ്പുകളിൽ തിരയേണ്ട ടെക്സ്റ്റ്
- **Case Sensitive**: വലിയക്ഷരം/ചെറിയക്ഷരം കൃത്യമായി പൊരുത്തപ്പെടുത്തുക (സ്ഥിരസ്ഥിതി: false)
- **Match Exactly**: substring-ന് പകരം കൃത്യമായ പൊരുത്തം ആവശ്യപ്പെടുക (സ്ഥിരസ്ഥിതി: false)
- **Invert**: പൊരുത്തപ്പെടാത്ത ഡാറ്റാ പോയിന്റുകൾ പകരം നിലനിർത്തുക (സ്ഥിരസ്ഥിതി: false)
    	]],
    	["mr"] = [[
डेटा पॉइंट्स त्यांच्या नोट फील्डनुसार फिल्टर करतो. फिल्टर निकषांशी जुळणारे डेटा पॉइंट्सच पुढे जातील.

कॉन्फिगरेशन:
- **फिल्टर नोट**: नोट्समध्ये शोधायचा मजकूर
- **अक्षरप्रकार संवेदनशील**: अक्षरप्रकार तंतोतंत जुळवा (डीफॉल्ट: false)
- **तंतोतंत जुळवा**: सबस्ट्रिंगऐवजी तंतोतंत जुळणी आवश्यक (डीफॉल्ट: false)
- **उलट करा**: त्याऐवजी जुळत नसलेले डेटा पॉइंट्स ठेवा (डीफॉल्ट: false)
    	]],
    	["mn"] = [[
Өгөгдлийн цэгүүдийг тэмдэглэлийн талбараар нь шүүнэ. Шүүлтүүрийн шалгуурт нийцсэн өгөгдлийн цэгүүд л нэвтэрнэ.

Тохиргоо:
- **Шүүлтүүрийн тэмдэглэл**: Тэмдэглэлээс хайх текст
- **Том жижиг үсэг ялгах**: Үсгийн хэлбэрийг яг тааруулах (анхдагч: false)
- **Яг тааруулах**: Дэд мөр бус, яг таарсан утгыг шаардах (анхдагч: false)
- **Урвуулах**: Харин таарахгүй өгөгдлийн цэгүүдийг үлдээх (анхдагч: false)
    	]],
    	["ne"] = [[
डेटा बिन्दुहरूलाई तिनको नोट फिल्डअनुसार फिल्टर गर्छ। फिल्टर मापदण्डसँग मिल्ने डेटा बिन्दुहरू मात्र अघि पठाइन्छन्।

कन्फिगरेसन:
- **फिल्टर नोट**: नोटमा खोज्ने पाठ
- **अक्षरको आकार मिलाउने**: अक्षरको आकार ठ्याक्कै मिलाउने (पूर्वनिर्धारित: false)
- **ठ्याक्कै मिलाउने**: सबस्ट्रिङको सट्टा ठ्याक्कै मिलान आवश्यक पर्ने (पूर्वनिर्धारित: false)
- **उल्ट्याउने**: नमिल्ने डेटा बिन्दुहरू राख्ने (पूर्वनिर्धारित: false)
    	]],
    	["no"] = [[
Filtrerer datapunkter etter notatfeltet. Bare datapunkter som samsvarer med filterkriteriene, slipper gjennom.

Konfigurasjon:
- **Filternotat**: Teksten det skal søkes etter i notater
- **Skill mellom store og små bokstaver**: Samsvar med nøyaktig bokstavkombinasjon (standard: false)
- **Nøyaktig samsvar**: Krev nøyaktig samsvar i stedet for delstreng (standard: false)
- **Inverter**: Behold datapunkter som IKKE samsvarer i stedet (standard: false)
    	]],
    	["pl"] = [[
Filtruje punkty danych według pola notatki. Przechodzą tylko punkty danych spełniające kryteria filtra.

Konfiguracja:
- **Notatka filtra**: Tekst wyszukiwany w notatkach
- **Uwzględniaj wielkość liter**: Dopasowuj dokładnie wielkość liter (domyślnie: false)
- **Dopasuj dokładnie**: Wymagaj dokładnego dopasowania zamiast dopasowania fragmentu (domyślnie: false)
- **Odwróć**: Zamiast tego zachowaj punkty danych, które NIE pasują (domyślnie: false)
    	]],
    	["pt"] = [[
Filtra os pontos de dados pelo campo de nota. Apenas os pontos de dados que correspondem aos critérios de filtro passam.

Configuração:
- **Nota do filtro**: O texto a procurar nas notas
- **Sensível a maiúsculas e minúsculas**: Corresponder exatamente às maiúsculas e minúsculas (predefinição: false)
- **Correspondência exata**: Exigir correspondência exata em vez de uma parte do texto (predefinição: false)
- **Inverter**: Manter os pontos de dados que NÃO correspondem (predefinição: false)
    	]],
    	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਉਨ੍ਹਾਂ ਦੇ ਨੋਟ ਖੇਤਰ ਅਨੁਸਾਰ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਸਿਰਫ਼ ਫਿਲਟਰ ਮਾਪਦੰਡ ਨਾਲ ਮੇਲ ਖਾਂਦੇ ਡਾਟਾ ਪੁਆਇੰਟ ਅੱਗੇ ਜਾਣਗੇ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਫਿਲਟਰ ਨੋਟ**: ਨੋਟਾਂ ਵਿੱਚ ਖੋਜਣ ਲਈ ਲਿਖਤ
- **ਅੱਖਰ-ਆਕਾਰ ਸੰਵੇਦਨਸ਼ੀਲ**: ਅੱਖਰ-ਆਕਾਰ ਦਾ ਬਿਲਕੁਲ ਸਹੀ ਮਿਲਾਪ (ਡਿਫਾਲਟ: false)
- **ਬਿਲਕੁਲ ਮੇਲ**: ਸਬਸਟਰਿੰਗ ਦੀ ਬਜਾਏ ਪੂਰਾ ਮਿਲਾਪ ਲਾਜ਼ਮੀ ਕਰੋ (ਡਿਫਾਲਟ: false)
- **ਉਲਟਾਓ**: ਇਸ ਦੀ ਬਜਾਏ ਉਹ ਡਾਟਾ ਪੁਆਇੰਟ ਰੱਖੋ ਜੋ ਮੇਲ ਨਹੀਂ ਖਾਂਦੇ (ਡਿਫਾਲਟ: false)
    	]],
    	["ro"] = [[
Filtrează punctele de date după câmpul notă. Trec mai departe doar punctele de date care corespund criteriilor de filtrare.

Configurare:
- **Notă de filtrare**: Textul căutat în note
- **Sensibil la majuscule**: Potrivește exact literele mari și mici (implicit: false)
- **Potrivire exactă**: Necesită o potrivire exactă în locul unei subsecvențe (implicit: false)
- **Inversare**: Păstrează în schimb punctele de date care NU corespund (implicit: false)
    	]],
    	["rm"] = [[
Filtra ils puncts da datas tenor lur champ da nota. Mo ils puncts da datas che correspundan als criteris da filtraziun vegnan transmess.

Configuraziun:
- **Nota da filtrar**: Il text da tschertgar en las notas
- **Sensibel a maiusclas e minusclas**: Cumparegliar exactamain la grafia (default: false)
- **Correspundenza exacta**: Pretender ina correspundenza exacta empè d’ina part da text (default: false)
- **Inverter**: Mantegna enstagl ils puncts da datas che NA correspundan (default: false)
    	]],
    	["ru"] = [[
Фильтрует точки данных по полю заметки. Пропускаются только точки данных, соответствующие критериям фильтра.

Конфигурация:
- **Заметка фильтра**: Текст для поиска в заметках
- **С учётом регистра**: Точное совпадение регистра (по умолчанию: false)
- **Точное совпадение**: Требовать точное совпадение, а не совпадение подстроки (по умолчанию: false)
- **Инвертировать**: Оставлять точки данных, которые НЕ соответствуют критериям (по умолчанию: false)
    	]],
    	["sr"] = [[
Filtrira tačke podataka prema njihovom polju beleške. Prolaze samo tačke podataka koje odgovaraju kriterijumima filtera.

Konfiguracija:
- **Beleška filtera**: Tekst za pretragu u beleškama
- **Razlikuj velika i mala slova**: Tačno poređenje veličine slova (podrazumevano: false)
- **Potpuno podudaranje**: Zahteva potpuno podudaranje umesto podniza (podrazumevano: false)
- **Obrni**: Umesto toga zadržava tačke podataka koje se NE podudaraju (podrazumevano: false)
    	]],
    	["si"] = [[
දත්ත ලක්ෂ්‍ය ඒවායේ සටහන් ක්ෂේත්‍රය අනුව පෙරහන් කරයි. පෙරහන් නිර්ණායකයට ගැළපෙන දත්ත ලක්ෂ්‍ය පමණක් ඉදිරියට යවයි.

වින්‍යාසය:
- **පෙරහන් සටහන**: සටහන්වල සෙවිය යුතු පෙළ
- **අකුරු විශාලත්වය සංවේදී**: අකුරු විශාලත්වය හරියටම ගැළපීම (පෙරනිමිය: false)
- **හරියටම ගැළපීම**: උපපෙළක් වෙනුවට සම්පූර්ණ ගැළපීමක් අවශ්‍ය කිරීම (පෙරනිමිය: false)
- **ප්‍රතිවර්තනය**: ඒ වෙනුවට නොගැළපෙන දත්ත ලක්ෂ්‍ය තබා ගැනීම (පෙරනිමිය: false)
    	]],
    	["sk"] = [[
Filtruje údajové body podľa ich poľa poznámky. Prejdú iba údajové body zodpovedajúce kritériám filtra.

Konfigurácia:
- **Poznámka filtra**: Text, ktorý sa má vyhľadať v poznámkach
- **Rozlišovať veľké a malé písmená**: Presne rozlišovať veľkosť písmen (predvolené: false)
- **Presná zhoda**: Vyžadovať presnú zhodu namiesto podreťazca (predvolené: false)
- **Invertovať**: Namiesto toho ponechať údajové body, ktoré sa NEZHODUJÚ (predvolené: false)
    	]],
    	["sl"] = [[
Filtrira podatkovne točke glede na njihovo polje opombe. Prepustijo se le podatkovne točke, ki ustrezajo merilom filtra.

Konfiguracija:
- **Opomba filtra**: Besedilo za iskanje v opombah
- **Razlikuj velike in male črke**: Natančno ujemanje velikosti črk (privzeto: false)
- **Natančno ujemanje**: Zahtevaj popolno ujemanje namesto podniza (privzeto: false)
- **Obrni**: Namesto tega obdrži podatkovne točke, ki se NE ujemajo (privzeto: false)
    	]],
    	["es"] = [[
Filtra los puntos de datos por su campo de nota. Solo pasan los puntos de datos que coinciden con los criterios del filtro.

Configuración:
- **Nota del filtro**: El texto que se buscará en las notas
- **Distinguir mayúsculas y minúsculas**: Coincidir exactamente el uso de mayúsculas y minúsculas (predeterminado: false)
- **Coincidencia exacta**: Requerir una coincidencia exacta en lugar de una subcadena (predeterminado: false)
- **Invertir**: Conservar los puntos de datos que NO coincidan (predeterminado: false)
    	]],
    	["sw"] = [[
Huchuja nukta za data kulingana na sehemu yake ya dokezo. Ni nukta za data zinazolingana na vigezo vya kichujio pekee ndizo zitapitishwa.

Usanidi:
- **Dokezo la Kichujio**: Maandishi ya kutafuta kwenye madokezo
- **Herufi Kubwa/ Ndogo Muhimu**: Linganisha ukubwa wa herufi sawasawa (chaguo-msingi: false)
- **Linganisha Sawasawa**: Hitaji ulinganifu kamili badala ya sehemu ya maandishi (chaguo-msingi: false)
- **Geuza**: Hifadhi badala yake nukta za data zisizolingana (chaguo-msingi: false)
    	]],
    	["sv"] = [[
Filtrerar datapunkter efter deras anteckningsfält. Endast datapunkter som matchar filterkriterierna släpps igenom.

Konfiguration:
- **Filteranteckning**: Texten som ska sökas efter i anteckningar
- **Skiftlägeskänslig**: Matcha exakt skiftläge (standard: false)
- **Exakt matchning**: Kräv exakt matchning i stället för delsträng (standard: false)
- **Invertera**: Behåll i stället datapunkter som INTE matchar (standard: false)
    	]],
    	["ta"] = [[
தரவுப் புள்ளிகளின் குறிப்பு புலத்தின் அடிப்படையில் வடிகட்டுகிறது. வடிகட்டி நிபந்தனைகளுடன் பொருந்தும் தரவுப் புள்ளிகள் மட்டுமே தொடரும்.

உள்ளமைவு:
- **வடிகட்டி குறிப்பு**: குறிப்புகளில் தேட வேண்டிய உரை
- **எழுத்து வடிவம் பொருந்துதல்**: எழுத்து வடிவத்தைத் துல்லியமாகப் பொருத்து (இயல்புநிலை: false)
- **துல்லியமாகப் பொருத்து**: துணைச்சரத்திற்கு பதிலாக துல்லியமான பொருத்தம் தேவை (இயல்புநிலை: false)
- **தலைகீழாக்கு**: பொருந்தாத தரவுப் புள்ளிகளை வைத்திரு (இயல்புநிலை: false)
    	]],
    	["te"] = [[
డేటా పాయింట్ల నోట్ ఫీల్డ్ ద్వారా ఫిల్టర్ చేస్తుంది. ఫిల్టర్ ప్రమాణాలకు సరిపడే డేటా పాయింట్లు మాత్రమే కొనసాగుతాయి.

కాన్ఫిగరేషన్:
- **ఫిల్టర్ నోట్**: నోట్లలో శోధించాల్సిన వచనం
- **కేస్ సెన్సిటివ్**: అక్షరాల కేస్‌ను ఖచ్చితంగా సరిపోల్చండి (డిఫాల్ట్: false)
- **ఖచ్చితంగా సరిపోల్చు**: సబ్‌స్ట్రింగ్‌కు బదులుగా ఖచ్చితమైన సరిపోలిక అవసరం (డిఫాల్ట్: false)
- **విలోమం**: సరిపోని డేటా పాయింట్లను ఉంచండి (డిఫాల్ట్: false)
    	]],
    	["th"] = [[
กรองจุดข้อมูลตามฟิลด์บันทึก เฉพาะจุดข้อมูลที่ตรงตามเกณฑ์การกรองเท่านั้นที่จะถูกส่งต่อ

การกำหนดค่า:
- **บันทึกสำหรับกรอง**: ข้อความที่ต้องการค้นหาในบันทึก
- **คำนึงถึงตัวพิมพ์เล็กใหญ่**: จับคู่ตัวพิมพ์ให้ตรงกัน (ค่าเริ่มต้น: false)
- **ตรงกันทุกประการ**: ต้องตรงกันทุกประการแทนการจับคู่บางส่วน (ค่าเริ่มต้น: false)
- **กลับด้าน**: เก็บจุดข้อมูลที่ไม่ตรงกันแทน (ค่าเริ่มต้น: false)
    	]],
    	["tr"] = [[
Veri noktalarını not alanlarına göre filtreler. Yalnızca filtre ölçütleriyle eşleşen veri noktaları geçer.

Yapılandırma:
- **Filtre Notu**: Notlarda aranacak metin
- **Büyük/Küçük Harfe Duyarlı**: Büyük/küçük harfleri tam olarak eşleştir (varsayılan: false)
- **Tam Eşleştir**: Alt dize yerine tam eşleşme gerektir (varsayılan: false)
- **Tersine Çevir**: Bunun yerine eşleşmeyen veri noktalarını tut (varsayılan: false)
    	]],
    	["uk"] = [[
Фільтрує точки даних за полем примітки. Пропускаються лише точки даних, що відповідають критеріям фільтра.

Конфігурація:
- **Примітка фільтра**: Текст для пошуку в примітках
- **З урахуванням регістру**: Точний збіг регістру (типово: false)
- **Точний збіг**: Вимагати точного збігу замість збігу підрядка (типово: false)
- **Інвертувати**: Залишати натомість точки даних, що НЕ відповідають (типово: false)
    	]],
    	["vi"] = [[
Lọc các điểm dữ liệu theo trường ghi chú. Chỉ những điểm dữ liệu khớp với tiêu chí lọc mới được giữ lại.

Cấu hình:
- **Ghi chú lọc**: Văn bản cần tìm trong các ghi chú
- **Phân biệt chữ hoa chữ thường**: Khớp chính xác kiểu chữ (mặc định: false)
- **Khớp chính xác**: Yêu cầu khớp chính xác thay vì khớp chuỗi con (mặc định: false)
- **Đảo ngược**: Thay vào đó, giữ lại các điểm dữ liệu KHÔNG khớp (mặc định: false)
    	]],
    },
    config = {
        text {
            id = "filter_note",
            name = {
            	["en"] = "Filter Note",
            	["af"] = "Filternota",
            	["sq"] = "Shënimi i filtrit",
            	["am"] = "የማጣሪያ ማስታወሻ",
            	["hy"] = "Զտման նշում",
            	["az"] = "Süzgəc qeydi",
            	["bn"] = "ফিল্টার নোট",
            	["eu"] = "Iragazki-oharra",
            	["be"] = "Заўвага фільтра",
            	["bg"] = "Бележка за филтриране",
            	["my"] = "စစ်ထုတ်မည့် မှတ်စု",
            	["ca"] = "Nota de filtratge",
            	["zh-Hans"] = "筛选备注",
            	["zh-Hant"] = "篩選備註",
            	["hr"] = "Bilješka filtra",
            	["cs"] = "Filtrovaná poznámka",
            	["da"] = "Filternote",
            	["nl"] = "Filternotitie",
            	["et"] = "Filtri märkus",
            	["fil"] = "Filter Note",
            	["fi"] = "Suodatettava muistiinpano",
            	["fr"] = "Note du filtre",
            	["gl"] = "Nota do filtro",
            	["ka"] = "გასაფილტრი შენიშვნა",
            	["de"] = "Filternotiz",
            	["el"] = "Σημείωση φίλτρου",
            	["gu"] = "ફિલ્ટર નોંધ",
            	["hi"] = "फ़िल्टर नोट",
            	["hu"] = "Szűrőmegjegyzés",
            	["is"] = "Síuathugasemd",
            	["id"] = "Catatan Saringan",
            	["it"] = "Nota del filtro",
            	["ja"] = "フィルタメモ",
            	["kn"] = "ಫಿಲ್ಟರ್ ಟಿಪ್ಪಣಿ",
            	["kk"] = "Сүзгі ескертпесі",
            	["km"] = "ចំណាំតម្រង",
            	["ko"] = "필터 메모",
            	["ky"] = "Чыпка эскертмеси",
            	["lo"] = "ໝາຍເຫດສຳລັບກັ່ນຕອງ",
            	["lv"] = "Filtrēšanas piezīme",
            	["lt"] = "Filtravimo pastaba",
            	["mk"] = "Белешка за филтрирање",
            	["ms"] = "Nota Penapis",
            	["ml"] = "ഫിൽട്ടർ കുറിപ്പ്",
            	["mr"] = "फिल्टर नोट",
            	["mn"] = "Шүүлтүүрийн тэмдэглэл",
            	["ne"] = "फिल्टर नोट",
            	["no"] = "Filternotat",
            	["pl"] = "Notatka filtra",
            	["pt"] = "Nota do filtro",
            	["pa"] = "ਫਿਲਟਰ ਨੋਟ",
            	["ro"] = "Notă de filtrare",
            	["rm"] = "Nota da filtrar",
            	["ru"] = "Заметка фильтра",
            	["sr"] = "Beleška filtera",
            	["si"] = "පෙරහන් සටහන",
            	["sk"] = "Poznámka filtra",
            	["sl"] = "Opomba filtra",
            	["es"] = "Nota del filtro",
            	["sw"] = "Dokezo la Kichujio",
            	["sv"] = "Filteranteckning",
            	["ta"] = "வடிகட்டி குறிப்பு",
            	["te"] = "ఫిల్టర్ నోట్",
            	["th"] = "บันทึกสำหรับกรอง",
            	["tr"] = "Filtre Notu",
            	["uk"] = "Примітка фільтра",
            	["vi"] = "Ghi chú lọc",
            }
        },
        checkbox {
            id = "case_sensitive",
            name = "_case_sensitive",
        },
        checkbox {
            id = "match_exactly",
            name = "_match_exactly",
        },
        checkbox {
            id = "invert",
            name = {
            	["en"] = "Invert",
            	["af"] = "Inverteer",
            	["sq"] = "Përmbyse",
            	["am"] = "መቀልበስ",
            	["hy"] = "Շրջել",
            	["az"] = "Tərsinə çevir",
            	["bn"] = "উল্টান",
            	["eu"] = "Alderantzikatu",
            	["be"] = "Інвертаваць",
            	["bg"] = "Инвертиране",
            	["my"] = "ပြောင်းပြန်",
            	["ca"] = "Inverteix",
            	["zh-Hans"] = "反转",
            	["zh-Hant"] = "反轉",
            	["hr"] = "Invertiraj",
            	["cs"] = "Invertovat",
            	["da"] = "Invertér",
            	["nl"] = "Omkeren",
            	["et"] = "Pööra ümber",
            	["fil"] = "Invert",
            	["fi"] = "Käänteinen",
            	["fr"] = "Inverser",
            	["gl"] = "Inverter",
            	["ka"] = "ინვერსია",
            	["de"] = "Umkehren",
            	["el"] = "Αντιστροφή",
            	["gu"] = "ઉલટાવો",
            	["hi"] = "उलटें",
            	["hu"] = "Invertálás",
            	["is"] = "Snúa við",
            	["id"] = "Balikkan",
            	["it"] = "Inverti",
            	["ja"] = "反転",
            	["kn"] = "ವಿಲೋಮಗೊಳಿಸಿ",
            	["kk"] = "Кері айналдыру",
            	["km"] = "បញ្ច្រាស",
            	["ko"] = "반전",
            	["ky"] = "Тескери",
            	["lo"] = "ກັບດ້ານ",
            	["lv"] = "Apgriezt",
            	["lt"] = "Invertuoti",
            	["mk"] = "Инвертирај",
            	["ms"] = "Songsangkan",
            	["ml"] = "വിപരീതമാക്കുക",
            	["mr"] = "उलट करा",
            	["mn"] = "Урвуулах",
            	["ne"] = "उल्ट्याउने",
            	["no"] = "Inverter",
            	["pl"] = "Odwróć",
            	["pt"] = "Inverter",
            	["pa"] = "ਉਲਟਾਓ",
            	["ro"] = "Inversare",
            	["rm"] = "Inverter",
            	["ru"] = "Инвертировать",
            	["sr"] = "Obrni",
            	["si"] = "ප්‍රතිවර්තනය",
            	["sk"] = "Invertovať",
            	["sl"] = "Obrni",
            	["es"] = "Invertir",
            	["sw"] = "Geuza",
            	["sv"] = "Invertera",
            	["ta"] = "தலைகீழாக்கு",
            	["te"] = "విలోమం",
            	["th"] = "กลับด้าน",
            	["tr"] = "Tersine Çevir",
            	["uk"] = "Інвертувати",
            	["vi"] = "Đảo ngược",
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local filter_note = config and config.filter_note
        local case_sensitive = config and config.case_sensitive or false
        local match_exactly = config and config.match_exactly or false
        local invert = config and config.invert or false

        return function()
            local data_point = source.dp()
            local should_match = not invert
            while data_point and (match(data_point, filter_note, case_sensitive, match_exactly) ~= should_match) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
