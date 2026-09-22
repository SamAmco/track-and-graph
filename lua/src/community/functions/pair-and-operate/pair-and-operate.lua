-- Lua Function to pair the data points of two input sources and perform
-- an operation on their values (addition, subtraction, multiplication, or division).
local duration = require("tng.config").duration
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "pair-and-operate",
  version = "1.0.4",
  inputCount = 2,
  categories = { "_combine" },
  title = {
  	["en"] = "Pair and Operate",
  	["af"] = "Koppel en Bewerk",
  	["sq"] = "Çifto dhe vepro",
  	["am"] = "አጣምር እና አከናውን",
  	["hy"] = "Զուգակցել և գործարկել",
  	["az"] = "Cütləşdir və əməliyyat apar",
  	["bn"] = "জোড়া তৈরি ও অপারেট করুন",
  	["eu"] = "Parekatzea eta eragiketa",
  	["be"] = "Спалучыць і выканаць аперацыю",
  	["bg"] = "Сдвояване и операция",
  	["my"] = "တွဲဖက်၍ လုပ်ဆောင်ရန်",
  	["ca"] = "Combina i opera",
  	["zh-Hans"] = "配对并运算",
  	["zh-Hant"] = "配對並運算",
  	["hr"] = "Upari i izvrši operaciju",
  	["cs"] = "Spárovat a provést operaci",
  	["da"] = "Par og beregn",
  	["nl"] = "Koppelen en bewerken",
  	["et"] = "Paarista ja teosta tehe",
  	["fil"] = "Pagtambalin at Operahan",
  	["fi"] = "Yhdistä ja laske",
  	["fr"] = "Associer et effectuer une opération",
  	["gl"] = "Emparellar e operar",
  	["ka"] = "შეწყვილება და ოპერაცია",
  	["de"] = "Paaren und verarbeiten",
  	["el"] = "Σύζευξη και πράξη",
  	["gu"] = "જોડી બનાવી ક્રિયા કરો",
  	["hi"] = "युग्म बनाएँ और संचालन करें",
  	["hu"] = "Párosítás és művelet",
  	["is"] = "Para og framkvæma aðgerð",
  	["id"] = "Pasangkan dan Operasikan",
  	["it"] = "Abbina ed esegui operazioni",
  	["ja"] = "ペアにして演算",
  	["kn"] = "ಜೋಡಿಸಿ ಮತ್ತು ಕಾರ್ಯಾಚರಿಸಿ",
  	["kk"] = "Жұптау және амал орындау",
  	["km"] = "ផ្គូផ្គង និងប្រតិបត្តិការ",
  	["ko"] = "쌍으로 연산",
  	["ky"] = "Жупташтыруу жана операция аткаруу",
  	["lo"] = "ຈັບຄູ່ ແລະ ດຳເນີນການ",
  	["lv"] = "Savienot un veikt darbību",
  	["lt"] = "Suporuoti ir atlikti operaciją",
  	["mk"] = "Спари и изврши операција",
  	["ms"] = "Padankan dan Kendalikan",
  	["ml"] = "ജോഡിയാക്കി പ്രവർത്തിപ്പിക്കുക",
  	["mr"] = "जोडी बनवून क्रिया करा",
  	["mn"] = "Хослуулж үйлдэл хийх",
  	["ne"] = "जोडी बनाएर सञ्चालन गर्नुहोस्",
  	["no"] = "Par og utfør operasjon",
  	["pl"] = "Połącz i wykonaj działanie",
  	["pt"] = "Emparelhar e operar",
  	["pa"] = "ਜੋੜਾ ਬਣਾਓ ਅਤੇ ਕਾਰਵਾਈ ਕਰੋ",
  	["ro"] = "Asociază și operează",
  	["rm"] = "Collegar e operar",
  	["ru"] = "Объединить и выполнить операцию",
  	["sr"] = "Upari i izvrši operaciju",
  	["si"] = "යුගල කර ක්‍රියා කරන්න",
  	["sk"] = "Spárovať a vykonať operáciu",
  	["sl"] = "Združi in izvedi operacijo",
  	["es"] = "Emparejar y operar",
  	["sw"] = "Oanisha na Fanya Operesheni",
  	["sv"] = "Para ihop och beräkna",
  	["ta"] = "இணைத்து செயல்படுத்து",
  	["te"] = "జత చేసి ఆపరేట్ చేయి",
  	["th"] = "จับคู่และดำเนินการ",
  	["tr"] = "Eşleştir ve İşlem Yap",
  	["uk"] = "Об’єднати та виконати операцію",
  	["vi"] = "Ghép cặp và thực hiện phép toán",
  },
  description = {
  	["en"] = [[
Pairs each data point in the first data source with the corresponding data point in the second data source and performs a specified operation (addition, subtraction, multiplication, or division) on their values. The pair for each data point is the first data point that falls within the given time threshold. Configuration:

- **Time Threshold:** The maximum duration between data points in the two sources to be considered a pair.
- **Operation:** The mathematical operation to perform on the paired data point values (addition, subtraction, multiplication, or division).
- **On Missing:** Specifies the behavior when a data point in the first source does not have a corresponding data point in the second source within the given time threshold. Options include:
  - **Skip:** Do not output anything for that data point.
  - **Pass Through:** Output the original data point from the first source without modification.

> **Note:** Division by zero is invalid and considered as missing data. The On Missing configuration will determine how such cases are handled.
  	]],
  	["af"] = [[
Koppel elke datapunt in die eerste databron aan die ooreenstemmende datapunt in die tweede databron en voer ’n gespesifiseerde bewerking (optelling, aftrekking, vermenigvuldiging of deling) op hul waardes uit. Die paar vir elke datapunt is die eerste datapunt wat binne die gegewe tydsdrempel val. Konfigurasie:

- **Tydsdrempel:** Die maksimum duur tussen datapunte in die twee bronne om as ’n paar beskou te word.
- **Bewerking:** Die wiskundige bewerking wat op die waardes van die gekoppelde datapunte uitgevoer word (optelling, aftrekking, vermenigvuldiging of deling).
- **By Ontbreking:** Spesifiseer die gedrag wanneer ’n datapunt in die eerste bron nie binne die gegewe tydsdrempel ’n ooreenstemmende datapunt in die tweede bron het nie. Opsies sluit in:
  - **Oorslaan:** Lewer niks vir daardie datapunt nie.
  - **Deurgee:** Lewer die oorspronklike datapunt uit die eerste bron sonder wysiging.

> **Nota:** Deling deur nul is ongeldig en word as ontbrekende data beskou. Die By Ontbreking-konfigurasie bepaal hoe sulke gevalle hanteer word.
  	]],
  	["sq"] = [[
Çifton çdo pikë të të dhënave në burimin e parë me pikën përkatëse në burimin e dytë dhe kryen një veprim të specifikuar (mbledhje, zbritje, shumëzim ose pjesëtim) mbi vlerat e tyre. Çifti për çdo pikë të të dhënave është pika e parë që bie brenda pragut kohor të dhënë. Konfigurimi:

- **Pragu kohor:** Kohëzgjatja maksimale midis pikave të të dhënave në dy burimet që ato të konsiderohen çift.
- **Veprimi:** Veprimi matematikor që kryhet mbi vlerat e pikave të çiftuara të të dhënave (mbledhje, zbritje, shumëzim ose pjesëtim).
- **Kur mungon:** Përcakton sjelljen kur një pikë e të dhënave në burimin e parë nuk ka një pikë përkatëse në burimin e dytë brenda pragut kohor të dhënë. Opsionet përfshijnë:
  - **Anashkalo:** Mos nxirr asgjë për atë pikë të të dhënave.
  - **Kaloje:** Nxirr pikën origjinale të të dhënave nga burimi i parë pa ndryshime.

> **Shënim:** Pjesëtimi me zero është i pavlefshëm dhe konsiderohet si e dhënë që mungon. Konfigurimi "Kur mungon" përcakton mënyrën e trajtimit të rasteve të tilla.
  	]],
  	["am"] = [[
በመጀመሪያው የውሂብ ምንጭ ያለውን እያንዳንዱን የውሂብ ነጥብ በሁለተኛው የውሂብ ምንጭ ካለው ተጓዳኝ የውሂብ ነጥብ ጋር ያጣምራል፣ ከዚያም በእሴቶቻቸው ላይ የተገለጸውን ክዋኔ (መደመር፣ መቀነስ፣ ማባዛት ወይም ማካፈል) ያከናውናል። ለእያንዳንዱ የውሂብ ነጥብ የሚጣመረው በተሰጠው የጊዜ ገደብ ውስጥ የሚገኘው የመጀመሪያው የውሂብ ነጥብ ነው። ውቅር፦

- **የጊዜ ገደብ፦** በሁለቱ ምንጮች ያሉ የውሂብ ነጥቦች እንደ ጥምረት እንዲቆጠሩ በመካከላቸው የሚፈቀደው ከፍተኛ የጊዜ ርዝመት።
- **ክዋኔ፦** በተጣመሩ የውሂብ ነጥቦች እሴቶች ላይ የሚከናወነው የሂሳብ ክዋኔ (መደመር፣ መቀነስ፣ ማባዛት ወይም ማካፈል)።
- **በሚጎድልበት ጊዜ፦** በመጀመሪያው ምንጭ ያለ የውሂብ ነጥብ በተሰጠው የጊዜ ገደብ ውስጥ በሁለተኛው ምንጭ ተጓዳኝ የውሂብ ነጥብ ከሌለው የሚደረገውን ይገልጻል። አማራጮቹ፦
  - **ዝለል፦** ለዚያ የውሂብ ነጥብ ምንም ነገር አታውጣ።
  - **በቀጥታ አስተላልፍ፦** ከመጀመሪያው ምንጭ የመጣውን የውሂብ ነጥብ ሳይቀየር አውጣ።

> **ማስታወሻ፦** በዜሮ ማካፈል ልክ ያልሆነ ሲሆን እንደ የጎደለ ውሂብ ይቆጠራል። እንዲህ ያሉ ጉዳዮች እንዴት እንደሚያዙ የ«በሚጎድልበት ጊዜ» ውቅሩ ይወስናል።
  	]],
  	["hy"] = [[
Առաջին տվյալների աղբյուրի յուրաքանչյուր տվյալակետը զուգակցում է երկրորդ աղբյուրի համապատասխան տվյալակետի հետ և դրանց արժեքների նկատմամբ կատարում նշված գործողությունը (գումարում, հանում, բազմապատկում կամ բաժանում)։ Յուրաքանչյուր տվյալակետի զույգը առաջին տվյալակետն է, որը գտնվում է նշված ժամանակային շեմի սահմաններում։ Կազմաձևում՝

- **Ժամանակային շեմ․** երկու աղբյուրների տվյալակետերի միջև թույլատրելի առավելագույն տևողությունը՝ զույգ համարվելու համար։
- **Գործողություն․** զուգակցված տվյալակետերի արժեքների նկատմամբ կատարվող մաթեմատիկական գործողությունը (գումարում, հանում, բազմապատկում կամ բաժանում)։
- **Բացակայի դեպքում․** սահմանում է վարքագիծը, երբ առաջին աղբյուրի տվյալակետի համար նշված ժամանակային շեմում երկրորդ աղբյուրում համապատասխան տվյալակետ չկա։ Ընտրանքները՝
  - **Բաց թողնել․** այդ տվյալակետի համար ոչինչ չարտածել։
  - **Փոխանցել առանց փոփոխության․** առաջին աղբյուրի սկզբնական տվյալակետը արտածել առանց փոփոխության։

> **Նշում․** զրոյի վրա բաժանելն անվավեր է և համարվում է բացակայող տվյալ։ Նման դեպքերի մշակումը կորոշի «Բացակայի դեպքում» կարգավորումը։
  	]],
  	["az"] = [[
Birinci məlumat mənbəyindəki hər məlumat nöqtəsini ikinci məlumat mənbəyindəki uyğun məlumat nöqtəsi ilə cütləşdirir və onların qiymətləri üzərində göstərilən əməliyyatı (toplama, çıxma, vurma və ya bölmə) aparır. Hər məlumat nöqtəsi üçün cüt verilən zaman həddinə düşən ilk məlumat nöqtəsidir. Konfiqurasiya:

- **Zaman həddi:** İki mənbədəki məlumat nöqtələri arasında cüt hesab edilməsi üçün maksimum müddət.
- **Əməliyyat:** Cütləşdirilmiş məlumat nöqtələrinin qiymətləri üzərində aparılacaq riyazi əməliyyat (toplama, çıxma, vurma və ya bölmə).
- **Çatışmayan məlumat olduqda:** Birinci mənbədəki məlumat nöqtəsinin verilən zaman həddi daxilində ikinci mənbədə uyğun nöqtəsi olmadıqda davranışı müəyyən edir. Seçimlər:
  - **Ötür:** Həmin məlumat nöqtəsi üçün heç nə çıxarma.
  - **Dəyişmədən ötür:** Birinci mənbədəki orijinal məlumat nöqtəsini dəyişiklik etmədən çıxar.

> **Qeyd:** Sıfıra bölmə etibarsızdır və çatışmayan məlumat hesab olunur. Belə halların necə idarə ediləcəyini Çatışmayan məlumat olduqda konfiqurasiyası müəyyən edir.
  	]],
  	["bn"] = [[
প্রথম ডেটা উৎসের প্রতিটি ডেটা পয়েন্টকে দ্বিতীয় ডেটা উৎসের সংশ্লিষ্ট ডেটা পয়েন্টের সঙ্গে জোড়া করে এবং তাদের মানের ওপর নির্দিষ্ট অপারেশন (যোগ, বিয়োগ, গুণ বা ভাগ) সম্পাদন করে। প্রতিটি ডেটা পয়েন্টের জোড়া হলো নির্দিষ্ট সময়সীমার মধ্যে পড়া প্রথম ডেটা পয়েন্ট। কনফিগারেশন:

- **সময়সীমা:** দুই উৎসের ডেটা পয়েন্টের মধ্যে জোড়া হিসেবে বিবেচিত হওয়ার সর্বোচ্চ সময়কাল।
- **অপারেশন:** জোড়া করা ডেটা পয়েন্টের মানের ওপর সম্পাদিত গাণিতিক অপারেশন (যোগ, বিয়োগ, গুণ বা ভাগ)।
- **অনুপস্থিত অবস্থায়:** প্রথম উৎসের কোনো ডেটা পয়েন্টের জন্য নির্দিষ্ট সময়সীমার মধ্যে দ্বিতীয় উৎসে সংশ্লিষ্ট ডেটা পয়েন্ট না থাকলে কী হবে। অপশন:
  - **এড়িয়ে যান:** ওই ডেটা পয়েন্টের জন্য কোনো আউটপুট দেবেন না।
  - **পাস থ্রু:** প্রথম উৎসের মূল ডেটা পয়েন্টটি অপরিবর্তিতভাবে আউটপুট করুন।

> **নোট:** শূন্য দিয়ে ভাগ করা অবৈধ এবং অনুপস্থিত ডেটা হিসেবে বিবেচিত। এমন ক্ষেত্রে কীভাবে পরিচালনা করা হবে তা অনুপস্থিত অবস্থায় কনফিগারেশন নির্ধারণ করবে।
  	]],
  	["eu"] = [[
Lehen datu-iturburuko datu-puntu bakoitza bigarren datu-iturburuko dagokion datu-puntuarekin parekatzen du, eta haien balioei zehaztutako eragiketa (batuketa, kenketa, biderketa edo zatiketa) aplikatzen die. Datu-puntu bakoitzaren bikotea emandako denbora-atalasearen barruan dagoen lehen datu-puntua da. Konfigurazioa:

- **Denbora-atalasea:** Bi iturrietako datu-puntuen arteko gehieneko iraupena bikotetzat hartzeko.
- **Eragiketa:** Parekatutako datu-puntuen balioei aplikatu beharreko eragiketa matematikoa (batuketa, kenketa, biderketa edo zatiketa).
- **Falta denean:** Lehen iturburuko datu-puntu batek emandako denbora-atalasearen barruan bigarren iturburuan dagokion datu-punturik ez duenean zer egin zehazten du. Aukerak:
  - **Saltatu:** Ez atera ezer datu-puntu horretarako.
  - **Igaroarazi:** Atera lehen iturburuko jatorrizko datu-puntua aldaketarik gabe.

> **Oharra:** Zerorekin zatitzea baliogabea da eta falta den datutzat hartzen da. Falta denean konfigurazioak zehaztuko du horrelako kasuak nola kudeatu.
  	]],
  	["be"] = [[
Спалучае кожную кропку даных з першай крыніцы з адпаведнай кропкай даных з другой крыніцы і выконвае зададзеную аперацыю (складанне, адніманне, множанне або дзяленне) над іх значэннямі. Для кожнай кропкі даных парай з’яўляецца першая кропка даных, якая трапляе ў зададзены часавы парог. Канфігурацыя:

- **Часавы парог:** максімальная працягласць паміж кропкамі даных у дзвюх крыніцах, каб лічыць іх парай.
- **Аперацыя:** матэматычная аперацыя над значэннямі спалучаных кропак даных (складанне, адніманне, множанне або дзяленне).
- **Пры адсутнасці:** задае паводзіны, калі для кропкі даных з першай крыніцы ў другой няма адпаведнай кропкі ў межах зададзенага часавага парога. Варыянты:
  - **Прапусціць:** нічога не выводзіць для гэтай кропкі даных.
  - **Прапусціць праз:** вывесці зыходную кропку даных з першай крыніцы без змяненняў.

> **Заўвага:** дзяленне на нуль недапушчальнае і лічыцца адсутнымі данымі. Налада «Пры адсутнасці» вызначае, як апрацоўваць такія выпадкі.
  	]],
  	["bg"] = [[
Сдвоява всяка точка от данни в първия източник със съответната точка от данни във втория източник и извършва зададена операция (събиране, изваждане, умножение или деление) върху стойностите им. За всяка точка от данни за сдвоена се избира първата точка от данни, която попада в зададения времеви праг. Конфигурация:

- **Времеви праг:** Максималната продължителност между точките от данни в двата източника, при която те се считат за двойка.
- **Операция:** Математическата операция, която се извършва върху стойностите на сдвоените точки от данни (събиране, изваждане, умножение или деление).
- **При липса:** Определя поведението, когато точка от данни в първия източник няма съответстваща точка във втория източник в рамките на зададения времеви праг. Опциите включват:
  - **Пропускане:** Не извежда нищо за тази точка от данни.
  - **Предаване без промяна:** Извежда оригиналната точка от данни от първия източник без промяна.

> **Забележка:** Делението на нула е невалидно и се счита за липсващи данни. Настройката „При липса“ определя как се обработват такива случаи.
  	]],
  	["my"] = [[
ပထမဒေတာရင်းမြစ်ရှိ ဒေတာအမှတ်တစ်ခုချင်းစီကို ဒုတိယဒေတာရင်းမြစ်ရှိ သက်ဆိုင်ရာ ဒေတာအမှတ်နှင့် တွဲဖက်ပြီး ၎င်းတို့၏တန်ဖိုးများအပေါ် သတ်မှတ်ထားသော လုပ်ဆောင်ချက် (ပေါင်းခြင်း၊ နုတ်ခြင်း၊ မြှောက်ခြင်း သို့မဟုတ် စားခြင်း) ကို လုပ်ဆောင်သည်။ ဒေတာအမှတ်တစ်ခုစီအတွက် တွဲဖက်ဒေတာအမှတ်မှာ သတ်မှတ်ထားသော အချိန်ကန့်သတ်အတွင်း ကျရောက်သည့် ပထမဆုံးဒေတာအမှတ် ဖြစ်သည်။ ပြင်ဆင်သတ်မှတ်မှုများ:

- **အချိန်ကန့်သတ်ချက်:** ရင်းမြစ်နှစ်ခုရှိ ဒေတာအမှတ်များကို တွဲဖက်ရန် ခွင့်ပြုသည့် အများဆုံးကြာချိန်။
- **လုပ်ဆောင်ချက်:** တွဲဖက်ထားသော ဒေတာအမှတ်တန်ဖိုးများအပေါ် လုပ်ဆောင်မည့် သင်္ချာလုပ်ဆောင်ချက် (ပေါင်းခြင်း၊ နုတ်ခြင်း၊ မြှောက်ခြင်း သို့မဟုတ် စားခြင်း)။
- **မရှိပါက:** ပထမရင်းမြစ်ရှိ ဒေတာအမှတ်အတွက် သတ်မှတ်ထားသော အချိန်ကန့်သတ်အတွင်း ဒုတိယရင်းမြစ်တွင် သက်ဆိုင်ရာဒေတာအမှတ် မရှိသည့်အခါ ပြုမူပုံကို သတ်မှတ်သည်။ ရွေးချယ်စရာများ:
  - **ကျော်ရန်:** ထိုဒေတာအမှတ်အတွက် မည်သည့်အရာမျှ ထုတ်မပေးပါ။
  - **အတိုင်း ဖြတ်သန်းရန်:** ပထမရင်းမြစ်မှ မူရင်းဒေတာအမှတ်ကို ပြောင်းလဲခြင်းမရှိဘဲ ထုတ်ပေးပါ။

> **မှတ်ချက်:** သုညဖြင့် စားခြင်းသည် မမှန်ကန်ပြီး ဒေတာမရှိခြင်းဟု သတ်မှတ်သည်။ ထိုအခြေအနေများကို မရှိပါက ပြင်ဆင်သတ်မှတ်မှုက ဆုံးဖြတ်ပေးမည်။
  	]],
  	["ca"] = [[
Combina cada punt de dades de la primera font de dades amb el punt de dades corresponent de la segona font de dades i realitza una operació especificada (suma, resta, multiplicació o divisió) amb els seus valors. La parella de cada punt de dades és el primer punt de dades que es troba dins del llindar de temps indicat. Configuració:

- **Llindar de temps:** La durada màxima entre els punts de dades de les dues fonts perquè es considerin una parella.
- **Operació:** L’operació matemàtica que cal realitzar amb els valors dels punts de dades combinats (suma, resta, multiplicació o divisió).
- **Si en falta:** Especifica el comportament quan un punt de dades de la primera font no té cap punt de dades corresponent a la segona font dins del llindar de temps indicat. Les opcions inclouen:
  - **Omet:** No produeix cap sortida per a aquest punt de dades.
  - **Passa’l sense canvis:** Produeix el punt de dades original de la primera font sense modificar-lo.

> **Nota:** La divisió per zero no és vàlida i es considera una dada absent. La configuració «Si en falta» determina com es gestionen aquests casos.
  	]],
  	["zh-Hans"] = [[
将第一个数据源中的每个数据点与第二个数据源中对应的数据点配对，并对它们的值执行指定运算（加法、减法、乘法或除法）。每个数据点的配对项是第二个数据源中第一个落在给定时间阈值内的数据点。配置：

- **时间阈值：**两个数据源中数据点之间可被视为配对的最大时长。
- **运算：**对配对数据点的值执行的数学运算（加法、减法、乘法或除法）。
- **缺失数据处理：**当第一个数据源中的数据点在给定时间阈值内没有第二个数据源中的对应数据点时的处理方式。选项包括：
  - **跳过：**不为该数据点输出任何内容。
  - **直接通过：**原样输出第一个数据源中的数据点。

> **注意：**除以零无效，并视为缺失数据。“缺失数据处理”配置将决定如何处理此类情况。
  	]],
  	["zh-Hant"] = [[
將第一個資料來源中的每個資料點與第二個資料來源中對應的資料點配對，並對其值執行指定操作（加法、減法、乘法或除法）。每個資料點的配對資料點，是落在指定時間閾值內的第一個資料點。設定：

- **時間閾值：** 兩個來源中的資料點可被視為配對的最大時間差。
- **運算：** 對配對資料點值執行的數學運算（加法、減法、乘法或除法）。
- **找不到資料時：** 指定第一個來源中的資料點在指定時間閾值內沒有第二個來源的對應資料點時的行為。選項包括：
  - **略過：** 不為該資料點輸出任何內容。
  - **直接傳遞：** 不加修改地輸出第一個來源中的原始資料點。

> **注意：** 除以零無效，會被視為遺失資料。「找不到資料時」設定將決定如何處理此類情況。
  	]],
  	["hr"] = [[
Uparuje svaku podatkovnu točku u prvom izvoru podataka s odgovarajućom podatkovnom točkom u drugom izvoru podataka i izvršava navedenu operaciju (zbrajanje, oduzimanje, množenje ili dijeljenje) nad njihovim vrijednostima. Par za svaku podatkovnu točku prva je podatkovna točka koja se nalazi unutar zadanog vremenskog praga. Konfiguracija:

- **Vremenski prag:** Najduže trajanje između podatkovnih točaka u dva izvora da bi se smatrale parom.
- **Operacija:** Matematička operacija koja se izvršava nad vrijednostima uparenih podatkovnih točaka (zbrajanje, oduzimanje, množenje ili dijeljenje).
- **Kod nedostajanja:** Određuje ponašanje kada podatkovna točka u prvom izvoru nema odgovarajuću podatkovnu točku u drugom izvoru unutar zadanog vremenskog praga. Mogućnosti uključuju:
  - **Preskoči:** Ne ispisuj ništa za tu podatkovnu točku.
  - **Propusti:** Ispiši izvornu podatkovnu točku iz prvog izvora bez izmjena.

> **Napomena:** Dijeljenje nulom nije valjano i smatra se nedostajućim podatkom. Konfiguracija Kod nedostajanja određuje kako će se takvi slučajevi obraditi.
  	]],
  	["cs"] = [[
Spáruje každý datový bod v prvním zdroji s odpovídajícím datovým bodem ve druhém zdroji a provede nad jejich hodnotami zadanou operaci (sčítání, odčítání, násobení nebo dělení). Párem pro každý datový bod je první datový bod, který spadá do zadaného časového prahu. Konfigurace:

- **Časový práh:** Maximální délka mezi datovými body ve dvou zdrojích, aby byly považovány za pár.
- **Operace:** Matematická operace prováděná s hodnotami spárovaných datových bodů (sčítání, odčítání, násobení nebo dělení).
- **Při chybějícím:** Určuje chování, když datový bod v prvním zdroji nemá odpovídající datový bod ve druhém zdroji v rámci zadaného časového prahu. Možnosti:
  - **Přeskočit:** Pro tento datový bod nic nevytvořit.
  - **Předat dál:** Vytvořit původní datový bod z prvního zdroje beze změny.

> **Poznámka:** Dělení nulou je neplatné a považuje se za chybějící data. Konfigurace Při chybějícím určí, jak se takové případy zpracují.
  	]],
  	["da"] = [[
Parrer hvert datapunkt i den første datakilde med det tilsvarende datapunkt i den anden datakilde og udfører en angivet operation (addition, subtraktion, multiplikation eller division) på deres værdier. Parret for hvert datapunkt er det første datapunkt, der ligger inden for den angivne tidsgrænse. Konfiguration:

- **Tidsgrænse:** Den maksimale varighed mellem datapunkter i de to kilder, for at de kan betragtes som et par.
- **Operation:** Den matematiske operation, der skal udføres på værdierne af de parrede datapunkter (addition, subtraktion, multiplikation eller division).
- **Ved manglende:** Angiver adfærden, når et datapunkt i den første kilde ikke har et tilsvarende datapunkt i den anden kilde inden for den angivne tidsgrænse. Indstillingerne omfatter:
  - **Spring over:** Udsend ikke noget for datapunktet.
  - **Send videre:** Udsend det oprindelige datapunkt fra den første kilde uden ændringer.

> **Bemærk:** Division med nul er ugyldig og betragtes som manglende data. Konfigurationen Ved manglende bestemmer, hvordan sådanne tilfælde håndteres.
  	]],
  	["nl"] = [[
Koppelt elk datapunt in de eerste gegevensbron aan het overeenkomstige datapunt in de tweede gegevensbron en voert een opgegeven bewerking (optellen, aftrekken, vermenigvuldigen of delen) uit op hun waarden. Het paar voor elk datapunt is het eerste datapunt dat binnen de opgegeven tijdsdrempel valt. Configuratie:

- **Tijdsdrempel:** De maximale tijdsduur tussen datapunten in de twee bronnen om als paar te worden beschouwd.
- **Bewerking:** De wiskundige bewerking die op de waarden van de gekoppelde datapunten moet worden uitgevoerd (optellen, aftrekken, vermenigvuldigen of delen).
- **Bij ontbreken:** Geeft aan wat er gebeurt wanneer een datapunt in de eerste bron geen overeenkomstig datapunt in de tweede bron heeft binnen de opgegeven tijdsdrempel. Opties:
  - **Overslaan:** Niets uitvoeren voor dat datapunt.
  - **Doorlaten:** Het oorspronkelijke datapunt uit de eerste bron ongewijzigd uitvoeren.

> **Opmerking:** Delen door nul is ongeldig en wordt beschouwd als ontbrekende gegevens. De configuratie Bij ontbreken bepaalt hoe dergelijke gevallen worden afgehandeld.
  	]],
  	["et"] = [[
Seob esimese andmeallika iga andmepunkti teise andmeallika vastava andmepunktiga ning teostab nende väärtustega määratud tehte (liitmine, lahutamine, korrutamine või jagamine). Iga andmepunkti paariks on esimene andmepunkt, mis jääb antud ajaläve piiresse. Seadistus:

- **Ajalävi:** kahe allika andmepunktide maksimaalne ajavahe, mille korral loetakse need paariks.
- **Tehe:** paaris andmepunktide väärtustega tehtav matemaatiline tehe (liitmine, lahutamine, korrutamine või jagamine).
- **Puudumisel:** määrab käitumise, kui esimese allika andmepunktil pole teises allikas antud ajaläve piires vastavat andmepunkti. Valikud:
  - **Jäta vahele:** ära väljasta selle andmepunkti kohta midagi.
  - **Läbimisele:** väljasta esimese allika algne andmepunkt muutmata kujul.

> **Märkus:** nulliga jagamine on kehtetu ja seda käsitletakse puuduvate andmetena. Seadistus „Puudumisel” määrab, kuidas selliseid juhtumeid käsitletakse.
  	]],
  	["fil"] = [[
Itinatambal ang bawat data point sa unang data source sa katumbas na data point sa ikalawang data source at nagsasagawa ng tinukoy na operasyon (pagdaragdag, pagbabawas, pagpaparami, o paghahati) sa kanilang mga halaga. Ang katambal ng bawat data point ay ang unang data point na pasok sa ibinigay na time threshold. Configuration:

- **Time Threshold:** Pinakamahabang tagal sa pagitan ng mga data point sa dalawang source upang maituring na magkatambal.
- **Operation:** Operasyong matematika na isasagawa sa mga halaga ng magkatambal na data point (pagdaragdag, pagbabawas, pagpaparami, o paghahati).
- **On Missing:** Tinutukoy ang gagawin kapag walang katumbas na data point sa ikalawang source ang data point sa unang source sa loob ng ibinigay na time threshold. Kasama sa mga opsyon ang:
  - **Skip:** Huwag maglabas ng anuman para sa data point na iyon.
  - **Pass Through:** Ilabas ang orihinal na data point mula sa unang source nang walang pagbabago.

> **Tandaan:** Hindi wasto ang paghahati sa zero at itinuturing itong nawawalang data. Tutukuyin ng configuration na On Missing kung paano hahawakan ang ganitong mga kaso.
  	]],
  	["fi"] = [[
Yhdistää kunkin ensimmäisen tietolähteen datapisteen vastaavaan toisen tietolähteen datapisteeseen ja suorittaa niiden arvoille määritetyn toiminnon (yhteenlasku, vähennyslasku, kertolasku tai jakolasku). Kunkin datapisteen pari on ensimmäinen annettuun aikakynnysarvoon sisältyvä datapiste. Määritys:

- **Aikakynnysarvo:** Kaksi lähdettä yhdistävien datapisteiden välinen enimmäiskesto.
- **Toiminto:** Yhdistettyjen datapisteiden arvoille suoritettava matemaattinen toiminto (yhteenlasku, vähennyslasku, kertolasku tai jakolasku).
- **Kun vastaava puuttuu:** Määrittää toiminnan, kun ensimmäisen lähteen datapisteellä ei ole toisen lähteen vastaavaa datapistettä annetun aikakynnysarvon sisällä. Vaihtoehdot:
  - **Ohita:** Älä tuota tälle datapisteelle mitään.
  - **Välitä sellaisenaan:** Tuota alkuperäinen datapiste ensimmäisestä lähteestä muuttamatta sitä.

> **Huomautus:** Nollalla jakaminen ei ole sallittua, ja se tulkitaan puuttuvaksi tiedoksi. Kun vastaava puuttuu -asetus määrittää, miten tällaiset tapaukset käsitellään.
  	]],
  	["fr"] = [[
Associe chaque point de données de la première source de données au point correspondant de la seconde source de données et effectue une opération indiquée (addition, soustraction, multiplication ou division) sur leurs valeurs. Pour chaque point de données, la paire est le premier point de données situé dans le seuil temporel indiqué. Configuration :

- **Seuil temporel :** Durée maximale entre les points de données des deux sources pour qu’ils soient considérés comme une paire.
- **Opération :** Opération mathématique à effectuer sur les valeurs des points de données associés (addition, soustraction, multiplication ou division).
- **En cas d’absence :** Comportement lorsque le point de données de la première source n’a pas de point correspondant dans la seconde source au sein du seuil temporel indiqué. Options :
  - **Ignorer :** Ne rien produire pour ce point de données.
  - **Transmettre :** Produire le point de données d’origine de la première source sans modification.

> **Remarque :** La division par zéro est invalide et considérée comme une donnée manquante. La configuration « En cas d’absence » détermine la manière de traiter ces cas.
  	]],
  	["gl"] = [[
Emparella cada punto de datos da primeira fonte de datos co punto de datos correspondente da segunda fonte de datos e realiza unha operación especificada (suma, resta, multiplicación ou división) sobre os seus valores. O par de cada punto de datos é o primeiro punto de datos que se atopa dentro do limiar temporal indicado. Configuración:

- **Limiar temporal:** A duración máxima entre puntos de datos das dúas fontes para consideralos un par.
- **Operación:** A operación matemática que se realizará sobre os valores dos puntos emparellados (suma, resta, multiplicación ou división).
- **En caso de ausencia:** Especifica o comportamento cando un punto de datos da primeira fonte non ten un punto de datos correspondente na segunda fonte dentro do limiar temporal indicado. As opcións inclúen:
  - **Omitir:** Non producir nada para ese punto de datos.
  - **Pasar sen cambios:** Producir o punto de datos orixinal da primeira fonte sen modificalo.

> **Nota:** A división por cero non é válida e considérase que faltan datos. A configuración «En caso de ausencia» determinará como se xestionan estes casos.
  	]],
  	["ka"] = [[
პირველ მონაცემთა წყაროში არსებულ თითოეულ მონაცემის წერტილს აწყვილებს მეორე წყაროს შესაბამის მონაცემის წერტილთან და მათ მნიშვნელობებზე ასრულებს მითითებულ ოპერაციას (მიმატება, გამოკლება, გამრავლება ან გაყოფა). თითოეული მონაცემის წერტილის წყვილი არის პირველი მონაცემის წერტილი, რომელიც მითითებულ დროის ზღვარში ხვდება. კონფიგურაცია:

- **დროის ზღვარი:** ორ წყაროში არსებულ მონაცემთა წერტილებს შორის წყვილად მიჩნევისთვის დასაშვები მაქსიმალური ხანგრძლივობა.
- **ოპერაცია:** შეწყვილებული მონაცემთა წერტილების მნიშვნელობებზე შესასრულებელი მათემატიკური ოპერაცია (მიმატება, გამოკლება, გამრავლება ან გაყოფა).
- **მონაცემის არქონისას:** განსაზღვრავს ქცევას, როდესაც პირველ წყაროში არსებულ მონაცემის წერტილს მითითებულ დროის ზღვარში მეორე წყაროში შესაბამისი მონაცემის წერტილი არ აქვს. ვარიანტები:
  - **გამოტოვება:** ამ მონაცემის წერტილისთვის არაფერი გამოიტანოს.
  - **გატარება:** პირველი წყაროდან თავდაპირველი მონაცემის წერტილი ცვლილების გარეშე გამოიტანოს.

> **შენიშვნა:** ნულზე გაყოფა არასწორია და მონაცემის არქონად ითვლება. ასეთ შემთხვევებში ქცევას „მონაცემის არქონისას“ კონფიგურაცია განსაზღვრავს.
  	]],
  	["de"] = [[
Paarweise jedem Datenpunkt der ersten Datenquelle den entsprechenden Datenpunkt der zweiten Datenquelle zu und führt eine angegebene Operation (Addition, Subtraktion, Multiplikation oder Division) auf deren Werten aus. Der Partner für jeden Datenpunkt ist der erste Datenpunkt, der innerhalb des angegebenen Zeitschwellenwerts liegt. Konfiguration:

- **Zeitschwellenwert:** Die maximale Dauer zwischen Datenpunkten der beiden Quellen, damit sie als Paar gelten.
- **Operation:** Die mathematische Operation, die auf die Werte der gepaarten Datenpunkte angewendet wird (Addition, Subtraktion, Multiplikation oder Division).
- **Bei fehlendem Partner:** Legt das Verhalten fest, wenn ein Datenpunkt der ersten Quelle innerhalb des angegebenen Zeitschwellenwerts keinen entsprechenden Datenpunkt in der zweiten Quelle hat. Optionen:
  - **Überspringen:** Für diesen Datenpunkt nichts ausgeben.
  - **Durchreichen:** Den ursprünglichen Datenpunkt der ersten Quelle unverändert ausgeben.

> **Hinweis:** Eine Division durch null ist ungültig und gilt als fehlende Daten. Die Einstellung „Bei fehlendem Partner“ legt fest, wie solche Fälle behandelt werden.
  	]],
  	["el"] = [[
Συνδυάζει κάθε σημείο δεδομένων της πρώτης πηγής δεδομένων με το αντίστοιχο σημείο δεδομένων της δεύτερης πηγής και εκτελεί μια καθορισμένη πράξη (πρόσθεση, αφαίρεση, πολλαπλασιασμό ή διαίρεση) στις τιμές τους. Το ζεύγος για κάθε σημείο δεδομένων είναι το πρώτο σημείο δεδομένων που βρίσκεται εντός του δεδομένου χρονικού ορίου. Διαμόρφωση:

- **Χρονικό όριο:** Η μέγιστη διάρκεια μεταξύ σημείων δεδομένων των δύο πηγών ώστε να θεωρηθούν ζεύγος.
- **Πράξη:** Η μαθηματική πράξη που θα εκτελεστεί στις τιμές των ζευγαρωμένων σημείων δεδομένων (πρόσθεση, αφαίρεση, πολλαπλασιασμός ή διαίρεση).
- **Σε περίπτωση απουσίας:** Καθορίζει τη συμπεριφορά όταν ένα σημείο δεδομένων της πρώτης πηγής δεν έχει αντίστοιχο σημείο δεδομένων στη δεύτερη πηγή εντός του δεδομένου χρονικού ορίου. Οι επιλογές περιλαμβάνουν:
  - **Παράλειψη:** Δεν παράγει έξοδο για αυτό το σημείο δεδομένων.
  - **Διέλευση:** Παράγει το αρχικό σημείο δεδομένων της πρώτης πηγής χωρίς τροποποίηση.

> **Σημείωση:** Η διαίρεση με το μηδέν δεν είναι έγκυρη και θεωρείται ελλιπές δεδομένο. Η διαμόρφωση «Σε περίπτωση απουσίας» καθορίζει τον τρόπο διαχείρισης αυτών των περιπτώσεων.
  	]],
  	["gu"] = [[
પ્રથમ ડેટા સ્રોતના દરેક ડેટા પોઇન્ટને બીજા ડેટા સ્રોતના અનુરૂપ ડેટા પોઇન્ટ સાથે જોડે છે અને તેમના મૂલ્યો પર નિર્દિષ્ટ ક્રિયા (સરવાળો, બાદબાકી, ગુણાકાર અથવા ભાગાકાર) કરે છે. દરેક ડેટા પોઇન્ટ માટેની જોડી આપેલ સમય મર્યાદાની અંદર આવતો પ્રથમ ડેટા પોઇન્ટ છે. ગોઠવણી:

- **સમય મર્યાદા:** બંને સ્રોતોના ડેટા પોઇન્ટ્સ વચ્ચે જોડી ગણાય તે માટેનો મહત્તમ સમયગાળો.
- **ક્રિયા:** જોડાયેલા ડેટા પોઇન્ટના મૂલ્યો પર કરવાની ગણિતીય ક્રિયા (સરવાળો, બાદબાકી, ગુણાકાર અથવા ભાગાકાર).
- **ગુમ થયેલ હોય ત્યારે:** પ્રથમ સ્રોતના ડેટા પોઇન્ટને આપેલી સમય મર્યાદામાં બીજા સ્રોતમાં અનુરૂપ ડેટા પોઇન્ટ ન મળે ત્યારે વર્તન નક્કી કરે છે. વિકલ્પોમાં સામેલ છે:
  - **છોડી દો:** તે ડેટા પોઇન્ટ માટે કંઈપણ આઉટપુટ ન આપો.
  - **આગળ પસાર કરો:** પ્રથમ સ્રોતનો મૂળ ડેટા પોઇન્ટ ફેરફાર વિના આઉટપુટ કરો.

> **નોંધ:** શૂન્ય વડે ભાગાકાર અમાન્ય છે અને તેને ગુમ થયેલ ડેટા ગણવામાં આવે છે. આવા કિસ્સાઓ કેવી રીતે સંભાળવા તે “ગુમ થયેલ હોય ત્યારે” ગોઠવણી નક્કી કરશે.
  	]],
  	["hi"] = [[
पहले डेटा स्रोत के प्रत्येक डेटा पॉइंट को दूसरे डेटा स्रोत के संबंधित डेटा पॉइंट के साथ युग्मित करता है और उनके मानों पर निर्दिष्ट संचालन (जोड़, घटाव, गुणा या भाग) करता है। प्रत्येक डेटा पॉइंट का युग्म वह पहला डेटा पॉइंट होता है जो दिए गए समय थ्रेशोल्ड के भीतर आता है। कॉन्फ़िगरेशन:

- **समय थ्रेशोल्ड:** दोनों स्रोतों के डेटा पॉइंट के बीच अधिकतम अवधि, जिसके भीतर उन्हें युग्म माना जाएगा।
- **संचालन:** युग्मित डेटा पॉइंट के मानों पर किया जाने वाला गणितीय संचालन (जोड़, घटाव, गुणा या भाग)।
- **अनुपलब्ध होने पर:** जब पहले स्रोत के डेटा पॉइंट का दूसरे स्रोत में दिए गए समय थ्रेशोल्ड के भीतर संबंधित डेटा पॉइंट न हो, तब व्यवहार निर्दिष्ट करता है। विकल्प:
  - **छोड़ें:** उस डेटा पॉइंट के लिए कुछ भी आउटपुट न दें।
  - **ज्यों का त्यों आगे भेजें:** पहले स्रोत के मूल डेटा पॉइंट को बिना बदलाव के आउटपुट करें।

> **नोट:** शून्य से भाग देना अमान्य है और इसे अनुपलब्ध डेटा माना जाता है। ऐसे मामलों को कैसे संभालना है, यह “अनुपलब्ध होने पर” कॉन्फ़िगरेशन निर्धारित करेगा।
  	]],
  	["hu"] = [[
Az első adatforrás minden adatpontját a második adatforrás megfelelő adatpontjával párosítja, majd megadott műveletet (összeadás, kivonás, szorzás vagy osztás) hajt végre az értékeiken. Minden adatponthoz az első olyan adatpontot választja párként, amely a megadott időküszöbön belül van. Konfiguráció:

- **Időküszöb:** A két forrás adatpontjai közötti maximális időtartam ahhoz, hogy párnak minősüljenek.
- **Művelet:** A párosított adatpontok értékein végrehajtandó matematikai művelet (összeadás, kivonás, szorzás vagy osztás).
- **Hiányzó adat esetén:** Meghatározza a viselkedést, ha az első forrás adatpontjához nem tartozik megfelelő adatpont a második forrásban a megadott időküszöbön belül. Lehetséges beállítások:
  - **Kihagyás:** Nem ad ki semmit az adott adatponthoz.
  - **Átengedés:** Módosítás nélkül kiadja az első forrás eredeti adatpontját.

> **Megjegyzés:** A nullával való osztás érvénytelen, és hiányzó adatnak minősül. A „Hiányzó adat esetén” beállítás határozza meg az ilyen esetek kezelését.
  	]],
  	["is"] = [[
Parar hvern gagnapunkt í fyrsta gagnagjafanum við samsvarandi gagnapunkt í öðrum gagnagjafanum og framkvæmir tilgreinda aðgerð (samlagningu, frádrátt, margföldun eða deilingu) á gildum þeirra. Samsvarandi gagnapunktur hvers gagnapunkts er fyrsti gagnapunkturinn sem fellur innan tilgreindra tímamarka. Stillingar:

- **Tímamörk:** Hámarkstími milli gagnapunkta í gagnagjöfunum tveimur til að þeir teljist par.
- **Aðgerð:** Stærðfræðilega aðgerðin sem framkvæma á á gildum paraðra gagnapunkta (samlagning, frádráttur, margföldun eða deiling).
- **Ef vantar:** Tilgreinir hegðun þegar gagnapunktur í fyrsta gagnagjafanum á engan samsvarandi gagnapunkt í öðrum gagnagjafanum innan tilgreindra tímamarka. Valkostir:
  - **Sleppa:** Ekki gefa neitt út fyrir þann gagnapunkt.
  - **Hleypa í gegn:** Gefa upphaflega gagnapunktinn úr fyrsta gagnagjafanum út án breytinga.

> **Athugið:** Deiling með núlli er ógild og telst gögn vanta. Stillingin „Ef vantar“ ákvarðar hvernig slík tilfelli eru meðhöndluð.
  	]],
  	["id"] = [[
Memasangkan setiap titik data dalam sumber data pertama dengan titik data yang sesuai dalam sumber data kedua, lalu melakukan operasi tertentu (penjumlahan, pengurangan, perkalian, atau pembagian) pada nilainya. Pasangan untuk setiap titik data adalah titik data pertama yang berada dalam ambang waktu yang diberikan. Konfigurasi:

- **Ambang Waktu:** Durasi maksimum antara titik data dalam kedua sumber agar dianggap sebagai pasangan.
- **Operasi:** Operasi matematika yang dilakukan pada nilai titik data yang dipasangkan (penjumlahan, pengurangan, perkalian, atau pembagian).
- **Jika Tidak Ada:** Menentukan perilaku saat titik data dalam sumber pertama tidak memiliki titik data yang sesuai dalam sumber kedua dalam ambang waktu yang diberikan. Pilihan meliputi:
  - **Lewati:** Tidak menghasilkan apa pun untuk titik data tersebut.
  - **Teruskan:** Menghasilkan titik data asli dari sumber pertama tanpa perubahan.

> **Catatan:** Pembagian dengan nol tidak valid dan dianggap sebagai data yang hilang. Konfigurasi Jika Tidak Ada menentukan cara menangani kasus tersebut.
  	]],
  	["it"] = [[
Abbina ogni punto dati della prima origine dati al punto dati corrispondente della seconda origine dati ed esegue un'operazione specificata (addizione, sottrazione, moltiplicazione o divisione) sui relativi valori. Per ogni punto dati, l'abbinamento è il primo punto dati che rientra nella soglia temporale specificata. Configurazione:

- **Soglia temporale:** la durata massima tra i punti dati delle due origini affinché siano considerati una coppia.
- **Operazione:** l'operazione matematica da eseguire sui valori dei punti dati abbinati (addizione, sottrazione, moltiplicazione o divisione).
- **In caso di assenza:** specifica il comportamento quando un punto dati della prima origine non ha un punto dati corrispondente nella seconda origine entro la soglia temporale specificata. Le opzioni includono:
  - **Ignora:** non produce alcun output per quel punto dati.
  - **Trasmetti:** produce il punto dati originale della prima origine senza modificarlo.

> **Nota:** la divisione per zero non è valida e viene considerata come dato mancante. La configurazione In caso di assenza determina come gestire questi casi.
  	]],
  	["ja"] = [[
1つ目のデータソースの各データポイントを、2つ目のデータソースの対応するデータポイントとペアにし、値に対して指定した演算（加算、減算、乗算、除算）を実行します。各データポイントのペアは、指定した時間しきい値以内にある最初のデータポイントです。設定:

- **時間しきい値:** 2つのソースのデータポイント間で、ペアとみなす最大時間差。
- **演算:** ペアになったデータポイントの値に対して実行する数学演算（加算、減算、乗算、除算）。
- **見つからない場合:** 指定した時間しきい値以内に、1つ目のソースのデータポイントに対応するデータポイントが2つ目のソースにない場合の動作。選択肢:
  - **スキップ:** そのデータポイントについて何も出力しません。
  - **そのまま通過:** 1つ目のソースの元のデータポイントを変更せずに出力します。

> **注:** 0による除算は無効で、データ欠損として扱われます。このような場合の処理は「見つからない場合」の設定で決まります。
  	]],
  	["kn"] = [[
ಮೊದಲ ಡೇಟಾ ಮೂಲದಲ್ಲಿನ ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವನ್ನು ಎರಡನೇ ಡೇಟಾ ಮೂಲದಲ್ಲಿನ ಸಂಬಂಧಿತ ಡೇಟಾ ಬಿಂದುವಿನೊಂದಿಗೆ ಜೋಡಿಸಿ, ಅವುಗಳ ಮೌಲ್ಯಗಳ ಮೇಲೆ ನಿರ್ದಿಷ್ಟ ಕಾರ್ಯಾಚರಣೆ (ಸೇರಿಸುವಿಕೆ, ಕಡಿತ, ಗುಣಾಕಾರ ಅಥವಾ ಭಾಗಾಕಾರ) ನಡೆಸುತ್ತದೆ. ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಜೋಡಿ ಎಂದರೆ ನೀಡಿರುವ ಸಮಯ ಮಿತಿಯೊಳಗೆ ಬರುವ ಮೊದಲ ಡೇಟಾ ಬಿಂದು. ಕಾನ್ಫಿಗರೇಶನ್:

- **ಸಮಯ ಮಿತಿ:** ಎರಡು ಮೂಲಗಳ ಡೇಟಾ ಬಿಂದುಗಳ ನಡುವಿನ ಜೋಡಿಯಾಗಿ ಪರಿಗಣಿಸಬಹುದಾದ ಗರಿಷ್ಠ ಅವಧಿ.
- **ಕಾರ್ಯಾಚರಣೆ:** ಜೋಡಿಸಲಾದ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳ ಮೇಲೆ ನಡೆಸುವ ಗಣಿತೀಯ ಕಾರ್ಯಾಚರಣೆ (ಸೇರಿಸುವಿಕೆ, ಕಡಿತ, ಗುಣಾಕಾರ ಅಥವಾ ಭಾಗಾಕಾರ).
- **ಲಭ್ಯವಿಲ್ಲದಾಗ:** ಮೊದಲ ಮೂಲದಲ್ಲಿನ ಡೇಟಾ ಬಿಂದುವಿಗೆ ನೀಡಿರುವ ಸಮಯ ಮಿತಿಯೊಳಗೆ ಎರಡನೇ ಮೂಲದಲ್ಲಿ ಸಂಬಂಧಿತ ಡೇಟಾ ಬಿಂದು ಇಲ್ಲದಿದ್ದಾಗ ಕೈಗೊಳ್ಳುವ ಕ್ರಮ. ಆಯ್ಕೆಗಳು:
  - **ಬಿಟ್ಟುಬಿಡಿ:** ಆ ಡೇಟಾ ಬಿಂದುವಿಗೆ ಯಾವುದೇ ಔಟ್‌ಪುಟ್ ನೀಡಬೇಡಿ.
  - **ಮೂಲದಂತೆ ಮುಂದುವರಿಸಿ:** ಮೊದಲ ಮೂಲದ ಮೂಲ ಡೇಟಾ ಬಿಂದುವನ್ನು ಬದಲಾವಣೆ ಇಲ್ಲದೆ ಔಟ್‌ಪುಟ್ ಮಾಡಿ.

> **ಟಿಪ್ಪಣಿ:** ಶೂನ್ಯದಿಂದ ಭಾಗಾಕಾರ ಅಮಾನ್ಯವಾಗಿದೆ ಮತ್ತು ಕಾಣೆಯಾದ ಡೇಟಾ ಎಂದು ಪರಿಗಣಿಸಲಾಗುತ್ತದೆ. ಇಂತಹ ಸಂದರ್ಭಗಳನ್ನು ಹೇಗೆ ನಿರ್ವಹಿಸಬೇಕು ಎಂಬುದನ್ನು ಲಭ್ಯವಿಲ್ಲದಾಗ ಕಾನ್ಫಿಗರೇಶನ್ ನಿರ್ಧರಿಸುತ್ತದೆ.
  	]],
  	["kk"] = [[
Бірінші дерек көзіндегі әр дерек нүктесін екінші дерек көзіндегі сәйкес дерек нүктесімен жұптап, олардың мәндеріне көрсетілген амалдың (қосу, азайту, көбейту немесе бөлу) бірін орындайды. Әр дерек нүктесіне арналған жұп — берілген уақыт шегіне түсетін бірінші дерек нүктесі. Конфигурация:

- **Уақыт шегі:** Екі көздегі дерек нүктелерінің жұп деп есептелуі үшін олардың арасындағы ең ұзақ уақыт аралығы.
- **Амал:** Жұпталған дерек нүктелерінің мәндеріне орындалатын математикалық амал (қосу, азайту, көбейту немесе бөлу).
- **Дерек жоқ кездегі әрекет:** Бірінші көздегі дерек нүктесінің берілген уақыт шегінде екінші көзде сәйкес дерек нүктесі болмағандағы әрекет. Опциялар:
  - **Өткізу:** Бұл дерек нүктесі үшін ештеңе шығармау.
  - **Өзгеріссіз өткізу:** Бірінші көздегі бастапқы дерек нүктесін өзгертпей шығару.

> **Ескерту:** Нөлге бөлу жарамсыз және деректер жоқ деп есептеледі. Мұндай жағдайлардың қалай өңделетінін «Дерек жоқ кездегі әрекет» конфигурациясы анықтайды.
  	]],
  	["km"] = [[
ផ្គូផ្គងចំណុចទិន្នន័យនីមួយៗក្នុងប្រភពទិន្នន័យទីមួយ ជាមួយចំណុចទិន្នន័យដែលត្រូវគ្នាក្នុងប្រភពទិន្នន័យទីពីរ ហើយអនុវត្តប្រតិបត្តិការដែលបានបញ្ជាក់ (បូក ដក គុណ ឬចែក) លើតម្លៃរបស់ពួកវា។ គូសម្រាប់ចំណុចទិន្នន័យនីមួយៗ គឺជាចំណុចទិន្នន័យដំបូងដែលស្ថិតក្នុងកម្រិតពេលវេលាដែលបានកំណត់។ ការកំណត់រចនា៖

- **កម្រិតពេលវេលា៖** រយៈពេលអតិបរមារវាងចំណុចទិន្នន័យក្នុងប្រភពទាំងពីរ ដើម្បីចាត់ទុកថាជាគូ។
- **ប្រតិបត្តិការ៖** ប្រតិបត្តិការគណិតវិទ្យាដែលត្រូវអនុវត្តលើតម្លៃចំណុចទិន្នន័យដែលបានផ្គូផ្គង (បូក ដក គុណ ឬចែក)។
- **នៅពេលបាត់៖** បញ្ជាក់ឥរិយាបថនៅពេលចំណុចទិន្នន័យក្នុងប្រភពទីមួយ មិនមានចំណុចទិន្នន័យដែលត្រូវគ្នាក្នុងប្រភពទីពីរ ក្នុងកម្រិតពេលវេលាដែលបានកំណត់។ ជម្រើសរួមមាន៖
  - **រំលង៖** មិនបញ្ចេញអ្វីសម្រាប់ចំណុចទិន្នន័យនោះ។
  - **បញ្ជូនឆ្លងកាត់៖** បញ្ចេញចំណុចទិន្នន័យដើមពីប្រភពទីមួយ ដោយមិនកែប្រែ។

> **ចំណាំ៖** ការចែកនឹងសូន្យមិនត្រឹមត្រូវ ហើយត្រូវបានចាត់ទុកជាទិន្នន័យបាត់។ ការកំណត់ នៅពេលបាត់ នឹងកំណត់របៀបដោះស្រាយករណីទាំងនេះ។
  	]],
  	["ko"] = [[
첫 번째 데이터 소스의 각 데이터 포인트를 두 번째 데이터 소스의 해당 데이터 포인트와 짝지어 값에 지정한 연산(더하기, 빼기, 곱하기 또는 나누기)을 수행합니다. 각 데이터 포인트의 짝은 지정한 시간 임계값 내에 있는 첫 번째 데이터 포인트입니다. 구성:

- **시간 임계값:** 두 소스의 데이터 포인트를 쌍으로 간주할 수 있는 최대 시간 간격입니다.
- **연산:** 짝지은 데이터 포인트 값에 수행할 수학 연산(더하기, 빼기, 곱하기 또는 나누기)입니다.
- **누락 시:** 첫 번째 소스의 데이터 포인트에 대해 지정한 시간 임계값 내에 두 번째 소스의 해당 데이터 포인트가 없을 때의 동작입니다. 옵션:
  - **건너뛰기:** 해당 데이터 포인트에 대해 아무것도 출력하지 않습니다.
  - **통과:** 첫 번째 소스의 원래 데이터 포인트를 수정 없이 출력합니다.

> **참고:** 0으로 나누기는 유효하지 않으며 누락된 데이터로 간주됩니다. 이러한 경우의 처리는 누락 시 설정에 따라 결정됩니다.
  	]],
  	["ky"] = [[
Биринчи маалымат булагындагы ар бир маалымат чекитин экинчи маалымат булагындагы тиешелүү маалымат чекити менен жупташтырып, алардын маанилерине көрсөтүлгөн операцияны (кошуу, кемитүү, көбөйтүү же бөлүү) аткарат. Ар бир маалымат чекити үчүн жуп — берилген убакыт чегинин ичинде кездешкен биринчи маалымат чекити. Тууралоо:

- **Убакыт чеги:** Эки булактагы маалымат чекиттеринин жуп деп эсептелиши үчүн алардын ортосундагы максималдуу убакыт.
- **Операция:** Жупташкан маалымат чекиттеринин маанилерине аткарылуучу математикалык операция (кошуу, кемитүү, көбөйтүү же бөлүү).
- **Жок болгондо:** Биринчи булактагы маалымат чекитине берилген убакыт чегинде экинчи булактан тиешелүү маалымат чекити табылбагандагы аракетти аныктайт. Параметрлер:
  - **Өткөрүп жиберүү:** Бул маалымат чекити үчүн эч нерсе чыгарбоо.
  - **Өзгөртүүсүз өткөрүү:** Биринчи булактан алынган баштапкы маалымат чекитин өзгөртүүсүз чыгаруу.

> **Эскертүү:** Нөлгө бөлүү жараксыз жана жок маалымат катары эсептелет. Мындай учурлар кандай иштетилерин «Жок болгондо» тууралоосу аныктайт.
  	]],
  	["lo"] = [[
ຈັບຄູ່ຈຸດຂໍ້ມູນແຕ່ລະຈຸດໃນແຫຼ່ງຂໍ້ມູນທຳອິດກັບຈຸດຂໍ້ມູນທີ່ກົງກັນໃນແຫຼ່ງຂໍ້ມູນທີສອງ ແລະ ດຳເນີນການທີ່ລະບຸ (ບວກ, ລົບ, ຄູນ ຫຼື ຫານ) ກັບຄ່າຂອງພວກມັນ. ຄູ່ຂອງແຕ່ລະຈຸດຂໍ້ມູນແມ່ນຈຸດຂໍ້ມູນທຳອິດທີ່ຢູ່ພາຍໃນຂອບເຂດເວລາທີ່ກຳນົດ. ການຕັ້ງຄ່າ:

- **ເກນເວລາ:** ໄລຍະເວລາສູງສຸດລະຫວ່າງຈຸດຂໍ້ມູນໃນສອງແຫຼ່ງ ເພື່ອໃຫ້ຖືກພິຈາລະນາເປັນຄູ່.
- **ການດຳເນີນການ:** ການດຳເນີນການທາງຄະນິດສາດທີ່ຈະເຮັດກັບຄ່າຂອງຈຸດຂໍ້ມູນທີ່ຈັບຄູ່ (ບວກ, ລົບ, ຄູນ ຫຼື ຫານ).
- **ເມື່ອຂາດ:** ລະບຸພຶດຕິກຳເມື່ອຈຸດຂໍ້ມູນໃນແຫຼ່ງທຳອິດບໍ່ມີຈຸດຂໍ້ມູນທີ່ກົງກັນໃນແຫຼ່ງທີສອງພາຍໃນເກນເວລາທີ່ກຳນົດ. ຕົວເລືອກລວມມີ:
  - **ຂ້າມ:** ບໍ່ສົ່ງອອກຫຍັງສຳລັບຈຸດຂໍ້ມູນນັ້ນ.
  - **ສົ່ງຕໍ່:** ສົ່ງອອກຈຸດຂໍ້ມູນເດີມຈາກແຫຼ່ງທຳອິດໂດຍບໍ່ປ່ຽນແປງ.

> **ໝາຍເຫດ:** ການຫານດ້ວຍສູນແມ່ນບໍ່ຖືກຕ້ອງ ແລະ ຖືວ່າເປັນຂໍ້ມູນຂາດ. ການຕັ້ງຄ່າເມື່ອຂາດຈະກຳນົດວິທີຈັດການກໍລະນີດັ່ງກ່າວ.
  	]],
  	["lv"] = [[
Savieno katru datu punktu pirmajā datu avotā ar atbilstošo datu punktu otrajā datu avotā un veic norādīto darbību (saskaitīšanu, atņemšanu, reizināšanu vai dalīšanu) ar to vērtībām. Katra datu punkta pāris ir pirmais datu punkts, kas ietilpst norādītajā laika slieksnī. Konfigurācija:

- **Laika slieksnis:** Maksimālais ilgums starp datu punktiem abos avotos, lai tos uzskatītu par pāri.
- **Darbība:** Matemātiskā darbība, kas jāveic ar pāra datu punktu vērtībām (saskaitīšana, atņemšana, reizināšana vai dalīšana).
- **Ja trūkst:** Norāda darbību, ja datu punktam pirmajā avotā noteiktajā laika slieksnī nav atbilstoša datu punkta otrajā avotā. Iespējas:
  - **Izlaist:** Šim datu punktam neko neizvadīt.
  - **Nodot tālāk:** Bez izmaiņām izvadīt sākotnējo datu punktu no pirmā avota.

> **Piezīme:** Dalīšana ar nulli nav derīga un tiek uzskatīta par trūkstošiem datiem. Iestatījums “Ja trūkst” nosaka, kā šādi gadījumi tiek apstrādāti.
  	]],
  	["lt"] = [[
Suporuoja kiekvieną pirmojo duomenų šaltinio duomenų tašką su atitinkamu antrojo duomenų šaltinio duomenų tašku ir atlieka nurodytą jų reikšmių operaciją (sudėtį, atimtį, daugybą arba dalybą). Kiekvieno duomenų taško pora yra pirmasis duomenų taškas, patenkantis į nurodytą laiko ribą. Konfigūracija:

- **Laiko riba:** Didžiausia trukmė tarp dviejų šaltinių duomenų taškų, kad jie būtų laikomi pora.
- **Operacija:** Su suporuotų duomenų taškų reikšmėmis atliekama matematinė operacija (sudėtis, atimtis, daugyba arba dalyba).
- **Kai trūksta:** Nurodo elgseną, kai pirmojo šaltinio duomenų taškas per nurodytą laiko ribą neturi atitinkamo duomenų taško antrajame šaltinyje. Parinktys:
  - **Praleisti:** Šiam duomenų taškui nieko neišvesti.
  - **Perduoti:** Nepakeistą išvesti pradinį pirmojo šaltinio duomenų tašką.

> **Pastaba:** Dalyba iš nulio negalima ir laikoma trūkstamais duomenimis. Parinktis „Kai trūksta“ nustatys, kaip tokie atvejai tvarkomi.
  	]],
  	["mk"] = [[
Ја спарува секоја точка на податоци во првиот извор на податоци со соодветната точка на податоци во вториот извор и извршува зададена операција (собирање, одземање, множење или делење) врз нивните вредности. Парата за секоја точка на податоци е првата точка на податоци што спаѓа во зададениот временски праг. Конфигурација:

- **Временски праг:** Максималното времетраење помеѓу точките на податоци во двата извора за да се сметаат за пар.
- **Операција:** Математичката операција што се извршува врз вредностите на спарените точки на податоци (собирање, одземање, множење или делење).
- **При недостиг:** Го одредува однесувањето кога точка на податоци во првиот извор нема соодветна точка на податоци во вториот извор во зададениот временски праг. Опциите вклучуваат:
  - **Прескокни:** Не создавај излез за таа точка на податоци.
  - **Пропушти:** Создај ја оригиналната точка на податоци од првиот извор без измени.

> **Забелешка:** Делењето со нула е невалидно и се смета за недостиг на податоци. Конфигурацијата „При недостиг“ ќе одреди како се постапува во таквите случаи.
  	]],
  	["ms"] = [[
Memadankan setiap titik data dalam sumber data pertama dengan titik data yang sepadan dalam sumber data kedua dan melaksanakan operasi yang ditentukan (tambah, tolak, darab atau bahagi) pada nilainya. Padanan bagi setiap titik data ialah titik data pertama yang berada dalam ambang masa yang diberikan. Konfigurasi:

- **Ambang Masa:** Tempoh maksimum antara titik data dalam kedua-dua sumber untuk dianggap sebagai padanan.
- **Operasi:** Operasi matematik yang hendak dilaksanakan pada nilai titik data yang dipadankan (tambah, tolak, darab atau bahagi).
- **Jika Tiada:** Menentukan tingkah laku apabila titik data dalam sumber pertama tidak mempunyai titik data sepadan dalam sumber kedua dalam ambang masa yang diberikan. Pilihan termasuk:
  - **Langkau:** Jangan keluarkan apa-apa untuk titik data tersebut.
  - **Teruskan:** Keluarkan titik data asal daripada sumber pertama tanpa pengubahsuaian.

> **Nota:** Pembahagian dengan sifar tidak sah dan dianggap sebagai data yang tiada. Konfigurasi Jika Tiada akan menentukan cara kes tersebut dikendalikan.
  	]],
  	["ml"] = [[
ആദ്യ ഡാറ്റാ ഉറവിടത്തിലെ ഓരോ ഡാറ്റാ പോയിന്റിനെയും രണ്ടാമത്തെ ഡാറ്റാ ഉറവിടത്തിലെ അനുബന്ധ ഡാറ്റാ പോയിന്റുമായി ജോഡിയാക്കി, അവയുടെ മൂല്യങ്ങളിൽ നിർദ്ദിഷ്ട പ്രവർത്തനം (കൂട്ടൽ, കുറയ്ക്കൽ, ഗുണനം, അല്ലെങ്കിൽ വിഭജനം) നടത്തുന്നു. ഓരോ ഡാറ്റാ പോയിന്റിനുമുള്ള ജോഡി നൽകിയ സമയപരിധിക്കുള്ളിൽ വരുന്ന ആദ്യ ഡാറ്റാ പോയിന്റാണ്. കോൺഫിഗറേഷൻ:

- **Time Threshold:** രണ്ട് ഉറവിടങ്ങളിലെ ഡാറ്റാ പോയിന്റുകൾ ജോഡിയായി പരിഗണിക്കാനുള്ള പരമാവധി ഇടവേള.
- **Operation:** ജോഡിയാക്കിയ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളിൽ നടത്തേണ്ട ഗണിത പ്രവർത്തനം (കൂട്ടൽ, കുറയ്ക്കൽ, ഗുണനം, അല്ലെങ്കിൽ വിഭജനം).
- **On Missing:** ആദ്യ ഉറവിടത്തിലെ ഒരു ഡാറ്റാ പോയിന്റിന് നൽകിയ സമയപരിധിക്കുള്ളിൽ രണ്ടാമത്തെ ഉറവിടത്തിൽ അനുബന്ധ ഡാറ്റാ പോയിന്റ് ഇല്ലാത്തപ്പോൾ ചെയ്യേണ്ടത്. ഓപ്ഷനുകൾ:
  - **Skip:** ആ ഡാറ്റാ പോയിന്റിനായി ഒന്നും ഔട്ട്പുട്ട് ചെയ്യരുത്.
  - **Pass Through:** ആദ്യ ഉറവിടത്തിലെ യഥാർത്ഥ ഡാറ്റാ പോയിന്റ് മാറ്റമില്ലാതെ ഔട്ട്പുട്ട് ചെയ്യുക.

> **കുറിപ്പ്:** പൂജ്യത്താൽ വിഭജിക്കുന്നത് അസാധുവാണ്, കൂടാതെ ഡാറ്റ ഇല്ലാത്തതായി കണക്കാക്കുന്നു. ഇത്തരം സാഹചര്യങ്ങൾ എങ്ങനെ കൈകാര്യം ചെയ്യണമെന്ന് On Missing കോൺഫിഗറേഷൻ നിർണ്ണയിക്കും.
  	]],
  	["mr"] = [[
पहिल्या डेटा स्रोतामधील प्रत्येक डेटा पॉइंटची दुसऱ्या डेटा स्रोतामधील संबंधित डेटा पॉइंटशी जोडी लावते आणि त्यांच्या मूल्यांवर निर्दिष्ट क्रिया (बेरीज, वजाबाकी, गुणाकार किंवा भागाकार) करते. प्रत्येक डेटा पॉइंटसाठीची जोडी ही दिलेल्या वेळेच्या मर्यादेत येणारा पहिला डेटा पॉइंट असतो. कॉन्फिगरेशन:

- **वेळेची मर्यादा:** जोडी मानण्यासाठी दोन स्रोतांमधील डेटा पॉइंट्समधील कमाल कालावधी.
- **क्रिया:** जोडीतील डेटा पॉइंट्सच्या मूल्यांवर करायची गणितीय क्रिया (बेरीज, वजाबाकी, गुणाकार किंवा भागाकार).
- **गहाळ असल्यास:** पहिल्या स्रोतामधील डेटा पॉइंटसाठी दिलेल्या वेळेच्या मर्यादेत दुसऱ्या स्रोतामध्ये संबंधित डेटा पॉइंट नसल्यास काय करायचे ते निर्दिष्ट करते. पर्याय:
  - **वगळा:** त्या डेटा पॉइंटसाठी काहीही आउटपुट करू नका.
  - **जसाच्या तसा पास करा:** पहिल्या स्रोतामधील मूळ डेटा पॉइंटमध्ये बदल न करता आउटपुट करा.

> **टीप:** शून्याने भागाकार अवैध मानला जातो आणि तो गहाळ डेटा समजला जातो. अशा प्रकरणांचे व्यवस्थापन गहाळ असल्यास कॉन्फिगरेशननुसार केले जाईल.
  	]],
  	["mn"] = [[
Эхний өгөгдлийн эх үүсвэрийн өгөгдлийн цэг бүрийг хоёр дахь эх үүсвэрийн харгалзах өгөгдлийн цэгтэй хослуулж, утгууд дээр нь заасан үйлдлийг (нэмэх, хасах, үржүүлэх эсвэл хуваах) хийнэ. Цэг бүрийн хос нь өгөгдсөн хугацааны босго доторх хамгийн эхний өгөгдлийн цэг байна. Тохиргоо:

- **Хугацааны босго:** Хоёр эх үүсвэрийн өгөгдлийн цэгүүдийг хос гэж үзэх хамгийн их хугацааны зөрүү.
- **Үйлдэл:** Хосолсон өгөгдлийн цэгүүдийн утгад хийх математик үйлдэл (нэмэх, хасах, үржүүлэх эсвэл хуваах).
- **Олдохгүй үед:** Эхний эх үүсвэрийн өгөгдлийн цэгт өгөгдсөн хугацааны босго дотор хоёр дахь эх үүсвэрт харгалзах цэг байхгүй үед хийх үйлдэл. Сонголтууд:
  - **Алгасах:** Тухайн өгөгдлийн цэгт юу ч гаргахгүй.
  - **Шууд дамжуулах:** Эхний эх үүсвэрийн өгөгдлийн цэгийг өөрчлөлтгүй гаргах.

> **Тэмдэглэл:** Тэгээр хуваах нь хүчингүй бөгөөд өгөгдөл байхгүй гэж үзнэ. Ийм тохиолдлыг хэрхэн боловсруулахыг “Олдохгүй үед” тохиргоо тодорхойлно.
  	]],
  	["ne"] = [[
पहिलो डेटा स्रोतका प्रत्येक डेटा बिन्दुलाई दोस्रो डेटा स्रोतको सम्बन्धित डेटा बिन्दुसँग जोडी बनाई तिनका मानमा निर्दिष्ट सञ्चालन (जोड, घटाउ, गुणन वा भाग) गर्छ। प्रत्येक डेटा बिन्दुको जोडी दिइएको समय सीमाभित्र पर्ने पहिलो डेटा बिन्दु हो। कन्फिगरेसन:

- **समय सीमा:** दुई स्रोतका डेटा बिन्दुबीच जोडी मान्न सकिने अधिकतम अवधि।
- **सञ्चालन:** जोडी बनाइएका डेटा बिन्दुका मानमा लागू गर्ने गणितीय सञ्चालन (जोड, घटाउ, गुणन वा भाग)।
- **नभेटिँदा:** पहिलो स्रोतको डेटा बिन्दुसँग दिइएको समय सीमाभित्र दोस्रो स्रोतमा सम्बन्धित डेटा बिन्दु नभएमा गर्ने व्यवहार। विकल्पहरू:
  - **छोड्ने:** उक्त डेटा बिन्दुका लागि केही पनि आउटपुट नगर्ने।
  - **जस्ताको तस्तै पठाउने:** पहिलो स्रोतको मूल डेटा बिन्दुलाई परिवर्तन नगरी आउटपुट गर्ने।

> **नोट:** शून्यले भाग गर्नु अमान्य मानिन्छ र हराएको डेटा ठानिन्छ। यस्ता अवस्थालाई कसरी सम्हाल्ने भन्ने कुरा नभेटिँदा कन्फिगरेसनले निर्धारण गर्छ।
  	]],
  	["no"] = [[
Parer hvert datapunkt i den første datakilden med det tilsvarende datapunktet i den andre datakilden og utfører en angitt operasjon (addisjon, subtraksjon, multiplikasjon eller divisjon) på verdiene deres. Paret for hvert datapunkt er det første datapunktet som faller innenfor den angitte tidsgrensen. Konfigurasjon:

- **Tidsgrense:** Maksimal varighet mellom datapunkter i de to kildene for at de skal regnes som et par.
- **Operasjon:** Den matematiske operasjonen som skal utføres på verdiene til datapunktene i paret (addisjon, subtraksjon, multiplikasjon eller divisjon).
- **Ved manglende:** Angir hva som skal skje når et datapunkt i den første kilden ikke har et tilsvarende datapunkt i den andre kilden innenfor den angitte tidsgrensen. Alternativene inkluderer:
  - **Hopp over:** Ikke produser noe for datapunktet.
  - **Send videre:** Produser det opprinnelige datapunktet fra den første kilden uten endringer.

> **Merk:** Divisjon på null er ugyldig og regnes som manglende data. Innstillingen Ved manglende avgjør hvordan slike tilfeller håndteres.
  	]],
  	["pl"] = [[
Łączy każdy punkt danych z pierwszego źródła danych z odpowiadającym mu punktem danych z drugiego źródła i wykonuje określone działanie (dodawanie, odejmowanie, mnożenie lub dzielenie) na ich wartościach. Dla każdego punktu danych parą jest pierwszy punkt danych mieszczący się w podanym progu czasowym. Konfiguracja:

- **Próg czasowy:** Maksymalny czas między punktami danych z obu źródeł, aby uznać je za parę.
- **Działanie:** Działanie matematyczne wykonywane na wartościach połączonych punktów danych (dodawanie, odejmowanie, mnożenie lub dzielenie).
- **Przy braku:** Określa zachowanie, gdy punkt danych z pierwszego źródła nie ma odpowiadającego punktu danych w drugim źródle w podanym progu czasowym. Dostępne opcje:
  - **Pomiń:** Nie generuj nic dla tego punktu danych.
  - **Przekaż dalej:** Wygeneruj oryginalny punkt danych z pierwszego źródła bez zmian.

> **Uwaga:** Dzielenie przez zero jest nieprawidłowe i jest traktowane jako brak danych. Konfiguracja „Przy braku” określa sposób obsługi takich przypadków.
  	]],
  	["pt"] = [[
Emparelha cada ponto de dados da primeira fonte de dados com o ponto de dados correspondente da segunda fonte de dados e executa uma operação especificada (adição, subtração, multiplicação ou divisão) nos respetivos valores. O par de cada ponto de dados é o primeiro ponto de dados que ocorre dentro do limite de tempo indicado. Configuração:

- **Limite de tempo:** A duração máxima entre pontos de dados das duas fontes para serem considerados um par.
- **Operação:** A operação matemática a executar nos valores dos pontos emparelhados (adição, subtração, multiplicação ou divisão).
- **Quando falta:** Especifica o comportamento quando um ponto de dados da primeira fonte não tem um ponto de dados correspondente na segunda fonte dentro do limite de tempo indicado. As opções incluem:
  - **Ignorar:** Não produzir nada para esse ponto de dados.
  - **Passar:** Produzir o ponto de dados original da primeira fonte sem modificações.

> **Nota:** A divisão por zero é inválida e considerada como dados em falta. A configuração Quando falta determina como estes casos são tratados.
  	]],
  	["pa"] = [[
ਪਹਿਲੇ ਡਾਟਾ ਸਰੋਤ ਦੇ ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਨੂੰ ਦੂਜੇ ਡਾਟਾ ਸਰੋਤ ਦੇ ਸੰਬੰਧਿਤ ਡਾਟਾ ਪੁਆਇੰਟ ਨਾਲ ਜੋੜਦਾ ਹੈ ਅਤੇ ਉਨ੍ਹਾਂ ਦੇ ਮੁੱਲਾਂ 'ਤੇ ਨਿਰਧਾਰਤ ਕਾਰਵਾਈ (ਜੋੜ, ਘਟਾਓ, ਗੁਣਾ ਜਾਂ ਭਾਗ) ਕਰਦਾ ਹੈ। ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਲਈ ਜੋੜਾ ਦਿੱਤੇ ਸਮੇਂ ਦੇ ਥ੍ਰੈਸ਼ਹੋਲਡ ਅੰਦਰ ਆਉਣ ਵਾਲਾ ਪਹਿਲਾ ਡਾਟਾ ਪੁਆਇੰਟ ਹੁੰਦਾ ਹੈ। ਕੌਂਫਿਗਰੇਸ਼ਨ:

- **ਸਮਾਂ ਥ੍ਰੈਸ਼ਹੋਲਡ:** ਦੋ ਸਰੋਤਾਂ ਦੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਵਿਚਕਾਰ ਜੋੜਾ ਮੰਨੀ ਜਾਣ ਵਾਲੀ ਅਧਿਕਤਮ ਮਿਆਦ।
- **ਕਾਰਵਾਈ:** ਜੋੜੇ ਦੇ ਡਾਟਾ ਪੁਆਇੰਟ ਮੁੱਲਾਂ 'ਤੇ ਕਰਨ ਵਾਲੀ ਗਣਿਤੀ ਕਾਰਵਾਈ (ਜੋੜ, ਘਟਾਓ, ਗੁਣਾ ਜਾਂ ਭਾਗ)।
- **ਗੁੰਮ ਹੋਣ 'ਤੇ:** ਜਦੋਂ ਪਹਿਲੇ ਸਰੋਤ ਦੇ ਡਾਟਾ ਪੁਆਇੰਟ ਦਾ ਦਿੱਤੇ ਸਮੇਂ ਦੇ ਥ੍ਰੈਸ਼ਹੋਲਡ ਅੰਦਰ ਦੂਜੇ ਸਰੋਤ ਵਿੱਚ ਸੰਬੰਧਿਤ ਡਾਟਾ ਪੁਆਇੰਟ ਨਾ ਹੋਵੇ, ਤਾਂ ਵਿਹਾਰ ਨਿਰਧਾਰਤ ਕਰਦਾ ਹੈ। ਵਿਕਲਪ:
  - **ਛੱਡੋ:** ਉਸ ਡਾਟਾ ਪੁਆਇੰਟ ਲਈ ਕੁਝ ਵੀ ਆਉਟਪੁੱਟ ਨਾ ਕਰੋ।
  - **ਅੱਗੇ ਭੇਜੋ:** ਪਹਿਲੇ ਸਰੋਤ ਦਾ ਮੂਲ ਡਾਟਾ ਪੁਆਇੰਟ ਬਿਨਾਂ ਬਦਲਾਅ ਦੇ ਆਉਟਪੁੱਟ ਕਰੋ।

> **ਨੋਟ:** ਜ਼ੀਰੋ ਨਾਲ ਭਾਗ ਕਰਨਾ ਅਵੈਧ ਹੈ ਅਤੇ ਇਸਨੂੰ ਗੁੰਮ ਡਾਟਾ ਮੰਨਿਆ ਜਾਂਦਾ ਹੈ। ਅਜਿਹੇ ਮਾਮਲਿਆਂ ਨੂੰ ਕਿਵੇਂ ਸੰਭਾਲਣਾ ਹੈ, ਇਹ “ਗੁੰਮ ਹੋਣ 'ਤੇ” ਕੌਂਫਿਗਰੇਸ਼ਨ ਨਿਰਧਾਰਤ ਕਰੇਗੀ।
  	]],
  	["ro"] = [[
Asociază fiecare punct de date din prima sursă de date cu punctul de date corespunzător din a doua sursă de date și efectuează o operație specificată (adunare, scădere, înmulțire sau împărțire) asupra valorilor acestora. Pentru fiecare punct de date, perechea este primul punct de date care se încadrează în pragul de timp specificat. Configurare:

- **Prag de timp:** Durata maximă dintre punctele de date din cele două surse pentru ca acestea să fie considerate o pereche.
- **Operație:** Operația matematică efectuată asupra valorilor punctelor de date asociate (adunare, scădere, înmulțire sau împărțire).
- **În lipsă:** Specifică comportamentul atunci când un punct de date din prima sursă nu are un punct de date corespunzător în a doua sursă în cadrul pragului de timp specificat. Opțiunile includ:
  - **Omite:** Nu produce nimic pentru acel punct de date.
  - **Transmite mai departe:** Produce punctul de date original din prima sursă fără modificări.

> **Notă:** Împărțirea la zero este invalidă și este considerată ca reprezentând date lipsă. Configurația „În lipsă” va determina modul de tratare a acestor cazuri.
  	]],
  	["rm"] = [[
Collega mintga punct da datas da la funtauna da datas emprim cun il punct da datas correspundent da la segunda funtauna da datas e exequescha ina operaziun spezificada (adiziun, suttracziun, multiplicaziun u divisiun) sin lur valurs. Il punct correspundent per mintga punct da datas è il prim punct da datas che croda entaifer la limita da temp dada. Configuraziun:

- **Limita da temp:** La durada maximala tranter puncts da datas en las duas funtaunas per vegnir considerads sco paregl.
- **Operaziun:** L'operaziun matematica da far sin las valurs dals puncts da datas colligads (adiziun, suttracziun, multiplicaziun u divisiun).
- **Sch'i manca:** Definescha il cumportament, sch'in punct da datas da l'emprima funtauna n'ha nagin punct correspundent en la segunda funtauna entaifer la limita da temp dada. Opziuns:
  - **Sursiglir:** Betg generar nagut per quest punct da datas.
  - **Laschar passar:** Generar il punct da datas original da l'emprima funtauna senza midadas.

> **Remartga:** La divisiun tras nulla n'è valida e vegn considerada sco datas mancantas. La configuraziun «Sch'i manca» determinescha co tals cas vegnan tractads.
  	]],
  	["ru"] = [[
Объединяет каждую точку данных из первого источника с соответствующей точкой данных из второго источника и выполняет заданную операцию (сложение, вычитание, умножение или деление) над их значениями. Для каждой точки данных парой является первая точка, попадающая в заданный временной порог. Конфигурация:

- **Временной порог:** Максимальная длительность между точками данных двух источников, при которой они считаются парой.
- **Операция:** Математическая операция над значениями точек данных пары (сложение, вычитание, умножение или деление).
- **При отсутствии:** Определяет поведение, если для точки данных из первого источника нет соответствующей точки во втором источнике в пределах заданного временного порога. Варианты:
  - **Пропустить:** Ничего не выводить для этой точки данных.
  - **Передать:** Вывести исходную точку данных из первого источника без изменений.

> **Примечание:** Деление на ноль недопустимо и считается отсутствующими данными. Параметр «При отсутствии» определяет обработку таких случаев.
  	]],
  	["sr"] = [[
Uparuje svaku tačku podataka iz prvog izvora podataka sa odgovarajućom tačkom podataka iz drugog izvora i izvršava zadatu operaciju (sabiranje, oduzimanje, množenje ili deljenje) nad njihovim vrednostima. Par za svaku tačku podataka je prva tačka podataka koja se nalazi unutar zadatog vremenskog praga. Konfiguracija:

- **Vremenski prag:** Maksimalno trajanje između tačaka podataka u dva izvora da bi se smatrale parom.
- **Operacija:** Matematička operacija koja se izvršava nad vrednostima uparenih tačaka podataka (sabiranje, oduzimanje, množenje ili deljenje).
- **Ako nedostaje:** Određuje ponašanje kada tačka podataka u prvom izvoru nema odgovarajuću tačku podataka u drugom izvoru unutar zadatog vremenskog praga. Opcije uključuju:
  - **Preskoči:** Ne daje izlaz za tu tačku podataka.
  - **Prosledi:** Daje originalnu tačku podataka iz prvog izvora bez izmena.

> **Napomena:** Deljenje nulom je nevažeće i smatra se nedostajućim podatkom. Konfiguracija „Ako nedostaje“ određuje kako se takvi slučajevi obrađuju.
  	]],
  	["si"] = [[
පළමු දත්ත මූලාශ්‍රයේ සෑම දත්ත ලක්ෂ්‍යයක්ම දෙවන දත්ත මූලාශ්‍රයේ අනුරූප දත්ත ලක්ෂ්‍යය සමඟ යුගල කර, ඒවායේ අගයන් මත නිශ්චිත ක්‍රියාවක් (එකතු කිරීම, අඩු කිරීම, ගුණ කිරීම හෝ බෙදීම) සිදු කරයි. සෑම දත්ත ලක්ෂ්‍යයකටම ඇති යුගලය වන්නේ ලබා දී ඇති කාල සීමාව තුළ වැටෙන පළමු දත්ත ලක්ෂ්‍යයයි. වින්‍යාසය:

- **කාල සීමාව:** යුගලයක් ලෙස සැලකීමට මූලාශ්‍ර දෙකේ දත්ත ලක්ෂ්‍ය අතර උපරිම කාල පරතරය.
- **ක්‍රියාව:** යුගල කළ දත්ත ලක්ෂ්‍ය අගයන් මත සිදු කළ යුතු ගණිතමය ක්‍රියාව (එකතු කිරීම, අඩු කිරීම, ගුණ කිරීම හෝ බෙදීම).
- **නොමැති විට:** පළමු මූලාශ්‍රයේ දත්ත ලක්ෂ්‍යයකට ලබා දී ඇති කාල සීමාව තුළ දෙවන මූලාශ්‍රයේ අනුරූප දත්ත ලක්ෂ්‍යයක් නොමැති විට හැසිරීම. විකල්ප:
  - **මඟ හරින්න:** එම දත්ත ලක්ෂ්‍යය සඳහා කිසිවක් ප්‍රතිදානය නොකරන්න.
  - **ඉදිරියට යවන්න:** පළමු මූලාශ්‍රයේ මුල් දත්ත ලක්ෂ්‍යය වෙනස් නොකර ප්‍රතිදානය කරන්න.

> **සටහන:** ශුන්‍යයෙන් බෙදීම වලංගු නොවන අතර නොමැති දත්ත ලෙස සැලකේ. එවැනි අවස්ථා හසුරුවන්නේ “නොමැති විට” වින්‍යාසය මගිනි.
  	]],
  	["sk"] = [[
Spáruje každý údajový bod v prvom údajovom zdroji so zodpovedajúcim údajovým bodom v druhom údajovom zdroji a vykoná určenú operáciu (sčítanie, odčítanie, násobenie alebo delenie) s ich hodnotami. Pár pre každý údajový bod je prvý údajový bod, ktorý spadá do zadaného časového prahu. Konfigurácia:

- **Časový prah:** Maximálne trvanie medzi údajovými bodmi v dvoch zdrojoch, aby sa považovali za pár.
- **Operácia:** Matematická operácia vykonaná s hodnotami spárovaných údajových bodov (sčítanie, odčítanie, násobenie alebo delenie).
- **Pri chýbajúcom:** Určuje správanie, keď údajový bod v prvom zdroji nemá v druhom zdroji zodpovedajúci údajový bod v rámci daného časového prahu. Možnosti:
  - **Preskočiť:** Pre daný údajový bod nevytvoriť žiadny výstup.
  - **Prepustiť:** Vytvoriť pôvodný údajový bod z prvého zdroja bez úprav.

> **Poznámka:** Delenie nulou je neplatné a považuje sa za chýbajúce údaje. Konfigurácia Pri chýbajúcom určí, ako sa takéto prípady spracujú.
  	]],
  	["sl"] = [[
Vsako podatkovno točko v prvem viru združi z ustrezno podatkovno točko v drugem viru in nad njunima vrednostma izvede določeno operacijo (seštevanje, odštevanje, množenje ali deljenje). Par za vsako podatkovno točko je prva podatkovna točka, ki pade znotraj danega časovnega praga. Konfiguracija:

- **Časovni prag:** Najdaljše trajanje med podatkovnima točkama v obeh virih, da se štejeta za par.
- **Operacija:** Matematična operacija, ki se izvede nad vrednostma para (seštevanje, odštevanje, množenje ali deljenje).
- **Ob manjkajoči točki:** Določa vedenje, ko podatkovna točka v prvem viru znotraj danega časovnega praga nima ustrezne podatkovne točke v drugem viru. Možnosti vključujejo:
  - **Preskoči:** Za to podatkovno točko ne ustvari ničesar.
  - **Prepusti:** Brez sprememb ustvari izhodno podatkovno točko iz prvega vira.

> **Opomba:** Deljenje z nič je neveljavno in se šteje kot manjkajoč podatek. Konfiguracija »Ob manjkajoči točki« določa, kako se takšni primeri obravnavajo.
  	]],
  	["es"] = [[
Empareja cada punto de datos de la primera fuente de datos con el punto de datos correspondiente de la segunda fuente y realiza una operación especificada (suma, resta, multiplicación o división) sobre sus valores. El par de cada punto de datos es el primer punto de datos que se encuentra dentro del umbral de tiempo indicado. Configuración:

- **Umbral de tiempo:** La duración máxima entre puntos de datos de las dos fuentes para considerarlos un par.
- **Operación:** La operación matemática que se realizará sobre los valores de los puntos emparejados (suma, resta, multiplicación o división).
- **Si falta:** Especifica el comportamiento cuando un punto de datos de la primera fuente no tiene un punto de datos correspondiente en la segunda fuente dentro del umbral de tiempo indicado. Opciones:
  - **Omitir:** No genera nada para ese punto de datos.
  - **Pasar sin cambios:** Genera el punto de datos original de la primera fuente sin modificaciones.

> **Nota:** La división entre cero no es válida y se considera un dato ausente. La configuración «Si falta» determinará cómo se gestionan estos casos.
  	]],
  	["sw"] = [[
Huunganisha kila nukta ya data katika chanzo cha kwanza cha data na nukta ya data inayolingana katika chanzo cha pili cha data, kisha hufanya operesheni maalum (kuongeza, kutoa, kuzidisha au kugawa) kwenye thamani zao. Jozi ya kila nukta ya data ni nukta ya kwanza ya data iliyo ndani ya kikomo cha muda kilichotolewa. Usanidi:

- **Kikomo cha Muda:** Muda wa juu zaidi kati ya nukta za data katika vyanzo viwili ili zichukuliwe kuwa jozi.
- **Operesheni:** Operesheni ya kihisabati ya kufanya kwenye thamani za nukta za data zilizoanishwa (kuongeza, kutoa, kuzidisha au kugawa).
- **Isipokosekana:** Hubainisha tabia wakati nukta ya data katika chanzo cha kwanza haina nukta inayolingana katika chanzo cha pili ndani ya kikomo cha muda kilichotolewa. Chaguo ni pamoja na:
  - **Ruka:** Usitoe chochote kwa nukta hiyo ya data.
  - **Pitisha:** Toa nukta ya awali ya data kutoka chanzo cha kwanza bila kuibadilisha.

> **Dokezo:** Kugawa kwa sifuri si halali na huchukuliwa kuwa data inayokosekana. Usanidi wa Isipokosekana utaamua jinsi hali kama hizo zitakavyoshughulikiwa.
  	]],
  	["sv"] = [[
Parar ihop varje datapunkt i den första datakällan med motsvarande datapunkt i den andra datakällan och utför en angiven operation (addition, subtraktion, multiplikation eller division) på deras värden. Paret för varje datapunkt är den första datapunkt som faller inom den angivna tidsgränsen. Konfiguration:

- **Tidsgräns:** Den maximala varaktigheten mellan datapunkter i de två källorna för att de ska betraktas som ett par.
- **Operation:** Den matematiska operation som ska utföras på de parade datapunkternas värden (addition, subtraktion, multiplikation eller division).
- **Vid saknad:** Anger beteendet när en datapunkt i den första källan saknar en motsvarande datapunkt i den andra källan inom den angivna tidsgränsen. Alternativen omfattar:
  - **Hoppa över:** Skapa ingen utdata för datapunkten.
  - **Släpp igenom:** Skapa den ursprungliga datapunkten från den första källan utan ändringar.

> **Obs!** Division med noll är ogiltig och betraktas som saknade data. Konfigurationen Vid saknad avgör hur sådana fall hanteras.
  	]],
  	["ta"] = [[
முதல் தரவு மூலத்தின் ஒவ்வொரு தரவுப் புள்ளியையும் இரண்டாவது தரவு மூலத்தின் தொடர்புடைய தரவுப் புள்ளியுடன் இணைத்து, அவற்றின் மதிப்புகளில் குறிப்பிடப்பட்ட செயல்பாட்டை (கூட்டல், கழித்தல், பெருக்கல் அல்லது வகுத்தல்) செய்கிறது. ஒவ்வொரு தரவுப் புள்ளிக்குமான இணை, கொடுக்கப்பட்ட நேர வரம்பிற்குள் வரும் முதல் தரவுப் புள்ளியாகும். உள்ளமைவு:

- **நேர வரம்பு:** இணையாகக் கருதப்படும் இரு மூலங்களின் தரவுப் புள்ளிகளுக்கிடையேயான அதிகபட்ச கால அளவு.
- **செயல்பாடு:** இணைக்கப்பட்ட தரவுப் புள்ளி மதிப்புகளில் செய்ய வேண்டிய கணிதச் செயல்பாடு (கூட்டல், கழித்தல், பெருக்கல் அல்லது வகுத்தல்).
- **கிடைக்காதபோது:** முதல் மூலத்தின் தரவுப் புள்ளிக்கு கொடுக்கப்பட்ட நேர வரம்பிற்குள் இரண்டாவது மூலத்தில் தொடர்புடைய புள்ளி இல்லாதபோது செய்ய வேண்டியது. விருப்பங்கள்:
  - **தவிர்:** அந்த தரவுப் புள்ளிக்கு எதையும் வெளியிடாதே.
  - **மாற்றமின்றி அனுப்பு:** முதல் மூலத்தின் அசல் தரவுப் புள்ளியை மாற்றமின்றி வெளியிடு.

> **குறிப்பு:** பூஜ்ஜியத்தால் வகுத்தல் செல்லாது; அது கிடைக்காத தரவாகக் கருதப்படும். இத்தகைய நிலைகள் எவ்வாறு கையாளப்பட வேண்டும் என்பதை கிடைக்காதபோது உள்ளமைவு தீர்மானிக்கும்.
  	]],
  	["te"] = [[
మొదటి డేటా మూలంలోని ప్రతి డేటా పాయింట్‌ను రెండో డేటా మూలంలోని సంబంధిత డేటా పాయింట్‌తో జత చేసి, వాటి విలువలపై పేర్కొన్న ఆపరేషన్ (జోడింపు, తీసివేత, గుణకారం లేదా భాగహారం) నిర్వహిస్తుంది. ప్రతి డేటా పాయింట్‌కు జతగా ఇచ్చిన సమయ పరిమితిలోకి వచ్చే మొదటి డేటా పాయింట్ తీసుకోబడుతుంది. కాన్ఫిగరేషన్:

- **సమయ పరిమితి:** రెండు మూలాల డేటా పాయింట్ల మధ్య జతగా పరిగణించగల గరిష్ఠ వ్యవధి.
- **ఆపరేషన్:** జత చేసిన డేటా పాయింట్ విలువలపై నిర్వహించే గణిత ఆపరేషన్ (జోడింపు, తీసివేత, గుణకారం లేదా భాగహారం).
- **లభించనప్పుడు:** మొదటి మూలంలోని డేటా పాయింట్‌కు ఇచ్చిన సమయ పరిమితిలో రెండో మూలంలో సంబంధిత డేటా పాయింట్ లేనప్పుడు ప్రవర్తన. ఎంపికలు:
  - **దాటవేయి:** ఆ డేటా పాయింట్‌కు ఏదీ అవుట్‌పుట్ చేయవద్దు.
  - **అలాగే పంపు:** మొదటి మూలంలోని అసలు డేటా పాయింట్‌ను మార్పులేకుండా అవుట్‌పుట్ చేయి.

> **గమనిక:** సున్నాతో భాగించడం చెల్లదు, దాన్ని లేని డేటాగా పరిగణిస్తారు. అలాంటి సందర్భాలను ఎలా నిర్వహించాలో లభించనప్పుడు సెట్టింగ్ నిర్ణయిస్తుంది.
  	]],
  	["th"] = [[
จับคู่จุดข้อมูลแต่ละจุดในแหล่งข้อมูลแรกกับจุดข้อมูลที่สอดคล้องกันในแหล่งข้อมูลที่สอง และดำเนินการที่กำหนด (บวก ลบ คูณ หรือหาร) กับค่า จุดข้อมูลคู่ของแต่ละจุดคือจุดข้อมูลแรกที่อยู่ภายในเกณฑ์เวลาที่กำหนด การกำหนดค่า:

- **เกณฑ์เวลา:** ระยะเวลาสูงสุดระหว่างจุดข้อมูลในแหล่งข้อมูลทั้งสองที่ถือว่าเป็นคู่กัน
- **การดำเนินการ:** การดำเนินการทางคณิตศาสตร์กับค่าของจุดข้อมูลที่จับคู่กัน (บวก ลบ คูณ หรือหาร)
- **เมื่อไม่มีข้อมูล:** ระบุลักษณะการทำงานเมื่อจุดข้อมูลในแหล่งข้อมูลแรกไม่มีจุดข้อมูลที่สอดคล้องกันในแหล่งข้อมูลที่สองภายในเกณฑ์เวลาที่กำหนด ตัวเลือกมีดังนี้:
  - **ข้าม:** ไม่สร้างผลลัพธ์สำหรับจุดข้อมูลนั้น
  - **ส่งต่อ:** ส่งต่อจุดข้อมูลเดิมจากแหล่งข้อมูลแรกโดยไม่แก้ไข

> **หมายเหตุ:** การหารด้วยศูนย์ไม่ถูกต้องและถือเป็นข้อมูลที่ขาดหาย การกำหนดค่าเมื่อไม่มีข้อมูลจะกำหนดวิธีจัดการกรณีดังกล่าว
  	]],
  	["tr"] = [[
İlk veri kaynağındaki her veri noktasını ikinci veri kaynağındaki karşılık gelen veri noktasıyla eşleştirir ve değerleri üzerinde belirtilen işlemi (toplama, çıkarma, çarpma veya bölme) gerçekleştirir. Her veri noktası için eşleşme, verilen zaman eşiği içinde kalan ilk veri noktasıdır. Yapılandırma:

- **Zaman Eşiği:** İki kaynaktaki veri noktalarının eşleşme sayılması için aralarındaki maksimum süre.
- **İşlem:** Eşleştirilen veri noktası değerlerine uygulanacak matematiksel işlem (toplama, çıkarma, çarpma veya bölme).
- **Eksik Veride:** İlk kaynaktaki bir veri noktasının, verilen zaman eşiği içinde ikinci kaynakta karşılığı olmadığında uygulanacak davranışı belirtir. Seçenekler:
  - **Atla:** Bu veri noktası için hiçbir çıktı üretme.
  - **Olduğu Gibi Geçir:** İlk kaynaktaki özgün veri noktasını değiştirmeden çıktıla.

> **Not:** Sıfıra bölme geçersizdir ve eksik veri olarak kabul edilir. Bu tür durumların nasıl ele alınacağını Eksik Veride yapılandırması belirler.
  	]],
  	["uk"] = [[
Поєднує кожну точку даних із першого джерела з відповідною точкою даних із другого джерела та виконує вказану операцію (додавання, віднімання, множення або ділення) над їхніми значеннями. Для кожної точки даних парою є перша точка даних, що потрапляє у вказаний часовий поріг. Конфігурація:

- **Часовий поріг:** Максимальна тривалість між точками даних у двох джерелах, за якої вони вважаються парою.
- **Операція:** Математична операція над значеннями поєднаних точок даних (додавання, віднімання, множення або ділення).
- **Якщо немає:** Визначає поведінку, коли для точки даних із першого джерела немає відповідної точки даних у другому джерелі в межах вказаного часового порогу. Варіанти:
  - **Пропустити:** Нічого не виводити для цієї точки даних.
  - **Передати без змін:** Вивести початкову точку даних із першого джерела без змін.

> **Примітка:** Ділення на нуль недійсне й вважається відсутніми даними. Конфігурація «Якщо немає» визначає обробку таких випадків.
  	]],
  	["vi"] = [[
Ghép mỗi điểm dữ liệu trong nguồn dữ liệu thứ nhất với điểm dữ liệu tương ứng trong nguồn dữ liệu thứ hai và thực hiện phép toán được chỉ định (cộng, trừ, nhân hoặc chia) trên các giá trị của chúng. Điểm dữ liệu ghép với mỗi điểm là điểm dữ liệu đầu tiên nằm trong ngưỡng thời gian đã cho. Cấu hình:

- **Ngưỡng thời gian:** Khoảng thời gian tối đa giữa các điểm dữ liệu trong hai nguồn để được xem là một cặp.
- **Phép toán:** Phép toán cần thực hiện trên các giá trị điểm dữ liệu đã ghép (cộng, trừ, nhân hoặc chia).
- **Khi thiếu:** Chỉ định cách xử lý khi một điểm dữ liệu trong nguồn thứ nhất không có điểm dữ liệu tương ứng trong nguồn thứ hai trong ngưỡng thời gian đã cho. Các tùy chọn gồm:
  - **Bỏ qua:** Không tạo đầu ra cho điểm dữ liệu đó.
  - **Chuyển tiếp:** Xuất điểm dữ liệu ban đầu từ nguồn thứ nhất mà không thay đổi.

> **Lưu ý:** Chia cho 0 là không hợp lệ và được xem là dữ liệu thiếu. Cấu hình Khi thiếu sẽ xác định cách xử lý các trường hợp đó.
  	]],
  },
  config = {
    duration {
      id = "threshold",
      name = "_time_threshold",
      default = core.DURATION.MINUTE,
    },
    enum {
      id = "operation",
      name = "_operation",
      options = { "_addition", "_subtraction", "_multiplication", "_division" },
      default = "_addition",
    },
    enum {
      id = "on_missing",
      name = "_on_missing",
      options = { "_skip", "_pass_through" },
      default = "_skip",
    },
  },

  -- Generator function
  generator = function(sources, config)
    local threshold = config.threshold or error("Missing 'threshold' in config")
    local operation = config.operation or error("Missing 'operation' in config")
    local on_missing = config.on_missing or error("Missing 'on_missing' in config")
    local source1 = sources[1] or error("Missing first data source")
    local source2 = sources[2] or error("Missing second data source")

    local source2_carry = nil

    return function()
      local result_dp = nil

      while true do
        local data_point = source1.dp()
        if not data_point then
          return nil
        end

        result_dp = data_point

        local time1 = data_point.timestamp
        local paired_dp = nil
        while true do
          local candidate_dp = source2_carry or source2.dp()
          source2_carry = nil
          if not candidate_dp then
            break
          end

          local time2 = candidate_dp.timestamp
          local time_diff = math.abs(time1 - time2)

          if time_diff <= threshold then
            paired_dp = candidate_dp
            break
          elseif time2 < time1 - threshold then
            source2_carry = candidate_dp
            break
          end
        end


        if paired_dp then
          if operation == "_addition" then
            result_dp.value = data_point.value + paired_dp.value
          elseif operation == "_subtraction" then
            result_dp.value = data_point.value - paired_dp.value
          elseif operation == "_multiplication" then
            result_dp.value = data_point.value * paired_dp.value
          elseif operation == "_division" then
            if paired_dp.value == 0 then
              if on_missing == "_skip" then
                result_dp = nil
              end
            else
              result_dp.value = data_point.value / paired_dp.value
            end
          else
            error("invalid operation: " .. operation)
          end
        elseif on_missing == "_pass_through" then
          break
        else
          result_dp = nil
        end

        if result_dp ~= nil then break end
      end

      return result_dp
    end
  end,
}
