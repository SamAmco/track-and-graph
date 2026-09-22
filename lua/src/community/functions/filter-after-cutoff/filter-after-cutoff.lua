-- Lua Function to filter data points after a cutoff timestamp
-- This function only passes through data points that occur at or after the specified cutoff time

local instant = require("tng.config").instant
local core = require("tng.core")

local now_time = core.time()
local now = now_time and now_time.timestamp or 0

return {
    -- Configuration metadata
    id = "filter-after-cutoff",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_filter", "_time"},
    title = {
    	["en"] = "Filter After Cutoff",
    	["af"] = "Filtreer Ná Afsnytyd",
    	["sq"] = "Filtro pas kufirit",
    	["am"] = "ከመጨረሻ ጊዜ በኋላ አጣራ",
    	["hy"] = "Զտել վերջնակետից հետո",
    	["az"] = "Kəsimdən sonranı süzgəcdən keçir",
    	["bn"] = "কাটঅফের পর ফিল্টার করুন",
    	["eu"] = "Iragazi muga-denboraren ondoren",
    	["be"] = "Фільтраваць пасля гранічнага часу",
    	["bg"] = "Филтриране след крайния момент",
    	["my"] = "ကန့်သတ်ချိန်နောက်ပိုင်း စစ်ထုတ်ရန်",
    	["ca"] = "Filtra després del límit",
    	["zh-Hans"] = "筛选截止时间之后",
    	["zh-Hant"] = "篩選截止時間之後",
    	["hr"] = "Filtriraj nakon graničnog vremena",
    	["cs"] = "Filtrovat po mezním čase",
    	["da"] = "Filtrér efter skæringstidspunkt",
    	["nl"] = "Na grens filteren",
    	["et"] = "Filtreeri pärast lõpptähtaega",
    	["fil"] = "Salain Pagkatapos ng Cutoff",
    	["fi"] = "Suodata katkaisuajan jälkeen",
    	["fr"] = "Filtrer après la limite",
    	["gl"] = "Filtrar despois do límite",
    	["ka"] = "საბოლოო დროის შემდეგ ფილტრაცია",
    	["de"] = "Nach Stichtag filtern",
    	["el"] = "Φιλτράρισμα μετά το όριο",
    	["gu"] = "કટઑફ પછી ફિલ્ટર કરો",
    	["hi"] = "कटऑफ़ के बाद फ़िल्टर करें",
    	["hu"] = "Szűrés a határidő után",
    	["is"] = "Sía eftir lokatíma",
    	["id"] = "Saring Setelah Batas Waktu",
    	["it"] = "Filtra dopo il limite",
    	["ja"] = "カットオフ以降をフィルタ",
    	["kn"] = "ಕಟ್‌ಆಫ್ ನಂತರ ಫಿಲ್ಟರ್ ಮಾಡಿ",
    	["kk"] = "Шектен кейін сүзу",
    	["km"] = "ត្រងបន្ទាប់ពីពេលកំណត់",
    	["ko"] = "기준 시점 이후 필터링",
    	["ky"] = "Чектен кийинкисин чыпкалоо",
    	["lo"] = "ກັ່ນຕອງຫຼັງຈາກເວລາຕັດ",
    	["lv"] = "Filtrēt pēc robežlaika",
    	["lt"] = "Filtruoti po ribos",
    	["mk"] = "Филтрирај по граничното време",
    	["ms"] = "Tapis Selepas Had Masa",
    	["ml"] = "കട്ടോഫിന് ശേഷമുള്ളവ ഫിൽട്ടർ ചെയ്യുക",
    	["mr"] = "कटऑफनंतर गाळा",
    	["mn"] = "Таслах хугацааны дараахыг шүүх",
    	["ne"] = "कटअफपछि फिल्टर गर्नुहोस्",
    	["no"] = "Filtrer etter grense",
    	["pl"] = "Filtruj po czasie granicznym",
    	["pt"] = "Filtrar após o limite",
    	["pa"] = "ਕਟਆਫ਼ ਤੋਂ ਬਾਅਦ ਫਿਲਟਰ ਕਰੋ",
    	["ro"] = "Filtrează după limită",
    	["rm"] = "Filtrar suenter il termin",
    	["ru"] = "Фильтр после отсечения",
    	["sr"] = "Filtriraj nakon krajnjeg roka",
    	["si"] = "අවසන් සීමාවෙන් පසු පෙරහන් කරන්න",
    	["sk"] = "Filtrovať po hraničnom čase",
    	["sl"] = "Filtriraj po presečnem času",
    	["es"] = "Filtrar después del límite",
    	["sw"] = "Chuja Baada ya Kikomo",
    	["sv"] = "Filtrera efter gräns",
    	["ta"] = "காலவரம்புக்குப் பிறகு வடிகட்டு",
    	["te"] = "కట్‌ఆఫ్ తర్వాత ఫిల్టర్ చేయి",
    	["th"] = "กรองหลังเวลาตัด",
    	["tr"] = "Kesimden Sonra Filtrele",
    	["uk"] = "Фільтрувати після граничного часу",
    	["vi"] = "Lọc sau thời điểm giới hạn",
    },
    description = {
    	["en"] = "Filters data points to only include those at or after the specified cutoff time.",
    	["af"] = "Filtreer datapunte om slegs dié op of ná die gespesifiseerde afsnytyd in te sluit.",
    	["sq"] = "Filtron pikat e të dhënave për të përfshirë vetëm ato në ose pas kohës së specifikuar të kufirit.",
    	["am"] = "የውሂብ ነጥቦችን በተገለጸው የመጨረሻ ጊዜ ላይ ወይም ከዚያ በኋላ ያሉትን ብቻ እንዲያካትቱ ያጣራል።",
    	["hy"] = "Զտում է տվյալակետերը՝ ներառելով միայն նշված վերջնակետին կամ դրանից հետո գտնվողները։",
    	["az"] = "Məlumat nöqtələrini yalnız göstərilən kəsim vaxtında və ya ondan sonra olanları daxil edəcək şəkildə süzgəcdən keçirir.",
    	["bn"] = "নির্দিষ্ট কাটঅফ সময়ে বা তার পরে থাকা ডেটা পয়েন্টগুলোই রাখে।",
    	["eu"] = "Datu-puntuak iragazten ditu, zehaztutako muga-denboran edo ondoren daudenak soilik sartzeko.",
    	["be"] = "Фільтруе кропкі даных, пакідаючы толькі тыя, што знаходзяцца ў зададзены час або пазней.",
    	["bg"] = "Филтрира точките от данни така, че да включва само тези, които са в или след зададения краен момент.",
    	["my"] = "သတ်မှတ်ထားသော ကန့်သတ်ချိန်တွင် သို့မဟုတ် ထိုအချိန်နောက်ပိုင်းရှိ ဒေတာမှတ်များသာ ပါဝင်အောင် စစ်ထုတ်သည်။",
    	["ca"] = "Filtra els punts de dades per incloure només els que es troben a l’hora de tall especificada o després.",
    	["zh-Hans"] = "仅筛选指定截止时间或之后的数据点。",
    	["zh-Hant"] = "篩選資料點，只包含指定截止時間或之後的資料點。",
    	["hr"] = "Filtrira podatkovne točke tako da uključuje samo one u navedeno granično vrijeme ili nakon njega.",
    	["cs"] = "Filtruje datové body tak, aby zahrnovaly pouze ty, které nastaly v zadaném mezním čase nebo po něm.",
    	["da"] = "Filtrerer datapunkter, så kun dem på eller efter det angivne skæringstidspunkt medtages.",
    	["nl"] = "Filtert gegevenspunten zodat alleen punten op of na de opgegeven grenstijd worden opgenomen.",
    	["et"] = "Jätab alles ainult määratud lõppajaga samal ajal või hiljem olevad andmepunktid.",
    	["fil"] = "Sinasala ang mga data point upang isama lamang ang mga nasa o pagkatapos ng tinukoy na cutoff time.",
    	["fi"] = "Suodattaa datapisteet niin, että mukaan otetaan vain määritettynä katkaisuaikana tai sen jälkeen olevat pisteet.",
    	["fr"] = "Filtre les points de données pour ne conserver que ceux situés à l’heure limite indiquée ou après celle-ci.",
    	["gl"] = "Filtra os puntos de datos para incluír só os que se produzan no momento de corte especificado ou despois.",
    	["ka"] = "ფილტრავს მონაცემთა წერტილებს და ტოვებს მხოლოდ მითითებულ საბოლოო დროს ან მის შემდეგ მდებარე წერტილებს.",
    	["de"] = "Filtert Datenpunkte so, dass nur diejenigen am oder nach dem angegebenen Stichtag enthalten sind.",
    	["el"] = "Φιλτράρει τα σημεία δεδομένων ώστε να περιλαμβάνει μόνο όσα βρίσκονται στη συγκεκριμένη ώρα ορίου ή αργότερα.",
    	["gu"] = "ડેટા પોઇન્ટ્સને માત્ર નિર્દિષ્ટ કટઑફ સમય પર અથવા ત્યાર પછીના પોઇન્ટ્સ સુધી મર્યાદિત કરે છે.",
    	["hi"] = "डेटा पॉइंट को केवल निर्दिष्ट कटऑफ़ समय पर या उसके बाद वाले पॉइंट तक सीमित करता है।",
    	["hu"] = "Csak a megadott határidő időpontjában vagy azt követően lévő adatpontokat tartja meg.",
    	["is"] = "Síar gagnapunkta þannig að aðeins þeir sem eru á eða eftir tilgreindum lokatíma séu með.",
    	["id"] = "Menyaring titik data agar hanya mencakup titik data pada atau setelah waktu batas yang ditentukan.",
    	["it"] = "Filtra i punti dati includendo solo quelli successivi o uguali all'orario limite specificato.",
    	["ja"] = "指定したカットオフ時刻以降のデータポイントだけに絞り込みます。",
    	["kn"] = "ನಿರ್ದಿಷ್ಟ ಕಟ್‌ಆಫ್ ಸಮಯದ ನಂತರ ಅಥವಾ ಅದೇ ಸಮಯದಲ್ಲಿರುವ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಮಾತ್ರ ಒಳಗೊಂಡಂತೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ.",
    	["kk"] = "Дерек нүктелерін көрсетілген шек уақытында немесе одан кейінгілерін ғана қамтитындай сүзеді.",
    	["km"] = "ត្រងចំណុចទិន្នន័យ ដោយរក្សាទុកតែចំណុចដែលនៅពេលកំណត់ ឬបន្ទាប់ពីពេលកំណត់ដែលបានបញ្ជាក់។",
    	["ko"] = "지정한 기준 시점 이후(기준 시점 포함)의 데이터 포인트만 남깁니다.",
    	["ky"] = "Маалымат чекиттеринин ичинен көрсөтүлгөн чек убактысында же андан кийинкилери гана калат.",
    	["lo"] = "ກັ່ນຕອງຈຸດຂໍ້ມູນໃຫ້ລວມສະເພາະຈຸດທີ່ຢູ່ໃນ ຫຼື ຫຼັງເວລາຕັດທີ່ກຳນົດ.",
    	["lv"] = "Filtrē datu punktus, iekļaujot tikai tos, kas atrodas norādītajā robežlaikā vai pēc tā.",
    	["lt"] = "Filtruoja duomenų taškus, palikdama tik esančius nurodytu ribos laiku arba vėliau.",
    	["mk"] = "Ги филтрира точките на податоци така што ги вклучува само оние на или по зададеното гранично време.",
    	["ms"] = "Menapis titik data supaya hanya yang berlaku pada atau selepas waktu had yang ditentukan disertakan.",
    	["ml"] = "നിർദ്ദിഷ്ട കട്ടോഫ് സമയത്തോ അതിന് ശേഷമോ ഉള്ള ഡാറ്റാ പോയിന്റുകൾ മാത്രം ഉൾപ്പെടുത്തുന്നു.",
    	["mr"] = "निर्दिष्ट कटऑफ वेळेच्या वेळी किंवा त्यानंतरचे डेटा बिंदूच समाविष्ट करण्यासाठी गाळते.",
    	["mn"] = "Зөвхөн заасан таслах хугацаанд эсвэл түүнээс хойшхи өгөгдлийн цэгүүдийг үлдээнэ.",
    	["ne"] = "निर्दिष्ट कटअफ समय वा त्यसपछि भएका डेटा बिन्दुहरू मात्र राखेर फिल्टर गर्छ।",
    	["no"] = "Filtrerer datapunkter slik at bare de på eller etter det angitte grenseklokkeslettet inkluderes.",
    	["pl"] = "Filtruje punkty danych, pozostawiając tylko te przypadające w określonym czasie granicznym lub później.",
    	["pt"] = "Filtra os pontos de dados para incluir apenas os que ocorrem no momento do limite especificado ou depois dele.",
    	["pa"] = "ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿੱਚੋਂ ਸਿਰਫ਼ ਨਿਰਧਾਰਤ ਕਟਆਫ਼ ਸਮੇਂ ’ਤੇ ਜਾਂ ਉਸ ਤੋਂ ਬਾਅਦ ਵਾਲੇ ਪੁਆਇੰਟ ਰੱਖਦਾ ਹੈ।",
    	["ro"] = "Filtrează punctele de date pentru a le include doar pe cele de la momentul limită specificat sau ulterior.",
    	["rm"] = "Filtra ils puncts da datas per includer mo quels al termin specificà u suenter el.",
    	["ru"] = "Оставляет только точки данных, произошедшие в указанное время или позже.",
    	["sr"] = "Filtrira tačke podataka tako da obuhvati samo one koje se javljaju u navedeno vreme ili nakon njega.",
    	["si"] = "නිශ්චිත අවසන් සීමා වේලාවේ හෝ ඉන් පසු ඇති දත්ත ලක්ෂ්‍ය පමණක් ඇතුළත් වන ලෙස පෙරහන් කරයි.",
    	["sk"] = "Filtruje údajové body tak, aby obsahovali iba tie, ktoré nastali v určenom hraničnom čase alebo po ňom.",
    	["sl"] = "Filtrira podatkovne točke tako, da vključi le tiste ob določenem presečnem času ali po njem.",
    	["es"] = "Filtra los puntos de datos para incluir solo los que se encuentran en el momento límite especificado o después.",
    	["sw"] = "Huchuja nukta za data ili kujumuisha zile zilizo kwenye au baada ya muda wa kikomo uliobainishwa pekee.",
    	["sv"] = "Filtrerar datapunkter så att endast de vid eller efter den angivna gränstiden inkluderas.",
    	["ta"] = "குறிப்பிட்ட காலவரம்பு நேரத்தில் அல்லது அதற்குப் பிறகு உள்ள தரவுப் புள்ளிகளை மட்டும் சேர்க்க வடிகட்டுகிறது.",
    	["te"] = "పేర్కొన్న కట్‌ఆఫ్ సమయానికి లేదా దాని తర్వాత ఉన్న డేటా పాయింట్లను మాత్రమే ఉంచుతుంది.",
    	["th"] = "กรองจุดข้อมูลให้เหลือเฉพาะจุดที่อยู่ ณ หรือหลังเวลาตัดที่ระบุ",
    	["tr"] = "Veri noktalarını yalnızca belirtilen kesim zamanında veya sonrasında olanları içerecek şekilde filtreler.",
    	["uk"] = "Фільтрує точки даних, залишаючи лише ті, що припадають на вказаний граничний час або пізніше.",
    	["vi"] = "Lọc các điểm dữ liệu để chỉ giữ lại những điểm ở thời điểm giới hạn hoặc sau đó.",
    },
    config = {
        instant {
            id = "cutoff",
            name = "_cutoff",
            default = now - (30 * core.DURATION.DAY),  -- 30 days ago
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

                -- Only return data points at or after the cutoff
                if data_point.timestamp >= cutoff then
                    return data_point
                end
                -- Otherwise, skip this data point and continue to the next
            end
        end
    end,
}
