-- Example Lua Function with Input Count and Configuration
-- This function filters data points by label

local tng_config = require("tng.config")
local text = tng_config.text
local checkbox = tng_config.checkbox

local function match(data_point, filter_label, case_sensitive, match_exactly)
    if filter_label == nil then
        return true
    end

    local data_label = data_point.label
    if not data_label then return false end

    -- Apply case sensitivity
    if not case_sensitive then
        data_label = string.lower(data_label)
        filter_label = string.lower(filter_label)
    end

    -- Apply matching mode
    if match_exactly then
        return data_label == filter_label
    else
        return string.find(data_label, filter_label, 1, true) ~= nil
    end
end


return {
    -- Configuration metadata
    id = "filter-by-label",
    version = "1.0.2",
    inputCount = 1,
    categories = {"_filter"},
    title = {
    	["en"] = "Filter by Label",
    	["af"] = "Filtreer volgens Etiket",
    	["sq"] = "Filtro sipas etiketës",
    	["am"] = "በመለያ አጣራ",
    	["hy"] = "Զտել ըստ պիտակի",
    	["az"] = "Etiketə görə süzgəcdən keçir",
    	["bn"] = "লেবেল দিয়ে ফিল্টার করুন",
    	["eu"] = "Iragazi etiketaren arabera",
    	["be"] = "Фільтраваць па ярлыку",
    	["bg"] = "Филтриране по етикет",
    	["my"] = "အညွှန်းဖြင့် စစ်ထုတ်ရန်",
    	["ca"] = "Filtra per etiqueta",
    	["zh-Hans"] = "按标签筛选",
    	["zh-Hant"] = "依標籤篩選",
    	["hr"] = "Filtriraj prema oznaci",
    	["cs"] = "Filtrovat podle štítku",
    	["da"] = "Filtrér efter etiket",
    	["nl"] = "Op label filteren",
    	["et"] = "Filtreeri sildi järgi",
    	["fil"] = "Salain ayon sa Label",
    	["fi"] = "Suodata selitteen perusteella",
    	["fr"] = "Filtrer par libellé",
    	["gl"] = "Filtrar por etiqueta",
    	["ka"] = "იარლიყით ფილტრაცია",
    	["de"] = "Nach Label filtern",
    	["el"] = "Φιλτράρισμα κατά ετικέτα",
    	["gu"] = "લેબલ દ્વારા ફિલ્ટર કરો",
    	["hi"] = "लेबल के अनुसार फ़िल्टर करें",
    	["hu"] = "Szűrés címke alapján",
    	["is"] = "Sía eftir merki",
    	["id"] = "Saring berdasarkan Label",
    	["it"] = "Filtra per etichetta",
    	["ja"] = "ラベルでフィルタ",
    	["kn"] = "ಲೇಬಲ್ ಮೂಲಕ ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Жапсырма бойынша сүзу",
    	["km"] = "ត្រងតាមស្លាក",
    	["ko"] = "라벨로 필터링",
    	["ky"] = "Энбелги боюнча чыпкалоо",
    	["lo"] = "ກັ່ນຕອງຕາມປ້າຍກຳກັບ",
    	["lv"] = "Filtrēt pēc etiķetes",
    	["lt"] = "Filtruoti pagal etiketę",
    	["mk"] = "Филтрирај според ознака",
    	["ms"] = "Tapis mengikut Label",
    	["ml"] = "ലേബൽ പ്രകാരം ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "लेबलनुसार फिल्टर करा",
    	["mn"] = "Шошгоор шүүх",
    	["ne"] = "लेबलअनुसार फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer etter etikett",
    	["pl"] = "Filtruj według etykiety",
    	["pt"] = "Filtrar por rótulo",
    	["pa"] = "ਲੇਬਲ ਅਨੁਸਾਰ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează după etichetă",
    	["rm"] = "Filtrar tenor etichetta",
    	["ru"] = "Фильтр по метке",
    	["sr"] = "Filtriraj prema oznaci",
    	["si"] = "ලේබලය අනුව පෙරහන් කරන්න",
    	["sk"] = "Filtrovať podľa označenia",
    	["sl"] = "Filtriraj po oznaki",
    	["es"] = "Filtrar por etiqueta",
    	["sw"] = "Chuja kwa Lebo",
    	["sv"] = "Filtrera efter etikett",
    	["ta"] = "லேபிளால் வடிகட்டு",
    	["te"] = "లేబుల్ ద్వారా ఫిల్టర్ చేయి",
    	["th"] = "กรองตามป้ายกำกับ",
    	["tr"] = "Etikete Göre Filtrele",
    	["uk"] = "Фільтрувати за міткою",
    	["vi"] = "Lọc theo nhãn",
    },
    description = {
    	["en"] = [[
Filters data points by their label field. Only data points matching the filter criteria will pass through.

Configuration:
- **Filter Label**: The text to search for in labels
- **Case Sensitive**: Match case exactly (default: false)
- **Match Exactly**: Require exact match instead of substring (default: false)
- **Invert**: Keep data points that DON'T match instead (default: false)
    	]],
    	["af"] = [[
Filtreer datapunte volgens hul etiketveld. Slegs datapunte wat aan die filterkriteria voldoen, word deurgegee.

Konfigurasie:
- **Filteretiket**: Die teks waarna in etikette gesoek word
- **Hooflettergevoelig**: Pas hoofletters presies aan (verstek: vals)
- **Presiese passing**: Vereis ’n presiese passing in plaas van ’n substring (verstek: vals)
- **Inverteer**: Behou eerder datapunte wat NIE pas nie (verstek: vals)
    	]],
    	["sq"] = [[
Filtron pikat e të dhënave sipas fushës së etiketës. Vetëm pikat e të dhënave që përputhen me kriteret e filtrit do të kalojnë.

Konfigurimi:
- **Etiketa e filtrit**: Teksti që kërkohet në etiketa
- **Ndjeshmëria ndaj shkronjave**: Përputhje e saktë e shkronjave të mëdha dhe të vogla (parazgjedhja: false)
- **Përputhje e saktë**: Kërkon përputhje të plotë në vend të nënvargut (parazgjedhja: false)
- **Përmbyse**: Mbaj në vend të tyre pikat e të dhënave që NUK përputhen (parazgjedhja: false)
    	]],
    	["am"] = [[
የውሂብ ነጥቦችን በመለያ መስካቸው ያጣራል። የማጣሪያ መስፈርቱን የሚያሟሉ የውሂብ ነጥቦች ብቻ ያልፋሉ።

ውቅር፦
- **የማጣሪያ መለያ**፦ በመለያዎች ውስጥ የሚፈለገው ጽሑፍ
- **ማክበር የሚገባውን ፊደል መለየት**፦ የፊደል አጠቃቀሙን በትክክል ማዛመድ (ነባሪ፦ false)
- **በትክክል ማዛመድ**፦ ከከፊል ማዛመድ ይልቅ ትክክለኛ ማዛመድን መጠየቅ (ነባሪ፦ false)
- **መቀልበስ**፦ የማይዛመዱ የውሂብ ነጥቦችን መያዝ (ነባሪ፦ false)
    	]],
    	["hy"] = [[
Զտում է տվյալակետերն ըստ դրանց պիտակի դաշտի։ Անցնում են միայն զտման չափանիշներին համապատասխանող տվյալակետերը։

Կազմաձևում՝
- **Զտման պիտակ**․ պիտակներում որոնվող տեքստը
- **Մեծատառերի զգայունություն**․ ճշգրիտ հաշվի առնել տառերի գրությունը (կանխադրված՝ false)
- **Ճշգրիտ համընկնում**․ պահանջել ճշգրիտ համընկնում՝ ենթատողի փոխարեն (կանխադրված՝ false)
- **Շրջել**․ փոխարենը պահել չհամընկնող տվյալակետերը (կանխադրված՝ false)
    	]],
    	["az"] = [[
Məlumat nöqtələrini etiket sahəsinə görə süzgəcdən keçirir. Yalnız süzgəc meyarlarına uyğun gələn məlumat nöqtələri keçir.

Konfiqurasiya:
- **Süzgəc etiketi**: Etiketlərdə axtarılacaq mətn
- **Böyük-kiçik hərfə həssas**: Böyük-kiçik hərf uyğunluğunu dəqiq tələb et (standart: false)
- **Dəqiq uyğunluq**: Alt sətir əvəzinə dəqiq uyğunluq tələb et (standart: false)
- **Tərsinə çevir**: Bunun əvəzinə uyğun gəlməyən məlumat nöqtələrini saxla (standart: false)
    	]],
    	["bn"] = [[
ডেটা পয়েন্টের লেবেল ক্ষেত্র অনুযায়ী ফিল্টার করে। কেবল ফিল্টার মানদণ্ডের সঙ্গে মিলে যাওয়া ডেটা পয়েন্টগুলোই পাস করবে।

কনফিগারেশন:
- **ফিল্টার লেবেল**: লেবেলে যে লেখা খোঁজা হবে
- **কেস সংবেদনশীল**: কেস হুবহু মেলান (ডিফল্ট: false)
- **হুবহু মিল**: সাবস্ট্রিংয়ের বদলে হুবহু মিল প্রয়োজন (ডিফল্ট: false)
- **উল্টান**: বরং যে ডেটা পয়েন্টগুলো মেলে না সেগুলো রাখুন (ডিফল্ট: false)
    	]],
    	["eu"] = [[
Datu-puntuak haien etiketa-eremuaren arabera iragazten ditu. Iragazki-irizpideekin bat datozen datu-puntuak soilik igaroko dira.

Konfigurazioa:
- **Iragazki-etiketa**: Etiketetan bilatu beharreko testua
- **Maiuskulak/minuskulak bereizi**: Maiuskulak eta minuskulak zehazki bat etortzea (lehenetsia: false)
- **Zehazki bat etorri**: Azpikate baten ordez bat-etortze zehatza eskatzea (lehenetsia: false)
- **Alderantzikatu**: Horren ordez, bat ez datozen datu-puntuak mantentzea (lehenetsia: false)
    	]],
    	["be"] = [[
Фільтруе кропкі даных па полі ярлыка. Прапускаюцца толькі кропкі даных, якія адпавядаюць крытэрыям фільтра.

Канфігурацыя:
- **Ярлык фільтра**: тэкст для пошуку ў ярлыках
- **Улічваць рэгістр**: дакладна супастаўляць рэгістр (па змаўчанні: false)
- **Дакладнае супадзенне**: патрабаваць дакладнае супадзенне, а не ўваходжанне падрадка (па змаўчанні: false)
- **Інвертаваць**: замест гэтага пакідаць кропкі даных, якія НЕ адпавядаюць (па змаўчанні: false)
    	]],
    	["bg"] = [[
Филтрира точките от данни по полето им за етикет. Преминават само точките от данни, които отговарят на критериите за филтриране.

Конфигурация:
- **Етикет за филтриране**: Текстът, който се търси в етикетите
- **Регистърът има значение**: Точно съвпадение на регистъра (по подразбиране: false)
- **Точно съвпадение**: Изисква точно съвпадение вместо съвпадение на подниз (по подразбиране: false)
- **Инвертиране**: Вместо това запазва точките от данни, които НЕ съвпадат (по подразбиране: false)
    	]],
    	["my"] = [[
ဒေတာမှတ်များကို ၎င်းတို့၏ အညွှန်းအကွက်အပေါ် အခြေခံ၍ စစ်ထုတ်သည်။ စစ်ထုတ်မှုစံနှုန်းနှင့် ကိုက်ညီသော ဒေတာမှတ်များသာ ဖြတ်သန်းမည်။

ဖွဲ့စည်းမှု:
- **စစ်ထုတ်မည့် အညွှန်း**: အညွှန်းများတွင် ရှာဖွေမည့် စာသား
- **စာလုံးအကြီးအသေး ခွဲခြားရန်**: စာလုံးအကြီးအသေးကို တိတိကျကျ ကိုက်ညီစေသည် (မူလ: false)
- **တိတိကျကျ ကိုက်ညီရန်**: စာသားတစ်စိတ်တစ်ပိုင်းအစား အတိအကျကိုက်ညီမှု လိုအပ်သည် (မူလ: false)
- **ပြောင်းပြန်**: မကိုက်ညီသော ဒေတာမှတ်များကိုသာ ထားသည် (မူလ: false)
    	]],
    	["ca"] = [[
Filtra els punts de dades pel camp de l’etiqueta. Només passen els punts de dades que compleixen els criteris de filtratge.

Configuració:
- **Etiqueta de filtratge**: El text que cal cercar a les etiquetes
- **Distingeix entre majúscules i minúscules**: Coincidència exacta de majúscules i minúscules (per defecte: false)
- **Coincidència exacta**: Requereix una coincidència exacta en lloc d’una subcadena (per defecte: false)
- **Inverteix**: Conserva els punts de dades que NO coincideixen (per defecte: false)
    	]],
    	["zh-Hans"] = [[
根据数据点的标签字段筛选数据点。只有符合筛选条件的数据点才会通过。

配置：
- **筛选标签**：要在标签中搜索的文本
- **区分大小写**：严格匹配大小写（默认：false）
- **完全匹配**：要求完全匹配，而不是子字符串匹配（默认：false）
- **反转**：改为保留不匹配的数据点（默认：false）
    	]],
    	["zh-Hant"] = [[
依資料點的標籤欄位篩選。只有符合篩選條件的資料點才會通過。

設定：
- **篩選標籤**：要在標籤中搜尋的文字
- **區分大小寫**：完全比對大小寫（預設：false）
- **完全比對**：要求完全相符，而非部分文字相符（預設：false）
- **反轉**：改為保留不相符的資料點（預設：false）
    	]],
    	["hr"] = [[
Filtrira podatkovne točke prema polju oznake. Prolaze samo podatkovne točke koje odgovaraju kriterijima filtra.

Konfiguracija:
- **Oznaka filtra**: Tekst koji se traži u oznakama
- **Razlikuj velika i mala slova**: Točno podudaranje veličine slova (zadano: false)
- **Točno podudaranje**: Zahtijeva točno podudaranje umjesto podniza (zadano: false)
- **Invertiraj**: Umjesto toga zadrži podatkovne točke koje se NE podudaraju (zadano: false)
    	]],
    	["cs"] = [[
Filtruje datové body podle pole štítku. Projdou pouze datové body odpovídající kritériím filtru.

Konfigurace:
- **Filtrovaný štítek**: Text, který se má vyhledávat ve štítcích
- **Rozlišovat velká a malá písmena**: Přesně rozlišovat velikost písmen (výchozí: false)
- **Přesná shoda**: Vyžadovat přesnou shodu namísto podřetězce (výchozí: false)
- **Invertovat**: Ponechat místo toho datové body, které NEodpovídají (výchozí: false)
    	]],
    	["da"] = [[
Filtrerer datapunkter efter deres etiketfelt. Kun datapunkter, der matcher filterkriterierne, går videre.

Konfiguration:
- **Filteretiket**: Den tekst, der skal søges efter i etiketter
- **Forskel på store og små bogstaver**: Match store og små bogstaver nøjagtigt (standard: false)
- **Match nøjagtigt**: Kræv et nøjagtigt match i stedet for delstreng (standard: false)
- **Invertér**: Behold i stedet datapunkter, der IKKE matcher (standard: false)
    	]],
    	["nl"] = [[
Filtert gegevenspunten op basis van hun lab‌elveld. Alleen gegevenspunten die aan de filtercriteria voldoen, worden doorgelaten.

Configuratie:
- **Filterlabel**: De tekst waarnaar in labels wordt gezocht
- **Hoofdlettergevoelig**: Hoofdletters exact laten overeenkomen (standaard: false)
- **Exact overeenkomen**: Exacte overeenkomst vereisen in plaats van een deelreeks (standaard: false)
- **Omkeren**: In plaats daarvan gegevenspunten behouden die NIET overeenkomen (standaard: false)
    	]],
    	["et"] = [[
Filtreerib andmepunkte sildiveeru alusel. Läbivad ainult filtrikriteeriumidele vastavad andmepunktid.

Seadistus:
- **Filtri silt**: siltidelt otsitav tekst
- **Tõstutundlik**: täpne tähesuuruse vaste (vaikimisi: väär)
- **Täielik vaste**: nõua alamstringi asemel täielikku vastet (vaikimisi: väär)
- **Pööra ümber**: jäta alles hoopis mittevastavad andmepunktid (vaikimisi: väär)
    	]],
    	["fil"] = [[
Sinasala ang mga data point ayon sa kanilang label field. Tanging mga data point na tumutugma sa pamantayan ng filter ang magpapatuloy.

Configuration:
- **Filter Label**: Tekstong hahanapin sa mga label
- **Case Sensitive**: Eksaktong itugma ang malaking-maliit na titik (default: false)
- **Match Exactly**: Mangailangan ng eksaktong tugma sa halip na substring (default: false)
- **Invert**: Sa halip, panatilihin ang mga data point na HINDI tumutugma (default: false)
    	]],
    	["fi"] = [[
Suodattaa datapisteet niiden selitekentän perusteella. Vain suodatusehdot täyttävät datapisteet päästetään läpi.

Määritys:
- **Suodatettava selite**: Selitteistä etsittävä teksti
- **Kirjainkoko merkitsee**: Vastaa kirjainkokoa täsmälleen (oletus: false)
- **Täsmällinen vastaavuus**: Edellytä täsmällistä vastaavuutta osamerkkijonon sijaan (oletus: false)
- **Käänteinen**: Säilytä sen sijaan datapisteet, jotka EIVÄT täsmää (oletus: false)
    	]],
    	["fr"] = [[
Filtre les points de données selon leur champ de libellé. Seuls les points de données correspondant aux critères du filtre sont conservés.

Configuration :
- **Libellé du filtre** : Texte à rechercher dans les libellés
- **Sensible à la casse** : Respecter exactement la casse (par défaut : false)
- **Correspondance exacte** : Exiger une correspondance exacte plutôt qu’une sous-chaîne (par défaut : false)
- **Inverser** : Conserver plutôt les points de données qui ne correspondent PAS (par défaut : false)
    	]],
    	["gl"] = [[
Filtra os puntos de datos polo seu campo de etiqueta. Só pasan os puntos de datos que coincidan cos criterios do filtro.

Configuración:
- **Etiqueta do filtro**: O texto que se buscará nas etiquetas
- **Distinguir maiúsculas e minúsculas**: Coincidir exactamente no uso de maiúsculas e minúsculas (predeterminado: false)
- **Coincidir exactamente**: Esixir unha coincidencia exacta en lugar dunha subcadea (predeterminado: false)
- **Inverter**: Manter no seu lugar os puntos de datos que NON coincidan (predeterminado: false)
    	]],
    	["ka"] = [[
ფილტრავს მონაცემთა წერტილებს მათი იარლიყის ველის მიხედვით. გაივლის მხოლოდ ფილტრის კრიტერიუმებთან შესაბამისი მონაცემთა წერტილები.

კონფიგურაცია:
- **გასაფილტრი იარლიყი**: ტექსტი, რომელიც იარლიყებში უნდა მოიძებნოს
- **რეგისტრის გათვალისწინება**: რეგისტრის ზუსტად დამთხვევა (ნაგულისხმევი: false)
- **ზუსტი დამთხვევა**: ქვესტრიქონის ნაცვლად ზუსტი დამთხვევის მოთხოვნა (ნაგულისხმევი: false)
- **ინვერსია**: სანაცვლოდ იმ მონაცემთა წერტილების დატოვება, რომლებიც არ ემთხვევა (ნაგულისხმევი: false)
    	]],
    	["de"] = [[
Filtert Datenpunkte anhand ihres Labelfelds. Nur Datenpunkte, die den Filterkriterien entsprechen, werden weitergegeben.

Konfiguration:
- **Filterlabel**: Der in Labels zu suchende Text
- **Groß-/Kleinschreibung beachten**: Groß-/Kleinschreibung exakt abgleichen (Standard: false)
- **Exakte Übereinstimmung**: Exakte Übereinstimmung statt Teilübereinstimmung verlangen (Standard: false)
- **Umkehren**: Stattdessen Datenpunkte behalten, die NICHT übereinstimmen (Standard: false)
    	]],
    	["el"] = [[
Φιλτράρει τα σημεία δεδομένων με βάση το πεδίο ετικέτας τους. Περνούν μόνο τα σημεία δεδομένων που αντιστοιχούν στα κριτήρια φίλτρου.

Διαμόρφωση:
- **Ετικέτα φίλτρου**: Το κείμενο προς αναζήτηση στις ετικέτες
- **Διάκριση πεζών-κεφαλαίων**: Ακριβής αντιστοίχιση πεζών-κεφαλαίων (προεπιλογή: false)
- **Ακριβής αντιστοίχιση**: Απαίτηση ακριβούς αντιστοίχισης αντί για τμήμα κειμένου (προεπιλογή: false)
- **Αντιστροφή**: Διατήρηση των σημείων δεδομένων που ΔΕΝ αντιστοιχούν (προεπιλογή: false)
    	]],
    	["gu"] = [[
ડેટા પોઇન્ટ્સને તેમના લેબલ ફીલ્ડ દ્વારા ફિલ્ટર કરે છે. ફિલ્ટર માપદંડ સાથે મેળ ખાતા ડેટા પોઇન્ટ્સ જ પસાર થશે.

ગોઠવણી:
- **ફિલ્ટર લેબલ**: લેબલ્સમાં શોધવાનો ટેક્સ્ટ
- **કેસ સંવેદનશીલ**: કેસને ચોક્કસ રીતે મેળવો (ડિફૉલ્ટ: false)
- **ચોક્કસ મેળ**: સબસ્ટ્રિંગને બદલે ચોક્કસ મેળ જરૂરી કરો (ડિફૉલ્ટ: false)
- **ઉલટાવો**: તેના બદલે મેળ ન ખાતા ડેટા પોઇન્ટ્સ રાખો (ડિફૉલ્ટ: false)
    	]],
    	["hi"] = [[
डेटा पॉइंट को उनके लेबल फ़ील्ड के आधार पर फ़िल्टर करता है। केवल फ़िल्टर मानदंड से मेल खाने वाले डेटा पॉइंट आगे जाते हैं।

कॉन्फ़िगरेशन:
- **फ़िल्टर लेबल**: लेबल में खोजा जाने वाला टेक्स्ट
- **केस संवेदनशील**: केस का हूबहू मिलान करें (डिफ़ॉल्ट: false)
- **सटीक मिलान**: सबस्ट्रिंग के बजाय सटीक मिलान आवश्यक करें (डिफ़ॉल्ट: false)
- **उलटें**: इसके बजाय वे डेटा पॉइंट रखें जो मेल नहीं खाते (डिफ़ॉल्ट: false)
    	]],
    	["hu"] = [[
Az adatpontokat a címkemezőjük alapján szűri. Csak a szűrési feltételeknek megfelelő adatpontok haladnak tovább.

Konfiguráció:
- **Szűrőcímke**: A címkékben keresendő szöveg
- **Kis- és nagybetűk megkülönböztetése**: A kis- és nagybetűk pontos egyezése (alapértelmezett: false)
- **Pontos egyezés**: A részszöveg helyett pontos egyezést követel meg (alapértelmezett: false)
- **Invertálás**: Ehelyett a NEM egyező adatpontokat tartja meg (alapértelmezett: false)
    	]],
    	["is"] = [[
Síar gagnapunkta eftir merkingarreit þeirra. Aðeins gagnapunktar sem passa við síuskilyrðin fara áfram.

Stillingar:
- **Síumerki**: Textinn sem leita á að í merkjum
- **Háð há- og lágstöfum**: Samsvörun verður að vera nákvæm að há- og lágstöfum (sjálfgefið: false)
- **Nákvæm samsvörun**: Krefst nákvæmrar samsvörunar í stað hlutasamsvörunar (sjálfgefið: false)
- **Snúa við**: Halda í staðinn gagnapunktum sem passa EKKI (sjálfgefið: false)
    	]],
    	["id"] = [[
Menyaring titik data berdasarkan kolom labelnya. Hanya titik data yang sesuai dengan kriteria saringan yang akan diteruskan.

Konfigurasi:
- **Label Saringan**: Teks yang dicari dalam label
- **Peka Huruf Besar/Kecil**: Mencocokkan huruf besar/kecil secara tepat (default: false)
- **Cocokkan Tepat**: Memerlukan kecocokan tepat, bukan substring (default: false)
- **Balikkan**: Mempertahankan titik data yang TIDAK cocok (default: false)
    	]],
    	["it"] = [[
Filtra i punti dati in base al campo dell'etichetta. Passano solo i punti dati che corrispondono ai criteri di filtro.

Configurazione:
- **Etichetta del filtro**: il testo da cercare nelle etichette
- **Maiuscole/minuscole**: richiede la corrispondenza esatta tra maiuscole e minuscole (predefinito: false)
- **Corrispondenza esatta**: richiede una corrispondenza esatta invece di una sottostringa (predefinito: false)
- **Inverti**: mantiene invece i punti dati che NON corrispondono (predefinito: false)
    	]],
    	["ja"] = [[
ラベルフィールドに基づいてデータポイントを絞り込みます。フィルタ条件に一致するデータポイントだけが通過します。

設定:
- **フィルタラベル**: ラベル内で検索するテキスト
- **大文字と小文字を区別**: 大文字と小文字を完全に一致させる（デフォルト: false）
- **完全一致**: 部分文字列ではなく完全一致を要求する（デフォルト: false）
- **反転**: 代わりに一致しないデータポイントを保持する（デフォルト: false）
    	]],
    	["kn"] = [[
ಡೇಟಾ ಬಿಂದುಗಳ ಲೇಬಲ್ ಕ್ಷೇತ್ರದ ಆಧಾರದ ಮೇಲೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ. ಫಿಲ್ಟರ್ ಮಾನದಂಡಕ್ಕೆ ಹೊಂದುವ ಡೇಟಾ ಬಿಂದುಗಳು ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತವೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಫಿಲ್ಟರ್ ಲೇಬಲ್**: ಲೇಬಲ್‌ಗಳಲ್ಲಿ ಹುಡುಕಬೇಕಾದ ಪಠ್ಯ
- **ಅಕ್ಷರ ಗಾತ್ರ ಸಂವೇದಿ**: ಅಕ್ಷರ ಗಾತ್ರವನ್ನು ನಿಖರವಾಗಿ ಹೊಂದಿಸಿ (ಡೀಫಾಲ್ಟ್: false)
- **ನಿಖರವಾಗಿ ಹೊಂದಿಸಿ**: ಉಪಸ್ಟ್ರಿಂಗ್ ಬದಲು ನಿಖರ ಹೊಂದಾಣಿಕೆ ಅಗತ್ಯವಿರಲಿ (ಡೀಫಾಲ್ಟ್: false)
- **ವಿಲೋಮಗೊಳಿಸಿ**: ಹೊಂದದ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಉಳಿಸಿ (ಡೀಫಾಲ್ಟ್: false)
    	]],
    	["kk"] = [[
Дерек нүктелерін жапсырма өрісі бойынша сүзеді. Сүзгі шарттарына сәйкес келетін дерек нүктелері ғана өтеді.

Конфигурация:
- **Сүзгі жапсырмасы**: Жапсырмалардан ізделетін мәтін
- **Регистрді ескеру**: Регистрді дәл сәйкестендіру (әдепкі: false)
- **Дәл сәйкестік**: Ішкі жолдың орнына дәл сәйкестікті талап ету (әдепкі: false)
- **Кері айналдыру**: Сәйкес КЕЛМЕЙТІН дерек нүктелерін қалдыру (әдепкі: false)
    	]],
    	["km"] = [[
ត្រងចំណុចទិន្នន័យតាមវាលស្លាករបស់វា។ មានតែចំណុចទិន្នន័យដែលត្រូវនឹងលក្ខខណ្ឌតម្រងប៉ុណ្ណោះ នឹងត្រូវបានបញ្ជូនបន្ត។

ការកំណត់៖
- **ស្លាកតម្រង**៖ អត្ថបទដែលត្រូវស្វែងរកក្នុងស្លាក
- **ប្រកាន់អក្សរតូចធំ**៖ ផ្គូផ្គងទម្រង់អក្សរឲ្យដូចគ្នាពិតប្រាកដ (លំនាំដើម៖ false)
- **ផ្គូផ្គងពិតប្រាកដ**៖ តម្រូវឲ្យផ្គូផ្គងពិតប្រាកដ ជំនួសឲ្យផ្នែកនៃអត្ថបទ (លំនាំដើម៖ false)
- **បញ្ច្រាស**៖ រក្សាទុកចំណុចទិន្នន័យដែលមិនត្រូវនឹងលក្ខខណ្ឌ (លំនាំដើម៖ false)
    	]],
    	["ko"] = [[
라벨 필드로 데이터 포인트를 필터링합니다. 필터 조건과 일치하는 데이터 포인트만 통과합니다.

구성:
- **필터 라벨**: 라벨에서 검색할 텍스트
- **대소문자 구분**: 대소문자를 정확히 일치시킵니다(기본값: false)
- **완전 일치**: 부분 문자열이 아닌 정확한 일치를 요구합니다(기본값: false)
- **반전**: 일치하지 않는 데이터 포인트를 대신 유지합니다(기본값: false)
    	]],
    	["ky"] = [[
Маалымат чекиттерин энбелги талаасы боюнча чыпкалайт. Чыпка критерийлерине туура келген маалымат чекиттери гана өткөрүлөт.

Тууралоо:
- **Чыпка энбелгиси**: Энбелгилерден изделе турган текст
- **Тамга регистрине сезгич**: Регистрди так дал келтирүү (демейки: false)
- **Так дал келтирүү**: Бөлүк саптын ордуна так дал келүүнү талап кылуу (демейки: false)
- **Тескери**: Анын ордуна дал келбеген маалымат чекиттерин калтыруу (демейки: false)
    	]],
    	["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນຕາມຊ່ອງປ້າຍກຳກັບ. ສະເພາະຈຸດຂໍ້ມູນທີ່ກົງຕາມເງື່ອນໄຂການກັ່ນຕອງເທົ່ານັ້ນຈຶ່ງຈະຜ່ານ.

ການຕັ້ງຄ່າ:
- **ປ້າຍກຳກັບສຳລັບກັ່ນຕອງ**: ຂໍ້ຄວາມທີ່ຈະຄົ້ນຫາໃນປ້າຍກຳກັບ
- **ແຍກຕົວພິມ**: ຈັບຄູ່ຕົວພິມໃຫ້ກົງກັນ (ຄ່າເລີ່ມຕົ້ນ: false)
- **ກົງກັນທຸກຕົວ**: ຕ້ອງການການກົງກັນທັງໝົດແທນການກົງບາງສ່ວນ (ຄ່າເລີ່ມຕົ້ນ: false)
- **ກັບດ້ານ**: ເກັບຈຸດຂໍ້ມູນທີ່ບໍ່ກົງກັນແທນ (ຄ່າເລີ່ມຕົ້ນ: false)
    	]],
    	["lv"] = [[
Filtrē datu punktus pēc to etiķetes lauka. Tiks nodoti tikai filtra kritērijiem atbilstošie datu punkti.

Konfigurācija:
- **Filtrēšanas etiķete**: Etiķetēs meklējamais teksts
- **Reģistrjutīgs**: Precīzi atbilst reģistram (noklusējums: false)
- **Precīza atbilstība**: Nepieciešama precīza atbilstība, nevis apakšvirknes atbilstība (noklusējums: false)
- **Apgriezt**: Tā vietā patur datu punktus, kas NEATBILST (noklusējums: false)
    	]],
    	["lt"] = [[
Filtruoja duomenų taškus pagal jų etiketės lauką. Toliau perduodami tik filtravimo kriterijus atitinkantys duomenų taškai.

Konfigūracija:
- **Filtravimo etiketė**: Etiketėse ieškomas tekstas
- **Skirti didžiąsias ir mažąsias raides**: Tiksliai atitikti raidžių dydį (numatyta: false)
- **Tikslus atitikmuo**: Reikalauti tikslaus atitikmens, o ne poeilutės (numatyta: false)
- **Invertuoti**: Vietoj to palikti neatitinkančius duomenų taškus (numatyta: false)
    	]],
    	["mk"] = [[
Ги филтрира точките на податоци според нивното поле за ознака. Само точките на податоци што ги исполнуваат критериумите на филтерот ќе поминат.

Конфигурација:
- **Ознака за филтрирање**: Текстот што се бара во ознаките
- **Разликувај големи и мали букви**: Точно совпаѓање на големината на буквите (стандардно: false)
- **Точно совпаѓање**: Барај точно совпаѓање наместо подниза (стандардно: false)
- **Инвертирај**: Наместо тоа, задржи ги точките на податоци што НЕ се совпаѓаат (стандардно: false)
    	]],
    	["ms"] = [[
Menapis titik data berdasarkan medan labelnya. Hanya titik data yang sepadan dengan kriteria penapis akan diteruskan.

Konfigurasi:
- **Label Penapis**: Teks untuk dicari dalam label
- **Sensitif Huruf Besar/Kecil**: Padankan huruf besar dan kecil dengan tepat (lalai: false)
- **Padan Tepat**: Memerlukan padanan tepat dan bukannya padanan separa (lalai: false)
- **Songsangkan**: Sebaliknya kekalkan titik data yang TIDAK sepadan (lalai: false)
    	]],
    	["ml"] = [[
ഡാറ്റാ പോയിന്റുകളുടെ label ഫീൽഡ് പ്രകാരം ഫിൽട്ടർ ചെയ്യുന്നു. ഫിൽട്ടർ മാനദണ്ഡങ്ങളുമായി പൊരുത്തപ്പെടുന്ന ഡാറ്റാ പോയിന്റുകൾ മാത്രമേ കടന്നുപോകൂ.

കോൺഫിഗറേഷൻ:
- **Filter Label**: ലേബലുകളിൽ തിരയേണ്ട ടെക്സ്റ്റ്
- **Case Sensitive**: വലിയക്ഷരം/ചെറിയക്ഷരം കൃത്യമായി പൊരുത്തപ്പെടുത്തുക (സ്ഥിരസ്ഥിതി: false)
- **Match Exactly**: substring-ന് പകരം കൃത്യമായ പൊരുത്തം ആവശ്യപ്പെടുക (സ്ഥിരസ്ഥിതി: false)
- **Invert**: പൊരുത്തപ്പെടാത്ത ഡാറ്റാ പോയിന്റുകൾ പകരം നിലനിർത്തുക (സ്ഥിരസ്ഥിതി: false)
    	]],
    	["mr"] = [[
डेटा पॉइंट्स त्यांच्या लेबल फील्डनुसार फिल्टर करतो. फिल्टर निकषांशी जुळणारे डेटा पॉइंट्सच पुढे जातील.

कॉन्फिगरेशन:
- **फिल्टर लेबल**: लेबलमध्ये शोधायचा मजकूर
- **अक्षरप्रकार संवेदनशील**: अक्षरप्रकार तंतोतंत जुळवा (डीफॉल्ट: false)
- **तंतोतंत जुळवा**: सबस्ट्रिंगऐवजी तंतोतंत जुळणी आवश्यक (डीफॉल्ट: false)
- **उलट करा**: त्याऐवजी जुळत नसलेले डेटा पॉइंट्स ठेवा (डीफॉल्ट: false)
    	]],
    	["mn"] = [[
Өгөгдлийн цэгүүдийг шошгоны талбараар нь шүүнэ. Шүүлтүүрийн шалгуурт нийцсэн өгөгдлийн цэгүүд л нэвтэрнэ.

Тохиргоо:
- **Шүүлтүүрийн шошго**: Шошгоноос хайх текст
- **Том жижиг үсэг ялгах**: Үсгийн хэлбэрийг яг тааруулах (анхдагч: false)
- **Яг тааруулах**: Дэд мөр бус, яг таарсан утгыг шаардах (анхдагч: false)
- **Урвуулах**: Харин таарахгүй өгөгдлийн цэгүүдийг үлдээх (анхдагч: false)
    	]],
    	["ne"] = [[
डेटा बिन्दुहरूलाई तिनको लेबल फिल्डअनुसार फिल्टर गर्छ। फिल्टर मापदण्डसँग मिल्ने डेटा बिन्दुहरू मात्र अघि पठाइन्छन्।

कन्फिगरेसन:
- **फिल्टर लेबल**: लेबलमा खोज्ने पाठ
- **अक्षरको आकार मिलाउने**: अक्षरको आकार ठ्याक्कै मिलाउने (पूर्वनिर्धारित: false)
- **ठ्याक्कै मिलाउने**: सबस्ट्रिङको सट्टा ठ्याक्कै मिलान आवश्यक पर्ने (पूर्वनिर्धारित: false)
- **उल्ट्याउने**: नमिल्ने डेटा बिन्दुहरू राख्ने (पूर्वनिर्धारित: false)
    	]],
    	["no"] = [[
Filtrerer datapunkter etter etikettfeltet. Bare datapunkter som samsvarer med filterkriteriene, slipper gjennom.

Konfigurasjon:
- **Filteretikett**: Teksten det skal søkes etter i etiketter
- **Skill mellom store og små bokstaver**: Samsvar med nøyaktig bokstavkombinasjon (standard: false)
- **Nøyaktig samsvar**: Krev nøyaktig samsvar i stedet for delstreng (standard: false)
- **Inverter**: Behold datapunkter som IKKE samsvarer i stedet (standard: false)
    	]],
    	["pl"] = [[
Filtruje punkty danych według pola etykiety. Przechodzą tylko punkty danych spełniające kryteria filtra.

Konfiguracja:
- **Etykieta filtra**: Tekst wyszukiwany w etykietach
- **Uwzględniaj wielkość liter**: Dopasowuj dokładnie wielkość liter (domyślnie: false)
- **Dopasuj dokładnie**: Wymagaj dokładnego dopasowania zamiast dopasowania fragmentu (domyślnie: false)
- **Odwróć**: Zamiast tego zachowaj punkty danych, które NIE pasują (domyślnie: false)
    	]],
    	["pt"] = [[
Filtra os pontos de dados pelo campo de rótulo. Apenas os pontos de dados que correspondem aos critérios de filtro passam.

Configuração:
- **Rótulo do filtro**: O texto a procurar nos rótulos
- **Sensível a maiúsculas e minúsculas**: Corresponder exatamente às maiúsculas e minúsculas (predefinição: false)
- **Correspondência exata**: Exigir correspondência exata em vez de uma parte do texto (predefinição: false)
- **Inverter**: Manter os pontos de dados que NÃO correspondem (predefinição: false)
    	]],
    	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਨੂੰ ਉਨ੍ਹਾਂ ਦੇ ਲੇਬਲ ਖੇਤਰ ਅਨੁਸਾਰ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਸਿਰਫ਼ ਫਿਲਟਰ ਮਾਪਦੰਡ ਨਾਲ ਮੇਲ ਖਾਂਦੇ ਡਾਟਾ ਪੁਆਇੰਟ ਅੱਗੇ ਜਾਣਗੇ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਫਿਲਟਰ ਲੇਬਲ**: ਲੇਬਲਾਂ ਵਿੱਚ ਖੋਜਣ ਲਈ ਲਿਖਤ
- **ਅੱਖਰ-ਆਕਾਰ ਸੰਵੇਦਨਸ਼ੀਲ**: ਅੱਖਰ-ਆਕਾਰ ਦਾ ਬਿਲਕੁਲ ਸਹੀ ਮਿਲਾਪ (ਡਿਫਾਲਟ: false)
- **ਬਿਲਕੁਲ ਮੇਲ**: ਸਬਸਟਰਿੰਗ ਦੀ ਬਜਾਏ ਪੂਰਾ ਮਿਲਾਪ ਲਾਜ਼ਮੀ ਕਰੋ (ਡਿਫਾਲਟ: false)
- **ਉਲਟਾਓ**: ਇਸ ਦੀ ਬਜਾਏ ਉਹ ਡਾਟਾ ਪੁਆਇੰਟ ਰੱਖੋ ਜੋ ਮੇਲ ਨਹੀਂ ਖਾਂਦੇ (ਡਿਫਾਲਟ: false)
    	]],
    	["ro"] = [[
Filtrează punctele de date după câmpul etichetă. Trec mai departe doar punctele de date care corespund criteriilor de filtrare.

Configurare:
- **Etichetă de filtrare**: Textul căutat în etichete
- **Sensibil la majuscule**: Potrivește exact literele mari și mici (implicit: false)
- **Potrivire exactă**: Necesită o potrivire exactă în locul unei subsecvențe (implicit: false)
- **Inversare**: Păstrează în schimb punctele de date care NU corespund (implicit: false)
    	]],
    	["rm"] = [[
Filtra ils puncts da datas tenor lur champ d’etichetta. Mo ils puncts da datas che correspundan als criteris da filtraziun vegnan transmess.

Configuraziun:
- **Etichetta da filtrar**: Il text da tschertgar en las etichettas
- **Sensibel a maiusclas e minusclas**: Cumparegliar exactamain la grafia (default: false)
- **Correspundenza exacta**: Pretender ina correspundenza exacta empè d’ina part da text (default: false)
- **Inverter**: Mantegna enstagl ils puncts da datas che NA correspundan (default: false)
    	]],
    	["ru"] = [[
Фильтрует точки данных по полю метки. Пропускаются только точки данных, соответствующие критериям фильтра.

Конфигурация:
- **Метка фильтра**: Текст для поиска в метках
- **С учётом регистра**: Точное совпадение регистра (по умолчанию: false)
- **Точное совпадение**: Требовать точное совпадение, а не совпадение подстроки (по умолчанию: false)
- **Инвертировать**: Оставлять точки данных, которые НЕ соответствуют критериям (по умолчанию: false)
    	]],
    	["sr"] = [[
Filtrira tačke podataka prema njihovom polju oznake. Prolaze samo tačke podataka koje odgovaraju kriterijumima filtera.

Konfiguracija:
- **Oznaka filtera**: Tekst za pretragu u oznakama
- **Razlikuj velika i mala slova**: Tačno poređenje veličine slova (podrazumevano: false)
- **Potpuno podudaranje**: Zahteva potpuno podudaranje umesto podniza (podrazumevano: false)
- **Obrni**: Umesto toga zadržava tačke podataka koje se NE podudaraju (podrazumevano: false)
    	]],
    	["si"] = [[
දත්ත ලක්ෂ්‍ය ඒවායේ ලේබල් ක්ෂේත්‍රය අනුව පෙරහන් කරයි. පෙරහන් නිර්ණායකයට ගැළපෙන දත්ත ලක්ෂ්‍ය පමණක් ඉදිරියට යවයි.

වින්‍යාසය:
- **පෙරහන් ලේබලය**: ලේබල්වල සෙවිය යුතු පෙළ
- **අකුරු විශාලත්වය සංවේදී**: අකුරු විශාලත්වය හරියටම ගැළපීම (පෙරනිමිය: false)
- **හරියටම ගැළපීම**: උපපෙළක් වෙනුවට සම්පූර්ණ ගැළපීමක් අවශ්‍ය කිරීම (පෙරනිමිය: false)
- **ප්‍රතිවර්තනය**: ඒ වෙනුවට නොගැළපෙන දත්ත ලක්ෂ්‍ය තබා ගැනීම (පෙරනිමිය: false)
    	]],
    	["sk"] = [[
Filtruje údajové body podľa ich poľa označenia. Prejdú iba údajové body zodpovedajúce kritériám filtra.

Konfigurácia:
- **Označenie filtra**: Text, ktorý sa má vyhľadať v označeniach
- **Rozlišovať veľké a malé písmená**: Presne rozlišovať veľkosť písmen (predvolené: false)
- **Presná zhoda**: Vyžadovať presnú zhodu namiesto podreťazca (predvolené: false)
- **Invertovať**: Namiesto toho ponechať údajové body, ktoré sa NEZHODUJÚ (predvolené: false)
    	]],
    	["sl"] = [[
Filtrira podatkovne točke glede na njihovo polje oznake. Prepustijo se le podatkovne točke, ki ustrezajo merilom filtra.

Konfiguracija:
- **Oznaka filtra**: Besedilo za iskanje v oznakah
- **Razlikuj velike in male črke**: Natančno ujemanje velikosti črk (privzeto: false)
- **Natančno ujemanje**: Zahtevaj popolno ujemanje namesto podniza (privzeto: false)
- **Obrni**: Namesto tega obdrži podatkovne točke, ki se NE ujemajo (privzeto: false)
    	]],
    	["es"] = [[
Filtra los puntos de datos por su campo de etiqueta. Solo pasan los puntos de datos que coinciden con los criterios del filtro.

Configuración:
- **Etiqueta del filtro**: El texto que se buscará en las etiquetas
- **Distinguir mayúsculas y minúsculas**: Coincidir exactamente el uso de mayúsculas y minúsculas (predeterminado: false)
- **Coincidencia exacta**: Requerir una coincidencia exacta en lugar de una subcadena (predeterminado: false)
- **Invertir**: Conservar los puntos de datos que NO coincidan (predeterminado: false)
    	]],
    	["sw"] = [[
Huchuja nukta za data kulingana na sehemu yake ya lebo. Ni nukta za data zinazolingana na vigezo vya kichujio pekee ndizo zitapitishwa.

Usanidi:
- **Lebo ya Kichujio**: Maandishi ya kutafuta kwenye lebo
- **Herufi Kubwa/ Ndogo Muhimu**: Linganisha ukubwa wa herufi sawasawa (chaguo-msingi: false)
- **Linganisha Sawasawa**: Hitaji ulinganifu kamili badala ya sehemu ya maandishi (chaguo-msingi: false)
- **Geuza**: Hifadhi badala yake nukta za data zisizolingana (chaguo-msingi: false)
    	]],
    	["sv"] = [[
Filtrerar datapunkter efter deras etikettfält. Endast datapunkter som matchar filterkriterierna släpps igenom.

Konfiguration:
- **Filteretikett**: Texten som ska sökas efter i etiketter
- **Skiftlägeskänslig**: Matcha exakt skiftläge (standard: false)
- **Exakt matchning**: Kräv exakt matchning i stället för delsträng (standard: false)
- **Invertera**: Behåll i stället datapunkter som INTE matchar (standard: false)
    	]],
    	["ta"] = [[
தரவுப் புள்ளிகளின் லேபிள் புலத்தின் அடிப்படையில் வடிகட்டுகிறது. வடிகட்டி நிபந்தனைகளுடன் பொருந்தும் தரவுப் புள்ளிகள் மட்டுமே தொடரும்.

உள்ளமைவு:
- **வடிகட்டி லேபிள்**: லேபிள்களில் தேட வேண்டிய உரை
- **எழுத்து வடிவம் பொருந்துதல்**: எழுத்து வடிவத்தைத் துல்லியமாகப் பொருத்து (இயல்புநிலை: false)
- **துல்லியமாகப் பொருத்து**: துணைச்சரத்திற்கு பதிலாக துல்லியமான பொருத்தம் தேவை (இயல்புநிலை: false)
- **தலைகீழாக்கு**: பொருந்தாத தரவுப் புள்ளிகளை வைத்திரு (இயல்புநிலை: false)
    	]],
    	["te"] = [[
డేటా పాయింట్ల లేబుల్ ఫీల్డ్ ద్వారా ఫిల్టర్ చేస్తుంది. ఫిల్టర్ ప్రమాణాలకు సరిపడే డేటా పాయింట్లు మాత్రమే కొనసాగుతాయి.

కాన్ఫిగరేషన్:
- **ఫిల్టర్ లేబుల్**: లేబుళ్లలో శోధించాల్సిన వచనం
- **కేస్ సెన్సిటివ్**: అక్షరాల కేస్‌ను ఖచ్చితంగా సరిపోల్చండి (డిఫాల్ట్: false)
- **ఖచ్చితంగా సరిపోల్చు**: సబ్‌స్ట్రింగ్‌కు బదులుగా ఖచ్చితమైన సరిపోలిక అవసరం (డిఫాల్ట్: false)
- **విలోమం**: సరిపోని డేటా పాయింట్లను ఉంచండి (డిఫాల్ట్: false)
    	]],
    	["th"] = [[
กรองจุดข้อมูลตามฟิลด์ป้ายกำกับ เฉพาะจุดข้อมูลที่ตรงตามเกณฑ์การกรองเท่านั้นที่จะถูกส่งต่อ

การกำหนดค่า:
- **ป้ายกำกับสำหรับกรอง**: ข้อความที่ต้องการค้นหาในป้ายกำกับ
- **คำนึงถึงตัวพิมพ์เล็กใหญ่**: จับคู่ตัวพิมพ์ให้ตรงกัน (ค่าเริ่มต้น: false)
- **ตรงกันทุกประการ**: ต้องตรงกันทุกประการแทนการจับคู่บางส่วน (ค่าเริ่มต้น: false)
- **กลับด้าน**: เก็บจุดข้อมูลที่ไม่ตรงกันแทน (ค่าเริ่มต้น: false)
    	]],
    	["tr"] = [[
Veri noktalarını etiket alanlarına göre filtreler. Yalnızca filtre ölçütleriyle eşleşen veri noktaları geçer.

Yapılandırma:
- **Filtre Etiketi**: Etiketlerde aranacak metin
- **Büyük/Küçük Harfe Duyarlı**: Büyük/küçük harfleri tam olarak eşleştir (varsayılan: false)
- **Tam Eşleştir**: Alt dize yerine tam eşleşme gerektir (varsayılan: false)
- **Tersine Çevir**: Bunun yerine eşleşmeyen veri noktalarını tut (varsayılan: false)
    	]],
    	["uk"] = [[
Фільтрує точки даних за полем мітки. Пропускаються лише точки даних, що відповідають критеріям фільтра.

Конфігурація:
- **Мітка фільтра**: Текст для пошуку в мітках
- **З урахуванням регістру**: Точний збіг регістру (типово: false)
- **Точний збіг**: Вимагати точного збігу замість збігу підрядка (типово: false)
- **Інвертувати**: Залишати натомість точки даних, що НЕ відповідають (типово: false)
    	]],
    	["vi"] = [[
Lọc các điểm dữ liệu theo trường nhãn. Chỉ những điểm dữ liệu khớp với tiêu chí lọc mới được giữ lại.

Cấu hình:
- **Nhãn lọc**: Văn bản cần tìm trong các nhãn
- **Phân biệt chữ hoa chữ thường**: Khớp chính xác kiểu chữ (mặc định: false)
- **Khớp chính xác**: Yêu cầu khớp chính xác thay vì khớp chuỗi con (mặc định: false)
- **Đảo ngược**: Thay vào đó, giữ lại các điểm dữ liệu KHÔNG khớp (mặc định: false)
    	]],
    },
    config = {
        text {
            id = "filter_label",
            name = {
            	["en"] = "Filter Label",
            	["af"] = "Filteretiket",
            	["sq"] = "Etiketa e filtrit",
            	["am"] = "የማጣሪያ መለያ",
            	["hy"] = "Զտման պիտակ",
            	["az"] = "Süzgəc etiketi",
            	["bn"] = "ফিল্টার লেবেল",
            	["eu"] = "Iragazki-etiketa",
            	["be"] = "Ярлык фільтра",
            	["bg"] = "Етикет за филтриране",
            	["my"] = "စစ်ထုတ်မည့် အညွှန်း",
            	["ca"] = "Etiqueta de filtratge",
            	["zh-Hans"] = "筛选标签",
            	["zh-Hant"] = "篩選標籤",
            	["hr"] = "Oznaka filtra",
            	["cs"] = "Filtrovaný štítek",
            	["da"] = "Filteretiket",
            	["nl"] = "Filterlabel",
            	["et"] = "Filtri silt",
            	["fil"] = "Filter Label",
            	["fi"] = "Suodatettava selite",
            	["fr"] = "Libellé du filtre",
            	["gl"] = "Etiqueta do filtro",
            	["ka"] = "გასაფილტრი იარლიყი",
            	["de"] = "Filterlabel",
            	["el"] = "Ετικέτα φίλτρου",
            	["gu"] = "ફિલ્ટર લેબલ",
            	["hi"] = "फ़िल्टर लेबल",
            	["hu"] = "Szűrőcímke",
            	["is"] = "Síumerki",
            	["id"] = "Label Saringan",
            	["it"] = "Etichetta del filtro",
            	["ja"] = "フィルタラベル",
            	["kn"] = "ಫಿಲ್ಟರ್ ಲೇಬಲ್",
            	["kk"] = "Сүзгі жапсырмасы",
            	["km"] = "ស្លាកតម្រង",
            	["ko"] = "필터 라벨",
            	["ky"] = "Чыпка энбелгиси",
            	["lo"] = "ປ້າຍກຳກັບສຳລັບກັ່ນຕອງ",
            	["lv"] = "Filtrēšanas etiķete",
            	["lt"] = "Filtravimo etiketė",
            	["mk"] = "Ознака за филтрирање",
            	["ms"] = "Label Penapis",
            	["ml"] = "ഫിൽട്ടർ ലേബൽ",
            	["mr"] = "फिल्टर लेबल",
            	["mn"] = "Шүүлтүүрийн шошго",
            	["ne"] = "फिल्टर लेबल",
            	["no"] = "Filteretikett",
            	["pl"] = "Etykieta filtra",
            	["pt"] = "Rótulo do filtro",
            	["pa"] = "ਫਿਲਟਰ ਲੇਬਲ",
            	["ro"] = "Etichetă de filtrare",
            	["rm"] = "Etichetta da filtrar",
            	["ru"] = "Метка фильтра",
            	["sr"] = "Oznaka filtera",
            	["si"] = "පෙරහන් ලේබලය",
            	["sk"] = "Označenie filtra",
            	["sl"] = "Oznaka filtra",
            	["es"] = "Etiqueta del filtro",
            	["sw"] = "Lebo ya Kichujio",
            	["sv"] = "Filteretikett",
            	["ta"] = "வடிகட்டி லேபிள்",
            	["te"] = "ఫిల్టర్ లేబుల్",
            	["th"] = "ป้ายกำกับสำหรับกรอง",
            	["tr"] = "Filtre Etiketi",
            	["uk"] = "Мітка фільтра",
            	["vi"] = "Nhãn lọc",
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
        local filter_label = config and config.filter_label
        local case_sensitive = config and config.case_sensitive or false
        local match_exactly = config and config.match_exactly or false
        local invert = config and config.invert or false

        return function()
            local data_point = source.dp()
            local should_match = not invert
            while data_point and (match(data_point, filter_label, case_sensitive, match_exactly) ~= should_match) do
                data_point = source.dp()
            end
            return data_point
        end
    end
}
