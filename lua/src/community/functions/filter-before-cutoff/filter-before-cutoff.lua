-- Lua Function to filter data points before a cutoff timestamp
-- This function only passes through data points that occur before the specified cutoff time
local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-before-cutoff",
    version = "1.0.1",
    inputCount = 1,
    categories = { "_filter", "_time" },

    title = {
    	["en"] = "Filter Before Cutoff",
    	["af"] = "Filtreer Voor Afsnytyd",
    	["sq"] = "Filtro para kufirit",
    	["am"] = "ከመገደቢያው በፊት አጣራ",
    	["hy"] = "Զտել վերջնակետից առաջ",
    	["az"] = "Kəsimdən əvvəlkini süzgəcdən keçir",
    	["bn"] = "কাটঅফের আগে ফিল্টার করুন",
    	["eu"] = "Iragazi muga-denboraren aurretik",
    	["be"] = "Фільтраваць да гранічнага часу",
    	["bg"] = "Филтриране преди крайния момент",
    	["my"] = "ကန့်သတ်ချိန်မတိုင်မီ စစ်ထုတ်ရန်",
    	["ca"] = "Filtra abans del límit",
    	["zh-Hans"] = "筛选截止时间之前",
    	["zh-Hant"] = "篩選截止時間之前",
    	["hr"] = "Filtriraj prije graničnog vremena",
    	["cs"] = "Filtrovat před mezním časem",
    	["da"] = "Filtrér før skæringstidspunkt",
    	["nl"] = "Voor grens filteren",
    	["et"] = "Filtreeri enne lõpptähtaega",
    	["fil"] = "Salain Bago ang Cutoff",
    	["fi"] = "Suodata ennen katkaisuaikaa",
    	["fr"] = "Filtrer avant la limite",
    	["gl"] = "Filtrar antes do límite",
    	["ka"] = "საბოლოო დრომდე ფილტრაცია",
    	["de"] = "Vor Stichtag filtern",
    	["el"] = "Φιλτράρισμα πριν από το όριο",
    	["gu"] = "કટઑફ પહેલાં ફિલ્ટર કરો",
    	["hi"] = "कटऑफ़ से पहले फ़िल्टर करें",
    	["hu"] = "Szűrés a határidő előtt",
    	["is"] = "Sía fyrir lokatíma",
    	["id"] = "Saring Sebelum Batas Waktu",
    	["it"] = "Filtra prima del limite",
    	["ja"] = "カットオフ以前をフィルタ",
    	["kn"] = "ಕಟ್‌ಆಫ್ ಮೊದಲು ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Шекке дейін сүзу",
    	["km"] = "ត្រងមុនពេលកំណត់",
    	["ko"] = "기준 시점 이전 필터링",
    	["ky"] = "Чекке чейинкини чыпкалоо",
    	["lo"] = "ກັ່ນຕອງກ່ອນເວລາຕັດ",
    	["lv"] = "Filtrēt pirms robežlaika",
    	["lt"] = "Filtruoti prieš ribą",
    	["mk"] = "Филтрирај пред граничното време",
    	["ms"] = "Tapis Sebelum Had Masa",
    	["ml"] = "കട്ടോഫിന് മുമ്പുള്ളവ ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "कटऑफपूर्वी फिल्टर करा",
    	["mn"] = "Таслах хугацааны өмнөхийг шүүх",
    	["ne"] = "कटअफअघि फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer før grense",
    	["pl"] = "Filtruj przed czasem granicznym",
    	["pt"] = "Filtrar antes do limite",
    	["pa"] = "ਕਟਆਫ਼ ਤੋਂ ਪਹਿਲਾਂ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează înainte de limită",
    	["rm"] = "Filtrar avant il termin",
    	["ru"] = "Фильтр до отсечения",
    	["sr"] = "Filtriraj pre krajnjeg roka",
    	["si"] = "අවසන් සීමාවට පෙර පෙරහන් කරන්න",
    	["sk"] = "Filtrovať pred hraničným časom",
    	["sl"] = "Filtriraj pred presečnim časom",
    	["es"] = "Filtrar antes del límite",
    	["sw"] = "Chuja Kabla ya Kikomo",
    	["sv"] = "Filtrera före gräns",
    	["ta"] = "காலவரம்புக்கு முன் வடிகட்டு",
    	["te"] = "కట్‌ఆఫ్‌కు ముందు ఫిల్టర్ చేయి",
    	["th"] = "กรองก่อนเวลาตัด",
    	["tr"] = "Kesimden Önce Filtrele",
    	["uk"] = "Фільтрувати до граничного часу",
    	["vi"] = "Lọc trước thời điểm giới hạn",
    },

    description = {
    	["en"] = "Filters data points to only include those before the specified cutoff time.",
    	["af"] = "Filtreer datapunte om slegs dié voor die gespesifiseerde afsnytyd in te sluit.",
    	["sq"] = "Filtron pikat e të dhënave për të përfshirë vetëm ato para kohës së specifikuar të kufirit.",
    	["am"] = "ከተገለጸው የመገደቢያ ጊዜ በፊት የሚከሰቱትን የውሂብ ነጥቦች ብቻ እንዲያካትቱ ያጣራል።",
    	["hy"] = "Զտում է տվյալակետերը՝ ներառելով միայն նշված վերջնակետից առաջ գտնվողները։",
    	["az"] = "Məlumat nöqtələrini yalnız göstərilən kəsim vaxtından əvvəl olanları daxil edəcək şəkildə süzgəcdən keçirir.",
    	["bn"] = "নির্দিষ্ট কাটঅফ সময়ের আগে থাকা ডেটা পয়েন্টগুলোই রাখে।",
    	["eu"] = "Datu-puntuak iragazten ditu, zehaztutako muga-denboraren aurretik daudenak soilik sartzeko.",
    	["be"] = "Фільтруе кропкі даных, пакідаючы толькі тыя, што знаходзяцца да зададзенага часу.",
    	["bg"] = "Филтрира точките от данни така, че да включва само тези преди зададения краен момент.",
    	["my"] = "သတ်မှတ်ထားသော ကန့်သတ်ချိန်မတိုင်မီရှိ ဒေတာမှတ်များသာ ပါဝင်အောင် စစ်ထုတ်သည်။",
    	["ca"] = "Filtra els punts de dades per incloure només els que es troben abans de l’hora de tall especificada.",
    	["zh-Hans"] = "仅筛选指定截止时间之前的数据点。",
    	["zh-Hant"] = "篩選資料點，只包含指定截止時間之前的資料點。",
    	["hr"] = "Filtrira podatkovne točke tako da uključuje samo one prije navedenog graničnog vremena.",
    	["cs"] = "Filtruje datové body tak, aby zahrnovaly pouze ty, které nastaly před zadaným mezním časem.",
    	["da"] = "Filtrerer datapunkter, så kun dem før det angivne skæringstidspunkt medtages.",
    	["nl"] = "Filtert gegevenspunten zodat alleen punten vóór de opgegeven grenstijd worden opgenomen.",
    	["et"] = "Jätab alles ainult määratud lõppajast varasemad andmepunktid.",
    	["fil"] = "Sinasala ang mga data point upang isama lamang ang mga bago sa tinukoy na cutoff time.",
    	["fi"] = "Suodattaa datapisteet niin, että mukaan otetaan vain ennen määritettyä katkaisuaikaa olevat pisteet.",
    	["fr"] = "Filtre les points de données pour ne conserver que ceux situés avant l’heure limite indiquée.",
    	["gl"] = "Filtra os puntos de datos para incluír só os que se produzan antes do momento de corte especificado.",
    	["ka"] = "ფილტრავს მონაცემთა წერტილებს და ტოვებს მხოლოდ მითითებულ საბოლოო დრომდე მდებარე წერტილებს.",
    	["de"] = "Filtert Datenpunkte so, dass nur diejenigen vor dem angegebenen Stichtag enthalten sind.",
    	["el"] = "Φιλτράρει τα σημεία δεδομένων ώστε να περιλαμβάνει μόνο όσα βρίσκονται πριν από τη συγκεκριμένη ώρα ορίου.",
    	["gu"] = "ડેટા પોઇન્ટ્સને માત્ર નિર્દિષ્ટ કટઑફ સમય પહેલાંના પોઇન્ટ્સ સુધી મર્યાદિત કરે છે.",
    	["hi"] = "डेटा पॉइंट को केवल निर्दिष्ट कटऑफ़ समय से पहले वाले पॉइंट तक सीमित करता है।",
    	["hu"] = "Csak a megadott határidő előtt lévő adatpontokat tartja meg.",
    	["is"] = "Síar gagnapunkta þannig að aðeins þeir sem eru fyrir tilgreindum lokatíma séu með.",
    	["id"] = "Menyaring titik data agar hanya mencakup titik data sebelum waktu batas yang ditentukan.",
    	["it"] = "Filtra i punti dati includendo solo quelli precedenti all'orario limite specificato.",
    	["ja"] = "指定したカットオフ時刻より前のデータポイントだけに絞り込みます。",
    	["kn"] = "ನಿರ್ದಿಷ್ಟ ಕಟ್‌ಆಫ್ ಸಮಯಕ್ಕಿಂತ ಮೊದಲು ಇರುವ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಮಾತ್ರ ಒಳಗೊಂಡಂತೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ.",
    	["kk"] = "Дерек нүктелерін көрсетілген шек уақытына дейінгілерін ғана қамтитындай сүзеді.",
    	["km"] = "ត្រងចំណុចទិន្នន័យ ដោយរក្សាទុកតែចំណុចដែលនៅមុនពេលកំណត់ដែលបានបញ្ជាក់។",
    	["ko"] = "지정한 기준 시점 이전의 데이터 포인트만 남깁니다.",
    	["ky"] = "Маалымат чекиттеринин ичинен көрсөтүлгөн чек убактысына чейинкилерин гана калат.",
    	["lo"] = "ກັ່ນຕອງຈຸດຂໍ້ມູນ ໃຫ້ລວມສະເພາະຈຸດຂໍ້ມູນກ່ອນເວລາຕັດທີ່ກຳນົດ.",
    	["lv"] = "Filtrē datu punktus, iekļaujot tikai tos, kas ir pirms norādītā robežlaika.",
    	["lt"] = "Filtruoja duomenų taškus, palikdama tik esančius prieš nurodytą ribos laiką.",
    	["mk"] = "Ги филтрира точките на податоци така што ги вклучува само оние пред зададеното гранично време.",
    	["ms"] = "Menapis titik data supaya hanya yang berlaku sebelum waktu had yang ditentukan disertakan.",
    	["ml"] = "നിർദ്ദിഷ്ട കട്ടോഫ് സമയത്തിന് മുമ്പുള്ള ഡാറ്റാ പോയിന്റുകൾ മാത്രം ഉൾപ്പെടുത്തുന്നു.",
    	["mr"] = "निर्दिष्ट कटऑफ वेळेपूर्वीचे डेटा पॉइंट्सच ठेवतो.",
    	["mn"] = "Зөвхөн заасан таслах хугацаанаас өмнөх өгөгдлийн цэгүүдийг үлдээнэ.",
    	["ne"] = "निर्दिष्ट कटअफ समयभन्दा अघि भएका डेटा बिन्दुहरू मात्र राखेर फिल्टर गर्छ।",
    	["no"] = "Filtrerer datapunkter slik at bare de før det angitte grenseklokkeslettet inkluderes.",
    	["pl"] = "Filtruje punkty danych, pozostawiając tylko te przypadające przed określonym czasem granicznym.",
    	["pt"] = "Filtra os pontos de dados para incluir apenas os que ocorrem antes do limite especificado.",
    	["pa"] = "ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿੱਚੋਂ ਸਿਰਫ਼ ਨਿਰਧਾਰਤ ਕਟਆਫ਼ ਸਮੇਂ ਤੋਂ ਪਹਿਲਾਂ ਵਾਲੇ ਪੁਆਇੰਟ ਰੱਖਦਾ ਹੈ।",
    	["ro"] = "Filtrează punctele de date pentru a le include doar pe cele anterioare momentului limită specificat.",
    	["rm"] = "Filtra ils puncts da datas per includer mo quels avant il termin specificà.",
    	["ru"] = "Оставляет только точки данных, произошедшие до указанного времени.",
    	["sr"] = "Filtrira tačke podataka tako da obuhvati samo one koje se javljaju pre navedenog vremena.",
    	["si"] = "නිශ්චිත අවසන් සීමා වේලාවට පෙර ඇති දත්ත ලක්ෂ්‍ය පමණක් ඇතුළත් වන ලෙස පෙරහන් කරයි.",
    	["sk"] = "Filtruje údajové body tak, aby obsahovali iba tie, ktoré nastali pred určeným hraničným časom.",
    	["sl"] = "Filtrira podatkovne točke tako, da vključi le tiste pred določenim presečnim časom.",
    	["es"] = "Filtra los puntos de datos para incluir solo los que se encuentran antes del momento límite especificado.",
    	["sw"] = "Huchuja nukta za data ili kujumuisha zile zilizo kabla ya muda wa kikomo uliobainishwa pekee.",
    	["sv"] = "Filtrerar datapunkter så att endast de före den angivna gränstiden inkluderas.",
    	["ta"] = "குறிப்பிட்ட காலவரம்பு நேரத்திற்கு முன் உள்ள தரவுப் புள்ளிகளை மட்டும் சேர்க்க வடிகட்டுகிறது.",
    	["te"] = "పేర్కొన్న కట్‌ఆఫ్ సమయానికి ముందు ఉన్న డేటా పాయింట్లను మాత్రమే ఉంచుతుంది.",
    	["th"] = "กรองจุดข้อมูลให้เหลือเฉพาะจุดที่อยู่ก่อนเวลาตัดที่ระบุ",
    	["tr"] = "Veri noktalarını yalnızca belirtilen kesim zamanından önce olanları içerecek şekilde filtreler.",
    	["uk"] = "Фільтрує точки даних, залишаючи лише ті, що припадають до вказаного граничного часу.",
    	["vi"] = "Lọc các điểm dữ liệu để chỉ giữ lại những điểm trước thời điểm giới hạn đã chỉ định.",
    },

    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now, -- Current time as default
        },
    },

    -- Generator function
    generator = function(source, config)
        local cutoff = config and config.cutoff or error("Cutoff configuration is required")

        return function()
            while true do
                local data_point = source.dp()
                if not data_point then
                    return nil
                end

                -- Only return data points before the cutoff
                if data_point.timestamp < cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
