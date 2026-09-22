-- Lua Function to override the label of all data points with a configurable string
-- This function sets all incoming data point labels to a specified value

local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "override-label",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_transform"},
    title = {
    	["en"] = "Override Label",
    	["af"] = "Oorskryf Etiket",
    	["sq"] = "Anashkalo etiketën",
    	["am"] = "መለያን ተካ",
    	["hy"] = "Փոխարինել պիտակը",
    	["az"] = "Etiketi əvəz et",
    	["bn"] = "লেবেল ওভাররাইড করুন",
    	["eu"] = "Gainidatzi etiketa",
    	["be"] = "Замяніць ярлык",
    	["bg"] = "Замяна на етикета",
    	["my"] = "အညွှန်းကို အစားထိုးရန်",
    	["ca"] = "Substitueix l’etiqueta",
    	["zh-Hans"] = "覆盖标签",
    	["zh-Hant"] = "覆寫標籤",
    	["hr"] = "Nadjačaj oznaku",
    	["cs"] = "Přepsat štítek",
    	["da"] = "Tilsidesæt etiket",
    	["nl"] = "Label overschrijven",
    	["et"] = "Asenda silt",
    	["fil"] = "I-override ang Label",
    	["fi"] = "Ohita selite",
    	["fr"] = "Remplacer le libellé",
    	["gl"] = "Substituír etiqueta",
    	["ka"] = "იარლიყის ჩანაცვლება",
    	["de"] = "Label überschreiben",
    	["el"] = "Παράκαμψη ετικέτας",
    	["gu"] = "લેબલ ઓવરરાઇડ કરો",
    	["hi"] = "लेबल ओवरराइड करें",
    	["hu"] = "Címke felülírása",
    	["is"] = "Yfirskrifa merki",
    	["id"] = "Ganti Label",
    	["it"] = "Sovrascrivi etichetta",
    	["ja"] = "ラベルを上書き",
    	["kn"] = "ಲೇಬಲ್ ಅತಿಕ್ರಮಿಸಿ",
    	["kk"] = "Жапсырманы қайта анықтау",
    	["km"] = "បដិសេធស្លាក",
    	["ko"] = "라벨 덮어쓰기",
    	["ky"] = "Энбелгини алмаштыруу",
    	["lo"] = "ລົບລ້າງປ້າຍກຳກັບ",
    	["lv"] = "Aizstāt etiķeti",
    	["lt"] = "Pakeisti etiketę",
    	["mk"] = "Замени ја ознаката",
    	["ms"] = "Gantikan Label",
    	["ml"] = "ലേബൽ അസാധുവാക്കുക",
    	["mr"] = "लेबल ओव्हरराइड करा",
    	["mn"] = "Шошгыг дарж тохируулах",
    	["ne"] = "लेबल ओभरराइड गर्नुहोस्",
    	["no"] = "Overstyr etikett",
    	["pl"] = "Zastąp etykietę",
    	["pt"] = "Substituir rótulo",
    	["pa"] = "ਲੇਬਲ ਓਵਰਰਾਈਡ ਕਰੋ",
    	["ro"] = "Înlocuiește eticheta",
    	["rm"] = "Surscriver etichetta",
    	["ru"] = "Заменить метку",
    	["sr"] = "Zameni oznaku",
    	["si"] = "ලේබලය අභිබවා යොදන්න",
    	["sk"] = "Prepísať označenie",
    	["sl"] = "Prepiši oznako",
    	["es"] = "Reemplazar etiqueta",
    	["sw"] = "Batilisha Lebo",
    	["sv"] = "Åsidosätt etikett",
    	["ta"] = "லேபிளை மேலெழுது",
    	["te"] = "లేబుల్‌ను భర్తీ చేయి",
    	["th"] = "แทนที่ป้ายกำกับ",
    	["tr"] = "Etiketi Geçersiz Kıl",
    	["uk"] = "Замінити мітку",
    	["vi"] = "Ghi đè nhãn",
    },
    description = {
    	["en"] = [[
Sets all incoming data point labels to a specified value
    	]],
    	["af"] = [[
Stel alle inkomende datapuntetikette op ’n gespesifiseerde waarde
    	]],
    	["sq"] = [[
Vendos të gjitha etiketat hyrëse të pikave të të dhënave në një vlerë të specifikuar
    	]],
    	["am"] = [[
ሁሉንም የሚገቡ የውሂብ ነጥብ መለያዎችን በተገለጸ እሴት ያዘጋጃል።
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի պիտակները սահմանում է նշված արժեքով
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin etiketlərini göstərilən qiymətə təyin edir
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের লেবেলকে নির্দিষ্ট মানে সেট করে
    	]],
    	["eu"] = [[
Sarrerako datu-puntu guztien etiketak zehaztutako balio batera ezartzen ditu
    	]],
    	["be"] = [[
Задае ўсім уваходным ярлыкам кропак даных пазначанае значэнне
    	]],
    	["bg"] = [[
Задава всички входящи етикети на точките от данни на определена стойност
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်အညွှန်းများအားလုံးကို သတ်မှတ်ထားသော တန်ဖိုးသို့ သတ်မှတ်သည်
    	]],
    	["ca"] = [[
Estableix totes les etiquetes dels punts de dades entrants en un valor especificat
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的标签设置为指定值
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的標籤設為指定值
    	]],
    	["hr"] = [[
Postavlja sve dolazne oznake podatkovnih točaka na navedenu vrijednost
    	]],
    	["cs"] = [[
Nastaví všechny příchozí štítky datových bodů na zadanou hodnotu
    	]],
    	["da"] = [[
Sætter alle indgående datapunkters etiketter til en angivet værdi
    	]],
    	["nl"] = [[
Stelt alle binnenkomende datapuntenlabels in op een opgegeven waarde
    	]],
    	["et"] = [[
Määrab kõikidele sisendandmepunktide siltidele määratud väärtuse
    	]],
    	["fil"] = [[
Itinatakda ang lahat ng papasok na label ng data point sa tinukoy na halaga
    	]],
    	["fi"] = [[
Asettaa kaikkien saapuvien datapisteiden selitteeksi määritetyn arvon
    	]],
    	["fr"] = [[
Définit tous les libellés des points de données entrants sur une valeur indiquée
    	]],
    	["gl"] = [[
Establece todas as etiquetas dos puntos de datos recibidos no valor especificado
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის იარლიყს მითითებული მნიშვნელობით ანაცვლებს
    	]],
    	["de"] = [[
Setzt alle eingehenden Datenpunktlabels auf einen angegebenen Wert
    	]],
    	["el"] = [[
Ορίζει όλες τις εισερχόμενες ετικέτες σημείων δεδομένων σε μια καθορισμένη τιμή
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના લેબલ્સને નિર્દિષ્ટ મૂલ્ય પર સેટ કરે છે
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के लेबल को निर्दिष्ट मान पर सेट करता है
    	]],
    	["hu"] = [[
Az összes beérkező adatpont címkéjét a megadott értékre állítja
    	]],
    	["is"] = [[
Stillir öll merki innkomandi gagnapunkta á tilgreint gildi
    	]],
    	["id"] = [[
Mengatur semua label titik data yang masuk ke nilai yang ditentukan
    	]],
    	["it"] = [[
Imposta tutte le etichette dei punti dati in arrivo su un valore specificato
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントのラベルを指定した値に設定します
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಲೇಬಲ್‌ಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ಮೌಲ್ಯಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің жапсырмаларын көрсетілген мәнге орнатады
    	]],
    	["km"] = [[
កំណត់ស្លាករបស់ចំណុចទិន្នន័យចូលទាំងអស់ទៅជាតម្លៃដែលបានបញ្ជាក់
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 라벨을 지정한 값으로 설정합니다
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин энбелгилерин көрсөтүлгөн мааниге өзгөртөт
    	]],
    	["lo"] = [[
ກຳນົດປ້າຍກຳກັບຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດເປັນຄ່າທີ່ກຳນົດ
    	]],
    	["lv"] = [[
Iestata visām ienākošo datu punktu etiķetēm norādīto vērtību
    	]],
    	["lt"] = [[
Nustato visų gaunamų duomenų taškų etiketes į nurodytą reikšmę
    	]],
    	["mk"] = [[
Ги поставува сите влезни ознаки на точките на податоци на зададена вредност
    	]],
    	["ms"] = [[
Menetapkan semua label titik data yang masuk kepada nilai yang ditentukan
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് ലേബലുകളും നിർദ്ദിഷ്ട മൂല്യമായി സജ്ജീകരിക്കുന്നു
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सची लेबले निर्दिष्ट मूल्यावर सेट करते
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн шошгыг заасан утгаар тохируулна
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका लेबललाई निर्दिष्ट मानमा सेट गर्छ
    	]],
    	["no"] = [[
Setter etiketten til alle innkommende datapunkter til en angitt verdi
    	]],
    	["pl"] = [[
Ustawia wszystkie przychodzące etykiety punktów danych na określoną wartość
    	]],
    	["pt"] = [[
Define todos os rótulos dos pontos de dados recebidos como um valor especificado
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਲੇਬਲਾਂ ਨੂੰ ਨਿਰਧਾਰਤ ਮੁੱਲ 'ਤੇ ਸੈੱਟ ਕਰਦਾ ਹੈ
    	]],
    	["ro"] = [[
Setează toate etichetele punctelor de date primite la o valoare specificată
    	]],
    	["rm"] = [[
Definescha tut las etichettas dals puncts da datas entrants ad ina valur spezificada
    	]],
    	["ru"] = [[
Задаёт всем входящим меткам точек данных указанное значение
    	]],
    	["sr"] = [[
Postavlja sve dolazne oznake tačaka podataka na zadatu vrednost
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය ලේබල නිශ්චිත අගයකට සකසයි
    	]],
    	["sk"] = [[
Nastaví označenia všetkých prichádzajúcich údajových bodov na zadanú hodnotu
    	]],
    	["sl"] = [[
Nastavi vse vhodne oznake podatkovnih točk na določeno vrednost
    	]],
    	["es"] = [[
Establece todas las etiquetas de los puntos de datos entrantes en un valor especificado
    	]],
    	["sw"] = [[
Huweka lebo zote za nukta za data zinazoingia kuwa thamani maalum
    	]],
    	["sv"] = [[
Ställer in alla inkommande datapunkters etiketter till ett angivet värde
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி லேபிள்களையும் குறிப்பிட்ட மதிப்பாக அமைக்கிறது
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ లేబుళ్లను పేర్కొన్న విలువగా సెట్ చేస్తుంది
    	]],
    	["th"] = [[
ตั้งค่าป้ายกำกับของจุดข้อมูลขาเข้าทั้งหมดเป็นค่าที่ระบุ
    	]],
    	["tr"] = [[
Gelen tüm veri noktası etiketlerini belirtilen değere ayarlar
    	]],
    	["uk"] = [[
Встановлює для всіх вхідних міток точок даних вказане значення
    	]],
    	["vi"] = [[
Đặt nhãn của tất cả điểm dữ liệu đầu vào thành một giá trị được chỉ định
    	]],
    },
    config = {
        text {
            id = "new_label",
            name = {
            	["en"] = "New Label",
            	["af"] = "Nuwe Etiket",
            	["sq"] = "Etiketa e re",
            	["am"] = "አዲስ መለያ",
            	["hy"] = "Նոր պիտակ",
            	["az"] = "Yeni etiket",
            	["bn"] = "নতুন লেবেল",
            	["eu"] = "Etiketa berria",
            	["be"] = "Новы ярлык",
            	["bg"] = "Нов етикет",
            	["my"] = "အညွှန်းအသစ်",
            	["ca"] = "Etiqueta nova",
            	["zh-Hans"] = "新标签",
            	["zh-Hant"] = "新標籤",
            	["hr"] = "Nova oznaka",
            	["cs"] = "Nový štítek",
            	["da"] = "Ny etiket",
            	["nl"] = "Nieuw label",
            	["et"] = "Uus silt",
            	["fil"] = "Bagong Label",
            	["fi"] = "Uusi selite",
            	["fr"] = "Nouveau libellé",
            	["gl"] = "Nova etiqueta",
            	["ka"] = "ახალი იარლიყი",
            	["de"] = "Neues Label",
            	["el"] = "Νέα ετικέτα",
            	["gu"] = "નવું લેબલ",
            	["hi"] = "नया लेबल",
            	["hu"] = "Új címke",
            	["is"] = "Nýtt merki",
            	["id"] = "Label Baru",
            	["it"] = "Nuova etichetta",
            	["ja"] = "新しいラベル",
            	["kn"] = "ಹೊಸ ಲೇಬಲ್",
            	["kk"] = "Жаңа жапсырма",
            	["km"] = "ស្លាកថ្មី",
            	["ko"] = "새 라벨",
            	["ky"] = "Жаңы энбелги",
            	["lo"] = "ປ້າຍກຳກັບໃໝ່",
            	["lv"] = "Jaunā etiķete",
            	["lt"] = "Nauja etiketė",
            	["mk"] = "Нова ознака",
            	["ms"] = "Label Baharu",
            	["ml"] = "പുതിയ ലേബൽ",
            	["mr"] = "नवीन लेबल",
            	["mn"] = "Шинэ шошго",
            	["ne"] = "नयाँ लेबल",
            	["no"] = "Ny etikett",
            	["pl"] = "Nowa etykieta",
            	["pt"] = "Novo rótulo",
            	["pa"] = "ਨਵਾਂ ਲੇਬਲ",
            	["ro"] = "Etichetă nouă",
            	["rm"] = "Nova etichetta",
            	["ru"] = "Новая метка",
            	["sr"] = "Nova oznaka",
            	["si"] = "නව ලේබලය",
            	["sk"] = "Nové označenie",
            	["sl"] = "Nova oznaka",
            	["es"] = "Nueva etiqueta",
            	["sw"] = "Lebo Mpya",
            	["sv"] = "Ny etikett",
            	["ta"] = "புதிய லேபிள்",
            	["te"] = "కొత్త లేబుల్",
            	["th"] = "ป้ายกำกับใหม่",
            	["tr"] = "Yeni Etiket",
            	["uk"] = "Нова мітка",
            	["vi"] = "Nhãn mới",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_label = config and config.new_label

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_label then
                return data_point
            end
            data_point.label = new_label

            return data_point
        end
    end,
}
