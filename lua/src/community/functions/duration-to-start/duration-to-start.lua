-- Move duration data points from the end of their duration to the start.
-- Duration values in Track & Graph are expressed in seconds, while timestamps are milliseconds.

local core = require("tng.core")

-- Input data points arrive newest-first. Negative duration values can move an arbitrarily old
-- input point to an arbitrarily new output timestamp, so the complete input must be transformed
-- before its chronological output order can be known.
local function comes_before(left, right)
	if left.data_point.timestamp ~= right.data_point.timestamp then
		return left.data_point.timestamp > right.data_point.timestamp
	end
	return left.input_order < right.input_order
end

return {
	id = "duration-to-start",
	version = "1.0.4",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Move Duration to Start",
		["af"] = "Skuif Duur na Begin",
		["sq"] = "Zhvendos kohëzgjatjen në fillim",
		["am"] = "ቆይታን ወደ መጀመሪያ አንቀሳቅስ",
		["hy"] = "Տևողությունը տեղափոխել սկիզբ",
		["az"] = "Müddəti başlanğıca keçir",
		["bn"] = "সময়কাল শুরুতে সরান",
		["eu"] = "Eraman iraupena hasierara",
		["be"] = "Перамясціць працягласць да пачатку",
		["bg"] = "Преместване на продължителността към началото",
		["my"] = "ကြာချိန်ကို အစသို့ ရွှေ့ရန်",
		["ca"] = "Mou la durada a l’inici",
		["zh-Hans"] = "将时长移至开始时间",
		["zh-Hant"] = "將持續時間移至開始時間",
		["hr"] = "Premjesti trajanje na početak",
		["cs"] = "Přesunout délku na začátek",
		["da"] = "Flyt varighed til start",
		["nl"] = "Duur naar begin verplaatsen",
		["et"] = "Teisalda kestus algusse",
		["fil"] = "Ilipat ang Tagal sa Simula",
		["fi"] = "Siirrä kesto alkuun",
		["fr"] = "Déplacer la durée au début",
		["gl"] = "Mover a duración ao inicio",
		["ka"] = "ხანგრძლივობის დასაწყისზე გადატანა",
		["de"] = "Dauer an den Anfang verschieben",
		["el"] = "Μετακίνηση διάρκειας στην αρχή",
		["gu"] = "અવધિને શરૂઆત પર ખસેડો",
		["hi"] = "अवधि को शुरुआत पर ले जाएँ",
		["hu"] = "Időtartam áthelyezése a kezdethez",
		["is"] = "Færa tímalengd að upphafi",
		["id"] = "Pindahkan Durasi ke Awal",
		["it"] = "Sposta la durata all'inizio",
		["ja"] = "継続時間を開始時点に移動",
		["kn"] = "ಅವಧಿಯನ್ನು ಪ್ರಾರಂಭಕ್ಕೆ ಸರಿಸಿ",
		["kk"] = "Ұзақтықты басталуына жылжыту",
		["km"] = "ផ្លាស់ទីរយៈពេលទៅដើម",
		["ko"] = "기간을 시작 시점으로 이동",
		["ky"] = "Узактыкты башына жылдыруу",
		["lo"] = "ຍ້າຍໄລຍະເວລາໄປຈຸດເລີ່ມຕົ້ນ",
		["lv"] = "Pārvietot ilgumu uz sākumu",
		["lt"] = "Perkelti trukmę į pradžią",
		["mk"] = "Премести го времетраењето на почетокот",
		["ms"] = "Pindahkan Tempoh ke Permulaan",
		["ml"] = "ദൈർഘ്യം ആരംഭത്തിലേക്ക് മാറ്റുക",
		["mr"] = "कालावधी सुरुवातीला हलवा",
		["mn"] = "Хугацааг эхлэл рүү шилжүүлэх",
		["ne"] = "अवधिलाई सुरुतिर सार्नुहोस्",
		["no"] = "Flytt varighet til start",
		["pl"] = "Przenieś czas trwania na początek",
		["pt"] = "Mover duração para o início",
		["pa"] = "ਮਿਆਦ ਨੂੰ ਸ਼ੁਰੂਆਤ ਵੱਲ ਲਿਜਾਓ",
		["ro"] = "Mută durata la început",
		["rm"] = "Spustar la durada al cumenzament",
		["ru"] = "Переместить длительность к началу",
		["sr"] = "Premesti trajanje na početak",
		["si"] = "කාලසීමාව ආරම්භයට ගෙන යන්න",
		["sk"] = "Presunúť trvanie na začiatok",
		["sl"] = "Premakni trajanje na začetek",
		["es"] = "Mover duración al inicio",
		["sw"] = "Hamisha Muda hadi Mwanzo",
		["sv"] = "Flytta varaktighet till början",
		["ta"] = "கால அளவை தொடக்கத்திற்கு நகர்த்து",
		["te"] = "వ్యవధిని ప్రారంభానికి తరలించు",
		["th"] = "ย้ายระยะเวลาไปยังจุดเริ่มต้น",
		["tr"] = "Süreyi Başlangıca Taşı",
		["uk"] = "Перемістити тривалість на початок",
		["vi"] = "Chuyển thời lượng về thời điểm bắt đầu",
	},
	description = {
		["en"] = [[
Moves each duration data point's timestamp from the end of its duration to the start.

Expects duration data points as input. Values, labels, and notes are preserved. The output maintains chronological order, including when durations overlap.

**Performance warning:** This function must load and sort all input data points before producing output, so it may be slow and use more memory with large datasets.
		]],
		["af"] = [[
Skuif elke duurndatapunt se tydstempel van die einde van sy duur na die begin.

Verwag duurndatapunte as invoer. Waardes, etikette en notas word behou. Die uitvoer behou chronologiese volgorde, ook wanneer durings oorvleuel.

**Werkverrigtingwaarskuwing:** Hierdie funksie moet alle invoerdatapunte laai en sorteer voordat uitvoer gelewer word, dus kan dit stadig wees en meer geheue gebruik met groot datastelle.
		]],
		["sq"] = [[
Zhvendos vulën kohore të çdo pike të të dhënave me kohëzgjatje nga fundi i kohëzgjatjes në fillim.

Kërkon pika të të dhënave me kohëzgjatje si hyrje. Vlerat, etiketat dhe shënimet ruhen. Dalja ruan rendin kronologjik, edhe kur kohëzgjatjet mbivendosen.

**Paralajmërim për performancën:** Ky funksion duhet të ngarkojë dhe rendisë të gjitha pikat hyrëse të të dhënave përpara se të prodhojë daljen, ndaj mund të jetë i ngadaltë dhe të përdorë më shumë memorie me grupe të mëdha të dhënash.
		]],
		["am"] = [[
የእያንዳንዱን የቆይታ የውሂብ ነጥብ የጊዜ ማህተም ከቆይታው መጨረሻ ወደ መጀመሪያው ያንቀሳቅሳል።

የቆይታ የውሂብ ነጥቦችን እንደ ግብዓት ይጠብቃል። ዋጋዎች፣ መለያዎች እና ማስታወሻዎች ይጠበቃሉ። ቆይታዎች ቢደራረቡም ውጤቱ የዘመን ቅደም ተከተልን ይጠብቃል።

**የአፈጻጸም ማስጠንቀቂያ፦** ይህ ፋንክሽን ውጤት ከማምጣቱ በፊት ሁሉንም የግብዓት የውሂብ ነጥቦች መጫንና መደርደር አለበት፤ ስለዚህ በትልቅ የውሂብ ስብስቦች ላይ ዝግ ሊል እና ተጨማሪ ማህደረ ትውስታ ሊጠቀም ይችላል።
		]],
		["hy"] = [[
Յուրաքանչյուր տևողության տվյալակետի ժամանակը տևողության ավարտից տեղափոխում է սկիզբ։

Մուտքում ակնկալվում են տևողության տվյալակետեր։ Արժեքները, պիտակները և նշումները պահպանվում են։ Ելքը պահպանում է ժամանակագրական կարգը, այդ թվում՝ համընկնող տևողությունների դեպքում։

**Արտադրողականության նախազգուշացում․** այս ֆունկցիան նախքան ելք ստեղծելը պետք է բեռնի և դասավորի բոլոր մուտքային տվյալակետերը, ուստի մեծ տվյալների հավաքածուների դեպքում կարող է դանդաղ աշխատել և ավելի շատ հիշողություն օգտագործել։
		]],
		["az"] = [[
Hər müddət məlumat nöqtəsinin zaman damğasını müddətin sonundan başlanğıcına keçirir.

Giriş kimi müddət məlumat nöqtələri gözlənilir. Qiymətlər, etiketlər və qeydlər qorunur. Müddətlər üst-üstə düşdükdə belə, çıxış xronoloji ardıcıllığı qoruyur.

**Məhsuldarlıq xəbərdarlığı:** Çıxış yaratmazdan əvvəl bu funksiya bütün giriş məlumat nöqtələrini yükləyib çeşidləməlidir; buna görə böyük məlumat dəstlərində yavaş işləyə və daha çox yaddaş istifadə edə bilər.
		]],
		["bn"] = [[
প্রতিটি সময়কাল ডেটা পয়েন্টের টাইমস্ট্যাম্পকে সময়কালের শেষ থেকে শুরুতে সরায়।

ইনপুট হিসেবে সময়কাল ডেটা পয়েন্ট প্রত্যাশা করে। মান, লেবেল ও নোট অপরিবর্তিত থাকে। সময়কাল পরস্পরকে অতিক্রম করলেও আউটপুট কালানুক্রমিক ক্রম বজায় রাখে।

**কার্যক্ষমতা সতর্কতা:** আউটপুট তৈরির আগে এই ফাংশনকে সব ইনপুট ডেটা পয়েন্ট লোড ও সাজাতে হয়, তাই বড় ডেটাসেটে এটি ধীর হতে পারে এবং বেশি মেমরি ব্যবহার করতে পারে।
		]],
		["eu"] = [[
Iraupen-datu-puntu bakoitzaren denbora-zigilua iraupenaren amaieratik hasierara eramaten du.

Iraupen-datu-puntuak espero ditu sarrera gisa. Balioak, etiketak eta oharrak mantentzen dira. Irteerak ordena kronologikoa mantentzen du, iraupenak gainjartzen direnean ere.

**Errendimendu-oharra:** Funtzio honek sarrerako datu-puntu guztiak kargatu eta ordenatu behar ditu irteera sortu aurretik; beraz, motelagoa izan daiteke eta memoria gehiago erabil dezake datu multzo handiekin.
		]],
		["be"] = [[
Перамяшчае метку часу кожнай кропкі даных працягласці з канца яе працягласці ў пачатак.

Чакае на ўваходзе кропкі даных працягласці. Значэнні, ярлыкі і заўвагі захоўваюцца. Вывад захоўвае храналагічны парадак, у тым ліку пры перакрыцці працягласцей.

**Папярэджанне аб прадукцыйнасці:** перад стварэннем вываду гэтая функцыя павінна загрузіць і адсартаваць усе ўваходныя кропкі даных, таму на вялікіх наборах даных яна можа працаваць павольна і выкарыстоўваць больш памяці.
		]],
		["bg"] = [[
Премества времевия отпечатък на всяка точка от данни за продължителност от края на продължителността към началото.

Очаква точки от данни за продължителност като вход. Стойностите, етикетите и бележките се запазват. Изходът запазва хронологичния ред, включително когато продължителностите се припокриват.

**Предупреждение за производителността:** Тази функция трябва да зареди и сортира всички входни точки от данни, преди да генерира изход, затова при големи набори от данни може да работи бавно и да използва повече памет.
		]],
		["my"] = [[
ကြာချိန်ဒေတာမှတ်တစ်ခုချင်းစီ၏ အချိန်တံဆိပ်ကို ကြာချိန်အဆုံးမှ အစသို့ ရွှေ့သည်။

ကြာချိန်ဒေတာမှတ်များကို ထည့်သွင်းရန် လိုအပ်သည်။ တန်ဖိုးများ၊ အညွှန်းများနှင့် မှတ်စုများကို ထိန်းသိမ်းသည်။ ကြာချိန်များ ထပ်နေသည့်အခါတွင်ပါ ထွက်ဒေတာသည် အချိန်အစဉ်လိုက် ဖြစ်နေမည်။

**စွမ်းဆောင်ရည် သတိပေးချက်:** ထွက်ဒေတာမထုတ်မီ ထည့်သွင်းဒေတာမှတ်အားလုံးကို တင်ပြီး စီရမည်ဖြစ်သောကြောင့် ဒေတာအစုကြီးများတွင် နှေးကွေးပြီး မှတ်ဉာဏ်ပိုသုံးနိုင်သည်။
		]],
		["ca"] = [[
Mou la marca de temps de cada punt de dades de durada del final de la durada a l’inici.

Espera punts de dades de durada com a entrada. Els valors, les etiquetes i les notes es conserven. La sortida manté l’ordre cronològic, fins i tot quan les durades se solapen.

**Avís de rendiment:** Aquesta funció ha de carregar i ordenar tots els punts de dades d’entrada abans de produir la sortida, de manera que pot ser lenta i utilitzar més memòria amb conjunts de dades grans.
		]],
		["zh-Hans"] = [[
将每个时长数据点的时间戳从时长结束时间移至开始时间。

输入应为时长数据点。值、标签和备注会保留。即使时长重叠，输出仍保持时间顺序。

**性能警告：**此函数必须加载并排序所有输入数据点后才能生成输出，因此处理大型数据集时可能较慢并占用更多内存。
		]],
		["zh-Hant"] = [[
將每個持續時間資料點的時間戳記從持續時間結束時間移至開始時間。

預期輸入為持續時間資料點。值、標籤和備註會予以保留。即使持續時間重疊，輸出仍會維持時間順序。

**效能警告：** 此函式必須先載入並排序所有輸入資料點才能產生輸出，因此處理大型資料集時可能較慢並使用更多記憶體。
		]],
		["hr"] = [[
Premješta vremensku oznaku svake podatkovne točke trajanja s kraja njezina trajanja na početak.

Očekuje podatkovne točke trajanja kao ulaz. Vrijednosti, oznake i bilješke ostaju sačuvane. Izlaz zadržava kronološki redoslijed, čak i kada se trajanja preklapaju.

**Upozorenje o performansama:** Ova funkcija mora učitati i sortirati sve ulazne podatkovne točke prije stvaranja izlaza, pa može biti spora i koristiti više memorije s velikim skupovima podataka.
		]],
		["cs"] = [[
Přesune časové razítko každého datového bodu typu délka z konce jeho délky na začátek.

Očekává vstupní datové body typu délka. Hodnoty, štítky a poznámky se zachovají. Výstup zachová chronologické pořadí, i když se délky překrývají.

**Upozornění na výkon:** Tato funkce musí před vytvořením výstupu načíst a seřadit všechny vstupní datové body, takže u velkých datových sad může být pomalá a využívat více paměti.
		]],
		["da"] = [[
Flytter hvert varighedsdatapunkts tidsstempel fra slutningen af dets varighed til begyndelsen.

Forventer varighedsdatapunkter som input. Værdier, etiketter og noter bevares. Outputtet bevarer kronologisk rækkefølge, også når varigheder overlapper.

**Ydelsesadvarsel:** Denne funktion skal indlæse og sortere alle inputdatapunkter, før outputtet genereres, så den kan være langsom og bruge mere hukommelse med store datasæt.
		]],
		["nl"] = [[
Verplaatst de tijdstempel van elk duurgegevenspunt van het einde van de duur naar het begin.

Verwacht duurgegevenspunten als invoer. Waarden, labels en notities blijven behouden. De uitvoer behoudt de chronologische volgorde, ook wanneer duren elkaar overlappen.

**Waarschuwing voor prestaties:** Deze functie moet alle invoergegevenspunten laden en sorteren voordat de uitvoer wordt gegenereerd. Bij grote datasets kan dit traag zijn en meer geheugen gebruiken.
		]],
		["et"] = [[
Teisaldab iga kestusega andmepunkti ajatempli kestuse lõpust algusesse.

Sisendiks on kestusega andmepunktid. Väärtused, sildid ja märkmed säilitatakse. Väljund säilitab kronoloogilise järjestuse, ka kattuvate kestuste korral.

**Jõudluse hoiatus:** see funktsioon peab enne väljundi loomist laadima ja sortima kõik sisendandmepunktid, mistõttu võib see suurte andmekogumite korral olla aeglane ja kasutada rohkem mälu.
		]],
		["fil"] = [[
Inililipat ang timestamp ng bawat duration data point mula sa dulo ng duration papunta sa simula.

Inaasahan nito ang duration data point bilang input. Pinapanatili ang mga value, label, at note. Pinananatili ng output ang kronolohikal na pagkakasunod-sunod, pati kapag nagkakapatong ang mga duration.

**Babala sa performance:** Kailangang i-load at ayusin ng function na ito ang lahat ng input data point bago gumawa ng output, kaya maaaring mabagal ito at gumamit ng mas maraming memory sa malalaking dataset.
		]],
		["fi"] = [[
Siirtää kunkin kestodatapisteen aikaleiman keston lopusta sen alkuun.

Odottaa syötteenä kestodatapisteitä. Arvot, selitteet ja muistiinpanot säilytetään. Tulokset säilyttävät aikajärjestyksen myös silloin, kun kestot ovat päällekkäisiä.

**Suorituskykyvaroitus:** Tämän funktion on ladattava ja lajiteltava kaikki syötedatapisteet ennen tulosten tuottamista, joten se voi olla suurilla aineistoilla hidas ja käyttää enemmän muistia.
		]],
		["fr"] = [[
Déplace l’horodatage de chaque point de données de type durée de la fin vers le début de sa durée.

Attend des points de données de type durée en entrée. Les valeurs, libellés et notes sont conservés. La sortie conserve l’ordre chronologique, y compris lorsque les durées se chevauchent.

**Avertissement de performance :** Cette fonction doit charger et trier tous les points de données entrants avant de produire la sortie ; elle peut donc être lente et utiliser davantage de mémoire avec de grands ensembles de données.
		]],
		["gl"] = [[
Move o momento de cada punto de datos de duración desde o final da súa duración ata o inicio.

Espera puntos de datos de duración como entrada. Os valores, as etiquetas e as notas consérvanse. A saída mantén a orde cronolóxica, mesmo cando as duracións se solapan.

**Aviso de rendemento:** Esta función debe cargar e ordenar todos os puntos de datos de entrada antes de producir a saída, polo que pode ser lenta e usar máis memoria con conxuntos de datos grandes.
		]],
		["ka"] = [[
თითოეული ხანგრძლივობის მონაცემის წერტილის დროის ნიშნულს ხანგრძლივობის ბოლოდან დასაწყისზე გადააქვს.

შემავალ მონაცემებად მოელის ხანგრძლივობის მონაცემთა წერტილებს. მნიშვნელობები, იარლიყები და შენიშვნები შენარჩუნებულია. გამოტანილი მონაცემები ქრონოლოგიურ თანმიმდევრობას ინარჩუნებს, მათ შორის გადაფარული ხანგრძლივობების შემთხვევაშიც.

**წარმადობის გაფრთხილება:** გამოტანის დაწყებამდე ამ ფუნქციამ უნდა ჩატვირთოს და დაალაგოს ყველა შემავალი მონაცემის წერტილი, ამიტომ დიდ მონაცემთა ნაკრებებზე შეიძლება ნელი იყოს და მეტი მეხსიერება გამოიყენოს.
		]],
		["de"] = [[
Verschiebt den Zeitstempel jedes Dauerdatenpunkts vom Ende seiner Dauer an deren Anfang.

Erwartet Dauerdatenpunkte als Eingabe. Werte, Labels und Notizen bleiben erhalten. Die Ausgabe behält die chronologische Reihenfolge bei, auch wenn sich Dauern überschneiden.

**Leistungshinweis:** Diese Funktion muss alle Eingabedatenpunkte laden und sortieren, bevor sie eine Ausgabe erzeugt. Bei großen Datensätzen kann sie daher langsam sein und mehr Speicher benötigen.
		]],
		["el"] = [[
Μετακινεί τη χρονική σήμανση κάθε σημείου δεδομένων διάρκειας από το τέλος της διάρκειάς του στην αρχή.

Αναμένει σημεία δεδομένων διάρκειας ως είσοδο. Οι τιμές, οι ετικέτες και οι σημειώσεις διατηρούνται. Η έξοδος διατηρεί τη χρονολογική σειρά, ακόμη και όταν οι διάρκειες επικαλύπτονται.

**Προειδοποίηση απόδοσης:** Αυτή η συνάρτηση πρέπει να φορτώσει και να ταξινομήσει όλα τα σημεία δεδομένων εισόδου πριν παράγει έξοδο, επομένως μπορεί να είναι αργή και να χρησιμοποιεί περισσότερη μνήμη με μεγάλα σύνολα δεδομένων.
		]],
		["gu"] = [[
દરેક અવધિ ડેટા પોઇન્ટનો ટાઇમસ્ટેમ્પ તેની અવધિના અંતથી શરૂઆત પર ખસેડે છે.

ઇનપુટ તરીકે અવધિ ડેટા પોઇન્ટ્સ અપેક્ષિત છે. મૂલ્યો, લેબલ્સ અને નોંધો જાળવવામાં આવે છે. અવધિઓ એકબીજા સાથે ઓવરલેપ થાય ત્યારે પણ આઉટપુટ કાળક્રમ જાળવે છે.

**પ્રદર્શન ચેતવણી:** આ ફંક્શન આઉટપુટ બનાવતાં પહેલાં તમામ ઇનપુટ ડેટા પોઇન્ટ્સ લોડ અને સૉર્ટ કરે છે, તેથી મોટા ડેટાસેટ્સ સાથે તે ધીમું હોઈ શકે છે અને વધુ મેમરી વાપરી શકે છે.
		]],
		["hi"] = [[
हर अवधि डेटा पॉइंट के टाइमस्टैम्प को उसकी अवधि के अंत से शुरुआत पर ले जाता है।

इनपुट के रूप में अवधि डेटा पॉइंट अपेक्षित हैं। मान, लेबल और नोट सुरक्षित रहते हैं। अवधि के ओवरलैप होने पर भी आउटपुट कालानुक्रमिक क्रम बनाए रखता है।

**प्रदर्शन चेतावनी:** आउटपुट बनाने से पहले इस फ़ंक्शन को सभी इनपुट डेटा पॉइंट लोड और क्रमबद्ध करने पड़ते हैं, इसलिए बड़े डेटासेट के साथ यह धीमा हो सकता है और अधिक मेमोरी उपयोग कर सकता है।
		]],
		["hu"] = [[
Az egyes időtartam-adatpontok időbélyegét az időtartam végéről a kezdetére helyezi át.

Bemenetként időtartam-adatpontokat vár. Az értékeket, címkéket és megjegyzéseket megőrzi. A kimenet időrendi sorrendben marad, átfedő időtartamok esetén is.

**Teljesítményre vonatkozó figyelmeztetés:** A kimenet előállítása előtt ennek a függvénynek be kell töltenie és sorba kell rendeznie az összes bemeneti adatpontot, ezért nagy adathalmazok esetén lassú lehet és több memóriát használhat.
		]],
		["is"] = [[
Færir tímamerki hvers tímalengdargagnapunkts frá lokum tímalengdar að upphafi hennar.

Býst við tímalengdargagnapunktum sem inntaki. Gildi, merki og athugasemdir eru varðveitt. Úttakið heldur tímaröð, einnig þegar tímalengdir skarast.

**Afkastaviðvörun:** Þessi aðgerð þarf að hlaða inn og raða öllum innkomnum gagnapunktum áður en úttak er búið til, svo hún getur verið hæg og notað meira minni með stór gagnasöfn.
		]],
		["id"] = [[
Memindahkan stempel waktu setiap titik data durasi dari akhir durasinya ke awal.

Mengharapkan titik data durasi sebagai masukan. Nilai, label, dan catatan dipertahankan. Keluaran mempertahankan urutan kronologis, termasuk saat durasi saling tumpang tindih.

**Peringatan performa:** Fungsi ini harus memuat dan mengurutkan semua titik data masukan sebelum menghasilkan keluaran, sehingga dapat berjalan lambat dan menggunakan lebih banyak memori pada kumpulan data besar.
		]],
		["it"] = [[
Sposta il timestamp di ogni punto dati di durata dalla fine della durata all'inizio.

Prevede punti dati di durata come input. Valori, etichette e note vengono mantenuti. L'output mantiene l'ordine cronologico, anche quando le durate si sovrappongono.

**Avviso sulle prestazioni:** questa funzione deve caricare e ordinare tutti i punti dati di input prima di produrre l'output, quindi potrebbe essere lenta e usare più memoria con dataset di grandi dimensioni.
		]],
		["ja"] = [[
各継続時間データポイントのタイムスタンプを、継続時間の終了時点から開始時点に移動します。

入力には継続時間データポイントを指定します。値、ラベル、メモは保持されます。継続時間が重なっている場合も含め、出力は時系列順になります。

**パフォーマンスに関する警告:** この関数は出力を生成する前にすべての入力データポイントを読み込み、並べ替える必要があるため、大規模なデータセットでは処理が遅くなり、メモリ使用量が増える場合があります。
		]],
		["kn"] = [[
ಪ್ರತಿ ಅವಧಿ ಡೇಟಾ ಬಿಂದುವಿನ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್ ಅನ್ನು ಅದರ ಅವಧಿಯ ಅಂತ್ಯದಿಂದ ಪ್ರಾರಂಭಕ್ಕೆ ಸರಿಸುತ್ತದೆ.

ಇನ್‌ಪುಟ್ ಆಗಿ ಅವಧಿ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ನಿರೀಕ್ಷಿಸುತ್ತದೆ. ಮೌಲ್ಯಗಳು, ಲೇಬಲ್‌ಗಳು ಮತ್ತು ಟಿಪ್ಪಣಿಗಳನ್ನು ಉಳಿಸಲಾಗುತ್ತದೆ. ಅವಧಿಗಳು ಒಂದಕ್ಕೊಂದು ಮೀರಿದಾಗಲೂ ಔಟ್‌ಪುಟ್ ಕಾಲಾನುಕ್ರಮವನ್ನು ಉಳಿಸುತ್ತದೆ.

**ಕಾರ್ಯಕ್ಷಮತೆ ಎಚ್ಚರಿಕೆ:** ಔಟ್‌ಪುಟ್ ಉತ್ಪಾದಿಸುವ ಮೊದಲು ಈ ಫಂಕ್ಷನ್ ಎಲ್ಲಾ ಇನ್‌ಪುಟ್ ಡೇಟಾ ಬಿಂದುಗಳನ್ನು ಲೋಡ್ ಮಾಡಿ ವಿಂಗಡಿಸಬೇಕು; ಆದ್ದರಿಂದ ದೊಡ್ಡ ಡೇಟಾಸೆಟ್‌ಗಳಲ್ಲಿ ಇದು ನಿಧಾನವಾಗಬಹುದು ಮತ್ತು ಹೆಚ್ಚು ಮೆಮೊರಿ ಬಳಸಬಹುದು.
		]],
		["kk"] = [[
Әр ұзақтық дерек нүктесінің уақыт белгісін ұзақтық соңынан басталуына жылжытады.

Кіріс ретінде ұзақтық дерек нүктелері күтіледі. Мәндер, жапсырмалар және ескертпелер сақталады. Ұзақтықтар қабаттасқан жағдайда да шығыс хронологиялық ретпен қалады.

**Өнімділік туралы ескерту:** Шығыс жасамас бұрын бұл функция барлық кіріс дерек нүктелерін жүктеп, сұрыптауы керек, сондықтан үлкен деректер жиынтығында баяу жұмыс істеп, көбірек жад қолдануы мүмкін.
		]],
		["km"] = [[
ផ្លាស់ទីត្រាពេលវេលារបស់ចំណុចទិន្នន័យរយៈពេលនីមួយៗ ពីចុងរយៈពេលទៅដើមរយៈពេល។

រំពឹងថាបញ្ចូលចំណុចទិន្នន័យរយៈពេល។ តម្លៃ ស្លាក និងចំណាំត្រូវបានរក្សាទុក។ លទ្ធផលរក្សាលំដាប់តាមកាលប្បវត្តិ រួមទាំងនៅពេលរយៈពេលត្រួតស៊ីគ្នា។

**ការព្រមានអំពីដំណើរការ៖** អនុគមន៍នេះត្រូវផ្ទុក និងតម្រៀបចំណុចទិន្នន័យបញ្ចូលទាំងអស់ មុនពេលបង្កើតលទ្ធផល ដូច្នេះវាអាចយឺត និងប្រើអង្គចងចាំច្រើនជាងមុនជាមួយសំណុំទិន្នន័យធំៗ។
		]],
		["ko"] = [[
각 기간 데이터 포인트의 타임스탬프를 기간의 끝에서 시작으로 이동합니다.

입력은 기간 데이터 포인트여야 합니다. 값, 라벨, 메모는 유지됩니다. 기간이 겹치는 경우에도 출력은 시간순서를 유지합니다.

**성능 경고:** 이 함수는 출력을 생성하기 전에 모든 입력 데이터 포인트를 불러와 정렬하므로, 대규모 데이터세트에서는 느리고 더 많은 메모리를 사용할 수 있습니다.
		]],
		["ky"] = [[
Ар бир узактык маалымат чекитинин убакыт белгисин анын аягынан башына жылдырат.

Кириш катары узактык маалымат чекиттери күтүлөт. Маанилер, энбелгилер жана эскертмелер сакталат. Узактыктар кабатташкан учурда да чыгаруу хронологиялык тартипти сактайт.

**Өндүрүмдүүлүк боюнча эскертүү:** Чыгарууну түзүүдөн мурун бул функция бардык кириш маалымат чекиттерин жүктөп, иреттеши керек, ошондуктан чоң маалымат топтомдорунда жай иштеп, көбүрөөк эстутум колдонушу мүмкүн.
		]],
		["lo"] = [[
ຍ້າຍເວລາຂອງຈຸດຂໍ້ມູນໄລຍະເວລາແຕ່ລະຈຸດຈາກທ້າຍໄລຍະເວລາໄປຫາຈຸດເລີ່ມຕົ້ນ.

ຮັບຈຸດຂໍ້ມູນໄລຍະເວລາເປັນຂາເຂົ້າ. ຄ່າ, ປ້າຍ ແລະ ໝາຍເຫດຈະຖືກຮັກສາໄວ້. ຜົນລັບຈະຮັກສາລຳດັບຕາມເວລາ ລວມເຖິງກໍລະນີທີ່ໄລຍະເວລາຊ້ອນກັນ.

**ຄຳເຕືອນດ້ານປະສິດທິພາບ:** ຟັງຊັນນີ້ຕ້ອງໂຫຼດ ແລະ ຈັດລຽງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດກ່ອນຜະລິດຜົນລັບ ດັ່ງນັ້ນອາດຊ້າ ແລະ ໃຊ້ຄວາມຈຳຫຼາຍຂຶ້ນກັບຊຸດຂໍ້ມູນຂະໜາດໃຫຍ່.
		]],
		["lv"] = [[
Pārvieto katra ilguma datu punkta laika zīmogu no tā ilguma beigām uz sākumu.

Sagaida ilguma datu punktus kā ievadi. Vērtības, etiķetes un piezīmes tiek saglabātas. Izvade saglabā hronoloģisko secību, arī tad, ja ilgumi pārklājas.

**Veiktspējas brīdinājums:** Šai funkcijai pirms izvades izveides jāielādē un jāsakārto visi ievades datu punkti, tāpēc lielām datu kopām tā var darboties lēni un izmantot vairāk atmiņas.
		]],
		["lt"] = [[
Kiekvieno trukmės duomenų taško laiko žymą perkelia iš trukmės pabaigos į pradžią.

Tikimasi, kad įvestis bus trukmės duomenų taškai. Reikšmės, etiketės ir pastabos išsaugomos. Išvestyje išlaikoma chronologinė tvarka, įskaitant persidengiančias trukmes.

**Našumo įspėjimas:** Prieš generuodama išvestį ši funkcija turi įkelti ir surikiuoti visus įvesties duomenų taškus, todėl su dideliais duomenų rinkiniais gali veikti lėtai ir naudoti daugiau atminties.
		]],
		["mk"] = [[
Го преместува временскиот печат на секоја точка на податоци за времетраење од крајот на времетраењето на почетокот.

Очекува точки на податоци за времетраење како влез. Вредностите, ознаките и белешките се зачувуваат. Излезот го задржува хронолошкиот редослед, вклучително и кога времетраењата се преклопуваат.

**Предупредување за перформансите:** Оваа функција мора да ги вчита и подреди сите влезни точки на податоци пред да создаде излез, па може да биде бавна и да користи повеќе меморија со големи множества податоци.
		]],
		["ms"] = [[
Memindahkan cap masa setiap titik data tempoh daripada penghujung tempohnya ke permulaan.

Menjangkakan titik data tempoh sebagai input. Nilai, label dan nota dikekalkan. Output mengekalkan susunan kronologi, termasuk apabila tempoh bertindih.

**Amaran prestasi:** Fungsi ini mesti memuatkan dan mengisih semua titik data input sebelum menghasilkan output, jadi ia mungkin perlahan dan menggunakan lebih banyak memori dengan set data yang besar.
		]],
		["ml"] = [[
ഓരോ duration ഡാറ്റാ പോയിന്റിന്റെയും timestamp അതിന്റെ ദൈർഘ്യത്തിന്റെ അവസാനത്തിൽ നിന്ന് ആരംഭത്തിലേക്ക് മാറ്റുന്നു.

ഇൻപുട്ടായി duration ഡാറ്റാ പോയിന്റുകൾ പ്രതീക്ഷിക്കുന്നു. മൂല്യങ്ങളും ലേബലുകളും കുറിപ്പുകളും നിലനിർത്തുന്നു. duration-കൾ തമ്മിൽ ഓവർലാപ്പ് ഉണ്ടായാലും ഔട്ട്പുട്ട് കാലക്രമം നിലനിർത്തുന്നു.

**പ്രകടന മുന്നറിയിപ്പ്:** ഔട്ട്പുട്ട് സൃഷ്ടിക്കുന്നതിന് മുമ്പ് ഈ ഫംഗ്ഷൻ എല്ലാ ഇൻപുട്ട് ഡാറ്റാ പോയിന്റുകളും ലോഡ് ചെയ്ത് ക്രമീകരിക്കണം; അതിനാൽ വലിയ ഡാറ്റാസെറ്റുകളിൽ ഇത് മന്ദഗതിയിലാകുകയും കൂടുതൽ മെമ്മറി ഉപയോഗിക്കുകയും ചെയ്യാം.
		]],
		["mr"] = [[
प्रत्येक कालावधी डेटा बिंदूचा टाइमस्टॅम्प त्याच्या कालावधीच्या शेवटापासून सुरुवातीपर्यंत हलवते.

इनपुट म्हणून कालावधी डेटा बिंदू अपेक्षित आहेत. मूल्ये, लेबले आणि टिपणे जतन केली जातात. कालावधी एकमेकांवर आच्छादित असतानाही आउटपुट कालानुक्रमिक क्रम राखते.

**कार्यक्षमता इशारा:** आउटपुट तयार करण्यापूर्वी या फंक्शनला सर्व इनपुट डेटा बिंदू लोड करून क्रमवारी लावावी लागते, त्यामुळे मोठ्या डेटासेटसह ते धीमे होऊ शकते आणि अधिक मेमरी वापरू शकते.
		]],
		["mn"] = [[
Хугацааны өгөгдлийн цэг бүрийн цагийн тэмдгийг хугацааны төгсгөлөөс эхлэл рүү шилжүүлнэ.

Оролтод хугацааны өгөгдлийн цэг шаардлагатай. Утга, шошго болон тэмдэглэл хадгалагдана. Хугацаанууд давхцсан ч гаралт он цагийн дарааллыг хадгална.

**Гүйцэтгэлийн анхааруулга:** Гаралт үүсгэхийн өмнө бүх оролтын өгөгдлийн цэгийг ачаалж эрэмбэлэх тул их хэмжээний өгөгдөл дээр удаан ажиллаж, илүү их санах ой ашиглаж болзошгүй.
		]],
		["ne"] = [[
प्रत्येक अवधि डेटा बिन्दुको टाइमस्ट्याम्पलाई अवधिको अन्त्यबाट सुरुतिर सार्छ।

इनपुटका रूपमा अवधि डेटा बिन्दुहरू अपेक्षित हुन्छन्। मान, लेबल र नोटहरू कायम रहन्छन्। अवधि ओभरल्याप हुँदा समेत आउटपुट कालानुक्रमिक क्रम कायम राख्छ।

**कार्यसम्पादन चेतावनी:** आउटपुट उत्पादन गर्नुअघि यस फङ्सनले सबै इनपुट डेटा बिन्दु लोड र क्रमबद्ध गर्नुपर्छ, त्यसैले ठूलो डेटासेटमा यो ढिलो हुन सक्छ र बढी मेमोरी प्रयोग गर्न सक्छ।
		]],
		["no"] = [[
Flytter tidsstempelet til hvert varighetsdatapunkt fra slutten av varigheten til starten.

Forventer varighetsdatapunkter som inndata. Verdier, etiketter og notater bevares. Utdataene beholder kronologisk rekkefølge, også når varigheter overlapper.

**Ytelsesadvarsel:** Denne funksjonen må laste inn og sortere alle inngående datapunkter før den produserer utdata, så den kan være treg og bruke mer minne med store datasett.
		]],
		["pl"] = [[
Przenosi znacznik czasu każdego punktu danych typu czas trwania z końca jego trwania na początek.

Oczekuje jako danych wejściowych punktów danych typu czas trwania. Wartości, etykiety i notatki są zachowywane. Dane wyjściowe zachowują kolejność chronologiczną, także gdy czasy trwania nakładają się na siebie.

**Ostrzeżenie dotyczące wydajności:** Ta funkcja musi wczytać i posortować wszystkie wejściowe punkty danych przed wygenerowaniem danych wyjściowych, dlatego przy dużych zbiorach danych może działać wolno i zużywać więcej pamięci.
		]],
		["pt"] = [[
Move o carimbo de data/hora de cada ponto de dados de duração do fim da duração para o início.

Espera pontos de dados de duração como entrada. Os valores, rótulos e notas são preservados. A saída mantém a ordem cronológica, inclusive quando as durações se sobrepõem.

**Aviso de desempenho:** Esta função tem de carregar e ordenar todos os pontos de dados de entrada antes de produzir a saída, pelo que pode ser lenta e usar mais memória com grandes conjuntos de dados.
		]],
		["pa"] = [[
ਹਰੇਕ ਮਿਆਦ ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਦਾ ਟਾਈਮਸਟੈਂਪ ਉਸਦੀ ਮਿਆਦ ਦੇ ਅੰਤ ਤੋਂ ਸ਼ੁਰੂਆਤ ਵੱਲ ਲਿਜਾਂਦਾ ਹੈ।

ਇਨਪੁੱਟ ਵਜੋਂ ਮਿਆਦ ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੀ ਉਮੀਦ ਕਰਦਾ ਹੈ। ਮੁੱਲ, ਲੇਬਲ ਅਤੇ ਨੋਟ ਸੁਰੱਖਿਅਤ ਰਹਿੰਦੇ ਹਨ। ਆਉਟਪੁੱਟ ਕਾਲਕ੍ਰਮਕ ਕ੍ਰਮ ਬਣਾਈ ਰੱਖਦਾ ਹੈ, ਭਾਵੇਂ ਮਿਆਦਾਂ ਇੱਕ-ਦੂਜੇ ਨਾਲ ਓਵਰਲੈਪ ਕਰਨ।

**ਕਾਰਗੁਜ਼ਾਰੀ ਚੇਤਾਵਨੀ:** ਆਉਟਪੁੱਟ ਬਣਾਉਣ ਤੋਂ ਪਹਿਲਾਂ ਇਸ ਫੰਕਸ਼ਨ ਨੂੰ ਸਾਰੇ ਇਨਪੁੱਟ ਡਾਟਾ ਪੁਆਇੰਟ ਲੋਡ ਕਰਕੇ ਕ੍ਰਮਬੱਧ ਕਰਨੇ ਪੈਂਦੇ ਹਨ, ਇਸ ਲਈ ਵੱਡੇ ਡਾਟਾਸੈੱਟਾਂ ਨਾਲ ਇਹ ਹੌਲੀ ਹੋ ਸਕਦਾ ਹੈ ਅਤੇ ਵਧੇਰੇ ਮੈਮੋਰੀ ਵਰਤ ਸਕਦਾ ਹੈ।
		]],
		["ro"] = [[
Mută marcajul temporal al fiecărui punct de date de tip durată de la sfârșitul duratei la începutul acesteia.

Așteaptă ca intrare puncte de date de tip durată. Valorile, etichetele și notele sunt păstrate. Rezultatul menține ordinea cronologică, inclusiv atunci când duratele se suprapun.

**Avertisment privind performanța:** Această funcție trebuie să încarce și să sorteze toate punctele de date de intrare înainte de a produce rezultatul, deci poate fi lentă și poate folosi mai multă memorie pentru seturi mari de date.
		]],
		["rm"] = [[
Spusta il timestamp da mintga punct da datas da durada da la fin da sia durada al cumenzament.

Spetga puncts da datas da durada sco input. Las valurs, etichettas e notas vegnan mantegnidas. L’output mantegna l’urden cronologic, era cura che duradas sa surpostan.

**Avis da prestaziun:** Questa funcziun sto chargiar e zavrar tut ils puncts da datas d’input avant da producir l’output; perquai po ella esser plauna e duvrar dapli memoria cun gronds datasets.
		]],
		["ru"] = [[
Перемещает временную метку каждой точки данных с длительностью с конца длительности на её начало.

Ожидает на вход точки данных с длительностью. Значения, метки и заметки сохраняются. Выход сохраняет хронологический порядок, в том числе при перекрытии длительностей.

**Предупреждение о производительности:** Перед формированием выходных данных функция должна загрузить и отсортировать все входные точки данных, поэтому на больших наборах данных она может работать медленно и использовать больше памяти.
		]],
		["sr"] = [[
Pomera vremensku oznaku svake tačke podataka tipa trajanje sa kraja trajanja na njegov početak.

Očekuje tačke podataka tipa trajanje kao ulaz. Vrednosti, oznake i beleške ostaju očuvane. Izlaz zadržava hronološki redosled, čak i kada se trajanja preklapaju.

**Upozorenje o performansama:** Ova funkcija mora da učita i sortira sve ulazne tačke podataka pre generisanja izlaza, pa može biti spora i koristiti više memorije sa velikim skupovima podataka.
		]],
		["si"] = [[
සෑම කාලසීමා දත්ත ලක්ෂ්‍යයකම වේලාමුද්‍රාව එහි කාලසීමාවේ අවසානයේ සිට ආරම්භයට ගෙන යයි.

ආදානය ලෙස කාලසීමා දත්ත ලක්ෂ්‍ය අපේක්ෂා කරයි. අගයන්, ලේබල් සහ සටහන් රඳවා ගනී. කාලසීමා අතිච්ඡාදනය වන විටත් ප්‍රතිදානය කාලානුක්‍රමික අනුපිළිවෙළ පවත්වා ගනී.

**කාර්යසාධන අනතුරු ඇඟවීම:** ප්‍රතිදානය නිපදවීමට පෙර මෙම ශ්‍රිතය සියලු ආදාන දත්ත ලක්ෂ්‍ය පූරණය කර වර්ග කළ යුතු බැවින්, විශාල දත්ත කට්ටල සමඟ එය මන්දගාමී වී වැඩි මතකයක් භාවිත කළ හැක.
		]],
		["sk"] = [[
Presunie časovú pečiatku každého údajového bodu trvania z konca trvania na jeho začiatok.

Očakáva ako vstup údajové body trvania. Hodnoty, označenia a poznámky sa zachovajú. Výstup zachová chronologické poradie aj pri prekrývaní trvaní.

**Upozornenie týkajúce sa výkonu:** Táto funkcia musí pred vytvorením výstupu načítať a zoradiť všetky vstupné údajové body, takže pri veľkých súboroch môže byť pomalá a spotrebovať viac pamäte.
		]],
		["sl"] = [[
Premakne časovni žig vsake podatkovne točke trajanja s konca trajanja na njegov začetek.

Pričakuje podatkovne točke trajanja kot vhod. Vrednosti, oznake in opombe se ohranijo. Izhod ohrani kronološki vrstni red, tudi ko se trajanja prekrivajo.

**Opozorilo glede zmogljivosti:** Ta funkcija mora pred ustvarjanjem izhoda naložiti in razvrstiti vse vhodne podatkovne točke, zato je lahko pri velikih naborih podatkov počasna in porabi več pomnilnika.
		]],
		["es"] = [[
Mueve la marca de tiempo de cada punto de datos de duración del final de su duración al inicio.

Espera puntos de datos de duración como entrada. Los valores, las etiquetas y las notas se conservan. La salida mantiene el orden cronológico, incluso cuando las duraciones se solapan.

**Advertencia de rendimiento:** Esta función debe cargar y ordenar todos los puntos de datos de entrada antes de producir la salida, por lo que puede ser lenta y usar más memoria con conjuntos de datos grandes.
		]],
		["sw"] = [[
Huhamisha muhuri wa muda wa kila nukta ya data ya muda kutoka mwisho wa muda wake hadi mwanzo.

Hutarajia nukta za data za muda kama ingizo. Thamani, lebo na madokezo huhifadhiwa. Matokeo hudumisha mpangilio wa wakati, hata muda unapopishana.

**Onyo la utendaji:** Function hii lazima ipakie na ipange nukta zote za data za ingizo kabla ya kutoa matokeo, hivyo inaweza kuwa polepole na kutumia kumbukumbu zaidi kwenye seti kubwa za data.
		]],
		["sv"] = [[
Flyttar varje varaktighetsdatapunkts tidsstämpel från varaktighetens slut till dess början.

Förväntar varaktighetsdatapunkter som indata. Värden, etiketter och anteckningar bevaras. Utdata behåller kronologisk ordning, även när varaktigheter överlappar.

**Prestandavarning:** Funktionen måste läsa in och sortera alla ingående datapunkter innan den producerar utdata, så den kan vara långsam och använda mer minne med stora datamängder.
		]],
		["ta"] = [[
ஒவ்வொரு கால அளவு தரவுப் புள்ளியின் நேரமுத்திரையை அதன் முடிவிலிருந்து தொடக்கத்திற்கு நகர்த்துகிறது.

உள்ளீடாக கால அளவு தரவுப் புள்ளிகளை எதிர்பார்க்கிறது. மதிப்புகள், லேபிள்கள் மற்றும் குறிப்புகள் தக்கவைக்கப்படும். கால அளவுகள் ஒன்றுடன் ஒன்று மாறினாலும், வெளியீடு காலவரிசையைத் தக்கவைக்கும்.

**செயல்திறன் எச்சரிக்கை:** வெளியீட்டை உருவாக்குவதற்கு முன் இந்த Function அனைத்து உள்ளீட்டுத் தரவுப் புள்ளிகளையும் ஏற்றி வரிசைப்படுத்த வேண்டும்; எனவே பெரிய தரவுத்தொகுப்புகளில் இது மெதுவாகவும் அதிக நினைவகத்தைப் பயன்படுத்துவதாகவும் இருக்கலாம்.
		]],
		["te"] = [[
ప్రతి వ్యవధి డేటా పాయింట్ టైమ్‌స్టాంప్‌ను దాని వ్యవధి ముగింపు నుండి ప్రారంభానికి తరలిస్తుంది.

ఇన్‌పుట్‌గా వ్యవధి డేటా పాయింట్లను ఆశిస్తుంది. విలువలు, లేబుళ్లు, నోట్లు అలాగే ఉంటాయి. వ్యవధులు ఒకదానిపై మరొకటి ఉన్నప్పటికీ, అవుట్‌పుట్ కాలక్రమ క్రమాన్ని కొనసాగిస్తుంది.

**పనితీరు హెచ్చరిక:** అవుట్‌పుట్‌ను ఉత్పత్తి చేయడానికి ముందు ఈ ఫంక్షన్ అన్ని ఇన్‌పుట్ డేటా పాయింట్లను లోడ్ చేసి క్రమబద్ధీకరించాలి; అందువల్ల పెద్ద డేటాసెట్‌లలో ఇది నెమ్మదిగా ఉండి ఎక్కువ మెమరీని ఉపయోగించవచ్చు.
		]],
		["th"] = [[
ย้ายเวลาของจุดข้อมูลระยะเวลาแต่ละจุดจากจุดสิ้นสุดของระยะเวลาไปยังจุดเริ่มต้น

ต้องใช้จุดข้อมูลระยะเวลาเป็นอินพุต ค่า ป้ายกำกับ และบันทึกจะคงเดิม ผลลัพธ์จะเรียงตามลำดับเวลา รวมถึงกรณีที่ระยะเวลาซ้อนทับกัน

**คำเตือนด้านประสิทธิภาพ:** ฟังก์ชันนี้ต้องโหลดและจัดเรียงจุดข้อมูลอินพุตทั้งหมดก่อนสร้างผลลัพธ์ จึงอาจทำงานช้าและใช้หน่วยความจำมากขึ้นเมื่อมีชุดข้อมูลขนาดใหญ่
		]],
		["tr"] = [[
Her süre veri noktasının zaman damgasını süresinin sonundan başlangıcına taşır.

Girdi olarak süre veri noktaları bekler. Değerler, etiketler ve notlar korunur. Süreler çakışsa bile çıktı kronolojik sırayı korur.

**Performans uyarısı:** Bu işlev, çıktı üretmeden önce tüm giriş veri noktalarını yükleyip sıralamalıdır; bu nedenle büyük veri kümelerinde yavaş olabilir ve daha fazla bellek kullanabilir.
		]],
		["uk"] = [[
Переміщує часову мітку кожної точки даних тривалості з кінця її тривалості на початок.

Очікує на вході точки даних тривалості. Значення, мітки та примітки зберігаються. Вихідні дані зберігають хронологічний порядок, зокрема коли тривалості перекриваються.

**Попередження про продуктивність:** Перед створенням вихідних даних ця функція має завантажити й відсортувати всі вхідні точки даних, тому на великих наборах даних може працювати повільно й використовувати більше пам’яті.
		]],
		["vi"] = [[
Chuyển dấu thời gian của mỗi điểm dữ liệu thời lượng từ cuối thời lượng về đầu thời lượng.

Yêu cầu đầu vào là các điểm dữ liệu thời lượng. Giá trị, nhãn và ghi chú được giữ nguyên. Đầu ra duy trì thứ tự thời gian, kể cả khi các thời lượng chồng lấn.

**Cảnh báo hiệu năng:** Hàm này phải tải và sắp xếp tất cả điểm dữ liệu đầu vào trước khi tạo đầu ra, vì vậy có thể chạy chậm và sử dụng nhiều bộ nhớ hơn với các tập dữ liệu lớn.
		]],
	},
	config = {},

	generator = function(source)
		local sorted_points = nil
		local output_index = 1

		return function()
			if not sorted_points then
				sorted_points = {}
				local input_order = 0
				while true do
					local data_point = source.dp()
					if not data_point then
						break
					end
					input_order = input_order + 1
					sorted_points[input_order] = {
						data_point = core.shift(data_point, -(data_point.value * 1000)),
						input_order = input_order,
					}
				end
				table.sort(sorted_points, comes_before)
			end

			local output = sorted_points[output_index]
			if output then
				output_index = output_index + 1
				return output.data_point
			end
			return nil
		end
	end,
}
