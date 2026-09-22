-- Lua Function to override the value of all data points with a configurable number
-- This function sets all incoming data point values to a specified value

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "override-value",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_transform"},
    title = {
    	["en"] = "Override Value",
    	["af"] = "Oorskryf Waarde",
    	["sq"] = "Anashkalo vlerën",
    	["am"] = "እሴትን ተካ",
    	["hy"] = "Փոխարինել արժեքը",
    	["az"] = "Qiyməti əvəz et",
    	["bn"] = "মান ওভাররাইড করুন",
    	["eu"] = "Gainidatzi balioa",
    	["be"] = "Замяніць значэнне",
    	["bg"] = "Замяна на стойността",
    	["my"] = "တန်ဖိုးကို အစားထိုးရန်",
    	["ca"] = "Substitueix el valor",
    	["zh-Hans"] = "覆盖值",
    	["zh-Hant"] = "覆寫值",
    	["hr"] = "Nadjačaj vrijednost",
    	["cs"] = "Přepsat hodnotu",
    	["da"] = "Tilsidesæt værdi",
    	["nl"] = "Waarde overschrijven",
    	["et"] = "Asenda väärtus",
    	["fil"] = "I-override ang Halaga",
    	["fi"] = "Ohita arvo",
    	["fr"] = "Remplacer la valeur",
    	["gl"] = "Substituír valor",
    	["ka"] = "მნიშვნელობის ჩანაცვლება",
    	["de"] = "Wert überschreiben",
    	["el"] = "Παράκαμψη τιμής",
    	["gu"] = "મૂલ્ય ઓવરરાઇડ કરો",
    	["hi"] = "मान ओवरराइड करें",
    	["hu"] = "Érték felülírása",
    	["is"] = "Yfirskrifa gildi",
    	["id"] = "Ganti Nilai",
    	["it"] = "Sovrascrivi valore",
    	["ja"] = "値を上書き",
    	["kn"] = "ಮೌಲ್ಯ ಅತಿಕ್ರಮಿಸಿ",
    	["kk"] = "Мәнді қайта анықтау",
    	["km"] = "បដិសេធតម្លៃ",
    	["ko"] = "값 덮어쓰기",
    	["ky"] = "Маанини алмаштыруу",
    	["lo"] = "ລົບລ້າງຄ່າ",
    	["lv"] = "Aizstāt vērtību",
    	["lt"] = "Pakeisti reikšmę",
    	["mk"] = "Замени ја вредноста",
    	["ms"] = "Gantikan Nilai",
    	["ml"] = "മൂല്യം അസാധുവാക്കുക",
    	["mr"] = "मूल्य ओव्हरराइड करा",
    	["mn"] = "Утгыг дарж тохируулах",
    	["ne"] = "मान ओभरराइड गर्नुहोस्",
    	["no"] = "Overstyr verdi",
    	["pl"] = "Zastąp wartość",
    	["pt"] = "Substituir valor",
    	["pa"] = "ਮੁੱਲ ਓਵਰਰਾਈਡ ਕਰੋ",
    	["ro"] = "Înlocuiește valoarea",
    	["rm"] = "Surscriver valur",
    	["ru"] = "Заменить значение",
    	["sr"] = "Zameni vrednost",
    	["si"] = "අගය අභිබවා යොදන්න",
    	["sk"] = "Prepísať hodnotu",
    	["sl"] = "Prepiši vrednost",
    	["es"] = "Reemplazar valor",
    	["sw"] = "Batilisha Thamani",
    	["sv"] = "Åsidosätt värde",
    	["ta"] = "மதிப்பை மேலெழுது",
    	["te"] = "విలువను భర్తీ చేయి",
    	["th"] = "แทนที่ค่า",
    	["tr"] = "Değeri Geçersiz Kıl",
    	["uk"] = "Замінити значення",
    	["vi"] = "Ghi đè giá trị",
    },
    description = {
    	["en"] = [[
Sets all incoming data point values to a specified value
    	]],
    	["af"] = [[
Stel alle inkomende datapuntwaardes op ’n gespesifiseerde waarde
    	]],
    	["sq"] = [[
Vendos të gjitha vlerat hyrëse të pikave të të dhënave në një vlerë të specifikuar
    	]],
    	["am"] = [[
ሁሉንም የሚገቡ የውሂብ ነጥብ እሴቶችን በተገለጸ እሴት ያዘጋጃል።
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի արժեքները սահմանում է նշված արժեքով
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qiymətlərini göstərilən qiymətə təyin edir
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের মানকে নির্দিষ্ট মানে সেট করে
    	]],
    	["eu"] = [[
Sarrerako datu-puntu guztien balioak zehaztutako balio batera ezartzen ditu
    	]],
    	["be"] = [[
Задае ўсім уваходным значэнням кропак даных пазначанае значэнне
    	]],
    	["bg"] = [[
Задава всички входящи стойности на точките от данни на определена стойност
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်တန်ဖိုးများအားလုံးကို သတ်မှတ်ထားသော တန်ဖိုးသို့ သတ်မှတ်သည်
    	]],
    	["ca"] = [[
Estableix tots els valors dels punts de dades entrants en un valor especificat
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的值设置为指定值
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的值設為指定值
    	]],
    	["hr"] = [[
Postavlja sve dolazne vrijednosti podatkovnih točaka na navedenu vrijednost
    	]],
    	["cs"] = [[
Nastaví všechny příchozí hodnoty datových bodů na zadanou hodnotu
    	]],
    	["da"] = [[
Sætter alle indgående datapunkters værdier til en angivet værdi
    	]],
    	["nl"] = [[
Stelt alle binnenkomende waarden van datapunten in op een opgegeven waarde
    	]],
    	["et"] = [[
Määrab kõikidele sisendandmepunktide väärtustele määratud väärtuse
    	]],
    	["fil"] = [[
Itinatakda ang lahat ng papasok na halaga ng data point sa tinukoy na halaga
    	]],
    	["fi"] = [[
Asettaa kaikkien saapuvien datapisteiden arvoksi määritetyn arvon
    	]],
    	["fr"] = [[
Définit toutes les valeurs des points de données entrants sur une valeur indiquée
    	]],
    	["gl"] = [[
Establece todos os valores dos puntos de datos recibidos no valor especificado
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის მნიშვნელობას მითითებული მნიშვნელობით ანაცვლებს
    	]],
    	["de"] = [[
Setzt alle eingehenden Datenpunktwerte auf einen angegebenen Wert
    	]],
    	["el"] = [[
Ορίζει όλες τις εισερχόμενες τιμές σημείων δεδομένων σε μια καθορισμένη τιμή
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના મૂલ્યોને નિર્દિષ્ટ મૂલ્ય પર સેટ કરે છે
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के मान को निर्दिष्ट मान पर सेट करता है
    	]],
    	["hu"] = [[
Az összes beérkező adatpont értékét a megadott értékre állítja
    	]],
    	["is"] = [[
Stillir öll gildi innkomandi gagnapunkta á tilgreint gildi
    	]],
    	["id"] = [[
Mengatur semua nilai titik data yang masuk ke nilai yang ditentukan
    	]],
    	["it"] = [[
Imposta tutti i valori dei punti dati in arrivo su un valore specificato
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントの値を指定した値に設定します
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ಮೌಲ್ಯಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің мәндерін көрсетілген мәнге орнатады
    	]],
    	["km"] = [[
កំណត់តម្លៃរបស់ចំណុចទិន្នន័យចូលទាំងអស់ទៅជាតម្លៃដែលបានបញ្ជាក់
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 값을 지정한 값으로 설정합니다
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин маанилерин көрсөтүлгөн мааниге өзгөртөт
    	]],
    	["lo"] = [[
ກຳນົດຄ່າຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດເປັນຄ່າທີ່ກຳນົດ
    	]],
    	["lv"] = [[
Iestata visām ienākošo datu punktu vērtībām norādīto vērtību
    	]],
    	["lt"] = [[
Nustato visų gaunamų duomenų taškų reikšmes į nurodytą reikšmę
    	]],
    	["mk"] = [[
Ги поставува сите влезни вредности на точките на податоци на зададена вредност
    	]],
    	["ms"] = [[
Menetapkan semua nilai titik data yang masuk kepada nilai yang ditentukan
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളും നിർദ്ദിഷ്ട മൂല്യമായി സജ്ജീകരിക്കുന്നു
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सची मूल्ये निर्दिष्ट मूल्यावर सेट करते
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн утгыг заасан утгаар тохируулна
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका मानलाई निर्दिष्ट मानमा सेट गर्छ
    	]],
    	["no"] = [[
Setter verdien til alle innkommende datapunkter til en angitt verdi
    	]],
    	["pl"] = [[
Ustawia wszystkie przychodzące wartości punktów danych na określoną wartość
    	]],
    	["pt"] = [[
Define todos os valores dos pontos de dados recebidos como um valor especificado
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਮੁੱਲਾਂ ਨੂੰ ਨਿਰਧਾਰਤ ਮੁੱਲ 'ਤੇ ਸੈੱਟ ਕਰਦਾ ਹੈ
    	]],
    	["ro"] = [[
Setează toate valorile punctelor de date primite la o valoare specificată
    	]],
    	["rm"] = [[
Definescha tut las valurs dals puncts da datas entrants ad ina valur spezificada
    	]],
    	["ru"] = [[
Задаёт всем входящим значениям точек данных указанное значение
    	]],
    	["sr"] = [[
Postavlja sve dolazne vrednosti tačaka podataka na zadatu vrednost
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය අගයන් නිශ්චිත අගයකට සකසයි
    	]],
    	["sk"] = [[
Nastaví hodnoty všetkých prichádzajúcich údajových bodov na zadanú hodnotu
    	]],
    	["sl"] = [[
Nastavi vse vhodne vrednosti podatkovnih točk na določeno vrednost
    	]],
    	["es"] = [[
Establece todos los valores de los puntos de datos entrantes en un valor especificado
    	]],
    	["sw"] = [[
Huunda thamani zote za nukta za data zinazoingia kuwa thamani maalum
    	]],
    	["sv"] = [[
Ställer in alla inkommande datapunkters värden till ett angivet värde
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி மதிப்புகளையும் குறிப்பிட்ட மதிப்பாக அமைக்கிறது
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ విలువలను పేర్కొన్న విలువగా సెట్ చేస్తుంది
    	]],
    	["th"] = [[
ตั้งค่าของจุดข้อมูลขาเข้าทั้งหมดเป็นค่าที่ระบุ
    	]],
    	["tr"] = [[
Gelen tüm veri noktası değerlerini belirtilen değere ayarlar
    	]],
    	["uk"] = [[
Встановлює для всіх вхідних значень точок даних вказане значення
    	]],
    	["vi"] = [[
Đặt giá trị của tất cả điểm dữ liệu đầu vào thành một giá trị được chỉ định
    	]],
    },
    config = {
        number {
            id = "new_value",
            name = {
            	["en"] = "New Value",
            	["af"] = "Nuwe Waarde",
            	["sq"] = "Vlera e re",
            	["am"] = "አዲስ እሴት",
            	["hy"] = "Նոր արժեք",
            	["az"] = "Yeni qiymət",
            	["bn"] = "নতুন মান",
            	["eu"] = "Balio berria",
            	["be"] = "Новае значэнне",
            	["bg"] = "Нова стойност",
            	["my"] = "တန်ဖိုးအသစ်",
            	["ca"] = "Valor nou",
            	["zh-Hans"] = "新值",
            	["zh-Hant"] = "新值",
            	["hr"] = "Nova vrijednost",
            	["cs"] = "Nová hodnota",
            	["da"] = "Ny værdi",
            	["nl"] = "Nieuwe waarde",
            	["et"] = "Uus väärtus",
            	["fil"] = "Bagong Halaga",
            	["fi"] = "Uusi arvo",
            	["fr"] = "Nouvelle valeur",
            	["gl"] = "Novo valor",
            	["ka"] = "ახალი მნიშვნელობა",
            	["de"] = "Neuer Wert",
            	["el"] = "Νέα τιμή",
            	["gu"] = "નવું મૂલ્ય",
            	["hi"] = "नया मान",
            	["hu"] = "Új érték",
            	["is"] = "Nýtt gildi",
            	["id"] = "Nilai Baru",
            	["it"] = "Nuovo valore",
            	["ja"] = "新しい値",
            	["kn"] = "ಹೊಸ ಮೌಲ್ಯ",
            	["kk"] = "Жаңа мән",
            	["km"] = "តម្លៃថ្មី",
            	["ko"] = "새 값",
            	["ky"] = "Жаңы маани",
            	["lo"] = "ຄ່າໃໝ່",
            	["lv"] = "Jaunā vērtība",
            	["lt"] = "Nauja reikšmė",
            	["mk"] = "Нова вредност",
            	["ms"] = "Nilai Baharu",
            	["ml"] = "പുതിയ മൂല്യം",
            	["mr"] = "नवीन मूल्य",
            	["mn"] = "Шинэ утга",
            	["ne"] = "नयाँ मान",
            	["no"] = "Ny verdi",
            	["pl"] = "Nowa wartość",
            	["pt"] = "Novo valor",
            	["pa"] = "ਨਵਾਂ ਮੁੱਲ",
            	["ro"] = "Valoare nouă",
            	["rm"] = "Nova valur",
            	["ru"] = "Новое значение",
            	["sr"] = "Nova vrednost",
            	["si"] = "නව අගය",
            	["sk"] = "Nová hodnota",
            	["sl"] = "Nova vrednost",
            	["es"] = "Nuevo valor",
            	["sw"] = "Thamani Mpya",
            	["sv"] = "Nytt värde",
            	["ta"] = "புதிய மதிப்பு",
            	["te"] = "కొత్త విలువ",
            	["th"] = "ค่าใหม่",
            	["tr"] = "Yeni Değer",
            	["uk"] = "Нове значення",
            	["vi"] = "Giá trị mới",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_value = config and config.new_value

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_value then
                return data_point
            end
            data_point.value = new_value

            return data_point
        end
    end,
}
