-- Lua Function to override the note of all data points with a configurable string
-- This function sets all incoming data point notes to a specified value

local text = require("tng.config").text

return {
    -- Configuration metadata
    id = "override-note",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_transform"},
    title = {
    	["en"] = "Override Note",
    	["af"] = "Oorskryf Nota",
    	["sq"] = "Anashkalo shënimin",
    	["am"] = "ማስታወሻን ተካ",
    	["hy"] = "Փոխարինել նշումը",
    	["az"] = "Qeydi əvəz et",
    	["bn"] = "নোট ওভাররাইড করুন",
    	["eu"] = "Gainidatzi oharra",
    	["be"] = "Замяніць заўвагу",
    	["bg"] = "Замяна на бележката",
    	["my"] = "မှတ်ချက်ကို အစားထိုးရန်",
    	["ca"] = "Substitueix la nota",
    	["zh-Hans"] = "覆盖备注",
    	["zh-Hant"] = "覆寫備註",
    	["hr"] = "Nadjačaj bilješku",
    	["cs"] = "Přepsat poznámku",
    	["da"] = "Tilsidesæt note",
    	["nl"] = "Notitie overschrijven",
    	["et"] = "Asenda märkus",
    	["fil"] = "I-override ang Note",
    	["fi"] = "Ohita muistiinpano",
    	["fr"] = "Remplacer la note",
    	["gl"] = "Substituír nota",
    	["ka"] = "შენიშვნის ჩანაცვლება",
    	["de"] = "Notiz überschreiben",
    	["el"] = "Παράκαμψη σημείωσης",
    	["gu"] = "નોંધ ઓવરરાઇડ કરો",
    	["hi"] = "नोट ओवरराइड करें",
    	["hu"] = "Megjegyzés felülírása",
    	["is"] = "Yfirskrifa athugasemd",
    	["id"] = "Ganti Catatan",
    	["it"] = "Sovrascrivi nota",
    	["ja"] = "メモを上書き",
    	["kn"] = "ಟಿಪ್ಪಣಿ ಅತಿಕ್ರಮಿಸಿ",
    	["kk"] = "Ескертпені қайта анықтау",
    	["km"] = "បដិសេធចំណាំ",
    	["ko"] = "메모 덮어쓰기",
    	["ky"] = "Эскертмени алмаштыруу",
    	["lo"] = "ລົບລ້າງໝາຍເຫດ",
    	["lv"] = "Aizstāt piezīmi",
    	["lt"] = "Pakeisti pastabą",
    	["mk"] = "Замени ја белешката",
    	["ms"] = "Gantikan Nota",
    	["ml"] = "കുറിപ്പ് അസാധുവാക്കുക",
    	["mr"] = "नोंद ओव्हरराइड करा",
    	["mn"] = "Тэмдэглэлийг дарж тохируулах",
    	["ne"] = "नोट ओभरराइड गर्नुहोस्",
    	["no"] = "Overstyr notat",
    	["pl"] = "Zastąp notatkę",
    	["pt"] = "Substituir nota",
    	["pa"] = "ਨੋਟ ਓਵਰਰਾਈਡ ਕਰੋ",
    	["ro"] = "Înlocuiește nota",
    	["rm"] = "Surscriver remartga",
    	["ru"] = "Заменить заметку",
    	["sr"] = "Zameni belešku",
    	["si"] = "සටහන අභිබවා යොදන්න",
    	["sk"] = "Prepísať poznámku",
    	["sl"] = "Prepiši opombo",
    	["es"] = "Reemplazar nota",
    	["sw"] = "Batilisha Dokezo",
    	["sv"] = "Åsidosätt anteckning",
    	["ta"] = "குறிப்பை மேலெழுது",
    	["te"] = "నోట్‌ను భర్తీ చేయి",
    	["th"] = "แทนที่บันทึก",
    	["tr"] = "Notu Geçersiz Kıl",
    	["uk"] = "Замінити примітку",
    	["vi"] = "Ghi đè ghi chú",
    },
    description = {
    	["en"] = [[
Sets all incoming data point notes to a specified value
    	]],
    	["af"] = [[
Stel alle inkomende datapuntnotas op ’n gespesifiseerde waarde
    	]],
    	["sq"] = [[
Vendos të gjitha shënimet hyrëse të pikave të të dhënave në një vlerë të specifikuar
    	]],
    	["am"] = [[
ሁሉንም የሚገቡ የውሂብ ነጥብ ማስታወሻዎችን በተገለጸ እሴት ያዘጋጃል።
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի նշումները սահմանում է նշված արժեքով
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qeydlərini göstərilən qiymətə təyin edir
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের নোটকে নির্দিষ্ট মানে সেট করে
    	]],
    	["eu"] = [[
Sarrerako datu-puntu guztien oharrak zehaztutako balio batera ezartzen ditu
    	]],
    	["be"] = [[
Задае ўсім уваходным заўвагам кропак даных пазначанае значэнне
    	]],
    	["bg"] = [[
Задава всички входящи бележки на точките от данни на определена стойност
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်မှတ်ချက်များအားလုံးကို သတ်မှတ်ထားသော တန်ဖိုးသို့ သတ်မှတ်သည်
    	]],
    	["ca"] = [[
Estableix totes les notes dels punts de dades entrants en un valor especificat
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的备注设置为指定值
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的備註設為指定值
    	]],
    	["hr"] = [[
Postavlja sve dolazne bilješke podatkovnih točaka na navedenu vrijednost
    	]],
    	["cs"] = [[
Nastaví všechny příchozí poznámky datových bodů na zadanou hodnotu
    	]],
    	["da"] = [[
Sætter alle indgående datapunkters noter til en angivet værdi
    	]],
    	["nl"] = [[
Stelt alle binnenkomende datapuntennotities in op een opgegeven waarde
    	]],
    	["et"] = [[
Määrab kõikidele sisendandmepunktide märkmetele määratud väärtuse
    	]],
    	["fil"] = [[
Itinatakda ang lahat ng papasok na note ng data point sa tinukoy na halaga
    	]],
    	["fi"] = [[
Asettaa kaikkien saapuvien datapisteiden muistiinpanoksi määritetyn arvon
    	]],
    	["fr"] = [[
Définit toutes les notes des points de données entrants sur une valeur indiquée
    	]],
    	["gl"] = [[
Establece todas as notas dos puntos de datos recibidos no valor especificado
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის შენიშვნას მითითებული მნიშვნელობით ანაცვლებს
    	]],
    	["de"] = [[
Setzt alle eingehenden Datenpunktnotizen auf einen angegebenen Wert
    	]],
    	["el"] = [[
Ορίζει όλες τις εισερχόμενες σημειώσεις σημείων δεδομένων σε μια καθορισμένη τιμή
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટની નોંધોને નિર્દિષ્ટ મૂલ્ય પર સેટ કરે છે
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के नोट को निर्दिष्ट मान पर सेट करता है
    	]],
    	["hu"] = [[
Az összes beérkező adatpont megjegyzését a megadott értékre állítja
    	]],
    	["is"] = [[
Stillir allar athugasemdir innkomandi gagnapunkta á tilgreint gildi
    	]],
    	["id"] = [[
Mengatur semua catatan titik data yang masuk ke nilai yang ditentukan
    	]],
    	["it"] = [[
Imposta tutte le note dei punti dati in arrivo su un valore specificato
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントのメモを指定した値に設定します
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಟಿಪ್ಪಣಿಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ಮೌಲ್ಯಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің ескертпелерін көрсетілген мәнге орнатады
    	]],
    	["km"] = [[
កំណត់ចំណាំរបស់ចំណុចទិន្នន័យចូលទាំងអស់ទៅជាតម្លៃដែលបានបញ្ជាក់
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 메모를 지정한 값으로 설정합니다
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин эскертмелерин көрсөтүлгөн мааниге өзгөртөт
    	]],
    	["lo"] = [[
ກຳນົດໝາຍເຫດຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດເປັນຄ່າທີ່ກຳນົດ
    	]],
    	["lv"] = [[
Iestata visām ienākošo datu punktu piezīmēm norādīto vērtību
    	]],
    	["lt"] = [[
Nustato visų gaunamų duomenų taškų pastabas į nurodytą reikšmę
    	]],
    	["mk"] = [[
Ги поставува сите влезни белешки на точките на податоци на зададена вредност
    	]],
    	["ms"] = [[
Menetapkan semua nota titik data yang masuk kepada nilai yang ditentukan
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് കുറിപ്പുകളും നിർദ്ദിഷ്ട മൂല്യമായി സജ്ജീകരിക്കുന്നു
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सच्या नोंदी निर्दिष्ट मूल्यावर सेट करते
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн тэмдэглэлийг заасан утгаар тохируулна
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका नोटलाई निर्दिष्ट मानमा सेट गर्छ
    	]],
    	["no"] = [[
Setter notatet til alle innkommende datapunkter til en angitt verdi
    	]],
    	["pl"] = [[
Ustawia wszystkie przychodzące notatki punktów danych na określoną wartość
    	]],
    	["pt"] = [[
Define todas as notas dos pontos de dados recebidos como um valor especificado
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਨੋਟਾਂ ਨੂੰ ਨਿਰਧਾਰਤ ਮੁੱਲ 'ਤੇ ਸੈੱਟ ਕਰਦਾ ਹੈ
    	]],
    	["ro"] = [[
Setează toate notele punctelor de date primite la o valoare specificată
    	]],
    	["rm"] = [[
Definescha tut las remartgas dals puncts da datas entrants ad ina valur spezificada
    	]],
    	["ru"] = [[
Задаёт всем входящим заметкам точек данных указанное значение
    	]],
    	["sr"] = [[
Postavlja sve dolazne beleške tačaka podataka na zadatu vrednost
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය සටහන් නිශ්චිත අගයකට සකසයි
    	]],
    	["sk"] = [[
Nastaví poznámky všetkých prichádzajúcich údajových bodov na zadanú hodnotu
    	]],
    	["sl"] = [[
Nastavi vse vhodne opombe podatkovnih točk na določeno vrednost
    	]],
    	["es"] = [[
Establece todas las notas de los puntos de datos entrantes en un valor especificado
    	]],
    	["sw"] = [[
Huandaa madokezo yote ya nukta za data zinazoingia kuwa thamani maalum
    	]],
    	["sv"] = [[
Ställer in alla inkommande datapunkters anteckningar till ett angivet värde
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி குறிப்புகளையும் குறிப்பிட்ட மதிப்பாக அமைக்கிறது
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ నోట్లను పేర్కొన్న విలువగా సెట్ చేస్తుంది
    	]],
    	["th"] = [[
ตั้งค่าบันทึกของจุดข้อมูลขาเข้าทั้งหมดเป็นค่าที่ระบุ
    	]],
    	["tr"] = [[
Gelen tüm veri noktası notlarını belirtilen değere ayarlar
    	]],
    	["uk"] = [[
Встановлює для всіх вхідних приміток точок даних вказане значення
    	]],
    	["vi"] = [[
Đặt ghi chú của tất cả điểm dữ liệu đầu vào thành một giá trị được chỉ định
    	]],
    },
    config = {
        text {
            id = "new_note",
            name = {
            	["en"] = "New Note",
            	["af"] = "Nuwe Nota",
            	["sq"] = "Shënimi i ri",
            	["am"] = "አዲስ ማስታወሻ",
            	["hy"] = "Նոր նշում",
            	["az"] = "Yeni qeyd",
            	["bn"] = "নতুন নোট",
            	["eu"] = "Ohar berria",
            	["be"] = "Новая заўвага",
            	["bg"] = "Нова бележка",
            	["my"] = "မှတ်ချက်အသစ်",
            	["ca"] = "Nota nova",
            	["zh-Hans"] = "新备注",
            	["zh-Hant"] = "新備註",
            	["hr"] = "Nova bilješka",
            	["cs"] = "Nová poznámka",
            	["da"] = "Ny note",
            	["nl"] = "Nieuwe notitie",
            	["et"] = "Uus märkus",
            	["fil"] = "Bagong Note",
            	["fi"] = "Uusi muistiinpano",
            	["fr"] = "Nouvelle note",
            	["gl"] = "Nova nota",
            	["ka"] = "ახალი შენიშვნა",
            	["de"] = "Neue Notiz",
            	["el"] = "Νέα σημείωση",
            	["gu"] = "નવી નોંધ",
            	["hi"] = "नया नोट",
            	["hu"] = "Új megjegyzés",
            	["is"] = "Ný athugasemd",
            	["id"] = "Catatan Baru",
            	["it"] = "Nuova nota",
            	["ja"] = "新しいメモ",
            	["kn"] = "ಹೊಸ ಟಿಪ್ಪಣಿ",
            	["kk"] = "Жаңа ескертпе",
            	["km"] = "ចំណាំថ្មី",
            	["ko"] = "새 메모",
            	["ky"] = "Жаңы эскертме",
            	["lo"] = "ໝາຍເຫດໃໝ່",
            	["lv"] = "Jaunā piezīme",
            	["lt"] = "Nauja pastaba",
            	["mk"] = "Нова белешка",
            	["ms"] = "Nota Baharu",
            	["ml"] = "പുതിയ കുറിപ്പ്",
            	["mr"] = "नवीन नोंद",
            	["mn"] = "Шинэ тэмдэглэл",
            	["ne"] = "नयाँ नोट",
            	["no"] = "Nytt notat",
            	["pl"] = "Nowa notatka",
            	["pt"] = "Nova nota",
            	["pa"] = "ਨਵਾਂ ਨੋਟ",
            	["ro"] = "Notă nouă",
            	["rm"] = "Nova remartga",
            	["ru"] = "Новая заметка",
            	["sr"] = "Nova beleška",
            	["si"] = "නව සටහන",
            	["sk"] = "Nová poznámka",
            	["sl"] = "Nova opomba",
            	["es"] = "Nueva nota",
            	["sw"] = "Dokezo Jipya",
            	["sv"] = "Ny anteckning",
            	["ta"] = "புதிய குறிப்பு",
            	["te"] = "కొత్త నోట్",
            	["th"] = "บันทึกใหม่",
            	["tr"] = "Yeni Not",
            	["uk"] = "Нова примітка",
            	["vi"] = "Ghi chú mới",
            },
        },
    },

    -- Generator function
    generator = function(source, config)
        local new_note = config and config.new_note

        return function()
            local data_point = source.dp()
            if not data_point then
                return nil
            end

            if not new_note then
                return data_point
            end
            data_point.note = new_note

            return data_point
        end
    end,
}
