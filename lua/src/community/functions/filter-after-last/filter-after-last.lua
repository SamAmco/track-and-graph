-- Lua Function to filter data points after a reference point
-- Outputs all data points from the first source that come after the last point in the second source

return {
	-- Configuration metadata
	id = "filter-after-last",
	version = "1.0.1",
	inputCount = 2,
	categories = {"_filter"},
	title = {
		["en"] = "Filter After Last",
		["af"] = "Filtreer Ná Laaste",
		["sq"] = "Filtro pas të fundit",
		["am"] = "ከመጨረሻው በኋላ አጣራ",
		["hy"] = "Զտել վերջինից հետո",
		["az"] = "Sonuncudan sonranı süzgəcdən keçir",
		["bn"] = "সর্বশেষের পর ফিল্টার করুন",
		["eu"] = "Iragazi azkenaren ondoren",
		["be"] = "Фільтраваць пасля апошняй",
		["bg"] = "Филтриране след последната",
		["my"] = "နောက်ဆုံးဒေတာနောက်ပိုင်း စစ်ထုတ်ရန်",
		["ca"] = "Filtra després de l’últim",
		["zh-Hans"] = "筛选最后一个之后",
		["zh-Hant"] = "篩選最後一筆之後",
		["hr"] = "Filtriraj nakon posljednje",
		["cs"] = "Filtrovat po posledním",
		["da"] = "Filtrér efter seneste",
		["nl"] = "Na laatste filteren",
		["et"] = "Filtreeri pärast viimast",
		["fil"] = "Salain Pagkatapos ng Huli",
		["fi"] = "Suodata viimeisen jälkeen",
		["fr"] = "Filtrer après le dernier",
		["gl"] = "Filtrar despois do último",
		["ka"] = "ბოლო წერტილის შემდეგ ფილტრაცია",
		["de"] = "Nach letztem Datenpunkt filtern",
		["el"] = "Φιλτράρισμα μετά το τελευταίο",
		["gu"] = "છેલ્લા પછી ફિલ્ટર કરો",
		["hi"] = "अंतिम के बाद फ़िल्टर करें",
		["hu"] = "Szűrés az utolsó után",
		["is"] = "Sía eftir síðasta",
		["id"] = "Saring Setelah Terakhir",
		["it"] = "Filtra dopo l'ultimo",
		["ja"] = "最後のデータポイント以降をフィルタ",
		["kn"] = "ಕೊನೆಯದಾದ ನಂತರ ಫಿಲ್ಟರ್ ಮಾಡಿ",
		["kk"] = "Соңғысынан кейін сүзу",
		["km"] = "ត្រងបន្ទាប់ពីចំណុចចុងក្រោយ",
		["ko"] = "마지막 항목 이후 필터링",
		["ky"] = "Акыркыдан кийинкисин чыпкалоо",
		["lo"] = "ກັ່ນຕອງຫຼັງຈຸດຂໍ້ມູນສຸດທ້າຍ",
		["lv"] = "Filtrēt pēc pēdējā",
		["lt"] = "Filtruoti po paskutinio",
		["mk"] = "Филтрирај по последната",
		["ms"] = "Tapis Selepas Terakhir",
		["ml"] = "അവസാനത്തേതിന് ശേഷമുള്ളവ ഫിൽട്ടർ ചെയ്യുക",
		["mr"] = "शेवटच्यानंतर फिल्टर करा",
		["mn"] = "Сүүлийнхээс хойшихыг шүүх",
		["ne"] = "अन्तिमपछि फिल्टर गर्नुहोस्",
		["no"] = "Filtrer etter siste",
		["pl"] = "Filtruj po ostatnim",
		["pt"] = "Filtrar após o último",
		["pa"] = "ਆਖਰੀ ਤੋਂ ਬਾਅਦ ਫਿਲਟਰ ਕਰੋ",
		["ro"] = "Filtrează după ultimul punct",
		["rm"] = "Filtrar suenter l’ultim",
		["ru"] = "Фильтр после последней",
		["sr"] = "Filtriraj nakon poslednje",
		["si"] = "අවසන් දත්ත ලක්ෂ්‍යයෙන් පසු පෙරහන් කරන්න",
		["sk"] = "Filtrovať po poslednom",
		["sl"] = "Filtriraj po zadnji točki",
		["es"] = "Filtrar después del último",
		["sw"] = "Chuja Baada ya Ya Mwisho",
		["sv"] = "Filtrera efter senaste",
		["ta"] = "கடைசியிற்குப் பிறகு வடிகட்டு",
		["te"] = "చివరిదాని తర్వాత ఫిల్టర్ చేయి",
		["th"] = "กรองหลังจุดล่าสุด",
		["tr"] = "Son Veriden Sonra Filtrele",
		["uk"] = "Фільтрувати після останньої",
		["vi"] = "Lọc sau điểm cuối cùng",
	},
	description = {
		["en"] = [[
Filters data points from the first input source to only include those that occur after the last data point in the second input source.

This is useful for filtering data based on a reference event or timestamp from another tracker.
		]],
		["af"] = [[
Filtreer datapunte uit die eerste invoerbron om slegs dié in te sluit wat ná die laaste datapunt in die tweede invoerbron voorkom.

Dit is nuttig om data te filtreer op grond van ’n verwysingsgebeurtenis of tydstempel uit ’n ander spoorsnyer.
		]],
		["sq"] = [[
Filtron pikat e të dhënave nga burimi i parë hyrës për të përfshirë vetëm ato që ndodhin pas pikës së fundit të të dhënave në burimin e dytë hyrës.

Kjo është e dobishme për filtrimin e të dhënave bazuar në një ngjarje ose vulë kohore referuese nga një gjurmues tjetër.
		]],
		["am"] = [[
ከመጀመሪያው የግቤት ምንጭ የሚመጡ የውሂብ ነጥቦችን ከሁለተኛው የግቤት ምንጭ የመጨረሻ የውሂብ ነጥብ በኋላ የሚከሰቱትን ብቻ እንዲያካትቱ ያጣራል።

ይህ ከሌላ መከታተያ በሚመጣ የማጣቀሻ ክስተት ወይም የጊዜ ማህተም መሠረት ውሂብን ለማጣራት ይጠቅማል።
		]],
		["hy"] = [[
Առաջին մուտքային աղբյուրի տվյալակետերից ներառում է միայն այն տվյալակետերը, որոնք գտնվում են երկրորդ մուտքային աղբյուրի վերջին տվյալակետից հետո։

Օգտակար է տվյալները մեկ այլ թրեքերից ստացված հղումային իրադարձության կամ ժամանակացույցի հիման վրա զտելու համար։
		]],
		["az"] = [[
Birinci giriş mənbəyindəki məlumat nöqtələrindən yalnız ikinci giriş mənbəyindəki son məlumat nöqtəsindən sonra baş verənləri saxlayır.

Bu, məlumatları başqa izləyicidəki istinad hadisəsinə və ya zaman damğasına əsasən süzgəcdən keçirmək üçün faydalıdır.
		]],
		["bn"] = [[
প্রথম ইনপুট উৎসের ডেটা পয়েন্টগুলোর মধ্যে কেবল সেগুলো রাখে, যেগুলো দ্বিতীয় ইনপুট উৎসের সর্বশেষ ডেটা পয়েন্টের পরে ঘটে।

অন্য ট্র্যাকার থেকে পাওয়া রেফারেন্স ইভেন্ট বা টাইমস্ট্যাম্পের ভিত্তিতে ডেটা ফিল্টার করতে এটি কার্যকর।
		]],
		["eu"] = [[
Lehen sarrera-iturburuko datu-puntuak iragazten ditu, bigarren sarrera-iturburuko azken datu-puntuaren ondoren gertatzen direnak soilik sartzeko.

Hau erabilgarria da datuak beste tracker bateko erreferentzia-gertaera edo denbora-zigilu batean oinarrituta iragazteko.
		]],
		["be"] = [[
Фільтруе кропкі даных з першай крыніцы, пакідаючы толькі тыя, што адбываюцца пасля апошняй кропкі даных у другой крыніцы.

Гэта карысна для фільтрацыі даных на аснове апорнай падзеі або меткі часу з іншага трэкерa.
		]],
		["bg"] = [[
Филтрира точките от данни от първия източник, като включва само тези, които са след последната точка от данни във втория източник.

Това е полезно за филтриране на данни въз основа на референтно събитие или времеви отпечатък от друг тракер.
		]],
		["my"] = [[
ပထမထည့်သွင်းရင်းမြစ်မှ ဒေတာမှတ်များအနက် ဒုတိယထည့်သွင်းရင်းမြစ်၏ နောက်ဆုံးဒေတာမှတ်နောက်ပိုင်းတွင် ဖြစ်ပေါ်သော ဒေတာမှတ်များသာ ပါဝင်အောင် စစ်ထုတ်သည်။

အခြား Tracker မှ ရည်ညွှန်းဖြစ်ရပ် သို့မဟုတ် အချိန်တံဆိပ်အပေါ် အခြေခံ၍ ဒေတာစစ်ထုတ်ရာတွင် အသုံးဝင်သည်။
		]],
		["ca"] = [[
Filtra els punts de dades de la primera font d’entrada per incloure només els que es produeixen després de l’últim punt de dades de la segona font d’entrada.

És útil per filtrar dades segons un esdeveniment o una marca de temps de referència d’un altre tracker.
		]],
		["zh-Hans"] = [[
从第一个输入源的数据点中，仅保留发生在第二个输入源最后一个数据点之后的数据点。

这适用于根据其他 Tracker 中的参考事件或时间戳筛选数据。
		]],
		["zh-Hant"] = [[
從第一個輸入來源篩選資料點，只包含發生在第二個輸入來源最後一個資料點之後的資料點。

這適合根據另一個追蹤器中的參考事件或時間戳記篩選資料。
		]],
		["hr"] = [[
Filtrira podatkovne točke iz prvog izvora tako da uključuje samo one koje se pojavljuju nakon posljednje podatkovne točke u drugom izvoru.

Korisno je za filtriranje podataka na temelju referentnog događaja ili vremenske oznake iz drugog trackera.
		]],
		["cs"] = [[
Filtruje datové body z prvního zdroje tak, aby zahrnoval pouze ty, které nastaly po posledním datovém bodu ve druhém zdroji.

To je užitečné pro filtrování dat podle referenční události nebo časového razítka z jiného trackeru.
		]],
		["da"] = [[
Filtrerer datapunkter fra den første inputkilde, så kun dem, der forekommer efter det seneste datapunkt i den anden inputkilde, medtages.

Dette er nyttigt til filtrering af data baseret på en referencehændelse eller et tidsstempel fra en anden tracker.
		]],
		["nl"] = [[
Filtert gegevenspunten uit de eerste invoerbron zodat alleen punten worden opgenomen die na het laatste gegevenspunt in de tweede invoerbron plaatsvinden.

Dit is handig om gegevens te filteren op basis van een referentiegebeurtenis of tijdstempel uit een andere tracker.
		]],
		["et"] = [[
Filtreerib esimese sisendallika andmepunkte, jättes alles ainult need, mis esinevad pärast teise sisendallika viimast andmepunkti.

See on kasulik andmete filtreerimiseks teise jälgija viitesündmuse või ajatempli alusel.
		]],
		["fil"] = [[
Sinasala ang mga data point mula sa unang input source upang isama lamang ang mga naganap pagkatapos ng huling data point sa ikalawang input source.

Kapaki-pakinabang ito para sa pagsala ng data batay sa reference event o timestamp mula sa ibang tracker.
		]],
		["fi"] = [[
Suodattaa ensimmäisestä syötelähteestä vain ne datapisteet, jotka ovat toisen syötelähteen viimeisen datapisteen jälkeen.

Tämä on hyödyllistä, kun tietoja suodatetaan toisen seurannan viitetapahtuman tai aikaleiman perusteella.
		]],
		["fr"] = [[
Filtre les points de données de la première source d’entrée pour ne conserver que ceux qui surviennent après le dernier point de données de la seconde source d’entrée.

Utile pour filtrer les données selon un événement ou un horodatage de référence provenant d’un autre tracker.
		]],
		["gl"] = [[
Filtra os puntos de datos da primeira fonte de entrada para incluír só os que se produzan despois do último punto de datos da segunda fonte de entrada.

É útil para filtrar datos en función dun evento ou momento de referencia doutro tracker.
		]],
		["ka"] = [[
პირველი შემავალი წყაროდან ტოვებს მხოლოდ იმ მონაცემთა წერტილებს, რომლებიც მეორე შემავალ წყაროში ბოლო მონაცემის წერტილის შემდეგ გვხვდება.

ეს სასარგებლოა მონაცემების სხვა ტრეკერიდან მიღებულ საორიენტაციო მოვლენაზე ან დროის ნიშნულზე დაფუძნებით გასაფილტრად.
		]],
		["de"] = [[
Filtert Datenpunkte aus der ersten Eingabequelle so, dass nur diejenigen enthalten sind, die nach dem letzten Datenpunkt der zweiten Eingabequelle liegen.

Dies ist nützlich, um Daten anhand eines Referenzereignisses oder Zeitstempels aus einem anderen Tracker zu filtern.
		]],
		["el"] = [[
Φιλτράρει τα σημεία δεδομένων από την πρώτη πηγή εισόδου ώστε να περιλαμβάνει μόνο όσα συμβαίνουν μετά το τελευταίο σημείο δεδομένων της δεύτερης πηγής εισόδου.

Αυτό είναι χρήσιμο για το φιλτράρισμα δεδομένων με βάση ένα συμβάν ή μια χρονική σήμανση αναφοράς από άλλο tracker.
		]],
		["gu"] = [[
પ્રથમ ઇનપુટ સ્રોતના ડેટા પોઇન્ટ્સમાંથી માત્ર તે પોઇન્ટ્સ રાખે છે જે બીજા ઇનપુટ સ્રોતના છેલ્લા ડેટા પોઇન્ટ પછી આવે છે.

બીજા ટ્રૅકરમાંથી મળેલી સંદર્ભ ઘટના અથવા ટાઇમસ્ટેમ્પના આધારે ડેટા ફિલ્ટર કરવા માટે આ ઉપયોગી છે.
		]],
		["hi"] = [[
पहले इनपुट स्रोत के डेटा पॉइंट को केवल उन पॉइंट तक सीमित करता है जो दूसरे इनपुट स्रोत के अंतिम डेटा पॉइंट के बाद आते हैं।

यह किसी अन्य ट्रैकर के संदर्भ इवेंट या टाइमस्टैम्प के आधार पर डेटा फ़िल्टर करने के लिए उपयोगी है।
		]],
		["hu"] = [[
Az első adatforrás adatpontjai közül csak azokat tartja meg, amelyek a második adatforrás utolsó adatpontja után következnek.

Ez hasznos egy másik nyomkövetőből származó referenciaesemény vagy időbélyeg alapján történő adatszűréshez.
		]],
		["is"] = [[
Síar gagnapunkta frá fyrsta inntaksgjafanum þannig að aðeins þeir sem eiga sér stað eftir síðasta gagnapunkt í seinni inntaksgjafanum séu með.

Þetta er gagnlegt til að sía gögn út frá viðburði eða tímamerki úr öðrum rekjara.
		]],
		["id"] = [[
Menyaring titik data dari sumber masukan pertama agar hanya mencakup titik data yang terjadi setelah titik data terakhir dalam sumber masukan kedua.

Ini berguna untuk menyaring data berdasarkan peristiwa atau stempel waktu referensi dari tracker lain.
		]],
		["it"] = [[
Filtra i punti dati della prima origine di input includendo solo quelli che si verificano dopo l'ultimo punto dati della seconda origine di input.

È utile per filtrare i dati in base a un evento o timestamp di riferimento di un altro tracker.
		]],
		["ja"] = [[
1つ目の入力ソースのデータポイントを、2つ目の入力ソースの最後のデータポイントより後に発生したものだけに絞り込みます。

別のトラッカーの基準イベントやタイムスタンプに基づいてデータを絞り込むのに便利です。
		]],
		["kn"] = [[
ಮೊದಲ ಇನ್‌ಪುಟ್ ಮೂಲದ ಡೇಟಾ ಬಿಂದುಗಳಲ್ಲಿ, ಎರಡನೇ ಇನ್‌ಪುಟ್ ಮೂಲದ ಕೊನೆಯ ಡೇಟಾ ಬಿಂದುವಿನ ನಂತರ ಸಂಭವಿಸುವವುಗಳನ್ನು ಮಾತ್ರ ಒಳಗೊಂಡಂತೆ ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ.

ಇನ್ನೊಂದು ಟ್ರ್ಯಾಕರ್‌ನ ಉಲ್ಲೇಖ ಘಟನೆ ಅಥವಾ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್ ಆಧರಿಸಿ ಡೇಟಾವನ್ನು ಫಿಲ್ಟರ್ ಮಾಡಲು ಇದು ಉಪಯುಕ್ತವಾಗಿದೆ.
		]],
		["kk"] = [[
Бірінші кіріс көзіндегі дерек нүктелерінен екінші кіріс көзіндегі соңғы дерек нүктесінен кейін болғандарын ғана қалдырады.

Бұл басқа трекердегі анықтамалық оқиға немесе уақыт белгісі негізінде деректерді сүзуге пайдалы.
		]],
		["km"] = [[
ត្រងចំណុចទិន្នន័យពីប្រភពបញ្ចូលទីមួយ ដោយរក្សាទុកតែចំណុចដែលកើតឡើងបន្ទាប់ពីចំណុចទិន្នន័យចុងក្រោយក្នុងប្រភពបញ្ចូលទីពីរ។

វាមានប្រយោជន៍សម្រាប់ត្រងទិន្នន័យដោយផ្អែកលើព្រឹត្តិការណ៍យោង ឬត្រាពេលវេលាពី Tracker មួយផ្សេងទៀត។
		]],
		["ko"] = [[
첫 번째 입력 소스의 데이터 포인트 중 두 번째 입력 소스의 마지막 데이터 포인트 이후에 발생한 항목만 남깁니다.

다른 트래커의 기준 이벤트나 타임스탬프를 바탕으로 데이터를 필터링할 때 유용합니다.
		]],
		["ky"] = [[
Биринчи кириш булагындагы маалымат чекиттеринен экинчи кириш булагындагы акыркы маалымат чекитинен кийин болгондорун гана калат.

Бул башка трекердеги шилтеме окуясынын же убакыт белгисинин негизинде маалыматты чыпкалоо үчүн пайдалуу.
		]],
		["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນຈາກແຫຼ່ງຂໍ້ມູນທຳອິດ ໃຫ້ລວມສະເພາະຈຸດຂໍ້ມູນທີ່ເກີດຫຼັງຈຸດຂໍ້ມູນສຸດທ້າຍໃນແຫຼ່ງຂໍ້ມູນທີສອງ.

ມີປະໂຫຍດສຳລັບການກັ່ນຕອງຂໍ້ມູນຕາມເຫດການອ້າງອີງ ຫຼື ເວລາຈາກ Tracker ອື່ນ.
		]],
		["lv"] = [[
Filtrē pirmā ievades avota datu punktus, iekļaujot tikai tos, kas ir pēc pēdējā datu punkta otrajā ievades avotā.

Tas ir noderīgi, lai filtrētu datus, pamatojoties uz atsauces notikumu vai laika zīmogu no cita izsekotāja.
		]],
		["lt"] = [[
Filtruoja pirmojo įvesties šaltinio duomenų taškus, palikdama tik tuos, kurie įvyksta po paskutinio duomenų taško antrajame įvesties šaltinyje.

Tai naudinga filtruojant duomenis pagal nuorodos įvykį arba laiko žymą iš kito sekiklio.
		]],
		["mk"] = [[
Ги филтрира точките на податоци од првиот извор така што ги вклучува само оние што се по последната точка на податоци во вториот извор.

Ова е корисно за филтрирање податоци врз основа на референтен настан или временски печат од друг tracker.
		]],
		["ms"] = [[
Menapis titik data daripada sumber input pertama supaya hanya titik data yang berlaku selepas titik data terakhir dalam sumber input kedua disertakan.

Ini berguna untuk menapis data berdasarkan peristiwa rujukan atau cap masa daripada penjejak lain.
		]],
		["ml"] = [[
ആദ്യ ഇൻപുട്ട് ഉറവിടത്തിലെ ഡാറ്റാ പോയിന്റുകളിൽ, രണ്ടാമത്തെ ഇൻപുട്ട് ഉറവിടത്തിലെ അവസാന ഡാറ്റാ പോയിന്റിന് ശേഷം സംഭവിക്കുന്നവ മാത്രം ഉൾപ്പെടുത്തുന്നു.

മറ്റൊരു tracker-ൽ നിന്നുള്ള റഫറൻസ് ഇവന്റ് അല്ലെങ്കിൽ timestamp അടിസ്ഥാനമാക്കി ഡാറ്റ ഫിൽട്ടർ ചെയ്യാൻ ഇത് സഹായകരമാണ്.
		]],
		["mr"] = [[
पहिल्या इनपुट स्रोतामधील डेटा पॉइंट्सपैकी फक्त दुसऱ्या इनपुट स्रोतामधील शेवटच्या डेटा पॉइंटनंतर आलेले डेटा पॉइंट्स ठेवतो.

दुसऱ्या ट्रॅकरमधील संदर्भ इव्हेंट किंवा टाइमस्टॅम्पवर आधारित डेटा फिल्टर करण्यासाठी हे उपयुक्त आहे.
		]],
		["mn"] = [[
Эхний оролтын эх үүсвэрийн өгөгдлийн цэгүүдээс хоёр дахь эх үүсвэрийн сүүлийн өгөгдлийн цэгийн дараа тохиолдсоныг л үлдээнэ.

Энэ нь өөр Tracker-ийн лавлах үйл явдал эсвэл цагийн тэмдэгт үндэслэн өгөгдөл шүүхэд ашигтай.
		]],
		["ne"] = [[
पहिलो इनपुट स्रोतका डेटा बिन्दुमध्ये दोस्रो इनपुट स्रोतको अन्तिम डेटा बिन्दुपछि भएका बिन्दुहरू मात्र राखेर फिल्टर गर्छ।

अर्को ट्र्याकरको सन्दर्भ घटना वा टाइमस्ट्याम्पका आधारमा डेटा फिल्टर गर्न यो उपयोगी हुन्छ।
		]],
		["no"] = [[
Filtrerer datapunkter fra den første inndatakilden slik at bare de som forekommer etter det siste datapunktet i den andre inndatakilden, inkluderes.

Dette er nyttig for å filtrere data basert på en referansehendelse eller et tidsstempel fra en annen sporer.
		]],
		["pl"] = [[
Filtruje punkty danych z pierwszego źródła wejściowego, pozostawiając tylko te, które występują po ostatnim punkcie danych z drugiego źródła wejściowego.

Jest to przydatne do filtrowania danych na podstawie zdarzenia referencyjnego lub znacznika czasu z innego trackera.
		]],
		["pt"] = [[
Filtra os pontos de dados da primeira fonte de entrada para incluir apenas os que ocorrem depois do último ponto de dados da segunda fonte de entrada.

Isto é útil para filtrar dados com base num evento ou carimbo de data/hora de referência de outro tracker.
		]],
		["pa"] = [[
ਪਹਿਲੇ ਇਨਪੁੱਟ ਸਰੋਤ ਦੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿੱਚੋਂ ਸਿਰਫ਼ ਉਹ ਰੱਖਦਾ ਹੈ ਜੋ ਦੂਜੇ ਇਨਪੁੱਟ ਸਰੋਤ ਦੇ ਆਖਰੀ ਡਾਟਾ ਪੁਆਇੰਟ ਤੋਂ ਬਾਅਦ ਆਉਂਦੇ ਹਨ।

ਇਹ ਕਿਸੇ ਹੋਰ ਟ੍ਰੈਕਰ ਦੇ ਹਵਾਲਾ ਇਵੈਂਟ ਜਾਂ ਟਾਈਮਸਟੈਂਪ ਦੇ ਆਧਾਰ ’ਤੇ ਡਾਟਾ ਫਿਲਟਰ ਕਰਨ ਲਈ ਲਾਭਦਾਇਕ ਹੈ।
		]],
		["ro"] = [[
Filtrează punctele de date din prima sursă de intrare pentru a le include doar pe cele care apar după ultimul punct de date din a doua sursă de intrare.

Este util pentru filtrarea datelor pe baza unui eveniment sau marcaj temporal de referință dintr-un alt tracker.
		]],
		["rm"] = [[
Filtra ils puncts da datas da la funtauna d’input emprimara per includer mo quels che vegnan suenter l’ultim punct da datas da la segunda funtauna d’input.

Quai è util per filtrar datas tenor in eveniment da referenza u in timestamp d’in auter tracker.
		]],
		["ru"] = [[
Фильтрует точки данных из первого источника, оставляя только те, которые произошли после последней точки данных во втором источнике.

Это удобно для фильтрации данных по событию или временной метке из другого трекера.
		]],
		["sr"] = [[
Filtrira tačke podataka iz prvog izvora tako da obuhvati samo one koje se javljaju nakon poslednje tačke podataka u drugom izvoru.

Ovo je korisno za filtriranje podataka na osnovu referentnog događaja ili vremenske oznake iz drugog trekera.
		]],
		["si"] = [[
පළමු ආදාන මූලාශ්‍රයේ දත්ත ලක්ෂ්‍යවලින්, දෙවන ආදාන මූලාශ්‍රයේ අවසන් දත්ත ලක්ෂ්‍යයෙන් පසු සිදුවන ඒවා පමණක් ඇතුළත් කරයි.

වෙනත් tracker එකක යොමු සිදුවීමක් හෝ වේලාමුද්‍රාවක් මත පදනම්ව දත්ත පෙරහන් කිරීමට මෙය ප්‍රයෝජනවත් වේ.
		]],
		["sk"] = [[
Filtruje údajové body z prvého vstupného zdroja tak, aby obsahoval iba tie, ktoré nastali po poslednom údajovom bode v druhom vstupnom zdroji.

Je to užitočné na filtrovanie údajov podľa referenčnej udalosti alebo časovej pečiatky z iného trackera.
		]],
		["sl"] = [[
Filtrira podatkovne točke iz prvega vira in vključi le tiste, ki se pojavijo po zadnji podatkovni točki v drugem viru.

To je uporabno za filtriranje podatkov na podlagi referenčnega dogodka ali časovnega žiga iz drugega sledilnika.
		]],
		["es"] = [[
Filtra los puntos de datos de la primera fuente de entrada para incluir solo los que ocurren después del último punto de datos de la segunda fuente de entrada.

Es útil para filtrar datos según un evento o una marca de tiempo de referencia de otro tracker.
		]],
		["sw"] = [[
Huchuja nukta za data kutoka chanzo cha kwanza cha ingizo ili kujumuisha zile zinazotokea baada ya nukta ya mwisho ya data katika chanzo cha pili cha ingizo pekee.

Hii ni muhimu kwa kuchuja data kulingana na tukio la rejeleo au muhuri wa muda kutoka tracker nyingine.
		]],
		["sv"] = [[
Filtrerar datapunkter från den första indatakällan så att endast de som inträffar efter den senaste datapunkten i den andra indatakällan inkluderas.

Detta är användbart för att filtrera data baserat på en referenshändelse eller tidsstämpel från en annan tracker.
		]],
		["ta"] = [[
முதல் உள்ளீட்டு மூலத்திலுள்ள தரவுப் புள்ளிகளில், இரண்டாவது உள்ளீட்டு மூலத்தின் கடைசி தரவுப் புள்ளிக்குப் பிறகு நிகழ்பவற்றை மட்டும் சேர்க்க வடிகட்டுகிறது.

மற்றொரு tracker-இலிருந்து வரும் குறிப்பு நிகழ்வு அல்லது நேரமுத்திரையின் அடிப்படையில் தரவை வடிகட்ட இது பயனுள்ளதாகும்.
		]],
		["te"] = [[
మొదటి ఇన్‌పుట్ మూలంలోని డేటా పాయింట్లలో, రెండో ఇన్‌పుట్ మూలంలోని చివరి డేటా పాయింట్ తర్వాత జరిగేవాటిని మాత్రమే ఉంచుతుంది.

మరొక ట్రాకర్‌లోని సూచన ఈవెంట్ లేదా టైమ్‌స్టాంప్ ఆధారంగా డేటాను ఫిల్టర్ చేయడానికి ఇది ఉపయోగకరం.
		]],
		["th"] = [[
กรองจุดข้อมูลจากแหล่งข้อมูลอินพุตแรกให้เหลือเฉพาะจุดที่เกิดขึ้นหลังจุดข้อมูลล่าสุดในแหล่งข้อมูลอินพุตที่สอง

มีประโยชน์สำหรับกรองข้อมูลโดยอิงตามเหตุการณ์หรือเวลาที่อ้างอิงจาก Tracker อื่น
		]],
		["tr"] = [[
İlk giriş kaynağındaki veri noktalarını, yalnızca ikinci giriş kaynağındaki son veri noktasından sonra gerçekleşenleri içerecek şekilde filtreler.

Bu, verileri başka bir takipçideki referans olayına veya zaman damgasına göre filtrelemek için kullanışlıdır.
		]],
		["uk"] = [[
Фільтрує точки даних із першого джерела, залишаючи лише ті, що відбулися після останньої точки даних у другому джерелі.

Це корисно для фільтрування даних на основі еталонної події або часової мітки з іншого трекера.
		]],
		["vi"] = [[
Lọc các điểm dữ liệu từ nguồn đầu vào thứ nhất để chỉ giữ lại những điểm xảy ra sau điểm dữ liệu cuối cùng trong nguồn đầu vào thứ hai.

Hữu ích khi lọc dữ liệu dựa trên một sự kiện hoặc dấu thời gian tham chiếu từ tracker khác.
		]],
	},
	config = {},

	-- Generator function
	generator = function(sources, config)
		local source1 = sources[1]
		local source2 = sources[2]
		local cutoff_timestamp = nil

		return function()
			-- Initialize cutoff on first call
			if cutoff_timestamp == nil then
				local reference_point = source2.dp()
				cutoff_timestamp = reference_point and reference_point.timestamp
			end

			-- Get next point from source1 and check if it's after cutoff
			local data_point = source1.dp()
			if not data_point then
				return nil
			end

			-- Data points are in reverse chronological order, so "after" means greater timestamp
			if not cutoff_timestamp or data_point.timestamp > cutoff_timestamp then
				return data_point
			end

			-- If the data point is not after the cutoff we're done
			return nil
		end
	end,
}
